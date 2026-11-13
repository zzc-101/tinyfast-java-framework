<template>
  <div>
    <div class="table-header">
      <h3>角色管理</h3>
      <a-button v-permission="'system:role:add'" type="primary" @click="handleAdd">新增角色</a-button>
    </div>

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
            <a v-permission="'system:role:edit'" @click="handleEdit(record)">编辑</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a v-permission="'system:role:delete'" class="danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑角色' : '新增角色'"
      :confirm-loading="submitting"
      width="560px"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="角色标识" name="roleKey">
          <a-input v-model:value="form.roleKey" placeholder="如 ADMIN" />
        </a-form-item>
        <a-form-item label="角色名称" name="roleName">
          <a-input v-model:value="form.roleName" />
        </a-form-item>
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="form.sort" :min="0" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">正常</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-input v-model:value="form.remark" />
        </a-form-item>
        <a-form-item label="菜单权限">
          <a-tree
            v-model:checkedKeys="checkedKeys"
            :tree-data="menuTree"
            :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
            checkable
            default-expand-all
            class="menu-tree"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue';
import {
  getRolePageApi,
  deleteRoleApi,
  createRoleApi,
  updateRoleApi,
  getMenuTreeApi,
  getRoleMenuIdsApi,
} from '@/api/system';

const loading = ref(false);
const tableData = ref<any[]>([]);

const queryParams = reactive({
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
  { title: '角色标识', dataIndex: 'roleKey', key: 'roleKey' },
  { title: '角色名称', dataIndex: 'roleName', key: 'roleName' },
  { title: '排序', dataIndex: 'sort', key: 'sort' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 150 },
];

onMounted(() => loadData());

async function loadData() {
  loading.value = true;
  try {
    const res: any = await getRolePageApi(queryParams);
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

const modalOpen = ref(false);
const submitting = ref(false);
const editingId = ref('');
const formRef = ref<FormInstance>();

const form = reactive({
  roleKey: '',
  roleName: '',
  sort: 0,
  status: 1,
  remark: '',
});

const rules = {
  roleKey: [{ required: true, message: '请输入角色标识' }],
  roleName: [{ required: true, message: '请输入角色名称' }],
};

const menuTree = ref<any[]>([]);
const checkedKeys = ref<string[]>([]);

/** 菜单ID -> 上级ID、叶子节点ID 集合 */
const parentOf = new Map<string, string>();
const leafIds = new Set<string>();

function indexMenuTree(nodes: any[]) {
  nodes.forEach((node) => {
    parentOf.set(node.id, node.parentId);
    if (node.children?.length) {
      indexMenuTree(node.children);
    } else {
      leafIds.add(node.id);
    }
  });
}

async function loadMenuTree() {
  const res: any = await getMenuTreeApi();
  menuTree.value = res.data;
  parentOf.clear();
  leafIds.clear();
  indexMenuTree(res.data);
}

/** 提交时，勾选的菜单连同它们的各级上级一起保存，这样侧边栏才能显示出上级目录 */
function collectMenuIds(): string[] {
  const ids = new Set<string>(checkedKeys.value);
  checkedKeys.value.forEach((id) => {
    for (let p = parentOf.get(id); p && p !== '0'; p = parentOf.get(p)) {
      ids.add(p);
    }
  });
  return [...ids];
}

async function handleAdd() {
  editingId.value = '';
  Object.assign(form, { roleKey: '', roleName: '', sort: 0, status: 1, remark: '' });
  checkedKeys.value = [];
  await loadMenuTree();
  modalOpen.value = true;
}

async function handleEdit(record: any) {
  editingId.value = record.id;
  Object.assign(form, {
    roleKey: record.roleKey,
    roleName: record.roleName,
    sort: record.sort,
    status: record.status,
    remark: record.remark,
  });
  const [, res]: any[] = await Promise.all([loadMenuTree(), getRoleMenuIdsApi(record.id)]);
  // 树是联动勾选的：只回显叶子节点，上级是否勾选由子节点推出来，避免没选全的目录被整个勾上
  checkedKeys.value = res.data.filter((id: string) => leafIds.has(id));
  modalOpen.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const data = { ...form, menuIds: collectMenuIds() };
    if (editingId.value) {
      await updateRoleApi(editingId.value, data);
    } else {
      await createRoleApi(data);
    }
    message.success('保存成功');
    modalOpen.value = false;
    loadData();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(id: string) {
  await deleteRoleApi(id);
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
.menu-tree {
  max-height: 320px;
  overflow: auto;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 8px;
}
</style>
