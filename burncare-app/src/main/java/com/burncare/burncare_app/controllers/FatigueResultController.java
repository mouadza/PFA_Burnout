package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.FatigueResultRequest;
import com.burncare.burncare_app.dto.FatigueResultResponse;
import com.burncare.burncare_app.services.FatigueResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fatigue-results")
@RequiredArgsConstructor
public class FatigueResultController {

    private final FatigueResultService fatigueResultService;

    @PostMapping
    public ResponseEntity<FatigueResultResponse> save(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody FatigueResultRequest request
    ) {
        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(fatigueResultService.saveForUser(keycloakId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<FatigueResultResponse>> myResults(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(fatigueResultService.getResultsForUser(keycloakId));
    }
}
