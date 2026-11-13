import { request } from '@/utils/request';

/** 用户管理 */
export function getUserPageApi(params: any) {
  return request({ url: '/system/users', method: 'get', params });
}

export function getUserByIdApi(id: string) {
  return request({ url: `/system/users/${id}`, method: 'get' });
}

export function createUserApi(data: any) {
  return request({ url: '/system/users', method: 'post', data });
}

export function updateUserApi(id: string, data: any) {
  return request({ url: `/system/users/${id}`, method: 'put', data });
}

export function deleteUserApi(id: string) {
  return request({ url: `/system/users/${id}`, method: 'delete' });
}

export function resetPasswordApi(id: string, newPassword: string) {
  return request({ url: `/system/users/${id}/reset-password`, method: 'put', data: { newPassword } });
}

/** 角色管理 */
export function getRolePageApi(params: any) {
  return request({ url: '/system/roles', method: 'get', params });
}

export function getRoleListApi() {
  return request({ url: '/system/roles/list', method: 'get' });
}

export function getRoleByIdApi(id: string) {
  return request({ url: `/system/roles/${id}`, method: 'get' });
}

/** 角色已分配的菜单ID */
export function getRoleMenuIdsApi(id: string) {
  return request({ url: `/system/roles/${id}/menus`, method: 'get' });
}

export function createRoleApi(data: any) {
  return request({ url: '/system/roles', method: 'post', data });
}

export function updateRoleApi(id: string, data: any) {
  return request({ url: `/system/roles/${id}`, method: 'put', data });
}

export function deleteRoleApi(id: string) {
  return request({ url: `/system/roles/${id}`, method: 'delete' });
}

/** 菜单管理 */
export function getMenuTreeApi() {
  return request({ url: '/system/menus', method: 'get' });
}

export function createMenuApi(data: any) {
  return request({ url: '/system/menus', method: 'post', data });
}

export function updateMenuApi(id: string, data: any) {
  return request({ url: `/system/menus/${id}`, method: 'put', data });
}

export function deleteMenuApi(id: string) {
  return request({ url: `/system/menus/${id}`, method: 'delete' });
}
