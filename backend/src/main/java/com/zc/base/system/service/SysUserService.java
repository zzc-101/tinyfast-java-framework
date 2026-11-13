package com.zc.base.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zc.base.common.core.PageResult;
import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.TransactionUtil;
import com.zc.base.security.service.TokenRevokeService;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.dto.SysUserDTO;
import com.zc.base.system.dto.SysUserQueryDTO;
import com.zc.base.system.entity.SysUser;
import com.zc.base.system.entity.SysUserRole;
import com.zc.base.system.mapper.SysUserMapper;
import com.zc.base.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.zc.base.system.entity.table.SysUserTableDef.SYS_USER;
import static com.zc.base.system.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthUserCache authUserCache;
    private final TokenRevokeService tokenRevokeService;

    /**
     * 分页查询用户
     */
    public PageResult<SysUser> page(SysUserQueryDTO query) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_USER.USERNAME.like(query.getUsername(), StrUtil.isNotBlank(query.getUsername())))
                .and(SYS_USER.PHONE.like(query.getPhone(), StrUtil.isNotBlank(query.getPhone())))
                .and(SYS_USER.STATUS.eq(query.getStatus(), query.getStatus() != null))
                .orderBy(SYS_USER.CREATE_TIME.desc());

        Page<SysUser> page = userMapper.paginate(
                Page.of(query.getPageNum(), query.getPageSize()), wrapper);

        // 隐藏密码
        page.getRecords().forEach(u -> u.setPassword(null));

        return new PageResult<>(page.getTotalRow(), page.getRecords(),
                (int) page.getPageNumber(), (int) page.getPageSize());
    }

    /**
     * 根据ID查询
     */
    public SysUser getById(Long id) {
        SysUser user = userMapper.selectOneById(id);
        Optional.ofNullable(user).ifPresent(u -> {
            u.setPassword(null);
            u.setRoleIds(userRoleMapper.selectListByCondition(SYS_USER_ROLE.USER_ID.eq(id)).stream()
                    .map(SysUserRole::getRoleId)
                    .toList());
        });
        return user;
    }

    /**
     * 新增用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(SysUserDTO dto) {
        // 检查用户名唯一
        long count = userMapper.selectCountByCondition(SYS_USER.USERNAME.eq(dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setStatus(Optional.ofNullable(dto.getStatus()).orElse(1));
        userMapper.insert(user);

        // 保存用户角色关联
        saveUserRoles(user.getId(), dto.getRoleIds());
    }

    /**
     * 修改用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysUserDTO dto) {
        SysUser user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String oldUsername = user.getUsername();

        // 用户名不允许修改为已存在的
        if (!user.getUsername().equals(dto.getUsername())) {
            long count = userMapper.selectCountByCondition(SYS_USER.USERNAME.eq(dto.getUsername()));
            if (count > 0) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(dto.getUsername());
        }

        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        user.setStatus(dto.getStatus());
        userMapper.update(user);

        // 更新角色关联
        if (dto.getRoleIds() != null) {
            userRoleMapper.deleteByCondition(SYS_USER_ROLE.USER_ID.eq(id));
            saveUserRoles(id, dto.getRoleIds());
        }

        // 改名时旧用户名的缓存也要失效
        authUserCache.evictUser(oldUsername, user.getUsername());
    }

    /**
     * 删除用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Optional.ofNullable(userMapper.selectOneById(id))
                .ifPresent(user -> authUserCache.evictUser(user.getUsername()));
        userMapper.deleteById(id);
        userRoleMapper.deleteByCondition(SYS_USER_ROLE.USER_ID.eq(id));
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);

        authUserCache.evictUser(user.getUsername());
        // 已签发的 token 全部失效，等事务提交（没有事务则立即）之后再吊销
        TransactionUtil.afterCommit(() -> tokenRevokeService.revokeUser(id));
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                userRoleMapper.insert(new SysUserRole(userId, roleId));
            }
        }
    }

}
