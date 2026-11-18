-- =====================================================================
-- 演示种子数据
-- 账号密码统一为 admin123（启动时由 PasswordBootstrapRunner 转为 BCrypt）
-- =====================================================================

-- ---------------------------------------------------------------- 租户
insert into sys_tenant (id, code, name, isolation_mode, schema_name, status, expire_at, contact_name,
                        contact_phone, max_users, remark, create_time, update_time, create_by, update_by, deleted)
values (1, 'platform', '平台运营中心', 'SHARED', null, 1, null, '平台运维', '13800000000', 50, '平台侧租户，承载超管与租户管理',
        current_timestamp, current_timestamp, null, null, 0),
       (2, 'acme', 'Acme 集团', 'SHARED', null, 1, '2030-12-31 23:59:59', '张伟', '13900000001', 500,
        '共享表隔离演示租户', current_timestamp, current_timestamp, null, null, 0);

-- ---------------------------------------------------------------- 部门
insert into sys_dept (id, tenant_id, parent_id, name, path, sort, leader, status,
                      create_time, update_time, create_by, update_by, deleted)
values (10, 1, 0, '平台运营部', '/10/', 1, '平台负责人', 1, current_timestamp, current_timestamp, null, null, 0),
       (100, 2, 0, 'Acme 总部', '/100/', 1, '张伟', 1, current_timestamp, current_timestamp, null, null, 0),
       (101, 2, 100, '研发中心', '/100/101/', 1, '李强', 1, current_timestamp, current_timestamp, null, null, 0),
       (102, 2, 101, '平台研发部', '/100/101/102/', 1, '王芳', 1, current_timestamp, current_timestamp, null, null, 0),
       (103, 2, 100, '销售部', '/100/103/', 2, '赵敏', 1, current_timestamp, current_timestamp, null, null, 0);

-- ---------------------------------------------------------------- 用户
insert into sys_user (id, tenant_id, dept_id, username, nickname, password, email, phone, avatar,
                      status, super_admin, last_login_time, create_time, update_time, create_by, update_by, deleted)
values (1, 1, 10, 'superadmin', '平台超管', 'admin123', 'super@platform.com', '13800000000', null,
        1, true, null, current_timestamp, current_timestamp, null, null, 0),
       (1001, 2, 100, 'admin', '系统管理员', 'admin123', 'admin@acme.com', '13900000001', null,
        1, false, null, current_timestamp, current_timestamp, null, null, 0),
       (1002, 2, 101, 'manager', '研发主管', 'admin123', 'manager@acme.com', '13900000002', null,
        1, false, null, current_timestamp, current_timestamp, null, null, 0),
       (1003, 2, 102, 'analyst', '数据分析师', 'admin123', 'analyst@acme.com', '13900000003', null,
        1, false, null, current_timestamp, current_timestamp, null, null, 0);

-- ---------------------------------------------------------------- 角色
insert into sys_role (id, tenant_id, code, name, data_scope, custom_dept_ids, status, builtin, remark,
                      create_time, update_time, create_by, update_by, deleted)
values (10, 1, 'PLATFORM_ADMIN', '平台管理员', 'ALL', null, 1, true, '跨租户运维角色',
        current_timestamp, current_timestamp, null, null, 0),
       (200, 2, 'TENANT_ADMIN', '租户管理员', 'ALL', null, 1, true, '租户内最高权限',
        current_timestamp, current_timestamp, null, null, 0),
       (201, 2, 'DEPT_MANAGER', '部门主管', 'DEPT_AND_CHILD', null, 1, false, '可见本部门及下级部门数据',
        current_timestamp, current_timestamp, null, null, 0),
       (202, 2, 'ANALYST', '数据分析师', 'SELF', null, 1, false, '仅可见本人数据',
        current_timestamp, current_timestamp, null, null, 0);

-- ---------------------------------------------------------------- 菜单（平台租户）
insert into sys_menu (id, tenant_id, parent_id, name, code, type, path, component, icon, http_method,
                      sort, visible, status, create_time, update_time, create_by, update_by, deleted)
