package com.vedant.eurds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginFeatureDTO {

    private String userId;
    private String username;
    private String ipAddress;
    private String deviceInfo;

    // ML Features -- all numbers
    private int loginFrequency24h;
    private int isNewIp;        // 1 = new IP, 0 = seen before
    private int isNewDevice;    // 1 = new device, 0 = seen before
    private double failedAttemptRatio;
    private int hourOfDay;      // 0-23
    private int isWeekend;      // 1 = weekend, 0 = weekday
    private int totalUniqueIps; // how many different IPs this user has used
}