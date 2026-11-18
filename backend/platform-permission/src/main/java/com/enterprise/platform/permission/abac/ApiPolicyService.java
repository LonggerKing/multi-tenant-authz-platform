package com.enterprise.platform.permission.abac;

import com.enterprise.platform.common.abac.AbacContext;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.cache.PermissionCacheService;
import com.enterprise.platform.permission.entity.SysApiPolicy;
import com.enterprise.platform.permission.mapper.SysApiPolicyMapper;
import com.enterprise.platform.permission.support.ResourceMatcher;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ABAC 策略求值：在 Casbin RBAC 放行之后叠加属性条件。
 * <p>语义：命中资源的策略中，若存在满足条件的 deny 则拒绝；否则只要有一条 allow 条件满足即放行；
 * 命中资源但全部条件不满足同样拒绝（相当于动态收窄授权）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiPolicyService {

    private final SysApiPolicyMapper apiPolicyMapper;
    private final AbacConditionEvaluator conditionEvaluator;
    private final PermissionCacheService cacheService;

    public boolean evaluate(AbacContext context, String obj, String action) {
        List<SysApiPolicy> matched = matching(obj, action);
        if (matched.isEmpty()) {
            return true;
        }
        List<SysApiPolicy> denyPolicies = matched.stream()
                .filter(p -> "deny".equalsIgnoreCase(p.getEffect())).toList();
        for (SysApiPolicy policy : denyPolicies) {
            if (conditionEvaluator.evaluate(policy.getConditionExpr(), context)) {
                log.info("[ABAC] 命中拒绝策略 {}: user={}, obj={}", policy.getName(), context.getUserId(), obj);
                return false;
            }
        }
        for (SysApiPolicy policy : matched) {
            if (!"deny".equalsIgnoreCase(policy.getEffect())
                    && conditionEvaluator.evaluate(policy.getConditionExpr(), context)) {
                return true;
            }
        }
        log.info("[ABAC] 全部条件不满足，拒绝访问: user={}, obj={}", context.getUserId(), obj);
        return false;
    }

    public List<SysApiPolicy> list() {
        return apiPolicyMapper.selectList(null);
    }

    public void save(SysApiPolicy policy) {
        if (policy.getId() == null) {
            apiPolicyMapper.insert(policy);
        } else {
            apiPolicyMapper.updateById(policy);
        }
        evict();
    }

    public void delete(Long id) {
        apiPolicyMapper.deleteById(id);
        evict();
    }

    public void evict() {
        cacheService.evictByPrefix("perm:abac:" + TenantContext.currentTenantCode());
    }

    private List<SysApiPolicy> matching(String obj, String action) {
        List<SysApiPolicy> policies = cacheService.get(
                "perm:abac:" + TenantContext.currentTenantCode(),
                new TypeReference<List<SysApiPolicy>>() {
                },
                () -> {
                    List<SysApiPolicy> loaded = new ArrayList<>(apiPolicyMapper.selectList(null));
                    loaded.sort(Comparator.comparing(
                            p -> p.getPriority() == null ? 100 : p.getPriority()));
                    return loaded;
                });
        if (policies == null || policies.isEmpty()) {
            return List.of();
        }
        List<SysApiPolicy> result = new ArrayList<>();
        for (SysApiPolicy policy : policies) {
            if (policy.getStatus() != null && policy.getStatus() != 1) {
                continue;
            }
            if (!StringUtils.hasText(policy.getResourcePattern())) {
                continue;
            }
            if (!ResourceMatcher.matches(obj, policy.getResourcePattern())) {
                continue;
            }
            if (StringUtils.hasText(policy.getHttpMethod())
                    && !"*".equals(policy.getHttpMethod())
                    && !policy.getHttpMethod().equalsIgnoreCase(action)) {
                continue;
            }
            result.add(policy);
        }
        return result;
    }
}
