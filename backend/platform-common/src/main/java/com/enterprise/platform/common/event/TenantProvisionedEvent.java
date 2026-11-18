package com.enterprise.platform.common.event;

/**
 * 租户开通完成事件。
 * <p>独立 Schema 租户的物理结构创建完毕后发布，权限域监听后重建 Casbin 策略引擎。</p>
 */
public record TenantProvisionedEvent(Long tenantId, String tenantCode, String isolationMode) {
}
