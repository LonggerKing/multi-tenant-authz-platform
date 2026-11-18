package com.enterprise.platform.permission.casbin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.platform.permission.entity.SysCasbinRule;
import com.enterprise.platform.permission.mapper.SysCasbinRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Casbin 策略存储（关系型持久化）。
 * <p>sys_casbin_rule 为全局表，不参与 tenant_id 自动过滤，因此所有读写都显式带上 tenant_id。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CasbinRuleRepository {

    private final SysCasbinRuleMapper ruleMapper;

    public List<SysCasbinRule> loadAll() {
        return ruleMapper.selectList(null);
    }

    public List<SysCasbinRule> loadByTenant(Long tenantId) {
        return ruleMapper.selectList(new LambdaQueryWrapper<SysCasbinRule>()
                .eq(SysCasbinRule::getTenantId, tenantId));
    }

    /**
     * 覆盖某个角色在某租户下的全部资源策略。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceRolePolicies(Long tenantId, String roleCode, List<List<String>> policies) {
        ruleMapper.delete(new LambdaQueryWrapper<SysCasbinRule>()
                .eq(SysCasbinRule::getTenantId, tenantId)
                .eq(SysCasbinRule::getPtype, "p")
                .eq(SysCasbinRule::getV0, roleCode));
        for (List<String> policy : policies) {
            ruleMapper.insert(SysCasbinRule.of(tenantId, "p", policy));
        }
        log.debug("[Casbin] 已同步角色 {} 的 {} 条策略", roleCode, policies.size());
    }

    /**
     * 覆盖用户的角色绑定（g 策略第三列为租户域，由调用方传入租户编码）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceUserRoles(Long tenantId, String tenantCode, String subject, List<String> roleCodes) {
        ruleMapper.delete(new LambdaQueryWrapper<SysCasbinRule>()
                .eq(SysCasbinRule::getTenantId, tenantId)
                .eq(SysCasbinRule::getPtype, "g")
                .eq(SysCasbinRule::getV0, subject));
        for (String roleCode : roleCodes) {
            List<String> rule = new ArrayList<>(3);
            rule.add(subject);
            rule.add(roleCode);
            rule.add(tenantCode);
            ruleMapper.insert(SysCasbinRule.of(tenantId, "g", rule));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByRole(Long tenantId, String roleCode) {
        ruleMapper.delete(new LambdaQueryWrapper<SysCasbinRule>()
                .eq(SysCasbinRule::getTenantId, tenantId)
                .eq(SysCasbinRule::getPtype, "p")
                .eq(SysCasbinRule::getV0, roleCode));
    }
}
