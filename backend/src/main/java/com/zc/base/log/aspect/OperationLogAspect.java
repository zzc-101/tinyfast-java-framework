package com.zc.base.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zc.base.common.util.ServletUtil;
import com.zc.base.log.annotation.OperationLog;
import com.zc.base.log.entity.SysOperationLog;
import com.zc.base.log.mapper.SysOperationLogMapper;
import com.zc.base.security.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog logEntity = new SysOperationLog();

        // 请求信息
        ServletUtil.getRequest().ifPresent(request -> {
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setOperatorIp(ServletUtil.getClientIp(request));
        });

        logEntity.setModule(operationLog.module());
        logEntity.setDescription(operationLog.description());
        logEntity.setOperatorId(SecurityUtil.getCurrentUserId().orElse(null));
        logEntity.setOperatorName(SecurityUtil.getCurrentUsername().orElse("anonymous"));

        // 请求参数
        if (operationLog.saveParams()) {
            try {
                Object[] args = point.getArgs();
                logEntity.setRequestParams(objectMapper.writeValueAsString(args));
            } catch (Exception e) {
                log.warn("操作日志序列化请求参数失败", e);
            }
        }

        Object result = null;
        try {
            result = point.proceed();
            logEntity.setStatus(1);

            // 响应结果
            if (operationLog.saveResult() && result != null) {
                try {
                    String responseStr = objectMapper.writeValueAsString(result);
                    // 限制响应结果长度
                    if (responseStr.length() > 2000) {
                        responseStr = responseStr.substring(0, 2000) + "...";
                    }
                    logEntity.setResponseBody(responseStr);
                } catch (Exception e) {
                    log.warn("操作日志序列化响应结果失败", e);
                }
            }
        } catch (Throwable e) {
            logEntity.setStatus(0);
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            logEntity.setCostTime(System.currentTimeMillis() - startTime);
            logEntity.setCreateTime(LocalDateTime.now());
            saveLog(logEntity);
        }

        return result;
    }

    /**
     * 同步保存日志。写入失败只记录错误，不影响业务请求。
     */
    private void saveLog(SysOperationLog logEntity) {
        try {
            logMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

}
