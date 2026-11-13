package com.zc.base.system.controller;

import com.zc.base.common.core.PageResult;
import com.zc.base.common.core.R;
import com.zc.base.log.annotation.OperationLog;
import com.zc.base.system.dto.ResetPasswordDTO;
import com.zc.base.system.dto.SysUserDTO;
import com.zc.base.system.dto.SysUserQueryDTO;
import com.zc.base.system.entity.SysUser;
import com.zc.base.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "用户分页")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<PageResult<SysUser>> page(SysUserQueryDTO query) {
        return R.success(userService.page(query));
    }

    @Operation(summary = "用户详情，含 roleIds")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<SysUser> getById(@PathVariable Long id) {
        return R.success(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @OperationLog(module = "用户管理", description = "新增用户", saveParams = false)
    public R<Void> create(@Validated(SysUserDTO.Create.class) @RequestBody SysUserDTO dto) {
        userService.create(dto);
        return R.success();
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @OperationLog(module = "用户管理", description = "修改用户", saveParams = false)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysUserDTO dto) {
        userService.update(id, dto);
        return R.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperationLog(module = "用户管理", description = "删除用户")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:resetPwd')")
    @OperationLog(module = "用户管理", description = "重置密码", saveParams = false)
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return R.success();
    }

}
