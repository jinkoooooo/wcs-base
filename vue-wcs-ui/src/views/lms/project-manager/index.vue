<template>
  <div class="inbound-page">
    <!-- 헤더 -->
    <div class="inbound-header">
      <h1 class="inbound-title">ANYWARE 프로젝트 관리</h1>

      <div class="inbound-header-actions">
        <button
          class="btn btn-ghost"
          @click="openAllWbs"
          :disabled="loading || main_all_ref.length === 0"
        >
          전체 현황
        </button>

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

        <button class="btn btn-ghost" @click="onReset" :disabled="loading">초기화</button>
      </div>
    </div>

    <!-- 검색 조건 (✅ Enter => 조회) -->
    <div class="card card-search" @keydown.enter.prevent="onSearch">
      <div class="search-grid search-grid-3">
        <div class="search-row">
          <span class="search-label">날짜</span>
          <DatePicker v-model="cond.date" />
        </div>

        <!-- ✅ 팀/파트: 서버 조회조건으로 전달 -->
        <div class="search-row">
          <span class="search-label">팀/파트</span>

          <select v-model="cond.team_name" class="form-select">
            <option v-for="o in team_options" :key="String(o.value)" :value="o.value">
              {{ o.label }}
            </option>
          </select>

          <select v-model="cond.part_name" class="form-select">
            <option v-for="o in part_options_view" :key="String(o.value)" :value="o.value">
              {{ o.label }}
            </option>
          </select>
        </div>

        <div class="search-row">
          <span class="search-label">프로젝트 명</span>
          <input v-model.trim="cond.project_name" class="form-input" placeholder="project_name" />
        </div>
      </div>
    </div>

    <!-- 상/하 2그리드 -->
    <div class="split-wrap">
      <!-- 상단 그리드 -->
      <div class="card card-grid split-half">
        <div class="grid-title">
          <span class="section-title">프로젝트 목록</span>
        </div>

        <div class="grid-body">
          <SimpleGrid
            :columns="topColumns"
            :rows="main_ref"
            row-key="id"
            height="100%"
            v-model:selectedKey="focusedKey"
            @row-dblclick="openWbsModal"
          />
        </div>

        <div class="grid-footer">
          <div class="footer-left">
            <span class="footer-note"
              >* 상위 프로젝트 생성 시 일정 스탭 자동생성 / 프로젝트 더블클릭 시 WBS 확인</span
            >
          </div>

          <div class="footer-actions">
            <button class="btn btn-ghost" @click="onAdd" :disabled="loading">추가</button>
            <button class="btn btn-ghost" @click="onDelete" :disabled="loading || !focusedKey">
              삭제
            </button>
            <button class="btn btn-primary" @click="onSave" :disabled="loading || !hasSaveTarget">
              저장
            </button>
          </div>
        </div>
      </div>

      <!-- 하단 그리드 -->
      <div class="card card-grid split-half">
        <div class="grid-title">
          <span class="section-title">프로젝트 일정</span>
        </div>

        <div class="grid-body">
          <SimpleGrid :columns="bottomColumns" :rows="bottomRows" row-key="id" height="100%">
            <!-- ✅ 헤더 + 버튼 -->
            <template #header-__ctrl>
              <button
                class="grid-icon-btn grid-icon-btn--add"
                :disabled="loading || !focusedKey"
                title="로우 추가"
                @click.stop="addBottomRow"
              >
                +
              </button>
            </template>

            <!-- ✅ 로우 - 버튼 -->
            <template #cell-__ctrl="{ row }">
              <button
                class="grid-icon-btn grid-icon-btn--del"
                title="로우 삭제"
                @click.stop="deleteBottomRow(row)"
              >
                −
              </button>
            </template>
          </SimpleGrid>
        </div>
      </div>
    </div>

    <WbsModal v-model="wbsOpen" :main="wbsMain" :steps="wbsSteps" :step-master="PM_STEP_MASTER" />
    <AllProjectsWbsModal
      v-if="allWbsOpen"
      :projects="main_all_ref"
      :baseStepUrl="BASE_STEP_URL"
      @close="allWbsOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref, watch } from 'vue';
  import DatePicker from './components/DatePicker.vue';
  import SimpleGrid from './components/SimpleGrid.vue';
  import { getSearchList, updateList } from '/@/api/common/api';
  import { defHttp } from '/@/utils/http/axios';
  import WbsModal from './modal/WbsModal.vue';
  import AllProjectsWbsModal from './modal/AllProjectsWbsModal.vue';

  /* =========================
   * User API (tb_pm_user)
   * ========================= */
  type PmUserApiRow = {
    id?: string;
    user_id?: string;
    user_name?: string;
    email?: string;
    team_name?: string;
    part_name?: string;
    position_name?: string;
    employment_status?: string;
    use_yn?: string;
  };

  type SelectItem = { value: any; label: string };

  // ✅ 저장=user_id / 표시=user_name
  const plUserOptions = ref<SelectItem[]>([{ value: null, label: '-' }]);

  // ✅ 팀/파트 옵션
  const team_options = ref<SelectItem[]>([{ value: null, label: '-' }]);
  const part_options = ref<SelectItem[]>([{ value: null, label: '-' }]);
  const part_options_by_team = ref<Record<string, SelectItem[]>>({});

  const cond = reactive({
    date: { from: '', to: '' },
    team_name: null as string | null,
    part_name: null as string | null,
    project_name: '',
  });

  const part_options_view = computed(() => {
    const t = String(cond.team_name ?? '').trim();
    if (t && part_options_by_team.value[t]) return part_options_by_team.value[t];
    return part_options.value;
  });

  // ✅ user_id -> user row 매핑
  const pm_user_by_user_id = ref<Record<string, PmUserApiRow>>({});

  function extractList(res: any): any[] {
    if (Array.isArray(res)) return res;
    if (Array.isArray(res?.items)) return res.items;
    if (Array.isArray(res?.list)) return res.list;
    if (Array.isArray(res?.data)) return res.data;
    if (Array.isArray(res?.results)) return res.results;
    return [];
  }

  async function fetchPmUsers() {
    const res = await getSearchList(BASE_USER_URL, { page: 1, limit: 2000 });
    const list = extractList(res) as PmUserApiRow[];

    const filtered = (list ?? [])
      .filter((u) => String(u?.use_yn ?? '').toUpperCase() === 'Y')
      .filter((u) => String(u?.user_id ?? '').trim() !== '')
      .filter((u) => String(u?.user_name ?? '').trim() !== '');

    // ✅ user_id -> row 맵
    const map: Record<string, PmUserApiRow> = {};
    for (const u of filtered) {
      const user_id = String(u.user_id ?? '').trim();
      if (!user_id) continue;
      map[user_id] = u;
    }
    pm_user_by_user_id.value = map;

    // ✅ PL 옵션(중복 제거: user_id 기준)
    const pl_items = filtered
      .slice()
      .sort((a, b) => String(a.user_name ?? '').localeCompare(String(b.user_name ?? ''), 'ko'));

    const seen = new Set<string>();
    const items: SelectItem[] = [];
    for (const u of pl_items) {
      const user_id = String(u.user_id ?? '').trim();
      const user_name = String(u.user_name ?? '').trim();
      if (!user_id || !user_name) continue;
      if (seen.has(user_id)) continue;
      seen.add(user_id);
      items.push({ value: user_id, label: user_name });
    }
    plUserOptions.value = [{ value: null, label: '-' }, ...items];

    // ✅ 팀/파트 옵션
    const team_set = new Set<string>();
    const part_set = new Set<string>();
    const team_to_parts = new Map<string, Set<string>>();

    for (const u of filtered) {
      const team_name = String(u.team_name ?? '').trim();
      const part_name = String(u.part_name ?? '').trim();

      if (team_name) team_set.add(team_name);
      if (part_name) part_set.add(part_name);

      if (team_name && part_name) {
        if (!team_to_parts.has(team_name)) team_to_parts.set(team_name, new Set<string>());
        team_to_parts.get(team_name)!.add(part_name);
      }
    }

    const teams = Array.from(team_set).sort((a, b) => a.localeCompare(b, 'ko'));
    team_options.value = [
      { value: null, label: '-' },
      ...teams.map((t) => ({ value: t, label: t })),
    ];

    const parts = Array.from(part_set).sort((a, b) => a.localeCompare(b, 'ko'));
    part_options.value = [
      { value: null, label: '-' },
      ...parts.map((p) => ({ value: p, label: p })),
    ];

    const by_team: Record<string, SelectItem[]> = {};
    for (const [t, set] of team_to_parts.entries()) {
      const arr = Array.from(set).sort((a, b) => a.localeCompare(b, 'ko'));
      by_team[t] = [{ value: null, label: '-' }, ...arr.map((p) => ({ value: p, label: p }))];
    }
    part_options_by_team.value = by_team;
  }

  /* =========================
   * Modal
   * ========================= */
  const allWbsOpen = ref(false);
  function openAllWbs() {
    allWbsOpen.value = true;
  }

  const wbsOpen = ref(false);
  const wbsMain = ref<PmProjectMainUiRow | null>(null);
  const wbsSteps = ref<PmProjectStepUiRow[]>([]);

  async function openWbsModal(row: PmProjectMainUiRow) {
    const id = String(row?.id ?? '');
    if (!id) return;

    focusedKey.value = id;

    if (!detailRef.value[id] && !isTempId(id)) {
      await fetchStepsForMain(id, true);
    }

    wbsMain.value = row;
    wbsSteps.value = (detailRef.value[id] ?? [])
      .slice()
      .sort((a, b) => Number(a.step_cd ?? 0) - Number(b.step_cd ?? 0));

    wbsOpen.value = true;
  }

  /* =========================
   * Types
   * ========================= */
  type CudFlag = 'c' | 'u' | 'd';
  type UiMeta = { _edit?: boolean; _isNew?: boolean; cud_flag_?: CudFlag };

  const CUD = {
    CREATE: 'c',
    UPDATE: 'u',
    DELETE: 'd',
  } as const;

  function newUuid(): string {
    if (typeof crypto !== 'undefined' && typeof (crypto as any).randomUUID === 'function') {
      return (crypto as any).randomUUID();
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  function ensureIdsForBatchSave() {
    const tmpToRealMainId = new Map<string, string>();

    for (const main of main_all_ref.value) {
      const oldId = String(main.id ?? '');
      if (!oldId || isTempId(oldId)) {
        const realId = newUuid();
        main.id = realId;
        main._isNew = true;

        if (oldId) tmpToRealMainId.set(oldId, realId);
        if (focusedKey.value === oldId) focusedKey.value = realId;
      }
    }

    for (const [tmpId, realId] of tmpToRealMainId.entries()) {
      moveDetailKey(tmpId, realId);
    }

    for (const mainId of Object.keys(detailRef.value)) {
      const steps = detailRef.value[mainId] ?? [];
      for (const step of steps) {
        const sid = String(step.id ?? '');
        if (!sid || isTempId(sid)) {
          step.id = newUuid();
          step._isNew = true;
        }
      }
    }
  }

  export type PmProjectMainApiRow = {
    id?: string;
    project_name?: string | null;

    owner_team_name?: string | null;
    owner_part_name?: string | null;

    project_status_cd?: number | string | null;
    supply_site?: string | null;
    contract_dt?: string | null;
    start_dt?: string | null;
    project_end_dt?: string | null;
    start_year?: number | string | null;
    end_year?: number | string | null;
    final_customer_name?: string | null;
    final_customer_contact?: string | null;
    mid_customer_name?: string | null;
    mid_customer_contact?: string | null;
    contract_company_name?: string | null;

    // ✅ pl_name/sub_pl_name 컬럼에는 user_id 저장
    pl_name?: string | null;
    sub_pl_name?: string | null;

    sales_contract_amt?: number | string | null;
    equip_purchase_amt?: number | string | null;
    outsource_purchase_amt?: number | string | null;
    remark?: string | null;
    use_yn?: string | boolean | null;
  };

  export type PmProjectStepApiRow = {
    id?: string;
    project_main_id?: string | null;
    step_cd?: number | string | null;
    step_desc?: string | null;
    start_date?: string | null;
    end_date?: string | null;
    duration_days?: number | string | null;
    dev1_name?: string | null;
    dev2_name?: string | null;
    dev3_name?: string | null;
    dev4_name?: string | null;
  };

  type PmProjectMainUiRow = PmProjectMainApiRow & UiMeta;
  type PmProjectStepUiRow = PmProjectStepApiRow & UiMeta;

  /* =========================
   * Constants (Step/Status)
   * ========================= */
  const PM_STEP_MASTER = [
    { code: 0, desc: '제안' },
    { code: 10, desc: '계약' },
    { code: 20, desc: '분석' },
    { code: 30, desc: '설계' },
    { code: 40, desc: '개발' },
    { code: 50, desc: '시뮬레이션테스트' },
    { code: 60, desc: '단동테스트' },
    { code: 70, desc: '연동테스트' },
    { code: 80, desc: '오픈' },
    { code: 90, desc: '안정화' },
    { code: 91, desc: '하자보수' },
    { code: 92, desc: '추가 유지보수 계약' },
  ] as const;

  const STEP_DESC_BY_CD = new Map<string, string>(
    PM_STEP_MASTER.map((s) => [String(s.code), s.desc]),
  );
  const STEP_OPTIONS: SelectItem[] = [
    { value: null, label: '-' },
    ...PM_STEP_MASTER.map((s) => ({ value: s.code, label: `${s.code} - ${s.desc}` })),
  ];

  function syncStepDescByCd(step: PmProjectStepUiRow) {
    const k = String(step.step_cd ?? '').trim();
    const next = k ? STEP_DESC_BY_CD.get(k) ?? null : null;
    if (String(step.step_desc ?? '') !== String(next ?? '')) {
      step.step_desc = next as any;
    }
  }

  const PROJECT_STATUS_OPTIONS = PM_STEP_MASTER.map((s) => ({
    value: s.code,
    label: `${s.code} - ${s.desc}`,
  }));

  const USE_YN = [
    { value: 'Y', label: '사용' },
    { value: 'N', label: '미사용' },
  ];

  /* =========================
   * State
   * ========================= */
  const loading = ref(false);

  // ✅ 서버에서 내려온 로우(=화면 로우)
  const main_all_ref = ref<PmProjectMainUiRow[]>([]);
  const main_ref = ref<PmProjectMainUiRow[]>([]);

  const detailRef = ref<Record<string, PmProjectStepUiRow[]>>({});
  const focusedKey = ref<string | null>(null);

  const pendingDeleteMainIds = ref<string[]>([]);
  const pendingDeleteStepIds = ref<string[]>([]);

  const mainSnapshot = ref(new Map<string, string>());
  const stepSnapshot = ref(new Map<string, string>());

  function stripMainForDirty(r: any) {
    const { _edit, _isNew, cud_flag_, ...rest } = r;
    return rest;
  }
  function stripStepForDirty(r: any) {
    const { _edit, _isNew, cud_flag_, ...rest } = r;
    return rest;
  }

  /* =========================
   * Computed
   * ========================= */
  const BASE_MAIN_URL = '/tb_pm_project_main'
  const BASE_STEP_URL = '/tb_pm_project_detail_step'
  const BASE_USER_URL = '/tb_pm_user'

  const bottomRows = computed(() => {
    const k = focusedKey.value;
    if (!k) return [];
    return detailRef.value?.[k] ?? [];
  });

  const hasSaveTarget = computed(() => {
    return (
      main_all_ref.value.length > 0 ||
      pendingDeleteMainIds.value.length > 0 ||
      pendingDeleteStepIds.value.length > 0
    );
  });

  /* =========================
   * Grid Columns
   * ========================= */
  const topColumns = computed(() => [
    { key: 'project_name', label: '프로젝트명', width: 300, editor: 'text' },

    // ✅ NEW: 담당팀/담당파트 (tb_pm_user에서 만든 옵션으로 select)
    {
      key: 'owner_team_name',
      label: '담당팀',
      width: 140,
      align: 'center' as const,
      editor: 'select',
      editorOptions: { items: team_options.value },
    },
    {
      key: 'owner_part_name',
      label: '담당파트',
      width: 140,
      align: 'center' as const,
      editor: 'select',
      // 팀 종속 옵션까지 하려면 SimpleGrid가 row 기반 동적 items를 지원해야 함.
      // 우선 전체 파트 목록 제공(요구사항: user 테이블의 파트 값으로 선택) 충족.
      editorOptions: { items: part_options.value },
    },

    {
      key: 'project_status_cd',
      label: '상태',
      width: 100,
      align: 'center' as const,
      editor: 'select',
      editorOptions: { items: PROJECT_STATUS_OPTIONS },
    },
    { key: 'supply_site', label: '공급장소(현장)', width: 160, editor: 'text' },
    {
      key: 'contract_dt',
      label: '계약일자',
      width: 160,
      align: 'center' as const,
      editor: 'date-ymd',
    },
    {
      key: 'project_end_dt',
      label: '종료일자',
      width: 160,
      align: 'center' as const,
      editor: 'date-ymd',
    },

    { key: 'final_customer_name', label: '최종 고객사', width: 140, editor: 'text' },
    { key: 'final_customer_contact', label: '담당자', width: 100, editor: 'text' },
    { key: 'mid_customer_name', label: '중도 고객사', width: 140, editor: 'text' },
    { key: 'mid_customer_contact', label: '담당자', width: 100, editor: 'text' },
    { key: 'contract_company_name', label: '계약사', width: 140, editor: 'text' },

    {
      key: 'pl_name', // ✅ 저장은 user_id
      label: 'SYS PL',
      width: 100,
      align: 'center' as const,
      editor: 'select',
      editorOptions: { items: plUserOptions.value },
    },
    {
      key: 'sub_pl_name', // ✅ 저장은 user_id
      label: 'SUB PL',
      width: 100,
      align: 'center' as const,
      editor: 'select',
      editorOptions: { items: plUserOptions.value },
    },

    {
      key: 'sales_contract_amt',
      label: '매출 계약금',
      width: 120,
      align: 'right' as const,
      editor: 'money',
    },
    {
      key: 'equip_purchase_amt',
      label: '장비 매입금',
      width: 120,
      align: 'right' as const,
      editor: 'money',
    },
    {
      key: 'outsource_purchase_amt',
      label: '외주 매입금',
      width: 120,
      align: 'right' as const,
      editor: 'money',
    },

    { key: 'remark', label: '비고', width: 180, editor: 'text' },
    {
      key: 'use_yn',
      label: '사용여부',
      width: 90,
      align: 'center' as const,
      editor: 'select',
      editorOptions: { items: USE_YN },
    },
  ]);

  const bottomColumns = computed(() => [
    { key: '__ctrl', label: '', width: 32, align: 'center' as const },
    {
      key: 'step_cd',
      label: 'Step',
      width: 110,
      align: 'center' as const,
      editor: 'select',
      editorOptions: { items: STEP_OPTIONS },
    },
    { key: 'step_desc', label: '설명', width: 140, editor: 'readonly' },
    {
      key: 'start_date',
      label: '시작일자',
      width: 100,
      align: 'center' as const,
      editor: 'date-ymd',
    },
    {
      key: 'end_date',
      label: '종료일자',
      width: 100,
      align: 'center' as const,
      editor: 'date-ymd',
    },
    {
      key: 'duration_days',
      label: '기간(일)',
      width: 60,
      align: 'right' as const,
      editor: 'readonly',
    },

    {
      key: 'dev1_name',
      label: '담당자1',
      width: 120,
      editor: 'select',
      editorOptions: { items: plUserOptions.value },
    },
    {
      key: 'dev2_name',
      label: '담당자2',
      width: 120,
      editor: 'select',
      editorOptions: { items: plUserOptions.value },
    },
    {
      key: 'dev3_name',
      label: '담당자3',
      width: 120,
      editor: 'select',
      editorOptions: { items: plUserOptions.value },
    },
    {
      key: 'dev4_name',
      label: '담당자4',
      width: 120,
      editor: 'select',
      editorOptions: { items: plUserOptions.value },
    },
  ]);

  /* =========================
   * Utils (ID / sanitize)
   * ========================= */
  function newTempId(prefix: string) {
    return `${prefix}-tmp-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
  function isTempId(id: any) {
    return String(id ?? '').includes('-tmp-');
  }
  // =========================
  // ✅ Sort helpers (YMD)
  // - 상단: contract_dt DESC (최근이 위)
  // - 하단: start_date ASC (빠른 시작이 위)
  // - 날짜 없으면(null/''/invalid) 맨 아래
  // - 신규(_isNew / temp id) 는 항상 맨 위
  // =========================
  function ymdKey(v: any): string {
    const s = String(v ?? '').trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return '';
    return s.replaceAll('-', ''); // YYYYMMDD
  }

  function cmpYmdAsc(a: any, b: any): number {
    const ka = ymdKey(a);
    const kb = ymdKey(b);
    if (!ka && !kb) return 0;
    if (!ka) return 1; // 날짜 없는 값은 아래
    if (!kb) return -1;
    return ka.localeCompare(kb); // ASC
  }

  function cmpYmdDesc(a: any, b: any): number {
    const ka = ymdKey(a);
    const kb = ymdKey(b);
    if (!ka && !kb) return 0;
    if (!ka) return 1; // 날짜 없는 값은 아래
    if (!kb) return -1;
    return kb.localeCompare(ka); // DESC
  }

  function isNewRow(r: any): boolean {
    return r?._isNew === true || isTempId(r?.id);
  }
  function pushUnique(list: string[], v: string) {
    if (!v) return;
    if (!list.includes(v)) list.push(v);
  }
  function clearPending() {
    pendingDeleteMainIds.value = [];
    pendingDeleteStepIds.value = [];
  }

  function sanitizeMain(row: any) {
    const copy: any = { ...row };
    delete copy._edit;
    delete copy._isNew;
    if (isTempId(copy.id)) delete copy.id;

    copy.contract_dt = toServerDateTime(copy.contract_dt, '00:00:00');
    copy.project_end_dt = toServerDateTime(copy.project_end_dt, '00:00:00');
    copy.start_dt = toServerDateTime(copy.start_dt, '00:00:00');

    return copy;
  }

  function sanitizeStep(row: any) {
    const copy: any = { ...row };
    delete copy._edit;
    delete copy._isNew;
    if (isTempId(copy.id)) delete copy.id;

    copy.start_date = toServerDateTime(copy.start_date, '00:00:00');
    copy.end_date = toServerDateTime(copy.end_date, '00:00:00');

    return copy;
  }

  function toServerDateTime(v: any, defaultHms = '00:00:00'): string | null {
    const s = String(v ?? '').trim();
    if (!s) return null;

    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return `${s} ${defaultHms}`;
    if (/^\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}$/.test(s)) return `${s}:00`;
    if (/^\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}$/.test(s)) return s;

    const m1 = s.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/);
    if (m1) return `${m1[1]} ${m1[2]}:00`;

    const m2 = s.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})/);
    if (m2) return `${m2[1]} ${m2[2]}`;

    return null;
  }

  function toUiYmd(v: any): string | null {
    const s = String(v ?? '').trim();
    if (!s) return null;
    const m = s.match(/^(\d{4}-\d{2}-\d{2})/);
    return m ? m[1] : null;
  }

  function buildDefaultSteps(mainId: string): PmProjectStepUiRow[] {
    return PM_STEP_MASTER.map((s) => ({
      id: newTempId('step'),
      project_main_id: mainId,
      step_cd: s.code,
      step_desc: s.desc,
      start_date: null,
      end_date: null,
      duration_days: null,
      dev1_name: null,
      dev2_name: null,
      dev3_name: null,
      dev4_name: null,
      _edit: true,
      _isNew: true,
    }));
  }

  function normalizeFocusAfterListChange() {
    if (!main_ref.value.length) {
      focusedKey.value = null;
      return;
    }
    if (!focusedKey.value) {
      focusedKey.value = String(main_ref.value[0].id ?? '');
      return;
    }
    const exists = main_ref.value.some((m) => String(m.id ?? '') === String(focusedKey.value));
    if (!exists) focusedKey.value = String(main_ref.value[0].id ?? '');
  }

  function moveDetailKey(oldKey: string, newKey: string) {
    if (oldKey === newKey) return;
    const oldSteps = detailRef.value?.[oldKey];
    if (!oldSteps) return;

    for (const s of oldSteps) s.project_main_id = newKey;

    detailRef.value[newKey] = oldSteps;
    delete detailRef.value[oldKey];
  }

  function pickYearFromDateTime(v: any): string | null {
    const s = String(v ?? '').trim();
    if (!s) return null;
    const y = s.slice(0, 4);
    return /^\d{4}$/.test(y) ? y : null;
  }

  function syncMainYears(row: PmProjectMainUiRow) {
    const sy = pickYearFromDateTime(row.contract_dt);
    const ey = pickYearFromDateTime(row.project_end_dt);

    if (sy !== null && String(row.start_year ?? '') !== sy) row.start_year = sy;
    if (sy === null && row.contract_dt == null && row.start_year != null) row.start_year = null;

    if (ey !== null && String(row.end_year ?? '') !== ey) row.end_year = ey;
    if (ey === null && row.project_end_dt == null && row.end_year != null) row.end_year = null;
  }

  /* =========================
   * 기간(일) 계산 로직 (그대로)
   * ========================= */
  const KR_FIXED_HOLIDAYS_MMDD = [
    '01-01',
    '03-01',
    '05-05',
    '06-06',
    '08-15',
    '10-03',
    '10-09',
    '12-25',
  ];
  const EXTRA_HOLIDAYS_BY_YEAR: Record<number, string[]> = {};
  const holidayCache = new Map<number, Set<string>>();

  function pad2(n: number) {
    return String(n).padStart(2, '0');
  }
  function ymd(d: Date) {
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  }
  function parseYmd(s: any): Date | null {
    const v = String(s ?? '').trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
    const [y, m, d] = v.split('-').map((x) => Number(x));
    const dt = new Date(y, m - 1, d);
    return Number.isNaN(dt.getTime()) ? null : dt;
  }

  function addObservedHoliday(set: Set<string>, base: Date) {
    const d = new Date(base);
    while (true) {
      const dow = d.getDay();
      const key = ymd(d);
      if (dow !== 0 && dow !== 6 && !set.has(key)) {
        set.add(key);
        break;
      }
      d.setDate(d.getDate() + 1);
    }
  }

  function getHolidaySet(year: number): Set<string> {
    if (holidayCache.has(year)) return holidayCache.get(year)!;

    const set = new Set<string>();
    for (const mmdd of KR_FIXED_HOLIDAYS_MMDD) {
      const base = new Date(year, Number(mmdd.slice(0, 2)) - 1, Number(mmdd.slice(3, 5)));
      const key = ymd(base);
      set.add(key);

      const dow = base.getDay();
      if (dow === 0 || dow === 6)
        addObservedHoliday(set, new Date(base.getTime() + 24 * 3600 * 1000));
    }

    const extras = EXTRA_HOLIDAYS_BY_YEAR[year] ?? [];
    for (const e of extras) set.add(e);

    holidayCache.set(year, set);
    return set;
  }

  function isBusinessDay(d: Date): boolean {
    const dow = d.getDay();
    if (dow === 0 || dow === 6) return false;
    const set = getHolidaySet(d.getFullYear());
    return !set.has(ymd(d));
  }

  function calcBusinessDaysKr(startYmd: any, endYmd: any): number | null {
    const start = parseYmd(startYmd);
    const end = parseYmd(endYmd);
    if (!start || !end) return null;
    if (end.getTime() < start.getTime()) return null;

    let cnt = 0;
    const cur = new Date(start);
    while (cur.getTime() <= end.getTime()) {
      if (isBusinessDay(cur)) cnt += 1;
      cur.setDate(cur.getDate() + 1);
    }
    return cnt;
  }

  function syncStepDuration(step: PmProjectStepUiRow) {
    const days = calcBusinessDaysKr(step.start_date, step.end_date);
    const next = days == null ? null : days;
    if (String(step.duration_days ?? '') !== String(next ?? '')) {
      step.duration_days = next;
    }
  }

  /* =========================
   * ✅ 검색 team 변경 시 part 초기화만 (조회는 버튼/엔터)
   * ========================= */
  watch(
    () => cond.team_name,
    () => {
      cond.part_name = null;
    },
  );

  /* =========================
   * Fetch (Main/Step)
   * ========================= */
  async function fetchMainFromApi() {
    // ✅ team/part 포함해서 서버에서 필터링
    const params = {
      from_year: cond.date.from,
      to_year: cond.date.to,
      project_name: cond.project_name,
      owner_team_name: cond.team_name,
      owner_part_name: cond.part_name,
    };

    const apiList = await defHttp.post({ url: `${BASE_MAIN_URL}/project_main_info`, data: params });

    const rows: PmProjectMainUiRow[] = Array.isArray(apiList)
      ? (apiList as PmProjectMainApiRow[]).map((r) => ({
          ...r,
          contract_dt: toUiYmd(r.contract_dt),
          project_end_dt: toUiYmd(r.project_end_dt),
          _edit: false,
          _isNew: false,
        }))
      : [];

    for (const m of rows) syncMainYears(m);

    // ✅ 계약일자(contract_dt) ASC, null은 뒤로
    rows.sort((a, b) => {
      const da = a.contract_dt ? String(a.contract_dt) : '9999-12-31';
      const db = b.contract_dt ? String(b.contract_dt) : '9999-12-31';
      return da.localeCompare(db) || String(a.project_name ?? '').localeCompare(String(b.project_name ?? ''), 'ko');
    });

    // ✅ 서버 결과 그대로 사용(화면 필터 제거)
    main_all_ref.value = rows;
    main_ref.value = rows;

    mainSnapshot.value = new Map(
      main_all_ref.value
        .filter((r) => r.id)
        .map((r) => [String(r.id), JSON.stringify(stripMainForDirty(r))]),
    );
  }

  async function fetchStepsForMain(mainId: string, force = false) {
    if (!mainId) return;
    if (isTempId(mainId)) return;
    if (!force && detailRef.value[mainId]) return;

    const params = { main_id: mainId };
    const apiList = await defHttp.post({ url: `${BASE_STEP_URL}/project_detail_step_info`, data: params });

    const steps: PmProjectStepUiRow[] = Array.isArray(apiList)
      ? (apiList as PmProjectStepApiRow[]).map((s) => {
          const row: PmProjectStepUiRow = {
            ...s,
            start_date: toUiYmd(s.start_date),
            end_date: toUiYmd(s.end_date),
            _edit: false,
            _isNew: false,
          };
          syncStepDescByCd(row);
          return row;
        })
      : [];

    // ✅ 하단 그리드: 시작일자(start_date) ASC 정렬
    steps.sort((a, b) => {
      const sa = a.start_date ? String(a.start_date) : '9999-12-31';
      const sb = b.start_date ? String(b.start_date) : '9999-12-31';
      const c1 = sa.localeCompare(sb);
      if (c1 !== 0) return c1;

      const ea = a.end_date ? String(a.end_date) : '9999-12-31';
      const eb = b.end_date ? String(b.end_date) : '9999-12-31';
      const c2 = ea.localeCompare(eb);
      if (c2 !== 0) return c2;

      return Number(a.step_cd ?? 999999) - Number(b.step_cd ?? 999999);
    });

    for (const s of steps) {
      syncStepDuration(s);
      syncStepDescByCd(s);
    }

    detailRef.value[mainId] = steps;

    for (const s of steps) {
      if (!s.id) continue;
      stepSnapshot.value.set(String(s.id), JSON.stringify(stripStepForDirty(s)));
    }
  }

  async function fetchAllFromApi() {
    loading.value = true;
    try {
      await fetchMainFromApi();
      detailRef.value = {};
      normalizeFocusAfterListChange();

      if (focusedKey.value) {
        await fetchStepsForMain(String(focusedKey.value), true);
      }
    } finally {
      loading.value = false;
    }
  }

  /* =========================
   * UI Actions
   * ========================= */
  async function onSearch() {
    clearPending();
    await fetchAllFromApi();
  }

  function onReset() {
    cond.project_name = '';
    cond.team_name = null;
    cond.part_name = null;

    focusedKey.value = null;
    clearPending();
    onSearch();
  }

  function onAdd() {
    const tmpMainId = newTempId('main');
    const nowYear = String(new Date().getFullYear());

    const newMain: PmProjectMainUiRow = {
      id: tmpMainId,
      project_name: '',

      // ✅ NEW
      owner_team_name: null,
      owner_part_name: null,

      project_status_cd: 0,
      supply_site: null,
      contract_dt: null,
      start_dt: null,
      project_end_dt: null,
      start_year: nowYear,
      end_year: nowYear,
      final_customer_name: null,
      final_customer_contact: null,
      mid_customer_name: null,
      mid_customer_contact: null,
      contract_company_name: null,
      pl_name: null,
      sub_pl_name: null,
      sales_contract_amt: null,
      equip_purchase_amt: null,
      outsource_purchase_amt: null,
      remark: null,
      use_yn: 'Y',
      _edit: true,
      _isNew: true,
    };

    main_all_ref.value = [newMain, ...main_all_ref.value];
    main_ref.value = main_all_ref.value;

    focusedKey.value = tmpMainId;

    const steps = buildDefaultSteps(tmpMainId);
    for (const s of steps) {
      syncStepDuration(s);
      syncStepDescByCd(s);
    }
    detailRef.value[tmpMainId] = steps;
  }

  function onDelete() {
    const mainId = focusedKey.value;
    if (!mainId) return;

    const idxAll = main_all_ref.value.findIndex((m) => String(m.id ?? '') === String(mainId));
    if (idxAll < 0) return;

    const steps = detailRef.value?.[String(mainId)] ?? [];
    for (const s of steps) {
      const sid = String(s.id ?? '');
      if (sid && !isTempId(sid)) pushUnique(pendingDeleteStepIds.value, sid);
    }

    const mid = String(main_all_ref.value[idxAll].id ?? '');
    if (mid && !isTempId(mid)) pushUnique(pendingDeleteMainIds.value, mid);

    main_all_ref.value.splice(idxAll, 1);
    main_ref.value = main_all_ref.value;

    delete detailRef.value[String(mainId)];

    normalizeFocusAfterListChange();
  }

  async function onSave() {
    if (loading.value) return;
    loading.value = true;

    try {
      ensureIdsForBatchSave();

      const mainCU: any[] = [];
      for (const main of main_all_ref.value) {
        syncMainYears(main);

        const flag = main._isNew === true ? CUD.CREATE : main._edit === true ? CUD.UPDATE : null;
        if (!flag) continue;

        const payload = sanitizeMain({ ...main, cud_flag_: flag });
        mainCU.push(payload);
      }

      const stepCU: any[] = [];
      for (const mainId of Object.keys(detailRef.value)) {
        const steps = detailRef.value[mainId] ?? [];

        for (const step of steps) {
          step.project_main_id = mainId;
          syncStepDuration(step);
          syncStepDescByCd(step);

          const flag = step._isNew === true ? CUD.CREATE : step._edit === true ? CUD.UPDATE : null;
          if (!flag) continue;

          const payload = sanitizeStep({ ...step, cud_flag_: flag });
          stepCU.push(payload);
        }
      }

      const stepD: any[] = pendingDeleteStepIds.value.map((id) => ({ id, cud_flag_: CUD.DELETE }));
      const mainD: any[] = pendingDeleteMainIds.value.map((id) => ({ id, cud_flag_: CUD.DELETE }));

      if (mainCU.length) await updateList(`${BASE_MAIN_URL}/update_multiple`, mainCU);
      if (stepCU.length) await updateList(`${BASE_STEP_URL}/update_multiple`, stepCU);
      if (stepD.length) await updateList(`${BASE_STEP_URL}/update_multiple`, stepD);
      if (mainD.length) await updateList(`${BASE_MAIN_URL}/update_multiple`, mainD);

      clearPending();

      await fetchMainFromApi();
      detailRef.value = {};
      normalizeFocusAfterListChange();

      if (focusedKey.value) {
        await fetchStepsForMain(String(focusedKey.value), true);
      }
    } finally {
      loading.value = false;
    }
  }

  function ensureBottomList(): PmProjectStepUiRow[] {
    const k = String(focusedKey.value ?? '');
    if (!k) return [];
    if (!detailRef.value[k]) detailRef.value[k] = [];
    return detailRef.value[k];
  }

  function addBottomRow() {
    const k = String(focusedKey.value ?? '');
    if (!k) return;

    const list = ensureBottomList();

    const row: PmProjectStepUiRow = {
      id: newTempId('step'),
      project_main_id: k,
      step_cd: null,
      step_desc: null,
      start_date: null,
      end_date: null,
      duration_days: null,
      dev1_name: null,
      dev2_name: null,
      dev3_name: null,
      dev4_name: null,
      _edit: true,
      _isNew: true,
    };

    list.unshift(row);
  }

  function deleteBottomRow(row: PmProjectStepUiRow) {
    const k = String(focusedKey.value ?? '');
    if (!k) return;

    const list = ensureBottomList();
    const idx = list.findIndex((x) => String(x.id ?? '') === String(row.id ?? ''));
    if (idx < 0) return;

    const sid = String(row.id ?? '');
    if (sid && !isTempId(sid)) pushUnique(pendingDeleteStepIds.value, sid);

    list.splice(idx, 1);
  }

  /* =========================
   * Watch
   * ========================= */
  watch(
    () => focusedKey.value,
    async (k) => {
      if (!k) return;
      await fetchStepsForMain(String(k), false);
    },
  );

  watch(
    () => main_all_ref.value,
    (list) => {
      for (const m of list) {
        syncMainYears(m);

        if (m._isNew) continue;

        const id = String(m.id ?? '');
        if (!id) continue;

        const prev = mainSnapshot.value.get(id);
        const now = JSON.stringify(stripMainForDirty(m));

        if (!prev) {
          mainSnapshot.value.set(id, now);
          continue;
        }

        const dirty = prev !== now;
        if (m._edit !== dirty) m._edit = dirty;
      }
    },
    { deep: true },
  );

  watch(
    () => detailRef.value,
    (map) => {
      for (const mainId of Object.keys(map)) {
        for (const s of map[mainId] ?? []) {
          syncStepDuration(s);
          syncStepDescByCd(s);

          if (s._isNew) continue;

          const id = String(s.id ?? '');
          if (!id) continue;

          const prev = stepSnapshot.value.get(id);
          const now = JSON.stringify(stripStepForDirty(s));

          if (!prev) {
            stepSnapshot.value.set(id, now);
            continue;
          }

          const dirty = prev !== now;
          if (s._edit !== dirty) s._edit = dirty;
        }
      }
    },
    { deep: true },
  );

  onMounted(async () => {
    await fetchPmUsers();
    onSearch();
  });
</script>

<style scoped lang="less">
  .inbound-page {
    --top-grid-card-h: clamp(300px, 40vh, 380px);
    --bottom-grid-card-h: clamp(300px, 40vh, 380px);

    padding: 24px;
    height: 100%;
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: 16px;
    background-color: fade(#5b61f6, 10%);
  }

  .inbound-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .inbound-title {
    font-size: 1.25rem;
    font-weight: 700;
  }
  .inbound-header-actions {
    display: flex;
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
    cursor: pointer;
  }
  .btn:active {
    transform: scale(0.98);
  }
  .btn-ghost {
    background-color: #fff;
    border: 1px solid #e5e7eb;
  }
  .btn-ghost:hover {
    background-color: #f9fafb;
  }
  .btn-primary {
    background-color: #5b61f6;
    color: #fff;
  }
  .btn-primary:hover {
    opacity: 0.9;
  }
  .btn:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  .card {
    border-radius: 16px;
    background-color: #fff;
    border: 1px solid #e5e7eb;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
    padding: 16px;
  }
  .card-search {
    background-image: linear-gradient(135deg, #ffffff, rgba(148, 163, 184, 0.1));
  }
  .card-grid {
    overflow: hidden;
    padding: 12px 12px 8px 12px;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .section-title {
    font-size: 0.875rem;
    font-weight: 600;
    color: #334155;
  }

  .search-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
  }
  @media (min-width: 768px) {
    .search-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
  @media (min-width: 1024px) {
    .search-grid-3 {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  .search-row {
    display: flex;
    align-items: center;
    column-gap: 8px;
  }
  .search-label {
    width: 5rem;
    font-size: 0.875rem;
    color: #475569;
    flex: 0 0 auto;
  }

  .form-input {
    flex: 1;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #fff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
  }

  .form-select {
    flex: 1;
    min-width: 0;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #fff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
    height: 34px;
  }

  .split-wrap {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .split-half {
    flex: 0 0 auto;
    min-height: 0;
  }
  .split-wrap > .split-half:first-child {
    height: var(--top-grid-card-h);
  }
  .split-wrap > .split-half:last-child {
    height: var(--bottom-grid-card-h);
  }

  .grid-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 2px 10px 2px;
  }
  .grid-body {
    flex: 1;
    min-height: 0;
  }

  .grid-icon-btn {
    width: 22px;
    height: 22px;
    border-radius: 6px;
    border: 1px solid rgba(226, 232, 240, 0.9);
    background: #fff;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    cursor: pointer;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }
  .grid-icon-btn:hover {
    background: #f8fafc;
  }
  .grid-icon-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .grid-icon-btn--add {
    color: #5b61f6;
    font-weight: 900;
  }
  .grid-icon-btn--del {
    color: #ef4444;
    font-weight: 900;
  }

  .grid-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding-top: 10px;
    border-top: 1px solid #eef2f7;
  }
  .footer-note {
    font-size: 12px;
    color: #64748b;
  }
  .footer-actions {
    display: flex;
    gap: 8px;
  }
</style>
