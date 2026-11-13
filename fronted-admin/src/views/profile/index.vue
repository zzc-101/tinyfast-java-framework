<template>
  <div>
    <h3>个人中心</h3>
    <a-row :gutter="24">
      <a-col :xs="24" :lg="12">
        <a-card title="个人资料" :loading="loading">
          <a-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 17 }"
          >
            <a-form-item label="用户名">
              <a-input :value="profile?.username" disabled />
            </a-form-item>
            <a-form-item label="角色">
              <a-space v-if="profile?.roleNames?.length">
                <a-tag v-for="name in profile.roleNames" :key="name">{{ name }}</a-tag>
              </a-space>
              <span v-else>-</span>
            </a-form-item>
            <a-form-item label="昵称" name="nickname">
              <a-input v-model:value="profileForm.nickname" :maxlength="64" />
            </a-form-item>
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="profileForm.email" />
            </a-form-item>
            <a-form-item label="手机号" name="phone">
              <a-input v-model:value="profileForm.phone" />
            </a-form-item>
            <a-form-item label="头像地址" name="avatar">
              <a-input v-model:value="profileForm.avatar" placeholder="图片地址" />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 5, span: 17 }">
              <a-button type="primary" :loading="profileSubmitting" @click="handleUpdateProfile">保存</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card title="修改密码">
          <a-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item label="旧密码" name="oldPassword">
              <a-input-password v-model:value="pwdForm.oldPassword" />
            </a-form-item>
            <a-form-item label="新密码" name="newPassword">
              <a-input-password v-model:value="pwdForm.newPassword" placeholder="至少 8 位，包含字母和数字" />
            </a-form-item>
            <a-form-item label="确认新密码" name="confirmPassword">
              <a-input-password v-model:value="pwdForm.confirmPassword" />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 6, span: 16 }">
              <a-button type="primary" :loading="pwdSubmitting" @click="handleChangePassword">修改密码</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import { getProfileApi, updateProfileApi, changePasswordApi, type Profile } from '@/api/profile';
import { useUserStore } from '@/store/user';
import { passwordRule } from '@/utils/validators';

const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const profile = ref<Profile | null>(null);
const profileFormRef = ref<FormInstance>();
const profileSubmitting = ref(false);
const profileForm = reactive({ nickname: '', email: '', phone: '', avatar: '' });

const profileRules = {
  email: [{ pattern: /^$|^[^@\s]+@[^@\s]+\.[^@\s]+$/, message: '邮箱格式不正确' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确' }],
};

onMounted(loadProfile);

async function loadProfile() {
  loading.value = true;
  try {
    const res = await getProfileApi();
    profile.value = res.data;
    Object.assign(profileForm, {
      nickname: res.data.nickname ?? '',
      email: res.data.email ?? '',
      phone: res.data.phone ?? '',
      avatar: res.data.avatar ?? '',
    });
  } finally {
    loading.value = false;
  }
}

async function handleUpdateProfile() {
  await profileFormRef.value?.validate();
  profileSubmitting.value = true;
  try {
    await updateProfileApi(profileForm);
    message.success('保存成功');
    // 右上角显示的昵称跟着变
    await userStore.fetchUserInfo();
  } finally {
    profileSubmitting.value = false;
  }
}

const pwdFormRef = ref<FormInstance>();
const pwdSubmitting = ref(false);
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码' }],
  newPassword: [{ required: true, message: '请输入新密码' }, passwordRule],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: async (_rule: unknown, value: string) => {
        if (value && value !== pwdForm.newPassword) {
          throw new Error('两次输入的新密码不一致');
        }
      },
    },
  ],
};

async function handleChangePassword() {
  await pwdFormRef.value?.validate();
  pwdSubmitting.value = true;
  try {
    await changePasswordApi({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword });
    message.success('密码已修改，请重新登录');
    // 后端已让该用户所有 token 失效，不用再调登出接口
    userStore.clearAuth();
    router.push('/login');
  } finally {
    pwdSubmitting.value = false;
  }
}
</script>
