<template>
  <div>
    <div class="table-header">
      <h3>菜单管理</h3>
      <a-button v-permission="'system:menu:add'" type="primary" @click="handleAdd()">新增菜单</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="false"
      v-model:expandedRowKeys="expandedRowKeys"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'menuType'">
          <a-tag :color="typeMap[record.menuType]?.color">{{ typeMap[record.menuType]?.text }}</a-tag>
        </template>
        <template v-if="column.key === 'visible'">
          {{ record.visible === 1 ? '是' : '否' }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '正常' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a v-if="record.menuType !== 'B'" v-permission="'system:menu:add'" @click="handleAdd(record.id)">新增</a>
            <a v-permission="'system:menu:edit'" @click="handleEdit(record)">编辑</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a v-permission="'system:menu:delete'" class="danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑菜单' : '新增菜单'"
      :confirm-loading="submitting"
      width="560px"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="上级菜单" name="parentId">
          <a-tree-select
            v-model:value="form.parentId"
            :tree-data="parentOptions"
            :field-names="{ label: 'title', value: 'value', children: 'children' }"
            tree-default-expand-all
            placeholder="请选择"
          />
        </a-form-item>
        <a-form-item label="菜单类型" name="menuType">
          <a-radio-group v-model:value="form.menuType">
            <a-radio value="D">目录</a-radio>
            <a-radio value="M">菜单</a-radio>
            <a-radio value="B">按钮</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="菜单名称" name="menuName">
          <a-input v-model:value="form.menuName" />
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'B'" label="图标" name="icon">
          <a-select v-model:value="form.icon" allow-clear placeholder="请选择">
            <a-select-option v-for="name in iconNames" :key="name" :value="name">
              <component :is="menuIcons[name]" /> {{ name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'B'" label="路由地址" name="path">
          <a-input v-model:value="form.path" placeholder="如 /system/user" />
        </a-form-item>
        <a-form-item v-if="form.menuType === 'M'" label="组件路径" name="component">
          <a-input v-model:value="form.component" placeholder="如 system/UserList" />
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'D'" label="权限标识" name="permission">
          <a-input v-model:value="form.permission" placeholder="模块:资源:操作，如 system:user:add" />
        </a-form-item>
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="form.sort" :min="0" />
        </a-form-item>
        <a-form-item v-if="form.menuType !== 'B'" label="是否可见" name="visible">
          <a-radio-group v-model:value="form.visible">
            <a-radio :value="1">是</a-radio>
            <a-radio :value="0">否</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">正常</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import { getMenuTreeApi, createMenuApi, updateMenuApi, deleteMenuApi } from '@/api/system';
import { menuIcons } from '@/utils/icons';

const ROOT_ID = '0';

const loading = ref(false);
const tableData = ref<any[]>([]);
const expandedRowKeys = ref<string[]>([]);

const typeMap: Record<string, { text: string; color: string }> = {
  D: { text: '目录', color: 'blue' },
  M: { text: '菜单', color: 'green' },
  B: { text: '按钮', color: 'orange' },
};

const iconNames = Object.keys(menuIcons);

const columns = [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName' },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 80 },
  { title: '图标', dataIndex: 'icon', key: 'icon' },
  { title: '路由地址', dataIndex: 'path', key: 'path' },
  { title: '权限标识', dataIndex: 'permission', key: 'permission' },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '可见', dataIndex: 'visible', key: 'visible', width: 70 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 180 },
];

onMounted(() => loadData());

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getMenuTreeApi();
    tableData.value = res.data;
    expandedRowKeys.value = collectKeys(res.data);
  } finally {
    loading.value = false;
  }
}

/** 目录和菜单默认展开 */
function collectKeys(nodes: any[]): string[] {
  return nodes
    .filter((node) => node.children?.length)
    .flatMap((node) => [node.id, ...collectKeys(node.children)]);
}

/** 上级菜单可选项：顶级 + 目录和菜单（按钮下不能再加菜单） */
function toOptions(nodes: any[]): any[] {
  return nodes
    .filter((node) => node.menuType !== 'B')
    .map((node) => ({ value: node.id, title: node.menuName, children: toOptions(node.children ?? []) }));
}

const parentOptions = computed(() => [
  { value: ROOT_ID, title: '顶级', children: toOptions(tableData.value) },
]);

const modalOpen = ref(false);
const submitting = ref(false);
const editingId = ref('');
const formRef = ref<FormInstance>();

function emptyForm() {
  return {
    parentId: ROOT_ID,
    menuType: 'M',
    menuName: '',
    icon: undefined as string | undefined,
    path: '',
    component: '',
    permission: '',
    sort: 0,
    visible: 1,
    status: 1,
  };
}

const form = reactive(emptyForm());

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称' }],
  menuType: [{ required: true, message: '请选择菜单类型' }],
};

function handleAdd(parentId: string = ROOT_ID) {
  editingId.value = '';
  Object.assign(form, emptyForm(), { parentId });
  modalOpen.value = true;
}

function handleEdit(record: any) {
  editingId.value = record.id;
  Object.assign(form, emptyForm(), {
    parentId: record.parentId,
    menuType: record.menuType,
    menuName: record.menuName,
    icon: record.icon || undefined,
    path: record.path,
    component: record.component,
    permission: record.permission,
    sort: record.sort,
    visible: record.visible,
    status: record.status,
  });
  modalOpen.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const data = { ...form, icon: form.icon ?? '' };
    if (editingId.value) {
      await updateMenuApi(editingId.value, data);
    } else {
      await createMenuApi(data);
    }
    message.success('保存成功');
    modalOpen.value = false;
    loadData();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(id: string) {
  await deleteMenuApi(id);
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
.danger {
  color: #ff4d4f;
}
</style>
