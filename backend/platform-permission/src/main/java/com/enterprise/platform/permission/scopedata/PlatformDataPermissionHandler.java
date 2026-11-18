package com.enterprise.platform.permission.scopedata;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.enterprise.platform.common.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 行级数据权限拦截器（MyBatis-Plus DataPermissionInterceptor）。
 * <p>在 SQL 解析阶段为指定表追加数据范围条件，业务代码无需感知。
 * 默认只对演示业务表 biz_order 以及用户分页查询生效，避免污染鉴权自身的查询。</p>
 */
@Slf4j
@Component
public class PlatformDataPermissionHandler implements MultiDataPermissionHandler {

    private final DataScopeService dataScopeService;
    private final boolean enabled;
    private final Set<String> scopedTables = new LinkedHashSet<>();
    private final Set<String> scopedStatements = new LinkedHashSet<>();

    public PlatformDataPermissionHandler(DataScopeService dataScopeService,
                                         @Value("${platform.data-permission.enabled:true}") boolean enabled,
                                         @Value("${platform.data-permission.tables:biz_order}") List<String> tables,
                                         @Value("${platform.data-permission.statements:}") List<String> statements) {
        this.dataScopeService = dataScopeService;
        this.enabled = enabled;
        tables.stream().filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase()).forEach(scopedTables::add);
        statements.stream().filter(s -> s != null && !s.isBlank())
                .map(String::trim).forEach(scopedStatements::add);
        log.info("[数据权限] 生效表={}, 生效语句={}, enabled={}", scopedTables, scopedStatements, enabled);
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        if (!enabled || table == null || !UserContext.isAuthenticated()) {
            return null;
        }
        String tableName = table.getName() == null ? "" : table.getName().replace("\"", "").toLowerCase();
        boolean tableScoped = scopedTables.contains(tableName);
        boolean statementScoped = mappedStatementId != null
                && scopedStatements.stream().anyMatch(mappedStatementId::contains);
        boolean userPageQuery = "sys_user".equals(tableName) && mappedStatementId != null
                && mappedStatementId.endsWith("selectPage");
        if (!tableScoped && !(statementScoped && userPageQuery)) {
            return null;
        }
        String alias = table.getAlias() != null ? table.getAlias().getName() : null;
        String condition = dataScopeService.buildCondition(alias, UserContext.currentUserId());
        if (condition == null) {
            return null;
        }
        try {
            log.debug("[数据权限] {} 追加条件: {}", tableName, condition);
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            log.warn("[数据权限] 条件解析失败，跳过: {}, {}", condition, e.getMessage());
            return null;
        }
    }
}
