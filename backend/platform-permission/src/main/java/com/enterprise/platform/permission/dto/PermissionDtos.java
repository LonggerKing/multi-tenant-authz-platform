package com.enterprise.platform.permission.dto;

import com.enterprise.platform.common.api.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限域 DTO 集合。
 */
public final class PermissionDtos {

    private PermissionDtos() {
    }

    /* ---------------- 用户 ---------------- */

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserQuery extends PageQuery {
        private String keyword;
        private Long deptId;
        private Integer status;
    }

    @Data
    public static class UserCreateRequest {
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$", message = "用户名需以字母开头，3-32 位字母/数字/下划线")
        private String username;

        @Size(max = 64)
        private String nickname;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
        private String password;

        private Long deptId;
        private String email;
        private String phone;
        private Integer status = 1;
        private List<Long> roleIds = new ArrayList<>();
    }

    @Data
    public static class UserUpdateRequest {
        @Size(max = 64)
        private String nickname;

        private Long deptId;
        private String email;
        private String phone;
        private Integer status;
        private List<Long> roleIds;
    }

    @Data
    public static class PasswordRequest {
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
        private String password;
    }

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
        private Long deptId;
        private String deptName;
        private String email;
        private String phone;
        private Integer status;
        private Boolean superAdmin;
        private LocalDateTime lastLoginTime;
        private LocalDateTime createTime;
        private List<Long> roleIds = new ArrayList<>();
        private List<String> roleCodes = new ArrayList<>();
        private List<String> roleNames = new ArrayList<>();
    }

    /* ---------------- 角色 ---------------- */

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class RoleQuery extends PageQuery {
        private String keyword;
        private Integer status;
    }

    @Data
    public static class RoleSaveRequest {
        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色编码需为大写字母开头的 2-32 位大写字母/数字/下划线")
        private String code;

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64)
        private String name;

        /** ALL / DEPT / DEPT_AND_CHILD / SELF / CUSTOM */
        private String dataScope = "SELF";

        private List<Long> customDeptIds = new ArrayList<>();

        private Integer status = 1;
        private String remark;
    }

    @Data
    public static class RoleVO {
        private Long id;
        private String code;
        private String name;
        private String dataScope;
        private List<Long> customDeptIds = new ArrayList<>();
        private Integer status;
        private Boolean builtin;
        private String remark;
        private List<Long> menuIds = new ArrayList<>();
    }

    @Data
    public static class IdsRequest {
        private List<Long> ids = new ArrayList<>();
    }

    /* ---------------- 菜单 ---------------- */

    @Data
    public static class MenuSaveRequest {
        private Long parentId = 0L;

        @NotBlank(message = "名称不能为空")
        private String name;

        private String code;
        /** DIR / MENU / BUTTON / API */
        private String type = "MENU";
        private String path;
        private String component;
        private String icon;
        private String httpMethod = "*";
        private Integer sort = 0;
        private Integer visible = 1;
        private Integer status = 1;
    }

    /* ---------------- 部门 ---------------- */

    @Data
    public static class DeptSaveRequest {
        private Long parentId = 0L;

        @NotBlank(message = "部门名称不能为空")
        private String name;

        private Integer sort = 0;
        private String leader;
        private Integer status = 1;
    }

    /* ---------------- 菜单树节点 ---------------- */

    @Data
    public static class MenuNode {
        private Long id;
        private Long parentId;
        private String name;
        private String code;
        private String type;
        private String path;
        private String component;
        private String icon;
        private String httpMethod;
        private Integer sort;
        private Integer visible;
        private List<MenuNode> children = new ArrayList<>();
    }

    /* ---------------- 部门树节点 ---------------- */

    @Data
    public static class DeptNode {
        private Long id;
        private Long parentId;
        private String name;
        private String path;
        private Integer sort;
        private String leader;
        private Integer status;
        private List<DeptNode> children = new ArrayList<>();
    }

    /* ---------------- 当前用户权限视图 ---------------- */

    @Data
    public static class UserPermissionView {
        private UserVO user;
        private List<String> roles = new ArrayList<>();
        private List<String> permissions = new ArrayList<>();
        private List<MenuNode> menus = new ArrayList<>();
        private String dataScope;
    }
}
