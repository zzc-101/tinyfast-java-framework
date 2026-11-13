package com.zc.base.log.listener;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.zc.base.log.entity.SysLoginLog;
import com.zc.base.log.service.SysLogService;
import com.zc.base.security.event.LoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 登录日志监听：登录成功或失败都写入 sys_login_log
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogListener {

    private final SysLogService logService;

    @EventListener
    public void onLogin(LoginEvent event) {
        try {
            Optional<UserAgent> userAgent = Optional.ofNullable(UserAgentUtil.parse(event.userAgent()));

            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUsername(event.username());
            loginLog.setLoginIp(event.ip());
            loginLog.setBrowser(userAgent.map(ua -> ua.getBrowser().getName()).orElse(""));
            loginLog.setOs(userAgent.map(ua -> ua.getOs().getName()).orElse(""));
            loginLog.setStatus(event.success() ? 1 : 0);
            loginLog.setMessage(event.message());
            loginLog.setCreateTime(LocalDateTime.now());
            logService.recordLoginLog(loginLog);
        } catch (Exception e) {
            // 日志写失败不能影响登录
            log.error("保存登录日志失败", e);
        }
    }

}
