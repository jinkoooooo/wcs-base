<template>
  <div class="location-master-container custom-scrollbar">
    <div class="page-header">
      <div class="header-titles">
        <h2>📍 로케이션 마스터 관리</h2>
        <p>물류센터의 전체 로케이션 목록을 조회하고 검증합니다.</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" @click="fetchLocations(1)">🔄 목록 갱신</button>
        <button class="btn-primary" @click="openGenModal">➕ 로케이션 자동 생성기</button>
      </div>
    </div>

    <div class="card search-card">
      <div class="search-grid">
        <div class="input-wrapper">
          <label>로케이션 코드</label>
          <input type="text" v-model="searchParams.locationCode" @keyup.enter="handleSearch" placeholder="예: H01-01-01" class="modern-input" />
        </div>
        <div class="input-wrapper">
          <label>구역 (Area)</label>
          <input type="text" v-model="searchParams.areaId" @keyup.enter="handleSearch" placeholder="예: ASRS1" class="modern-input" />
        </div>
        <div class="input-wrapper">
          <label>방향 (Side)</label>
          <select v-model="searchParams.sideCode" @change="handleSearch" class="modern-select">
            <option value="">전체</option>
            <option value="L">LEFT (L)</option>
            <option value="R">RIGHT (R)</option>
          </select>
        </div>
        <div class="input-wrapper">
          <label>등급 (Grade)</label>
          <select v-model="searchParams.locationGrade" @change="handleSearch" class="modern-select">
            <option value="">전체</option>
            <option value="A">A 등급</option>
            <option value="B">B 등급</option>
            <option value="C">C 등급</option>
            <option value="D">D 등급</option>
          </select>
        </div>
      </div>
      <div class="search-actions">
        <button class="btn-reset" @click="handleReset">초기화</button>
        <button class="btn-search" @click="handleSearch">🔍 조회</button>
      </div>
    </div>

    <div class="card table-card">
      <div class="table-header">
        <span class="total-count">총 <strong>{{ totalCount.toLocaleString() }}</strong>건</span>
      </div>

      <div class="table-wrapper custom-scrollbar">
        <table class="modern-table">
          <thead>
          <tr>
            <th>구역(Area)</th>
            <th>로케이션 코드</th>
            <th>Aisle(통로)</th>
            <th>방향(Side)</th>
            <th>Bay(열)</th>
            <th>Level(단)</th>
            <th>Deep(행)</th>
            <th>우선순위(Front)</th>
            <th>접근 등급</th>
            <th>접근 점수</th>
            <th>상태(Active)</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="isLoadingLocations">
            <td colspan="11" class="empty-text">데이터를 불러오는 중입니다...</td>
          </tr>
          <tr v-else-if="locationList.length === 0">
            <td colspan="11" class="empty-text">조회된 로케이션 데이터가 없습니다.</td>
          </tr>
          <tr v-else v-for="loc in locationList" :key="loc.id">
            <td>{{ loc.area_id || loc.areaId }}</td>
            <td class="font-bold">{{ loc.location_code || loc.locationCode }}</td>
            <td>{{ loc.aisle_no || loc.aisleNo }}</td>
            <td>
                <span :class="['badge', (loc.side_code || loc.sideCode) === 'L' ? 'badge-blue' : 'badge-green']">
                  {{ loc.side_code || loc.sideCode }}
                </span>
            </td>
            <td>{{ loc.bay_no || loc.bayNo }}</td>
            <td>{{ loc.level_no || loc.levelNo }}</td>
            <td>{{ loc.depth_no || loc.depthNo }}</td>
            <td>
              <span v-if="(loc.front_priority_yn || loc.frontPriorityYn) === 'Y'" class="badge badge-priority">Y</span>
              <span v-else class="text-gray">-</span>
            </td>
            <td>
              <span :class="['grade-badge', 'grade-' + (loc.location_grade || loc.locationGrade)]">
                {{ loc.location_grade || loc.locationGrade || '-' }}
              </span>
            </td>
            <td class="text-right">{{ loc.access_score || loc.accessScore || '-' }}</td>
            <td>
              <span class="status-dot" :class="(loc.active_yn || loc.activeYn) === 'Y' ? 'active' : 'inactive'"></span>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination" v-if="totalPages > 0">
        <button class="page-btn" :disabled="currentPage === 1" @click="changePage(1)">«</button>
        <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">‹</button>

        <button
          v-for="page in pageNumbers"
          :key="page"
          class="page-btn"
          :class="{ active: currentPage === page }"
          @click="changePage(page)">
          {{ page }}
        </button>

        <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">›</button>
        <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(totalPages)">»</button>
      </div>
    </div>

    <LocationGenerator
      :visible="showGenModal"
      @close="showGenModal = false"
      @generated="onLocationsGenerated"
    />

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
// 🔥 목록 조회 전용 API 임포트
import { getCommonGetListApi } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import LocationGenerator from './LocationGenerator.vue';

