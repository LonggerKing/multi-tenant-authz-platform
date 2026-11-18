package com.enterprise.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志（安全审计）。
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private String username;

    private String ip;

    private String userAgent;

    private Boolean success;

    private String message;

    private LocalDateTime createTime;
}
