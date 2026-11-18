package com.enterprise.platform.tenant.dto;

import com.enterprise.platform.common.api.PageQuery;
import com.enterprise.platform.common.tenant.IsolationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户相关请求/响应对象。
 */
public final class TenantDtos {

    private TenantDtos() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends PageQuery {
        private String keyword;
        private Integer status;
    }

    @Data
    public static class CreateRequest {
        @NotBlank(message = "租户编码不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9_]{2,31}$", message = "租户编码需以小写字母开头，仅含小写字母/数字/下划线，长度 3-32")
        private String code;

        @NotBlank(message = "租户名称不能为空")
        @Size(max = 64, message = "租户名称过长")
        private String name;

        private IsolationMode isolationMode = IsolationMode.SHARED;

        private LocalDateTime expireAt;
        private String contactName;
        private String contactPhone;
        private Integer maxUsers = 100;
        private String remark;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "租户名称不能为空")
        @Size(max = 64, message = "租户名称过长")
        private String name;

        private LocalDateTime expireAt;
        private String contactName;
        private String contactPhone;
        private Integer maxUsers;
        private String remark;
    }

    @Data
    public static class StatusRequest {
        /** 1 启用，0 停用。 */
        private Integer status;
    }
}
