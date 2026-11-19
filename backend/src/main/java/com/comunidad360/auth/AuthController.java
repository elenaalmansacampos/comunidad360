package com.comunidad360.auth;

import com.comunidad360.auth.dto.AcceptInvitationRequest;
import com.comunidad360.auth.dto.ForgotPasswordRequest;
import com.comunidad360.auth.dto.LoginRequest;
import com.comunidad360.auth.dto.ResetPasswordRequest;
import com.comunidad360.auth.dto.TokenResponse;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.security.AuthenticatedUser;
import com.comunidad360.security.CookieService;
import com.comunidad360.security.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    public AuthController(AuthService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        AuthTokens tokens = authService.login(
                request,
                RequestInfo.userAgent(httpRequest),
                RequestInfo.ipHash(httpRequest),
                httpRequest
        );
        cookieService.setRefreshCookie(httpResponse, tokens.refreshToken());
        return ResponseEntity.ok(TokenResponse.bearer(tokens.accessToken(), tokens.expiresIn()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = CookieService.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (refreshToken == null) {
            throw BusinessException.unauthorized("AUTH_REFRESH_MISSING", "errors.auth.refreshMissing");
        }
        AuthTokens tokens = authService.refresh(
                refreshToken,
                RequestInfo.userAgent(httpRequest),
                RequestInfo.ipHash(httpRequest)
        );
        cookieService.setRefreshCookie(httpResponse, tokens.refreshToken());
        return ResponseEntity.ok(TokenResponse.bearer(tokens.accessToken(), tokens.expiresIn()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieService.REFRESH_COOKIE, required = false) String refreshToken,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        authService.logout(refreshToken, user == null ? null : user.id(), httpRequest);
        cookieService.clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser user,
                                          HttpServletRequest httpRequest,
                                          HttpServletResponse httpResponse) {
        authService.logoutAll(user.id(), httpRequest);
        cookieService.clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                              HttpServletRequest httpRequest) {
        authService.resetPassword(request, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<TokenResponse> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request,
                                                          HttpServletRequest httpRequest,
                                                          HttpServletResponse httpResponse) {
        AuthTokens tokens = authService.acceptInvitation(
                request,
                RequestInfo.userAgent(httpRequest),
                RequestInfo.ipHash(httpRequest),
                httpRequest
        );
        cookieService.setRefreshCookie(httpResponse, tokens.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TokenResponse.bearer(tokens.accessToken(), tokens.expiresIn()));
    }
}
