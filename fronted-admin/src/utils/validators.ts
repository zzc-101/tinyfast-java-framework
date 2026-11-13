import type { Rule } from 'ant-design-vue/es/form';

/** 密码强度：至少 8 位，同时包含字母和数字；留空时不校验，是否必填由 required 规则决定 */
export const passwordRule: Rule = {
  validator: async (_rule, value: string) => {
    if (value && !/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(value)) {
      throw new Error('密码至少 8 位，且必须同时包含字母和数字');
    }
  },
  trigger: 'blur',
};
