package com.vedant.eurds.service;

import com.vedant.eurds.dto.LoginFeatureDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class MlService {

    private final WebClient webClient;
    private final FeatureEngineeringService featureEngineeringService;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    public MlService(FeatureEngineeringService featureEngineeringService) {
        this.featureEngineeringService = featureEngineeringService;
        this.webClient = WebClient.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    // ============================================================
    // CALL ML SERVICE FOR RISK PREDICTION
    // ============================================================
    public MlPrediction predict(LoginFeatureDTO features) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "loginFrequency24h", features.getLoginFrequency24h(),
                    "isNewIp", features.getIsNewIp(),
                    "isNewDevice", features.getIsNewDevice(),
                    "failedAttemptRatio", features.getFailedAttemptRatio(),
                    "hourOfDay", features.getHourOfDay(),
                    "isWeekend", features.getIsWeekend(),
                    "totalUniqueIps", features.getTotalUniqueIps()
            );

            MlPrediction prediction = webClient.post()
                    .uri(mlServiceUrl + "/predict")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(MlPrediction.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            log.info("ML prediction: score={}, level={}, model={}",
                    prediction.getRiskScore(),
                    prediction.getRiskLevel(),
                    prediction.getModelUsed());

            return prediction;

        } catch (WebClientRequestException e) {
            log.warn("ML service unavailable, using rule-based fallback: {}", e.getMessage());
            return fallbackPrediction(features);
        } catch (Exception e) {
            log.error("ML prediction failed, using fallback: {}", e.getMessage());
            return fallbackPrediction(features);
        }
    }

    // ============================================================
    // FALLBACK -- delegates to FeatureEngineeringService (single source of truth)
    // ============================================================
    private MlPrediction fallbackPrediction(LoginFeatureDTO features) {
        int score = featureEngineeringService.calculateRuleBasedRisk(features);
        String level = featureEngineeringService.getRiskLevel(score);
        return new MlPrediction(score, level, "rule_based_fallback");
    }

    // ============================================================
    // PREDICTION RESULT CLASS
    // ============================================================
    public static class MlPrediction {
        private int riskScore;
        private String riskLevel;
        private String modelUsed;

        public MlPrediction() {}

        public MlPrediction(int riskScore, String riskLevel, String modelUsed) {
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.modelUsed = modelUsed;
        }

        public int getRiskScore() { return riskScore; }
        public String getRiskLevel() { return riskLevel; }
        public String getModelUsed() { return modelUsed; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    }
}