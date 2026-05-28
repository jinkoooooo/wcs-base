<template>
  <div class="dashboard-page">
    <!-- 메인 헤더 -->
    <div class="dashboard-header">
      <h1 class="dashboard-title">통합 검사 결과 대시보드 (VISION / GTR)</h1>
      <div class="dashboard-header-actions">
        <button class="btn btn-ghost" @click="onSearch" :disabled="loading">
          <svg v-if="loading" class="animate-spin h-4 w-4 mr-1" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <svg v-else class="h-4 w-4 mr-1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.3-4.3" />
          </svg>
          조회
        </button>
        <button class="btn btn-ghost" @click="onReset" :disabled="loading">초기화</button>
      </div>
    </div>

    <!-- 검색 영역 -->
    <div class="card card-search">
      <div class="search-grid">
        <div class="search-row">
          <span class="search-label">조회 일자</span>
          <input type="date" v-model="cond.startDate" class="form-input" />
          <span style="margin: 0 8px; color: #64748b;">~</span>
          <input type="date" v-model="cond.endDate" class="form-input" />
        </div>
        <div class="search-row">
          <span class="search-label">상품바코드</span>
          <input v-model.trim="cond.itemCode" class="form-input" placeholder="바코드 입력" @keyup.enter="onSearch" />
        </div>
      </div>
    </div>

    <!-- 메인 요약 카드 (GTR & VISION) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">

      <!-- GTR 메인 요약 카드 -->
      <div class="card summary-card gtr-card">
        <div class="summary-header">
          <div>
            <h2 class="text-xl font-bold text-slate-800">GTR 외관 검사</h2>
            <p class="text-sm text-slate-500">삼성전자 GTR 로봇 비전 시스템</p>
          </div>
          <button class="btn btn-primary" @click="openModal('gtr')">
            GTR 상세 보기
          </button>
        </div>
        <div class="kpi-grid">
          <div class="kpi-item">
            <span class="kpi-label">총 검사</span>
            <span class="kpi-value">{{ gtrStats.total }}</span>
          </div>
          <div class="kpi-item text-emerald-600">
            <span class="kpi-label text-emerald-600">정상 (Normal)</span>
            <span class="kpi-value">{{ gtrStats.normal }}</span>
          </div>
          <div class="kpi-item text-red-500">
            <span class="kpi-label text-red-500">불량 (Damaged)</span>
            <span class="kpi-value">{{ gtrStats.damaged }}</span>
          </div>
        </div>
      </div>

      <!-- VISION 메인 요약 카드 -->
      <div class="card summary-card vision-card">
        <div class="summary-header">
          <div>
            <h2 class="text-xl font-bold text-slate-800">VISION 체적/바코드 검사</h2>
            <p class="text-sm text-slate-500">Cognex/체적 측정 비전 시스템</p>
          </div>
          <button class="btn btn-primary" @click="openModal('vision')">
            VISION 상세 보기
          </button>
        </div>
        <div class="kpi-grid">
          <div class="kpi-item">
            <span class="kpi-label">총 스캔</span>
            <span class="kpi-value">{{ visionStats.total }}</span>
          </div>
          <div class="kpi-item text-emerald-600">
            <span class="kpi-label text-emerald-600">정상 판독 (OK)</span>
            <span class="kpi-value">{{ visionStats.ok }}</span>
          </div>
          <div class="kpi-item text-orange-500">
            <span class="kpi-label text-orange-500">오류/미인식 (NG)</span>
            <span class="kpi-value">{{ visionStats.ng }}</span>
          </div>
        </div>
      </div>

    </div>

    <!-- ==========================================
         🌟 팝업 1: GTR 상세 대시보드 모달
         ========================================== -->
    <div v-if="activeModal === 'gtr'" class="modal-overlay" @click.self="closeModal">
      <div class="modal-container">
        <div class="modal-header">
          <h2 class="text-xl font-bold text-slate-800">GTR 외관 검사 상세 대시보드</h2>
          <button class="btn btn-ghost !p-2" @click="closeModal">✕</button>
        </div>

        <div class="modal-body">
          <!-- 팝업 내 요약 & 시각화 영역 -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
            <div class="card bg-slate-50 flex flex-col justify-center items-center py-6">
              <span class="text-slate-500 font-medium">검사 양품률</span>
              <span class="text-4xl font-bold text-emerald-600 mt-2">
                {{ gtrStats.total > 0 ? ((gtrStats.normal / gtrStats.total) * 100).toFixed(1) : 0 }}%
              </span>
            </div>
            <div class="card md:col-span-2">
              <h3 class="section-title">불량 사유 분포 (Top 3)</h3>
              <div class="space-y-3 mt-3">
                <div v-if="gtrStats.reasons.length === 0" class="text-sm text-slate-400 mt-4">불량 데이터가 없습니다.</div>
                <div v-else v-for="reason in gtrStats.reasons" :key="reason.name" class="flex items-center text-sm">
                  <span class="w-1/3 truncate pr-2 text-slate-600" :title="reason.name">{{ reason.name || '알 수 없음' }}</span>
                  <div class="w-2/3 bg-slate-200 rounded-full h-2.5">
                    <div class="bg-red-500 h-2.5 rounded-full" :style="{ width: `${(reason.count / gtrStats.damaged) * 100}%` }"></div>
                  </div>
                  <span class="ml-3 font-semibold text-slate-700 w-12 text-right">{{ reason.count }}건</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 팝업 내 GTR 데이터 그리드 -->
          <!-- 팝업 내 GTR 데이터 그리드 -->
          <div class="card flex-1 flex flex-col p-0 overflow-hidden min-h-[450px]">
            <div class="px-4 py-3 border-b border-slate-200 bg-slate-50 font-semibold text-sm text-slate-700 flex justify-between shrink-0">
              <span>검사 이력 리스트 (최근 500건)</span>
              <span class="text-xs font-normal text-slate-500">총 {{ gtrRows.length }}건 조회됨</span>
            </div>

            <!-- 🌟 핵심 수정: relative 추가, 높이 상속 보장 -->
            <div class="grid-body bg-white flex-1 relative min-h-0 h-full w-full">
              <SimpleGrid
                :columns="gtrColumns"
                :rows="gtrRows"
                row-key="id"
                height="100%"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==========================================
         🌟 팝업 2: VISION 상세 대시보드 모달
         ========================================== -->
    <div v-if="activeModal === 'vision'" class="modal-overlay" @click.self="closeModal">
      <div class="modal-container border-t-4 border-t-sky-500">
        <div class="modal-header">
          <h2 class="text-xl font-bold text-slate-800">VISION 체적/바코드 상세 대시보드</h2>
          <button class="btn btn-ghost !p-2" @click="closeModal">✕</button>
        </div>

        <div class="modal-body">
          <!-- VISION 요약 시각화 영역 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div class="card bg-slate-50 flex flex-col justify-center items-center py-6">
              <span class="text-slate-500 font-medium">정상 인식률 (OK)</span>
              <span class="text-4xl font-bold text-sky-600 mt-2">
                {{ visionStats.total > 0 ? ((visionStats.ok / visionStats.total) * 100).toFixed(1) : 0 }}%
              </span>
            </div>
            <div class="card bg-slate-50 flex flex-col justify-center items-center py-6">
              <span class="text-slate-500 font-medium">오류 발생률 (NG)</span>
              <span class="text-4xl font-bold text-orange-500 mt-2">
                {{ visionStats.total > 0 ? ((visionStats.ng / visionStats.total) * 100).toFixed(1) : 0 }}%
              </span>
            </div>
          </div>

          <!-- 팝업 내 VISION 데이터 그리드 -->
          <div class="card flex-1 flex flex-col p-0 overflow-hidden min-h-[450px]">
            <div class="px-4 py-3 border-b border-slate-200 bg-slate-50 font-semibold text-sm text-slate-700 flex justify-between shrink-0">
              <span>스캔 이력 리스트 (최근 500건)</span>
              <span class="text-xs font-normal text-slate-500">총 {{ visionRows.length }}건 조회됨</span>
            </div>

            <!-- 🌟 핵심 수정: relative 추가, 높이 상속 보장 -->
            <div class="grid-body bg-white flex-1 relative min-h-0 h-full w-full">
              <SimpleGrid
                :columns="visionColumns"
                :rows="visionRows"
                row-key="id"
                height="100%"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { callApi } from './common/api/callApi.js';
