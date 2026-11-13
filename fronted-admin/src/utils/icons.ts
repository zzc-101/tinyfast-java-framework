import type { Component } from 'vue';
import {
  AppstoreOutlined,
  DashboardOutlined,
  FileTextOutlined,
  MenuOutlined,
  MonitorOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons-vue';

/** 菜单图标名称（存在 sys_menu.icon）到组件的映射，新增图标在这里登记 */
export const menuIcons: Record<string, Component> = {
  AppstoreOutlined,
  DashboardOutlined,
  FileTextOutlined,
  MenuOutlined,
  MonitorOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
};
