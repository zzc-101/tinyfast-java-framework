package com.zc.base.monitor.service;

import com.zc.base.common.util.RedisCache;
import com.zc.base.security.service.TokenRevokeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 在线用户服务
 */
@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private static final String ONLINE_USER_PREFIX = "online:user:";

    private final RedisCache redisCache;
    private final TokenRevokeService tokenRevokeService;

    /**
     * 获取在线用户列表
     */
    @SuppressWarnings("unchecked")
    public List<OnlineUser> getOnlineUsers() {
        Set<String> keys = redisCache.keys(ONLINE_USER_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<OnlineUser> onlineUsers = new ArrayList<>();
        for (String key : keys) {
            Optional<Map<String, Object>> opt = redisCache.get(key);
            opt.ifPresent(map -> {
                OnlineUser user = new OnlineUser();
                user.setUserId(String.valueOf(map.get("userId")));
                user.setUsername(String.valueOf(map.get("username")));
                user.setIp(String.valueOf(map.get("ip")));
                user.setLoginTime((Long) map.get("loginTime"));
                onlineUsers.add(user);
            });
        }
        return onlineUsers;
    }

    /**
     * 强退用户
     */
    public void forceLogout(Long userId) {
        tokenRevokeService.revokeUser(userId);
        redisCache.delete(ONLINE_USER_PREFIX + userId);
    }

    @Data
    public static class OnlineUser {
        private String userId;
        private String username;
        private String ip;
        private Long loginTime;
    }

}
