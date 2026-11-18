package com.enterprise.platform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 演示数据密码初始化：把种子数据里的明文密码转换为 BCrypt 哈希。
 * <p>仅处理非 BCrypt 格式的历史数据，可重复执行。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordBootstrapRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, password from sys_user where password not like '$2%'");
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String raw = String.valueOf(row.get("password"));
            jdbcTemplate.update("update sys_user set password = ? where id = ?",
                    passwordEncoder.encode(raw), id);
        }
        if (!rows.isEmpty()) {
            log.info("[初始化] 已为 {} 个种子账号生成 BCrypt 密码哈希", rows.size());
        }
    }
}
