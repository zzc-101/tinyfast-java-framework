import type { Directive } from 'vue';
import { useUserStore } from '@/store/user';

/**
 * 按钮级权限指令：v-permission="'system:user:add'"，
 * 传数组表示满足任意一个即可显示：v-permission="['system:user:add', 'system:user:edit']"。
 * 没有权限时隐藏元素。
 */
function check(el: HTMLElement, value: string | string[]) {
  const userStore = useUserStore();
  const required = Array.isArray(value) ? value : [value];
  if (!required.some((p) => userStore.hasPermission(p))) {
    el.style.display = 'none';
  }
}

export const permission: Directive<HTMLElement, string | string[]> = {
  mounted: (el, binding) => check(el, binding.value),
  updated: (el, binding) => check(el, binding.value),
};
