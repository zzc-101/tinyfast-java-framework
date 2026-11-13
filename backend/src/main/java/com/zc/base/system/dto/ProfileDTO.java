package com.zc.base.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 个人资料
 */
@Data
public class ProfileDTO {

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String avatar;

    /** 角色名称 */
    private List<String> roleNames;

}
