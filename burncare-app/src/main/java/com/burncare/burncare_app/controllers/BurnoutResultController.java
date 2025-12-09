package com.burncare.burncare_app.controllers;// package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.BurnoutResultRequest;
import com.burncare.burncare_app.dto.BurnoutResultResponse;
import com.burncare.burncare_app.services.BurnoutResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/burnout-results")
@RequiredArgsConstructor
public class BurnoutResultController {

    private final BurnoutResultService burnoutResultService;

    @PostMapping
    public ResponseEntity<BurnoutResultResponse> save(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BurnoutResultRequest request
    ) {
        String keycloakId = jwt.getSubject(); // "sub"
        BurnoutResultResponse response = burnoutResultService.saveForUser(keycloakId, request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/me")
    public ResponseEntity<List<BurnoutResultResponse>> getMyResults(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String keycloakId = jwt.getSubject();
        List<BurnoutResultResponse> results = burnoutResultService.getResultsForUser(keycloakId);
        return ResponseEntity.ok(results);
    }
}
