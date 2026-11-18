-- =====================================================================
-- 独立 Schema 租户的初始化数据（初始管理员 admin / admin123）
-- 占位符：{schema} {tenant_id} {tenant_code} {admin_hash}
-- =====================================================================

insert into {schema}.sys_dept (id, tenant_id, parent_id, name, path, sort, leader, status,
                               create_time, update_time, create_by, update_by, deleted)
values (100, {tenant_id}, 0, '总部', '/100/', 1, '管理员', 1, current_timestamp, current_timestamp, null, null, 0);

insert into {schema}.sys_user (id, tenant_id, dept_id, username, nickname, password, email, phone, avatar,
                               status, super_admin, last_login_time, create_time, update_time, create_by, update_by, deleted)
values (1000, {tenant_id}, 100, 'admin', '租户管理员', '{admin_hash}', null, null, null,
        1, false, null, current_timestamp, current_timestamp, null, null, 0);

insert into {schema}.sys_role (id, tenant_id, code, name, data_scope, custom_dept_ids, status, builtin, remark,
                               create_time, update_time, create_by, update_by, deleted)
values (200, {tenant_id}, 'TENANT_ADMIN', '租户管理员', 'ALL', null, 1, true, '独立 Schema 租户内置管理员',
        current_timestamp, current_timestamp, null, null, 0);

insert into {schema}.sys_user_role (id, tenant_id, user_id, role_id)
values (7002, {tenant_id}, 1000, 200);

insert into {schema}.sys_menu (id, tenant_id, parent_id, name, code, type, path, component, icon, http_method,
                               sort, visible, status, create_time, update_time, create_by, update_by, deleted)
values (320, {tenant_id}, 0, '工作台', 'dashboard:view', 'MENU', '/dashboard', 'dashboard', 'Odometer', '*', 0, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (300, {tenant_id}, 0, '系统管理', null, 'DIR', '/system', null, 'Setting', '*', 10, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (301, {tenant_id}, 300, '用户管理', 'system:user:list', 'MENU', '/system/users', 'system/users', 'User', '*', 1, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (302, {tenant_id}, 300, '角色权限', 'system:role:list', 'MENU', '/system/roles', 'system/roles', 'Avatar', '*', 2, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (303, {tenant_id}, 300, '菜单资源', 'system:menu:list', 'MENU', '/system/menus', 'system/menus', 'Menu', '*', 3, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (304, {tenant_id}, 300, '组织架构', 'system:dept:list', 'MENU', '/system/depts', 'system/depts', 'Share', '*', 4, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (305, {tenant_id}, 300, 'ABAC 策略', 'system:abac:list', 'MENU', '/system/abac', 'system/abac', 'Lock', '*', 5, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (306, {tenant_id}, 300, '登录审计', 'system:log:list', 'MENU', '/system/logs', 'system/logs', 'Document', '*', 6, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (310, {tenant_id}, 0, '业务演示', null, 'DIR', '/demo', null, 'DataAnalysis', '*', 20, 1, 1,
        current_timestamp, current_timestamp, null, null, 0),
       (311, {tenant_id}, 310, '订单数据权限', 'demo:order:list', 'MENU', '/demo/orders', 'demo/orders', 'Tickets', '*', 1, 1, 1,
        current_timestamp, current_timestamp, null, null, 0);

insert into {schema}.sys_role_menu (id, tenant_id, role_id, menu_id)
values (8100, {tenant_id}, 200, 320),
       (8101, {tenant_id}, 200, 300),
       (8102, {tenant_id}, 200, 301),
       (8103, {tenant_id}, 200, 302),
       (8104, {tenant_id}, 200, 303),
       (8105, {tenant_id}, 200, 304),
       (8106, {tenant_id}, 200, 305),
       (8107, {tenant_id}, 200, 306),
       (8108, {tenant_id}, 200, 310),
       (8109, {tenant_id}, 200, 311);
