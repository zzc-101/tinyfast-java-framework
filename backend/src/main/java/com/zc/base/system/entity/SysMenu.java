package com.zc.base.system.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.zc.base.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_menu")
public class SysMenu extends BaseEntity {

    /** 父菜单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 类型(D:目录 M:菜单 B:按钮) */
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识 */
    private String permission;

    /** 菜单图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 是否可见 */
    private Integer visible;

    /** 状态 */
    private Integer status;

    /** 子菜单，仅构建菜单树时填充，不对应数据库字段 */
    @Column(ignore = true)
    private List<SysMenu> children;

}