const { notification } = useMessage();

const locationList = ref([]);
const isLoadingLocations = ref(false);
const showGenModal = ref(false);

const totalCount = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);

const searchParams = reactive({
  locationCode: '',
  areaId: '',
  sideCode: '',
  locationGrade: ''
});

// 🔥 메인 리스트 조회 함수
const fetchLocations = async (page = 1) => {
  isLoadingLocations.value = true;
  currentPage.value = page;

  try {
    // 💡 핵심: 여기서도 절대 /rest 를 붙이지 않습니다!
    const url = `/tb_ac_location`;

    // 💡 page와 pageSize만 넘기면 getCommonGetListApi가 알아서 Elidom 규격으로 변환해 줍니다.
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    };

    if (searchParams.locationCode) params.location_code = searchParams.locationCode;
    if (searchParams.areaId) params.area_id = searchParams.areaId;
    if (searchParams.sideCode) params.side_code = searchParams.sideCode;
    if (searchParams.locationGrade) params.location_grade = searchParams.locationGrade;

    // API 호출
    const res = await getCommonGetListApi(url, params);

    locationList.value = res.items || res.records || res.content || [];
    totalCount.value = res.total || res.totalElements || locationList.value.length || 0;

  } catch (error) {
    console.error(error);
    notification.error({ message: '조회 실패', description: '로케이션 목록을 불러오지 못했습니다.' });
  } finally {
    isLoadingLocations.value = false;
  }
};

const handleSearch = () => {
  fetchLocations(1);
};

const handleReset = () => {
  searchParams.locationCode = '';
  searchParams.areaId = '';
  searchParams.sideCode = '';
  searchParams.locationGrade = '';
  fetchLocations(1);
};

const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value) || 1);

const pageNumbers = computed(() => {
  const maxVisiblePages = 5;
  let start = Math.max(1, currentPage.value - Math.floor(maxVisiblePages / 2));
  let end = start + maxVisiblePages - 1;

  if (end > totalPages.value) {
    end = totalPages.value;
    start = Math.max(1, end - maxVisiblePages + 1);
  }

  const pages = [];
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
});

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    fetchLocations(page);
  }
};

const openGenModal = () => {
  showGenModal.value = true;
};

const onLocationsGenerated = () => {
  showGenModal.value = false;
  fetchLocations(1);
};

onMounted(() => {
  fetchLocations(1);
});
</script>

