package com.vedant.eurds;

import com.vedant.eurds.repository.AuditEventRepository;
import com.vedant.eurds.repository.LoginLogRepository;
import com.vedant.eurds.service.IdsDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdsDetectionServiceTest {

    @Mock private LoginLogRepository loginLogRepository;
    @Mock private AuditEventRepository auditEventRepository;

    @InjectMocks
    private IdsDetectionService idsDetectionService;

    // ============================================================
    // RATE LIMITING TESTS
    // ============================================================

    @Test
    void isRateLimited_UnderLimit_ReturnsFalse() {
        // Act -- single call should be well under 30/min
        boolean result = idsDetectionService.isRateLimited("192.168.1.1");

        // Assert
        assertFalse(result);
    }

    @Test
    void isRateLimited_OverLimit_ReturnsTrue() {
        String ip = "10.0.0.1";

        // Act -- hit the rate limiter 31 times
        boolean result = false;
        for (int i = 0; i < 31; i++) {
            result = idsDetectionService.isRateLimited(ip);
        }

        // Assert -- the 31st call should be rate limited
        assertTrue(result);
    }

    @Test
    void isRateLimited_DifferentIps_AreIndependent() {
        // Act -- hit from two different IPs, 20 times each
        for (int i = 0; i < 20; i++) {
            idsDetectionService.isRateLimited("192.168.1.1");
            idsDetectionService.isRateLimited("192.168.1.2");
        }

        // Assert -- neither should be rate limited (both under 30)
        assertFalse(idsDetectionService.isRateLimited("192.168.1.1"));
        assertFalse(idsDetectionService.isRateLimited("192.168.1.2"));
    }

    // ============================================================
    // MALICIOUS PATTERN TESTS
    // ============================================================

    @Test
    void containsMaliciousPattern_SqlInjection_Detected() {
        assertTrue(idsDetectionService.containsMaliciousPattern(
                "/api/users?q=SELECT * FROM users", null));
    }

    @Test
    void containsMaliciousPattern_XssAttack_Detected() {
        assertTrue(idsDetectionService.containsMaliciousPattern(
                "/api/data", "<script>alert('xss')</script>"));
    }

    @Test
    void containsMaliciousPattern_DirectoryTraversal_Detected() {
        assertTrue(idsDetectionService.containsMaliciousPattern(
                "/api/files/../../../etc/passwd", null));
    }

    @Test
    void containsMaliciousPattern_UnionSelect_Detected() {
        assertTrue(idsDetectionService.containsMaliciousPattern(
                "/api/users?id=1 UNION SELECT * FROM users", null));
    }

    @Test
    void containsMaliciousPattern_CleanRequest_NotDetected() {
        assertFalse(idsDetectionService.containsMaliciousPattern(
                "/api/auth/login", "{\"username\":\"test\",\"password\":\"pass\"}"));
    }

    @Test
    void containsMaliciousPattern_CommandInjection_Detected() {
        assertTrue(idsDetectionService.containsMaliciousPattern(
                "/api/exec", "cmd.exe /c dir"));
    }

    // ============================================================
    // BRUTE FORCE TESTS
    // ============================================================

    @Test
    void isBruteForceAttempt_UnderThreshold_ReturnsFalse() {
        when(loginLogRepository.countFailedLoginsByIpSince(anyString(), any()))
                .thenReturn(5L);

        assertFalse(idsDetectionService.isBruteForceAttempt("192.168.1.1"));
    }

    @Test
    void isBruteForceAttempt_OverThreshold_ReturnsTrue() {
        when(loginLogRepository.countFailedLoginsByIpSince(anyString(), any()))
                .thenReturn(15L);

        assertTrue(idsDetectionService.isBruteForceAttempt("192.168.1.1"));
    }

    @Test
    void isBruteForceAttempt_ExactThreshold_ReturnsTrue() {
        when(loginLogRepository.countFailedLoginsByIpSince(anyString(), any()))
                .thenReturn(10L);

        assertTrue(idsDetectionService.isBruteForceAttempt("192.168.1.1"));
    }

    // ============================================================
    // COMPOSITE ANALYSIS TESTS
    // ============================================================

    @Test
    void analyzeThreat_CleanRequest_NoThreats() {
        when(loginLogRepository.countFailedLoginsByIpSince(anyString(), any()))
                .thenReturn(0L);

        IdsDetectionService.ThreatAnalysis analysis =
                idsDetectionService.analyzeThreat("192.168.1.1", "/api/auth/login", null);

        assertFalse(analysis.isThreat());
        assertEquals("NONE", analysis.threatLevel());
        assertTrue(analysis.detectedThreats().isEmpty());
    }

    @Test
    void analyzeThreat_MultipleThreats_HighLevel() {
        // Brute force detected
        when(loginLogRepository.countFailedLoginsByIpSince(anyString(), any()))
                .thenReturn(20L);

        // Malicious pattern in URI
        IdsDetectionService.ThreatAnalysis analysis =
                idsDetectionService.analyzeThreat("192.168.1.1", "/api/../etc/passwd", null);

        assertTrue(analysis.isThreat());
        assertEquals("HIGH", analysis.threatLevel());
        assertTrue(analysis.detectedThreats().size() >= 2);
    }
}
