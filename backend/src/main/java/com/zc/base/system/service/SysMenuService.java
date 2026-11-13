package com.zc.base.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.dto.SysMenuDTO;
import com.zc.base.system.entity.SysMenu;
import com.zc.base.system.mapper.SysMenuMapper;
import com.zc.base.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zc.base.system.entity.table.SysMenuTableDef.SYS_MENU;
import static com.zc.base.system.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;

@Service
@RequiredArgsConstructor
public class SysMenuService {

    /** 顶级菜单的 parentId */
    private static final Long ROOT_ID = 0L;

    private static final String TYPE_BUTTON = "B";

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final AuthUserCache authUserCache;

    /**
     * 完整菜单树（含按钮、禁用项），供菜单管理和角色分配使用
     */
    public List<SysMenu> tree() {
        return buildTree(menuMapper.selectAll());
    }

    /**
     * 用户可见的菜单树：目录和菜单，不含按钮，仅启用且可见
     *
     * @param admin   是否超级管理员，true 时忽略 roleIds，取全部菜单
     * @param roleIds 用户的有效角色ID
     */
    public List<SysMenu> userMenuTree(boolean admin, Collection<Long> roleIds) {
        List<SysMenu> menus = listEnabled(admin, roleIds).stream()
                .filter(menu -> !TYPE_BUTTON.equals(menu.getMenuType()))
                .filter(menu -> Integer.valueOf(1).equals(menu.getVisible()))
                .toList();
        return buildTree(menus);
    }

    /**
     * 用户拥有的权限标识
     */
    public Set<String> listPermissions(boolean admin, Collection<Long> roleIds) {
        return listEnabled(admin, roleIds).stream()
                .map(SysMenu::getPermission)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SysMenuDTO dto) {
        Long parentId = Optional.ofNullable(dto.getParentId()).orElse(ROOT_ID);
        checkParent(parentId);

        SysMenu menu = new SysMenu();
        fill(menu, dto, parentId);
        menuMapper.insert(menu);
        // ADMIN 的权限是全部菜单，菜单变更要让全部用户缓存失效
        authUserCache.evictAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysMenuDTO dto) {
        SysMenu menu = Optional.ofNullable(menuMapper.selectOneById(id))
                .orElseThrow(() -> new BusinessException("菜单不存在"));

        Long parentId = Optional.ofNullable(dto.getParentId()).orElse(ROOT_ID);
        checkParent(parentId);
        // 上级不能是自己或自己的下级，否则菜单树会成环
        for (Long ancestor = parentId; !ROOT_ID.equals(ancestor); ancestor = parentOf(ancestor)) {
            if (ancestor.equals(id)) {
                throw new BusinessException("上级菜单不能选择自己或自己的下级");
            }
        }

        fill(menu, dto, parentId);
        menuMapper.update(menu);
        authUserCache.evictAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (menuMapper.selectCountByCondition(SYS_MENU.PARENT_ID.eq(id)) > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.deleteByCondition(SYS_ROLE_MENU.MENU_ID.eq(id));
        authUserCache.evictAll();
    }

    private void fill(SysMenu menu, SysMenuDTO dto, Long parentId) {
        menu.setParentId(parentId);
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(Optional.ofNullable(dto.getPath()).orElse(""));
        menu.setComponent(Optional.ofNullable(dto.getComponent()).orElse(""));
        menu.setPermission(Optional.ofNullable(dto.getPermission()).orElse(""));
        menu.setIcon(Optional.ofNullable(dto.getIcon()).orElse(""));
        menu.setSort(Optional.ofNullable(dto.getSort()).orElse(0));
        menu.setVisible(Optional.ofNullable(dto.getVisible()).orElse(1));
        menu.setStatus(Optional.ofNullable(dto.getStatus()).orElse(1));
    }

    private void checkParent(Long parentId) {
        if (ROOT_ID.equals(parentId)) {
            return;
        }
        SysMenu parent = Optional.ofNullable(menuMapper.selectOneById(parentId))
                .orElseThrow(() -> new BusinessException("上级菜单不存在"));
        if (TYPE_BUTTON.equals(parent.getMenuType())) {
            throw new BusinessException("按钮下不能再添加菜单");
        }
    }

    private Long parentOf(Long id) {
        return Optional.ofNullable(menuMapper.selectOneById(id))
                .map(SysMenu::getParentId)
                .orElse(ROOT_ID);
    }

    /**
     * 启用状态的菜单：超级管理员取全部，其他按角色分配
     */
    private List<SysMenu> listEnabled(boolean admin, Collection<Long> roleIds) {
        if (admin) {
            return menuMapper.selectListByCondition(SYS_MENU.STATUS.eq(1));
        }
        if (roleIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper query = QueryWrapper.create()
                .from(SYS_MENU)
                .where(SYS_MENU.STATUS.eq(1))
                .and(SYS_MENU.ID.in(
                        QueryWrapper.create()
                                .select(SYS_ROLE_MENU.MENU_ID)
                                .from(SYS_ROLE_MENU)
                                .where(SYS_ROLE_MENU.ROLE_ID.in(roleIds))));
        return menuMapper.selectListByQuery(query);
    }

    /**
     * 按 sort 排序组装成树。上级不在列表中的节点（上级被禁用或未分配）会被丢弃。
     */
    static List<SysMenu> buildTree(List<SysMenu> menus) {
        Comparator<SysMenu> order = Comparator
                .comparing((SysMenu m) -> Optional.ofNullable(m.getSort()).orElse(0))
                .thenComparing(SysMenu::getId);
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .sorted(order)
                .collect(Collectors.groupingBy(
                        m -> Optional.ofNullable(m.getParentId()).orElse(ROOT_ID),
                        LinkedHashMap::new,
                        Collectors.toList()));
        menus.forEach(m -> Optional.ofNullable(byParent.get(m.getId())).ifPresent(m::setChildren));
        return byParent.getOrDefault(ROOT_ID, List.of());
    }

}
