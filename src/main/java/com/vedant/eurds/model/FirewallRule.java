package com.vedant.eurds.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "firewall_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirewallRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 10)
    private String ruleType; // ALLOW or DENY

    @Column(name = "source_ip", length = 50)
    private String sourceIp; // nullable = wildcard (matches all IPs)

    @Column(name = "destination_port")
    private Integer destinationPort; // nullable = all ports

    @Column(name = "protocol", length = 10)
    private String protocol = "ANY"; // TCP, UDP, ICMP, ANY

    @Column(name = "priority", nullable = false)
    private Integer priority; // lower number = evaluated first

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}