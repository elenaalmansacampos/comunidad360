package com.comunidad360.user;

import com.comunidad360.security.AuthenticatedUser;
import com.comunidad360.user.dto.UpdateLanguageRequest;
import com.comunidad360.user.dto.UpdateProfileRequest;
import com.comunidad360.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return userService.profile(user.id());
    }

    @PatchMapping
    public UserResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.id(), request);
    }

    @PutMapping("/language")
    public UserResponse language(@AuthenticationPrincipal AuthenticatedUser user,
                                 @Valid @RequestBody UpdateLanguageRequest request) {
        return userService.updateLanguage(user.id(), request);
    }
}
