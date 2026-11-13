package com.zc.base.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "base.security")
public class SecurityProperties {

    /** JWT 配置 */
    private Jwt jwt = new Jwt();

    /** 不需要认证的URL */
    private List<String> ignoreUrls = new ArrayList<>();

    /** 允许跨域访问的来源，默认不允许任何跨域 */
    private List<String> corsAllowedOrigins = new ArrayList<>();

    /** 登录图形验证码 */
    private Captcha captcha = new Captcha();

    /** 登录失败锁定 */
    private Login login = new Login();

    /** 鉴权用户信息缓存时间（分钟） */
    private int userCacheMinutes = 30;

    @Data
    public static class Jwt {
        /** 密钥，至少 32 个字符；无默认值，必须由各环境配置 */
        private String secret;
        /** access_token 过期时间（分钟） */
        private int accessTokenExpire = 120;
        /** refresh_token 过期时间（天） */
        private int refreshTokenExpire = 7;
    }

    @Data
    public static class Captcha {
        /** 是否启用登录验证码 */
        private boolean enabled = true;
    }

    @Data
    public static class Login {
        /** 同一用户名连续密码错误多少次后锁定 */
        private int maxFailCount = 5;
        /** 锁定时长（分钟） */
        private int lockMinutes = 15;
    }

}
