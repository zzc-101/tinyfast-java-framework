<template>
  <div>
    <div class="table-header">
      <h3>登录日志</h3>
      <a-popconfirm title="确定清空所有登录日志？" @confirm="handleClear">
        <a-button v-permission="'log:login:clear'" danger>清空</a-button>
      </a-popconfirm>
    </div>

    <a-form layout="inline" :model="queryParams" class="search-form">
      <a-form-item label="用户名">
        <a-input v-model:value="queryParams.username" placeholder="请输入" allow-clear />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="queryParams.status" placeholder="全部" allow-clear style="width: 120px">
          <a-select-option :value="1">成功</a-select-option>
          <a-select-option :value="0">失败</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="loadData">查询</a-button>
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
            {{ record.status === 1 ? '成功' : '失败' }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { getLoginLogPageApi, clearLoginLogApi } from '@/api/log';

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
  { title: 'IP地址', dataIndex: 'loginIp', key: 'loginIp' },
  { title: '登录地点', dataIndex: 'loginLocation', key: 'loginLocation' },
  { title: '浏览器', dataIndex: 'browser', key: 'browser' },
  { title: '操作系统', dataIndex: 'os', key: 'os' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '消息', dataIndex: 'message', key: 'message' },
  { title: '登录时间', dataIndex: 'createTime', key: 'createTime' },
];

onMounted(() => loadData());

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getLoginLogPageApi(queryParams);
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

async function handleClear() {
  await clearLoginLogApi();
  message.success('已清空');
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
</style>
