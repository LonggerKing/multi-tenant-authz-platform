-- =====================================================================
-- 独立 Schema 租户的表结构模板
-- {schema} 由 TenantSchemaInitializer 替换为实际 schema 名称
-- =====================================================================

create table if not exists {schema}.sys_dept
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

create table if not exists {schema}.sys_user
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
create unique index if not exists uk_{schema}_user_username on {schema}.sys_user (username);

create table if not exists {schema}.sys_role
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

create table if not exists {schema}.sys_menu
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

create table if not exists {schema}.sys_user_role
(
    id        bigint not null primary key,
    tenant_id bigint not null,
    user_id   bigint not null,
    role_id   bigint not null
);

create table if not exists {schema}.sys_role_menu
(
    id        bigint not null primary key,
    tenant_id bigint not null,
    role_id   bigint not null,
    menu_id   bigint not null
);

create table if not exists {schema}.sys_casbin_rule
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

create table if not exists {schema}.sys_api_policy
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

create table if not exists {schema}.sys_login_log
(
    id          bigint not null primary key,
    tenant_id   bigint not null,
    username    varchar(64),
    ip          varchar(64),
    user_agent  varchar(255),
    success     boolean,
    message     varchar(255),
    create_time timestamp
);

create table if not exists {schema}.biz_order
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
