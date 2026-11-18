# 架构设计说明

## 1. 分层与模块职责

```
┌───────────────────────────── platform-boot ─────────────────────────────┐
│ 启动类 / MyBatis 拦截器链 / 配置 / 演示业务 / SQL 资源                    │
├──────────────┬───────────────┬──────────────────┬───────────────────────┤
│ platform-auth│platform-      │platform-permission│ platform-tenant       │
│ 认证与安全链 │ permission    │ Casbin / ACL / 数据权限 / ABAC           │
│              │               │                  │ 租户生命周期 / 隔离    │
├──────────────┴───────────────┴──────────────────┴───────────────────────┤
│ platform-common：统一响应、异常、租户上下文、用户上下文、注解、事件        │
└─────────────────────────────────────────────────────────────────────────┘
```

依赖方向单向向下，`platform-auth` 依赖 `permission` 与 `tenant`，不存在反向依赖；
跨域协作通过 `platform-common` 中的上下文与事件完成（例如 `TenantProvisionedEvent`）。

## 2. 多租户隔离

### 2.1 共享表 + tenant_id（默认）

`TenantLineInnerInterceptor` 自动为所有非忽略表追加 `tenant_id = ?`：

- 忽略表：`sys_tenant`（平台级）、`sys_casbin_rule`（全局策略表）。
- INSERT 时若 SQL 已显式包含 `tenant_id` 列，则不重复注入。
- 租户来自 `TenantContext`，取值优先级：**JWT 声明 > `X-Tenant-Id` 请求头 > 子域名**。

### 2.2 独立 Schema（大客户/强合规）

1. `POST /api/tenants` 指定 `isolationMode=SCHEMA`；
2. `TenantSchemaInitializer` 执行 `create schema` + `db/tenant-schema.sql` + `db/tenant-seed.sql`；
3. 业务表 DDL 的 `{schema}` 占位符被替换为实际 Schema 名，种子数据写入初始管理员 `admin / admin123`；
4. 请求进入后 `TenantContext.currentSchema()` 非空，`DynamicTableNameInnerInterceptor` 将
   `sys_user` 改写为 `tenant_globex.sys_user`，同时关闭 `tenant_id` 过滤；
5. 权限策略仍写入全局 `sys_casbin_rule`，通过 domain 隔离，保证单引擎可服务全部租户。

### 2.3 防串租户

- 所有上下文使用 `ThreadLocal`，过滤器与认证过滤器的 `finally` 中强制清理；
- 令牌中的租户声明覆盖请求头，客户端无法通过伪造 `X-Tenant-Id` 越权；
- 租户状态与到期时间在解析阶段校验（启用 + 未过期）。

## 3. 认证

### 3.1 双模式

| 模式       | 校验方式                                                                | 适用场景                |
| ---------- | ----------------------------------------------------------------------- | ----------------------- |
| `internal` | HS256 自签 JWT，`MACVerifier` 校验签名 + 过期 + jti 黑名单              | 内部系统、离线部署      |
| `keycloak` | `RemoteJWKSet` 拉取 JWKS，`ConfigurableJWTProcessor` 验签并校验必需声明 | 已有 IdP、需要 SSO/OIDC |

`TokenVerifier` 接口按 `platform.auth.mode` 条件装配，认证过滤器对二者无感。

### 3.2 Keycloak 声明映射

| Keycloak 声明        | 平台字段     | 说明                                  |
| -------------------- | ------------ | ------------------------------------- |
| `sub`                | —            | 由 `platform_user_id` 决定平台用户    |
| `platform_user_id`   | `userId`     | 用户需预先在本平台存在                |
| `preferred_username` | `username`   |                                       |
| `tenant_code`        | `tenantCode` | 自定义 attribute mapper               |
| `realm_access.roles` | `roles`      | 角色编码需与平台 `sys_role.code` 一致 |

### 3.3 令牌与黑名单

- Access Token 默认 2h，Refresh Token 默认 7d；
- 登出将 `jti` 写入黑名单缓存（当前为本地 Caffeine，集群部署切换 Redis 即可）；
- 认证失败统一返回 `401 + {code:40100}`，不泄露具体失败原因。

## 4. 权限模型

### 4.1 Casbin 模型

```ini
[request_definition]
r = sub, dom, obj, act

[policy_definition]
p = sub, dom, obj, act, eft

[role_definition]
g = _, _, _

[policy_effect]
e = some(where (p.eft == allow)) && !some(where (p.eft == deny))

[matchers]
m = (r.sub == p.sub || g(r.sub, p.sub, r.dom)) && r.dom == p.dom \
    && keyMatch(r.obj, p.obj) && (p.act == "*" || r.act == p.act)
```

- `sub`：`u:{userId}`，通过 `g` 关联到角色；
- `dom`：租户编码，实现同一引擎内的租户隔离；
- `obj`：权限点编码（`system:user:list`）或接口路径模式（`/api/demo/orders/*`）；
- `keyMatch` 原生支持 `*` 通配，避免 `keyMatch2` 对纯通配符产生非法正则。

### 4.2 策略生命周期

```
角色-菜单变更 → 覆盖 sys_role_menu → 重建该角色 p 策略 → 重建 Enforcer → 清理缓存
用户-角色变更 → 覆盖 sys_user_role → 重建该用户 g 策略 → 重建 Enforcer → 清理缓存
```

Enforcer 采用"构建新实例 → volatile 原子替换"，读请求无锁，写请求串行，避免策略半更新状态。

