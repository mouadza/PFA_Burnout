package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.AuthResponse;
import com.burncare.burncare_app.dto.ChangePasswordRequest;
import com.burncare.burncare_app.dto.UpdateProfileRequest;
import com.burncare.burncare_app.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user") // ✅ Route dédiée aux utilisateurs
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Endpoint: PUT /api/user/profile
    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(@RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(req));
    }

    // Endpoint: PUT /api/user/password
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        userService.changePassword(req);
        return ResponseEntity.ok().build();
    }
}