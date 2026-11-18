# 企业级多租户权限中台

面向 SaaS 多业务线的统一权限基础设施：**一套账号体系、一套授权模型、一套审计口径**，
支撑共享表与独立 Schema 混合隔离，RBAC 与 ABAC 叠加判定，行列级数据权限自动注入。

```
Java 21 · Spring Boot 3.3 · Spring Security · Casbin(jcasbin) · MyBatis-Plus
PostgreSQL / H2 · Redis + Caffeine · Keycloak(可选) · Vue 3 + Vite + Element Plus
```

## 一、能力概览

| 能力         | 实现要点                                                                           |
| ------------ | ---------------------------------------------------------------------------------- |
| 多租户隔离   | `tenant_id` 共享表（MyBatis 拦截器自动注入）+ 独立 Schema（动态表名改写）混合方案  |
| 统一认证     | 内置 JWT（HS256）与 Keycloak JWKS 双模式，配置 `platform.auth.mode` 切换           |
| 权限模型     | Casbin 的 RBAC（角色→资源，按租户 domain 隔离）+ ABAC 属性条件引擎（SpEL）         |
| 细粒度授权   | 目录 / 页面 / 按钮权限点 / 接口资源四类资源统一建模，菜单驱动前端渲染              |
| 行级数据权限 | 角色数据范围（全部 / 本部门 / 本部门及下级 / 本人 / 自定义），SQL 解析阶段注入条件 |
| 列级数据权限 | 通过接口资源 + ABAC 条件收敛可见能力（如脱敏、导出限制）                           |
| 性能         | Caffeine L1 + Redis L2 两级缓存，本地命中为微秒级；策略变更原子重建 + 缓存失效     |
| 安全审计     | 登录日志、租户状态与到期校验、JWT 登出黑名单、请求链路隔离                         |

## 二、目录结构

```
.
├── backend/                        # Maven 多模块
│   ├── platform-common/            # 统一响应、异常、租户/用户上下文、注解、ABAC 上下文
│   ├── platform-tenant/            # 租户生命周期、隔离策略、Schema 开通、租户过滤器
│   ├── platform-permission/        # Casbin 引擎、用户/角色/菜单/部门、数据权限、ABAC 策略
│   ├── platform-auth/              # 登录/刷新/登出、JWT 与 Keycloak 双模式、Security 链
│   └── platform-boot/              # 启动类、拦截器装配、SQL、配置、演示业务
├── frontend/                       # Vue 3 管理控制台
├── docker/                         # Keycloak realm 等编排资产
├── scripts/verify.ps1              # 端到端验证脚本
├── docs/architecture.md            # 架构与关键设计说明
└── docker-compose.yml              # PostgreSQL + Redis + Keycloak + 应用
```

## 三、快速启动（无需 Docker）

环境要求：JDK 21、Maven 3.9+、Node 18+。

```bash
# 1. 后端（默认 dev profile：H2 内存库 + 本地缓存，开箱即用）
cd backend
mvn -DskipTests package
java -jar platform-boot/target/permission-platform.jar

# 2. 前端
cd frontend
npm install
npm run dev          # http://localhost:5173
```

后端启动后：

- 接口文档：<http://localhost:8080/swagger-ui.html>
- 健康检查：<http://localhost:8080/actuator/health>

### 演示账号（密码均为 `admin123`）

| 租户编码   | 账号         | 角色           | 数据范围                 |
| ---------- | ------------ | -------------- | ------------------------ |
| `platform` | `superadmin` | PLATFORM_ADMIN | 全部（可跨租户运维）     |
| `acme`     | `admin`      | TENANT_ADMIN   | 全部                     |
| `acme`     | `manager`    | DEPT_MANAGER   | 本部门（研发中心）及下级 |
| `acme`     | `analyst`    | ANALYST        | 仅本人                   |

## 四、端到端验证

