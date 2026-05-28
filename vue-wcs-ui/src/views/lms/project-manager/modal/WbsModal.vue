<template>
  <teleport to="body">
    <div v-if="modelValue" class="wbs-overlay" @click.self="close">
      <div class="wbs-modal" role="dialog" aria-modal="true">
        <div class="wbs-header">
          <div class="wbs-title">
            프로젝트 수행 WBS
            <span class="wbs-sub" v-if="main?.project_name">- {{ main.project_name }}</span>
          </div>

          <button class="wbs-close" @click="close" aria-label="닫기">×</button>
        </div>

        <div class="wbs-body">
          <!-- ✅ 상단 요약: 좌(메인 수행기간) / 우(스텝 전체기간) -->
          <div class="wbs-summary">
            <div class="sum-item">
              <span class="sum-label">수행기간</span>
              <span class="sum-val">{{ mainPeriodLabel }}</span>
            </div>

            <div class="sum-item">
              <span class="sum-label">전체기간</span>
              <span class="sum-val">{{ stepPeriodLabel }}</span>
            </div>

            <div class="sum-item" v-if="main?.pl_name || main?.sub_pl_name">
              <span class="sum-label">PL</span>
              <span class="sum-val"
                >SYS: {{ main?.pl_name ?? '-' }} / SUB: {{ main?.sub_pl_name ?? '-' }}</span
              >
            </div>
          </div>

          <!-- Gantt -->
          <div class="gantt">
            <!-- 좌측 라벨 -->
            <div class="g-left">
              <div class="g-left-header">단계</div>
              <div class="g-left-row" v-for="s in stepMaster" :key="String(s.code)">
                {{ s.desc }}
              </div>
            </div>

            <!-- 우측 타임라인 -->
            <div class="g-right">
              <div class="g-scroll" ref="scrollEl">
                <!-- ✅ Tooltip -->
                <div v-if="tooltip.show" class="tooltip" :style="tooltip.style">
                  {{ tooltip.text }}
                </div>

                <!-- 헤더 -->
                <div class="g-header">
                  <!-- 년 -->
                  <div class="year-row" :style="{ width: timelineWidthPx + 'px' }">
                    <div
                      v-for="g in yearGroups"
                      :key="'y-' + g.year"
                      class="year-cell"
                      :style="{ width: g.count * monthW + 'px' }"
                    >
                      {{ g.year }}년
                    </div>
                  </div>

                  <!-- 월 -->
                  <div class="month-row" :style="{ width: timelineWidthPx + 'px' }">
                    <div
                      v-for="m in months"
                      :key="m.key"
                      class="month-cell"
                      :style="{ width: monthW + 'px' }"
                    >
                      {{ m.m }}
                    </div>
                  </div>
                </div>

                <!-- 바디 -->
                <div class="g-body">
                  <div
                    class="timeline-row"
                    v-for="s in stepMaster"
                    :key="'r-' + String(s.code)"
                    :style="{
                      width: timelineWidthPx + 'px',
                      '--month-w': monthW + 'px',
                      '--row-h': ROW_H + 'px',
                    }"
                  >
                    <div
                      v-if="barByStepCode[String(s.code)]"
                      class="bar"
                      :style="barByStepCode[String(s.code)]"
                      @mouseenter="onBarEnter(String(s.code), $event)"
                      @mousemove="onBarMove($event)"
                      @mouseleave="onBarLeave"
                    ></div>
                  </div>
                </div>
              </div>

              <div class="hint">
                * 월 단위 표시(시작~종료 월 기준). 기간이 길면 가로 스크롤이 자동 활성화됩니다.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

  type StepMaster = { code: number | string; desc: string };

  const props = defineProps<{
    modelValue: boolean;
    main: any | null;
    steps: any[];
    stepMaster: StepMaster[];
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void;
  }>();

  function close() {
    emit('update:modelValue', false);
  }

  // ESC 닫기
  function onKeydown(e: KeyboardEvent) {
    if (!props.modelValue) return;
    if (e.key === 'Escape') close();
  }

  // ====== Layout/Measure ======
  const scrollEl = ref<HTMLElement | null>(null);
  const viewportW = ref(0);
  let ro: ResizeObserver | null = null;

  function refreshViewport() {
    if (!scrollEl.value) return;
    viewportW.value = scrollEl.value.clientWidth || 0;
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeydown);

    refreshViewport();
    if (scrollEl.value && typeof ResizeObserver !== 'undefined') {
      ro = new ResizeObserver(() => refreshViewport());
      ro.observe(scrollEl.value);
    }
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', onKeydown);
    if (ro && scrollEl.value) ro.unobserve(scrollEl.value);
    ro = null;
  });

  // ====== Timeline Config ======
  const BASE_MONTH_W = 34; // 기본 월폭(px) - 기간이 길 때 스크롤
  const MAX_FILL_W = 60; // 기간이 짧을 때는 월폭을 늘려 빈공간 제거(상한)
  const ROW_H = 38;

  function pad2(n: number) {
    return String(n).padStart(2, '0');
  }

  function toUiYmdAny(v: any): string | null {
    const s = String(v ?? '').trim();
    if (!s) return null;
    const m = s.match(/^(\d{4}-\d{2}-\d{2})/);
    return m ? m[1] : null;
  }

  function parseYmd(s: any): Date | null {
    const v = String(s ?? '').trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
    const [y, m, d] = v.split('-').map((x) => Number(x));
    const dt = new Date(y, m - 1, d);
    return Number.isNaN(dt.getTime()) ? null : dt;
  }

  function monthStart(d: Date) {
    return new Date(d.getFullYear(), d.getMonth(), 1);
  }

  function monthSerial(d: Date) {
    return d.getFullYear() * 12 + d.getMonth();
  }

  function serialToYm(serial: number) {
    const y = Math.floor(serial / 12);
    const m = (serial % 12) + 1;
    return { y, m };
  }

  function fmtYmd(d: Date | null) {
    if (!d) return '';
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  }

  function fmtMmdd(d: Date | null) {
    if (!d) return '';
    return `${pad2(d.getMonth() + 1)}/${pad2(d.getDate())}`;
  }

  function clamp(n: number, min: number, max: number) {
    return Math.max(min, Math.min(max, n));
  }

  // ====== 기간 라벨(좌: main / 우: step) ======
  const mainStartYmd = computed(() => {
    // 우선순위: contract_dt > start_dt (둘 다 있는 경우 대비)
    return toUiYmdAny(props.main?.contract_dt) ?? toUiYmdAny(props.main?.start_dt) ?? null;
  });

  const mainEndYmd = computed(() => {
    return toUiYmdAny(props.main?.project_end_dt) ?? null;
  });

  const mainPeriodLabel = computed(() => {
    const s = mainStartYmd.value;
    const e = mainEndYmd.value;
    if (s && e) return `${s} ~ ${e}`;
    if (s && !e) return `${s} ~`;
    if (!s && e) return `~ ${e}`;
    return '-';
  });

  // ✅ step 전체기간: steps 최소 start ~ 최대 end
  const minStepDate = computed<Date | null>(() => {
    let min: Date | null = null;
    for (const s of props.steps ?? []) {
      const d = parseYmd(s?.start_date);
      if (!d) continue;
      if (!min || d.getTime() < min.getTime()) min = d;
    }
    return min;
  });

  const maxStepDate = computed<Date | null>(() => {
    let max: Date | null = null;
    for (const s of props.steps ?? []) {
      const d = parseYmd(s?.end_date);
      if (!d) continue;
      if (!max || d.getTime() > max.getTime()) max = d;
    }
    return max;
  });

  const stepPeriodLabel = computed(() => {
    const s = minStepDate.value ? fmtYmd(minStepDate.value) : '';
    const e = maxStepDate.value ? fmtYmd(maxStepDate.value) : '';
    if (s && e) return `${s} ~ ${e}`;
    if (s) return `${s} ~`;
    if (e) return `~ ${e}`;
    return '-';
  });

  // ====== Timeline Range(기준은 step) ======
  const timelineStart = computed<Date>(() => {
    const d = minStepDate.value;
    if (d) return monthStart(d);

    // steps가 비어있으면 main 수행기간 기반 fallback
    const ms = parseYmd(mainStartYmd.value);
    if (ms) return monthStart(ms);

    return new Date(new Date().getFullYear(), 0, 1);
  });

  const timelineEnd = computed<Date>(() => {
    const d = maxStepDate.value;
    if (d) return monthStart(d);

    const me = parseYmd(mainEndYmd.value);
    if (me) return monthStart(me);

    return new Date(new Date().getFullYear(), 11, 1);
  });

  // 전체 월 수(포함)
  const totalMonths = computed(() => {
    const a = monthSerial(timelineStart.value);
    const b = monthSerial(timelineEnd.value);
    const diff = b - a + 1;
    return diff > 0 ? diff : 1;
  });

  // ✅ 빈공간 자동 맞춤 (짧으면 월폭 늘림, 길면 스크롤)
  const monthW = computed(() => {
    const vw = viewportW.value;
    const m = totalMonths.value;
    if (!vw || m <= 0) return BASE_MONTH_W;

    const need = m * BASE_MONTH_W;
    if (need >= vw) return BASE_MONTH_W;

    const fill = Math.floor(vw / m);
    return clamp(fill, BASE_MONTH_W, MAX_FILL_W);
  });

  const timelineWidthPx = computed(() => totalMonths.value * monthW.value);

  // 월 배열 생성
  const months = computed(() => {
    const list: { key: string; y: number; m: number; serial: number }[] = [];
    const startS = monthSerial(timelineStart.value);
    const endS = monthSerial(timelineEnd.value);

    for (let s = startS; s <= endS; s++) {
      const { y, m } = serialToYm(s);
      list.push({ key: `${y}-${m}`, y, m, serial: s });
    }
    return list;
  });

  // 년 헤더 그룹
  const yearGroups = computed(() => {
    const map = new Map<number, number>();
    for (const m of months.value) map.set(m.y, (map.get(m.y) ?? 0) + 1);
    return Array.from(map.entries()).map(([year, count]) => ({ year, count }));
  });

  function monthIndexFromDate(d: Date): number {
    return monthSerial(monthStart(d)) - monthSerial(timelineStart.value);
  }

  // ====== 스탭별 색상 ======
  const PALETTE = [
    '#5B61F6',
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

  const stepIndexByCode = computed(() => {
    const m = new Map<string, number>();
    props.stepMaster?.forEach((s, idx) => m.set(String(s.code), idx));
    return m;
  });

  function colorForStepCode(code: string) {
    const idx = stepIndexByCode.value.get(code) ?? 0;
    return PALETTE[idx % PALETTE.length];
  }

  // steps 빠른 조회용
  const stepByCode = computed<Record<string, any>>(() => {
    const map: Record<string, any> = {};
    for (const s of props.steps ?? []) {
      const code = String(s.step_cd ?? '');
      if (!code) continue;
      map[code] = s;
    }
    return map;
  });

  function barStyleForStep(step: any): { style: Record<string, string> } | null {
    const sd = parseYmd(step?.start_date);
    const ed = parseYmd(step?.end_date);
    if (!sd || !ed) return null;

    let sIdx = monthIndexFromDate(sd);
    let eIdx = monthIndexFromDate(ed) + 1; // 종료월 포함(Exclusive)

    const max = totalMonths.value;
    sIdx = clamp(sIdx, 0, max);
    eIdx = clamp(eIdx, 0, max);
    if (eIdx <= sIdx) return null;

    const leftPx = sIdx * monthW.value;
    const widthPx = Math.max(monthW.value, (eIdx - sIdx) * monthW.value);

    const bg = colorForStepCode(String(step?.step_cd ?? ''));

    return {
      style: {
        left: `${leftPx}px`,
        width: `${widthPx}px`,
        background: bg,
      },
    };
  }

  const barByStepCode = computed<Record<string, Record<string, string>>>(() => {
    const map: Record<string, Record<string, string>> = {};
    for (const s of props.steps ?? []) {
      const code = String(s.step_cd ?? '');
      if (!code) continue;

      const info = barStyleForStep(s);
      if (!info) continue;

      map[code] = info.style;
    }
    return map;
  });

  // ====== ✅ Tooltip ======
  const tooltip = ref<{ show: boolean; text: string; style: Record<string, string> }>({
    show: false,
    text: '',
    style: { left: '0px', top: '0px' },
  });

  function getStepDesc(code: string): string {
    const st = stepByCode.value[code];
    if (st?.step_desc) return String(st.step_desc);
    const m = props.stepMaster?.find((x) => String(x.code) === code);
    return m?.desc ?? code;
  }

  function getDurationDays(step: any, sd: Date, ed: Date): number {
    // 기존 데이터(duration_days)가 있으면 그걸 우선 사용(업무일 계산 결과일 가능성 높음)
    const raw = step?.duration_days;
    const n = Number(raw);
    if (Number.isFinite(n) && n > 0) return Math.floor(n);

    // fallback: 캘린더 일수(포함)
    const diff = Math.floor((ed.getTime() - sd.getTime()) / (24 * 3600 * 1000)) + 1;
    return diff > 0 ? diff : 0;
  }

  function placeTooltip(e: MouseEvent) {
    if (!scrollEl.value) return;

    const rect = scrollEl.value.getBoundingClientRect();

    // scroll 컨텐츠 기준 좌표로 배치(스크롤해도 정확)
    const x = e.clientX - rect.left + scrollEl.value.scrollLeft;
    const y = e.clientY - rect.top + scrollEl.value.scrollTop;

    const offset = 14;
    tooltip.value.style = {
      left: `${x + offset}px`,
      top: `${y + offset}px`,
    };
  }

  function onBarEnter(code: string, e: MouseEvent) {
    const step = stepByCode.value[code];
    if (!step) return;

    const sd = parseYmd(step?.start_date);
    const ed = parseYmd(step?.end_date);
    if (!sd || !ed) return;

    const desc = getStepDesc(code);
    const days = getDurationDays(step, sd, ed);

    // ✅ "제안, MM/DD ~ MM/DD, n일"
    tooltip.value.text = `${desc}, ${fmtMmdd(sd)} ~ ${fmtMmdd(ed)}, ${days}일`;
    tooltip.value.show = true;
    placeTooltip(e);
  }

  function onBarMove(e: MouseEvent) {
    if (!tooltip.value.show) return;
    placeTooltip(e);
  }

  function onBarLeave() {
    tooltip.value.show = false;
  }
</script>

<style scoped>
  .wbs-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    z-index: 9999;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 18px;
  }

  .wbs-modal {
    width: min(1200px, 92vw);
    height: min(760px, 88vh);
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 16px 40px rgba(15, 23, 42, 0.25);
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .wbs-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-bottom: 1px solid #eef2f7;
    background: #f8fafc;
  }

  .wbs-title {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
  }
  .wbs-sub {
    font-weight: 600;
    color: #475569;
    margin-left: 6px;
  }

  .wbs-close {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
    background: #fff;
    cursor: pointer;
    font-size: 20px;
    line-height: 1;
  }
  .wbs-close:hover {
    background: #f1f5f9;
  }

  .wbs-body {
    flex: 1;
    min-height: 0;
    padding: 14px 16px 16px 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  /* ✅ 상단 요약(좌/우 블럭) */
  .wbs-summary {
    display: flex;
    gap: 14px;
    flex-wrap: wrap;
    align-items: center;
  }
  .sum-item {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    border: 1px solid #eef2f7;
    border-radius: 12px;
    background: #fff;
  }
  .sum-label {
    font-size: 12px;
    color: #64748b;
    min-width: 56px;
  }
  .sum-val {
    font-size: 12px;
    color: #0f172a;
    font-weight: 700;
  }

  .gantt {
    flex: 1;
    min-height: 0;
    display: flex;
    border: 1px solid #eef2f7;
    border-radius: 12px;
    overflow: hidden;
  }

  .g-left {
    width: 170px;
    flex: 0 0 auto;
    border-right: 1px solid #eef2f7;
    background: #fff;
    display: flex;
    flex-direction: column;
  }
  .g-left-header {
    height: 72px;
    display: flex;
    align-items: center;
    padding: 0 10px;
    font-weight: 700;
    font-size: 12px;
    color: #0f172a;
    border-bottom: 1px solid #eef2f7;
    background: #f8fafc;
  }
  .g-left-row {
    height: 38px;
    display: flex;
    align-items: center;
    padding: 0 10px;
    border-bottom: 1px solid #f1f5f9;
    font-size: 12px;
    color: #334155;
    white-space: nowrap;
  }

  .g-right {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    background: #fff;
  }

  /* ✅ 기간이 길면 여기서 가로스크롤 자동 발생 */
  .g-scroll {
    position: relative; /* ✅ tooltip absolute 기준 */
    flex: 1;
    min-height: 0;
    overflow: auto;
  }

  .tooltip {
    position: absolute;
    z-index: 10;
    pointer-events: none;
    background: rgba(15, 23, 42, 0.92);
    color: #fff;
    font-size: 12px;
    padding: 8px 10px;
    border-radius: 10px;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.25);
    white-space: nowrap;
  }

  .g-header {
    position: sticky;
    top: 0;
    z-index: 2;
    background: #fff;
    border-bottom: 1px solid #eef2f7;
  }

  .year-row {
    height: 36px;
    display: flex;
    align-items: stretch;
    background: #f8fafc;
    border-bottom: 1px solid #eef2f7;
  }
  .year-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    color: #0f172a;
    border-right: 1px solid #eef2f7;
    white-space: nowrap;
  }

  .month-row {
    height: 36px;
    display: flex;
    align-items: stretch;
    background: #fff;
  }
  .month-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    color: #475569;
    border-right: 1px solid #eef2f7;
    white-space: nowrap;
  }

  .g-body {
    background: #fff;
  }

  .timeline-row {
    height: var(--row-h);
    position: relative;
    border-bottom: 1px solid #f1f5f9;

    /* 월 경계선 */
    background-image: linear-gradient(to right, rgba(15, 23, 42, 0.08) 1px, transparent 1px);
    background-size: var(--month-w) 100%;
    background-repeat: repeat;
  }

  .bar {
    position: absolute;
    top: 9px;
    height: 20px;
    border-radius: 999px;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.15);
    cursor: default;
  }

  .hint {
    padding: 8px 10px;
    font-size: 12px;
    color: #64748b;
    border-top: 1px solid #eef2f7;
    background: #fafafa;
  }
</style>
