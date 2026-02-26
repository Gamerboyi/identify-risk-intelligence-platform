package com.vedant.eurds.service;

import com.vedant.eurds.dto.FirewallRuleDTO;
import com.vedant.eurds.model.AuditEvent;
import com.vedant.eurds.model.FirewallRule;
import com.vedant.eurds.repository.AuditEventRepository;
import com.vedant.eurds.repository.FirewallRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirewallService {

    private final FirewallRuleRepository firewallRuleRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public FirewallRule addRule(FirewallRuleDTO dto, UUID createdBy) {
        if (firewallRuleRepository.existsByRuleName(dto.getRuleName())) {
            throw new RuntimeException("Rule already exists: " + dto.getRuleName());
        }
        FirewallRule rule = FirewallRule.builder()
                .ruleName(dto.getRuleName())
                .ruleType(dto.getRuleType().toUpperCase())
                .sourceIp(dto.getSourceIp())
                .destinationPort(dto.getDestinationPort())
                .protocol(dto.getProtocol() != null ? dto.getProtocol().toUpperCase() : "ANY")
                .priority(dto.getPriority())
                .active(dto.isActive())
                .createdBy(createdBy)
                .build();
        FirewallRule saved = firewallRuleRepository.save(rule);
        logAudit("FIREWALL_RULE_ADDED", createdBy, Map.of("ruleName", saved.getRuleName(), "ruleType", saved.getRuleType()));
        log.info("Firewall rule added: {} [{}]", saved.getRuleName(), saved.getRuleType());
        return saved;
    }

    public List<FirewallRule> getAllRules() {
        return firewallRuleRepository.findAllByOrderByPriorityAsc();
    }

    public List<FirewallRule> getActiveRules() {
        return firewallRuleRepository.findByActiveTrueOrderByPriorityAsc();
    }

    @Transactional
    public FirewallRule toggleRule(Integer ruleId, boolean active, UUID updatedBy) {
        FirewallRule rule = firewallRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
        rule.setActive(active);
        FirewallRule updated = firewallRuleRepository.save(rule);
        String eventType = active ? "FIREWALL_RULE_ENABLED" : "FIREWALL_RULE_DISABLED";
        logAudit(eventType, updatedBy, Map.of("ruleName", rule.getRuleName()));
        return updated;
    }

    @Transactional
    public void deleteRule(Integer ruleId, UUID deletedBy) {
        FirewallRule rule = firewallRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
        firewallRuleRepository.delete(rule);
        logAudit("FIREWALL_RULE_DELETED", deletedBy, Map.of("ruleName", rule.getRuleName()));
    }

    public String evaluateTraffic(String sourceIp, Integer port, String protocol) {
        List<FirewallRule> activeRules = firewallRuleRepository.findByActiveTrueOrderByPriorityAsc();
        for (FirewallRule rule : activeRules) {
            boolean ipMatch = rule.getSourceIp() == null || matchesCidr(sourceIp, rule.getSourceIp());
            boolean portMatch = rule.getDestinationPort() == null || rule.getDestinationPort().equals(port);
            boolean protocolMatch = rule.getProtocol().equals("ANY") || rule.getProtocol().equalsIgnoreCase(protocol);
            if (ipMatch && portMatch && protocolMatch) {
                log.info("Traffic from {} port {} matched rule: {} [{}]", sourceIp, port, rule.getRuleName(), rule.getRuleType());
                return rule.getRuleType();
            }
        }
        return "ALLOW";
    }

    /**
     * Checks if a traffic IP matches a rule's IP, supporting both exact IPs
     * and CIDR notation (e.g. "192.168.1.0/24").
     */
    private boolean matchesCidr(String trafficIp, String ruleIp) {
        try {
            if (!ruleIp.contains("/")) {
                // Exact IP match
                return ruleIp.equals(trafficIp);
            }
            // CIDR subnet match
            String[] parts = ruleIp.split("/");
            byte[] ruleAddr = InetAddress.getByName(parts[0]).getAddress();
            byte[] trafficAddr = InetAddress.getByName(trafficIp).getAddress();
            int prefixLength = Integer.parseInt(parts[1]);

            // Compare each bit up to the prefix length
            for (int i = 0; i < prefixLength; i++) {
                int byteIndex = i / 8;
                int bitIndex = 7 - (i % 8);
                int ruleBit = (ruleAddr[byteIndex] >> bitIndex) & 1;
                int trafficBit = (trafficAddr[byteIndex] >> bitIndex) & 1;
                if (ruleBit != trafficBit) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to parse IP for CIDR matching: rule={}, traffic={}", ruleIp, trafficIp, e);
            return ruleIp.equals(trafficIp);
        }
    }

    private void logAudit(String eventType, UUID actorId, Map<String, Object> data) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .actorId(actorId)
                .eventData(data)
                .build();
        auditEventRepository.save(event);
    }
}