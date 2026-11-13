<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">Base Admin</h2>
      <a-form
        :model="formState"
        @finish="handleLogin"
        layout="vertical"
      >
        <a-form-item name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input
            v-model:value="formState.username"
            placeholder="用户名"
            size="large"
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password
            v-model:value="formState.password"
            placeholder="密码"
            size="large"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>
        <a-form-item
          v-if="captcha.enabled"
          name="captchaCode"
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <div class="captcha-row">
            <a-input
              v-model:value="formState.captchaCode"
              placeholder="验证码"
              size="large"
              :maxlength="8"
            >
              <template #prefix>
                <SafetyOutlined />
              </template>
            </a-input>
            <img
              v-if="captcha.image"
              :src="captcha.image"
              class="captcha-img"
              alt="验证码"
              title="点击刷新"
              @click="refreshCaptcha"
            />
            <a-button v-else size="large" @click="refreshCaptcha">获取验证码</a-button>
          </div>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading" block size="large">
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue';
import { useUserStore } from '@/store/user';
import { getCaptchaApi, type CaptchaResult } from '@/api/auth';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);

const formState = reactive({
  username: '',
  password: '',
  captchaCode: '',
});

const captcha = reactive<CaptchaResult>({ enabled: false });

onMounted(refreshCaptcha);

/** 获取新验证码；后端关闭验证码时 enabled 为 false，输入框不显示 */
async function refreshCaptcha() {
  formState.captchaCode = '';
  try {
    const res = await getCaptchaApi();
    Object.assign(captcha, { captchaId: undefined, image: undefined }, res.data);
  } catch {
    // 错误提示已由请求拦截器显示，点击图片位置的按钮可以重试
    captcha.image = undefined;
  }
}

async function handleLogin() {
  loading.value = true;
  try {
    await userStore.login({ ...formState, captchaId: captcha.captchaId });
    message.success('登录成功');
    const redirect = (route.query.redirect as string) || '/';
    router.push(redirect);
  } catch {
    // 错误提示（密码错误、验证码错误、账号锁定等后端文字）已由请求拦截器显示，这里只需要换一张验证码
    await refreshCaptcha();
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.captcha-row {
  display: flex;
  gap: 12px;
}

.captcha-img {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border-radius: 6px;
  flex-shrink: 0;
}

.login-title {
  text-align: center;
  margin-bottom: 32px;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}
</style>
