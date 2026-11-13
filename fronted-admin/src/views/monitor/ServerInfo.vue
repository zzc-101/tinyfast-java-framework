<template>
  <div>
    <h3>服务监控</h3>
    <a-spin :spinning="loading">
      <a-row :gutter="16" v-if="serverInfo">
        <!-- CPU -->
        <a-col :span="12">
          <a-card title="CPU 信息">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="型号">{{ serverInfo.cpu?.modelName }}</a-descriptions-item>
              <a-descriptions-item label="核心数">{{ serverInfo.cpu?.coreCount }}</a-descriptions-item>
              <a-descriptions-item label="使用率">
                <a-progress :percent="serverInfo.cpu?.usageRate" :stroke-color="getProgressColor(serverInfo.cpu?.usageRate)" />
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 内存 -->
        <a-col :span="12">
          <a-card title="内存信息">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="总内存">{{ serverInfo.memory?.total }}</a-descriptions-item>
              <a-descriptions-item label="已使用">{{ serverInfo.memory?.used }}</a-descriptions-item>
              <a-descriptions-item label="使用率">
                <a-progress :percent="serverInfo.memory?.usageRate" :stroke-color="getProgressColor(serverInfo.memory?.usageRate)" />
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- JVM -->
        <a-col :span="12" style="margin-top: 16px">
          <a-card title="JVM 信息">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="Java版本">{{ serverInfo.jvm?.javaVersion }}</a-descriptions-item>
              <a-descriptions-item label="堆内存(已用/最大)">{{ serverInfo.jvm?.heapUsed }} / {{ serverInfo.jvm?.heapMax }}</a-descriptions-item>
              <a-descriptions-item label="堆使用率">
                <a-progress :percent="serverInfo.jvm?.heapUsageRate" :stroke-color="getProgressColor(serverInfo.jvm?.heapUsageRate)" />
              </a-descriptions-item>
              <a-descriptions-item label="线程数">{{ serverInfo.jvm?.threadCount }}</a-descriptions-item>
              <a-descriptions-item label="运行时长">{{ serverInfo.jvm?.uptime }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 系统信息 -->
        <a-col :span="12" style="margin-top: 16px">
          <a-card title="系统信息">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="操作系统">{{ serverInfo.system?.osName }}</a-descriptions-item>
              <a-descriptions-item label="系统架构">{{ serverInfo.system?.osArch }}</a-descriptions-item>
              <a-descriptions-item label="主机名">{{ serverInfo.system?.hostName }}</a-descriptions-item>
              <a-descriptions-item label="IP地址">{{ serverInfo.system?.hostIp }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 磁盘 -->
        <a-col :span="24" style="margin-top: 16px">
          <a-card title="磁盘信息">
            <a-table :columns="diskColumns" :data-source="serverInfo.disks" :pagination="false" size="small" row-key="mount">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'usageRate'">
                  <a-progress :percent="record.usageRate" size="small" :stroke-color="getProgressColor(record.usageRate)" />
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getServerInfoApi } from '@/api/monitor';

const loading = ref(false);
const serverInfo = ref<any>(null);

const diskColumns = [
  { title: '挂载点', dataIndex: 'mount', key: 'mount' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '总大小', dataIndex: 'total', key: 'total' },
  { title: '已使用', dataIndex: 'used', key: 'used' },
  { title: '可用', dataIndex: 'free', key: 'free' },
  { title: '使用率', dataIndex: 'usageRate', key: 'usageRate', width: 200 },
];

onMounted(() => loadData());

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getServerInfoApi();
    serverInfo.value = res.data;
  } finally {
    loading.value = false;
  }
}

function getProgressColor(percent: number): string {
  if (percent >= 90) return '#ff4d4f';
  if (percent >= 70) return '#faad14';
  return '#52c41a';
}
</script>
