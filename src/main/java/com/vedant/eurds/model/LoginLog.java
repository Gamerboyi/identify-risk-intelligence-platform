package com.vedant.eurds.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "login_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "login_timestamp")
    private LocalDateTime loginTimestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "success_flag", nullable = false)
    private boolean successFlag;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "geo_flag")
    private boolean geoFlag = false;

    @PrePersist
    protected void onCreate() {
        loginTimestamp = LocalDateTime.now();
    }
}