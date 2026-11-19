package com.comunidad360.votings;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.PropertyOwnership;
import com.comunidad360.property.PropertyOwnershipRepository;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitRepository;
import com.comunidad360.votings.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class VotingService {
    private final VotingRepository votingRepository;
    private final VotingOptionRepository optionRepository;
    private final VotingEligibilityRepository eligibilityRepository;
    private final VoteRepository voteRepository;
    private final MembershipRepository membershipRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final PropertyOwnershipRepository ownershipRepository;
    private final UnitRepository unitRepository;
    private final AuditService auditService;

    public VotingService(VotingRepository votingRepository, VotingOptionRepository optionRepository,
                         VotingEligibilityRepository eligibilityRepository, VoteRepository voteRepository,
                         MembershipRepository membershipRepository, TenantAccessService tenantAccessService,
                         PermissionService permissionService, PropertyOwnershipRepository ownershipRepository,
                         UnitRepository unitRepository, AuditService auditService) {
        this.votingRepository = votingRepository;
        this.optionRepository = optionRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.voteRepository = voteRepository;
        this.membershipRepository = membershipRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.ownershipRepository = ownershipRepository;
        this.unitRepository = unitRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<VotingResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "VOTE_VIEW");
        return votingRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .map(voting -> toResponse(communityId, membership, voting)).toList();
    }

    @Transactional(readOnly = true)
    public VotingDetailResponse detail(UUID communityId, UUID userId, UUID votingId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "VOTE_VIEW");
        Voting voting = find(communityId, votingId);
        List<VotingOptionResponse> options = optionRepository
                .findByCommunityIdAndVotingIdOrderByPositionAsc(communityId, votingId).stream()
                .map(VotingOptionResponse::from).toList();
        VotingResultsResponse results = voting.getStatus() == VotingStatus.CLOSED
                ? results(communityId, votingId) : null;
        return new VotingDetailResponse(toResponse(communityId, membership, voting), options, results);
    }

    @Transactional
    public VotingResponse create(UUID communityId, UUID userId, CreateVotingRequest request,
                                 HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "VOTE_MANAGE");
        if (request.closesAt().isBefore(Instant.now())) {
            throw BusinessException.unprocessable("VOTING_DATES_INVALID", "errors.voting.datesInvalid");
        }
        Set<String> unique = new LinkedHashSet<>();
        request.options().forEach(option -> unique.add(option.trim()));
        if (unique.size() < 2 || unique.stream().anyMatch(String::isBlank)) {
            throw BusinessException.unprocessable("VOTING_OPTIONS_INVALID", "errors.voting.optionsInvalid");
        }
        Instant now = Instant.now();
        Voting voting = votingRepository.saveAndFlush(new Voting(
                UUID.randomUUID(), communityId, request.title().trim(), clean(request.description()),
                request.opensAt(), request.closesAt(), userId, now, now));
        int position = 1;
        for (String label : unique) {
            optionRepository.save(new VotingOption(UUID.randomUUID(), communityId, voting.getId(), label, position++));
        }
        optionRepository.flush();
        auditService.record(communityId, userId, "USER", "VOTING_CREATED", "VOTING",
                voting.getId(), null, VotingResponse.from(voting, 0, false), httpRequest);
        return VotingResponse.from(voting, 0, false);
    }

    @Transactional
    public VotingResponse open(UUID communityId, UUID userId, UUID votingId,
                               HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "VOTE_MANAGE");
        Voting voting = find(communityId, votingId);
        if (voting.getStatus() != VotingStatus.DRAFT) {
            throw BusinessException.unprocessable("VOTING_STATUS_INVALID", "errors.voting.statusInvalid");
        }
        Instant now = Instant.now();
        if (voting.getOpensAt() == null) voting.setOpensAt(now);
        if (voting.getClosesAt().isBefore(now)) {
            throw BusinessException.unprocessable("VOTING_DATES_INVALID", "errors.voting.datesInvalid");
        }
        List<Membership> members = membershipRepository.findByCommunityIdOrderByCreatedAtAsc(communityId);
        for (Membership member : members) {
            if (member.getStatus() != MembershipStatus.ACTIVE) continue;
            eligibilityRepository.save(new VotingEligibility(
                    UUID.randomUUID(), communityId, votingId, member.getId(),
                    coefficientFor(communityId, member.getUserId())));
        }
        eligibilityRepository.flush();
        voting.setStatus(VotingStatus.OPEN);
        voting.setUpdatedAt(now);
        Voting saved = votingRepository.saveAndFlush(voting);
        auditService.record(communityId, userId, "USER", "VOTING_OPENED", "VOTING",
                votingId, null, VotingResponse.from(saved, members.size(), false), httpRequest);
        return toResponse(communityId, membership, saved);
    }

    @Transactional
    public VotingResponse close(UUID communityId, UUID userId, UUID votingId,
                                HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "VOTE_MANAGE");
        Voting voting = find(communityId, votingId);
        if (voting.getStatus() != VotingStatus.OPEN) {
            throw BusinessException.unprocessable("VOTING_STATUS_INVALID", "errors.voting.statusInvalid");
        }
        voting.setStatus(VotingStatus.CLOSED);
        voting.setUpdatedAt(Instant.now());
        Voting saved = votingRepository.saveAndFlush(voting);
        auditService.record(communityId, userId, "USER", "VOTING_CLOSED", "VOTING",
                votingId, null, VotingResponse.from(saved, 0, false), httpRequest);
        return toResponse(communityId, membership, saved);
    }

    @Transactional
    public VotingDetailResponse castVote(UUID communityId, UUID userId, UUID votingId,
                                         CastVoteRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "VOTE_CAST");
        Voting voting = find(communityId, votingId);
        Instant now = Instant.now();
        if (voting.getStatus() != VotingStatus.OPEN || (voting.getOpensAt() != null && now.isBefore(voting.getOpensAt()))
                || (voting.getClosesAt() != null && now.isAfter(voting.getClosesAt()))) {
            throw BusinessException.unprocessable("VOTING_NOT_OPEN", "errors.voting.notOpen");
        }
        VotingEligibility eligibility = eligibilityRepository
                .findByVotingIdAndMembershipId(votingId, membership.getId())
                .orElseThrow(() -> BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied"));
        if (eligibility.isHasVoted()) {
            throw BusinessException.conflict("VOTE_ALREADY_CAST", "errors.voting.alreadyVoted");
        }
        VotingOption option = optionRepository.findByIdAndCommunityId(request.optionId(), communityId)
                .filter(value -> value.getVotingId().equals(votingId))
                .orElseThrow(() -> BusinessException.unprocessable("VOTING_OPTION_INVALID", "errors.voting.optionInvalid"));
        voteRepository.saveAndFlush(new Vote(UUID.randomUUID(), communityId, votingId,
                eligibility.getId(), option.getId(), now));
        eligibility.setHasVoted(true);
        eligibilityRepository.saveAndFlush(eligibility);
        auditService.record(communityId, userId, "USER", "VOTE_CAST", "VOTING",
                votingId, null, Map.of("optionId", option.getId().toString()), httpRequest);
        return detail(communityId, userId, votingId);
    }

    @Transactional(readOnly = true)
    public VotingResultsResponse results(UUID communityId, UUID votingId) {
        List<VotingEligibility> eligibility = eligibilityRepository.findByCommunityIdAndVotingId(communityId, votingId);
        List<VotingOption> options = optionRepository.findByCommunityIdAndVotingIdOrderByPositionAsc(communityId, votingId);
        List<Vote> votes = voteRepository.findByCommunityIdAndVotingId(communityId, votingId);
        Map<UUID, Long> countByOption = new HashMap<>();
        Map<UUID, BigDecimal> coefficientByOption = new HashMap<>();
        Map<UUID, BigDecimal> eligibilityCoefficient = new HashMap<>();
        eligibility.forEach(value -> eligibilityCoefficient.put(value.getId(), value.getCoefficientSnapshot()));
        BigDecimal totalCoefficient = eligibility.stream().map(VotingEligibility::getCoefficientSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal participatingCoefficient = BigDecimal.ZERO;
        Set<UUID> participants = new HashSet<>();
        for (Vote vote : votes) {
            countByOption.merge(vote.getOptionId(), 1L, Long::sum);
            BigDecimal coefficient = eligibilityCoefficient.getOrDefault(vote.getEligibilityId(), BigDecimal.ZERO);
            coefficientByOption.merge(vote.getOptionId(), coefficient, BigDecimal::add);
            participatingCoefficient = participatingCoefficient.add(coefficient);
            participants.add(vote.getEligibilityId());
        }
        List<VotingOptionResultResponse> optionResults = new ArrayList<>();
        for (VotingOption option : options) {
            BigDecimal coefficient = coefficientByOption.getOrDefault(option.getId(), BigDecimal.ZERO);
            BigDecimal percentage = participatingCoefficient.signum() == 0 ? BigDecimal.ZERO
                    : coefficient.multiply(BigDecimal.valueOf(100))
                            .divide(participatingCoefficient, 2, RoundingMode.HALF_UP);
            optionResults.add(new VotingOptionResultResponse(option.getId(), option.getLabel(),
                    countByOption.getOrDefault(option.getId(), 0L), coefficient.setScale(6, RoundingMode.HALF_UP),
                    percentage));
        }
        BigDecimal quorum = totalCoefficient.signum() == 0 ? BigDecimal.ZERO
                : participatingCoefficient.multiply(BigDecimal.valueOf(100))
                        .divide(totalCoefficient, 2, RoundingMode.HALF_UP);
        return new VotingResultsResponse(eligibility.size(), participants.size(),
                totalCoefficient.setScale(6, RoundingMode.HALF_UP),
                participatingCoefficient.setScale(6, RoundingMode.HALF_UP), quorum, optionResults);
    }

    private VotingResponse toResponse(UUID communityId, Membership membership, Voting voting) {
        long total = eligibilityRepository.findByCommunityIdAndVotingId(communityId, voting.getId()).size();
        boolean hasVoted = eligibilityRepository.findByVotingIdAndMembershipId(voting.getId(), membership.getId())
                .map(VotingEligibility::isHasVoted).orElse(false);
        return VotingResponse.from(voting, total, hasVoted);
    }

    private Voting find(UUID communityId, UUID votingId) {
        return votingRepository.findByIdAndCommunityId(votingId, communityId)
                .orElseThrow(() -> BusinessException.notFound("VOTING_NOT_FOUND", "errors.resource.notFound"));
    }

    private BigDecimal coefficientFor(UUID communityId, UUID userId) {
        LocalDate today = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO;
        for (PropertyOwnership ownership : ownershipRepository
                .findByCommunityIdAndUserIdOrderByValidFromDesc(communityId, userId)) {
            if (!ownership.isActiveOn(today)) continue;
            Unit unit = unitRepository.findById(ownership.getUnitId()).orElse(null);
            if (unit == null || !unit.getCommunityId().equals(communityId)) continue;
            total = total.add(unit.getCoefficient().multiply(ownership.getSharePercentage())
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        }
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
