import axios, { type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/store/user';
import router from '@/router';

type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

const baseURL = import.meta.env.VITE_API_BASE_URL;
const timeout = 30000;

/** 这些接口自己的 401 不触发 token 刷新 */
const NO_REFRESH_URLS = ['/auth/login', '/auth/refresh', '/auth/logout'];

const service = axios.create({ baseURL, timeout });

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

/** 正在进行的刷新请求。并发的 401 共用同一个 Promise，只发一次 /auth/refresh */
let refreshing: Promise<void> | null = null;

function refreshAccessToken(): Promise<void> {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

async function doRefresh() {
  const userStore = useUserStore();
  try {
    if (!userStore.refreshToken) {
      throw new Error('登录已过期，请重新登录');
    }
    // 用不带拦截器的 axios 调用，避免刷新请求自身的 401 再触发刷新
    const res = await axios.post(`${baseURL}/auth/refresh`, { refreshToken: userStore.refreshToken }, { timeout });
    if (res.data.code !== 200) {
      throw new Error(res.data.message || '登录已过期，请重新登录');
    }
    userStore.setTokens(res.data.data.accessToken, res.data.data.refreshToken);
  } catch (error: any) {
    // 刷新失败才清登录态。这段只会执行一次，排队的请求只管 reject
    expireLogin(error.response?.data?.message || error.message);
    throw error;
  }
}

/** 清登录态并跳转登录页 */
function expireLogin(msg?: string) {
  message.error(msg || '登录已过期，请重新登录');
  useUserStore().clearAuth();
  if (router.currentRoute.value.path !== '/login') {
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
  }
}

/**
 * 处理 401：刷新 token 后重放原请求。
 * 登录、刷新、登出接口自己的 401，以及重放后仍是 401 的请求，不再刷新。
 */
async function handleUnauthorized(config: RetryableConfig, error: any) {
  const url = config.url ?? '';
  if (NO_REFRESH_URLS.some((u) => url.endsWith(u))) {
    if (!url.endsWith('/auth/logout')) {
      message.error(error.response?.data?.message || error.message || '未登录或登录已过期');
    }
    return Promise.reject(error);
  }
  if (config._retry) {
    // 刚换了新 token 仍然 401（例如账号刚被禁用），不再重试
    expireLogin(error.response?.data?.message || error.message);
    return Promise.reject(error);
  }

  await refreshAccessToken();
  config._retry = true;
  return service(config);
}

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    if (res.code !== 200) {
      // 401: 未登录/token过期
      if (res.code === 401) {
        return handleUnauthorized(response.config as RetryableConfig, new Error(res.message || '请求失败'));
      }
      message.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res;
  },
  (error) => {
    if (error.response?.status === 401 && error.config) {
      return handleUnauthorized(error.config as RetryableConfig, error);
    }
    message.error(error.response?.data?.message || error.message || '网络错误');
    return Promise.reject(error);
  },
);

// 封装请求方法
export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service(config) as any;
}

export default service;
