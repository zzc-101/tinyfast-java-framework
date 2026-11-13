import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/store/user';

const Layout = () => import('@/views/layout/BasicLayout.vue');

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'DashboardOutlined' },
      },
      {
        path: '403',
        name: 'Forbidden',
        component: () => import('@/views/error/403.vue'),
        meta: { title: '无权访问', skipMenuCheck: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', skipMenuCheck: true },
      },
    ],
  },
  {
    path: '/system',
    component: Layout,
    meta: { title: '系统管理', icon: 'SettingOutlined' },
    children: [
      {
        path: 'user',
        name: 'SystemUser',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'role',
        name: 'SystemRole',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/MenuList.vue'),
        meta: { title: '菜单管理' },
      },
    ],
  },
  {
    path: '/monitor',
    component: Layout,
    meta: { title: '系统监控', icon: 'MonitorOutlined' },
    children: [
      {
        path: 'server',
        name: 'MonitorServer',
        component: () => import('@/views/monitor/ServerInfo.vue'),
        meta: { title: '服务监控' },
      },
      {
        path: 'online',
        name: 'MonitorOnline',
        component: () => import('@/views/monitor/OnlineUser.vue'),
        meta: { title: '在线用户' },
      },
    ],
  },
  {
    path: '/log',
    component: Layout,
    meta: { title: '日志管理', icon: 'FileTextOutlined' },
    children: [
      {
        path: 'operation',
        name: 'LogOperation',
        component: () => import('@/views/log/OperationLog.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'login',
        name: 'LogLogin',
        component: () => import('@/views/log/LoginLog.vue'),
        meta: { title: '登录日志' },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore();
  const isLogin = !!userStore.accessToken;

  if (to.meta.requiresAuth === false) {
    next();
    return;
  }

  if (!isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } });
    return;
  }

  // 获取用户信息、权限和菜单
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserData();
    } catch {
      userStore.logout();
      next({ path: '/login' });
      return;
    }
  }

  // 页面必须出现在用户的菜单里才能进入
  if (to.meta.skipMenuCheck !== true && !userStore.menuPaths.has(to.path)) {
    next({ path: '/403' });
    return;
  }

  next();
});

export default router;