values (400, 1, 0, '平台运维', null, 'DIR', '/platform', null, 'Platform', '*', 10, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (401, 1, 400, '租户管理', 'system:tenant:list', 'MENU', '/platform/tenants', 'platform/tenants', 'OfficeBuilding',
        '*', 1, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (402, 1, 0, '工作台', 'dashboard:view', 'MENU', '/dashboard', 'dashboard', 'Odometer', '*', 0, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (4401, 1, 401, '新增租户', 'system:tenant:create', 'BUTTON', null, null, null, '*', 1, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (4402, 1, 401, '编辑租户', 'system:tenant:update', 'BUTTON', null, null, null, '*', 2, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (4403, 1, 401, '删除租户', 'system:tenant:delete', 'BUTTON', null, null, null, '*', 3, 1, 1,
        current_timestamp, current_timestamp, null, null, 0);

-- ---------------------------------------------------------------- 菜单（Acme 租户）
insert into sys_menu (id, tenant_id, parent_id, name, code, type, path, component, icon, http_method,
                      sort, visible, status, create_time, update_time, create_by, update_by, deleted)
values (320, 2, 0, '工作台', 'dashboard:view', 'MENU', '/dashboard', 'dashboard', 'Odometer', '*', 0, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (300, 2, 0, '系统管理', null, 'DIR', '/system', null, 'Setting', '*', 10, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (301, 2, 300, '用户管理', 'system:user:list', 'MENU', '/system/users', 'system/users', 'User', '*', 1, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (302, 2, 300, '角色权限', 'system:role:list', 'MENU', '/system/roles', 'system/roles', 'Avatar', '*', 2, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (303, 2, 300, '菜单资源', 'system:menu:list', 'MENU', '/system/menus', 'system/menus', 'Menu', '*', 3, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (304, 2, 300, '组织架构', 'system:dept:list', 'MENU', '/system/depts', 'system/depts', 'Share', '*', 4, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (305, 2, 300, 'ABAC 策略', 'system:abac:list', 'MENU', '/system/abac', 'system/abac', 'Lock', '*', 5, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (306, 2, 300, '登录审计', 'system:log:list', 'MENU', '/system/logs', 'system/logs', 'Document', '*', 6, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (310, 2, 0, '业务演示', null, 'DIR', '/demo', null, 'DataAnalysis', '*', 20, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (311, 2, 310, '订单数据权限', 'demo:order:list', 'MENU', '/demo/orders', 'demo/orders', 'Tickets', '*', 1, 1, 1,
        current_timestamp, current_timestamp, null, null, 0);

insert into sys_menu (id, tenant_id, parent_id, name, code, type, path, component, icon, http_method,
                      sort, visible, status, create_time, update_time, create_by, update_by, deleted)
values (3301, 2, 301, '新增用户', 'system:user:create', 'BUTTON', null, null, null, '*', 1, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3302, 2, 301, '编辑用户', 'system:user:update', 'BUTTON', null, null, null, '*', 2, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3303, 2, 301, '删除用户', 'system:user:delete', 'BUTTON', null, null, null, '*', 3, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3304, 2, 301, '重置密码', 'system:user:reset', 'BUTTON', null, null, null, '*', 4, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3305, 2, 301, '分配角色', 'system:user:assign', 'BUTTON', null, null, null, '*', 5, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3311, 2, 302, '新增角色', 'system:role:create', 'BUTTON', null, null, null, '*', 1, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3312, 2, 302, '编辑角色', 'system:role:update', 'BUTTON', null, null, null, '*', 2, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3313, 2, 302, '删除角色', 'system:role:delete', 'BUTTON', null, null, null, '*', 3, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3314, 2, 302, '授权菜单', 'system:role:assign', 'BUTTON', null, null, null, '*', 4, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3321, 2, 303, '新增资源', 'system:menu:create', 'BUTTON', null, null, null, '*', 1, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3322, 2, 303, '编辑资源', 'system:menu:update', 'BUTTON', null, null, null, '*', 2, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3323, 2, 303, '删除资源', 'system:menu:delete', 'BUTTON', null, null, null, '*', 3, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3331, 2, 304, '新增部门', 'system:dept:create', 'BUTTON', null, null, null, '*', 1, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3332, 2, 304, '编辑部门', 'system:dept:update', 'BUTTON', null, null, null, '*', 2, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3333, 2, 304, '删除部门', 'system:dept:delete', 'BUTTON', null, null, null, '*', 3, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3341, 2, 305, '新增策略', 'system:abac:create', 'BUTTON', null, null, null, '*', 1, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3342, 2, 305, '编辑策略', 'system:abac:update', 'BUTTON', null, null, null, '*', 2, 1, 1, current_timestamp, current_timestamp, null, null, 0),
       (3343, 2, 305, '删除策略', 'system:abac:delete', 'BUTTON', null, null, null, '*', 3, 1, 1, current_timestamp, current_timestamp, null, null, 0);

-- ---------------------------------------------------------------- 用户-角色
insert into sys_user_role (id, tenant_id, user_id, role_id)
values (7001, 1, 1, 10),
       (7002, 2, 1001, 200),
       (7003, 2, 1002, 201),
       (7004, 2, 1003, 202);

-- ---------------------------------------------------------------- 角色-菜单
insert into sys_role_menu (id, tenant_id, role_id, menu_id)
values (8001, 1, 10, 400),
       (8002, 1, 10, 401),
       (8003, 1, 10, 402),
       (8004, 1, 10, 4401),
       (8005, 1, 10, 4402),
       (8006, 1, 10, 4403),
       (8100, 2, 200, 320),
       (8101, 2, 200, 300),
       (8102, 2, 200, 301),
       (8103, 2, 200, 302),
       (8104, 2, 200, 303),
       (8105, 2, 200, 304),
       (8106, 2, 200, 305),
       (8107, 2, 200, 306),
       (8108, 2, 200, 310),
       (8109, 2, 200, 311),
       (8110, 2, 200, 3301),
       (8111, 2, 200, 3302),
       (8112, 2, 200, 3303),
       (8113, 2, 200, 3304),
       (8114, 2, 200, 3305),
       (8115, 2, 200, 3311),
       (8116, 2, 200, 3312),
       (8117, 2, 200, 3313),
       (8118, 2, 200, 3314),
       (8119, 2, 200, 3321),
       (8120, 2, 200, 3322),
       (8121, 2, 200, 3323),
       (8122, 2, 200, 3331),
       (8123, 2, 200, 3332),
       (8124, 2, 200, 3333),
       (8125, 2, 200, 3341),
       (8126, 2, 200, 3342),
       (8127, 2, 200, 3343),
       (8200, 2, 201, 320),
       (8201, 2, 201, 300),
       (8202, 2, 201, 301),
       (8203, 2, 201, 304),
       (8204, 2, 201, 310),
       (8205, 2, 201, 311),
       (8300, 2, 202, 320),
       (8301, 2, 202, 310),
       (8302, 2, 202, 311);

-- ---------------------------------------------------------------- Casbin 策略
-- p: 角色, 租户域, 资源, 动作, 效果
insert into sys_casbin_rule (id, tenant_id, ptype, v0, v1, v2, v3, v4, v5)
values (9001, 1, 'p', 'PLATFORM_ADMIN', 'platform', '*', '*', 'allow', null),
       (9002, 2, 'p', 'TENANT_ADMIN', 'acme', '*', '*', 'allow', null),
       (9003, 2, 'p', 'DEPT_MANAGER', 'acme', 'system:user:list', '*', 'allow', null),
       (9004, 2, 'p', 'DEPT_MANAGER', 'acme', 'system:dept:list', '*', 'allow', null),
       (9005, 2, 'p', 'DEPT_MANAGER', 'acme', 'demo:order:list', '*', 'allow', null),
       (9006, 2, 'p', 'ANALYST', 'acme', 'demo:order:list', '*', 'allow', null),
       (9007, 2, 'p', 'ANALYST', 'acme', 'dashboard:view', '*', 'allow', null),
       (9008, 2, 'p', 'DEPT_MANAGER', 'acme', 'dashboard:view', '*', 'allow', null),
       (9009, 2, 'p', 'TENANT_ADMIN', 'acme', 'demo:order:list', '*', 'allow', null);

-- g: 主体, 角色, 租户域
insert into sys_casbin_rule (id, tenant_id, ptype, v0, v1, v2, v3, v4, v5)
values (9101, 1, 'g', 'u:1', 'PLATFORM_ADMIN', 'platform', null, null, null),
       (9102, 2, 'g', 'u:1001', 'TENANT_ADMIN', 'acme', null, null, null),
       (9103, 2, 'g', 'u:1002', 'DEPT_MANAGER', 'acme', null, null, null),
       (9104, 2, 'g', 'u:1003', 'ANALYST', 'acme', null, null, null);

-- ---------------------------------------------------------------- ABAC 条件策略
insert into sys_api_policy (id, tenant_id, name, resource_pattern, http_method, condition_expr, effect, status,
                            priority, remark, create_time, update_time, create_by, update_by, deleted)
values (6001, 2, '登录审计始终可访问', 'system:log:list', '*', 'hour >= 0', 'allow', 1, 10,
        '示例：恒为真，用于验证 ABAC 求值链路', current_timestamp, current_timestamp, null, null, 0),
       (6002, 2, '订单数据仅工作时间可查', 'demo:order:list', '*', 'hour >= 9 && hour < 19', 'allow', 0, 20,
        '示例：默认关闭。开启后 19:00-09:00 将拒绝访问订单接口', current_timestamp, current_timestamp, null, null, 0);

-- ---------------------------------------------------------------- 演示订单
insert into biz_order (id, tenant_id, dept_id, order_no, customer, amount, status,
                       create_time, update_time, create_by, update_by, deleted)
values (5001, 2, 101, 'ACME-2026-0001', '星辰科技', 128000.00, 'PAID',
        current_timestamp, current_timestamp, 1002, null, 0),
       (5002, 2, 101, 'ACME-2026-0002', '云图信息', 56000.00, 'PAID',
        current_timestamp, current_timestamp, 1002, null, 0),
       (5003, 2, 102, 'ACME-2026-0003', '联创网络', 89000.00, 'PENDING',
        current_timestamp, current_timestamp, 1003, null, 0),
       (5004, 2, 102, 'ACME-2026-0004', '恒信集团', 230000.00, 'PAID',
        current_timestamp, current_timestamp, 1003, null, 0),
       (5005, 2, 100, 'ACME-2026-0005', '中远物流', 450000.00, 'PAID',
        current_timestamp, current_timestamp, 1001, null, 0),
       (5006, 2, 103, 'ACME-2026-0006', '蓝海传媒', 78000.00, 'CANCELLED',
        current_timestamp, current_timestamp, 1001, null, 0);
