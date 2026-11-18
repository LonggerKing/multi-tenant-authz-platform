package com.enterprise.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.enterprise.platform.common.tenant.TenantContext;
import com.enterprise.platform.permission.scopedata.PlatformDataPermissionHandler;
import com.enterprise.platform.tenant.config.TenantProperties;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * MyBatis-Plus 拦截器链，顺序至关重要：
 * <ol>
 *   <li>动态表名：独立 Schema 租户 → {@code tenant_xxx.sys_user}</li>
 *   <li>多租户：共享表租户 → 自动追加 {@code tenant_id = ?}</li>
 *   <li>行级数据权限：按角色数据范围追加 {@code dept_id in (...) / create_by = ?}</li>
 *   <li>分页</li>
 * </ol>
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    private final TenantProperties tenantProperties;
    private final PlatformDataPermissionHandler dataPermissionHandler;

    /**
     * 数据权限处理器依赖 Mapper（Mapper 又依赖 SqlSessionFactory），
     * 因此这里必须使用 @Lazy 注入代理，打断与 MybatisPlusInterceptor 的循环依赖。
     */
    public MybatisPlusConfig(TenantProperties tenantProperties,
                             @Lazy PlatformDataPermissionHandler dataPermissionHandler) {
        this.tenantProperties = tenantProperties;
        this.dataPermissionHandler = dataPermissionHandler;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 独立 Schema 隔离
        DynamicTableNameInnerInterceptor dynamicTableName = new DynamicTableNameInnerInterceptor();
        dynamicTableName.setTableNameHandler((sql, tableName) -> {
            String schema = TenantContext.currentSchema();
            if (schema == null || isIgnored(tableName)) {
                return tableName;
            }
            return schema + "." + tableName;
        });
        interceptor.addInnerInterceptor(dynamicTableName);

        // 2. 共享表租户隔离
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.require().getId());
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 独立 Schema 租户的物理表已隔离，不再叠加 tenant_id 条件
                if (TenantContext.currentSchema() != null) {
                    return true;
                }
                return isIgnored(tableName);
            }
        }));

        // 3. 行级数据权限（ABAC 数据面）
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataPermissionHandler));

        // 4. 分页
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }

    private boolean isIgnored(String tableName) {
        if (tableName == null) {
            return true;
        }
        String name = tableName.replace("\"", "").toLowerCase();
        return tenantProperties.getIgnoreTables().stream()
                .anyMatch(ignored -> ignored.equalsIgnoreCase(name));
    }
}
