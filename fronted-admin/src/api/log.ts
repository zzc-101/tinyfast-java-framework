import { request } from '@/utils/request';

/** 操作日志列表 */
export function getOperationLogPageApi(params: any) {
  return request({ url: '/log/operation', method: 'get', params });
}

/** 登录日志列表 */
export function getLoginLogPageApi(params: any) {
  return request({ url: '/log/login', method: 'get', params });
}

/** 清空操作日志 */
export function clearOperationLogApi() {
  return request({ url: '/log/operation/clear', method: 'delete' });
}

/** 清空登录日志 */
export function clearLoginLogApi() {
  return request({ url: '/log/login/clear', method: 'delete' });
}
