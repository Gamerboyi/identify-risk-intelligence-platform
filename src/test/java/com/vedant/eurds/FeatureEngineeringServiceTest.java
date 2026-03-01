package com.vedant.eurds;

import com.vedant.eurds.dto.LoginFeatureDTO;
import com.vedant.eurds.model.User;
import com.vedant.eurds.repository.LoginLogRepository;
import com.vedant.eurds.repository.UserRepository;
import com.vedant.eurds.service.FeatureEngineeringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureEngineeringServiceTest {

    @Mock private LoginLogRepository loginLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private FeatureEngineeringService featureEngineeringService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@test.com")
                .passwordHash("hashed")
                .accountLocked(false)
                .failedAttemptCount(0)
                .build();
    }

    // ============================================================
    // FEATURE EXTRACTION TESTS
    // ============================================================

    @Test
    void extractFeatures_NewIpAndDevice_FlagsAsNew() {
        // Arrange - empty history means new IP and device
        when(loginLogRepository.countRecentLogins(any(), any())).thenReturn(0L);
        when(loginLogRepository.findDistinctIpsByUserId(any())).thenReturn(List.of());
        when(loginLogRepository.findDistinctDevicesByUserId(any())).thenReturn(List.of());
        when(loginLogRepository.countRecentFailedLogins(any(), any())).thenReturn(0L);

        // Act
        LoginFeatureDTO features = featureEngineeringService.extractFeatures(
                testUser, "192.168.1.99", "Chrome/NewDevice");

        // Assert
        assertEquals(1, features.getIsNewIp());
        assertEquals(1, features.getIsNewDevice());
    }

    @Test
    void extractFeatures_KnownIpAndDevice_FlagsAsNotNew() {
        // Arrange - IP and device already in history
        when(loginLogRepository.countRecentLogins(any(), any())).thenReturn(3L);
        when(loginLogRepository.findDistinctIpsByUserId(any()))
                .thenReturn(List.of("192.168.1.10"));
        when(loginLogRepository.findDistinctDevicesByUserId(any()))
                .thenReturn(List.of("Mozilla/5.0 Chrome"));
        when(loginLogRepository.countRecentFailedLogins(any(), any())).thenReturn(0L);

        // Act
        LoginFeatureDTO features = featureEngineeringService.extractFeatures(
                testUser, "192.168.1.10", "Mozilla/5.0 Chrome");

        // Assert
        assertEquals(0, features.getIsNewIp());
        assertEquals(0, features.getIsNewDevice());
    }

    // ============================================================
    // RISK SCORE TESTS
    // ============================================================

    @Test
    void calculateRuleBasedRisk_NewIpAndDevice_ReturnsMediumRisk() {
        LoginFeatureDTO features = LoginFeatureDTO.builder()
                .isNewIp(1)
                .isNewDevice(1)
                .hourOfDay(14)
                .failedAttemptRatio(0.0)
                .loginFrequency24h(1)
                .totalUniqueIps(1)
                .isWeekend(0)
                .build();

        int score = featureEngineeringService.calculateRuleBasedRisk(features);

        assertEquals(45, score); // 25 + 20
        assertEquals("MEDIUM", featureEngineeringService.getRiskLevel(score));
    }

    @Test
    void calculateRuleBasedRisk_AllSuspicious_ReturnsHighRisk() {
        LoginFeatureDTO features = LoginFeatureDTO.builder()
                .isNewIp(1)
                .isNewDevice(1)
                .hourOfDay(2)
                .failedAttemptRatio(0.8)
                .loginFrequency24h(15)
                .totalUniqueIps(8)
                .isWeekend(0)
                .build();

        int score = featureEngineeringService.calculateRuleBasedRisk(features);

        assertTrue(score >= 70);
        assertEquals("HIGH", featureEngineeringService.getRiskLevel(score));
    }

    @Test
    void calculateRuleBasedRisk_NormalBehavior_ReturnsLowRisk() {
        LoginFeatureDTO features = LoginFeatureDTO.builder()
                .isNewIp(0)
                .isNewDevice(0)
                .hourOfDay(14)
                .failedAttemptRatio(0.0)
                .loginFrequency24h(2)
                .totalUniqueIps(1)
                .isWeekend(0)
                .build();

        int score = featureEngineeringService.calculateRuleBasedRisk(features);

        assertEquals(0, score);
        assertEquals("LOW", featureEngineeringService.getRiskLevel(score));
    }

    @Test
    void getRiskLevel_Boundaries() {
        assertEquals("LOW", featureEngineeringService.getRiskLevel(0));
        assertEquals("LOW", featureEngineeringService.getRiskLevel(29));
        assertEquals("MEDIUM", featureEngineeringService.getRiskLevel(30));
        assertEquals("MEDIUM", featureEngineeringService.getRiskLevel(69));
        assertEquals("HIGH", featureEngineeringService.getRiskLevel(70));
        assertEquals("HIGH", featureEngineeringService.getRiskLevel(100));
    }
}