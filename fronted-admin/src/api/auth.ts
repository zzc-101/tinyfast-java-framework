import { request } from '@/utils/request';

export interface LoginParams {
  username: string;
  password: string;
  /** 验证码关闭时不传 */
  captchaId?: string;
  captchaCode?: string;
}

export interface CaptchaResult {
  /** false 表示登录不需要验证码 */
  enabled: boolean;
  captchaId?: string;
  /** base64 图片（data URI） */
  image?: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserInfo {
  userId: string;
  username: string;
  nickname: string;
  roles: string[];
  permissions: string[];
}

/** 用户菜单树节点（目录和菜单，不含按钮） */
export interface UserMenu {
  id: string;
  menuName: string;
  /** D 目录 / M 菜单 */
  menuType: string;
  path: string;
  icon: string;
  children?: UserMenu[];
}

/** 获取登录验证码 */
export function getCaptchaApi() {
  return request<{ code: number; data: CaptchaResult }>({
    url: '/auth/captcha',
    method: 'get',
  });
}

/** 登录 */
export function loginApi(data: LoginParams) {
  return request<{ code: number; data: LoginResult }>({
    url: '/auth/login',
    method: 'post',
    data,
  });
}

/** 刷新token */
export function refreshTokenApi(refreshToken: string) {
  return request<{ code: number; data: LoginResult }>({
    url: '/auth/refresh',
    method: 'post',
    data: { refreshToken },
  });
}

/** 登出 */
export function logoutApi() {
  return request({ url: '/auth/logout', method: 'post' });
}

/** 获取当前用户信息 */
export function getUserInfoApi() {
  return request<{ code: number; data: UserInfo }>({
    url: '/auth/info',
    method: 'get',
  });
}

/** 获取当前用户的菜单树 */
export function getUserMenusApi() {
  return request<{ code: number; data: UserMenu[] }>({
    url: '/auth/menus',
    method: 'get',
  });
}
