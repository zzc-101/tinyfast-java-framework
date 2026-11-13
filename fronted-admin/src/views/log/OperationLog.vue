<template>
  <div>
    <div class="table-header">
      <h3>操作日志</h3>
      <a-popconfirm title="确定清空所有操作日志？" @confirm="handleClear">
        <a-button v-permission="'log:operation:clear'" danger>清空</a-button>
      </a-popconfirm>
    </div>

    <a-form layout="inline" :model="queryParams" class="search-form">
      <a-form-item label="模块">
        <a-input v-model:value="queryParams.module" placeholder="请输入" allow-clear />
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
import { getOperationLogPageApi, clearOperationLogApi } from '@/api/log';

const loading = ref(false);
const tableData = ref<any[]>([]);

const queryParams = reactive({
  module: '',
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
  { title: '操作模块', dataIndex: 'module', key: 'module' },
  { title: '操作描述', dataIndex: 'description', key: 'description' },
  { title: '请求方式', dataIndex: 'requestMethod', key: 'requestMethod', width: 80 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName' },
  { title: 'IP', dataIndex: 'operatorIp', key: 'operatorIp' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '耗时(ms)', dataIndex: 'costTime', key: 'costTime', width: 100 },
  { title: '操作时间', dataIndex: 'createTime', key: 'createTime' },
];

onMounted(() => loadData());

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getOperationLogPageApi(queryParams);
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
  await clearOperationLogApi();
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
