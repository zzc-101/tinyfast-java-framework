package com.zc.base.system.service;

import com.zc.base.common.exception.BusinessException;
import com.zc.base.common.util.TransactionUtil;
import com.zc.base.security.service.TokenRevokeService;
import com.zc.base.system.cache.AuthUserCache;
import com.zc.base.system.dto.ChangePasswordDTO;
import com.zc.base.system.dto.ProfileDTO;
import com.zc.base.system.dto.ProfileUpdateDTO;
import com.zc.base.system.entity.SysRole;
import com.zc.base.system.entity.SysUser;
import com.zc.base.system.entity.SysUserRole;
import com.zc.base.system.mapper.SysRoleMapper;
import com.zc.base.system.mapper.SysUserMapper;
import com.zc.base.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.zc.base.system.entity.table.SysRoleTableDef.SYS_ROLE;
import static com.zc.base.system.entity.table.SysUserRoleTableDef.SYS_USER_ROLE;

/**
 * 当前登录用户的个人中心。用户ID一律由调用方从登录上下文取，不接收请求参数里的ID
 */
@Service
@RequiredArgsConstructor
public class SysProfileService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthUserCache authUserCache;
    private final TokenRevokeService tokenRevokeService;

    public ProfileDTO get(Long userId) {
        SysUser user = getUser(userId);
        ProfileDTO profile = new ProfileDTO();
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setAvatar(user.getAvatar());
        profile.setRoleNames(listRoleNames(userId));
        return profile;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, ProfileUpdateDTO dto) {
        SysUser user = getUser(userId);

        // 只更新这四个字段。不整行回写，避免覆盖并发修改的密码
        SysUser update = new SysUser();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setEmail(dto.getEmail());
        update.setPhone(dto.getPhone());
        update.setAvatar(dto.getAvatar());
        userMapper.update(update);

        authUserCache.evictUser(user.getUsername());
    }

    /**
     * 修改密码，成功后该用户已签发的全部 token 失效（包括当前这个），需要重新登录
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        SysUser user = getUser(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.update(update);

        authUserCache.evictUser(user.getUsername());
        TransactionUtil.afterCommit(() -> tokenRevokeService.revokeUser(userId));
    }

    private SysUser getUser(Long userId) {
        return Optional.ofNullable(userMapper.selectOneById(userId))
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    private List<String> listRoleNames(Long userId) {
        List<Long> roleIds = userRoleMapper.selectListByCondition(SYS_USER_ROLE.USER_ID.eq(userId)).stream()
                .map(SysUserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectListByCondition(SYS_ROLE.ID.in(roleIds).and(SYS_ROLE.STATUS.eq(1))).stream()
                .map(SysRole::getRoleName)
                .toList();
    }

}
