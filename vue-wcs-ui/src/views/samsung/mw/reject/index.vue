<template>
  <div class="reject-page">
    <!-- 🌟 로딩 오버레이 화면 -->
    <div
      v-if="isDownloading"
      class="fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-black bg-opacity-60 text-white backdrop-blur-sm"
    >
      <svg
        class="animate-spin h-12 w-12 text-white mb-4"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path
          class="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        />
      </svg>
      <h2 class="text-xl font-bold">이미지를 압축하여 다운로드 중입니다...</h2>
      <p class="mt-2 text-sm text-gray-300">
        데이터 손실을 막기 위해 창을 닫거나 새로고침하지 마세요.
      </p>
    </div>

    <div class="reject-header">
      <h1 class="reject-title"> 리젝 판정 이력/처리 </h1>

      <div class="reject-header-actions">
        <button class="btn btn-ghost" @click="onSearch" :disabled="loading">
          <svg
            class="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.3-4.3" />
          </svg>
          조회
        </button>

        <button class="btn btn-ghost" @click="onReset"> 초기화 </button>

        <button class="btn btn-ghost" @click="handleDemoDataResetBtn"> DEMO RESET </button>
        <button class="btn btn-ghost" @click="handleHeartbeatStartBtn"> 하트비트 시작 </button>
      </div>
    </div>

    <div ref="searchCardRef" class="card card-search" @keydown.enter.prevent="onSearch">
      <div class="search-grid">
        <div class="search-row">
          <span class="search-label">날짜</span>
          <RangePicker v-model="cond.dateRange" :maxRangeDays="30" />
        </div>

        <div class="search-row">
          <span class="search-label">상품바코드</span>
          <input v-model.trim="cond.itemBarcode" class="form-input" placeholder="상품 바코드" />
        </div>

        <div class="search-row">
          <span class="search-label">Serial Code</span>
          <input v-model.trim="cond.serialCode" class="form-input" placeholder="시리얼 코드" />
        </div>

        <div class="search-row">
          <span class="search-label search-label-wide">Reject Type</span>
          <select v-model="cond.rejectType" class="form-select">
            <option value="">전체</option>
            <option v-for="t in REJECT_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>

        <div class="search-row">
          <span class="search-label search-label-wide">Fianl Type</span>
          <select v-model="cond.finalType" class="form-select">
            <option value="">전체</option>
            <option v-for="t in FINAL_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
      </div>
    </div>

    <div class="card card-grid">
      <DataGrid
        class="h-[50vh]"
        :columns="columns"
        :rows="rows"
        row-key="id"
        v-model:selectedKey="selectedKey"
        @row-selected="onRowSelected"
        :loading="loading"
        :height="'25vh'"
      />
    </div>

    <div class="detail-layout">
      <div class="card">
        <div class="flex items-center justify-between mb-3">
          <h3 class="section-title !mb-0" style="margin-bottom: 0">선택 이미지</h3>

          <button
            v-if="images.length"
            class="btn btn-ghost !min-h-[32px] !text-xs px-3 py-1 flex items-center gap-1"
            @click="downloadAllImages"
            :disabled="isDownloading"
            title="모든 이미지 다운로드"
          >
            <svg
              v-if="isDownloading"
              class="animate-spin h-4 w-4"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                class="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                stroke-width="4"
              />
              <path
                class="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>

            <svg
              v-else
              class="h-4 w-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              stroke-width="2"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>

            {{ isDownloading ? '압축 및 다운로드 중...' : '전체 다운로드' }}
          </button>
        </div>

        <ImageCarousel :images="images" v-model:index="imageIndex" :max="7" />
      </div>

      <div class="detail-right">
        <div class="card">
          <div class="flex items-center justify-between">
            <h3 class="section-title">박스 체적 정보</h3>

            <span v-if="isSelectedNoRead" class="noread-badge"> NOREAD 수동 입력 필요 </span>
          </div>

          <InfoSummary :info="infoData" />
        </div>

        <div class="card">
          <h3 class="section-title mb-3">리젝 결과 처리</h3>

          <div v-if="isSelectedNoRead" class="noread-guide">
            라벨 손상 등으로 계속 NOREAD가 발생하는 박스는 작업자가 시리얼번호, 주문번호,
            컨테이너번호, 상품코드를 직접 입력한 뒤 최종 불량 처리합니다.
          </div>

          <div class="grid grid-cols-1 md:grid-cols-6 gap-3">
            <div class="md:col-span-3">
              <textarea
                v-model="form.reason"
                rows="3"
                class="form-textarea"
                placeholder="작업자 결과 판단 사유 입력"
              ></textarea>
            </div>

            <div class="md:col-span-1 text-right">
              <label class="block text-sm text-slate-600 mt-3">결과 타입</label>
              <label class="block text-sm text-slate-600 mt-3">최종 결과</label>
            </div>

            <div class="md:col-span-1">
              <select v-model="form.resultType" class="result-select">
                <option v-for="t in REJECT_TYPES" :key="t" :value="t">{{ t }}</option>
              </select>

              <select v-model="form.finalType" class="result-select">
                <option v-for="t in FINAL_TYPES" :key="t" :value="t">{{ t }}</option>
              </select>
            </div>

            <div class="md:col-span-1 flex flex-col gap-2 items-stretch">
              <button class="btn btn-ghost btn-block" @click="onFormReset"> 초기화 </button>

              <button
                v-if="isSelectedNoRead"
                class="btn btn-ghost btn-block btn-noread"
                @click="openNoReadModal"
                :disabled="!selectedRow"
              >
                NOREAD 수동처리
              </button>

              <button class="btn btn-primary btn-block" @click="onSave" :disabled="!selectedRow">
                저장
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ✅ NOREAD 수동 입력 모달: 별도 컴포넌트 없이 현재 화면 내부에 구성 -->
    <div
      v-if="noReadModalVisible"
      class="no-read-modal-backdrop"
      @mousedown.self="closeNoReadModal"
    >
      <div class="no-read-modal">
        <div class="no-read-modal-header">
          <div>
            <h3 class="no-read-modal-title">NOREAD 박스 수동 불량 처리</h3>
            <p class="no-read-modal-desc">
              라벨 손상 박스의 실제 정보를 입력한 뒤 최종 불량으로 처리합니다.
            </p>
          </div>

          <button class="no-read-modal-close" @click="closeNoReadModal">×</button>
        </div>

        <div class="no-read-origin">
          <div>
            <span class="origin-label">기존 NOREAD Serial</span>
            <span class="origin-value">{{ selectedRow?.box_id || '-' }}</span>
          </div>
          <div>
            <span class="origin-label">입고 결과</span>
            <span class="origin-value">{{ selectedRow?.reject_type || '-' }}</span>
          </div>
        </div>

        <div class="no-read-form-grid">
          <div class="no-read-form-row">
            <label class="no-read-label required">시리얼번호</label>
            <input
              v-model.trim="noReadForm.serialNo"
              class="no-read-input"
              placeholder="상품 고유번호 입력"
              autofocus
            />
            <p class="no-read-help">예: 박스 라벨 또는 작업자가 확인한 상품 고유번호</p>
          </div>

          <div class="no-read-form-row">
            <label class="no-read-label required">상품코드</label>
            <input
              v-model.trim="noReadForm.itemCode"
              class="no-read-input"
              placeholder="88코드 입력"
            />
            <p class="no-read-help">상품 바코드 기준 88로 시작하는 상품코드를 입력하세요.</p>
          </div>

          <div class="no-read-form-row">
            <label class="no-read-label required">컨테이너번호</label>
            <select
              v-model="noReadForm.cntrNo"
              class="no-read-input"
            >
              <option value="" disabled>컨테이너를 선택하세요</option>
              <option v-for="cntr in uniqueCntrNoList" :key="cntr" :value="cntr">
                {{ cntr }}
              </option>
            </select>
            <p class="no-read-help">해당 박스가 투입된 컨테이너 번호를 선택하세요.</p>
          </div>

          <div class="no-read-form-row">
            <label class="no-read-label required">주문번호</label>
            <select
              v-model="noReadForm.blNo"
              class="no-read-input"
            >
              <option value="" disabled>주문번호를 선택하세요</option>
              <option v-for="bl in uniqueBlNoList" :key="bl" :value="bl">
                {{ bl }}
              </option>
            </select>
            <p class="no-read-help">입고 주문 기준 BL_NO 값을 선택하세요.</p>
          </div>

          <div class="no-read-form-row no-read-form-row-full">
            <label class="no-read-label">처리 사유</label>
            <textarea
              v-model.trim="noReadForm.finalRemark"
              rows="3"
              class="no-read-textarea"
              placeholder="라벨 손상으로 인한 NOREAD 최종 불량 처리"
            ></textarea>
          </div>
        </div>

        <div class="no-read-modal-footer">
          <button class="btn btn-ghost" @click="closeNoReadModal" :disabled="loading">
            취소
          </button>

          <button class="btn btn-primary" @click="onNoReadManualRejectSave" :disabled="loading">
            불량 처리
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';
  import DataGrid from './components/DataGrid.vue';
  import ImageCarousel from './components/ImageCarousel.vue';
  import InfoSummary from './components/InfoSummary.vue';
  import RangePicker from './components/RangePicker.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { callApi } from '../common/api/callApi.js';
  import JSZip from 'jszip';

  type Row = Record<string, any>;

  const { createConfirm, createSuccessModal, createErrorModal } = useMessage();

  // 스캐너 ref
  const scanBuffer = ref('');
  let lastKeyTime = 0;
  let scanTimer: number | undefined;
  const searchCardRef = ref<HTMLElement | null>(null);

  const REJECT_TYPES = ['정상', '외관불량', '체적불량', '수행대기', 'NOREAD'] as const;
  const FINAL_TYPES = ['미정', '정상', '불량'] as const;

  type RejectType = (typeof REJECT_TYPES)[number];
  type FinalType = (typeof FINAL_TYPES)[number];

  interface RejectForm {
    reason: string;
    resultType: RejectType;
    finalType: FinalType;
  }

  interface InfoData {
    itemCode: string;
    itemName: string;
    serialNo: string;
    box_length: string;
    box_height: string;
    box_width: string;
    box_angle: string;
    real_box_length: string;
    real_box_height: string;
    real_box_width: string;
  }

  interface NoReadManualForm {
    serialNo: string;
    blNo: string;
    cntrNo: string;
    itemCode: string;
    finalRemark: string;
  }

  const handleHeartbeatStartBtn = () => {
    createConfirm({
      iconType: 'warning',
      title: () => '하트비트 시작',
      content: () => '하트비트 프로세스를 시작하시겠습니까?',
      onOk: async () => {
        try {
          const url = `http://${window.location.hostname}:9500/api/heartbeat/start`;
          const res = await callApi('POST', url, {});

          if (res) {
            createSuccessModal({
              title: '전송 성공',
              content: '하트비트 시작 요청이 정상적으로 처리되었습니다.',
            });
          } else {
            throw new Error('응답 결과 없음');
          }
        } catch (e: any) {
          console.error(e);
          createErrorModal({
            title: '전송 실패',
            content: '서버 통신 중 오류가 발생했습니다.\n' + (e.message || e),
          });
        }
      },
    });
  };

  /* ---------------- 버튼 ---------------- */
  const handleDemoDataResetBtn = () => {
    createConfirm({
      iconType: 'warning',
      title: () => 'DEMO 배치 일괄삭제',
      content: () => 'DEMO 배치를 일괄삭제 하시겠습니까?',
      onOk: async () => {
        await demoDataResetApi();
      },
    });
  };

  /* ---------------- 그리드 ---------------- */
  const loading = ref(false);

  const columns = ref([
    { field: 'received_at', label: '투입 시간', width: 160 },
    { field: 'bl_no', label: '주문 번호', width: 150 },
    { field: 'cntr_no', label: '컨테이너 번호', width: 120 },
    { field: 'final_result_string', label: '박스 최종 결과', width: 200 },
    { field: 'tracking_at', label: '최종 추적 시간', width: 160 },
    { field: 'tracking_status', label: '현재 상태', width: 100 },
    { field: 'tracking_desc', label: '상태 설명', width: 160 },
    {
      field: 'reject_type',
      label: '입고 결과',
      width: 160,
      cellClass: (value: string) => {
        if (value === '정상') return 'text-emerald-600 font-semibold';
        if (value === '외관불량') return 'text-red-600 font-semibold';
        if (value === '체적불량') return 'text-orange-500 font-semibold';
        if (value === 'NOREAD') return 'text-orange-500 font-semibold';
        if (value === '수행대기') return 'text-sky-600 font-semibold';
        return '';
      },
    },
    {
      field: 'reject_desc',
      label: '결과 사유',
      width: 200,
    },
    { field: 'box_id', label: '박스 고유번호(Serial No)', width: 200 },
    { field: 'item_code', label: '상품 바코드', width: 150 },
    { field: 'item_name', label: '그룹/상품코드', width: 300 },
    { field: 'cognex_result_at', label: '비전 검사 시간', width: 160 },
    { field: 'cognex_result_string', label: '비전 검사 결과', width: 100 },
    { field: 'cognex_result_rmk', label: '비전 결과 사유', width: 160 },
    { field: 'manual_result_at', label: '체적 검사 시간', width: 160 },
    { field: 'manual_result_string', label: '체적 검사 결과', width: 100 },
    { field: 'manual_result_rmk', label: '체적 결과 사유', width: 500 },
  ]);

  const rows = ref<Row[]>([]);

  const selectedKey = ref<string | number | null>(null);

  const selectedRow = computed<Row | null>(
    () => rows.value.find((r) => r.id === selectedKey.value) ?? null,
  );

  function onRowSelected(row: Row) {
    infoData.value = {
      itemCode: row.item_code ?? '',
      itemName: row.item_name ?? '',
      serialNo: row.box_id ?? '',
      box_length: row.box_length ?? '',
      box_height: row.box_height ?? '',
      box_width: row.box_width ?? '',
      box_angle: row.box_angle ?? '',
      real_box_length: row.attribute1 ?? '',
      real_box_height: row.attribute2 ?? '',
      real_box_width: row.attribute3 ?? '',
    };

    images.value = buildImageList(row).slice(0, 7);
    imageIndex.value = 0;

    form.value = {
      reason: row.final_remark ?? '',
      resultType: getRejectType(row.reject_type),
      finalType: getFinalType(row.final_status),
    };
  }

  function getRejectType(desc: string | undefined): RejectType {
    const idx = desc ? REJECT_TYPES.indexOf(desc as RejectType) : -1;
    return idx !== -1 ? REJECT_TYPES[idx] : REJECT_TYPES[0];
  }

  /* ---------------- 검색 조건 ---------------- */
  const cond = reactive({
    dateRange: [new Date().toISOString().slice(0, 10), new Date().toISOString().slice(0, 10)],
    itemBarcode: '',
    serialCode: '',
    rejectType: '' as '' | (typeof REJECT_TYPES)[number],
    finalType: '' as '' | (typeof FINAL_TYPES)[number],
  });

  function getFinalType(status: string | number | undefined): FinalType {
    const normalized = String(status ?? '');
    return normalized === '250'
      ? FINAL_TYPES[1]
      : normalized === '700'
      ? FINAL_TYPES[1]
      : normalized === '251'
      ? FINAL_TYPES[2]
      : FINAL_TYPES[0];
  }

  /* ---------------- NOREAD 판단 및 수동 처리 모달 ---------------- */
  const noReadModalVisible = ref(false);

  const noReadForm = ref<NoReadManualForm>({
    serialNo: '',
    blNo: '',
    cntrNo: '',
    itemCode: '',
    finalRemark: '',
  });

  function isNoReadText(value: any): boolean {
    return String(value ?? '')
      .toUpperCase()
      .includes('NOREAD');
  }

  const isSelectedNoRead = computed(() => {
    const row = selectedRow.value;
    if (!row) return false;

    return (
      isNoReadText(row.reject_type) ||
      isNoReadText(row.reject_desc) ||
      isNoReadText(row.box_id) ||
      isNoReadText(row.cognex_result_rmk)
    );
  });

  function openNoReadModal() {
    if (!selectedRow.value) {
      alert('행을 선택하세요.');
      return;
    }

    if (!isSelectedNoRead.value) {
      alert('NOREAD 박스만 수동 처리할 수 있습니다.');
      return;
    }

    form.value.resultType = '외관불량';
    form.value.finalType = '불량';

    noReadForm.value = {
      serialNo: '',
      blNo: selectedRow.value.bl_no ?? '',
      cntrNo: selectedRow.value.cntr_no ?? '',
      itemCode: selectedRow.value.item_code ?? '',
      finalRemark: form.value.reason || '라벨 손상으로 인한 NOREAD 최종 불량 처리',
    };

    noReadModalVisible.value = true;
  }

  function closeNoReadModal() {
    noReadModalVisible.value = false;
  }

  function validateNoReadForm() {
    if (!noReadForm.value.serialNo.trim()) {
      alert('시리얼번호를 입력하세요.');
      return false;
    }

    if (!noReadForm.value.itemCode.trim()) {
      alert('상품코드를 입력하세요.');
      return false;
    }

    if (!noReadForm.value.cntrNo.trim()) {
      alert('컨테이너번호를 선택하세요.');
      return false;
    }

    if (!noReadForm.value.blNo.trim()) {
      alert('주문번호를 선택하세요.');
      return false;
    }

    return true;
  }

  async function onNoReadManualRejectSave() {
    if (!selectedRow.value) return;
    if (!validateNoReadForm()) return;

    try {
      loading.value = true;

      const payload = {
        id: selectedRow.value.id,
        serialNo: noReadForm.value.serialNo.trim(),
        blNo: noReadForm.value.blNo.trim(),
        cntrNo: noReadForm.value.cntrNo.trim(),
        itemCode: noReadForm.value.itemCode.trim(),
        finalRemark: noReadForm.value.finalRemark.trim(),
      };

      const result = await callApi(
        'POST',
        `http://${window.location.hostname}:9500/rest/tb_mw_reject_box/noread_manual_reject`,
        payload,
      );

      if (result) {
        alert('NOREAD 박스 정보 보정 및 최종 불량 처리가 완료되었습니다.');
        noReadModalVisible.value = false;
        await onSearch();
      } else {
        alert('NOREAD 처리에 실패했습니다.');
      }
    } catch (e: any) {
      console.error(e);
      createErrorModal({
        title: 'NOREAD 처리 실패',
        content: e.message || 'NOREAD 수동 처리 중 오류가 발생했습니다.',
      });
    } finally {
      loading.value = false;
    }
  }

  /* ---------------- 좌측 이미지 뷰어 ---------------- */
  const images = ref<string[]>([]);
  const imageIndex = ref(0);
  const IMAGE_BASE_URL = `http://${window.location.hostname}:9500/rest/tb_mw_reject_box/image/`;

  function buildImageList(row: Row): string[] {
    const names = [
      row.file_name_front,
      row.file_name_back,
      row.file_name_left,
      row.file_name_right,
      row.file_name_top,
      row.file_name_bottom_left,
      row.file_name_bottom_right,
    ];

    return names
      .filter((n: string | null | undefined) => !!n && String(n).trim() !== '')
      .map((n: string) => IMAGE_BASE_URL + encodeURIComponent(n));
  }

  const isDownloading = ref(false);

  async function downloadAllImages() {
    if (!images.value || images.value.length === 0) return;
    if (!selectedRow.value) return;

    isDownloading.value = true;

    try {
      const itemCode = selectedRow.value.item_code ?? infoData.value.itemCode ?? 'UNKNOWN_ITEM';
      const serialNo = selectedRow.value.box_id ?? infoData.value.serialNo ?? 'UNKNOWN_SERIAL';
      const rawDate = selectedRow.value.received_at || '';
      const datePart = rawDate.slice(0, 10).replace(/-/g, '');

      const zipFileName = `${datePart}_${itemCode}_${serialNo}.zip`;

      const zip = new JSZip();
      let hasImages = false;

      for (let i = 0; i < images.value.length; i++) {
        const url = images.value[i];

        try {
          const response = await fetch(url);
          if (!response.ok) throw new Error(`Network error`);

          const blob = await response.blob();
          const imageFileName = buildDownloadFileName(url, serialNo);

          zip.file(imageFileName, blob);
          hasImages = true;
        } catch (error) {
          console.error(`Failed to fetch image ${i + 1}:`, error);
        }
      }

      if (hasImages) {
        const zipBlob = await zip.generateAsync({ type: 'blob' });
        const objectUrl = URL.createObjectURL(zipBlob);
        const link = document.createElement('a');

        link.href = objectUrl;
        link.download = zipFileName;

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(objectUrl);
      } else {
        alert('다운로드할 수 있는 유효한 이미지가 없습니다.');
      }
    } catch (error) {
      console.error('Failed to generate ZIP:', error);
      alert('압축 파일 생성 중 오류가 발생했습니다.');
    } finally {
      isDownloading.value = false;
    }
  }

  function formatNowForFileName(): string {
    const d = new Date();
    const yyyy = d.getFullYear();
    const MM = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const HH = String(d.getHours()).padStart(2, '0');
    const mm = String(d.getMinutes()).padStart(2, '0');
    const ss = String(d.getSeconds()).padStart(2, '0');

    return `${yyyy}${MM}${dd}${HH}${mm}${ss}`;
  }

  function sanitizeFileNamePart(value: string | number | undefined | null): string {
    return String(value ?? '')
      .trim()
      .replace(/[\\/:*?"<>|]/g, '_');
  }

  function buildDownloadFileName(url: string, boxId: string): string {
    try {
      const urlObj = new URL(url);
      const pathParts = urlObj.pathname.split('/');
      const lastPart = decodeURIComponent(pathParts[pathParts.length - 1] || '');

      if (!lastPart) {
        return `${formatNowForFileName()}_${sanitizeFileNamePart(boxId)}.jpg`;
      }

      const parts = lastPart.split('_');
      const suffix = parts.length >= 3 ? parts.slice(2).join('_') : lastPart;

      return `${formatNowForFileName()}_${sanitizeFileNamePart(boxId)}_${suffix}`;
    } catch (e) {
      return `${formatNowForFileName()}_${sanitizeFileNamePart(boxId)}.jpg`;
    }
  }

  /* ---------------- 우측 정보패널 ---------------- */
  const infoData = ref<InfoData>({
    itemCode: '',
    itemName: '',
    serialNo: '',
    box_length: '',
    box_height: '',
    box_width: '',
    box_angle: '',
    real_box_length: '',
    real_box_height: '',
    real_box_width: '',
  });

  /* ---------------- 하단 입력폼 ---------------- */
  const form = ref<RejectForm>({
    reason: '',
    resultType: REJECT_TYPES[0],
    finalType: FINAL_TYPES[0],
  });

  function onFormReset() {
    form.value = {
      reason: '',
      resultType: REJECT_TYPES[0],
      finalType: FINAL_TYPES[0],
    };
  }

  function isStackedCompletedStatus(status: any): boolean {
    return String(status ?? '').trim() === '700';
  }

  async function onSave() {
    if (!selectedRow.value) {
      alert('행을 선택하세요.');
      return;
    }

    const row = selectedRow.value;

    // 적치완료 건은 결과 재처리 불가
    if (isStackedCompletedStatus(row.final_status)) {
      alert('이미 완료된 작업입니다.');
      return;
    }

    // NOREAD 박스를 최종 불량 처리하는 경우에는 필수값 수동 입력 모달을 먼저 띄움
    if (isSelectedNoRead.value && form.value.finalType === '불량') {
      openNoReadModal();
      return;
    }

    selectedRow.value.final_remark = form.value.reason;
    selectedRow.value.reject_type = form.value.resultType;
    selectedRow.value.final_status =
      form.value.finalType == '정상'
        ? '250'
        : form.value.finalType == '불량'
          ? '251'
          : selectedRow.value.final_status;

    const param = {
      param: selectedRow.value,
    };

    const result = await callApi(
      'POST',
      `http://${window.location.hostname}:9500/rest/tb_mw_reject_box/reject`,
      param,
    );

    if (result) {
      alert('결과처리 완료 처리되었습니다.');
      await onSearch();
    } else {
      alert('결과 처리에 실패했습니다.');
    }
  }

  /* ---------------- 조회/초기화 ---------------- */
  async function onSearch() {
    try {
      loading.value = true;

      await fetchDeliveryInfo();

      await fetchBoxInfoFromApi();

      if (rows.value && rows.value.length > 0) {
        const firstRow = rows.value[0];
        selectedKey.value = firstRow.id;
        onRowSelected(firstRow);
      } else {
        selectedKey.value = null;
        images.value = [];
        infoData.value = {
          itemCode: '',
          itemName: '',
          serialNo: '',
          box_length: '',
          box_height: '',
          box_width: '',
          box_angle: '',
          real_box_length: '',
          real_box_height: '',
          real_box_width: '',
        };
        onFormReset();
      }
    } catch (e: any) {
      console.error(e);
      createErrorModal({
        title: '조회 실패',
        content: e.message || '데이터 조회 중 오류가 발생했습니다.',
      });
    } finally {
      loading.value = false;
    }
  }

  function onReset() {
    cond.itemBarcode = '';
    cond.serialCode = '';
    cond.rejectType = '';
    cond.finalType = '';
    onSearch();
  }

  onMounted(() => {
    onSearch();
    window.addEventListener('keydown', handleScannerKeydown);
    window.addEventListener('keydown', handleGridNavigation, true);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleScannerKeydown);
    window.removeEventListener('keydown', handleGridNavigation, true);

    if (scanTimer) window.clearTimeout(scanTimer);
  });

  watch(selectedRow, (row) => {
    if (!row) return;
  });

  function isInsideSearchArea(target: EventTarget | null): boolean {
    const el = target as Node | null;
    return !!(el && searchCardRef.value?.contains(el));
  }

  function isTypingElement(target: EventTarget | null): boolean {
    const el = target as HTMLElement | null;
    if (!el) return false;

    return ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName);
  }

  function resetScanBuffer() {
    scanBuffer.value = '';
  }

  function handleScannerKeydown(e: KeyboardEvent) {
    if (noReadModalVisible.value) return;

    const inSearchArea = isInsideSearchArea(e.target);
    if (inSearchArea || isTypingElement(e.target)) return;

    const currentTime = Date.now();
    const timeDiff = currentTime - lastKeyTime;
    lastKeyTime = currentTime;

    if (e.ctrlKey || e.altKey || e.metaKey || (e.key.length !== 1 && e.key !== 'Enter')) return;

    if (timeDiff > 50) {
      resetScanBuffer();
    }

    if (e.key === 'Enter') {
      e.preventDefault();

      const code = scanBuffer.value.trim();
      resetScanBuffer();

      if (!code || code.length < 3) return;

      if (code.startsWith('88')) {
        cond.itemBarcode = code;
        cond.serialCode = '';
      } else {
        cond.serialCode = code;
        cond.itemBarcode = '';
      }

      onSearch();
      return;
    }

    scanBuffer.value += e.key;
  }

  async function handleGridNavigation(e: KeyboardEvent) {
    if (noReadModalVisible.value) return;

    const activeEl = document.activeElement as HTMLElement;

    if (activeEl && ['INPUT', 'TEXTAREA', 'SELECT'].includes(activeEl.tagName)) return;

    if (!rows.value || rows.value.length === 0) return;

    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      e.stopPropagation();

      const currentIndex = rows.value.findIndex((r) => r.id === selectedKey.value);
      let nextIndex = currentIndex;

      if (e.key === 'ArrowDown') {
        nextIndex = currentIndex === -1 ? 0 : Math.min(currentIndex + 1, rows.value.length - 1);
      } else if (e.key === 'ArrowUp') {
        nextIndex = currentIndex === -1 ? rows.value.length - 1 : Math.max(currentIndex - 1, 0);
      }

      if (nextIndex !== currentIndex) {
        const nextRow = rows.value[nextIndex];
        selectedKey.value = nextRow.id;
        onRowSelected(nextRow);

        await nextTick();

        setTimeout(() => {
          const scrollContainer = document.querySelector(
            '.card-grid .overflow-auto',
          ) as HTMLElement;

          if (!scrollContainer) return;

          const rowElements = scrollContainer.querySelectorAll('.cursor-pointer');

          let targetElement: HTMLElement | null = null;

          if (rowElements.length > nextIndex) {
            targetElement = rowElements[nextIndex] as HTMLElement;
          }

          if (targetElement) {
            const containerRect = scrollContainer.getBoundingClientRect();
            const elementRect = targetElement.getBoundingClientRect();

            const relativeTop = elementRect.top - containerRect.top;

            const scrollAdjustment =
              relativeTop - containerRect.height / 2 + elementRect.height / 2;

            scrollContainer.scrollBy({
              top: scrollAdjustment,
              behavior: 'smooth',
            });
          }
        }, 50);
      }
    }
  }

  /** (API 1) 박스 정보 조회 */
  const fetchBoxInfoFromApi = async () => {
    const params = {
      date_mode: 'range',
      start_date: cond.dateRange[0] ?? '',
      end_date: cond.dateRange[1] ?? '',
      barcode: cond.itemBarcode,
      serial: cond.serialCode,
      reject_type: cond.rejectType,
    };

    let boxInfoList = await callApi(
      'POST',
      `http://${window.location.hostname}:9500/rest/tb_mw_reject_box/get_box_info`,
      params,
    );

    if (!boxInfoList) {
      throw new Error('주문 정보가 존재하지 않습니다.');
    }

    const finalType = cond.finalType as string | undefined;

    boxInfoList = boxInfoList
      .map((box) => {
        const final_result_string =
          box.final_status === 700
            ? box.final_status + ' (적치완료)'
            : box.final_status === 250
            ? box.final_status + ' (최종 정상처리)'
            : box.final_status === 251
            ? box.final_status + ' (최종 불량처리)'
            : '미정';

        return {
          ...box,
          cognex_result_string:
            box.cognex_result === 0 ? box.cognex_result + ' (NG)' : box.cognex_result + ' (OK)',
          manual_result_string:
            box.manual_result === 0 ? box.manual_result + ' (NG)' : box.manual_result + ' (OK)',
          final_result_string,
        };
      })
      .filter((box) => {
        if (!finalType) return true;

        switch (finalType) {
          case '미정':
            return box.final_status !== 250 && box.final_status !== 251 && box.final_status !== 700;
          case '정상':
            return box.final_status === 250 || box.final_status === 700;
          case '불량':
            return box.final_status === 251;
          default:
            return true;
        }
      });

    rows.value = boxInfoList;
  };

  /** (API 2) MW 오더 프로세스 상태 초기화 */
  const demoDataResetApi = async () => {
    const params = {};

    const result = await callApi(
      'POST',
      `http://${window.location.hostname}:9500/rest/tb_mw_reject_box/dataReset`,
      params,
    );

    if (!result) {
      throw new Error('Demo 초기화 실패.');
    }
  };
  // 셀렉트 박스 구성을 위해 API에서 가져온 Inbound Delivery 목록 저장
  const deliveryList = ref<any[]>([]);

  // 컨테이너/주문번호 셀렉트 박스용 Computed (중복 제거 및 오름차순 정렬)
  const uniqueCntrNoList = computed(() => {
    if (!deliveryList.value.length) return [];
    const list = deliveryList.value
      .map(item => item.cntr_no || item.cntrNo)
      .filter(val => !!val && String(val).trim() !== '');
    return Array.from(new Set(list)).sort();
  });

  const uniqueBlNoList = computed(() => {
    if (!deliveryList.value.length) return [];
    const list = deliveryList.value
      .map(item => item.bl_no || item.blNo)
      .filter(val => !!val && String(val).trim() !== '');
    return Array.from(new Set(list)).sort();
  });

  // (API 연동 추가) - 검색 시 날짜 구간에 해당하는 Inbound Delivery 데이터를 미리 받아옵니다.
  const fetchDeliveryInfo = async () => {
    try {
      const payload = {
        startDate: cond.dateRange[0] ?? '',
        endDate: cond.dateRange[1] ?? '',
      };

      const result = await callApi(
        'POST',
        `http://${window.location.hostname}:9500/rest/tb_mw_inbound_delivery/inbound_delivey_info_date`,
        payload
      );

      if (result && Array.isArray(result)) {
        deliveryList.value = result;
      } else {
        deliveryList.value = [];
      }
    } catch (error) {
      console.error('Failed to fetch delivery info:', error);
      deliveryList.value = [];
    }
  };
