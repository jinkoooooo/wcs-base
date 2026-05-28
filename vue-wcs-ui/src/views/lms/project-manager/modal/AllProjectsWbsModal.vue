<template>
  <div class="ovl" @click.self="emit('close')">
    <div class="md" :style="uiVars">
      <!-- header -->
      <div class="hd">
        <div class="hd-txt">
          <b>전체 프로젝트 WBS</b>
          <span v-if="rangeText" class="sub">({{ rangeText }})</span>
        </div>

        <!-- ✅ Zoom + Fit + Close -->
        <div class="hd-right">
          <div class="zoom">
            <button class="zb" @click="zoomOut" :disabled="zoom <= ZOOM_MIN" aria-label="줌 아웃"
              >−</button
            >

            <input
              class="zs"
              type="range"
              :min="ZOOM_MIN"
              :max="ZOOM_MAX"
              step="0.05"
              v-model.number="zoom"
              aria-label="줌 슬라이더"
            />

            <button class="zb" @click="zoomIn" :disabled="zoom >= ZOOM_MAX" aria-label="줌 인"
              >+</button
            >

            <button class="fit" @click="fitToView" aria-label="화면에 맞추기">Fit</button>
            <span class="zp">{{ Math.round(scale * 100) }}%</span>
          </div>

          <button class="x" @click="emit('close')" aria-label="닫기">✕</button>
        </div>
      </div>

      <!-- legend -->
      <div class="lg">
        <div class="lg-left">
          <span v-for="s in legend" :key="String(s.step_cd)" class="li">
            <i class="dot" :style="{ background: stepColor(String(s.step_cd)) }"></i>
            {{ s.step_desc }}
          </span>
        </div>
        <div class="lg-right">* 막대에 마우스를 올리면 상세 툴팁이 표시됩니다.</div>
      </div>

      <!-- ✅ (추가 #3) Filter / Sort Bar -->
      <div class="ctl">
        <div class="ctl-left">
          <span class="ctllb">상태</span>
          <select class="sel" v-model="filterStatus" aria-label="상태 필터">
            <option value="ALL">전체</option>
            <option value="IN_PROGRESS">진행중</option>
            <option value="DUE_SOON">종료임박</option>
            <option value="OVERDUE">지연</option>
            <option value="NOT_STARTED">미시작</option>
          </select>

          <span class="ctllb">정렬</span>
          <select class="sel" v-model="sortMode" aria-label="정렬 방식">
            <option value="CONTRACT_START">계약 시작일</option>
            <option value="PROJECT_START">프로젝트 시작일</option>
            <option value="END_ASC">종료 임박순</option>
            <option value="OVERDUE_FIRST">지연 우선</option>
            <option value="PROGRESS_DESC">진행률 높은 순</option>
            <option value="NAME_ASC">프로젝트명</option>
          </select>
        </div>

        <div class="ctl-right">
          <span class="cnt">표시 {{ displayedRows.length }}건</span>
        </div>
      </div>

      <!-- body -->
      <div class="bd">
        <div v-if="loading" class="loading">불러오는 중...</div>

        <div v-else ref="wrapRef" class="wrap" @wheel="onWheel">
          <!-- ✅ TODAY 세로선 (범위 안일 때만 표시) -->
          <div
            v-if="todayLeft !== null"
            class="today-line"
            :style="{ left: todayLeft + 'px' }"
            aria-hidden="true"
          ></div>

          <!-- sticky header -->
          <div class="th">
            <div class="th-left">프로젝트</div>

            <div class="th-right">
              <div class="yrow">
                <div
                  v-for="g in yearGroups"
                  :key="g.year"
                  class="ycell"
                  :style="{ width: g.len * monthW + 'px' }"
                >
                  {{ g.year }}년
                </div>
              </div>

              <div class="mrow">
                <div
                  v-for="m in months"
                  :key="m.key"
                  class="mcell"
                  :style="{ width: monthW + 'px' }"
                >
                  {{ m.month }}
                </div>
              </div>
            </div>
          </div>

          <!-- rows -->
          <div v-if="displayedRows.length === 0" class="empty">표시할 WBS 데이터가 없습니다.</div>

          <div v-for="p in displayedRows" :key="p.id" class="pg" :class="p.rowClass">
            <!-- ✅ project cell: 요약 + 클릭으로 펼침 -->
            <div
              class="pc"
              :class="{
                compact: !isExpanded(p.id),
                risk: p.status === 'OVERDUE',
                warn: p.status === 'DUE_SOON',
              }"
              :style="{ height: rowHeight(p) + 'px' }"
              @click="toggleExpand(p.id)"
            >
              <!-- ✅ (추가 #1) 프로젝트 요약: 현재단계/진행률/D-day -->
              <div class="pn-row">
                <div class="pn" :title="p.name">{{ p.name }}</div>

                <!-- compact에서도 한 줄로 보이게 우측에 배치 -->
                <div class="sum" :class="{ compact: !isExpanded(p.id) }">
                  <span class="chip st" :class="p.statusChipClass">{{ p.statusLabel }}</span>
                  <span class="chip pr">{{ p.progressPct }}%</span>
                  <span class="chip dd" :class="p.ddayClass">{{ p.ddayText }}</span>
                </div>
              </div>

              <!-- 펼쳐졌을 때만 상세 -->
              <template v-if="isExpanded(p.id)">
                <div class="pp">계약기간: {{ p.contractRange }}</div>
                <div class="pp">전체기간: {{ p.stepRange }}</div>
              </template>
            </div>

            <!-- steps -->
            <div class="st">
              <!-- collapsed: 1줄 요약(모든 step bar를 한 줄에 그림) -->
              <div
                v-if="!isExpanded(p.id)"
                class="sr"
                :style="{ height: UI.ROW_H_COMPACT + 'px' }"
                @click="toggleExpand(p.id)"
              >
                <div
                  class="tc"
                  :style="{
                    width: timelineW + 'px',
                    height: UI.ROW_H_COMPACT + 'px',
                    backgroundSize: monthW + 'px 100%',
                  }"
                >
                  <div class="year-lines" aria-hidden="true">
                    <span
                      v-for="x in yearLineLefts"
                      :key="x"
                      class="year-line"
                      :style="{ left: x + 'px' }"
                    ></span>
                  </div>

                  <div
                    v-for="s in p.steps"
                    :key="s.key"
                    class="bar"
                    :class="[
                      s.phaseClass,
                      s.isWarn ? 'bar--warn' : '',
                      s.isRisk ? 'bar--risk' : '',
                    ]"
                    :style="{
                      left: s.leftBase * scale + 'px',
                      width: s.widthBase * scale + 'px',
                      top: barTop(UI.ROW_H_COMPACT) + 'px',
                      height: UI.BAR_H + 'px',
                      background: s.color,
                    }"
                    @click.stop
                    @mouseenter="showTip($event, p.name, s)"
                    @mousemove="moveTip($event)"
                    @mouseleave="hideTip"
                  ></div>
                </div>
              </div>

              <!-- expanded: step을 행으로 펼침 -->
              <template v-else>
                <div
                  v-for="s in p.steps"
                  :key="s.key"
                  class="sr"
                  :style="{ height: UI.ROW_H + 'px' }"
                >
                  <div
                    class="tc"
                    :style="{
                      width: timelineW + 'px',
                      height: UI.ROW_H + 'px',
                      backgroundSize: monthW + 'px 100%',
                    }"
                  >
                    <div class="year-lines" aria-hidden="true">
                      <span
                        v-for="x in yearLineLefts"
                        :key="x"
                        class="year-line"
                        :style="{ left: x + 'px' }"
                      ></span>
                    </div>

                    <div
                      class="bar"
                      :class="[
                        s.phaseClass,
                        s.isWarn ? 'bar--warn' : '',
                        s.isRisk ? 'bar--risk' : '',
                      ]"
                      :style="{
                        left: s.leftBase * scale + 'px',
                        width: s.widthBase * scale + 'px',
                        top: barTop(UI.ROW_H) + 'px',
                        height: UI.BAR_H + 'px',
                        background: s.color,
                      }"
                      @click.stop
                      @mouseenter="showTip($event, p.name, s)"
                      @mousemove="moveTip($event)"
                      @mouseleave="hideTip"
                    ></div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- tooltip -->
        <div v-if="tip.open" class="tip" :style="{ left: tip.x + 'px', top: tip.y + 'px' }">
          <div class="t1">{{ tip.title }}</div>
          <div class="t2">{{ tip.line1 }}</div>
          <div class="t2">{{ tip.line2 }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { defHttp } from '/@/utils/http/axios';

  type Main = {
    id?: string;
    project_name?: string | null;
    contract_dt?: string | null;
    project_end_dt?: string | null;
  };

  type Step = {
    step_cd?: number | string | null;
    step_desc?: string | null;
    start_date?: string | null;
    end_date?: string | null;
    duration_days?: number | string | null;
  };

  const props = defineProps<{ projects: Main[]; baseStepUrl: string }>();
  const emit = defineEmits<{ (e: 'close'): void }>();

  const UI = {
    PROJECT_W: 500,
    BASE_MONTH_W: 32, // ✅ 줌 기준 월 폭
    ROW_H: 18,
    ROW_H_COMPACT: 24,
    BAR_H: 6,
  } as const;

  function barTop(rowH: number) {
    return Math.floor((rowH - UI.BAR_H) / 2);
  }

  const uiVars = computed(() => ({
    '--project-w': `${UI.PROJECT_W}px`,
    '--row-h': `${UI.ROW_H}px`,
    '--row-compact-h': `${UI.ROW_H_COMPACT}px`,
  }));

  const loading = ref(false);

  /* =========================
   * ✅ Zoom / Fit
   * ========================= */
  const ZOOM_MIN = 0.6 as const;
  const ZOOM_MAX = 2.2 as const;
  const zoom = ref(1);

  const monthW = computed(() => Math.round(UI.BASE_MONTH_W * zoom.value));
  const scale = computed(() => monthW.value / UI.BASE_MONTH_W);

  function clamp(n: number, min: number, max: number) {
    return Math.max(min, Math.min(max, n));
  }

  function zoomIn() {
    zoom.value = clamp(zoom.value + 0.1, ZOOM_MIN, ZOOM_MAX);
  }
  function zoomOut() {
    zoom.value = clamp(zoom.value - 0.1, ZOOM_MIN, ZOOM_MAX);
  }

  const wrapRef = ref<HTMLElement | null>(null);

  function fitToView() {
    const el = wrapRef.value;
    if (!el) return;
    if (!months.value.length) return;

    const avail = el.clientWidth - UI.PROJECT_W - 16;
    if (avail <= 200) return;

    const targetMonthW = avail / months.value.length;
    const z = targetMonthW / UI.BASE_MONTH_W;

    zoom.value = clamp(z, ZOOM_MIN, ZOOM_MAX);
    el.scrollLeft = 0;
  }

  /* =========================
   * ✅ Ctrl/Shift + MouseWheel => Horizontal Scroll
   * ========================= */
  function onWheel(e: WheelEvent) {
    const useHorizontal = e.ctrlKey || e.shiftKey;
    if (!useHorizontal) return;

    const el = wrapRef.value;
    if (!el) return;

    e.preventDefault();
    const delta = Math.abs(e.deltaX) > Math.abs(e.deltaY) ? e.deltaX : e.deltaY;
    el.scrollLeft += delta;
  }

  /* =========================
   * Tooltip
   * ========================= */
  type StepUi = {
    key: string;
    stepCd: string;
    stepDesc: string;
    s: string;
    e: string;
    dur: string;
    leftBase: number;
    widthBase: number;
    color: string;

    // ✅ (추가 #2) 단계 상태 클래스
    phaseClass: 'bar--done' | 'bar--active' | 'bar--todo';
    isLast: boolean;
    isWarn: boolean;
    isRisk: boolean;
  };

  const tip = ref({ open: false, x: 0, y: 0, title: '', line1: '', line2: '' });

  const showTip = (e: MouseEvent, projectName: string, s: StepUi) => {
    tip.value.open = true;
    tip.value.title = projectName;
    tip.value.line1 = `${s.stepDesc} (${s.stepCd})`;
    tip.value.line2 = `${s.s} ~ ${s.e} / ${s.dur}`;
    moveTip(e);
  };

  const moveTip = (e: MouseEvent) => {
    tip.value.x = e.clientX + 12;
    tip.value.y = e.clientY + 12;
  };

  const hideTip = () => (tip.value.open = false);

  /* =========================
   * Expand/Collapse
   * ========================= */
  const expanded = ref<Record<string, boolean>>({});

  function isExpanded(id: string) {
    return !!expanded.value[id];
  }

  function toggleExpand(id: string) {
    expanded.value[id] = !expanded.value[id];
  }

  type ProjectStatus = 'OVERDUE' | 'DUE_SOON' | 'IN_PROGRESS' | 'NOT_STARTED' | 'ON_TRACK';

  type RowUi = {
    id: string;
    name: string;
    contractRange: string;
    stepRange: string;

    // ✅ sort keys
    contractStart: string; // yyyy-mm-dd or 9999...
    projectStart: string;
    projectEnd: string;

    // ✅ (추가 #1) 요약
    currentStepCd: string;
    currentStepDesc: string;
    progressPct: number;
    ddayText: string;
    ddayValue: number; // signed (end - today)
    ddayClass: string;
    status: ProjectStatus;
    statusLabel: string;
    statusChipClass: string;
    rowClass: string;

    steps: StepUi[];
  };

  function rowHeight(p: RowUi) {
    return isExpanded(p.id) ? p.steps.length * UI.ROW_H : UI.ROW_H_COMPACT;
  }

  /* =========================
   * Date helpers
   * ========================= */
  const ymd = (v: any): string | null => {
    const s = String(v ?? '').trim();
    const m = s.match(/^(\d{4}-\d{2}-\d{2})/);
    return m ? m[1] : null;
  };

  const pad2 = (n: number) => String(n).padStart(2, '0');

  const parseYmd = (s: string): Date => {
    const [y, m, d] = s.split('-').map(Number);
    return new Date(y, m - 1, d);
  };

  const daysInMonth = (d: Date) => new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();

  const monthsDiff = (a: Date, base: Date) =>
    (a.getFullYear() - base.getFullYear()) * 12 + (a.getMonth() - base.getMonth());

  const diffDaysInc = (s: string, e: string) => {
    const ms = parseYmd(e).getTime() - parseYmd(s).getTime();
    return Math.max(1, Math.floor(ms / 86400000) + 1);
  };

  const diffDaysSigned = (fromYmd: string, toYmd: string) => {
    const ms = parseYmd(toYmd).getTime() - parseYmd(fromYmd).getTime();
    return Math.floor(ms / 86400000);
  };

  const fmtRange = (s: string | null, e: string | null) => `${s ?? '-'} ~ ${e ?? '-'}`;

  const todayYmd = () => {
    const now = new Date();
    return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`;
  };

  const isBetween = (t: string, s: string, e: string) => s <= t && t <= e;

  /* =========================
   * Axis
   * ========================= */
  const months = ref<{ key: string; year: number; month: number }[]>([]);
  const yearGroups = ref<{ year: number; len: number }[]>([]);
  const gStart = ref<string | null>(null);
  const gEnd = ref<string | null>(null);

  const rangeText = computed(() =>
    gStart.value && gEnd.value ? `${gStart.value} ~ ${gEnd.value}` : '',
  );

  const timelineW = computed(() => months.value.length * monthW.value);

  const yearLineLefts = computed<number[]>(() => {
    const xs: number[] = [];
    for (let i = 0; i < months.value.length; i++) {
      const m = months.value[i];
      if (m.month === 1) xs.push(i * monthW.value);
    }
    return xs;
  });

  function buildAxis(startYmd: string, endYmd: string) {
    const s = parseYmd(startYmd);
    const e = parseYmd(endYmd);
    const cur = new Date(s.getFullYear(), s.getMonth(), 1);
    const last = new Date(e.getFullYear(), e.getMonth(), 1);

    const list: { key: string; year: number; month: number }[] = [];
    while (cur.getTime() <= last.getTime()) {
      const y = cur.getFullYear();
      const m = cur.getMonth() + 1;
      list.push({ key: `${y}-${pad2(m)}`, year: y, month: m });
      cur.setMonth(cur.getMonth() + 1);
    }
    months.value = list;

    const groups: { year: number; len: number }[] = [];
    for (const x of list) {
      const g = groups[groups.length - 1];
      if (!g || g.year !== x.year) groups.push({ year: x.year, len: 1 });
      else g.len += 1;
    }
    yearGroups.value = groups;
  }

  function calcPosBase(sYmd: string, eYmd: string, globalStart: string) {
    const baseStart = parseYmd(globalStart);
    const base = new Date(baseStart.getFullYear(), baseStart.getMonth(), 1);

    const s = parseYmd(sYmd);
    const e = parseYmd(eYmd);

    const sIdx = monthsDiff(new Date(s.getFullYear(), s.getMonth(), 1), base);
    const eIdx = monthsDiff(new Date(e.getFullYear(), e.getMonth(), 1), base);

    const left = sIdx * UI.BASE_MONTH_W + ((s.getDate() - 1) / daysInMonth(s)) * UI.BASE_MONTH_W;
    const right = eIdx * UI.BASE_MONTH_W + (e.getDate() / daysInMonth(e)) * UI.BASE_MONTH_W;

    return { left, width: Math.max(2, right - left) };
  }

  /* =========================
   * Today line
   * ========================= */
  const todayXBase = computed<number | null>(() => {
    if (!gStart.value || !gEnd.value) return null;

    const t = todayYmd();
    if (t < gStart.value || t > gEnd.value) return null;

    const baseStart = parseYmd(gStart.value);
    const base = new Date(baseStart.getFullYear(), baseStart.getMonth(), 1);

    const td = parseYmd(t);
    const idx = monthsDiff(new Date(td.getFullYear(), td.getMonth(), 1), base);

    return idx * UI.BASE_MONTH_W + ((td.getDate() - 1) / daysInMonth(td)) * UI.BASE_MONTH_W;
  });

  const todayLeft = computed<number | null>(() => {
    if (todayXBase.value == null) return null;
    return UI.PROJECT_W + todayXBase.value * scale.value;
  });

  /* =========================
   * Colors
   * ========================= */
  const COLORS = [
    '#6366F1',
    '#10B981',
    '#F59E0B',
    '#EF4444',
    '#06B6D4',
    '#8B5CF6',
    '#F97316',
    '#22C55E',
    '#3B82F6',
    '#E11D48',
    '#14B8A6',
    '#A855F7',
  ];
  const colorMap = ref<Record<string, string>>({});

  function setColor(stepCd: string) {
    if (colorMap.value[stepCd]) return;
    const idx = Object.keys(colorMap.value).length % COLORS.length;
    colorMap.value[stepCd] = COLORS[idx];
  }

  function stepColor(stepCd: string) {
    return colorMap.value[stepCd] ?? COLORS[0];
  }

  /* =========================
   * Models
   * ========================= */
  type Legend = { step_cd: string; step_desc: string };
  const legend = ref<Legend[]>([]);
  const rows = ref<RowUi[]>([]);

  /* =========================
   * ✅ (추가 #3) Filter / Sort
   * ========================= */
  const filterStatus = ref<'ALL' | ProjectStatus>('ALL');
  const sortMode = ref<
    'CONTRACT_START' | 'PROJECT_START' | 'END_ASC' | 'OVERDUE_FIRST' | 'PROGRESS_DESC' | 'NAME_ASC'
  >('CONTRACT_START');

  const displayedRows = computed<RowUi[]>(() => {
    const f = filterStatus.value;
    let arr = rows.value.slice();

    if (f !== 'ALL') arr = arr.filter((r) => r.status === f);

    const byStr = (a: string, b: string) => String(a).localeCompare(String(b));
    const byNum = (a: number, b: number) => (a === b ? 0 : a < b ? -1 : 1);

    arr.sort((a, b) => {
      switch (sortMode.value) {
        case 'CONTRACT_START':
          return (
            byStr(a.contractStart, b.contractStart) ||
            byStr(a.projectStart, b.projectStart) ||
            byStr(a.name, b.name)
          );
        case 'PROJECT_START':
          return (
            byStr(a.projectStart, b.projectStart) ||
            byStr(a.contractStart, b.contractStart) ||
            byStr(a.name, b.name)
          );
        case 'END_ASC':
          return byStr(a.projectEnd, b.projectEnd) || byStr(a.projectStart, b.projectStart);
        case 'OVERDUE_FIRST': {
          const aO = a.status === 'OVERDUE' ? 1 : 0;
          const bO = b.status === 'OVERDUE' ? 1 : 0;
          if (aO !== bO) return bO - aO; // overdue 먼저
          // 종료 임박(남은 일수 작은 순)
          return byNum(a.ddayValue, b.ddayValue) || byStr(a.projectEnd, b.projectEnd);
        }
        case 'PROGRESS_DESC':
          return byNum(b.progressPct, a.progressPct) || byStr(a.projectEnd, b.projectEnd);
        case 'NAME_ASC':
          return byStr(a.name, b.name);
        default:
          return 0;
      }
    });

    return arr;
  });

  /* =========================
   * Fetch / Load
   * ========================= */
  async function fetchSteps(mainId: string) {
    const api = await defHttp.post({ url: `${props.baseStepUrl}/project_detail_step_info`, data: { main_id: mainId } });
    return Array.isArray(api) ? (api as Step[]) : [];
  }

  async function mapLimit<T, R>(arr: T[], limit: number, fn: (x: T) => Promise<R>) {
    const out = new Array<R>(arr.length);
    let idx = 0;

    const workers = Array(Math.min(limit, arr.length))
      .fill(0)
      .map(async () => {
        while (idx < arr.length) {
          const cur = idx++;
          out[cur] = await fn(arr[cur]);
        }
      });

    await Promise.all(workers);
    return out;
  }

  const loadSeq = ref(0);

  function statusMeta(status: ProjectStatus) {
    switch (status) {
      case 'OVERDUE':
        return { label: '지연', chip: 'chip--risk', row: 'row--risk' };
      case 'DUE_SOON':
        return { label: '임박', chip: 'chip--warn', row: 'row--warn' };
      case 'IN_PROGRESS':
        return { label: '진행', chip: 'chip--ok', row: '' };
      case 'NOT_STARTED':
        return { label: '미시작', chip: 'chip--muted', row: '' };
      case 'ON_TRACK':
      default:
        return { label: '정상', chip: 'chip--muted', row: '' };
    }
  }

  function ddayMeta(dday: number) {
    if (dday === 0) return { text: 'D-day', cls: 'dday--zero' };
    if (dday > 0) return { text: `D-${dday}`, cls: dday <= 14 ? 'dday--soon' : 'dday--pos' };
    return { text: `D+${Math.abs(dday)}`, cls: 'dday--neg' };
  }

  async function load() {
    const seq = ++loadSeq.value;
    loading.value = true;

    try {
      hideTip();

      const mains = (props.projects ?? [])
        .map((p) => ({
          id: String(p.id ?? '').trim(),
          name: String(p.project_name ?? '').trim() || '(No Name)',
          mainS: ymd(p.contract_dt),
          mainE: ymd(p.project_end_dt),
        }))
        .filter((p) => p.id);

      const packed = await mapLimit(mains, 6, async (p) => {
        const raw = await fetchSteps(p.id);

        const steps = raw
          .map((s) => ({
            cd: String(s.step_cd ?? '').trim(),
            desc: String(s.step_desc ?? '').trim(),
            s: ymd(s.start_date),
            e: ymd(s.end_date),
            dur: s.duration_days,
          }))
          .filter((x) => x.cd && x.desc && x.s && x.e) as Array<{
          cd: string;
          desc: string;
          s: string;
          e: string;
          dur: any;
        }>;

        // ✅ Step 정렬: 시작일자 ASC
        steps.sort((a, b) => {
          const c1 = String(a.s).localeCompare(String(b.s));
          if (c1 !== 0) return c1;
          const c2 = String(a.e).localeCompare(String(b.e));
          if (c2 !== 0) return c2;

          const na = Number.isFinite(Number(a.cd)) ? Number(a.cd) : 999999;
          const nb = Number.isFinite(Number(b.cd)) ? Number(b.cd) : 999999;
          if (na !== nb) return na - nb;

          return String(a.desc ?? '').localeCompare(String(b.desc ?? ''), 'ko');
        });

        // ✅ 프로젝트 기본 정렬 키(계약 시작일 우선, 없으면 step 시작일)
        const baseSort =
          (p.mainS && String(p.mainS)) || (steps.length ? steps[0].s : null) || '9999-12-31';

        return { p, steps, baseSort };
      });

      if (seq !== loadSeq.value) return;

      const allS: string[] = [];
      const allE: string[] = [];
      for (const x of packed) for (const s of x.steps) allS.push(s.s), allE.push(s.e);

      if (!allS.length || !allE.length) {
        rows.value = [];
        legend.value = [];
        months.value = [];
        yearGroups.value = [];
        gStart.value = null;
        gEnd.value = null;
        colorMap.value = {};
        return;
      }

      const gs = allS.sort()[0];
      const ge = allE.slice().sort().reverse()[0];
      gStart.value = gs;
      gEnd.value = ge;

      buildAxis(gs, ge);

      // legend
      const m = new Map<string, string>();
      for (const x of packed) for (const s of x.steps) if (!m.has(s.cd)) m.set(s.cd, s.desc);

      const lg = Array.from(m.entries())
        .map(([step_cd, step_desc]) => ({ step_cd, step_desc }))
        .sort((a, b) => Number(a.step_cd) - Number(b.step_cd));

      colorMap.value = {};
      for (const s of lg) setColor(String(s.step_cd));
      legend.value = lg;

      const t = todayYmd();

      rows.value = packed
        .filter((x) => x.steps.length)
        .map((x) => {
          const stepS = x.steps.map((v) => v.s).sort()[0];
          const stepE = x.steps
            .map((v) => v.e)
            .sort()
            .reverse()[0];

          // ✅ 프로젝트 상태 판정
          const projectStart = stepS ?? '9999-12-31';
          const projectEnd = stepE ?? '0000-01-01';
          const dday = diffDaysSigned(t, projectEnd);

          const hasActive = x.steps.some((s) => isBetween(t, s.s, s.e));
          const status: ProjectStatus =
            t > projectEnd
              ? 'OVERDUE'
              : t < projectStart
              ? 'NOT_STARTED'
              : dday <= 14
              ? 'DUE_SOON'
              : hasActive
              ? 'IN_PROGRESS'
              : 'ON_TRACK';

          const meta = statusMeta(status);

          // ✅ 현재 단계 (오늘 포함 단계 > 다음 예정 > 마지막)
          const active = x.steps.find((s) => isBetween(t, s.s, s.e));
          const next = x.steps.find((s) => t < s.s);
          const last = x.steps[x.steps.length - 1];
          const cur = active ?? next ?? last;

          // ✅ 진행률(기간 기준: step 기간 합 대비 오늘까지 진행된 일수)
          const totalDays = x.steps.reduce((acc, s) => acc + diffDaysInc(s.s, s.e), 0);
          const doneDays = x.steps.reduce((acc, s) => {
            if (t < s.s) return acc;
            if (t > s.e) return acc + diffDaysInc(s.s, s.e);
            return acc + diffDaysInc(s.s, t);
          }, 0);

          const pct =
            totalDays > 0
              ? Math.max(0, Math.min(100, Math.round((doneDays / totalDays) * 100)))
              : 0;

          const dm = ddayMeta(dday);

          // ✅ step bars + 상태 레이어
          const lastIdx = x.steps.length - 1;
          const stepsUi: StepUi[] = x.steps.map((s, i) => {
            const { left, width } = calcPosBase(s.s, s.e, gs);
            const dur =
              s.dur != null && String(s.dur).trim() !== ''
                ? `${s.dur}일`
                : `${diffDaysInc(s.s, s.e)}일`;

            const phaseClass: StepUi['phaseClass'] =
              t > s.e ? 'bar--done' : isBetween(t, s.s, s.e) ? 'bar--active' : 'bar--todo';

            // ✅ (추가 #2) 지연/임박 레이어는 “마지막 단계”에만 강조(과도한 빨간색 방지)
            const isLast = i === lastIdx;
            const isRisk = status === 'OVERDUE' && isLast;
            const isWarn = status === 'DUE_SOON' && isLast;

            return {
              key: `${x.p.id}-${s.cd}-${s.s}-${s.e}`,
              stepCd: s.cd,
              stepDesc: s.desc,
              s: s.s,
              e: s.e,
              dur,
              leftBase: left,
              widthBase: width,
              color: stepColor(s.cd),
              phaseClass,
              isLast,
              isWarn,
              isRisk,
            };
          });

          const contractStart = (x.p.mainS && String(x.p.mainS)) || '9999-12-31';

          return {
            id: x.p.id,
            name: x.p.name,

            contractRange: fmtRange(x.p.mainS, x.p.mainE),
            stepRange: fmtRange(stepS, stepE),

            contractStart,
            projectStart,
            projectEnd,

            currentStepCd: cur?.cd ?? '-',
            currentStepDesc: cur?.desc ?? '-',
            progressPct: pct,

            ddayValue: dday,
            ddayText: dm.text,
            ddayClass: dm.cls,

            status,
            statusLabel: meta.label,
            statusChipClass: meta.chip,
            rowClass: meta.row,

            steps: stepsUi,
          };
        });
    } finally {
      loading.value = false;
    }
  }

  /* =========================
   * ESC close
   * ========================= */
  const onKey = (e: KeyboardEvent) => e.key === 'Escape' && emit('close');

  onMounted(() => {
    window.addEventListener('keydown', onKey);
    load();
  });

  onBeforeUnmount(() => window.removeEventListener('keydown', onKey));

  watch(
    () => props.projects,
    () => load(),
    { deep: true },
  );
</script>

<style scoped>
  .ovl {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
  }

  .md {
    width: min(1840px, calc(100vw - 24px));
    height: clamp(560px, 88vh, 940px);
    max-height: calc(100vh - 24px);
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 18px 60px rgba(15, 23, 42, 0.25);
    display: flex;
    flex-direction: column;
    overflow: hidden;

    --project-w: 500px;
    --row-h: 18px;
    --row-compact-h: 24px;
  }

  @media (max-width: 768px) {
    .md {
      width: calc(100vw - 16px);
      height: calc(100vh - 16px);
      max-height: calc(100vh - 16px);
      border-radius: 12px;
    }
  }

  .hd {
    height: 44px;
    padding: 0 12px 0 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #eef2f7;
    gap: 12px;
  }

  .hd-txt {
    display: flex;
    align-items: baseline;
    gap: 8px;
    min-width: 0;
  }

  .sub {
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
  }

  .hd-right {
    display: flex;
    align-items: center;
    gap: 10px;
    flex: none;
  }

  .zoom {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .zs {
    width: 160px;
  }

  .zb {
    width: 28px;
    height: 28px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
    background: #fff;
    cursor: pointer;
  }
  .zb:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .fit {
    height: 28px;
    padding: 0 10px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
    background: #fff;
    cursor: pointer;
  }
  .fit:hover {
    background: #f8fafc;
  }

  .zp {
    font-size: 12px;
    color: #64748b;
    width: 52px;
    text-align: right;
  }

  .x {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
    background: #fff;
    cursor: pointer;
  }
  .x:hover {
    background: #f8fafc;
  }

  .lg {
    padding: 6px 14px;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    border-bottom: 1px solid #eef2f7;
  }
  .lg-left {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 12px;
    min-width: 0;
  }
  .li {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #334155;
  }
  .dot {
    width: 10px;
    height: 10px;
    border-radius: 999px;
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
  }
  .lg-right {
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
  }

  /* ✅ Filter/Sort bar */
  .ctl {
    padding: 8px 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    border-bottom: 1px solid #eef2f7;
    background: #fff;
  }
  .ctl-left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    flex-wrap: wrap;
  }
  .ctllb {
    font-size: 12px;
    color: #64748b;
  }
  .sel {
    height: 28px;
    padding: 0 10px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
    background: #fff;
    font-size: 12px;
    color: #0f172a;
    cursor: pointer;
  }
  .ctl-right .cnt {
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
  }

  .bd {
    flex: 1;
    min-height: 0;
    position: relative;
  }
  .loading {
    padding: 14px;
    font-size: 13px;
    color: #475569;
  }

  .wrap {
    height: 100%;
    overflow: auto;
    background: #fff;
    position: relative;
  }

  .today-line {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 2px;
    background: rgba(239, 68, 68, 0.72);
    box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.14);
    pointer-events: none;
    z-index: 8;
  }

  .th {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    border-bottom: 1px solid #eef2f7;
    background: #fff;
    box-shadow: 0 1px 0 rgba(15, 23, 42, 0.04);
  }

  .th-left {
    position: sticky;
    left: 0;
    z-index: 21;
    width: var(--project-w);
    min-width: var(--project-w);
    padding: 6px 10px;
    font-size: 12px;
    font-weight: 700;
    color: #334155;
    border-right: 1px solid #eef2f7;
    background: #fff;
  }

  .th-right {
    flex: none;
    display: flex;
    flex-direction: column;
    background: #fff;
  }

  .yrow,
  .mrow {
    display: flex;
  }

  .ycell {
    height: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    color: #0f172a;
    border-right: 1px solid #eef2f7;
    background: #f8fafc;
  }

  .mcell {
    height: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    color: #475569;
    border-right: 1px solid #eef2f7;
    background: #fff;
  }

  .empty {
    padding: 14px;
    font-size: 12px;
    color: #64748b;
  }

  .pg {
    display: flex;
    border-bottom: 1px solid rgba(203, 213, 225, 0.9);
    box-shadow: 0 1px 0 rgba(15, 23, 42, 0.03);
  }
  .pg + .pg {
    border-top: 2px solid rgba(15, 23, 42, 0.03);
  }

  /* ✅ (추가 #2) 프로젝트 row 상태 배경 */
  .row--warn {
    background: rgba(245, 158, 11, 0.04);
  }
  .row--risk {
    background: rgba(239, 68, 68, 0.04);
  }

  .pc {
    position: sticky;
    left: 0;
    z-index: 12;
    width: var(--project-w);
    min-width: var(--project-w);
    padding: 6px 10px;
    border-right: 1px solid #eef2f7;
    background: #fff;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    overflow: hidden;
    cursor: pointer;
  }

  /* ✅ compact에서는 “진짜 한 줄”로 보이게 */
  .pc.compact {
    padding: 0 10px;
    justify-content: center;
    gap: 0;
  }

  .pn-row {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .pn {
    font-size: 12px;
    font-weight: 700;
    color: #0f172a;

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.2;
    flex: 1;
    min-width: 0;
  }

  /* ✅ (추가 #1) 요약 칩들 */
  .sum {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: none;
    max-width: 160px;
  }
  .sum.compact {
    max-width: 175px;
  }

  .chip {
    height: 18px;
    padding: 0 8px;
    border-radius: 999px;
    font-size: 11px;
    display: inline-flex;
    align-items: center;
    border: 1px solid rgba(203, 213, 225, 0.9);
    background: #fff;
    color: #334155;
    white-space: nowrap;
  }
  .chip.step {
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .chip.pr {
    font-weight: 700;
  }

  .chip--ok {
    border-color: rgba(16, 185, 129, 0.45);
    background: rgba(16, 185, 129, 0.08);
    color: #065f46;
    font-weight: 800;
  }
  .chip--warn {
    border-color: rgba(245, 158, 11, 0.5);
    background: rgba(245, 158, 11, 0.1);
    color: #92400e;
    font-weight: 800;
  }
  .chip--risk {
    border-color: rgba(239, 68, 68, 0.55);
    background: rgba(239, 68, 68, 0.1);
    color: #7f1d1d;
    font-weight: 800;
  }
  .chip--muted {
    border-color: rgba(148, 163, 184, 0.55);
    background: rgba(148, 163, 184, 0.08);
    color: #334155;
    font-weight: 700;
  }

  .dday--pos {
    border-color: rgba(148, 163, 184, 0.6);
    color: #334155;
  }
  .dday--soon {
    border-color: rgba(245, 158, 11, 0.55);
    color: #92400e;
    font-weight: 800;
  }
  .dday--zero {
    border-color: rgba(59, 130, 246, 0.55);
    color: #1d4ed8;
    font-weight: 800;
  }
  .dday--neg {
    border-color: rgba(239, 68, 68, 0.65);
    color: #7f1d1d;
    font-weight: 900;
  }

  .pp {
    font-size: 11px;
    color: #64748b;
    white-space: normal;
    word-break: break-word;
    overflow-wrap: anywhere;
    line-height: 1.25;
  }

  .st {
    display: flex;
    flex-direction: column;
  }

  .sr {
    display: flex;
  }

  .tc {
    position: relative;
    flex: none;
    background-image: linear-gradient(to right, rgba(226, 232, 240, 0.65) 1px, transparent 1px);
    overflow: hidden;
  }

  .year-lines {
    position: absolute;
    inset: 0;
    pointer-events: none;
    z-index: 2;
  }

  .year-line {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 2px;
    background: rgba(148, 163, 184, 0.55);
    box-shadow: 1px 0 0 rgba(15, 23, 42, 0.04);
  }

  .bar {
    position: absolute;
    border-radius: 999px;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.12);
    cursor: pointer;
    z-index: 7;
    overflow: hidden;
  }

  /* ✅ (추가 #2) 단계 상태 표현: 완료/진행/예정 */
  .bar--done {
    opacity: 0.35;
    filter: saturate(0.65);
    box-shadow: none;
  }
  .bar--todo {
    opacity: 0.85;
    filter: saturate(0.9);
  }
  .bar--active {
    opacity: 1;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.35), 0 8px 18px rgba(15, 23, 42, 0.18);
  }

  /* ✅ (추가 #2) 프로젝트 상태 레이어: warn / risk (마지막 step에만 강조) */
  .bar--warn {
    box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.55), 0 8px 18px rgba(15, 23, 42, 0.18);
  }
  .bar--risk {
    box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.6), 0 8px 18px rgba(15, 23, 42, 0.18);
  }
  .bar--risk::after {
    content: '';
    position: absolute;
    inset: 0;
    background-image: repeating-linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.28) 0,
      rgba(255, 255, 255, 0.28) 4px,
      rgba(255, 255, 255, 0) 4px,
      rgba(255, 255, 255, 0) 8px
    );
    pointer-events: none;
  }

  .tip {
    position: fixed;
    z-index: 99999;
    min-width: 220px;
    max-width: 340px;
    padding: 10px 12px;
    border-radius: 12px;
    background: rgba(15, 23, 42, 0.92);
    color: #fff;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.3);
    pointer-events: none;
  }

  .t1 {
    font-size: 12px;
    font-weight: 800;
    margin-bottom: 6px;
  }

  .t2 {
    font-size: 12px;
    line-height: 1.35;
    color: rgba(255, 255, 255, 0.92);
  }
</style>
