import { request } from '@/utils/request';

/** 获取服务器信息 */
export function getServerInfoApi() {
  return request({ url: '/monitor/server', method: 'get' });
}

/** 获取在线用户 */
export function getOnlineUsersApi() {
  return request({ url: '/monitor/online-users', method: 'get' });
}

/** 强退用户 */
export function forceLogoutApi(userId: string) {
  return request({ url: `/monitor/online-users/${userId}`, method: 'delete' });
}