</script>

<style scoped lang="less">
  .reject-page {
    padding: 24px;
    min-height: 90vh;
    background-color: fade(#5b61f6, 10%);
  }

  .reject-page > * + * {
    margin-top: 1rem;
  }

  .reject-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .reject-title {
    display: flex;
    align-items: center;
    column-gap: 8px;
    font-size: 1.25rem;
    font-weight: 700;
  }

  .reject-header-actions {
    display: flex;
    align-items: center;
    column-gap: 8px;
  }

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    column-gap: 8px;
    padding: 8px 12px;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05), 0 1px 3px rgba(15, 23, 42, 0.1);
    transition: transform 0.1s ease-in-out, box-shadow 0.1s ease-in-out;
  }

  .btn:active {
    transform: scale(0.98);
  }

  .btn:disabled {
    cursor: not-allowed;
    opacity: 0.65;
  }

  .btn-ghost {
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
  }

  .btn-ghost:hover {
    background-color: #f9fafb;
  }

  .btn-primary {
    background-color: #5b61f6;
    color: #ffffff;
  }

  .btn-primary:hover {
    opacity: 0.9;
  }

  .btn-block {
    width: 100%;
  }

  .btn-noread {
    color: #ea580c;
    border-color: #fed7aa;
    background-color: #fff7ed;
  }

  .btn-noread:hover {
    background-color: #ffedd5;
  }

  .card {
    border-radius: 16px;
    background-color: #ffffff;
    border: 1px solid #e5e7eb;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
    padding: 16px;
  }

  .card-search {
    background-image: linear-gradient(135deg, #ffffff, rgba(148, 163, 184, 0.1));
  }

  .card-grid {
    overflow: hidden;
    padding: 0;
  }

  .detail-layout {
    display: grid;
    grid-template-columns: 1fr;
    gap: 16px;

    @media (min-width: 1024px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  .detail-right {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .section-title {
    font-size: 0.875rem;
    font-weight: 600;
    color: #334155;
    margin-bottom: 8px;
  }

  .search-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;

    @media (min-width: 768px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (min-width: 1280px) {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  .search-row {
    display: flex;
    align-items: center;
    column-gap: 8px;
  }

  .search-label {
    font-size: 0.875rem;
    color: #475569;
    white-space: nowrap;
    flex-shrink: 0;
  }

  .search-label-wide {
    width: 6rem;
  }

  .form-input {
    flex: 1;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #ffffff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
  }

  .form-select {
    flex: 1;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #ffffff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
  }

  .result-select {
    width: 100%;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #ffffff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
  }

  .form-textarea {
    width: 100%;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #ffffff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
    resize: vertical;
  }

  .noread-badge {
    display: inline-flex;
    align-items: center;
    padding: 4px 8px;
    border-radius: 999px;
    background-color: #fff7ed;
    color: #ea580c;
    border: 1px solid #fed7aa;
    font-size: 12px;
    font-weight: 700;
  }

  .noread-guide {
    margin-bottom: 12px;
    padding: 10px 12px;
    border-radius: 10px;
    background-color: #fff7ed;
    color: #9a3412;
    border: 1px solid #fed7aa;
    font-size: 13px;
    line-height: 1.5;
  }

  .no-read-modal-backdrop {
    position: fixed;
    inset: 0;
    z-index: 9998;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background-color: rgba(15, 23, 42, 0.45);
  }

  .no-read-modal {
    width: 560px;
    max-width: calc(100vw - 48px);
    border-radius: 18px;
    background-color: #ffffff;
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.25);
    padding: 20px;
  }

  .no-read-modal-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }

  .no-read-modal-title {
    font-size: 16px;
    font-weight: 800;
    color: #0f172a;
  }

  .no-read-modal-desc {
    margin-top: 4px;
    font-size: 13px;
    color: #64748b;
  }

  .no-read-modal-close {
    width: 32px;
    height: 32px;
    border-radius: 999px;
    color: #64748b;
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    font-size: 20px;
    line-height: 1;
  }

  .no-read-modal-close:hover {
    background-color: #f1f5f9;
    color: #0f172a;
  }

  .no-read-origin {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    margin-bottom: 14px;
    padding: 10px;
    border-radius: 12px;
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
  }

  .origin-label {
    display: block;
    font-size: 12px;
    color: #64748b;
    margin-bottom: 4px;
  }

  .origin-value {
    display: block;
    font-size: 13px;
    font-weight: 700;
    color: #334155;
    word-break: break-all;
  }

  .no-read-form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .no-read-form-row {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .no-read-form-row-full {
    grid-column: 1 / -1;
  }

  .no-read-label {
    font-size: 13px;
    font-weight: 700;
    color: #475569;
  }

  .no-read-label.required::after {
    content: ' *';
    color: #ef4444;
  }

  .no-read-input,
  .no-read-textarea {
    width: 100%;
    padding: 9px 11px;
    border-radius: 8px;
    border: 1px solid #dbe3ef;
    background-color: #ffffff;
    font-size: 14px;
    color: #0f172a;
    outline: none;
  }

  .no-read-input:focus,
  .no-read-textarea:focus {
    border-color: #5b61f6;
    box-shadow: 0 0 0 3px rgba(91, 97, 246, 0.12);
  }

  .no-read-textarea {
    resize: vertical;
  }

  .no-read-modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 18px;
  }

  .no-read-help {
    margin: 0;
    font-size: 12px;
    line-height: 1.4;
    color: #64748b;
  }
</style>
