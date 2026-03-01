package com.vedant.eurds.service;

import com.vedant.eurds.dto.FirewallRuleDTO;
import com.vedant.eurds.model.AuditEvent;
import com.vedant.eurds.model.FirewallRule;
import com.vedant.eurds.repository.AuditEventRepository;
import com.vedant.eurds.repository.FirewallRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirewallServiceTest {

    @Mock private FirewallRuleRepository firewallRuleRepository;
    @Mock private AuditEventRepository auditEventRepository;

    @InjectMocks
    private FirewallService firewallService;

    private UUID adminId;
    private FirewallRuleDTO validDto;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        validDto = FirewallRuleDTO.builder()
                .ruleName("Block SSH")
                .ruleType("DENY")
                .sourceIp("10.0.0.0")
                .destinationPort(22)
                .protocol("TCP")
                .priority(1)
                .active(true)
                .build();
    }

    // ============================================================
    // ADD RULE TESTS
    // ============================================================

    @Test
    void addRule_ValidRule_Success() {
        when(firewallRuleRepository.existsByRuleName("Block SSH")).thenReturn(false);
        when(firewallRuleRepository.save(any(FirewallRule.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(auditEventRepository.save(any())).thenReturn(null);

        FirewallRule result = firewallService.addRule(validDto, adminId);

        assertEquals("Block SSH", result.getRuleName());
        assertEquals("DENY", result.getRuleType());
        assertEquals("TCP", result.getProtocol());
        verify(firewallRuleRepository).save(any(FirewallRule.class));
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    void addRule_DuplicateName_ThrowsException() {
        when(firewallRuleRepository.existsByRuleName("Block SSH")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> firewallService.addRule(validDto, adminId));

        verify(firewallRuleRepository, never()).save(any());
    }

    @Test
    void addRule_NullProtocol_DefaultsToAny() {
        validDto.setProtocol(null);
        when(firewallRuleRepository.existsByRuleName(any())).thenReturn(false);
        when(firewallRuleRepository.save(any(FirewallRule.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(auditEventRepository.save(any())).thenReturn(null);

        FirewallRule result = firewallService.addRule(validDto, adminId);

        assertEquals("ANY", result.getProtocol());
    }

    // ============================================================
    // TOGGLE RULE TESTS
    // ============================================================

    @Test
    void toggleRule_Disable_Success() {
        FirewallRule rule = FirewallRule.builder()
                .id(1).ruleName("Test Rule").active(true).build();

        when(firewallRuleRepository.findById(1)).thenReturn(Optional.of(rule));
        when(firewallRuleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditEventRepository.save(any())).thenReturn(null);

        FirewallRule result = firewallService.toggleRule(1, false, adminId);

        assertFalse(result.isActive());
    }

    @Test
    void toggleRule_NotFound_ThrowsException() {
        when(firewallRuleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> firewallService.toggleRule(99, true, adminId));
    }

    // ============================================================
    // DELETE RULE TESTS
    // ============================================================

    @Test
    void deleteRule_Exists_Deletes() {
        FirewallRule rule = FirewallRule.builder()
                .id(1).ruleName("Test Rule").build();
        when(firewallRuleRepository.findById(1)).thenReturn(Optional.of(rule));
        when(auditEventRepository.save(any())).thenReturn(null);

        firewallService.deleteRule(1, adminId);

        verify(firewallRuleRepository).delete(rule);
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    // ============================================================
    // TRAFFIC EVALUATION TESTS
    // ============================================================

    @Test
    void evaluateTraffic_MatchesDenyRule_ReturnsDeny() {
        FirewallRule denyRule = FirewallRule.builder()
                .ruleName("Block SSH")
                .ruleType("DENY")
                .sourceIp("10.0.0.1")
                .destinationPort(22)
                .protocol("TCP")
                .priority(1)
                .active(true)
                .build();

        when(firewallRuleRepository.findByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(denyRule));

        String result = firewallService.evaluateTraffic("10.0.0.1", 22, "TCP");

        assertEquals("DENY", result);
    }

    @Test
    void evaluateTraffic_NoMatchingRules_DefaultAllow() {
        when(firewallRuleRepository.findByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of());

        String result = firewallService.evaluateTraffic("10.0.0.1", 80, "TCP");

        assertEquals("ALLOW", result);
    }

    @Test
    void evaluateTraffic_WildcardRule_MatchesAll() {
        FirewallRule wildcardRule = FirewallRule.builder()
                .ruleName("Block All")
                .ruleType("DENY")
                .sourceIp(null)        // wildcard
                .destinationPort(null) // wildcard
                .protocol("ANY")       // wildcard
                .priority(100)
                .active(true)
                .build();

        when(firewallRuleRepository.findByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(wildcardRule));

        assertEquals("DENY", firewallService.evaluateTraffic("any.ip", 12345, "UDP"));
    }

    @Test
    void evaluateTraffic_PriorityOrder_FirstMatchWins() {
        FirewallRule allowRule = FirewallRule.builder()
                .ruleName("Allow HTTP").ruleType("ALLOW")
                .sourceIp(null).destinationPort(80).protocol("TCP")
                .priority(1).active(true).build();
        FirewallRule denyAll = FirewallRule.builder()
                .ruleName("Deny All").ruleType("DENY")
                .sourceIp(null).destinationPort(null).protocol("ANY")
                .priority(100).active(true).build();

        when(firewallRuleRepository.findByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(allowRule, denyAll));

        // Port 80 matches the ALLOW rule first
        assertEquals("ALLOW", firewallService.evaluateTraffic("10.0.0.1", 80, "TCP"));
        // Port 443 skips the ALLOW rule, matches DENY ALL
        assertEquals("DENY", firewallService.evaluateTraffic("10.0.0.1", 443, "TCP"));
    }

    @Test
    void evaluateTraffic_CidrRange_MatchesSubnet() {
        FirewallRule cidrRule = FirewallRule.builder()
                .ruleName("Block Subnet")
                .ruleType("DENY")
                .sourceIp("192.168.1.0/24")
                .destinationPort(22)
                .protocol("TCP")
                .priority(1)
                .active(true)
                .build();

        when(firewallRuleRepository.findByActiveTrueOrderByPriorityAsc())
                .thenReturn(List.of(cidrRule));

        // IP within the /24 subnet — should match
        assertEquals("DENY", firewallService.evaluateTraffic("192.168.1.50", 22, "TCP"));
        assertEquals("DENY", firewallService.evaluateTraffic("192.168.1.255", 22, "TCP"));

        // IP outside the /24 subnet — should NOT match, default ALLOW
        assertEquals("ALLOW", firewallService.evaluateTraffic("192.168.2.1", 22, "TCP"));
    }
}