### 4.3 方法级鉴权

```java
@PreAuthorize("@perm.has('system:user:create')")
```

`PermissionChecker`（Bean 名 `perm`）→ `AuthorizationService`：

```
superAdmin ? allow
           : casbin.enforce(sub, tenant, obj, act) && abac.evaluate(ctx, obj, act)
```

另提供 `@RequirePermission` / `@RequireSuperAdmin` 注解 + AOP 切面，
用于无法使用 SpEL 的场景（内部服务、RPC 门面）。

## 5. ABAC 条件引擎

- 策略表 `sys_api_policy`：资源模式 + HTTP 方法 + 条件表达式 + 效果（allow/deny）+ 优先级；
- 表达式为 SpEL，root 为 `AbacContext`：

| 变量                         | 含义                         |
| ---------------------------- | ---------------------------- |
| `userId` / `username`        | 主体标识                     |
| `tenantId` / `tenantCode`    | 租户                         |
| `deptId`                     | 所属部门                     |
| `roles` / `hasRole('X')`     | 角色集合                     |
| `ip`                         | 客户端 IP                    |
| `hour` / `now`               | 访问时间                     |
| `attributes` / `attr('key')` | 扩展属性（单据归属、金额等） |

- 判定语义：命中资源的策略中，任一 `deny` 条件成立即拒绝；否则至少一条 `allow` 条件成立才放行；
  命中但全部条件不成立同样拒绝（动态收窄授权）。
- 表达式解析失败按拒绝处理（fail-closed），并记录告警。

示例：

```text
hour >= 9 && hour < 19                     # 仅工作时间
ip != null && ip.startsWith('10.')         # 仅内网
hasRole('TENANT_ADMIN')                    # 仅租户管理员
```

## 6. 行级数据权限

### 6.1 角色数据范围合并

| 范围             | 语义         | 生成条件                          |
| ---------------- | ------------ | --------------------------------- |
| `ALL`            | 全部         | 不追加条件（任一角色 ALL 即生效） |
| `DEPT`           | 本部门       | `dept_id in (userDeptId)`         |
| `DEPT_AND_CHILD` | 本部门及下级 | 依据 `sys_dept.path` 前缀匹配子树 |
| `SELF`           | 仅本人       | `create_by = userId`              |
| `CUSTOM`         | 自定义部门   | `dept_id in (配置集合)`           |

多角色取并集；部门集合为空时退化为"仅本人"，避免意外放大权限。

### 6.2 SQL 注入点

`PlatformDataPermissionHandler` 实现 `MultiDataPermissionHandler`：

- 仅对配置的表（默认 `biz_order`）与指定语句（用户分页查询）生效；
- 通过别名拼接，兼容多表查询；
- 条件解析失败时跳过并告警，不阻断业务（可用性优先，隔离性由表级策略兜底）。

## 7. 缓存设计

| 层级 | 组件     | TTL         | 作用                       |
| ---- | -------- | ----------- | -------------------------- |
| L1   | Caffeine | 60s（上限） | 单实例高频命中，微秒级     |
| L2   | Redis    | 300~600s    | 多实例共享，减少数据库压力 |

缓存键（均含租户编码，天然隔离）：

```
perm:decision:{tenant}:{sub}:{obj}:{act}   # 鉴权决策
perm:roles:{tenant}:{userId}              # 角色编码
perm:codes:{tenant}:{userId}              # 按钮权限点
perm:menu:{tenant}:{userId}               # 菜单树
perm:scope:{tenant}:{userId}              # 数据范围
perm:abac:{tenant}                        # ABAC 策略集
```

失效策略：按前缀驱逐（Caffeine 遍历 + Redis SCAN），授权变更后立即生效。
Redis 异常时标记降级，仅使用 L1，不影响正确性。

## 8. 数据库设计要点

- 主键统一雪花 ID（`ASSIGN_ID`），避免跨库/跨 Schema 自增冲突；
- 统一审计字段：`create_time / update_time / create_by / update_by`，逻辑删除 `deleted`；
- `sys_dept.path` 物化路径支持 O(1) 子树条件；
- 唯一索引保证租户内用户名、角色编码唯一：`(tenant_id, username)`、`(tenant_id, code)`；
- `sys_casbin_rule` 使用 `v0~v5` 通用列，兼容不同模型扩展。

## 9. 安全清单

- [x] 无状态认证 + 登出黑名单
- [x] 租户状态/到期校验
- [x] 令牌租户声明覆盖请求头，防伪造
- [x] 方法级权限校验（fail-closed）
- [x] ABAC 表达式异常拒绝
- [x] 登录审计与失败原因记录
- [x] 统一异常处理，避免堆栈外泄
- [ ] 敏感字段加密与脱敏（列级权限落地）
- [ ] 策略变更审批流与变更审计
- [ ] 密码策略、登录失败锁定、MFA

## 10. 演进路线

1. **网关化**：Spring Cloud Gateway + 全局过滤器做 URL 级鉴权，服务只保留方法级校验；
2. **策略异步化**：策略变更写入事件总线（Kafka），各实例订阅刷新 Enforcer；
3. **Schema 迁移**：Flyway 多 Schema 版本化，替代同步 DDL；
4. **可观测性**：鉴权耗时直方图、缓存命中率、策略规模指标接入 Prometheus；
5. **列级权限**：基于字段白名单 + 脱敏规则的响应裁剪，与 ABAC 策略共用条件引擎。
