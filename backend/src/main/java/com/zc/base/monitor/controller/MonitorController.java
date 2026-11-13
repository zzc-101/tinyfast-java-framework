package com.zc.base.monitor.controller;

import com.zc.base.common.core.R;
import com.zc.base.log.annotation.OperationLog;
import com.zc.base.monitor.service.OnlineUserService;
import com.zc.base.monitor.service.ServerMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统监控
 */
@Tag(name = "系统监控")
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ServerMonitorService serverMonitorService;
    private final OnlineUserService onlineUserService;

    @Operation(summary = "获取服务器信息")
    @GetMapping("/server")
    @PreAuthorize("hasAuthority('monitor:server:list')")
    public R<ServerMonitorService.ServerInfo> serverInfo() {
        return R.success(serverMonitorService.getServerInfo());
    }

    @Operation(summary = "获取在线用户列表")
    @GetMapping("/online-users")
    @PreAuthorize("hasAuthority('monitor:online:list')")
    public R<List<OnlineUserService.OnlineUser>> onlineUsers() {
        return R.success(onlineUserService.getOnlineUsers());
    }

    @Operation(summary = "强退用户")
    @DeleteMapping("/online-users/{userId}")
    @PreAuthorize("hasAuthority('monitor:online:forceLogout')")
    @OperationLog(module = "系统监控", description = "强退用户")
    public R<Void> forceLogout(@PathVariable Long userId) {
        onlineUserService.forceLogout(userId);
        return R.success();
    }

}