<style scoped>
/* 기존 스타일 유지 */
.location-master-container { padding: 30px; font-family: -apple-system, sans-serif; color: #0f172a; background: #f1f5f9; min-height: 100vh; overflow-y: auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; border-bottom: 2px solid #e2e8f0; padding-bottom: 16px; }
.header-titles h2 { margin: 0 0 8px 0; font-size: 1.8rem; color: #1e293b;}
.header-titles p { margin: 0; color: #64748b; }
.header-actions { display: flex; gap: 10px; }

.btn-refresh { padding: 12px 16px; background: #fff; color: #475569; border: 1px solid #cbd5e1; border-radius: 6px; cursor: pointer; font-weight: bold; transition: 0.2s; }
.btn-refresh:hover { background: #f8fafc; }
.btn-primary { padding: 12px 20px; background: #4f46e5; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; transition: 0.2s; box-shadow: 0 4px 6px rgba(79, 70, 229, 0.2); }
.btn-primary:hover { background: #4338ca; transform: translateY(-1px); }

.card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); margin-bottom: 20px; }
.search-card { display: flex; flex-direction: column; gap: 16px; }
.search-grid { display: flex; gap: 20px; flex-wrap: wrap; }
.input-wrapper { display: flex; flex-direction: column; gap: 6px; flex: 1; min-width: 180px; }
.input-wrapper label { font-size: 0.85rem; font-weight: 600; color: #475569; }
.modern-input, .modern-select { padding: 10px 12px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: 0.95rem; color: #1e293b; outline: none; transition: border-color 0.2s; }
.modern-input:focus, .modern-select:focus { border-color: #4f46e5; box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1); }
.search-actions { display: flex; justify-content: flex-end; gap: 10px; border-top: 1px solid #e2e8f0; padding-top: 16px; }
.btn-reset { padding: 10px 16px; background: #f1f5f9; color: #475569; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; }
.btn-reset:hover { background: #e2e8f0; }
.btn-search { padding: 10px 24px; background: #10b981; color: #fff; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; transition: 0.2s; }
.btn-search:hover { background: #059669; }

.table-header { margin-bottom: 12px; }
.total-count { font-size: 0.95rem; color: #475569; }
.total-count strong { color: #4f46e5; font-size: 1.1rem; }
.table-wrapper { max-height: 55vh; overflow-y: auto; border: 1px solid #e2e8f0; border-radius: 8px; }
.modern-table { width: 100%; border-collapse: collapse; text-align: left; }
.modern-table th { background: #f8fafc; padding: 14px; font-size: 0.9rem; color: #475569; border-bottom: 1px solid #cbd5e1; position: sticky; top: 0; z-index: 10; font-weight: 700; white-space: nowrap; }
.modern-table td { padding: 12px 14px; border-bottom: 1px solid #e2e8f0; font-size: 0.9rem; color: #334155; }
.modern-table tr:hover td { background: #f8fafc; }
.font-bold { font-weight: 700; color: #0f172a; }
.empty-text { text-align: center; padding: 40px; color: #94a3b8; }
.text-right { text-align: right; }
.text-gray { color: #cbd5e1; }

.badge { padding: 4px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: bold; }
.badge-blue { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; }
.badge-green { background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; }
.badge-priority { background: #fef3c7; color: #d97706; border: 1px solid #fde68a; }

/* 🔥 등급 전용 뱃지 스타일 */
.grade-badge { padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 800; display: inline-block; width: 30px; text-align: center; }
.grade-A { background: #dcfce7; color: #15803d; }
.grade-B { background: #e0e7ff; color: #4338ca; }
.grade-C { background: #fef9c3; color: #a16207; }
.grade-D { background: #f3f4f6; color: #6b7280; }

.status-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; }
.status-dot.active { background: #10b981; box-shadow: 0 0 6px rgba(16, 185, 129, 0.4); }
.status-dot.inactive { background: #cbd5e1; }

.pagination { display: flex; justify-content: center; gap: 6px; margin-top: 20px; }
.page-btn { min-width: 36px; height: 36px; display: flex; justify-content: center; align-items: center; background: #fff; border: 1px solid #cbd5e1; border-radius: 6px; color: #475569; font-weight: 600; cursor: pointer; transition: 0.2s; }
.page-btn:hover:not(:disabled) { border-color: #4f46e5; color: #4f46e5; }
.page-btn.active { background: #4f46e5; color: #fff; border-color: #4f46e5; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; background: #f8fafc; }

.custom-scrollbar::-webkit-scrollbar { width: 8px; height: 8px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
</style>
