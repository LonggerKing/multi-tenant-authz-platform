package com.enterprise.platform.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Casbin 策略持久化行（p / g 两种 ptype，v0~v5 为通用字段）。
 */
@Data
@TableName("sys_casbin_rule")
public class SysCasbinRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** p / g。 */
    private String ptype;

    private String v0;
    private String v1;
    private String v2;
    private String v3;
    private String v4;
    private String v5;

    public List<String> toRule() {
        List<String> rule = new ArrayList<>(6);
        for (String value : new String[]{v0, v1, v2, v3, v4, v5}) {
            if (value == null) {
                break;
            }
            rule.add(value);
        }
        return rule;
    }

    public static SysCasbinRule of(Long tenantId, String ptype, List<String> rule) {
        SysCasbinRule entity = new SysCasbinRule();
        entity.setTenantId(tenantId);
        entity.setPtype(ptype);
        entity.setV0(rule.size() > 0 ? rule.get(0) : null);
        entity.setV1(rule.size() > 1 ? rule.get(1) : null);
        entity.setV2(rule.size() > 2 ? rule.get(2) : null);
        entity.setV3(rule.size() > 3 ? rule.get(3) : null);
        entity.setV4(rule.size() > 4 ? rule.get(4) : null);
        entity.setV5(rule.size() > 5 ? rule.get(5) : null);
        return entity;
    }
}
