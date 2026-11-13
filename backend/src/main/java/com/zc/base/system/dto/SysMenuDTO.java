package com.zc.base.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SysMenuDTO {

    /** 父菜单ID，不传或 0 表示顶级 */
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "[DMB]", message = "菜单类型只能是 D、M、B")
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识，格式 模块:资源:操作 */
    private String permission;

    /** 菜单图标 */
    private String icon;

    private Integer sort;

    /** 是否可见(1:是 0:否) */
    private Integer visible;

    /** 状态(1:正常 0:禁用) */
    private Integer status;

}
