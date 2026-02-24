package com.vedant.eurds.service;

import com.vedant.eurds.dto.LoginFeatureDTO;
import com.vedant.eurds.model.User;
import com.vedant.eurds.repository.LoginLogRepository;
import com.vedant.eurds.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureEngineeringService {

    private final LoginLogRepository loginLogRepository;
    private final UserRepository userRepository;

    // ============================================================
    // MAIN METHOD -- extract all features for a login attempt
    // ============================================================
    public LoginFeatureDTO extractFeatures(User user, String ipAddress, String deviceInfo) {

        String userId = user.getId().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusHours(24);

        // Feature 1: How many times has this user logged in last 24 hours?
        long loginFrequency = loginLogRepository.countRecentLogins(user.getId(), yesterday);

        // Feature 2: Is this a new IP address for this user?
        List<String> knownIps = loginLogRepository.findDistinctIpsByUserId(user.getId());
        int isNewIp = knownIps.contains(ipAddress) ? 0 : 1;

        // Feature 3: Is this a new device for this user?
        List<String> knownDevices = loginLogRepository.findDistinctDevicesByUserId(user.getId());
        int isNewDevice = knownDevices.contains(deviceInfo) ? 0 : 1;

        // Feature 4: What ratio of recent logins were failures?
        long recentLogins = loginLogRepository.countRecentLogins(user.getId(), yesterday);
        long recentFailures = loginLogRepository.countRecentFailedLogins(user.getId(), yesterday);

        double failedAttemptRatio = recentLogins > 0
                ? (double) recentFailures / recentLogins
                : 0.0;

        // Feature 5: What hour of day is this login? (0-23)
        int hourOfDay = now.getHour();

        // Feature 6: Is it the weekend?
        int isWeekend = (now.getDayOfWeek().getValue() >= 6) ? 1 : 0;

        // Feature 7: How many unique IPs has this user ever used?
        int totalUniqueIps = knownIps.size();

        log.info("Features extracted for user {}: freq={}, newIp={}, newDevice={}, failRatio={}, hour={}, weekend={}, uniqueIps={}",
                user.getUsername(), loginFrequency, isNewIp, isNewDevice,
                failedAttemptRatio, hourOfDay, isWeekend, totalUniqueIps);

        return LoginFeatureDTO.builder()
                .userId(userId)
                .username(user.getUsername())
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .loginFrequency24h((int) loginFrequency)
                .isNewIp(isNewIp)
                .isNewDevice(isNewDevice)
                .failedAttemptRatio(failedAttemptRatio)
                .hourOfDay(hourOfDay)
                .isWeekend(isWeekend)
                .totalUniqueIps(totalUniqueIps)
                .build();
    }

    // ============================================================
    // RISK RULE ENGINE -- calculate risk score without ML
    // Used as fallback when ML service is down
    // ============================================================
    public int calculateRuleBasedRisk(LoginFeatureDTO features) {
        int riskScore = 0;

        // New IP = +25 risk
        if (features.getIsNewIp() == 1) riskScore += 25;

        // New device = +20 risk
        if (features.getIsNewDevice() == 1) riskScore += 20;

        // Login at unusual hour (midnight to 5am) = +20 risk
        if (features.getHourOfDay() >= 0 && features.getHourOfDay() <= 5) riskScore += 20;

        // High failed attempt ratio = +30 risk
        if (features.getFailedAttemptRatio() > 0.5) riskScore += 30;

        // Too many logins in 24 hours = +15 risk
        if (features.getLoginFrequency24h() > 10) riskScore += 15;

        // Using many different IPs = +10 risk (suspicious pattern)
        if (features.getTotalUniqueIps() > 5) riskScore += 10;

        // Cap at 100
        return Math.min(riskScore, 100);
    }

    // ============================================================
    // RISK LEVEL -- convert score to label
    // ============================================================
    public String getRiskLevel(int riskScore) {
        if (riskScore >= 70) return "HIGH";
        if (riskScore >= 30) return "MEDIUM";
        return "LOW";
    }
}