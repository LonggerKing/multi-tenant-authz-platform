package com.enterprise.platform.tenant.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 独立 Schema 租户的开通：建 Schema、建表、写入该租户的初始管理员。
 * <p>真实生产环境建议改为异步任务 + 版本化 DDL 迁移（Flyway 多 Schema），
 * 这里保留可直接执行的同步实现，便于本地与演示环境一键开通。</p>
 */
@Slf4j
@Component
public class TenantSchemaInitializer {

    private static final String SCHEMA_SQL = "db/tenant-schema.sql";
    private static final String SEED_SQL = "db/tenant-seed.sql";

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TenantSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initialize(String schemaName, Long tenantId, String tenantCode) {
        String schema = schemaName.toLowerCase();
        log.info("[租户开通] 创建独立 Schema: {}", schema);
        jdbcTemplate.execute("create schema if not exists " + schema);

        List<String> ddl = loadStatements(SCHEMA_SQL, schema, tenantId, tenantCode);
        for (String sql : ddl) {
            jdbcTemplate.execute(sql);
        }

        String adminHash = passwordEncoder.encode("admin123");
        List<String> seed = loadStatements(SEED_SQL, schema, tenantId, tenantCode);
        for (String sql : seed) {
            jdbcTemplate.update(sql.replace("{admin_hash}", adminHash));
        }

        // Casbin 策略存放于全局策略表（引擎为全局单实例，按 domain 隔离），
        // 即使租户使用独立 Schema 也不写入租户 schema。
        long policyId = IdWorker.getId();
        long groupingId = IdWorker.getId();
        jdbcTemplate.update("insert into sys_casbin_rule (id, tenant_id, ptype, v0, v1, v2, v3, v4, v5) "
                        + "values (?, ?, 'p', 'TENANT_ADMIN', ?, '*', '*', 'allow', null)",
                policyId, tenantId, tenantCode);
        jdbcTemplate.update("insert into sys_casbin_rule (id, tenant_id, ptype, v0, v1, v2, v3, v4, v5) "
                        + "values (?, ?, 'g', 'u:1000', 'TENANT_ADMIN', ?, null, null, null)",
                groupingId, tenantId, tenantCode);

        log.info("[租户开通] Schema {} 初始化完成，初始管理员 admin / admin123", schema);
    }

    /**
     * 读取 SQL 资源并做占位符替换、语句切分。
     */
    private List<String> loadStatements(String resource, String schema, Long tenantId, String tenantCode) {
        ClassPathResource classPathResource = new ClassPathResource(resource);
        if (!classPathResource.exists()) {
            throw new IllegalStateException("缺少租户初始化脚本: " + resource);
        }
        try (InputStream in = classPathResource.getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{schema}", schema)
                    .replace("{tenant_id}", String.valueOf(tenantId))
                    .replace("{tenant_code}", tenantCode);
            List<String> statements = new ArrayList<>();
            for (String raw : content.split(";")) {
                String sql = stripComments(raw).trim();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
            }
            return statements;
        } catch (IOException e) {
            throw new IllegalStateException("读取租户初始化脚本失败: " + resource, e);
        }
    }

    private String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
