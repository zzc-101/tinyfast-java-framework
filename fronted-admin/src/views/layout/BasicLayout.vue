<template>
  <a-layout class="layout">
    <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible>
      <div class="logo">
        <h3 v-if="!collapsed">Base Admin</h3>
        <h3 v-else>B</h3>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        :items="menuItems"
        theme="dark"
        mode="inline"
        @click="handleMenuClick"
      />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <MenuFoldOutlined v-if="!collapsed" @click="collapsed = true" class="trigger" />
          <MenuUnfoldOutlined v-else @click="collapsed = false" class="trigger" />
        </div>
        <div class="header-right">
          <a-dropdown>
            <span class="user-info">
              <UserOutlined />
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="router.push('/profile')">个人中心</a-menu-item>
                <a-menu-item @click="handleLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, h, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import type { MenuProps } from 'ant-design-vue';
import { MenuFoldOutlined, MenuUnfoldOutlined, UserOutlined } from '@ant-design/icons-vue';
import { useUserStore } from '@/store/user';
import type { UserMenu } from '@/api/auth';
import { menuIcons } from '@/utils/icons';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const collapsed = ref(false);
const selectedKeys = ref<string[]>([route.path]);
const openKeys = ref<string[]>([]);

/** 后端菜单树转成 a-menu 的 items，key 用路由地址 */
function toMenuItems(menus: UserMenu[]): NonNullable<MenuProps['items']> {
  return menus.map((menu) => {
    const icon = menuIcons[menu.icon];
    return {
      key: menu.path,
      label: menu.menuName,
      icon: icon ? () => h(icon) : undefined,
      children: menu.children?.length ? toMenuItems(menu.children) : undefined,
    };
  });
}

const menuItems = computed(() => toMenuItems(userStore.menus));

/** 当前页面所在的各级目录，用来展开侧边栏；菜单树里没有该页面时返回 null */
function findAncestorKeys(menus: UserMenu[], path: string): string[] | null {
  for (const menu of menus) {
    if (menu.path === path) {
      return [];
    }
    const found = menu.children ? findAncestorKeys(menu.children, path) : null;
    if (found) {
      return [menu.path, ...found];
    }
  }
  return null;
}

watch(
  () => [route.path, userStore.menus] as const,
  ([path, menus]) => {
    selectedKeys.value = [path];
    (findAncestorKeys(menus, path) ?? []).forEach((key) => {
      if (!openKeys.value.includes(key)) {
        openKeys.value.push(key);
      }
    });
  },
  { immediate: true },
);

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  router.push(String(key));
};

async function handleLogout() {
  await userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo h3 {
  margin: 0;
  color: #fff;
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}

.trigger:hover {
  color: #1890ff;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 64px - 48px);
}
</style>
