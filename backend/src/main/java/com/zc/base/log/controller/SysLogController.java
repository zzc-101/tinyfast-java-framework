package com.zc.base.log.controller;

import com.zc.base.common.core.PageQuery;
import com.zc.base.common.core.PageResult;
import com.zc.base.common.core.R;
import com.zc.base.log.annotation.OperationLog;
import com.zc.base.log.entity.SysLoginLog;
import com.zc.base.log.entity.SysOperationLog;
import com.zc.base.log.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 日志管理
 */
@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService logService;

    @Operation(summary = "操作日志分页")
    @GetMapping("/operation")
    @PreAuthorize("hasAuthority('log:operation:list')")
    public R<PageResult<SysOperationLog>> pageOperationLog(
            PageQuery query,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status) {
        return R.success(logService.pageOperationLog(query, module, status));
    }

    @Operation(summary = "登录日志分页")
    @GetMapping("/login")
    @PreAuthorize("hasAuthority('log:login:list')")
    public R<PageResult<SysLoginLog>> pageLoginLog(
            PageQuery query,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return R.success(logService.pageLoginLog(query, username, status));
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping("/operation/clear")
    @PreAuthorize("hasAuthority('log:operation:clear')")
    @OperationLog(module = "日志管理", description = "清空操作日志")
    public R<Void> clearOperationLog() {
        logService.clearOperationLog();
        return R.success();
    }

    @Operation(summary = "清空登录日志")
    @DeleteMapping("/login/clear")
    @PreAuthorize("hasAuthority('log:login:clear')")
    @OperationLog(module = "日志管理", description = "清空登录日志")
    public R<Void> clearLoginLog() {
        logService.clearLoginLog();
        return R.success();
    }

}
