package operato.logis.ecs.base.wcs.consts;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class WcsDomainEnums {

    private WcsDomainEnums() { }

    // =========================================================================
    // 1. 셔틀 오더 처리 상태 (tb_wcs_shuttle_order.order_status)
    // =========================================================================

    public enum ShuttleOrderStatus implements EnumCode {

        // --- [정상 프로세스: 0 ~ 89] ---
        CREATED(0, "생성됨 - 주문 생성 및 데이터 준비 완료"),
        SENT(10, "전송됨 - ECS 인터페이스 전송 완료"),
        ACCEPTED(20, "수락됨 - ECS에서 주문 수신 및 적합성 확인"),
        WAITING(25, "대기 - 물리적 트리거 대기 (BCR 스캔 전)"),
        RUNNING(30, "실행중 - 설비(셔틀/컨베이어) 가동 중"),
        ARRIVED(40, "도착 - 목적지 렉단/컨베이어 도착 완료"),

        // --- [종료 상태: 90 ~ 99] ---
        COMPLETED(90, "완료 - 전체 작업 정상 종료"),
        CANCELLED(91, "취소 - 사용자 또는 시스템에 의한 강제 취소"),
        ABORTED(95, "중단 - 작업 중 미완료 상태로 강제 종료"),

        // --- [에러 상태: 100 이상] ---
        ERROR_GENERAL(100, "일반 에러 - 정의되지 않은 일반 오류"),
        ERROR_SEND_FAIL(110, "전송 실패 - ECS 통신/소켓 전송 실패"),
        ERROR_TIMEOUT(120, "응답 지연 - ECS 작업 지시 응답 시간 초과"),
        ERROR_HARDWARE(130, "설비 에러 - 셔틀/설비 하드웨어 장애(알람)"),
        ERROR_LOCATION(140, "로케이션 에러 - 위치 부정합 또는 가득 참"),
        ERROR_INVENTORY(150, "재고 에러 - 재고 부족 또는 유실 발생"),
        ERROR_SYSTEM_RESTART(190, "시스템 재시작 에러 - 서버 재기동 시 진행 중 상태로 남은 고아 오더");

        private final Integer code;
        private final String desc;

        ShuttleOrderStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public Integer code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() {
            return switch (this) {
                case RUNNING -> List.of("IN_PROGRESS", "WORK");
                case COMPLETED -> List.of("COMPLETE", "FINISHED", "SUCCESS");
                case CANCELLED -> List.of("CANCEL", "VOID");
                case ERROR_GENERAL -> List.of("ERROR", "FAILED", "FAIL", "ERR");
                default -> List.of();
            };
        }

        public static boolean isFinalStatus(Integer code) {
            return code != null && code >= COMPLETED.codeAsIntOrNull();
        }

        public static ShuttleOrderStatus from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(ShuttleOrderStatus.class, codeOrAlias,
                    true, ShuttleOrderStatus::aliases);
        }
    }

    // =========================================================================
    // 2. HOST 오더 처리 상태 (tb_wcs_host_order.order_status)
    // =========================================================================

    public enum HostOrderStatus implements EnumCode {

        RECEIVED(0, "수신됨"),
        WAITING_SCHEDULE(5, "예정일 대기 - scheduled_date 미도래 상태"),
        TEST_WAIT(7, "시험 대기 - 외부 시험 API 응답 대기 중"),
        TEST_FAILED(8, "시험 실패 - 사용자 확인 필요. 산출 절대 진입 불가"),
        VALIDATED(10, "검증완료"),
        READY_FOR_ALLOC(12, "산출 준비 완료 - 다음 스케줄러 tick에 산출"),
        // ALLOCATED(20) 제거 — 실사용 없음. READY_FOR_ALLOC → WAITING_EXEC 직결.
        WAITING_EXEC(30, "ECS 실행 대기"),
        EXECUTING(40, "설비 실행 중"),
        PUTBACK_WAIT(60, "하위 재입고 대기 - 원 OUTBOUND 완료, PUTBACK 진행 중"),
        COMPLETED(80, "작업 완료"),
        CANCELLED(85, "취소"),
        REJECTED(88, "검증 실패 거절"),
        ERROR(100, "처리 중 오류");

        private final Integer code;
        private final String desc;

        HostOrderStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public Integer code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() {
            return switch (this) {
                case WAITING_EXEC -> List.of("PROCESSING");
                case VALIDATED -> List.of("VALIDATE");
                default -> List.of();
            };
        }

        public static HostOrderStatus from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(HostOrderStatus.class, codeOrAlias,
                    true, HostOrderStatus::aliases);
        }
    }

    // =========================================================================
    // 3. ECS 인터페이스 상태 (tb_wcs_shuttle_order.ecs_if_status)
    // =========================================================================

    public enum EcsIfStatus implements EnumCode {

        /** 초기 생성 — ECS 미전달, 오케스트레이터 대기 중 */
        READY(0, "대기중 - 아직 ECS로 전송하지 않은 상태"),
        /** WcsOrderOrchestrator 가 ECS 릴리즈를 결정하고 큐에 올림 */
        SENDING(10, "전송중(릴리즈) - 오케스트레이터가 ECS 전달을 허가한 상태"),
        SENT(20, "전송됨 - ECS로 명령 전송 완료"),
        ACK(30, "응답수신 - ECS로부터 정상 응답 수신"),
        FAIL(99, "실패 - ECS 통신 오류");

        private final Integer code;
        private final String desc;

        EcsIfStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public Integer code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() { return List.of(); }

        public static EcsIfStatus from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(EcsIfStatus.class, codeOrAlias,
                    true, EcsIfStatus::aliases);
        }
    }

    // =========================================================================
    // 4. ECS 콜백 상태 코드 (콜백 요청 body.status)
    // =========================================================================

    public enum EcsCallbackStatus implements EnumCode {

        ACCEPTED("ACCEPTED", "작업 수락 - ECS가 작업 지시를 정상적으로 수신하고 처리 대기열에 등록"),
        STARTED("STARTED", "작업 시작 - 셔틀이 해당 작업을 시작함"),
        IN_PROGRESS("IN_PROGRESS", "작업 진행 중 - 중간 보고(선택)"),
        FROM_LOADING_COMPLETE("FROM_LOADING_COMPLETE", "출발지 로딩 완료 - from에서 화물 픽업 완료"),
        TO_UNLOADING_COMPLETE("TO_UNLOADING_COMPLETE", "목적지 언로딩 완료 - to에 화물 적재 완료"),
        RACK_CONVEYOR_ARRIVED("RACK_CONVEYOR_ARRIVED", "작업 진행 중 - 렉단 컨베이어 도착"),
        COMPLETE("COMPLETE", "작업 완료 - 셔틀이 작업을 성공적으로 완료함"),
        ERROR("ERROR", "작업 오류 - 셔틀 작업 중 오류 발생"),
        CANCELLED("CANCELLED", "작업 취소 - 작업이 취소됨(운영 정책에 따라 사용)");

        private final String code;
        private final String desc;

        EcsCallbackStatus(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() {
            return switch (this) {
                case COMPLETE -> List.of("COMPLETED");
                case ERROR -> List.of("FAILED");
                default -> List.of();
            };
        }

        public static EcsCallbackStatus from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(EcsCallbackStatus.class, codeOrAlias,
                    true, EcsCallbackStatus::aliases);
        }

        public static boolean isAccepted(Object codeOrAlias) { return ACCEPTED.equals(from(codeOrAlias)); }

        public static boolean isStarted(Object codeOrAlias) { return STARTED.equals(from(codeOrAlias)); }

        public static boolean isInProgress(Object codeOrAlias) { return IN_PROGRESS.equals(from(codeOrAlias)); }

        public static boolean isComplete(Object codeOrAlias) { return COMPLETE.equals(from(codeOrAlias)); }

        public static boolean isError(Object codeOrAlias) { return ERROR.equals(from(codeOrAlias)); }

        public static boolean isCancelled(Object codeOrAlias) { return CANCELLED.equals(from(codeOrAlias)); }
    }

    // =========================================================================
    // 5. 포트 운영 모드 (tb_inventory_location.port_mode)
    // =========================================================================

    public enum PortMode implements EnumCode {

        IDLE("IDLE", "유휴 - 입/출고 모두 가능한 대기 상태"),
        INBOUND("INBOUND", "입고 모드 - 현재 입고 작업 중 (출고 할당 차단)"),
        OUTBOUND("OUTBOUND", "출고 모드 - 현재 출고 작업 대기/진행 중"),
        OUTBOUND_PRIORITY("OUTBOUND_PRIORITY", "출고 우선 모드 - 기아 방지로 인한 신규 입고 차단, 현재 입고 완료 후 강제 출고 전환"),

        /**
         * INBOUND 에서 OUTBOUND 로 전환 대기 중인 드레인 상태.
         * 진행중인 INBOUND 작업이 모두 종료될 때까지 신규 배차는 모두 차단된다.
         * 작업 0건이 되는 순간 스케줄러가 자동으로 OUTBOUND 로 전환한다.
         * 운영자는 이 상태에서 "전환 취소"를 통해 INBOUND 로 복귀할 수 있다.
         */
        SWITCHING_TO_OUTBOUND("SWITCHING_TO_OUTBOUND", "출고 전환 대기 - 진행중 입고 작업 완료 시 자동으로 OUTBOUND 로 전환"),

        /**
         * OUTBOUND 에서 INBOUND 로 전환 대기 중인 드레인 상태.
         * PUTBACK 을 포함한 진행중 OUTBOUND 작업이 모두 종료될 때까지 신규 배차는 차단된다.
         */
        SWITCHING_TO_INBOUND("SWITCHING_TO_INBOUND", "입고 전환 대기 - 진행중 출고/PUTBACK 작업 완료 시 자동으로 INBOUND 로 전환");

        private final String code;
        private final String desc;

        PortMode(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        /** 신규 배차를 막아야 하는 드레인 상태인가. Orchestrator 의 PortAbility 판정에 사용. */
        public boolean isDraining() {
            return this == SWITCHING_TO_INBOUND || this == SWITCHING_TO_OUTBOUND;
        }

        /** SWITCHING_* 상태일 때 최종 목표 모드 반환. 일반 모드면 null. */
        public PortMode pendingTarget() {
            return switch (this) {
                case SWITCHING_TO_INBOUND -> INBOUND;
                case SWITCHING_TO_OUTBOUND -> OUTBOUND;
                default -> null;
            };
        }

        /** 현재 모드(드레인 직전 모드) 반환. SWITCHING_TO_OUTBOUND 면 INBOUND, 그 반대. */
        public PortMode drainingFrom() {
            return switch (this) {
                case SWITCHING_TO_INBOUND -> OUTBOUND;
                case SWITCHING_TO_OUTBOUND -> INBOUND;
                default -> this;
            };
        }
    }

    // =========================================================================
    // 6. 포트 라우팅 룰 타입 (tb_wcs_port_routing_rule.rule_type)
    // =========================================================================

    public enum PortRuleType implements EnumCode {

        INTERLEAVING("INTERLEAVING", "교차 제어 - 입/출고를 순차 교차하여 데드락 방지"),
        PARALLEL("PARALLEL", "동시 처리 - 전용 포트 위주 현장에서 입/출고 동시 처리");

        private final String code;
        private final String desc;

        PortRuleType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }
    }

    // =========================================================================
    // 7. 오더 타입 (tb_wcs_shuttle_order.order_type)
    // =========================================================================

    public enum OrderType implements EnumCode {

        INBOUND("INBOUND", "입고 - 물품을 창고의 빈 로케이션에 적재"),
        OUTBOUND("OUTBOUND", "출고 - 재고가 있는 로케이션에서 물품을 출고 포트로 이동"),
        MOVE("MOVE", "이동 - 재고를 한 로케이션에서 다른 로케이션으로 재배치"),
        PUTBACK("PUTBACK", "재입고 - host_order 전용 (shuttle_order 에는 INBOUND 사용)");

        private final String code;
        private final String desc;

        OrderType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() { return Collections.emptyList(); }

        public static OrderType from(Object code) {
            return EnumCodeUtil.fromCodeOrNull(OrderType.class, code,
                    true, OrderType::aliases);
        }
    }

    // =========================================================================
    // 7-1. 전역 운영 모드 (tb_wcs_system_mode.operation_mode)
    // =========================================================================

    public enum WcsOperationMode implements EnumCode {

        NORMAL("NORMAL", "정상"),
        INBOUND_PRIORITY("INBOUND_PRIORITY", "입고 우선"),
        OUTBOUND_PRIORITY("OUTBOUND_PRIORITY", "출고 우선"),
        RELOCATION("RELOCATION", "재배치"),
        MAINTENANCE("MAINTENANCE", "점검");

        private final String code;
        private final String desc;

        WcsOperationMode(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() { return List.of(); }

        public boolean allows(OrderType type) {
            return switch (this) {
                case NORMAL -> true;
                case INBOUND_PRIORITY ->
                        type == OrderType.INBOUND || type == OrderType.PUTBACK || type == OrderType.MOVE;
                case OUTBOUND_PRIORITY ->
                        type == OrderType.OUTBOUND || type == OrderType.PUTBACK || type == OrderType.MOVE;
                case RELOCATION -> type == OrderType.MOVE;
                case MAINTENANCE -> false;
            };
        }

        public static WcsOperationMode from(Object code) {
            return EnumCodeUtil.fromCodeOrNull(WcsOperationMode.class, code,
                    true, WcsOperationMode::aliases);
        }
    }

    // =========================================================================
    // 8. 로케이션 타입 (tb_wcs_loc_mst.loc_type)
    // =========================================================================

    public enum LocType implements EnumCode {

        RACK("RACK", true, "물리 랙 셀 — 재고 보관 위치, Lock 가능"),
        INBOUND_PORT("INBOUND_PORT", false, "입고 포트 — 컨베이어/입구, Lock 불가"),
        OUTBOUND_PORT("OUTBOUND_PORT", false, "출고 포트 — 컨베이어/출구, Lock 불가"),
        IN_OUTBOUND_PORT("IN_OUTBOUND_PORT", false, "입/출고 겸용 포트 — Lock 불가"),
        CHARGE_PORT("CHARGE_PORT", false, "충전 포트 — 셔틀 충전 위치, Lock 불가"),
        VIRTUAL("VIRTUAL", false, "가상 위치 — 시뮬레이션/테스트용, Lock 불가");

        private static final Set<String> NON_LOCKABLE_CODES = Set.of(
                "INBOUND_PORT", "OUTBOUND_PORT", "CHARGE_PORT", "VIRTUAL");

        private final String code;
        private final boolean lockable;
        private final String desc;

        LocType(String code, boolean lockable, String desc) {
            this.code = code;
            this.lockable = lockable;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        public boolean isLockable() { return lockable; }

        public static boolean isLockable(String locType) {
            if (locType == null || locType.isBlank()) return true;
            return !NON_LOCKABLE_CODES.contains(locType.toUpperCase());
        }
    }

    // =========================================================================
    // 9. 로케이션 상태 (tb_wcs_loc_mst.status)
    // =========================================================================

    public enum LocStatus implements EnumCode {

        NULL(null, "NULL"),
        EMPTY(0, "비어있음 - 재고 없음, 입고 가능"),
        OCCUPIED(10, "재고있음 - 재고 존재, 출고 가능"),
        LOCKED(20, "작업중 - 입출고 주문이 점유 중, 입출고 불가"),
        EMPTY_PICK(30, "공출고 감지 - 시스템 재고O 실물X, 수동 재고 삭제로 복구 필요"),
        DOUBLE_ENTRY(40, "이중입고 감지 - 실물O 시스템 재고X, 수동 재고 생성으로 복구 필요"),
        DISABLED(90, "사용불가 - 운영자 통제 상태, 입출고 불가");

        private final Integer code;
        private final String desc;

        LocStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public Integer code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() {
            return switch (this) {
                case NULL -> null;
                case EMPTY -> List.of("FREE", "AVAILABLE");
                case OCCUPIED -> List.of("FULL", "IN_USE");
                case LOCKED -> List.of("PROCESSING", "IN_PROGRESS");
                case EMPTY_PICK -> List.of("PHANTOM_STOCK", "EMPTY_PICK_ERROR");
                case DOUBLE_ENTRY -> List.of("GHOST_CARGO", "DOUBLE_ENTRY_ERROR");
                case DISABLED -> List.of("BLOCKED", "INACTIVE");
            };
        }

        public static LocStatus from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(LocStatus.class, codeOrAlias,
                    true, LocStatus::aliases);
        }
    }

    // =========================================================================
    // 10. WCS 에러 코드
    // =========================================================================

    public enum WcsError implements EnumCode {

        MISSING_REQUIRED_FIELD("ERR_REQUIRED", "필수 필드 누락 - 요청에 필수 데이터가 없음"),
        INVALID_ORDER_TYPE("ERR_ORDER_TYPE", "주문 유형 오류 - 지원하지 않는 orderType 값"),
        DUPLICATE_LINE("ERR_DUP_LINE", "중복 라인 번호 - 동일 주문 내 lineNo가 중복됨"),
        INSUFFICIENT_STOCK("ERR_STOCK", "재고 부족/없음 - 요청 수량보다 가용 재고가 적거나 없음"),
        NO_AVAILABLE_LOCATION("ERR_LOC", "가용 로케이션 없음 - 입고/이동 시 빈 로케이션을 찾을 수 없음"),
        ALLOCATION_FAILED("ERR_ALLOC", "할당 실패 - 로케이션/재고 할당 과정에서 실패"),
        ECS_SEND_FAILED("ERR_ECS", "ECS 전송 실패 - ECS로 명령 전송 중 오류 발생"),
        NO_AVAILABLE_STOCK("ERR_NO_AVAILABLE_STOCK", "가용 재고 없음 - 해당 SKU/LOT에 대해 출고 가능한 재고가 없음"),
        INVALID_REQUEST("ERR_BAD_REQUEST", "요청 형식 오류 - JSON 구조/필드 값이 요구사항과 다름"),
        INVALID_ORDER_ITEM("ERR_BAD_ITEM", "주문 아이템 오류 - item list/lineNo/qty 등 라인 검증 실패"),
        LOCATION_LOCKED("ERR_LOC_LOCKED", "로케이션 잠금 실패 - 다른 작업이 점유 중이거나 락 획득 실패"),
        ORDER_NOT_FOUND("ERR_ORDER_NOT_FOUND", "주문 미존재 - 해당 orderKey/order를 찾지 못함"),
        STOCK_RESERVATION_FAILED("ERR_RESERVE", "재고 예약 실패 - 재고 할당 또는 reservation 처리 실패"),
        INVALID_PARAMETER("ERR_INVALID_PARAM", "파라미터 오류 - 필수 값이 null이거나 형식이 잘못됨"),
        INTERNAL_ERROR("ERR_INTERNAL", "내부 오류 - 예상치 못한 시스템 오류"),

        // --- wcs-ops 플래그/게이팅/락/시험 전용 ---
        ALLOCATION_GATED("ERR_ALLOC_GATED", "산출 게이팅 차단"),
        OPERATION_MODE_BLOCKED("ERR_OP_MODE_BLOCKED", "운영 모드에 의해 차단"),
        SCHEDULED_DATE_NOT_REACHED("ERR_SCHEDULED", "scheduled_date 미도래"),
        PORT_DISPATCH_LOCKED("ERR_PORT_LOCKED", "포트 배차 락으로 인한 지연"),
        PORT_LOCK_RELEASE_FAILED("ERR_PORT_UNLOCK", "포트 락 강제 해제 실패"),
        INVALID_PORT_MODE_CHANGE("ERR_PORT_MODE_CHANGE", "포트 모드 전환 불가"),
        TEST_NOT_PASSED("ERR_TEST_NOT_PASSED", "시험 미통과 - 산출 진입 불가"),
        TEST_STATE_INVALID("ERR_TEST_STATE", "시험 상태 불일치 - 결과 수신 거부"),

        PORT_MODE_NOT_READY("ERR_PORT_MODE_NOT_READY", "포트 모드 불일치 - 현재 포트 모드로는 해당 주문 산출 불가");

        private final String code;
        private final String desc;

        WcsError(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() {
            return switch (this) {
                // 기존 INSPECTION_* 호출 코드 호환용 alias (서비스 코드 점진 이전 동안)
                case TEST_NOT_PASSED -> List.of("INSPECTION_NOT_PASSED");
                case TEST_STATE_INVALID -> List.of("INSPECTION_STATE_INVALID");
                default -> List.of();
            };
        }

        public static WcsError from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(WcsError.class, codeOrAlias,
                    true, WcsError::aliases);
        }
    }

    // =========================================================================
    // 11. 단위 타입 (UOM - Unit Of Measure)
    //     HOST → WCS 내부 환산의 기준. 재고/출고는 항상 EA로 통일.
    // =========================================================================
    public enum UomType implements EnumCode {

        EA("EA", "낱개 - 재고/출고 관리의 기준 단위"),
        BOX("BOX", "박스 - 입고 시 환산 필요 (itemMaster.boxQty)"),
        PLT("PLT", "팔레트 - 입고 시 환산 필요 (itemMaster.boxQty × palletQty)");

        private final String code;
        private final String desc;

        UomType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() {
            return switch (this) {
                case EA -> List.of("EACH", "PCS", "PIECE", "UNIT");
                case BOX -> List.of("CTN", "CARTON", "CASE");
                case PLT -> List.of("PALLET", "PL");
            };
        }

        public boolean isBase() { return this == EA; }

        public boolean needsConversion() { return this != EA; }

        public static UomType from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(UomType.class, codeOrAlias,
                    true, UomType::aliases);
        }

        /** null/빈값은 EA로 간주 — HOST가 UOM을 생략한 경우 기본값 */
        public static UomType fromOrDefault(Object codeOrAlias) {
            UomType result = from(codeOrAlias);
            return result != null ? result : EA;
        }
    }

    // =========================================================================
    // 12. 시험(Test) 진행 상태 (tb_wcs_host_order.test_status)
    //
    //     null = 시험 비대상
    //     REQUESTED → 시험 의뢰됨
    //     PASSED    → 시험 통과
    //     FAILED    → 시험 실패
    // =========================================================================

    public enum TestStatus implements EnumCode {

        REQUESTED("REQUESTED", "시험 의뢰됨 - 외부 시험 결과 대기 중"),
        PASSED("PASSED", "시험 통과 - 산출 진입 가능"),
        FAILED("FAILED", "시험 실패 - 사용자 확인 필요");

        private final String code;
        private final String desc;

        TestStatus(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public String code() { return code; }

        @Override
        public String desc() { return desc; }

        public List<String> aliases() { return List.of(); }

        public static TestStatus from(Object codeOrAlias) {
            return EnumCodeUtil.fromCodeOrNull(TestStatus.class, codeOrAlias,
                    true, TestStatus::aliases);
        }

        /** 대소문자 무관 비교 헬퍼. */
        public boolean matches(String value) {
            return value != null && code.equalsIgnoreCase(value.trim());
        }
    }
}