package com.zc.base.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zc.base.common.core.PageQuery;
import com.zc.base.common.core.PageResult;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.dto.SysRoleDTO;
import com.zc.base.system.entity.SysRole;
import com.zc.base.system.entity.SysRoleMenu;
import com.zc.base.system.mapper.SysRoleMapper;
import com.zc.base.system.mapper.SysRoleMenuMapper;
import com.zc.base.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.zc.base.system.entity.table.SysRoleTableDef.SYS_ROLE;
import static com.zc.base.system.entity.table.SysRoleMenuTableDef.SYS_ROLE_MENU;
import static com.zc.base.system.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final AuthUserCache authUserCache;

    /**
     * 分页查询角色
     */
    public PageResult<SysRole> page(PageQuery query) {
        QueryWrapper wrapper = QueryWrapper.create()
                .orderBy(SYS_ROLE.SORT.asc());

        Page<SysRole> page = roleMapper.paginate(
                Page.of(query.getPageNum(), query.getPageSize()), wrapper);

        return new PageResult<>(page.getTotalRow(), page.getRecords(),
                (int) page.getPageNumber(), (int) page.getPageSize());
    }

    /**
     * 查询所有角色
     */
    public List<SysRole> listAll() {
        return roleMapper.selectListByCondition(SYS_ROLE.STATUS.eq(1));
    }

    /**
     * 根据ID查询
     */
    public SysRole getById(Long id) {
        return roleMapper.selectOneById(id);
    }

    /**
     * 查询角色已分配的菜单ID
     */
    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectListByCondition(SYS_ROLE_MENU.ROLE_ID.eq(roleId)).stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

    /**
     * 新增角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SysRoleDTO dto) {
        long count = roleMapper.selectCountByCondition(SYS_ROLE.ROLE_KEY.eq(dto.getRoleKey()));
        if (count > 0) {
            throw new BusinessException("角色标识已存在");
        }

        SysRole role = new SysRole();
        role.setRoleKey(dto.getRoleKey());
        role.setRoleName(dto.getRoleName());
        role.setSort(Optional.ofNullable(dto.getSort()).orElse(0));
        role.setStatus(Optional.ofNullable(dto.getStatus()).orElse(1));
        role.setRemark(dto.getRemark());
        roleMapper.insert(role);

        Optional.ofNullable(dto.getMenuIds()).ifPresent(menuIds -> saveRoleMenus(role.getId(), menuIds));
    }

    /**
     * 修改角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysRoleDTO dto) {
        SysRole role = roleMapper.selectOneById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        if (!role.getRoleKey().equals(dto.getRoleKey())) {
            long count = roleMapper.selectCountByCondition(SYS_ROLE.ROLE_KEY.eq(dto.getRoleKey()));
            if (count > 0) {
                throw new BusinessException("角色标识已存在");
            }
            role.setRoleKey(dto.getRoleKey());
        }

        role.setRoleName(dto.getRoleName());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        role.setRemark(dto.getRemark());
        roleMapper.update(role);

        // menuIds 为 null 表示不改菜单分配
        Optional.ofNullable(dto.getMenuIds()).ifPresent(menuIds -> {
            roleMenuMapper.deleteByCondition(SYS_ROLE_MENU.ROLE_ID.eq(id));
            saveRoleMenus(id, menuIds);
        });

        // 角色状态、标识、菜单分配都会影响拥有该角色的用户，简单起见全部失效
        authUserCache.evictAll();
    }

    /**
     * 删除角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否有用户关联
        long count = userRoleMapper.selectCountByCondition(SYS_USER_ROLE.ROLE_ID.eq(id));
        if (count > 0) {
            throw new BusinessException("该角色下存在用户，无法删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByCondition(SYS_ROLE_MENU.ROLE_ID.eq(id));
        authUserCache.evictAll();
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            roleMenuMapper.insert(new SysRoleMenu(roleId, menuId));
        }
    }

}
