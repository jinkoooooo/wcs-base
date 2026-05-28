package operato.logis.ecs.base.ecs.domain.enums;

import java.util.Arrays;

// todo: 전체 검토
public class EcsDBConsts {

    public enum DominaId {
        defaultId(7L,"kmat2026");

        private final long value;
        private final String description;

        DominaId(long value, String description) {
            this.value = value;
            this.description = description;
        }

        public long getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum EcsIfStatus {
        READY(20,"지시 수신 대상"),
        RECEIVE(30,"지시 수신 완료"),
        RACK_IN_MOVE_COMPLETE(40,"이송지시 랙단 이동 완료"),
        COMPLETE(50,"완료보고");

        private final int value;
        private final String description;

        EcsIfStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }

    // TODO : 현장별 우선순위 정의
    // 검토중) 사용확인
    public enum OrderPriority {
        MOVE_CAR_FLOOR(4, "셔틀카 층간 이송"),
        MOVE_HOME(3, "홈포지션이동"),
        NORMAL(5,"기본"); // 사용

        private final int value;
        private final String description;

        OrderPriority(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }

    // 검토완료)
    public enum OrderStatus {
        READY(0, "대기"),
        EQ_SEND(1, "설비 지시 전송"),
        WORKING(2, "작업중"),
        COMPLETE(9, "완료"),

        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        OrderStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.OrderStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토중) 사용 확인
    public enum OrderType {
        MOVE(11, "MOVE","이동"),

        INBOUND(21, "INBOUND", "입고 이송"), // 사용
        OUTBOUND(22, "OUTBOUND","출고 이송"), // 사용
        RACK_TO_RACK(23, "RACK_TO_RACK","Rack To Rack 이송"),
        STATION_TO_STATION(24, "STATION_TO_STATION","Station To Station 이송"),

        DESTINATION_CHANGE(31, "DESTINATION_CHANGE","목적지 변경"),

        //INBOUND(11,"INBOUND", "입고"),
        //OUTBOUND(12, "OUTBOUND", "출고"),
        //MOVE(13, "MOVE", "재고이동"),
        MOVE_HOME(14, "MOVE_HOME", "홈(여유버퍼) 포지션 이동"),
        //MOVE_CA?R_FLOOR(15, "MOVE_CAR_FLOOR", "셔틀카 층간 이송"),
        //
        UNKNOWN(000, "알 수 없음", "알 수 없음");

        private final int value;
        private final String strVvalue;
        private final String description;

        OrderType(int value, String strVvalue, String description) {
            this.value = value;
            this.strVvalue = strVvalue;
            this.description = description;
        }

        public int getValue() {
            return value;
        }
        public String getStrValue() {
            return strVvalue;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.OrderType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }

        public static EcsDBConsts.OrderType find(String strValue) {
            return Arrays.stream(values())
                    .filter(v -> v.getStrValue().equals(strValue))
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum EqGroupType{
        SHUTTLE_RACK_4WAY(11, "4way 셔틀 랙"),
        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EqGroupType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.EqGroupType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토) CONVEYOR 사용 확인
    public enum EqType{
        RACK(11, "보관설비"),
        CONVEYOR(21, "이송설비"),
        SHUTTLE_CAR(22, "셔틀 카"),
        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EqType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.EqType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토중) 랙 유형
    public enum RackType{
        CELL(11, "셀"),
        INBOUND_PORT(21, "입고포트"),
        OUTBOUND_PORT(22, "출고포트"),
        IN_OUTBOUND_PORT(23, "입출고포트"),
        CHARGE_ENTER_PORT(32, "출정진입포트"),
        BAN_CELL(41, "영구 금지 셀"), // 사용
        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        RackType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.RackType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토중) 사용확인
    public enum ConveyorType{
        GROUND(1, "지상컨베이어"),
        INBOUND(2, "입고대컨베이어"),
        OUTBOUND(3, "출고대컨베이어"),
        IN_OUTBOUND(4, "입출고대컨베이어"),
        LIFT(11, "리프트컨베이어"),
        //RACK_IN(12, "랙단 컨베이어"), // 사용
        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        ConveyorType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.ConveyorType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토중) 사용 확인
    public enum EcsRackOrderCmdStatus {
        READY(0, "대기"),
        MOVE(11, "일반이송"),
        LOAD_MOVE(12, "로드이송"),
        UNLOAD_MOVE(13, "언로드이송"),
        LOAD(14, "로드"),
        UNLOAD(15, "언로드"),
        MOVE_HOME(31, "홈 이동"),
        MOVE_CAR_FROM_RACK_CV(41, "셔틀카 출발지 랙단 이송"),
        MOVE_CAR_LIFT_CV(42, "셔틀카 목적지 랙단 이송"),
        MOVE_CAR_LIFT_MOVE(43, "셔틀카 목적지 랙단 이송"),
        MOVE_CAR_TO_RACK_CV(44, "셔틀카 목적지 랙단 이송"),
        // --------------------------------------------------
        //MOVE(11, "이동"),

        INBOUND(21, "입고 이송"),
        OUTBOUND(22, "출고 이송"),
        RACK_TO_RACK(23, "Rack To Rack 이송"),
        STATION_TO_STATION(24, "Station To Station 이송"),

        DESTINATION_CHANGE(31, "목적지 변경"),
        // --------------------------------------------------
        COMPLETE(99, "완료"),
        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EcsRackOrderCmdStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.EcsRackOrderCmdStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토 중) 사용확인 / STATION_READY
    public enum EcsRouteOrderCmdStatus {
        READY(0, "대기"),
        INBOUND_READY(11, "입고 대기"),
        LIFT_MOVE(12, "리프트 이송"),
        STATION_READY(13, "입출고대 대기"),

        COMPLETE(9, "완료"),
        // ---------------------------------

        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EcsRouteOrderCmdStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.EcsRouteOrderCmdStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토) 사용 확인 / READY, RUN
    public enum EqCraneStatus {
        READY(0, "대기"),
        RESERVE(1, "설비 예약"),
        RUN(2, "작업중"),
        EMR_STOP(5, "비상정지"),
        ERROR(8, "에러"),
        COMPLETE(9, "완료"),

        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EqCraneStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.EqCraneStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    // 검토중) 사용확인 / 랙 상태 관리
    public enum EqRackStatus {
        READY(0, "대기"), // 사용
        MOVE_RESERVE(1, "주행 예약"),
        CARGO_RESERVE(2, "화물 예약"), // 사용
        CARGO(5, "화물 적재"), // 사용
        CAR(6, "카 위치"),

        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EqRackStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.EqRackStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum EqConveyorStatus {
        READY(0, "대기"),
        MOVE_RESERVE(1, "주행 예약"),

        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EqConveyorStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.EqConveyorStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum PlcEqType {
        SHUTTLE_CAR(1, "셔틀 카 별 PLC"),
        CONVEYOR_AND_LIFT(2, "컨베이어, 리프트, 랙단컨베이어 지상반 PLC"),
        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        PlcEqType(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EcsDBConsts.PlcEqType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }
}