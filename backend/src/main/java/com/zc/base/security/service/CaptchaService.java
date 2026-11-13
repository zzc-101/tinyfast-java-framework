package com.zc.base.security.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zc.base.common.config.properties.SecurityProperties;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.RedisCache;
import com.zc.base.security.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 登录图形验证码：答案存 Redis，2 分钟过期，校验一次即删除（无论对错）
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int EXPIRE_MINUTES = 2;
    /** 去掉容易看混的 0/o、1/l/i */
    private static final String CODE_CHARS = "23456789abcdefghjkmnpqrstuvwxyz";

    private final RedisCache redisCache;
    private final SecurityProperties securityProperties;

    /**
     * 生成验证码；未启用时只返回 enabled=false
     */
    public CaptchaResponse generate() {
        if (!isEnabled()) {
            return new CaptchaResponse(false, null, null);
        }
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        captcha.setGenerator(new RandomGenerator(CODE_CHARS, 4));
        captcha.createCode();

        String captchaId = IdUtil.fastSimpleUUID();
        redisCache.set(CAPTCHA_PREFIX + captchaId, captcha.getCode().toLowerCase(), EXPIRE_MINUTES, TimeUnit.MINUTES);
        return new CaptchaResponse(true, captchaId, captcha.getImageBase64Data());
    }

    /**
     * 校验验证码，不区分大小写；未启用时直接通过。校验失败抛 BusinessException(400)
     */
    public void verify(String captchaId, String code) {
        if (!isEnabled()) {
            return;
        }
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(code)) {
            throw new BusinessException(400, "请输入验证码");
        }
        String key = CAPTCHA_PREFIX + captchaId;
        Optional<String> answer = redisCache.get(key);
        // 无论对错都删除；删除成功的请求才有资格校验，并发重放同一个验证码只会有一个通过
        boolean consumed = redisCache.delete(key);
        if (answer.isEmpty() || !consumed) {
            throw new BusinessException(400, "验证码已过期，请刷新后重试");
        }
        if (!answer.get().equalsIgnoreCase(code.trim())) {
            throw new BusinessException(400, "验证码错误");
        }
    }

    private boolean isEnabled() {
        return securityProperties.getCaptcha().isEnabled();
    }

}
