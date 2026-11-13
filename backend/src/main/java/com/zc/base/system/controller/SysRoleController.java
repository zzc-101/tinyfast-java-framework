package com.zc.base.system.controller;

import com.zc.base.common.core.PageQuery;
import com.zc.base.common.core.PageResult;
import com.zc.base.common.core.R;
import com.zc.base.log.annotation.OperationLog;
import com.zc.base.system.dto.SysRoleDTO;
import com.zc.base.system.entity.SysRole;
import com.zc.base.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色分页")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<PageResult<SysRole>> page(PageQuery query) {
        return R.success(roleService.page(query));
    }

    @Operation(summary = "全部启用角色，供用户分配角色时选择")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('system:role:list', 'system:user:add', 'system:user:edit')")
    public R<List<SysRole>> listAll() {
        return R.success(roleService.listAll());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.success(roleService.getById(id));
    }

    @Operation(summary = "角色已分配的菜单ID")
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAnyAuthority('system:role:list', 'system:role:edit')")
    public R<List<String>> listMenuIds(@PathVariable Long id) {
        return R.success(roleService.listMenuIds(id).stream().map(String::valueOf).toList());
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    @OperationLog(module = "角色管理", description = "新增角色")
    public R<Void> create(@Valid @RequestBody SysRoleDTO dto) {
        roleService.create(dto);
        return R.success();
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @OperationLog(module = "角色管理", description = "修改角色")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysRoleDTO dto) {
        roleService.update(id, dto);
        return R.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @OperationLog(module = "角色管理", description = "删除角色")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.success();
    }

}
