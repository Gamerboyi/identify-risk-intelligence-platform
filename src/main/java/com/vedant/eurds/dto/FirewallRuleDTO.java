package com.vedant.eurds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirewallRuleDTO {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    @NotBlank(message = "Rule type is required")
    private String ruleType;

    private String sourceIp;

    private Integer destinationPort;

    @Builder.Default
    private String protocol = "ANY";

    @NotNull(message = "Priority is required")
    private Integer priority;

    @Builder.Default
    private boolean active = true;
}