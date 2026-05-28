<template>
  <CommonPage
    ref="commPageRef"
    :actionColumns="actionColumns"
    @gridDbClicked="onGridDbClicked"
    :limit="limit"
  >
    <Button
      v-for="button in buttons"
      :key="button.title"
      @click="button.listener"
      :type="button.auth === 'delete' ? 'dashed' : 'primary'"
      :danger="button.auth === 'delete'"
    >
      <template #icon v-if="button.auth === 'export'">
        <DownloadOutlined />
      </template>

      <template #icon v-if="button.auth === 'import'">
        <UploadOutlined />
      </template>
      {{ t(button.title) }}</Button
    >

    <RoleModal @register="registerRoleModal" @success="handleSuccess" width="fit-content" />
    <UserAddModal @register="registerUserAddModal" @success="handleSuccess" width="fit-content" />
  </CommonPage>
</template>
<script lang="ts" setup>
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ref, computed, onMounted } from 'vue';
  import { Button } from 'ant-design-vue';
  import { updateList } from '/@/api/common/api';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useModal } from '/@/components/Modal';

  import CommonPage from '/src/views/common/CommonPage.vue';
  import RoleModal from './RoleModal.vue';
  import UserAddModal from './UserAddModal.vue';
  import { DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue';

  const { t } = useI18n();

  const { createConfirm, notification } = useMessage();
  const [registerRoleModal, { openModal: openRoleModal }] = useModal();
  const [registerUserAddModal, { openModal: openUserAddModal }] = useModal();
  /**
   * local ref
   */
  const commPageRef = ref(null as any);
  let limit = 100;
  let gridRef = computed(() => commPageRef.value?.grid);
  let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);
  let buttons = ref([] as any[]);

  const actionColumns = [
    {
      name: 'role',
      header: t('label.role'),
      align: 'center',
      width: 1,
      minWidth: 40,
      renderer: {
        type: 'icon',
        options: {
          icon: (record) => {
            return record && record.id && record.status != 'RUNNING'
              ? 'ant-design:edit-twotone'
              : '';
          },
        },
      },
    },
    {
      name: 'user',
      header: t('label.user'),
      align: 'center',
      width: 1,
      minWidth: 40,
      renderer: {
        type: 'icon',
        options: {
          icon: (record) => {
            return record && record.id && record.status != 'RUNNING'
              ? 'ant-design:copy-outlined'
              : '';
          },
        },
      },
    },
  ];
  //LifeCycle
  onMounted(() => {
    buttons.value = [
      {
        name: 'addRow',
        title: 'button.add',
        auth: 'create',
        hidden: 0,
        listener: addRow,
      },
      {
        name: 'saveRows',
        title: 'button.save',
        auth: 'update',
        hidden: 0,
        listener: saveRows,
      },
      {
        name: 'deleteRows',
        title: 'button.delete',
        auth: 'delete',
        hidden: 0,
        listener: deleteRows,
      },
    ];
  });
  const onGridDbClicked = (clickedRow) => {
    let event = clickedRow.event;
    if (event.columnName === 'role') {
      openPopup(clickedRow, openRoleModal);
    } else if (event.columnName === 'user') {
      openPopup(clickedRow, openUserAddModal);
    }
  };

  const openPopup = (clickedRow, openModal) => {
    openModal(true, {
      selectedRow: clickedRow,
      isUpdate: true,
    });
  };

  const handleSuccess = () => {
    // gridRef.value.fetch();
  };
  /**
   * 행추가
   */
  const addRow = () => {
    gridRef.value?.addRow();
  };

  /**
   * 수정
   */
  const saveRows = async () => {
    let patches = gridRef.value.getCURows();
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    let response = await updateList(gridSaveUrl.value, patches);
    if (response) {
      gridRef.value?.fetch();
    }
  };

  /**
   * 삭제
   */
  const deleteRows = () => {
    let selectedRows = gridRef.value.getCheckedRows();
    if (selectedRows.length === 0)
      return notification.error({
        message: '에러',
        description: `선택한 데이터가 없습니다`,
        duration: 1,
      });
    createConfirm({
      iconType: 'warning',
      title: () => '삭제',
      content: () => '선택한 데이터를 삭제하시겠습니까?',
      onOk: async () => {
        let selectedActualRows = gridRef.value.getSelectedRowsToDelete();
        let response = await updateList(gridSaveUrl.value, selectedActualRows);
        if (response) {
          gridRef.value.fetch();
        }
      },
    });
  };
</script>
