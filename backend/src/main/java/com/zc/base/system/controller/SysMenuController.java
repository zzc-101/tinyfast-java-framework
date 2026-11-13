package com.zc.base.system.controller;

import com.zc.base.common.core.R;
import com.zc.base.log.annotation.OperationLog;
import com.zc.base.system.dto.SysMenuDTO;
import com.zc.base.system.entity.SysMenu;
import com.zc.base.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "菜单树")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('system:menu:list', 'system:role:add', 'system:role:edit')")
    public R<List<SysMenu>> tree() {
        return R.success(menuService.tree());
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    @OperationLog(module = "菜单管理", description = "新增菜单")
    public R<Void> create(@Valid @RequestBody SysMenuDTO dto) {
        menuService.create(dto);
        return R.success();
    }

    @Operation(summary = "修改菜单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @OperationLog(module = "菜单管理", description = "修改菜单")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysMenuDTO dto) {
        menuService.update(id, dto);
        return R.success();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @OperationLog(module = "菜单管理", description = "删除菜单")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.success();
    }

}
