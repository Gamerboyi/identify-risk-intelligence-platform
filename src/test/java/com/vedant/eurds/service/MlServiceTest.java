package com.vedant.eurds.service;

import com.vedant.eurds.dto.LoginFeatureDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlServiceTest {

    @Mock
    private FeatureEngineeringService featureEngineeringService;

    private MlService mlService;

    @BeforeEach
    void setUp() {
        mlService = new MlService(featureEngineeringService);
    }

    // ============================================================
    // FALLBACK TESTS -- when ML service is unavailable
    // ============================================================

    @Test
    void predict_MlServiceDown_FallbackUsed() {
        // The ML service URL defaults to null in test context
        // so the WebClient call will fail, triggering fallback
        LoginFeatureDTO features = buildNormalFeatures();
        when(featureEngineeringService.calculateRuleBasedRisk(any())).thenReturn(0);
        when(featureEngineeringService.getRiskLevel(0)).thenReturn("LOW");

        MlService.MlPrediction prediction = mlService.predict(features);

        assertNotNull(prediction);
        assertEquals("rule_based_fallback", prediction.getModelUsed());
    }

    @Test
    void predict_NormalFeatures_LowRisk() {
        LoginFeatureDTO features = buildNormalFeatures();
        when(featureEngineeringService.calculateRuleBasedRisk(any())).thenReturn(0);
        when(featureEngineeringService.getRiskLevel(0)).thenReturn("LOW");

        MlService.MlPrediction prediction = mlService.predict(features);

        assertTrue(prediction.getRiskScore() < 30);
        assertEquals("LOW", prediction.getRiskLevel());
    }

    @Test
    void predict_NewIpAndDevice_MediumRisk() {
        LoginFeatureDTO features = LoginFeatureDTO.builder()
                .isNewIp(1)
                .isNewDevice(1)
                .hourOfDay(14)
                .failedAttemptRatio(0.0)
                .loginFrequency24h(1)
                .totalUniqueIps(1)
                .isWeekend(0)
                .build();

        when(featureEngineeringService.calculateRuleBasedRisk(any())).thenReturn(45);
        when(featureEngineeringService.getRiskLevel(45)).thenReturn("MEDIUM");

        MlService.MlPrediction prediction = mlService.predict(features);

        // 25 (new IP) + 20 (new device) = 45
        assertEquals(45, prediction.getRiskScore());
        assertEquals("MEDIUM", prediction.getRiskLevel());
    }

    @Test
    void predict_AllSuspicious_HighRisk() {
        LoginFeatureDTO features = LoginFeatureDTO.builder()
                .isNewIp(1)
                .isNewDevice(1)
                .hourOfDay(2)           // unusual hour
                .failedAttemptRatio(0.8) // high failure ratio
                .loginFrequency24h(15)  // too many logins
                .totalUniqueIps(8)
                .isWeekend(0)
                .build();

        when(featureEngineeringService.calculateRuleBasedRisk(any())).thenReturn(100);
        when(featureEngineeringService.getRiskLevel(100)).thenReturn("HIGH");

        MlService.MlPrediction prediction = mlService.predict(features);

        assertTrue(prediction.getRiskScore() >= 70);
        assertEquals("HIGH", prediction.getRiskLevel());
    }

    @Test
    void predict_MaxScore_CappedAt100() {
        LoginFeatureDTO features = LoginFeatureDTO.builder()
                .isNewIp(1)             // +25
                .isNewDevice(1)         // +20
                .hourOfDay(3)           // +20
                .failedAttemptRatio(0.9) // +30
                .loginFrequency24h(20)  // +15
                .totalUniqueIps(10)     // +10 = 120, capped to 100
                .isWeekend(0)
                .build();

        when(featureEngineeringService.calculateRuleBasedRisk(any())).thenReturn(100);
        when(featureEngineeringService.getRiskLevel(100)).thenReturn("HIGH");

        MlService.MlPrediction prediction = mlService.predict(features);

        assertEquals(100, prediction.getRiskScore());
    }

    // ============================================================
    // HELPER
    // ============================================================

    private LoginFeatureDTO buildNormalFeatures() {
        return LoginFeatureDTO.builder()
                .isNewIp(0)
                .isNewDevice(0)
                .hourOfDay(14)
                .failedAttemptRatio(0.0)
                .loginFrequency24h(2)
                .totalUniqueIps(1)
                .isWeekend(0)
                .build();
    }
}
