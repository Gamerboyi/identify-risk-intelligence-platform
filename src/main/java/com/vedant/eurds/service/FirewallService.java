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
                .isActive(dto.isActive())
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
        return firewallRuleRepository.findByIsActiveTrueOrderByPriorityAsc();
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
        List<FirewallRule> activeRules = firewallRuleRepository.findByIsActiveTrueOrderByPriorityAsc();
        for (FirewallRule rule : activeRules) {
            boolean ipMatch = rule.getSourceIp() == null || rule.getSourceIp().equals(sourceIp);
            boolean portMatch = rule.getDestinationPort() == null || rule.getDestinationPort().equals(port);
            boolean protocolMatch = rule.getProtocol().equals("ANY") || rule.getProtocol().equalsIgnoreCase(protocol);
            if (ipMatch && portMatch && protocolMatch) {
                log.info("Traffic from {} port {} matched rule: {} [{}]", sourceIp, port, rule.getRuleName(), rule.getRuleType());
                return rule.getRuleType();
            }
        }
        return "ALLOW";
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