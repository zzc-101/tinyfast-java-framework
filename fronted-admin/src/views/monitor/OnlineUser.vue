<template>
  <div>
    <div class="table-header">
      <h3>在线用户</h3>
      <a-button @click="loadData">刷新</a-button>
    </div>
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="false"
      row-key="userId"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'loginTime'">
          {{ formatTime(record.loginTime) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定强退该用户？" @confirm="handleForceLogout(record.userId)">
            <a v-permission="'monitor:online:forceLogout'" class="danger">强退</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { getOnlineUsersApi, forceLogoutApi } from '@/api/monitor';

const loading = ref(false);
const tableData = ref<any[]>([]);

const columns = [
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: 'IP地址', dataIndex: 'ip', key: 'ip' },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime' },
  { title: '操作', key: 'action', width: 100 },
];

onMounted(() => loadData());

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getOnlineUsersApi();
    tableData.value = res.data;
  } finally {
    loading.value = false;
  }
}

function formatTime(timestamp: number): string {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss');
}

async function handleForceLogout(userId: string) {
  await forceLogoutApi(userId);
  message.success('已强退');
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
.danger {
  color: #ff4d4f;
}
</style>
