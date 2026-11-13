import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import {
  loginApi,
  getUserInfoApi,
  getUserMenusApi,
  logoutApi,
  type LoginParams,
  type UserInfo,
  type UserMenu,
} from '@/api/auth';

/** 收集菜单树里所有菜单（M）的路由地址 */
function collectMenuPaths(menus: UserMenu[], paths: Set<string>) {
  menus.forEach((menu) => {
    if (menu.menuType === 'M' && menu.path) {
      paths.add(menu.path);
    }
    collectMenuPaths(menu.children ?? [], paths);
  });
  return paths;
}

export const useUserStore = defineStore(
  'user',
  () => {
    const accessToken = ref<string>('');
    const refreshToken = ref<string>('');
    const userInfo = ref<UserInfo | null>(null);
    const menus = ref<UserMenu[]>([]);

    /** 用户可进入的页面路径，路由守卫据此校验 */
    const menuPaths = computed(() => collectMenuPaths(menus.value, new Set<string>()));

    async function login(params: LoginParams) {
      const res: any = await loginApi(params);
      setTokens(res.data.accessToken, res.data.refreshToken);
      await fetchUserData();
    }

    function setTokens(access: string, refresh: string) {
      accessToken.value = access;
      refreshToken.value = refresh;
    }

    async function fetchUserInfo() {
      const res: any = await getUserInfoApi();
      userInfo.value = res.data;
    }

    async function fetchMenus() {
      const res: any = await getUserMenusApi();
      menus.value = res.data;
    }

    /** 用户信息、权限和菜单一起加载 */
    async function fetchUserData() {
      await Promise.all([fetchUserInfo(), fetchMenus()]);
    }

    /** 只清本地登录态，不请求后端 */
    function clearAuth() {
      accessToken.value = '';
      refreshToken.value = '';
      userInfo.value = null;
      menus.value = [];
    }

    async function logout() {
      try {
        await logoutApi();
      } catch {
        // 即使接口失败也清除本地状态
      }
      clearAuth();
    }

    function hasRole(role: string): boolean {
      return userInfo.value?.roles?.includes(role) ?? false;
    }

    function hasPermission(permission: string): boolean {
      return userInfo.value?.permissions?.includes(permission) ?? false;
    }

    return {
      accessToken,
      refreshToken,
      userInfo,
      menus,
      menuPaths,
      login,
      setTokens,
      fetchUserInfo,
      fetchMenus,
      fetchUserData,
      clearAuth,
      logout,
      hasRole,
      hasPermission,
    };
  },
  {
    persist: {
      pick: ['accessToken', 'refreshToken'],
    },
  },
);