import SimpleGrid from './inbound-seq/components/SimpleGrid.vue';

// --- 상태 및 변수 선언 ---
const loading = ref(false);

// 날짜 초기화 (오늘 기준)
const today = new Date();
const formattedToday = today.toISOString().slice(0, 10);

const cond = reactive({
  startDate: formattedToday,
  endDate: formattedToday,
  itemCode: ''
});

// 모달 제어 상태
const activeModal = ref<'none' | 'gtr' | 'vision'>('none');

// 통계 데이터 반응형 변수
const gtrStats = reactive({ total: 0, normal: 0, damaged: 0, reasons: [] as any[] });
const visionStats = reactive({ total: 0, ok: 0, ng: 0 });

const gtrRows = ref<any[]>([]);
const visionRows = ref<any[]>([]);

// --- SimpleGrid 컬럼 정의 ---
const gtrColumns = [
  { key: 'timestamp', label: '검사 시간', width: 160 },
  { key: 'serial', label: '상품바코드(Serial)', width: 180 },
  { key: 'result', label: '결과', width: 100, align: 'center' },
  { key: 'reason', label: '상세 사유', width: 250 },
  { key: 'confidence', label: '신뢰도', width: 100, align: 'right' }
] as any[];

const visionColumns = [
  { key: 'timestamp', label: '스캔 시간', width: 160 },
  { key: 'serial', label: '상품바코드(Seqno)', width: 180 },
  { key: 'result', label: '결과', width: 100, align: 'center' },
  { key: 'reason', label: '결과 코드(사유)', width: 250 }
] as any[];


