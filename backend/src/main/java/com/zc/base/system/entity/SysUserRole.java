package com.zc.base.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户角色关联
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_user_role")
public class SysUserRole implements Serializable {

    @Id
    private Long userId;

    @Id
    private Long roleId;

}
