package com.zc.base.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码ID，验证码关闭时可不传 */
    private String captchaId;

    /** 验证码，验证码关闭时可不传 */
    private String captchaCode;

}
