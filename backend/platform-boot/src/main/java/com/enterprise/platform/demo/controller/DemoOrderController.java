package com.enterprise.platform.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.platform.common.api.ApiResult;
import com.enterprise.platform.common.api.PageQuery;
import com.enterprise.platform.common.api.PageResult;
import com.enterprise.platform.common.security.UserContext;
import com.enterprise.platform.demo.entity.BizOrder;
import com.enterprise.platform.demo.mapper.BizOrderMapper;
import com.enterprise.platform.permission.scopedata.DataScopeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 行级数据权限演示：不同角色看到的订单范围不同。
 * <p>SQL 中不会出现任何数据范围条件，全部由 MyBatis 拦截器按当前用户动态注入。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/demo/orders")
@RequiredArgsConstructor
public class DemoOrderController {

    private final BizOrderMapper orderMapper;
    private final DataScopeService dataScopeService;

    @GetMapping
    @PreAuthorize("@perm.has('demo:order:list')")
    public ApiResult<PageResult<BizOrder>> page(@Valid PageQuery query) {
        Page<BizOrder> page = orderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<BizOrder>().orderByDesc(BizOrder::getCreateTime));
        return ApiResult.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    /**
     * 当前登录用户的数据范围画像，便于前端展示与联调排查。
     */
    @GetMapping("/scope")
    @PreAuthorize("@perm.has('demo:order:list')")
    public ApiResult<Map<String, Object>> scope() {
        DataScopeService.ScopeFilter filter = dataScopeService.resolve(UserContext.currentUserId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("all", filter.isAll());
        result.put("selfOnly", filter.isSelfOnly());
        result.put("deptIds", filter.getDeptIds());
        result.put("sqlCondition", dataScopeService.buildCondition(null, UserContext.currentUserId()));
        return ApiResult.ok(result);
    }
}
