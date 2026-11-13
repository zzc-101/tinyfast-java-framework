package com.zc.base.system.dto;

import com.zc.base.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @StrongPassword
    private String newPassword;

}
