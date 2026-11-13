package com.zc.base.system.dto;

import com.zc.base.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQueryDTO extends PageQuery {

    /** 用户名（模糊搜索） */
    private String username;

    /** 手机号 */
    private String phone;

    /** 状态 */
    private Integer status;

}
