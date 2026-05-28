export const CENTER_STATUS = ['running', 'warning', 'stopped'] as const
export type CenterStatusType = typeof CENTER_STATUS[number]
export const ALARM_LEVEL = ['info', 'warn', 'error', 'clearedError'] as const
export type AlarmLevelType = typeof ALARM_LEVEL[number]
export const EQUIP_STATUS = ['run', 'idle', 'stop', 'error'] as const
export type EquipStatusType = typeof EQUIP_STATUS[number]

// 대시보드에 표시할 센터 정보
export interface CenterInfo {
  lcId:         string            // 센터 코드
  lcNm:         string            // 센터 명
  lineNm:       string            // 라인 명
  centerStatus: CenterStatusType  // 센터 상태
}

// KPI 측정 지표
export interface KpiMetric {
  value:  number    // 값
  unit:   string    // 단위
  change: number    // 변화율
  up:     boolean   // 이전 값 대비 상승/하강
}

// 대시보드에 표시할 KPI
export interface KpiData {
  orderNumber:     string    // 지시번호 (e.g. batchSeq)
  throughput:      KpiMetric // 시간 당 처리율
  capacityUtilitzation:   KpiMetric // 설비 가동률
  fullFillmentRate:       KpiMetric // 출고 지시 처리율 / TODO: 입고 지시 포함 여부 검토
  avgProcessTime:  KpiMetric // 평균 처리 시간
}

// 운영 현황 탭 - 실시간 알람
export interface Alarm {
  id:         string         // 알람 ID
  equipId:    string         // 관련 설비
  alarmMsg:   string         // 알람 메세지
  level:      AlarmLevelType // 알람 수준
  occurredAt: string         // 알람 발생 일시
}

// 운영 현황 탭 - 설비 상태 통계
export interface EquipmentStatus {
  run:   number // 가동 설비
  stop:  number // 정지 설비
  error: number // 에러 설비
  idle:  number // 대기 설비
}

// 운영 협황 탭 - 시간별 처리량
export interface Throughput {
  hours:   string[] // 시간 목록
  hourly:  number[] // 시간 당 처리량
  daily:   number   // 일간 처리량
  weekly:  number   // 주간 처리량
  monthly: number   // 월간 처리량
}

// 운영 협황 탭 - 입출고 흐름 데이터
export interface FlowPeriod {
  labels:   string[] // 시간 목록
  inbound:  number[] // 시간 당 입고량
  outbound: number[] // 시간 당 출고량
}

// 운영 협황 탭 - 입출고 흐름
export interface InventoryFlow {
  daily:   FlowPeriod // 일별 입/출고량
  weekly:  FlowPeriod // 주별 입/출고량
  monthly: FlowPeriod // 월별 입/출고량
}

// 작업 관리 탭 - 입/출고/재고정리 이행현황
export interface FulfillmentData {
  total:      number // 전체 지시
  completed:  number // 완료 지시
  inProgress: number // 진행중인 지시
  pending:    number // 대기 지시
}

// 작업 관리 탭 - 최근 완료 작업
export interface RecentOrder {
  orderNumber: string // 작업 번호
  quantity:    number // 박스 수
  completedAt: string // 완료일시
  duration:    string // 작업 소요 시간
}

// 작업 관리 탭 - 목표 대비 실적
export interface TargetVsActual {
  target: number // 목표 건수
  actual: number // 실적 건수
}

// 설비/품질 탭 - 설비 상태 목록
export interface Equipment {
  id:        string           // 설비 코드
  name:      string           // 설비 명
  status:    EquipStatusType  // 설비 상태
  uptime:    string           // 가동 시간
  lastError: string           // 마지막 에러 메세지
}

// 설비/품질 탭 - 유지보수 현황
export interface Maintenance {
  inProgress: number // 진행 건수
  completed:  number // 완료 건수
  deleted:    number // 삭제 건수
}

// 설비/품질 탭 - 에러율 추이
export interface ErrorRateTrend {
  hours:     string[] // 시간 목록
  rates:     number[] // 시간 당 에러율
  threshold: number   // 기준 에러율
}
