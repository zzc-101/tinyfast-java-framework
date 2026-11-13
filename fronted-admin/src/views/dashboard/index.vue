<template>
  <div>
    <h2>仪表盘</h2>
    <a-row :gutter="16">
      <a-col :span="6">
        <a-card>
          <a-statistic title="用户总数" :value="0" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="角色总数" :value="0" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="在线用户" :value="0" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日访问" :value="0" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>
