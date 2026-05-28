<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    @ok="handleSubmit"
    :title="getTitle"
    :useWrapper="false"
    :minHeight="500"
  >
    <div class="modal-size">
      <BasicGrid
        ref="gridRef"
        :options="gridProps.options"
        :theme="gridProps.myTheme"
        :rowHeaders="gridProps.rowHeaders"
        :columns="gridProps.columns"
        :columnOptions="gridProps.columnOptions"
        :limit="20"
        :pageSizeOptions="[10, 20, 50, 100]"
        :fetchHandler="fetchHandler"
      >
        <div class="ant-form-item-control-input form-color flex space-x-2 justify-end">
          <Button type="primary" @click="addRow">추가</Button>
          <Button type="primary" @click="saveRows">저장</Button>
          <Button danger @click="deleteRows">삭제</Button>
        </div>
      </BasicGrid>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { Grid as BasicGrid } from '/@/components/Grid/index';
  import { Button } from 'ant-design-vue';
  import { ref, onMounted, computed, nextTick } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { defHttp } from '/@/utils/http/axios';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getCommonPostApi, updateList, getSearchList } from '/@/api/common/api';

  const isUpdate = ref(true);
  const { createConfirm, notification } = useMessage();

  const gridProps = ref();
  const { t } = useI18n();
  const gridRef = ref();
  const record = ref();
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    record.value = data.selectedRow;
    gridRef.value.fetch();
  });

  const emit = defineEmits(['success']);
  const getTitle = computed(() => t('title.menu_auth'));

  onMounted(async () => {
    try {
      let allUsers = await getUserList(); // 전체 유저목록

      const selectOptions = allUsers.map((user) => ({
        text: `${user.name} (${user.email || '이메일 없음'})`,
        value: user.id,
      }));
      selectOptions.unshift({
        text: '=============== 사용자명 (이메일) ===============',
        value: '',
      });

      gridProps.value = {
        rowHeaders: ['checkbox', 'rowNum'],
        columnOptions: {
          resizable: true,
          frozenCount: 1,
        },
        columns: [
          {
            header: t('label.user'),
            name: 'user_id',
            formatter: 'listItemText',
            editor: {
              type: 'select',
              options: {
                listItems: selectOptions,
              },
            },
            editable: false,
            align: 'center',
            sortable: true,
          },
        ],
        data: [],
        myTheme: 'striped',
        options: {
          rowHeaders: ['checkbox'],
        },
      };
    } catch (error) {
      console.error('Grid initialization error:', error);
      notification.error({
        message: '초기화 오류',
        description: 'Grid 초기화 중 오류가 발생했습니다.',
        duration: 3,
      });
    }
  });

  const getUserList = async function () {
    let url = '/domain_users/search_domain_users';
    const response = await defHttp.get({ url });
    return (response || []).map((item) => ({
      id: item.id, // 실제 값
      name: item.name, // 표시될 이름
      email: item.email, // 이메일
    }));
  };

  async function fetchHandler({
    page,
    limit,
    sorters = [{ field: 'created_at', ascending: false }],
  }) {
    try {
      // const filterCols: { name: string; operator: string; value: string; relation: boolean }[] = [
      //   {
      //     name: 'lc_id',
      //     operator: '=',
      //     value: record.value?.lc_id,
      //     relation: false,
      //   },
      // ];

      // const params = {
      //   query: JSON.stringify(filterCols),
      //   sort: JSON.stringify(sorters),
      //   page,
      //   limit,
      // };

      const response = await getSearchList(
        `/centers_users/${record.value.lc_id}/center_users`,
        null,
      );
      if (Array.isArray(response)) {
        return {
          total: response.length,
          records: response,
        };
      } else {
        return {
          total: response.total,
          records: response.items,
        };
      }
    } catch (error) {
      console.log(error);
      return { total: 0, records: [] };
    }
  }

  /**
   * 행추가
   */
  const addRow = () => {
    if (!record.value) return;
    gridRef.value?.addRow();
  };

  async function saveRows() {
    try {
      setModalProps({ confirmLoading: true });
      const records = gridRef.value.getCURows();
      if (!record.value) {
        return notification.error({
          message: t('title.info'),
          description: t('text.NOTHING_CHANGED'),
          duration: 1,
        });
      } else {
        const convertedRecords = records.map((item) => ({
          cud_flag_: item.cud_flag_,
          lc_id: record.value.lc_id,
          user_id: item.user_id,
        }));
        let response = await getCommonPostApi(
          `/centers_users/${record.value.lc_id}/update_multiple`,
          convertedRecords,
        );
        if (response) {
          gridRef.value.fetch();
        }
      }
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

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
        const convertedRecords = selectedActualRows.map((item) => ({
          ...item,
          lc_id: record.value.lc_id,
        }));
        let response = await updateList(
          `/centers_users/${record.value.lc_id}/update_multiple`,
          convertedRecords,
        );
        if (response) {
          gridRef.value.fetch();
        }
      },
    });
  };

  const handleSubmit = async () => {
    closeModal();
  };
</script>
<style lang="less" scoped>
  .modal-size {
    height: 490px;
    display: flex;
    flex-direction: column;
    width: 60vw;
    background-color: @component-background;
  }
</style>
