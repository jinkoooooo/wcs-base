<template>
  <div class="audit-result-page">
    <CommonPage ref="commPageRef" :limit="50" :showPagination="true" :fetchHandler="fetchHandler">
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btn-handler="btnHandler" />
    </CommonPage>
  </div>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed } from 'vue';
  import CommonPage from '../../common/CommonPage.vue';
  import { getSearchList } from '/@/api/common/api';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';

  const { notification } = useMessage();

  const commPageRef = ref(null as any);
  const getFormFields = computed(() => commPageRef.value?.getFormFields);
  const validate = computed(() => commPageRef.value?.formValidate);
  const buttonlist = computed(() => commPageRef.value?.buttons);

  // PDF 보고서는 화면의 현재 검색조건/정렬을 그대로 사용 (마지막 조회값 보관)
  const lastQuery = ref('[]');
  const lastSort = ref('[]');

  /**
   * 커스텀 fetchHandler
   * - 표준 query/sort/page/limit 파라미터로 /wcs/audit/result 호출
   * - getFormattedFilters 로 DatePicker/RangePicker/in 등 자동 변환
   */
  async function fetchHandler(page: number, limit: number, sorters: any[], searchProps: any[]) {
    try {
      try {
        await validate.value();
      } catch (_) {
        /* outOfDate 무시 */
      }
      const fields = getFormFields.value();
      const queryFilters = await getFormattedFilters(fields, searchProps);

      lastQuery.value = JSON.stringify(queryFilters);
      lastSort.value = JSON.stringify(sorters);

      const resp = await getSearchList('/wcs/audit/result', {
        query: lastQuery.value,
        sort: lastSort.value,
        page,
        limit,
      });

      let records: any[] = [];
      let total = 0;
      if (resp && resp.items) {
        records = resp.items;
        total = resp.total;
      } else if (Array.isArray(resp)) {
        records = resp;
        total = resp.length;
      }
      return { total, records };
    } catch (e: any) {
      console.error('[AuditResult] ERROR:', e);
      notification.error({
        message: '오류',
        description: e?.message || '조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  /** 현재 검색조건으로 PDF 보고서를 새 탭에 출력. */
  async function downloadPdf() {
    try {
      const blob = await defHttp.get(
        {
          url: '/wcs/audit/result/report/pdf',
          params: { query: lastQuery.value, sort: lastSort.value, inline: true },
          responseType: 'blob',
          transformResponse: (data: any) => new Blob([data], { type: 'application/pdf' }),
        },
        { isTransformResponse: true },
      );
      const url = window.URL.createObjectURL(blob as any);
      window.open(url, '_blank');
    } catch (e: any) {
      notification.error({
        message: '오류',
        description: e?.message || 'PDF 생성 중 오류가 발생했습니다.',
        duration: 2,
      });
    }
  }

  function btnHandler(listenerName: any) {
    const handlers: Record<string, () => void> = {
      pdfBtnHandler: () => downloadPdf(),
      exportBtnHandler: () => commPageRef.value?.downExcel(),
      exceldownBtnHandler: () => commPageRef.value?.downExcel(),
    };
    const handler = handlers[listenerName];
    if (handler) handler();
  }
</script>

<style scoped>
  .audit-result-page {
    height: 100%;
  }
</style>
