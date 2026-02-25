package com.vedant.eurds.service;

import com.vedant.eurds.model.AuditEvent;
import com.vedant.eurds.repository.AuditEventRepository;
import com.vedant.eurds.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdsDetectionService {

    private final LoginLogRepository loginLogRepository;
    private final AuditEventRepository auditEventRepository;

    // In-memory request counter per IP -- tracks requests in last 60 seconds
    // ConcurrentHashMap is thread-safe -- multiple requests can hit this simultaneously
    private final ConcurrentHashMap<String, List<Long>> requestTimestamps = new ConcurrentHashMap<>();

    // Rate limit -- max requests per IP per minute
    private static final int MAX_REQUESTS_PER_MINUTE = 30;

    // Known malicious patterns in request paths
    private static final List<String> MALICIOUS_PATTERNS = List.of(
            "../",          // Directory traversal attack
            "<script",      // XSS attack
            "SELECT ",      // SQL injection
            "DROP TABLE",   // SQL injection
            "UNION SELECT", // SQL injection
            "/etc/passwd",  // Linux file access attempt
            "cmd.exe",      // Windows command injection
            "eval(",        // Code injection
            "base64_decode" // PHP injection
    );

    // Cleanup stale IPs every 100 calls to prevent memory leak
    private final AtomicInteger callCounter = new AtomicInteger(0);

    // ============================================================
    // RATE LIMITING -- detect too many requests from one IP
    // ============================================================
    public boolean isRateLimited(String ipAddress) {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60000;

        // Periodically clean up IPs with no recent activity to prevent memory leak
        if (callCounter.incrementAndGet() % 100 == 0) {
            cleanupStaleEntries(oneMinuteAgo);
        }

        // computeIfAbsent is atomic -- no race condition
        List<Long> timestamps = requestTimestamps.computeIfAbsent(
                ipAddress, k -> new CopyOnWriteArrayList<>());

        // Remove timestamps older than 1 minute and add current
        synchronized (timestamps) {
            timestamps.removeIf(t -> t < oneMinuteAgo);
            timestamps.add(now);

            if (timestamps.size() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} ({} requests/min)", ipAddress, timestamps.size());
                logThreatEvent("RATE_LIMIT_EXCEEDED", ipAddress,
                        Map.of("requestCount", timestamps.size(), "ip", ipAddress));
                return true; // is rate limited
            }
        }
        return false;
    }

    // Remove IPs with no recent activity to prevent unbounded memory growth
    private void cleanupStaleEntries(long cutoff) {
        requestTimestamps.entrySet().removeIf(entry -> {
            List<Long> timestamps = entry.getValue();
            timestamps.removeIf(t -> t < cutoff);
            return timestamps.isEmpty();
        });
    }

    // ============================================================
    // PATTERN DETECTION -- detect known attack signatures
    // ============================================================
    public boolean containsMaliciousPattern(String requestUri, String requestBody) {
        String combined = (requestUri + " " + (requestBody != null ? requestBody : "")).toLowerCase();

        for (String pattern : MALICIOUS_PATTERNS) {
            if (combined.contains(pattern.toLowerCase())) {
                log.warn("Malicious pattern detected: {} in request: {}", pattern, requestUri);
                logThreatEvent("MALICIOUS_PATTERN_DETECTED", null,
                        Map.of("pattern", pattern, "uri", requestUri));
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // BRUTE FORCE DETECTION -- too many failed logins from one IP
    // ============================================================
    public boolean isBruteForceAttempt(String ipAddress) {
        LocalDateTime fifteenMinutesAgo = LocalDateTime.now().minusMinutes(15);

        // Count failed logins from this IP in last 15 minutes using efficient DB query
        long failedAttempts = loginLogRepository
                .countFailedLoginsByIpSince(ipAddress, fifteenMinutesAgo);

        if (failedAttempts >= 10) {
            log.warn("Brute force detected from IP: {} ({} failed attempts)", ipAddress, failedAttempts);
            logThreatEvent("BRUTE_FORCE_DETECTED", null,
                    Map.of("ip", ipAddress, "failedAttempts", failedAttempts));
            return true;
        }
        return false;
    }

    // ============================================================
    // FULL THREAT ANALYSIS -- run all checks at once
    // ============================================================
    public ThreatAnalysis analyzeThreat(String ipAddress, String requestUri, String requestBody) {
        List<String> threats = new ArrayList<>();

        if (isRateLimited(ipAddress)) {
            threats.add("RATE_LIMIT_EXCEEDED");
        }
        if (containsMaliciousPattern(requestUri, requestBody)) {
            threats.add("MALICIOUS_PATTERN");
        }
        if (isBruteForceAttempt(ipAddress)) {
            threats.add("BRUTE_FORCE");
        }

        boolean isThreat = !threats.isEmpty();
        String threatLevel = threats.isEmpty() ? "NONE" : threats.size() >= 2 ? "HIGH" : "MEDIUM";

        return new ThreatAnalysis(isThreat, threatLevel, threats);
    }

    // ============================================================
    // THREAT ANALYSIS RESULT CLASS
    // ============================================================
    public record ThreatAnalysis(boolean isThreat, String threatLevel, List<String> detectedThreats) {}

    // ============================================================
    // PRIVATE HELPER
    // ============================================================
    private void logThreatEvent(String eventType, String ipAddress, Map<String, Object> data) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .eventData(data)
                .build();
        auditEventRepository.save(event);
    }
}