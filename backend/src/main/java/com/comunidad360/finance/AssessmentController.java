package com.comunidad360.finance;

import com.comunidad360.finance.dto.AssessmentChargeResponse;
import com.comunidad360.finance.dto.AssessmentResponse;
import com.comunidad360.finance.dto.CreateAssessmentRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/finance")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/assessments")
    public List<AssessmentResponse> list(@PathVariable UUID communityId,
                                         @RequestParam(required = false) UUID fiscalYearId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return assessmentService.list(communityId, user.id(), fiscalYearId);
    }

    @PostMapping("/assessments")
    public ResponseEntity<AssessmentResponse> create(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateAssessmentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.create(communityId, user.id(), request, idempotencyKey, httpRequest));
    }

    @PostMapping("/assessments/{assessmentId}/issue")
    public AssessmentResponse issue(@PathVariable UUID communityId,
                                    @PathVariable UUID assessmentId,
                                    @AuthenticationPrincipal AuthenticatedUser user,
                                    HttpServletRequest httpRequest) {
        return assessmentService.issue(communityId, user.id(), assessmentId, httpRequest);
    }

    @GetMapping("/charges")
    public List<AssessmentChargeResponse> charges(@PathVariable UUID communityId,
                                                  @RequestParam(required = false) UUID fiscalYearId,
                                                  @AuthenticationPrincipal AuthenticatedUser user) {
        return assessmentService.listCharges(communityId, user.id(), fiscalYearId);
    }
}
