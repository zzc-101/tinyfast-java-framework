package com.zc.base.system.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.zc.base.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class SysRole extends BaseEntity {

    /** 角色标识 */
    private String roleKey;

    /** 角色名称 */
    private String roleName;

    /** 排序 */
    private Integer sort;

    /** 状态(1:正常 0:禁用) */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;

}
