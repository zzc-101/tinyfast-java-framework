package com.zc.base.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料。不接收用户ID、用户名、状态，这些不允许通过个人中心修改
 */
@Data
public class ProfileUpdateDTO {

    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    @Size(max = 128, message = "邮箱不能超过 128 个字符")
    @Email(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "邮箱格式不正确")
    private String email;

    /** 大陆手机号，允许为空 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 头像地址，只是字符串，不做文件上传 */
    @Size(max = 256, message = "头像地址不能超过 256 个字符")
    private String avatar;

}
