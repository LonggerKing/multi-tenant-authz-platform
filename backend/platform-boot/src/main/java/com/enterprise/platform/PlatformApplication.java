package com.enterprise.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 企业级多租户权限中台启动类。
 */
@SpringBootApplication(
        scanBasePackages = "com.enterprise.platform",
        exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan({
        "com.enterprise.platform.tenant.mapper",
        "com.enterprise.platform.permission.mapper",
        "com.enterprise.platform.auth.mapper",
        "com.enterprise.platform.demo.mapper"
})
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
