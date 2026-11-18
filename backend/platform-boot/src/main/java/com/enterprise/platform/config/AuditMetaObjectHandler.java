package com.enterprise.platform.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.enterprise.platform.common.security.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        if (UserContext.isAuthenticated()) {
            Long userId = UserContext.currentUserId();
            strictInsertFill(metaObject, "createBy", Long.class, userId);
            strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        if (UserContext.isAuthenticated()) {
            strictUpdateFill(metaObject, "updateBy", Long.class, UserContext.currentUserId());
        }
    }
}
