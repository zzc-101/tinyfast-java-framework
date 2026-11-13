<template>
  <div>
    <div class="table-header">
      <h3>用户管理</h3>
      <a-button v-permission="'system:user:add'" type="primary" @click="handleAdd">新增用户</a-button>
    </div>

    <!-- 搜索区域 -->
    <a-form layout="inline" :model="queryParams" class="search-form">
      <a-form-item label="用户名">
        <a-input v-model:value="queryParams.username" placeholder="请输入" allow-clear />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="queryParams.status" placeholder="全部" allow-clear style="width: 120px">
          <a-select-option :value="1">正常</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="loadData">查询</a-button>
        <a-button style="margin-left: 8px" @click="resetQuery">重置</a-button>
      </a-form-item>
    </a-form>

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '正常' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a v-permission="'system:user:edit'" @click="handleEdit(record)">编辑</a>
            <a v-permission="'system:user:resetPwd'" @click="handleResetPwd(record)">重置密码</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a v-permission="'system:user:delete'" class="danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑用户' : '新增用户'"
      :confirm-loading="submitting"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" />
        </a-form-item>
        <a-form-item :label="editingId ? '新密码' : '密码'" name="password">
          <a-input-password
            v-model:value="form.password"
            :placeholder="editingId ? '不修改请留空' : '至少 8 位，包含字母和数字'"
          />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="form.nickname" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="form.phone" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="form.email" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">正常</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="角色" name="roleIds">
          <a-select v-model:value="form.roleIds" mode="multiple" placeholder="请选择角色">
            <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
              {{ role.roleName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="pwdModalOpen"
      :title="`重置密码：${pwdTarget?.username ?? ''}`"
      :confirm-loading="pwdSubmitting"
      @ok="handlePwdSubmit"
    >
      <a-form ref="pwdFormRef" :model="pwdForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item
          label="新密码"
          name="newPassword"
          :rules="[{ required: true, message: '请输入新密码' }, passwordRule]"
        >
          <a-input-password v-model:value="pwdForm.newPassword" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import {
  getUserPageApi,
  getUserByIdApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  resetPasswordApi,
  getRoleListApi,
} from '@/api/system';
import { passwordRule } from '@/utils/validators';

const loading = ref(false);
const tableData = ref<any[]>([]);

const queryParams = reactive({
  username: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 200 },
];

onMounted(() => loadData());

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getUserPageApi(queryParams);
    tableData.value = res.data.records;
    pagination.total = res.data.total;
  } finally {
    loading.value = false;
  }
}

function handleTableChange(pag: any) {
  queryParams.pageNum = pag.current;
  queryParams.pageSize = pag.pageSize;
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
}

function resetQuery() {
  queryParams.username = '';
  queryParams.status = undefined;
  queryParams.pageNum = 1;
  loadData();
}

const modalOpen = ref(false);
const submitting = ref(false);
const editingId = ref('');
const formRef = ref<FormInstance>();
const roleOptions = ref<any[]>([]);

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  status: 1,
  roleIds: [] as string[],
});

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名' }],
  // 新增必填，修改时留空表示不改
  password: [{ required: !editingId.value, message: '请输入密码' }, passwordRule],
}));

async function loadRoleOptions() {
  const res: any = await getRoleListApi();
  roleOptions.value = res.data;
}

async function handleAdd() {
  editingId.value = '';
  Object.assign(form, { username: '', password: '', nickname: '', phone: '', email: '', status: 1, roleIds: [] });
  await loadRoleOptions();
  modalOpen.value = true;
}

async function handleEdit(record: any) {
  editingId.value = record.id;
  // 列表里没有角色，查详情回显 roleIds
  const [, res]: any[] = await Promise.all([loadRoleOptions(), getUserByIdApi(record.id)]);
  Object.assign(form, {
    username: res.data.username,
    password: '',
    nickname: res.data.nickname,
    phone: res.data.phone,
    email: res.data.email,
    status: res.data.status,
    roleIds: res.data.roleIds ?? [],
  });
  modalOpen.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (editingId.value) {
      await updateUserApi(editingId.value, form);
    } else {
      await createUserApi(form);
    }
    message.success('保存成功');
    modalOpen.value = false;
    loadData();
  } finally {
    submitting.value = false;
  }
}

const pwdModalOpen = ref(false);
const pwdSubmitting = ref(false);
const pwdTarget = ref<any>(null);
const pwdFormRef = ref<FormInstance>();
const pwdForm = reactive({ newPassword: '' });

function handleResetPwd(record: any) {
  pwdTarget.value = record;
  pwdForm.newPassword = '';
  pwdModalOpen.value = true;
}

async function handlePwdSubmit() {
  await pwdFormRef.value?.validate();
  pwdSubmitting.value = true;
  try {
    await resetPasswordApi(pwdTarget.value.id, pwdForm.newPassword);
    message.success('密码已重置');
    pwdModalOpen.value = false;
  } finally {
    pwdSubmitting.value = false;
  }
}

async function handleDelete(id: string) {
  await deleteUserApi(id);
  message.success('删除成功');
  loadData();
}
</script>

<style scoped>
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.search-form {
  margin-bottom: 16px;
}
.danger {
  color: #ff4d4f;
}
</style>