// --- 함수 ---
function openModal(type: 'gtr' | 'vision') {
  activeModal.value = type;
}

function closeModal() {
  activeModal.value = 'none';
}

function onReset() {
  cond.startDate = formattedToday;
  cond.endDate = formattedToday;
  cond.itemCode = '';
  onSearch();
}

// 백엔드 연동: 데이터 조회 로직
async function onSearch() {
  if (loading.value) return;
  loading.value = true;

  try {
    const payload = {
      startDate: cond.startDate,
      endDate: cond.endDate,
      itemCode: cond.itemCode
    };

    // 🌟 API 호출 (IP 및 포트는 실제 서버 환경에 맞춰 수정)
    const res = await callApi('POST', `http://${window.location.hostname}:9500/rest/dashboard/integrated/stats`, payload);

    if (res && res.data) {
      // 1. GTR 매핑
      const gtr = res.data.gtr;
      if (gtr && gtr.stats) {
        gtrStats.total = Number(gtr.stats.total || 0);
        gtrStats.normal = Number(gtr.stats.normal || 0);
        gtrStats.damaged = Number(gtr.stats.damaged || 0);
        gtrStats.reasons = gtr.stats.reasons || [];
      } else {
        gtrStats.total = 0; gtrStats.normal = 0; gtrStats.damaged = 0; gtrStats.reasons = [];
      }
      gtrRows.value = gtr?.list || [];

      // 2. VISION 매핑
      const vision = res.data.vision;
      if (vision && vision.stats) {
        visionStats.total = Number(vision.stats.total || 0);
        visionStats.ok = Number(vision.stats.ok || 0);
        visionStats.ng = Number(vision.stats.ng || 0);
      } else {
        visionStats.total = 0; visionStats.ok = 0; visionStats.ng = 0;
      }
      visionRows.value = vision?.list || [];
    }
  } catch (error) {
    console.error('대시보드 데이터 조회 실패:', error);
    alert('데이터를 불러오는데 실패했습니다.');
  } finally {
    loading.value = false;
  }
}

// 컴포넌트 마운트 시 자동 1회 조회
onMounted(() => {
  onSearch();
});
</script>

