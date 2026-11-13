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
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
public class SysUser extends BaseEntity {

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 */
    private String avatar;

    /** 状态(1:正常 0:禁用) */
    private Integer status;

    /** 逻辑删除 */
    @Column(isLogicDelete = true)
    private Integer deleted;

    /** 角色ID列表，仅用户详情接口填充，不对应数据库字段 */
    @Column(ignore = true)
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> roleIds;

}
