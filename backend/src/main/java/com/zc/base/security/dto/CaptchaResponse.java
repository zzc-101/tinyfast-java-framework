package com.zc.base.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /** 是否启用验证码；false 时登录不需要验证码，下面两个字段为空 */
    private boolean enabled;

    /** 验证码ID，登录时回传 */
    private String captchaId;

    /** base64 图片（data URI） */
    private String image;

}
