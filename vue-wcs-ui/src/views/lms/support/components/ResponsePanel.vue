<template>
  <div class="flex flex-col h-full max-h-full w-full">
    <!-- 1. 답변 목록 -->
    <div class="flex-[1_1_auto] min-h-0 overflow-auto relative">
      <!-- 빈 데이터 처리 -->
      <div
        v-if="!loading && filteredItems.length === 0"
        class="flex items-center justify-center h-full"
      >
        <div class="text-center py-8 text-gray-500">
          <Empty :image="Empty.PRESENTED_IMAGE_SIMPLE">
            <template #description>
              <span>답변 데이터 없음</span>
            </template>
          </Empty>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-else-if="loading" class="space-y-4 p-4">
        <Skeleton active :paragraph="{ rows: 2 }" />
      </div>

      <List
        v-else
        :dataSource="filteredItems"
        itemLayout="vertical"
        :pagination="paginationProp"
        rowKey="res_id"
      >
        <template #renderItem="{ item }">
          <ListItem class="py-2">
            <!-- 1행 : 작성자 & 버튼 -->
            <div class="flex items-center justify-between gap-3 mb-1.5">
              <!-- 작성자 -->
              <div class="inline-flex items-center gap2 min-w-0">
                <strong class="whitespace-nowrap">{{ item.creator_nm }}</strong>
                <span class="text-gray-500 text-xs">{{ item.updated_at || item.created_at }}</span>
                <span v-if="item.is_deleted" class="text-red-500 text-xs ml-1">[삭제됨]</span>
              </div>

              <!-- 버튼 -->
              <div class="flex space-x-1.5">
                <Button size="middle" v-if="permissions.canWrite" @click="onEdit(item)"
                  >수정
                </Button>
                <Button size="middle" v-if="permissions.canDelete" danger @click="onDelete(item)">
                  삭제
                </Button>
              </div>
            </div>
            <!-- 2행 : 본문 -->
            <div class="whitespace-pre-wrap break-words">{{ item.content }}</div>
          </ListItem>
        </template>
      </List>
    </div>

    <!-- 2. 작성창 -->
    <div v-if="permissions.canWrite" class="flex-none border-t border-gray-200 pt-2">
      <!-- 버튼 -->
      <div class="mb-3 flex gap-6">
        <label class="flex items-center gap-1 cursor-pointer">
          <input
            type="checkbox"
            v-model="isCompleted"
            class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <span class="text-sm font-medium">요청 완료 처리</span>
        </label>
        <label class="flex items-center gap-1 cursor-pointer">
          <input
            type="checkbox"
            v-model="needSentMail"
            class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <span class="text-sm font-medium">메일 발송</span>
        </label>
      </div>

      <!-- 작성자 & 본문 -->
      <Input
        size="middle"
        v-model:value="creatorModel"
        placeholder="작성자 / 예시: 홍길동 프로(로지스올시스템즈)"
        :maxlength="64"
        showCount
      >
        <template #prefix>
          <UserOutlined />
        </template>
      </Input>
      <Textarea
        v-model:value="contentModel"
        :rows="6"
        placeholder="요청에 대한 답변을 입력해주세요 (2000자 이내)"
        :maxlength="2000"
        showCount
      />

      <!-- 버튼 -->
      <div class="mt-2">
        <Button
          type="primary"
          :loading="submitLoading"
          :disabled="contentModel === '' || creatorModel === ''"
          @click="onSubmit(editingItem)"
          >등록
        </Button>
        <Button
          class="ml-2"
          @click="onCancel"
          :disabled="contentModel === '' && creatorModel === ''"
          >취소
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref, watch, withDefaults } from 'vue';
  import { Button, Empty, Input, List, ListItem, Skeleton, Textarea } from 'ant-design-vue';
  import { UserOutlined } from '@ant-design/icons-vue';
  import type { Reply } from '@/views/support/types';
  import { getCommonPostApi, getSearchList, updateList } from '@/api/common/api';
  import { useMessage } from '@/hooks/web/useMessage';

  interface Props {
    data: any;
    permissions?: { canWrite?: boolean; canDelete?: boolean };
    saveUrl?: string;
    readUrl?: string;
    fetcher?: (_id: string) => Promise<Reply[]>;
  }

  const props = withDefaults(defineProps<Props>(), {
    permissions: () => ({ canWrite: true, canDelete: true }),
    fetcher: () => async (_id: string) => [] as Reply[],
  });

  const emit = defineEmits<{
    (e: 'submitted'): void;
    (e: 'updated'): void;
    (e: 'deleted'): void;
    (e: 'error', err: unknown): void;
    (e: 'completed', isCompleted: boolean): void;
    (e: 'sendMail'): void;
  }>();

  defineExpose({ onReset });

  const { createConfirm, notification } = useMessage();

  /**
   * 변수
   */
  const items = ref<Reply[]>([]); // 답변 목록
  const filteredItems = computed(() => {
    return items.value.filter((item) => {
      if (item.is_deleted) return props.permissions.canWrite || props.permissions.canDelete;
      return true;
    });
  }); // 답변 목록 - 삭제 건 필터링
  const editingItem = ref<Reply | undefined>(); // 작성 중인 답변
  const creatorModel = computed<string>({
    // 기본 creator_nm 보장
    get: () => editingItem.value?.creator_nm ?? '',
    set: (newCreator: string) => {
      editingItem.value = { ...(editingItem.value ?? ({} as Reply)), creator_nm: newCreator };
    },
  });
  const contentModel = computed<string>({
    // 기본 content 보장
    get: () => editingItem.value?.content ?? '',
    set: (newContent: string) => {
      editingItem.value = { ...(editingItem.value ?? ({} as Reply)), content: newContent };
    },
  });
  const isCompleted = ref<boolean>(true); // 요청 폼 완료처리 여부
  const needSentMail = ref<boolean>(false); // 요청 폼 작성자에게 메일 발송 여부

  /**
   * 스타일 세팅
   */
  const loading = ref<boolean>(false);
  const submitLoading = ref<boolean>(false);
  const page = ref(1);
  const pageSize = ref(3);
  const total = ref(0);
  const paginationProp = reactive({
    showSizeChanger: false,
    showQuickJumper: false,
    pageSize,
    current: page.value,
    total: total.value,
    showTotal: (total) => `총 ${total} 건`,
    onChange: pageChange,
    onShowSizeChange: pageSizeChange,
  });

  function pageChange(p, pz) {
    page.value = p;
    pageSize.value = pz;
    fetch();
  }

  function pageSizeChange(_current, size) {
    pageSize.value = size;
    fetch();
  }

  /**
   * 데이터 API
   */
  // 답변 조회
  async function fetch() {
    if (!props.data?.support_id || !props.data?.lc_id) {
      return;
    }

    if (!props.readUrl) {
      return notification.error({
        message: 'ERROR',
        description: '답변 조회 자원을 찾을 수 없습니다.',
        duration: 2,
      });
    }

    loading.value = true;

    const filterCols = [
      {
        name: 'support_id',
        operator: 'eq',
        value: props.data.support_id,
        relation: false,
      },
    ];

    try {
      const requestParams = {
        query: JSON.stringify(filterCols),
        sort: JSON.stringify([]),
        page: page.value, //현재 페이지
        limit: pageSize.value, // 페이지 당 데이터 수
      };

      const response = await getSearchList(props.readUrl, requestParams);
      const list = Array.isArray(response?.items)
        ? response.items
        : Array.isArray(response)
        ? response
        : [];
      items.value = list;
      const t =
        typeof response?.total === 'number'
          ? response.total
          : Array.isArray(response)
          ? response.length
          : 0;
      total.value = t ?? 0;

      paginationProp.current = page.value;
      paginationProp.total = total.value;
    } catch (e) {
      emit('error', e);
    } finally {
      loading.value = false;
    }
  }

  // 답변 생성/수정
  async function onSubmit(data: Reply | undefined, cudFlag?: string) {
    //NOTE: data parameter 활용 개선 고안
    if (!editingItem.value) {
      return notification.info({
        message: 'INFO',
        description: '답변 내용을 작성해주세요',
        duration: 2,
      });
    }

    const lcId = editingItem.value.lc_id ?? props.data?.lc_id;

    if (!lcId) {
      console.log('[DEBUG] onSubmit - lcId is undefined / lcId = ', lcId);
      return notification.info({
        message: 'INFO',
        description: '유지보수 요청 내역을 선택해주세요',
        duration: 2,
      });
    }

    if (!props.saveUrl) {
      return notification.error({
        message: 'ERROR',
        description: '답변 자원을 찾을 수 없습니다.',
        duration: 2,
      });
    }

    submitLoading.value = true;

    const newCudFlag = cudFlag ? cudFlag : editingItem.value.res_id ? 'u' : 'c';
    const emitNm = newCudFlag === 'c' ? 'submitted' : 'updated';
    const payload = {
      sr: {
        ...editingItem.value,
        support_id: props.data?.support_id, // 요청 데이터로 업데이트
        lc_id: props.data?.lc_id, // 요청 데이터로 업데이트
        is_deleted: false, // 삭제 건 재등록
        cud_flag_: newCudFlag,
      },
      need_sent_mail: needSentMail.value
    }

    try {
      const response = await getCommonPostApi(props.saveUrl, payload);
      if (response == true) {
        emit(emitNm);
        emit('completed', isCompleted.value);
        if (needSentMail.value === true) {
          emit('sendMail')
        }
        editingItem.value = undefined;
        await fetch();
      } else {
        notification.error({
          message: 'ERROR',
          description: '답변을 작성할 수 없습니다',
          duration: 2,
        });
      }
    } catch (e) {
      emit('error', e);
    } finally {
      submitLoading.value = false;
      needSentMail.value = false;
    }
  }

  // 답변 삭제
  async function onDelete(data: Reply) {
    if (!data) {
      return notification.error({
        message: 'INFO',
        description: '삭제할 내용이 없습니다',
        duration: 2,
      });
    }

    if (!props.saveUrl) {
      return notification.error({
        message: 'ERROR',
        description: '답변 자원을 찾을 수 없습니다.',
        duration: 2,
      });
    }

    // 작성 창 데이터 삭제
    if (editingItem.value?.res_id === data.res_id) {
      editingItem.value = onCancel();
    }

    const payload = {
      sr: {
        ...data,
        deleted: true,
        is_deleted: true,
        cud_flag_: 'u',
      }
    }

    try {
      const response = await updateList(props.saveUrl, payload);
      if (response == true) {
        emit('deleted');
        await fetch();
      } else {
        notification.error({
          message: "ERROR",
          description: "답변 삭제가 불가합니다."
        })
      }
    } catch (e) {
      emit('error', e);
    }
  }

  /**
   * 스타일 함수
   */
  // 답변 작성 창 변경
  function onEdit(data: Reply | undefined) {
    if (!data) {
      return notification.error({
        message: 'INFO',
        description: '작성 내용이 없습니다',
        duration: 2,
      });
    }
    editingItem.value = { ...data };
  }

  // 답변 목록 리셋
  function onReset() {
    items.value = [];
    isCompleted.value = true;
    onCancel();
  }

  // 답변 작성 창 리셋
  function onCancel() {
    editingItem.value = undefined;
    needSentMail.value = false;
  }

  // 새로운 요청사항마다 답변조회
  watch(
    () => props.data?.support_id,
    () => {
      page.value = 1;
      fetch();
    },
  );

  onMounted(fetch);
</script>

<style scoped></style>
