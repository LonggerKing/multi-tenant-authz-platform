package com.enterprise.platform.permission.abac;

import com.enterprise.platform.common.abac.AbacContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ABAC 条件表达式引擎。
 * <p>策略表达式由管理员配置，基于 SpEL 对 {@link AbacContext} 求值。
 * 表达式解析失败或抛异常一律按“不通过”处理（安全优先）。</p>
 */
@Slf4j
@Component
public class AbacConditionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Cache<String, Expression> expressionCache = Caffeine.newBuilder()
            .maximumSize(2_000)
            .build();

    public boolean evaluate(String expression, AbacContext context) {
        if (!StringUtils.hasText(expression)) {
            return true;
        }
        try {
            Expression compiled = expressionCache.get(expression.trim(), parser::parseExpression);
            if (compiled == null) {
                return false;
            }
            Boolean result = compiled.getValue(new StandardEvaluationContext(context), Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("[ABAC] 条件求值失败 expr={}, msg={}", expression, e.getMessage());
            return false;
        }
    }
}
