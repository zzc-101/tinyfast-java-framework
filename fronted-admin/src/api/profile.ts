import { request } from '@/utils/request';

export interface Profile {
  username: string;
  nickname: string;
  email: string;
  phone: string;
  avatar: string;
  roleNames: string[];
}

export interface ProfileUpdateParams {
  nickname: string;
  email: string;
  phone: string;
  avatar: string;
}

/** 查看个人资料 */
export function getProfileApi() {
  return request<{ code: number; data: Profile }>({ url: '/system/profile', method: 'get' });
}

/** 修改个人资料 */
export function updateProfileApi(data: ProfileUpdateParams) {
  return request({ url: '/system/profile', method: 'put', data });
}

/** 修改密码，成功后需要重新登录 */
export function changePasswordApi(data: { oldPassword: string; newPassword: string }) {
  return request({ url: '/system/profile/password', method: 'put', data });
}
