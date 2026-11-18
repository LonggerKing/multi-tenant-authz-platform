-- =====================================================================
-- 企业级多租户权限中台 - 主库表结构
-- 兼容 PostgreSQL 与 H2(PostgreSQL 模式)，用于本地开发与容器化部署
-- =====================================================================

-- ---------------------------------------------------------------- 租户
create table if not exists sys_tenant
(
    id             bigint       not null primary key,
    code           varchar(32)  not null,
    name           varchar(64)  not null,
    isolation_mode varchar(16)  not null default 'SHARED',
    schema_name    varchar(64),
    status         smallint     not null default 1,
    expire_at      timestamp,
    contact_name   varchar(64),
    contact_phone  varchar(32),
    max_users      integer,
    remark         varchar(255),
    create_time    timestamp,
    update_time    timestamp,
    create_by      bigint,
    update_by      bigint,
    deleted        smallint     not null default 0
);
create unique index if not exists uk_tenant_code on sys_tenant (code);

-- ---------------------------------------------------------------- 部门
create table if not exists sys_dept
(
    id          bigint      not null primary key,
    tenant_id   bigint      not null,
    parent_id   bigint      not null default 0,
    name        varchar(64) not null,
    path        varchar(255),
    sort        integer              default 0,
    leader      varchar(64),
    status      smallint    not null default 1,
    create_time timestamp,
    update_time timestamp,
    create_by   bigint,
    update_by   bigint,
    deleted     smallint    not null default 0
);
create index if not exists idx_dept_tenant on sys_dept (tenant_id);

-- ---------------------------------------------------------------- 用户
create table if not exists sys_user
(
    id              bigint       not null primary key,
    tenant_id       bigint       not null,
    dept_id         bigint,
    username        varchar(32)  not null,
    nickname        varchar(64),
    password        varchar(128) not null,
    email           varchar(128),
    phone           varchar(32),
    avatar          varchar(255),
    status          smallint     not null default 1,
    super_admin     boolean      not null default false,
    last_login_time timestamp,
    create_time     timestamp,
    update_time     timestamp,
    create_by       bigint,
    update_by       bigint,
    deleted         smallint     not null default 0
);
create unique index if not exists uk_user_username on sys_user (tenant_id, username);
create index if not exists idx_user_dept on sys_user (tenant_id, dept_id);

-- ---------------------------------------------------------------- 角色
create table if not exists sys_role
(
    id              bigint      not null primary key,
    tenant_id       bigint      not null,
    code            varchar(32) not null,
    name            varchar(64) not null,
    data_scope      varchar(32) not null default 'SELF',
    custom_dept_ids varchar(512),
    status          smallint    not null default 1,
    builtin         boolean     not null default false,
    remark          varchar(255),
    create_time     timestamp,
    update_time     timestamp,
    create_by       bigint,
    update_by       bigint,
    deleted         smallint    not null default 0
);
create unique index if not exists uk_role_code on sys_role (tenant_id, code);

-- ---------------------------------------------------------------- 菜单 / 资源
create table if not exists sys_menu
(
    id          bigint      not null primary key,
    tenant_id   bigint      not null,
    parent_id   bigint      not null default 0,
    name        varchar(64) not null,
    code        varchar(64),
    type        varchar(16) not null default 'MENU',
    path        varchar(255),
    component   varchar(255),
    icon        varchar(64),
    http_method varchar(16)          default '*',
    sort        integer              default 0,
    visible     smallint    not null default 1,
    status      smallint    not null default 1,
    create_time timestamp,
    update_time timestamp,
    create_by   bigint,
    update_by   bigint,
    deleted     smallint    not null default 0
);
create index if not exists idx_menu_tenant on sys_menu (tenant_id, parent_id);

-- ---------------------------------------------------------------- 关联表
create table if not exists sys_user_role
(
    id        bigint not null primary key,
    tenant_id bigint not null,
    user_id   bigint not null,
    role_id   bigint not null
);
create unique index if not exists uk_user_role on sys_user_role (user_id, role_id);
create index if not exists idx_user_role_role on sys_user_role (role_id);

create table if not exists sys_role_menu
(
    id        bigint not null primary key,
    tenant_id bigint not null,
    role_id   bigint not null,
    menu_id   bigint not null
);
create unique index if not exists uk_role_menu on sys_role_menu (role_id, menu_id);

-- ---------------------------------------------------------------- Casbin 策略
create table if not exists sys_casbin_rule
(
    id        bigint      not null primary key,
    tenant_id bigint      not null,
    ptype     varchar(8)  not null,
    v0        varchar(255),
    v1        varchar(255),
    v2        varchar(255),
    v3        varchar(255),
    v4        varchar(255),
    v5        varchar(255)
);
create index if not exists idx_casbin_tenant on sys_casbin_rule (tenant_id, ptype);

-- ---------------------------------------------------------------- ABAC 条件策略
create table if not exists sys_api_policy
(
    id               bigint       not null primary key,
    tenant_id        bigint       not null,
    name             varchar(64)  not null,
    resource_pattern varchar(255) not null,
    http_method      varchar(16)           default '*',
    condition_expr   varchar(512),
    effect           varchar(16)  not null default 'allow',
    status           smallint     not null default 1,
    priority         integer               default 100,
    remark           varchar(255),
    create_time      timestamp,
    update_time      timestamp,
    create_by        bigint,
    update_by        bigint,
    deleted          smallint     not null default 0
);
create index if not exists idx_api_policy_tenant on sys_api_policy (tenant_id);

-- ---------------------------------------------------------------- 登录审计
create table if not exists sys_login_log
(
    id         bigint      not null primary key,
    tenant_id  bigint      not null,
    username   varchar(64),
    ip         varchar(64),
    user_agent varchar(255),
    success    boolean,
    message    varchar(255),
    create_time timestamp
);
create index if not exists idx_login_log_tenant on sys_login_log (tenant_id, create_time);

-- ---------------------------------------------------------------- 演示业务表
create table if not exists biz_order
(
    id          bigint         not null primary key,
    tenant_id   bigint         not null,
    dept_id     bigint,
    order_no    varchar(64)    not null,
    customer    varchar(128),
    amount      numeric(12, 2) not null default 0,
    status      varchar(16),
    create_time timestamp,
    update_time timestamp,
    create_by   bigint,
    update_by   bigint,
    deleted     smallint       not null default 0
);
create index if not exists idx_order_tenant on biz_order (tenant_id, dept_id);