后端启动后执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify.ps1
```

实测结果：

```
login acme/admin -> user=admin tenant=acme roles=TENANT_ADMIN superAdmin=False
userinfo -> roles=TENANT_ADMIN perms=26 menus=3 dataScope=ALL
orders admin(ALL) -> total=6
orders manager(DEPT_AND_CHILD) -> total=4 depts=101,101,102,102
orders analyst(SELF) -> total=2 createBy=1003,1003
analyst -> GET /api/system/users status=403 (expect 403)
admin -> GET /api/system/users total=3
admin -> GET /api/tenants status=403 (expect 403)
superadmin -> GET /api/tenants total=2
no token -> GET /api/demo/orders status=401 (expect 401)
```

另验证了独立 Schema 租户开通全链路：`POST /api/tenants`（`isolationMode=SCHEMA`）→
自动创建 `tenant_globex` schema、建表、写入初始管理员与策略 → 该租户管理员登录后
读写全部落在自己的 schema 内。

## 五、Docker 一键编排

```bash
docker compose up -d --build
# 前端      http://localhost:5173
# 后端      http://localhost:8080
# Keycloak  http://localhost:8180  (admin / admin)
```

切换 Keycloak 验签：

```bash
PLATFORM_AUTH_MODE=keycloak docker compose up -d backend
```

## 六、主要接口

| 方法                | 路径                           | 说明                                   |
| ------------------- | ------------------------------ | -------------------------------------- |
| POST                | `/api/auth/login`              | 租户编码 + 账号密码登录                |
| POST                | `/api/auth/refresh`            | 刷新访问令牌                           |
| POST                | `/api/auth/logout`             | 登出（jti 加入黑名单）                 |
| GET                 | `/api/auth/userinfo`           | 当前用户角色、权限点、菜单树、数据范围 |
| GET/POST/PUT/DELETE | `/api/tenants/**`              | 租户管理（仅平台超管）                 |
| GET/POST/PUT/DELETE | `/api/system/users/**`         | 用户管理                               |
| GET/POST/PUT/DELETE | `/api/system/roles/**`         | 角色与授权（含菜单授权）               |
| GET/POST/PUT/DELETE | `/api/system/menus/**`         | 菜单/资源管理                          |
| GET/POST/PUT/DELETE | `/api/system/depts/**`         | 组织架构                               |
| GET/POST/PUT/DELETE | `/api/system/abac-policies/**` | ABAC 条件策略                          |
| GET                 | `/api/system/login-logs`       | 登录审计                               |
| GET                 | `/api/demo/orders`             | 行级数据权限演示（自动过滤）           |
| GET                 | `/api/demo/orders/scope`       | 当前用户数据范围画像                   |

## 七、关键设计取舍

1. **拦截器顺序**：动态表名 → 多租户 → 数据权限 → 分页。顺序错误会导致 SQL 语义偏差，
   例如分页先于租户会导致 count 语句缺少隔离条件。
2. **策略真相源**：`sys_role_menu` 是授权的唯一事实来源，保存后重建该角色的 Casbin 策略，
   避免"菜单改了但权限没变"的不一致。
3. **独立 Schema 的 Casbin 策略仍写全局表**：策略引擎是全局单实例、以租户编码作为 domain，
   因此策略表不随租户 Schema 走，只有业务表跟随。
4. **权限缓存可降级**：Redis 不可用时自动退回单机缓存，鉴权正确性不受影响，仅损失跨实例一致性。
5. **请求头不可信**：`X-Tenant-Id` 仅用于登录/无状态场景，一旦携带令牌，以令牌中的租户声明为准。

更多细节见 [docs/architecture.md](docs/architecture.md)。

## 八、生产落地建议

- 将 `platform.auth.secret`、数据库与 Redis 口令全部改为密钥管理服务注入。
- URL 级统一拦截建议下沉到网关（按 Casbin 策略 + 租户 domain 判定），应用层保留方法级校验。
- Schema 开通改为异步任务 + Flyway 多 Schema 版本化迁移，避免同步 DDL 阻塞请求。
- 接入 Prometheus 指标（鉴权耗时、缓存命中率、策略数量）与分布式追踪。
- 权限变更走审批流，并保留变更审计表，满足等保/审计要求。
