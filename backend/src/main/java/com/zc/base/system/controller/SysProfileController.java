package com.zc.base.system.controller;

import com.zc.base.common.core.R;
import com.zc.base.log.annotation.OperationLog;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.security.util.SecurityUtil;
import com.zc.base.system.dto.ChangePasswordDTO;
import com.zc.base.system.dto.ProfileDTO;
import com.zc.base.system.dto.ProfileUpdateDTO;
import com.zc.base.system.service.SysProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心：只操作当前登录用户自己的数据，只要求已登录，不需要权限标识
 */
@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/system/profile")
@RequiredArgsConstructor
public class SysProfileController {

    private final SysProfileService profileService;

    @Operation(summary = "查看个人资料")
    @GetMapping
    public R<ProfileDTO> get() {
        return R.success(profileService.get(currentUserId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping
    @OperationLog(module = "个人中心", description = "修改个人资料")
    public R<Void> update(@Valid @RequestBody ProfileUpdateDTO dto) {
        profileService.update(currentUserId(), dto);
        return R.success();
    }

    @Operation(summary = "修改密码，成功后需要重新登录")
    @PutMapping("/password")
    @OperationLog(module = "个人中心", description = "修改密码", saveParams = false)
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        profileService.changePassword(currentUserId(), dto);
        return R.success();
    }

    /** 用户ID只取自登录上下文 */
    private Long currentUserId() {
        return SecurityUtil.getCurrentUserId().orElseThrow(() -> new BusinessException(401, "未登录或登录已过期"));
    }

}
