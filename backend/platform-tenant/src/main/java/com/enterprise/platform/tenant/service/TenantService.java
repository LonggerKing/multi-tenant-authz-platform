package com.enterprise.platform.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.common.exception.BizException;
import com.enterprise.platform.common.exception.ErrorCode;
import com.enterprise.platform.common.event.TenantProvisionedEvent;
import com.enterprise.platform.common.tenant.IsolationMode;
import com.enterprise.platform.common.tenant.TenantInfo;
import com.enterprise.platform.tenant.config.TenantProperties;
import com.enterprise.platform.tenant.dto.TenantDtos;
import com.enterprise.platform.tenant.entity.SysTenant;
import com.enterprise.platform.tenant.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户生命周期管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final SysTenantMapper tenantMapper;
    private final TenantCache tenantCache;
    private final TenantSchemaInitializer schemaInitializer;
    private final TenantProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    public PageResult<SysTenant> page(TenantDtos.Query query) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysTenant::getCode, kw).or().like(SysTenant::getName, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysTenant::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysTenant::getCreateTime);

        Page<SysTenant> page = tenantMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public List<SysTenant> listAll() {
        return tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .orderByDesc(SysTenant::getCreateTime));
    }

    public SysTenant getById(Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw BizException.of(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysTenant create(TenantDtos.CreateRequest request) {
        Long exists = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, request.getCode()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.CONFLICT, "租户编码已存在: " + request.getCode());
        }

        IsolationMode mode = request.getIsolationMode() == null
                ? IsolationMode.SHARED : request.getIsolationMode();
        if (mode == IsolationMode.SCHEMA && !properties.isSchemaIsolationEnabled()) {
            throw BizException.of(ErrorCode.ISOLATION_NOT_SUPPORTED,
                    "当前环境未开启独立 Schema 隔离");
        }

        SysTenant tenant = new SysTenant();
        tenant.setCode(request.getCode());
        tenant.setName(request.getName());
        tenant.setIsolationMode(mode);
        tenant.setStatus(1);
        tenant.setExpireAt(request.getExpireAt());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setMaxUsers(request.getMaxUsers());
        tenant.setRemark(request.getRemark());
        if (mode == IsolationMode.SCHEMA) {
            tenant.setSchemaName(properties.getSchemaPrefix() + request.getCode());
        }
        tenantMapper.insert(tenant);

        if (mode == IsolationMode.SCHEMA) {
            try {
                schemaInitializer.initialize(tenant.getSchemaName(), tenant.getId(), tenant.getCode());
            } catch (Exception e) {
                log.error("[租户开通] Schema 初始化失败，回滚租户创建: {}", tenant.getCode(), e);
                throw BizException.of(ErrorCode.SCHEMA_INIT_FAILED, e.getMessage());
            }
        }

        tenantCache.put(tenant);
        eventPublisher.publishEvent(new TenantProvisionedEvent(
                tenant.getId(), tenant.getCode(), tenant.getIsolationMode().name()));
        log.info("[租户开通] 租户 {} 创建成功，隔离模式={}", tenant.getCode(), mode);
        return tenant;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysTenant update(Long id, TenantDtos.UpdateRequest request) {
        SysTenant tenant = getById(id);
        tenant.setName(request.getName());
        tenant.setExpireAt(request.getExpireAt());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setMaxUsers(request.getMaxUsers());
        tenant.setRemark(request.getRemark());
        tenantMapper.updateById(tenant);
        tenantCache.evict(tenant.getCode());
        return tenant;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        SysTenant tenant = getById(id);
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
        tenantCache.evict(tenant.getCode());
        log.info("[租户状态] {} -> {}", tenant.getCode(), status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysTenant tenant = getById(id);
        tenantMapper.deleteById(id);
        tenantCache.evict(tenant.getCode());
    }

    /**
     * 按编码或 ID 解析租户，供请求过滤器使用。
     */
    public TenantInfo resolve(String codeOrId) {
        if (!StringUtils.hasText(codeOrId)) {
            return null;
        }
        String key = codeOrId.trim();
        TenantInfo cached = tenantCache.getByCode(key);
        if (cached != null) {
            return cached;
        }
        SysTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, key).last("limit 1"));
        if (tenant == null) {
            try {
                Long id = Long.parseLong(key);
                TenantInfo cachedById = tenantCache.getById(id);
                if (cachedById != null) {
                    return cachedById;
                }
                tenant = tenantMapper.selectById(id);
            } catch (NumberFormatException ignore) {
                // 非数字，按编码处理
            }
        }
        if (tenant == null) {
            throw BizException.of(ErrorCode.TENANT_NOT_FOUND, "租户不存在: " + key);
        }
        assertActive(tenant);
        tenantCache.put(tenant);
        return TenantCache.toInfo(tenant);
    }

    /**
     * 登录前校验：租户必须存在、启用且未过期。
     */
    public SysTenant requireActiveByCode(String code) {
        SysTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, code).last("limit 1"));
        if (tenant == null) {
            throw BizException.of(ErrorCode.TENANT_NOT_FOUND);
        }
        assertActive(tenant);
        tenantCache.put(tenant);
        return tenant;
    }

    private void assertActive(SysTenant tenant) {
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw BizException.of(ErrorCode.TENANT_DISABLED, "租户已停用: " + tenant.getCode());
        }
        if (tenant.getExpireAt() != null && tenant.getExpireAt().isBefore(LocalDateTime.now())) {
            throw BizException.of(ErrorCode.TENANT_EXPIRED, "租户已过期: " + tenant.getCode());
        }
    }
}
