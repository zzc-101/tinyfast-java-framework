package com.zc.base.system.dto;

import com.zc.base.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;
import lombok.Data;

import java.util.List;

@Data
public class SysUserDTO {

    /** 新增用户的校验分组，在默认校验之外额外要求密码必填 */
    public interface Create extends Default {
    }

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 新增必填；修改时留空表示不改密码 */
    @NotBlank(message = "密码不能为空", groups = Create.class)
    @StrongPassword
    private String password;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    private Integer status;

    /** 角色ID列表 */
    private List<Long> roleIds;

}