<style scoped>
/* ==========================================
   기본 레이아웃 및 공통 스타일
   ========================================== */
.dashboard-page { padding: 24px; height: 100%; min-height: 100vh; background-color: rgba(91, 97, 246, 0.05); }
.dashboard-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.dashboard-title { font-size: 1.25rem; font-weight: 700; color: #1e293b; }
.dashboard-header-actions { display: flex; column-gap: 8px; }

/* 버튼 스타일 */
.btn {
  display: inline-flex; align-items: center; justify-content: center; padding: 8px 12px; border-radius: 8px; font-size: 0.875rem; font-weight: 500;
  transition: all 0.15s ease; cursor: pointer;
}
.btn:active { transform: scale(0.98); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-ghost { background-color: #fff; border: 1px solid #cbd5e1; color: #475569; }
.btn-ghost:hover:not(:disabled) { background-color: #f8fafc; border-color: #94a3b8; }
.btn-primary { background-color: #5b61f6; color: #fff; border: 1px solid transparent; }
.btn-primary:hover:not(:disabled) { background-color: #4f54e6; }

/* 카드 스타일 */
.card { border-radius: 12px; background-color: #fff; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05); padding: 16px; }
.card-search { background-color: #fff; }

/* 검색 영역 그리드 */
.search-grid { display: flex; gap: 16px; flex-wrap: wrap; }
.search-row { display: flex; align-items: center; }
.search-label { font-size: 0.875rem; font-weight: 600; color: #475569; margin-right: 8px; white-space: nowrap; }
.form-input { padding: 8px 12px; border-radius: 6px; border: 1px solid #cbd5e1; font-size: 0.875rem; outline: none; transition: border-color 0.2s; }
.form-input:focus { border-color: #5b61f6; box-shadow: 0 0 0 2px rgba(91,97,246,0.1); }

/* ==========================================
   메인 요약 카드 전용 스타일
   ========================================== */
.summary-card { display: flex; flex-direction: column; gap: 20px; border-top: 4px solid transparent; }
.gtr-card { border-top-color: #5b61f6; }
.vision-card { border-top-color: #0ea5e9; }
.summary-header { display: flex; justify-content: space-between; align-items: flex-start; }

.kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1px; background-color: #e2e8f0; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
.kpi-item { display: flex; flex-direction: column; align-items: center; justify-content: center; background-color: #f8fafc; padding: 16px; }
.kpi-label { font-size: 0.875rem; font-weight: 600; color: #64748b; margin-bottom: 4px; }
.kpi-value { font-size: 1.875rem; font-weight: 800; color: #1e293b; }

.section-title { font-size: 1rem; font-weight: 600; color: #334155; }

/* ==========================================
   팝업(Modal) 전용 스타일
   ========================================== */
.modal-overlay {
  position: fixed; inset: 0; z-index: 9999; background-color: rgba(15, 23, 42, 0.4); backdrop-filter: blur(2px);
  display: flex; align-items: center; justify-content: center; padding: 24px; animation: fadeIn 0.15s ease-out;
}
.modal-container {
  background-color: #ffffff; border-radius: 16px; width: 100%; max-width: 1200px; height: 85vh;
  display: flex; flex-direction: column; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  animation: slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}
.modal-header { padding: 16px 24px; border-bottom: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
.modal-body { padding: 20px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 16px; background-color: #f1f5f9; border-bottom-left-radius: 16px; border-bottom-right-radius: 16px; }

.grid-body {
  display: block;
  flex: 1;
  min-height: 0;
  height: 100%;
  position: relative; /* SimpleGrid의 height: 100% 가 이것을 기준으로 꽉 차게 됨 */
}

/* 팝업 모달 본문 영역이 넘치지 않고 그리드에게 높이를 잘 넘겨주도록 처리 */
.modal-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #f1f5f9;
  border-bottom-left-radius: 16px;
  border-bottom-right-radius: 16px;
  overflow-y: hidden; /* 모달 전체 스크롤을 막고, 그리드 내부에서 스크롤되도록 설정 */
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { opacity: 0; transform: translateY(10px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
</style>
