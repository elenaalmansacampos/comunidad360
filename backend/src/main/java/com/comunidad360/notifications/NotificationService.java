package com.comunidad360.notifications;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.meetings.Meeting;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.notifications.dto.NotificationPreferenceRequest;
import com.comunidad360.notifications.dto.NotificationPreferenceResponse;
import com.comunidad360.notifications.dto.NotificationResponse;
import com.comunidad360.push.PushSubscriptionService;
import com.comunidad360.push.dto.PushSendSummary;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Set<String> CATEGORIES = Set.of(
            "MEETINGS", "INCIDENTS", "FINANCE", "SYSTEM"
    );
    private static final List<NotificationChannel> CHANNELS = List.of(
            NotificationChannel.IN_APP, NotificationChannel.EMAIL, NotificationChannel.PUSH
    );
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final EmailService emailService;
    private final PushSubscriptionService pushService;
    private final MessageSource messageSource;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferenceRepository preferenceRepository,
                               NotificationDeliveryRepository deliveryRepository,
                               MembershipRepository membershipRepository,
                               AppUserRepository userRepository,
                               EmailService emailService,
                               PushSubscriptionService pushService,
                               MessageSource messageSource) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.deliveryRepository = deliveryRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.pushService = pushService;
        this.messageSource = messageSource;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID userId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndChannelAndStatusNotOrderByCreatedAtDesc(
                        userId, NotificationChannel.IN_APP, NotificationStatus.READ)
                : notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(
                        userId, NotificationChannel.IN_APP);
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndChannelAndStatusNot(
                userId, NotificationChannel.IN_APP, NotificationStatus.READ);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> BusinessException.notFound(
                        "NOTIFICATION_NOT_FOUND", "errors.resource.notFound"));
        if (notification.getStatus() != NotificationStatus.READ) {
            notification.markRead(Instant.now());
            notificationRepository.saveAndFlush(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> preferences(UUID userId) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (NotificationPreference preference : preferenceRepository.findByUserIdOrderByCategoryAscChannelAsc(userId)) {
            if (preference.getCommunityId() == null) {
                values.put(key(preference.getCategory(), preference.getChannel()), preference.isEnabled());
            }
        }
        List<NotificationPreferenceResponse> result = new ArrayList<>();
        for (String category : CATEGORIES) {
            for (NotificationChannel channel : CHANNELS) {
                result.add(new NotificationPreferenceResponse(
                        category, channel, values.getOrDefault(key(category, channel), true)));
            }
        }
        return result;
    }

    @Transactional
    public List<NotificationPreferenceResponse> updatePreferences(
            UUID userId, List<NotificationPreferenceRequest> requests) {
        Instant now = Instant.now();
        for (NotificationPreferenceRequest request : requests) {
            if (!CATEGORIES.contains(request.category())) {
                throw BusinessException.unprocessable("NOTIFICATION_CATEGORY_INVALID", "errors.validation");
            }
            if (!CHANNELS.contains(request.channel())) {
                throw BusinessException.unprocessable("NOTIFICATION_CHANNEL_INVALID", "errors.validation");
            }
            NotificationPreference preference = preferenceRepository
                    .findByUserIdAndCommunityIdIsNullAndCategoryAndChannel(
                            userId, request.category(), request.channel())
                    .orElseGet(() -> new NotificationPreference(
                            UUID.randomUUID(), userId, null, request.category(),
                            request.channel(), true, now));
            preference.setEnabled(request.enabled());
            preference.setUpdatedAt(now);
            preferenceRepository.save(preference);
        }
        preferenceRepository.flush();
        return preferences(userId);
    }

    @Transactional
    public void notifyMeetingConvened(Meeting meeting, UUID actorUserId) {
        notifyMeeting(meeting, actorUserId, "MEETING_CONVENED",
                "mail.meeting.convened.subject", "mail.meeting.convened.body");
    }

    @Transactional
    public void notifyMeetingMinutes(Meeting meeting, UUID actorUserId) {
        notifyMeeting(meeting, actorUserId, "MEETING_MINUTES",
                "mail.meeting.minutes.subject", "mail.meeting.minutes.body");
    }

    private void notifyMeeting(Meeting meeting, UUID actorUserId, String type,
                               String subjectKey, String bodyKey) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("meetingId", meeting.getId().toString());
        params.put("title", meeting.getTitle());
        params.put("scheduledStart", meeting.getScheduledStart().toString());
        params.put("location", meeting.getLocation() == null ? "" : meeting.getLocation());
        String date = DATE_FORMAT.format(meeting.getScheduledStart());
        for (Membership membership : membershipRepository.findByCommunityIdOrderByCreatedAtAsc(meeting.getCommunityId())) {
            if (membership.getStatus() != MembershipStatus.ACTIVE || membership.getUserId().equals(actorUserId)) {
                continue;
            }
            AppUser user = userRepository.findById(membership.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            notifyUser(user, meeting.getCommunityId(), type, subjectKey, bodyKey, params,
                    "/communities/" + meeting.getCommunityId() + "/meetings/" + meeting.getId(),
                    meeting.getTitle(), date, params.get("location").toString());
        }
    }

    private void notifyUser(AppUser user, UUID communityId, String type, String subjectKey, String bodyKey,
                            Map<String, Object> params, String actionUrl, Object... emailArgs) {
        boolean inApp = enabled(user.getId(), "MEETINGS", NotificationChannel.IN_APP);
        boolean email = enabled(user.getId(), "MEETINGS", NotificationChannel.EMAIL);
        boolean push = enabled(user.getId(), "MEETINGS", NotificationChannel.PUSH) && pushService.isConfigured();
        if (!inApp && !email && !push) {
            return;
        }
        String locale = user.getPreferredLanguage() == null ? "es" : user.getPreferredLanguage();
        NotificationChannel primary = inApp
                ? NotificationChannel.IN_APP
                : (email ? NotificationChannel.EMAIL : NotificationChannel.PUSH);
        Notification notification = notificationRepository.saveAndFlush(new Notification(
                UUID.randomUUID(),
                communityId,
                user.getId(),
                type,
                "MEETINGS",
                subjectKey,
                params,
                locale,
                primary,
                NotificationStatus.SENT,
                Instant.now()
        ));
        if (email) {
            EmailService.EmailResult result = emailService.send(
                    user.getEmail(), subjectKey, bodyKey, emailArgs,
                    Locale.forLanguageTag(locale));
            deliveryRepository.save(new NotificationDelivery(
                    UUID.randomUUID(),
                    notification.getId(),
                    NotificationChannel.EMAIL,
                    1,
                    result.sent() ? NotificationDeliveryStatus.SENT : NotificationDeliveryStatus.FAILED,
                    null,
                    result.errorCode(),
                    Instant.now()
            ));
            if (!result.sent() && !"MAIL_DISABLED".equals(result.errorCode())) {
                notification.setErrorCode(result.errorCode());
                notificationRepository.save(notification);
            }
        }
        if (push) {
            Locale userLocale = Locale.forLanguageTag(locale);
            String title = messageSource.getMessage(subjectKey, emailArgs, subjectKey, userLocale);
            String body = messageSource.getMessage(bodyKey, emailArgs, bodyKey, userLocale);
            PushSendSummary summary = pushService.sendToUser(user.getId(), title, body, actionUrl);
            if (summary.attempted() > 0) {
                deliveryRepository.save(new NotificationDelivery(
                        UUID.randomUUID(),
                        notification.getId(),
                        NotificationChannel.PUSH,
                        1,
                        summary.sent() > 0 ? NotificationDeliveryStatus.SENT : NotificationDeliveryStatus.FAILED,
                        null,
                        summary.failed() > 0 ? "PUSH_PARTIAL_FAILURE" : null,
                        Instant.now()
                ));
            }
        }
    }

    private boolean enabled(UUID userId, String category, NotificationChannel channel) {
        return preferenceRepository
                .findByUserIdAndCommunityIdIsNullAndCategoryAndChannel(userId, category, channel)
                .map(NotificationPreference::isEnabled)
                .orElse(true);
    }

    private String key(String category, NotificationChannel channel) {
        return category + ":" + channel.name();
    }
}
