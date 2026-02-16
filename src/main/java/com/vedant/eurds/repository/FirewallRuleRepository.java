package com.vedant.eurds.repository;

import com.vedant.eurds.model.FirewallRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FirewallRuleRepository extends JpaRepository<FirewallRule, Integer> {

    List<FirewallRule> findByActiveTrueOrderByPriorityAsc();

    List<FirewallRule> findAllByOrderByPriorityAsc();

    boolean existsByRuleName(String ruleName);

    List<FirewallRule> findByRuleTypeAndActiveTrue(String ruleType);
}