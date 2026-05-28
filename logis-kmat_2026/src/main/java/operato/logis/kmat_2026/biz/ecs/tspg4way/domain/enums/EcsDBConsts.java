package operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums;

import java.util.Arrays;

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
    public enum OrderType {
        INBOUND(11,"INBOUND", "입고"),
        OUTBOUND(12, "OUTBOUND", "출고"),
        MOVE(13, "MOVE", "재고이동"),
        CHARGE(21, "CHARGE", "충전"),

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

        public static EqGroupType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

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

        public static EqType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }


    public enum RackType{
        CELL(11, "셀"),
        INBOUND_PORT(21, "입고포트"),
        OUTBOUND_PORT(22, "출고포트"),
        IN_OUTBOUND_PORT(23, "입출고포트"), // TODO 전시회때 전시회 기준으로 입출고포트 설정 필요.
        CHARGE_PORT(31, "충전포트"),
        CHARGE_ENTER_PORT(32, "출정진입포트"),
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

        public static RackType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ConveyorType{
        GROUND(1, "지상컨베이어"),
        INBOUND(2, "입고대컨베이어"),
        OUTBOUND(3, "출고대컨베이어"),
        IN_OUTBOUND(4, "입출고대컨베이어"),
        LIFT(11, "리프트컨베이어"),
        RACK_IN(12, "랙단 컨베이어"),
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

        public static ConveyorType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }


    public enum EcsRackOrderCmdStatus {
        READY(0, "대기"),
        MOVE(11, "일반이송"),
        LOAD_MOVE(12, "로드이송"),
        UNLOAD_MOVE(13, "언로드이송"),
        LOAD(14, "로드"),
        UNLOAD(15, "언로드"),
        CHARGE_MOVE(21, "충전이송"),
        CHARGE(22, "충전"),

        COMPLETE(9, "완료"),
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

        public static EcsRackOrderCmdStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }


    public enum EcsRouteOrderCmdStatus {
        READY(0, "대기"),
        INBOUND_READY(11, "입고 대기"),
        LIFT_MOVE(12, "리프트 이송"),
        RACK_CV_READY(13, "랙단컨베이어 대기"),

        COMPLETE(9, "완료"),
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

        public static EcsRouteOrderCmdStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }


    public enum EqCarStatus {
        READY(0, "대기"),
        RESERVE(1, "설비 예약"),
        RUN(2, "작업중"),
        EMR_STOP(5, "비상정지"),
        ERROR(8, "에러"),
        COMPLETE(9, "완료"),

        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EqCarStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EqCarStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }
    public enum EqCarBatteryStatus {
        CAN_MOVE(0, "작업가능"),
        NEED_CHARGE(1, "충전필요"),
        CHARGING(2, "충전중"),
        COMPLETE_CHARGE(9, "충전완료"),

        UNKNOWN(000, "알 수 없음");

        private final int value;
        private final String description;

        EqCarBatteryStatus(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static EqCarBatteryStatus find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum EqRackStatus {
        READY(0, "대기"),
        MOVE_RESERVE(1, "주행 예약"),
        CARGO_RESERVE(2, "화물 예약"),
        CHARGE_RESERVE(3, "충전 예약"),
        CARGO(5, "화물 적재"),

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

        public static EqRackStatus find(int value) {
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

        public static EqConveyorStatus find(int value) {
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

        public static PlcEqType find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

}
