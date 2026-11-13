package com.zc.base.system.service;

import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.entity.SysMenu;
import com.zc.base.system.mapper.SysMenuMapper;
import com.zc.base.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysMenuServiceTest {

    private SysMenuMapper menuMapper;
    private SysRoleMenuMapper roleMenuMapper;
    private SysMenuService service;

    @BeforeEach
    void setUp() {
        menuMapper = mock(SysMenuMapper.class);
        roleMenuMapper = mock(SysRoleMenuMapper.class);
        service = new SysMenuService(menuMapper, roleMenuMapper, mock(AuthUserCache.class));
    }

    @Test
    void userMenuTreeExcludesButtonsAndHiddenAndSortsBySort() {
        when(menuMapper.selectListByCondition(any(QueryCondition.class))).thenReturn(List.of(
                menu(3L, 0L, "D", "监控", 2, 1),
                menu(31L, 3L, "M", "在线用户", 2, 1),
                menu(32L, 3L, "M", "服务监控", 1, 1),
                menu(33L, 3L, "B", "强退", 3, 1),
                menu(34L, 3L, "M", "隐藏页", 4, 0),
                menu(1L, 0L, "M", "仪表盘", 1, 1)));

        List<SysMenu> tree = service.userMenuTree(true, List.of());

        assertEquals(List.of("仪表盘", "监控"), tree.stream().map(SysMenu::getMenuName).toList());
        SysMenu monitor = tree.get(1);
        assertEquals(List.of("服务监控", "在线用户"), monitor.getChildren().stream().map(SysMenu::getMenuName).toList());
    }

    @Test
    void userMenuTreeDropsChildrenWhoseParentIsNotAssigned() {
        when(menuMapper.selectListByQuery(any(QueryWrapper.class))).thenReturn(List.of(
                menu(31L, 3L, "M", "在线用户", 1, 1)));

        assertEquals(List.of(), service.userMenuTree(false, List.of(2L)));
    }

    @Test
    void userMenuTreeWithoutRolesQueriesNothing() {
        assertEquals(List.of(), service.userMenuTree(false, List.of()));
        verify(menuMapper, never()).selectListByQuery(any(QueryWrapper.class));
    }

    @Test
    void deleteMenuWithChildrenIsRejected() {
        when(menuMapper.selectCountByCondition(any(QueryCondition.class))).thenReturn(2L);

        BusinessException e = assertThrows(BusinessException.class, () -> service.delete(2L));
        assertEquals("存在子菜单，无法删除", e.getMessage());
        verify(menuMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void deleteLeafMenuAlsoRemovesRoleAssignments() {
        when(menuMapper.selectCountByCondition(any(QueryCondition.class))).thenReturn(0L);

        service.delete(211L);

        verify(menuMapper).deleteById(211L);
        verify(roleMenuMapper).deleteByCondition(any(QueryCondition.class));
    }

    private SysMenu menu(Long id, Long parentId, String type, String name, int sort, int visible) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuType(type);
        menu.setMenuName(name);
        menu.setSort(sort);
        menu.setVisible(visible);
        menu.setStatus(1);
        return menu;
    }
}
