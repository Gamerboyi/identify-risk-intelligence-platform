package com.vedant.eurds.controller;

import com.vedant.eurds.dto.FirewallRuleDTO;
import com.vedant.eurds.model.FirewallRule;
import com.vedant.eurds.service.FirewallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.vedant.eurds.repository.UserRepository;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/firewall")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Firewall", description = "Firewall rule management endpoints")
public class FirewallController {

    private final FirewallService firewallService;
    private final UserRepository userRepository;

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a new firewall rule")
    public ResponseEntity<FirewallRule> addRule(
            @Valid @RequestBody FirewallRuleDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        FirewallRule rule = firewallService.addRule(dto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get all firewall rules")
    public ResponseEntity<List<FirewallRule>> getAllRules() {
        return ResponseEntity.ok(firewallService.getAllRules());
    }

    @GetMapping("/rules/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get only active firewall rules")
    public ResponseEntity<List<FirewallRule>> getActiveRules() {
        return ResponseEntity.ok(firewallService.getActiveRules());
    }

    @PutMapping("/rules/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable a firewall rule")
    public ResponseEntity<FirewallRule> toggleRule(
            @PathVariable Integer id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(firewallService.toggleRule(id, active, user.getId()));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a firewall rule")
    public ResponseEntity<Map<String, String>> deleteRule(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        firewallService.deleteRule(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "Rule deleted successfully"));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Evaluate traffic against firewall rules")
    public ResponseEntity<Map<String, String>> evaluateTraffic(
            @RequestParam String sourceIp,
            @RequestParam Integer port,
            @RequestParam String protocol) {
        String result = firewallService.evaluateTraffic(sourceIp, port, protocol);
        return ResponseEntity.ok(Map.of(
                "sourceIp", sourceIp,
                "port", String.valueOf(port),
                "protocol", protocol,
                "decision", result
        ));
    }
}