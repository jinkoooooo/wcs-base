/**
 * gridSummaryBuilder.ts
 *
 * TUI Grid 하단 합계행(summary)을 컬럼 정의 기반으로 자동 생성하는 범용 유틸리티.
 *
 * ── 사용법 ──────────────────────────────────────────────
 *
 * 1) BasicGrid를 직접 쓰는 페이지 (CellUsage 등)
 *    import { buildGridSummary } from '/@/utils/grid/gridSummaryBuilder';
 *
 *    const gridOptions = {
 *      rowHeight: 30,
 *      ...buildGridSummary(gridColumns, {
 *        labelColumn: 'layer',
 *        overrides: {
 *          usage_rate: () => computeWeightedRate(),
 *        },
 *      }),
 *    };
 *
 * 2) CommonPage 내부
 *    buildGridSummary(columns) 호출 후
 *    gridProps.options 에 spread
 *
 * ── API ─────────────────────────────────────────────────
 *
 * buildGridSummary(columns, options?)
 *   @param columns      - TUI Grid 컬럼 배열 ({ name, align, ... }[])
 *   @param options       - 선택 옵션 (아래 참고)
 *   @returns { summary } - gridOptions에 spread 가능한 객체
 *
 * Options:
 *   labelColumn?    : string            — '합계' 라벨을 넣을 컬럼 name (기본: 첫 번째 컬럼)
 *   labelText?      : string            — 라벨 텍스트 (기본: '합계')
 *   height?         : number            — 합계행 높이 (기본: 32)
 *   sumColumns?     : string[]          — 합산 대상 컬럼 (빈 배열이면 align='right' 자동 감지)
 *   formatter?      : (v: number) => string — 숫자 포맷 (기본: 천단위 콤마)
 *   overrides?      : Record<string, (summaryValues: any) => string>
 *                                       — 컬럼별 커스텀 template 함수
 */

export interface GridSummaryOptions {
  /** '합계' 라벨을 표시할 컬럼 name. 미지정 시 첫 번째 컬럼 */
  labelColumn?: string;
  /** 라벨 텍스트 (기본: '합계') */
  labelText?: string;
  /** 합계행 높이 (기본: 32) */
  height?: number;
  /** 합산할 컬럼 name 배열. 빈 배열이면 align='right' 자동 감지 */
  sumColumns?: string[];
  /** 숫자 포맷 함수 (기본: 천단위 콤마) */
  formatter?: (value: number) => string;
  /**
   * 컬럼별 커스텀 summary template 오버라이드.
   * key: 컬럼 name, value: (summaryValues) => string
   *
   * summaryValues는 TUI Grid가 넘겨주는 { sum, avg, min, max, cnt } 객체.
   * 단, 외부 데이터 기반 계산이 필요하면 인자 무시하고 직접 계산 가능.
   *
   * 예: { usage_rate: ({ avg }) => avg.toFixed(2) }
   * 예: { usage_rate: () => computeWeightedRate() }  // 외부 reactive 참조
   */
  overrides?: Record<string, (summaryValues: any) => string>;
}

/** 기본 포맷: 천단위 콤마 */
function defaultFormatter(value: number): string {
  if (value == null || isNaN(value)) return '0';
  return Number(value).toLocaleString();
}

/**
 * TUI Grid 컬럼 배열로부터 summary 옵션을 자동 생성한다.
 *
 * @returns `{ summary: { ... } }` 객체 — gridOptions에 spread하면 됨
 */
export function buildGridSummary(
  columns: any[],
  options: GridSummaryOptions = {},
): { summary: Record<string, any> } {
  if (!columns || columns.length === 0) {
    return { summary: {} };
  }

  const {
    labelColumn = '',
    labelText = '합계',
    height = 32,
    sumColumns = [],
    formatter = defaultFormatter,
    overrides = {},
  } = options;

  // ── 합산 대상 컬럼 결정 ──
  let sumTargetNames: string[];
  if (sumColumns.length > 0) {
    sumTargetNames = [...sumColumns];
  } else {
    // 자동 감지: align이 'right'인 컬럼 = 숫자 컬럼으로 간주
    sumTargetNames = columns
      .filter((col) => col.align === 'right' || col.align === 'center-right')
      .map((col) => col.name);
  }

  // ── 라벨 컬럼 결정 ──
  const labelColName = labelColumn || columns[0]?.name || '';

  // ── columnContent 구성 ──
  const columnContent: Record<string, any> = {};

  // 라벨 컬럼
  if (labelColName) {
    columnContent[labelColName] = {
      template: () => labelText,
    };
  }

  // 합산 컬럼
  sumTargetNames.forEach((colName) => {
    // 라벨 컬럼과 겹치면 스킵
    if (colName === labelColName) return;

    // 커스텀 오버라이드 우선
    if (overrides[colName]) {
      columnContent[colName] = {
        template: overrides[colName],
      };
    } else {
      // 기본: sum 포맷
      columnContent[colName] = {
        template: ({ sum }: any) => formatter(sum),
      };
    }
  });

  return {
    summary: {
      height,
      position: 'bottom',
      columnContent,
    },
  };
}
