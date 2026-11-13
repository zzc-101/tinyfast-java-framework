package com.zc.base.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SysRoleDTO {

    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private Integer sort;

    private Integer status;

    private String remark;

    /** 菜单ID列表，传 null 表示不修改菜单分配 */
    private List<Long> menuIds;

}
