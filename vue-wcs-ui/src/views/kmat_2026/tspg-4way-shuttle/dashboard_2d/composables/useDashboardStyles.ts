/**
 * useDashboardStyles.ts
 * 대시보드 설비/셔틀/화물 스타일 및 클래스 계산 컴포저블
 *
 * ============================================
 * 기능 설명
 * ============================================
 * - 정적 설비(RACK, CONVEYOR 등)의 위치/크기 스타일 계산
 * - 동적 설비(SHUTTLE)의 위치/크기 스타일 계산
 * - 화물(CARGO)의 위치/크기 스타일 계산
 * - 각 요소의 상태에 따른 CSS 클래스 결정
 * - 툴팁 텍스트 생성
 *
 * ============================================
 * 좌표계 설명
 * ============================================
 * - posX: 왼쪽 기준 X 좌표 (left 속성 사용)
 * - posY: 아래 기준 Y 좌표 (bottom 속성 사용)
 *   → 물류센터 좌표계는 보통 좌하단이 원점
 * - transform-origin: 'left bottom' 적용
 *
 * ============================================
 * z-index 레이어 구조
 * ============================================
 * - 정적 설비: 10 (가장 아래)
 * - 컨베이어 화물: 75
 * - 랙 재고 (보관중): 80
 * - 이동 화물: 90
 * - 셔틀: 100 (가장 위)
 *
 * ============================================
 * 사용 방법
 * ============================================
 * ```ts
 * const {
 *   getStaticEquipmentStyle,
 *   getEquipmentClass,
 *   getShuttleStyle,
 *   getShuttleClass,
 *   getCargoStyle,
 *   getCargoClass
 * } = useDashboardStyles(store, shuttleSize, cargoSize);
 * ```
 */

import { ComputedRef, Ref, unref } from 'vue';
import type {
  DashboardEquipmentData,
  TbEqRackMst,
  RtConveyorStatus,
  RtLifterStatus,
} from '../api/types';
import { LayoutEquipmentType } from '../api/types';

import { RackType } from '../../constants/EcsDBConsts';

/** Conveyor pallet 3-way 상태 — 백엔드 EquipmentStateClassifier.PalletState 와 1:1 일치 */
export type PalletStateCode = 'DATA_AND_PHY' | 'PHY_ONLY' | 'DATA_ONLY';

/** 컨베이어 화물 아이템 인터페이스 */
export interface ConveyorCargoItem {
  /** 레이아웃 데이터 */
  layout: DashboardEquipmentData;
  /** 컨베이어 실시간 상태 */
  cv: RtConveyorStatus;
  /** Pallet 상태 (백엔드 정규화값) */
  palletState: PalletStateCode;
}

/** 랙 재고 아이템 인터페이스 */
export interface RackInventoryItem {
  /** 레이아웃 데이터 */
  layout: DashboardEquipmentData;
  /** 랙 셀 재고 정보 */
  cell: TbEqRackMst;
}

/** 스토어 인터페이스 (필요한 부분만) */
export interface StyleStore {
  hasRackInventory: (cellId: string) => boolean;
  /** ECS 실시간 컨베이어 상태 (작업 정보 포함) */
  rtConveyors: Ref<RtConveyorStatus[]> | RtConveyorStatus[];
  /** ECS 실시간 리프터 상태 (작업 정보 포함) */
  rtLifters: Ref<RtLifterStatus[]> | RtLifterStatus[];
}

/**
 * 대시보드 스타일 컴포저블
 *
 * @param store - 스토어 인스턴스 (재고/컨베이어 상태 조회용)
 * @param shuttleSizeRef - 셔틀 크기 (반응형)
 * @param cargoSizeRef - 화물 크기 (반응형)
 * @param conveyorCargoItemsRef - 컨베이어 화물 아이템 목록 (반응형)
 * @returns 스타일/클래스 계산 함수들
 */
