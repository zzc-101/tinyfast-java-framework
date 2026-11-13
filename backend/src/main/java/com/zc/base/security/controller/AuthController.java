package com.zc.base.security.controller;

import com.zc.base.common.core.R;
import com.zc.base.security.dto.CaptchaResponse;
import com.zc.base.security.dto.LoginRequest;
import com.zc.base.security.dto.LoginResponse;
import com.zc.base.security.dto.RefreshTokenRequest;
import com.zc.base.security.service.AuthService;
import com.zc.base.security.service.CaptchaService;
import com.zc.base.security.service.SecurityUserDetails;
import com.zc.base.system.entity.SysMenu;
import com.zc.base.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证接口
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysMenuService menuService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取登录验证码，enabled=false 时登录不需要验证码")
    @GetMapping("/captcha")
    public R<CaptchaResponse> captcha() {
        return R.success(captchaService.generate());
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return R.success(response);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return R.success(response);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return R.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<SecurityUserDetails> getUserInfo(@AuthenticationPrincipal SecurityUserDetails userDetails) {
        return R.success(userDetails);
    }

    @Operation(summary = "获取当前用户的菜单树（目录和菜单，不含按钮）")
    @GetMapping("/menus")
    public R<List<SysMenu>> getUserMenus(@AuthenticationPrincipal SecurityUserDetails userDetails) {
        return R.success(menuService.userMenuTree(userDetails.isAdmin(), userDetails.getRoleIds()));
    }

}
