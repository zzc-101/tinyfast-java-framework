package com.zc.base.system.cache;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 缓存在 Redis 里的鉴权用户信息。不含密码哈希。
 * 集合字段必须是 JDK 的可变集合（HashSet、ArrayList 等），Set.of / List.of 的不可变实现反序列化不回来。
 */
@Data
public class AuthUserInfo {

    private Long userId;
    private String username;
    private String nickname;
    private Integer status;

    /** 角色标识集合 */
    private Set<String> roles = new LinkedHashSet<>();

    /** 权限标识集合 */
    private Set<String> permissions = new LinkedHashSet<>();

    /** 有效（未禁用）角色的ID */
    private List<Long> roleIds = new ArrayList<>();

    /** 写入缓存的时间（毫秒），用来判断是否超过缓存时间 */
    private long cachedAt;

}