export function useDashboardStyles(
  store: StyleStore,
  shuttleSizeRef: Ref<number> | ComputedRef<number>,
  cargoSizeRef: Ref<number> | ComputedRef<number>,
  conveyorCargoItemsRef?: Ref<ConveyorCargoItem[]> | ComputedRef<ConveyorCargoItem[]>,
) {
  // ============================================
  // 정적 설비 스타일/클래스
  // ============================================

  /**
   * 정적 설비의 CSS 스타일 객체를 생성합니다.
   *
   * 처리 항목:
   * - 위치 (posX → left, posY → bottom)
   * - 크기 (width, height)
   * - 회전 (rotation)
   * - 뒤집기 (flipH, flipV → scaleX, scaleY)
   * - z-index, opacity
   *
   * @param obj - 설비 데이터 객체
   * @returns CSS 스타일 객체
   *
   * @example
   * const style = getStaticEquipmentStyle(rackData);
   * // 결과: { position: 'absolute', left: '100px', bottom: '200px', ... }
   */
  function getStaticEquipmentStyle(obj: DashboardEquipmentData) {
    const scaleX = (obj.scaleX || 1) * (obj.flipH ? -1 : 1);
    const scaleY = (obj.scaleY || 1) * (obj.flipV ? -1 : 1);

    return {
      position: 'absolute' as const,
      left: `${obj.posX}px`,
      bottom: `${obj.posY}px`,
      width: `${obj.width}px`,
      height: `${obj.height}px`,
      transform: `rotate(${obj.rotation || 0}deg) scale(${scaleX}, ${scaleY})`,

      // 🔥 이 부분을 찾아 반드시 'center center'로 변경하거나 추가하세요!
      transformOrigin: 'center center',

      zIndex: obj.zIndex || 0,
      opacity: obj.opacity ?? 1,
    };
  }

  /**
   * 정적 설비의 CSS 클래스 목록을 반환합니다.
   *
   * 적용되는 클래스:
   * - status-ready/reserve/run/emr-stop/error/complete: 설비 상태 (EcsDBConsts 기준)
   * - selected: 선택된 설비
   * - has-inventory: 재고가 있는 랙
   * - conveyor-state-*: 정규화된 컨베이어 상태 (legend-spec)
   * - ecs-active-job: ECS 작업 진행 중
   * - lifter-has-shuttle: 리프터에 셔틀 적재
   *
   * 상태 코드 (EcsDBConsts.EqCarStatus / EqRackStatus):
   * - 0: READY (대기)
   * - 1: RESERVE (예약)
   * - 2: RUN (작업중)
   * - 5: EMR_STOP (비상정지) / CARGO (화물적재-랙)
   * - 8: ERROR (에러)
   * - 9: COMPLETE (완료)
   *
   * @param obj - 설비 데이터 객체
   * @param selectedEquipmentIds - 현재 선택된 설비 ID 목록
   * @returns CSS 클래스 배열
   */
  function getEquipmentClass(
    obj: DashboardEquipmentData,
    selectedEquipmentIds?: string[] | null,
  ): string[] {
    const classes: string[] = [];

    // status-ready/-reserve/-run/-emr-stop/-complete 클래스는 제거됨.
    // 셔틀은 shuttle-state-*, 컨베이어는 conveyor-state-* 로 교체.

    if (selectedEquipmentIds?.includes(obj.id)) {
      classes.push('selected');
    }

    if (obj.equipmentTypeCode === LayoutEquipmentType.RACK.code) {
      if (obj.wcsLocUseYn === 0) {
        classes.push('status-disabled');
      }
      // 수동 잠금 (잠금여부가 1인 경우)
      if (obj.wcsLocLockYn === 1) {
        classes.push('status-locked');
      }

      // 랙 타입별 클래스 (입고포트, 출고포트, 입출고포트, 충전포트, 충전진입포트)
      const rackType = obj.realRackType;
      if (rackType != null) {
        switch (rackType) {
          case RackType.CELL.code:
            classes.push('rack-type-cell');
            break;
          case RackType.INBOUND_PORT.code:
            classes.push('rack-type-inbound');
            break;
          case RackType.OUTBOUND_PORT.code:
            classes.push('rack-type-outbound');
            break;
          case RackType.IN_OUTBOUND_PORT.code:
            classes.push('rack-type-inout');
            break;
          case RackType.CHARGE_PORT.code:
            classes.push('rack-type-charge');
            break;
          case RackType.CHARGE_ENTER_PORT.code:
            classes.push('rack-type-charge-enter');
            break;
        }
      }

      // 주행 전용 (적재 불가) 구역 표시
      if (obj.realRackDriveOnlyYn === true) {
        classes.push('rack-drive-only');
      }

      // WCS 정합성 오류 상태 — 대시보드에서 색상으로 즉시 식별 가능하게 표시
      // 공출고(Empty Pick): 시스템 재고O 실물X → 빨간 테두리/배경
      if (obj.wcsLocStatus === 30) classes.push('rack-empty-pick');
      // 이중입고(Double Entry): 실물O 시스템 재고X → 주황 테두리/배경
      if (obj.wcsLocStatus === 40) classes.push('rack-double-entry');
    }

    if (obj.equipmentTypeCode === LayoutEquipmentType.CONVEYOR.code && obj.realEqId) {
      const rtCv = getRtConveyors(store).find(
        (cv) => cv.eqId === obj.realEqId || cv.equipmentId === obj.realEqId,
      );
      if (rtCv) {
        // legend-spec 의 ConveyorState 코드값을 그대로 CSS 클래스로 매핑
        if (rtCv.conveyorState) {
          classes.push(`conveyor-state-${rtCv.conveyorState}`);
        }
        // run_yn 기반 모터 가동 펄스 효과 (범례엔 표기 없음, 시각 피드백만)
        if (rtCv.moving === true || rtCv.runYn === true) {
          classes.push('cv-moving');
        }
      }
    }

    if (obj.equipmentTypeCode === LayoutEquipmentType.LIFTER.code && obj.realEqId) {
      const rtLift = getRtLifters(store).find(
        (lf) => lf.eqId === obj.realEqId || lf.equipmentId === obj.realEqId,
      );
      if (rtLift) {
        if (rtLift.hasActiveJob) {
          classes.push('ecs-active-job');
        }
        if (rtLift.hasShuttle) {
          classes.push('lifter-has-shuttle');
        }
        if (rtLift.hasCargo) {
          classes.push('lifter-has-cargo');
        }
        // TODO: Lifter 도 정규화 *State 필드 도입 시 lifter-state-ERROR 로 교체.
        if (rtLift.status === 8) classes.push('status-error');
      }
    }

    if (obj.equipmentTypeCode === LayoutEquipmentType.PILLAR.code) {
      classes.push('pillar-base');
      classes.push('rack-type-ban-cell');
    }
    if (obj.equipmentTypeCode === LayoutEquipmentType.BCR.code) {
      classes.push('bcr-base');
    }
    if (obj.equipmentTypeCode === LayoutEquipmentType.STV.code) {
      classes.push('stv-base');
    }
    if (obj.equipmentTypeCode === LayoutEquipmentType.CRANE.code) {
      classes.push('crane-base');
    }

    return classes;
  }

  // ============================================
  // 셔틀 스타일/클래스
  // ============================================

  /**
   * 셔틀의 CSS 스타일 객체를 생성합니다.
   *
   * 특징:
   * - 셔틀 중심이 좌표에 오도록 offset 적용
   * - z-index: 100 (최상위 레이어)
   *
   * @param shuttle - 보간된 셔틀 데이터 (currentX, currentY 포함)
   * @returns CSS 스타일 객체
   */
  function getShuttleStyle(shuttle: any): Record<string, any> {
    const size = shuttleSizeRef.value;
    // 중심점 기준으로 표시하기 위한 offset
    const centerOffsetX = size / 2;
    const centerOffsetY = size / 2;

    // ✨ transform 충돌(에러 애니메이션 등)을 막기 위해 left, bottom 직접 사용
    return {
      position: 'absolute' as const,
      left: `${shuttle.currentX - centerOffsetX}px`,
      bottom: `${shuttle.currentY - centerOffsetY}px`,
      width: `${size}px`,
      height: `${size}px`,
      zIndex: 100,
    };
  }

  /**
   * 셔틀의 CSS 클래스 목록.
   *
   * 상태는 백엔드 `shuttleState` 정규화 값(legend-spec 의 ShuttleState) 만 사용한다.
   * 우선순위 / 세부 분기는 전부 backend EquipmentStateClassifier 에서 결정 완료됨.
   *
   * 추가 클래스 (범례 외 부가 표현):
   * - low-battery: 배터리 20% 이하 경고
   * - selected: 선택된 셔틀
   */
  function getShuttleClass(shuttle: any, selectedShuttleId?: string | null): string[] {
    const classes: string[] = [];
    const data = shuttle.data || {};

    // 정규화된 shuttleState 그대로 CSS 클래스로. 누락 시 IDLE 로 fallback.
    const code = (data.shuttleState as string) || 'IDLE';
    classes.push(`shuttle-state-${code}`);

    // 배터리 부족 경고 (20% 이하) — 범례엔 표기 없음, 부가 표현
    if (data.batteryLevel != null && data.batteryLevel <= 20) {
      classes.push('low-battery');
    }

    if (selectedShuttleId === shuttle.id) {
      classes.push('selected');
    }

    return classes;
  }

  // ============================================
  // 화물 스타일/클래스
  // ============================================

  /**
   * 화물의 CSS 스타일 객체를 생성합니다.
   *
   * z-index:
   * - 보관중(2): 80
   * - 이동중(1,3): 90
   *
   * @param cargo - 보간된 화물 데이터 (currentX, currentY 포함)
   * @returns CSS 스타일 객체
   */
  function getCargoStyle(cargo: any): Record<string, any> {
    const size = cargoSizeRef.value;
    const half = size / 2;
    const cargoStatus = cargo.data?.cargoStatus ?? 0;
    const zIndex = cargoStatus === 2 ? 80 : 90;

    const tx = cargo.currentX - half;
    const ty = -(cargo.currentY - half);

    return {
      position: 'absolute' as const,
      left: '0px',
      bottom: '0px',
      width: `${size}px`,
      height: `${size}px`,
      transform: `translate3d(${tx}px, ${ty}px, 0)`,
      willChange: 'transform',
      zIndex,
    };
  }

  /**
   * 화물의 CSS 클래스 목록을 반환합니다.
   *
   * 상태별 클래스:
   * - cargo-pending: 대기 (0)
   * - cargo-moving: 이동중 (1)
   * - cargo-stored: 보관중 (2)
   * - cargo-picking: 피킹중 (3)
   * - cargo-error: 에러 (9)
   *
   * @param cargo - 화물 데이터
   * @returns CSS 클래스 배열
   */
  function getCargoClass(cargo: any): string[] {
    const classes: string[] = [];

    switch (cargo.data?.cargoStatus) {
      case 0:
        classes.push('cargo-pending');
        break;
      case 1:
        classes.push('cargo-moving');
        break;
      case 2:
        classes.push('cargo-stored');
        break;
      case 3:
        classes.push('cargo-picking');
        break;
      case 9:
        classes.push('cargo-error');
        break;
    }

    return classes;
  }

  /**
   * 화물 툴팁 텍스트를 생성합니다.
   *
   * @param cargo - 화물 데이터
   * @returns 툴팁 문자열 (줄바꿈 포함)
   */
  function getCargoTooltip(cargo: any): string {
    const data = cargo.data || {};

    /** 상태 코드 → 텍스트 매핑 */
    const statusMap: Record<number, string> = {
      0: '대기',
      1: '이동중',
      2: '보관중',
      3: '피킹중',
      9: '에러',
    };

    const status = statusMap[data.cargoStatus] || '알 수 없음';

    return `바코드: ${data.barcode || '-'}\n상태: ${status}\n셀: ${data.storedCellId || '-'}`;
  }

  // ============================================
  // 컨베이어 화물 스타일
  // ============================================

  /**
   * 컨베이어 화물의 CSS 스타일 객체를 생성합니다.
   *
   * 특징:
   * - 컨베이어 크기의 60% 크기로 표시
   * - 컨베이어 중앙에 위치
   * - z-index: 75
   *
   * @param item - 컨베이어 화물 아이템
   * @returns CSS 스타일 객체
   */
  function getConveyorCargoStyle(item: ConveyorCargoItem): Record<string, any> {
    const layout = item.layout;
    const width = layout.width || layout.defaultWidth || 100;
    const height = layout.height || layout.defaultHeight || 50;

    // 화물 크기: 컨베이어 크기의 60%
    const cargoSize = Math.min(width, height) * 0.6;

    return {
      position: 'absolute' as const,
      left: `${layout.posX + (width - cargoSize) / 2}px`,
      bottom: `${layout.posY + (height - cargoSize) / 2}px`,
      width: `${cargoSize}px`,
      height: `${cargoSize}px`,
      zIndex: 75,
    };
  }

  /**
   * 컨베이어 화물 툴팁 텍스트를 생성합니다.
   * ECS 작업 정보도 포함합니다.
   *
   * @param item - 컨베이어 화물 아이템
   * @returns 툴팁 문자열
   */
  function getConveyorCargoTooltip(item: ConveyorCargoItem): string {
    const statusMap: Record<PalletStateCode, string> = {
      DATA_AND_PHY: '정상 (데이터+화물)',
      PHY_ONLY: '⚠️ 화물만 있음 (데이터 누락)',
      DATA_ONLY: '⚠️ 데이터만 있음 (화물 없음)',
    };

    let tooltip = `컨베이어: ${item.layout.realEqId || '-'}\n상태: ${statusMap[item.palletState]}`;

    const rtConveyors = getRtConveyors(store);

    // ECS 작업 정보 추가
    const rtCv = rtConveyors.find(
      (cv) => cv.eqId === item.layout.realEqId || cv.equipmentId === item.layout.realEqId,
    );
    if (rtCv?.hasActiveJob) {
      const jobTypeMap: Record<number, string> = { 11: '입고', 12: '출고', 21: '충전', 22: '이동' };
      tooltip += `\n\n[현재 작업]`;
      tooltip += `\n작업키: ${rtCv.currentOrderKey || '-'}`;
      tooltip += `\n타입: ${jobTypeMap[rtCv.currentOrderType || 0] || '-'}`;
      tooltip += `\n바코드: ${rtCv.currentBarcode || '-'}`;
      tooltip += `\n${rtCv.currentFromLoc || '-'} → ${rtCv.currentToLoc || '-'}`;
    }

    return tooltip;
  }

  /**
   * 리프터 툴팁 텍스트를 생성합니다.
   * ECS 작업 정보, 셔틀/화물 운반 상태 포함
   *
   * @param obj - 설비 데이터 객체
   * @returns 툴팁 문자열
   */
  function getLifterTooltip(obj: DashboardEquipmentData): string {
    const rtLifters = getRtLifters(store);

    const rtLift = rtLifters.find(
      (lf) => lf.eqId === obj.realEqId || lf.equipmentId === obj.realEqId,
    );

    if (!rtLift) {
      return `리프터: ${obj.realEqId || obj.equipmentCode || '-'}`;
    }

    let tooltip = `리프터: ${rtLift.eqId || obj.equipmentCode || '-'}`;
    tooltip += `\n현재층: ${rtLift.currentLevel || '-'}F`;

    if (rtLift.moving && rtLift.targetLevel) {
      tooltip += ` → ${rtLift.targetLevel}F 이동중`;
    }

    if (rtLift.hasShuttle) {
      tooltip += `\n🚗 셔틀 운반 중`;
    }
    if (rtLift.hasCargo) {
      tooltip += `\n📦 화물 적재`;
    }

    // ECS 작업 정보 추가
    if (rtLift.hasActiveJob) {
      const jobTypeMap: Record<number, string> = { 11: '입고', 12: '출고', 21: '충전', 22: '이동' };
      tooltip += `\n\n[현재 작업]`;
      tooltip += `\n작업키: ${rtLift.currentOrderKey || '-'}`;
      tooltip += `\n타입: ${jobTypeMap[rtLift.currentOrderType || 0] || '-'}`;
      tooltip += `\n바코드: ${rtLift.currentBarcode || '-'}`;
      tooltip += `\n${rtLift.currentFromLoc || '-'} → ${rtLift.currentToLoc || '-'}`;
    }

    if (rtLift.errorId) {
      tooltip += `\n\n⚠️ 에러: ${rtLift.errorId}`;
      if (rtLift.errorMessage) tooltip += `\n${rtLift.errorMessage}`;
    }

    return tooltip;
  }

  /**
   * 컨베이어 툴팁 텍스트를 생성합니다.
   * ECS 작업 정보 포함
   *
   * EcsDBConsts.EqConveyorStatus 기준:
   * - 0: READY (대기)
   * - 1: MOVE_RESERVE (주행 예약)
   *
   * @param obj - 설비 데이터 객체
   * @returns 툴팁 문자열
   */
  function getConveyorTooltip(obj: DashboardEquipmentData): string {
    const rtCv = getRtConveyors(store).find(
      (cv) => cv.eqId === obj.realEqId || cv.equipmentId === obj.realEqId,
    );

    if (!rtCv) {
      return `컨베이어: ${obj.realEqId || obj.equipmentCode || '-'}`;
    }

    let tooltip = `컨베이어: ${rtCv.eqId || obj.equipmentCode || '-'}`;

    // 상태 정보 (EcsDBConsts.EqConveyorStatus 기준)
    const statusMap: Record<number, string> = {
      0: '대기', // READY
      1: '주행 예약', // MOVE_RESERVE
      2: '가동중', // (확장용)
      8: '에러', // ERROR
    };
    tooltip += `\n상태: ${statusMap[rtCv.status ?? 0] || '알 수 없음'}`;

    // 화물 정보
    if (rtCv.hasCargo) {
      tooltip += `\n📦 화물 있음`;
    }

    // ECS 작업 정보 추가
    if (rtCv.hasActiveJob) {
      const jobTypeMap: Record<number, string> = { 11: '입고', 12: '출고', 21: '충전', 22: '이동' };
      tooltip += `\n\n[현재 작업]`;
      tooltip += `\n작업키: ${rtCv.currentOrderKey || '-'}`;
      tooltip += `\n타입: ${jobTypeMap[rtCv.currentOrderType || 0] || '-'}`;
      tooltip += `\n바코드: ${rtCv.currentBarcode || '-'}`;
      tooltip += `\n${rtCv.currentFromLoc || '-'} → ${rtCv.currentToLoc || '-'}`;
    }

    if (rtCv.errorCode) {
      tooltip += `\n\n⚠️ 에러: ${rtCv.errorCode}`;
      if (rtCv.errorMessage) tooltip += `\n${rtCv.errorMessage}`;
    }

    return tooltip;
  }

  // ============================================
  // 랙 재고 스타일
  // ============================================

  /**
   * 랙 재고 화물의 CSS 스타일 객체를 생성합니다.
   *
   * 특징:
   * - 랙 크기의 70% 크기로 표시
   * - 랙 중앙에 위치
   * - z-index: 80
   *
   * @param inv - 랙 재고 아이템
   * @returns CSS 스타일 객체
   */
  function getInventoryCargoStyle(inv: RackInventoryItem): Record<string, any> {
    const layout = inv.layout;
    const width = layout.width || layout.defaultWidth || 100;
    const height = layout.height || layout.defaultHeight || 100;

    // 화물 크기: 랙 크기의 70%
    const cargoSize = Math.min(width, height) * 0.7;

    return {
      position: 'absolute' as const,
      left: `${layout.posX + (width - cargoSize) / 2}px`,
      bottom: `${layout.posY + (height - cargoSize) / 2}px`,
      width: `${cargoSize}px`,
      height: `${cargoSize}px`,
      zIndex: 80,
    };
  }

  // ============================================
  // 유틸리티 함수
  // ============================================

  /**
   * 화물 상태 코드를 텍스트로 변환합니다.
   *
   * @param status - 상태 코드
   * @returns 상태 텍스트
   */
  function getCargoStatusText(status: number | undefined): string {
    const statusMap: Record<number, string> = {
      0: '대기',
      1: '이동중',
      2: '보관중',
      3: '피킹중',
      9: '에러',
    };

    return statusMap[status ?? -99] || '알 수 없음';
  }

  /**
   * 화물 상태 코드를 CSS 클래스로 변환합니다.
   *
   * @param status - 상태 코드
   * @returns CSS 클래스명
   */
  function getCargoStatusClass(status: number | undefined): string {
    const classMap: Record<number, string> = {
      0: 'status-pending',
      1: 'status-moving',
      2: 'status-stored',
      3: 'status-picking',
      9: 'status-error',
    };

    return classMap[status ?? -99] || '';
  }

  function getRtConveyors(store: StyleStore): RtConveyorStatus[] {
    const data = unref(store.rtConveyors as any);
    return Array.isArray(data) ? data : [];
  }

  function getRtLifters(store: StyleStore): RtLifterStatus[] {
    const data = unref(store.rtLifters as any);
    return Array.isArray(data) ? data : [];
  }

  // ============================================
  // 반환 (Return)
  // ============================================

  return {
    // 정적 설비
    getStaticEquipmentStyle,
    getEquipmentClass,

    // 셔틀
    getShuttleStyle,
    getShuttleClass,

    // 화물
    getCargoStyle,
    getCargoClass,
    getCargoTooltip,
    getCargoStatusText,
    getCargoStatusClass,

    // 컨베이어 화물
    getConveyorCargoStyle,
    getConveyorCargoTooltip,

    // 컨베이어 설비
    getConveyorTooltip,

    // 리프터
    getLifterTooltip,

    // 랙 재고
    getInventoryCargoStyle,
  };
}
