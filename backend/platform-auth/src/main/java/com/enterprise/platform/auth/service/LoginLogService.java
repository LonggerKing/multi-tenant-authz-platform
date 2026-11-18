package com.enterprise.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.platform.auth.entity.SysLoginLog;
import com.enterprise.platform.auth.mapper.SysLoginLogMapper;
import com.enterprise.platform.common.api.PageQuery;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录审计日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    public void record(String username, String ip, String userAgent, boolean success, String message) {
        if (!TenantContext.isPresent()) {
            return;
        }
        try {
            SysLoginLog log = new SysLoginLog();
            log.setTenantId(TenantContext.currentTenantId());
            log.setUsername(username);
            log.setIp(ip);
            log.setUserAgent(userAgent == null ? null
                    : userAgent.substring(0, Math.min(userAgent.length(), 200)));
            log.setSuccess(success);
            log.setMessage(message);
            log.setCreateTime(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception e) {
            // 审计失败不影响登录主流程
            log.warn("[登录日志] 写入失败: {}", e.getMessage());
        }
    }

    public PageResult<SysLoginLog> page(PageQuery query) {
        Page<SysLoginLog> page = loginLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysLoginLog>().orderByDesc(SysLoginLog::getCreateTime));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
