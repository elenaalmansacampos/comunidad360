package com.comunidad360.auth;

import com.comunidad360.audit.AuditService;
import com.comunidad360.auth.dto.AcceptInvitationRequest;
import com.comunidad360.auth.dto.ForgotPasswordRequest;
import com.comunidad360.auth.dto.LoginRequest;
import com.comunidad360.auth.dto.ResetPasswordRequest;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.config.AppProperties;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.Role;
import com.comunidad360.membership.RoleRepository;
import com.comunidad360.security.JwtService;
import com.comunidad360.security.TokenSupport;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final RoleRepository roleRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties properties;

    public AuthService(AppUserRepository userRepository,
                       CommunityRepository communityRepository,
                       MembershipRepository membershipRepository,
                       MembershipRoleRepository membershipRoleRepository,
                       RoleRepository roleRepository,
                       InvitationRepository invitationRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       RefreshTokenService refreshTokenService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       AppProperties properties) {
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.roleRepository = roleRepository;
        this.invitationRepository = invitationRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.properties = properties;
    }

    public AuthTokens login(LoginRequest request, String userAgent, String ipHash,
                            HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            throw BusinessException.unauthorized("AUTH_INVALID_CREDENTIALS", "errors.auth.invalidCredentials");
        }

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw BusinessException.unauthorized("AUTH_ACCOUNT_LOCKED", "errors.auth.accountLocked");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(now.plus(LOCK_DURATION));
            }
            user.setUpdatedAt(now);
            userRepository.save(user);
            auditService.record(null, user.getId(), "USER", "AUTH_LOGIN_FAILED", "USER", user.getId(),
                    null, Map.of("reason", "BAD_CREDENTIALS"), httpRequest);
            throw BusinessException.unauthorized("AUTH_INVALID_CREDENTIALS", "errors.auth.invalidCredentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.unauthorized("AUTH_ACCOUNT_DISABLED", "errors.auth.accountDisabled");
        }
        if (user.getEmailVerifiedAt() == null) {
            throw BusinessException.unauthorized("AUTH_EMAIL_NOT_VERIFIED", "errors.auth.emailNotVerified");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.create(user.getId(), userAgent, ipHash);
        String accessToken = jwtService.createAccessToken(user);
        auditService.record(null, user.getId(), "USER", "AUTH_LOGIN_SUCCESS", "USER", user.getId(),
                null, Map.of("email", user.getEmail()), httpRequest);
        return new AuthTokens(accessToken, refreshToken.rawToken(), jwtService.getAccessTokenTtlSeconds());
    }

    public AuthTokens refresh(String rawToken, String userAgent, String ipHash) {
        if (rawToken == null || rawToken.isBlank()) {
            throw BusinessException.unauthorized("AUTH_REFRESH_MISSING", "errors.auth.refreshMissing");
        }
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawToken, userAgent, ipHash);
        AppUser user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> BusinessException.unauthorized(
                        "AUTH_REFRESH_INVALID", "errors.auth.refreshMissing"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.unauthorized("AUTH_ACCOUNT_DISABLED", "errors.auth.accountDisabled");
        }
        String accessToken = jwtService.createAccessToken(user);
        return new AuthTokens(accessToken, rotated.rawToken(), jwtService.getAccessTokenTtlSeconds());
    }

    @Transactional
    public AuthTokens acceptInvitation(AcceptInvitationRequest request, String userAgent, String ipHash,
                                       HttpServletRequest httpRequest) {
        Invitation invitation = invitationRepository.findByTokenHash(TokenSupport.sha256Hex(request.token()))
                .orElseThrow(() -> BusinessException.unprocessable(
                        "INVITATION_INVALID", "errors.invitation.invalid"));
        if (invitation.getAcceptedAt() != null) {
            throw BusinessException.conflict("INVITATION_ALREADY_USED", "errors.invitation.used");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw BusinessException.unprocessable("INVITATION_EXPIRED", "errors.invitation.expired");
        }

        Community community = communityRepository.findById(invitation.getCommunityId())
                .orElseThrow(() -> BusinessException.unprocessable(
                        "INVITATION_INVALID", "errors.invitation.invalid"));
        if (community.getStatus() != CommunityStatus.ACTIVE) {
            throw BusinessException.forbidden("TENANT_FORBIDDEN", "errors.tenant.forbidden");
        }
        Role role = roleRepository.findById(invitation.getRoleCode())
                .filter(value -> !"PLATFORM".equals(value.getScope()))
                .orElseThrow(() -> BusinessException.unprocessable("ROLE_INVALID", "errors.role.invalid"));

        String email = normalizeEmail(invitation.getEmail());
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        Instant now = Instant.now();
        if (user == null) {
            user = new AppUser(
                    UUID.randomUUID(),
                    email,
                    passwordEncoder.encode(request.password()),
                    request.fullName().trim(),
                    community.getDefaultLanguage(),
                    UserStatus.ACTIVE,
                    now,
                    now
            );
            user.setEmailVerifiedAt(now);
            userRepository.save(user);
        } else {
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw BusinessException.unauthorized("AUTH_ACCOUNT_DISABLED", "errors.auth.accountDisabled");
            }
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw BusinessException.unauthorized(
                        "AUTH_INVALID_CREDENTIALS", "errors.auth.invalidCredentials");
            }
            if (user.getEmailVerifiedAt() == null) {
                user.setEmailVerifiedAt(now);
                user.setUpdatedAt(now);
                userRepository.save(user);
            }
        }

        if (membershipRepository.findByCommunityIdAndUserId(community.getId(), user.getId()).isPresent()) {
            throw BusinessException.conflict("INVITATION_ALREADY_USED", "errors.invitation.used");
        }

        Membership membership = new Membership(
                UUID.randomUUID(),
                community.getId(),
                user.getId(),
                MembershipStatus.ACTIVE,
                now,
                now,
                now
        );
        membershipRepository.save(membership);
        membershipRoleRepository.save(new MembershipRole(
                UUID.randomUUID(),
                membership.getId(),
                role.getCode(),
                now,
                invitation.getInvitedByUserId()
        ));

        invitation.setAcceptedAt(now);
        invitationRepository.save(invitation);

        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.create(user.getId(), userAgent, ipHash);
        String accessToken = jwtService.createAccessToken(user);
        auditService.record(community.getId(), user.getId(), "USER", "INVITATION_ACCEPTED",
                "MEMBERSHIP", membership.getId(), null,
                Map.of("role", role.getCode(), "email", email), httpRequest);
        return new AuthTokens(accessToken, refreshToken.rawToken(), jwtService.getAccessTokenTtlSeconds());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return;
        }
        String rawToken = TokenSupport.randomToken();
        passwordResetTokenRepository.save(new PasswordResetToken(
                UUID.randomUUID(),
                user.getId(),
                TokenSupport.sha256Hex(rawToken),
                Instant.now().plus(PASSWORD_RESET_TTL),
                Instant.now()
        ));
        if (properties.getSecurity().isExposeDevTokens()) {
            // lgtm[java/sensitive-log] Token solo en desarrollo y necesario para probar el flujo local.
            log.info("Password reset token generated for {}: {}", email, rawToken);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(
                        TokenSupport.sha256Hex(request.token()))
                .orElseThrow(() -> BusinessException.unprocessable(
                        "PASSWORD_RESET_INVALID", "errors.password.resetInvalid"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw BusinessException.unprocessable(
                    "PASSWORD_RESET_INVALID", "errors.password.resetInvalid");
        }
        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> BusinessException.unprocessable(
                        "PASSWORD_RESET_INVALID", "errors.password.resetInvalid"));
        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUpdatedAt(now);
        userRepository.save(user);
        refreshTokenService.revokeAll(user.getId());
        token.setUsedAt(now);
        passwordResetTokenRepository.save(token);
        auditService.record(null, user.getId(), "USER", "PASSWORD_RESET", "USER", user.getId(),
                null, null, httpRequest);
    }

    public void logout(String refreshToken, UUID userId, HttpServletRequest httpRequest) {
        refreshTokenService.revoke(refreshToken);
        if (userId != null) {
            auditService.record(null, userId, "USER", "AUTH_LOGOUT", "USER", userId,
                    null, null, httpRequest);
        }
    }

    public void logoutAll(UUID userId, HttpServletRequest httpRequest) {
        refreshTokenService.revokeAll(userId);
        auditService.record(null, userId, "USER", "AUTH_LOGOUT_ALL", "USER", userId,
                null, null, httpRequest);
    }

    public CreatedInvitation createInvitation(UUID communityId, UUID invitedByUserId, String email, String roleCode) {
        String normalizedEmail = normalizeEmail(email);
        Role role = roleRepository.findById(roleCode)
                .filter(value -> !"PLATFORM".equals(value.getScope()))
                .orElseThrow(() -> BusinessException.unprocessable("ROLE_INVALID", "errors.role.invalid"));

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(existing -> {
            if (membershipRepository.findByCommunityIdAndUserId(communityId, existing.getId()).isPresent()) {
                throw BusinessException.conflict(
                        "INVITATION_ALREADY_USED", "errors.invitation.used");
            }
        });

        String rawToken = TokenSupport.randomToken();
        Invitation invitation = new Invitation(
                UUID.randomUUID(),
                communityId,
                normalizedEmail,
                role.getCode(),
                TokenSupport.sha256Hex(rawToken),
                Instant.now().plus(INVITATION_TTL),
                invitedByUserId,
                Instant.now()
        );
        invitationRepository.save(invitation);
        return new CreatedInvitation(invitation, rawToken);
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public record CreatedInvitation(Invitation invitation, String rawToken) {
    }
}
