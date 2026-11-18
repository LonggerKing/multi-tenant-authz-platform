package com.enterprise.platform.permission.casbin;

import com.enterprise.platform.common.event.TenantProvisionedEvent;
import com.enterprise.platform.permission.cache.PermissionCacheService;
import com.enterprise.platform.permission.entity.SysCasbinRule;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Casbin 决策引擎。
 * <p>全局单实例：所有租户策略放在同一个 Enforcer 中，通过 domain(租户编码) 隔离。
 * 策略变更后整体重建 Enforcer 并原子替换，避免并发读写脏状态。</p>
 * <p>RBAC 负责“能不能访问某资源”，ABAC 条件由
 * {@code AbacConditionEvaluator} 叠加，详见 {@code AuthorizationService}。</p>
 */
@Slf4j
@Service
public class CasbinEnforcerService {

    private static final String MODEL_RESOURCE = "casbin/model.conf";
    private static final String DECISION_KEY_PREFIX = "perm:decision:";

    private final CasbinRuleRepository ruleRepository;
    private final PermissionCacheService cacheService;
    private final String modelText;

    private volatile Enforcer enforcer;

    public CasbinEnforcerService(CasbinRuleRepository ruleRepository,
                                 PermissionCacheService cacheService) {
        this.ruleRepository = ruleRepository;
        this.cacheService = cacheService;
        this.modelText = readModel();
    }

    /**
     * 应用就绪后预热，避免首个请求承担初始化开销。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            reload();
        } catch (Exception e) {
            log.warn("[Casbin] 预热失败，将在首次鉴权时重试: {}", e.getMessage());
        }
    }

    /**
     * 新租户开通后重建策略引擎，使其初始授权立即生效。
     */
    @EventListener(TenantProvisionedEvent.class)
    public void onTenantProvisioned(TenantProvisionedEvent event) {
        log.info("[Casbin] 租户 {} 开通完成，重建策略引擎", event.tenantCode());
        reload();
    }

    /**
     * 全量重建策略引擎。
     */
    public synchronized void reload() {
        List<SysCasbinRule> rules = ruleRepository.loadAll();
        var next = build(rules);
        this.enforcer = next;
        log.info("[Casbin] 策略引擎已重建，共 {} 条策略", rules.size());
    }

    public boolean enforce(String subject, String tenantCode, String obj, String action) {
        ensureInitialized();
        String cacheKey = DECISION_KEY_PREFIX + tenantCode + ":" + subject + ":" + obj + ":" + action;
        Boolean cached = cacheService.get(cacheKey, new TypeReference<Boolean>() {
        }, () -> {
            Enforcer current = this.enforcer;
            if (current == null) {
                return Boolean.FALSE;
            }
            try {
                return current.enforce(subject, tenantCode, obj, action);
            } catch (Exception e) {
                log.error("[Casbin] 鉴权执行异常: sub={}, obj={}, act={}", subject, obj, action, e);
                return Boolean.FALSE;
            }
        });
        return Boolean.TRUE.equals(cached);
    }

    public int policyCount() {
        ensureInitialized();
        Enforcer current = this.enforcer;
        return current == null ? 0 : current.getPolicy().size();
    }

    private void ensureInitialized() {
        if (this.enforcer != null) {
            return;
        }
        synchronized (this) {
            if (this.enforcer == null) {
                reload();
            }
        }
    }

    private Enforcer build(List<SysCasbinRule> rules) {
        Path modelFile = null;
        try {
            modelFile = Files.createTempFile("platform-casbin-", ".conf");
            Files.writeString(modelFile, modelText, StandardCharsets.UTF_8);
            Enforcer built = new Enforcer(modelFile.toString());
            int loaded = 0;
            for (SysCasbinRule rule : rules) {
                List<String> values = rule.toRule();
                if (values.isEmpty() || rule.getPtype() == null) {
                    continue;
                }
                if (rule.getPtype().startsWith("g")) {
                    built.addNamedGroupingPolicy(rule.getPtype(), values);
                } else {
                    built.addNamedPolicy(rule.getPtype(), values);
                }
                loaded++;
            }
            built.buildRoleLinks();
            log.debug("[Casbin] 载入策略 {} / {}", loaded, rules.size());
            return built;
        } catch (IOException e) {
            throw new IllegalStateException("初始化 Casbin 模型失败", e);
        } finally {
            if (modelFile != null) {
                try {
                    Files.deleteIfExists(modelFile);
                } catch (IOException ignore) {
                    // 临时文件清理失败可忽略
                }
            }
        }
    }

    private String readModel() {
        try (InputStream in = new ClassPathResource(MODEL_RESOURCE).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("缺少 Casbin 模型文件: " + MODEL_RESOURCE, e);
        }
    }
}
