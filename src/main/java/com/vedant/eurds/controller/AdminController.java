package com.vedant.eurds.controller;

import com.vedant.eurds.model.AuditEvent;
import com.vedant.eurds.model.LoginLog;
import com.vedant.eurds.model.User;
import com.vedant.eurds.repository.AuditEventRepository;
import com.vedant.eurds.repository.LoginLogRepository;
import com.vedant.eurds.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin-only dashboard endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final LoginLogRepository loginLogRepository;

    @GetMapping("/users")
    @Operation(summary = "List all users with their status")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("email", u.getEmail());
            map.put("accountLocked", u.isAccountLocked());
            map.put("failedAttemptCount", u.getFailedAttemptCount());
            map.put("roles", u.getRoles().stream().map(r -> r.getRoleName()).toList());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/users/{id}/unlock")
    @Operation(summary = "Unlock a locked user account")
    public ResponseEntity<Map<String, String>> unlockUser(@PathVariable UUID id) {
        userRepository.unlockAccount(id);
        log.info("Admin unlocked user account: {}", id);
        return ResponseEntity.ok(Map.of("message", "Account unlocked successfully", "userId", id.toString()));
    }

    @GetMapping("/audit-events")
    @Operation(summary = "Get latest 50 audit events")
    public ResponseEntity<List<AuditEvent>> getRecentAuditEvents() {
        return ResponseEntity.ok(auditEventRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @GetMapping("/login-logs")
    @Operation(summary = "Get latest 50 login logs")
    public ResponseEntity<List<LoginLog>> getRecentLoginLogs() {
        return ResponseEntity.ok(loginLogRepository.findTop50ByOrderByLoginTimestampDesc());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get aggregated dashboard statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long totalUsers = userRepository.count();
        long lockedUsers = userRepository.countByAccountLockedTrue();
        long totalLogins = loginLogRepository.count();
        long totalAuditEvents = auditEventRepository.count();

        // Calculate average risk score from recent login logs
        List<LoginLog> recentLogs = loginLogRepository.findTop50ByOrderByLoginTimestampDesc();
        double avgRiskScore = recentLogs.stream()
                .filter(l -> l.getRiskScore() != null)
                .mapToInt(LoginLog::getRiskScore)
                .average()
                .orElse(0.0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("lockedUsers", lockedUsers);
        stats.put("totalLogins", totalLogins);
        stats.put("totalAuditEvents", totalAuditEvents);
        stats.put("avgRiskScore", Math.round(avgRiskScore * 10.0) / 10.0);
        return ResponseEntity.ok(stats);
    }
}
