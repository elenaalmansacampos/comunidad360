package com.comunidad360.push;

import com.comunidad360.push.dto.PushPublicKeyResponse;
import com.comunidad360.push.dto.PushSendSummary;
import com.comunidad360.push.dto.PushSubscriptionRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/me/push")
public class PushController {

    private final PushSubscriptionService pushService;

    public PushController(PushSubscriptionService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/public-key")
    public PushPublicKeyResponse publicKey() {
        return new PushPublicKeyResponse(pushService.isConfigured(), pushService.publicKey());
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal AuthenticatedUser user,
                          @Valid @RequestBody PushSubscriptionRequest request,
                          HttpServletRequest httpRequest) {
        pushService.subscribe(user.id(), request, httpRequest.getHeader("User-Agent"));
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@AuthenticationPrincipal AuthenticatedUser user,
                            @RequestParam @NotBlank String endpoint) {
        pushService.unsubscribe(user.id(), endpoint);
    }

    @PostMapping("/test")
    public PushSendSummary test(@AuthenticationPrincipal AuthenticatedUser user) {
        return pushService.sendToUser(user.id(), "Comunidad 360", "Notificación de prueba", "/");
    }
}
