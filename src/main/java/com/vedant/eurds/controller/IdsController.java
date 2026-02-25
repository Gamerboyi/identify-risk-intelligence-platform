package com.vedant.eurds.controller;

import com.vedant.eurds.service.IdsDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ids")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "IDS", description = "Intrusion Detection System endpoints")
public class IdsController {

    private final IdsDetectionService idsDetectionService;

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Analyze a request for threats")
    public ResponseEntity<IdsDetectionService.ThreatAnalysis> analyzeThreat(
            @RequestParam String ipAddress,
            @RequestParam String requestUri,
            @RequestParam(required = false) String requestBody) {
        IdsDetectionService.ThreatAnalysis analysis =
                idsDetectionService.analyzeThreat(ipAddress, requestUri, requestBody);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/check/ratelimit")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Check if an IP is rate limited")
    public ResponseEntity<Map<String, Object>> checkRateLimit(@RequestParam String ipAddress) {
        boolean limited = idsDetectionService.isRateLimited(ipAddress);
        return ResponseEntity.ok(Map.of(
                "ipAddress", ipAddress,
                "isRateLimited", limited,
                "message", limited ? "IP is rate limited" : "IP is within limits"
        ));
    }

    @GetMapping("/check/bruteforce")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Check if an IP is performing brute force attack")
    public ResponseEntity<Map<String, Object>> checkBruteForce(@RequestParam String ipAddress) {
        boolean bruteForce = idsDetectionService.isBruteForceAttempt(ipAddress);
        return ResponseEntity.ok(Map.of(
                "ipAddress", ipAddress,
                "isBruteForce", bruteForce,
                "message", bruteForce ? "Brute force detected from this IP" : "No brute force detected"
        ));
    }

    @PostMapping("/check/pattern")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Check if a request contains malicious patterns")
    public ResponseEntity<Map<String, Object>> checkPattern(
            @RequestParam String requestUri,
            @RequestParam(required = false) String requestBody) {
        boolean malicious = idsDetectionService.containsMaliciousPattern(requestUri, requestBody);
        return ResponseEntity.ok(Map.of(
                "requestUri", requestUri,
                "isMalicious", malicious,
                "message", malicious ? "Malicious pattern detected" : "No malicious patterns found"
        ));
    }
}