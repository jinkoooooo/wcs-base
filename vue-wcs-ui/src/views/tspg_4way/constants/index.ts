/**
 * tspg-4way-shuttle/constants
 *
 * dashboard_2d / cell_state_2d / 그 외 sub-module 가 공통으로 import 하는 상수 모음.
 * 백엔드 enum/상태값과 1:1 매칭되며, 백엔드 변경 시 이 폴더만 수정하면 된다.
 *
 * - EcsDBConsts:    ECS 도메인 enum 전체 (설비/오더/포트/IF 등)
 * - wcsConsts:      WCS 오더/로케이션/재고 상태 + 라벨
 * - statusHelpers:  상태 판정 유틸 (isOrderError 등)
 */

export * from './enumHelpers';
export * from './EcsDBConsts';
export * from './wcsConsts';
export * from './statusHelpers';
