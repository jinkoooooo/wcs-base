package operato.logis.connector.equipment.tspg.shuttle4way.domain.enums;

import operato.logis.connector.plc.PlcBitEnum;

import java.util.Arrays;

public class ConveyorReadConsts {

    public enum ConveyorReadAddress {
        CONVEYOR_STATUS(5, "컨베이어 상태"),
        SIZE_STATUS(7, "사이즈체커 상태"),
        LIFT_STATUS(8, "리프트 위치"),
        ERROR_CODE(9, "에러코드"),

        UNKNOWN(000, "알 수 없음");

        private final int address;
        private final String description;

        ConveyorReadAddress(int address, String description) {
            this.address = address;
            this.description = description;
        }

        public int getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public static ConveyorReadAddress find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getAddress() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }
    public enum ConveyorStatus implements PlcBitEnum {
        TRACK_AUTO(0, "트랙 자동/수동 0: MANUAL , 1: AUTO"),
        CARGO_STATUS(1, "화물 유/무 0: 무, 1:유"),
        DATA_STATUS(2, "데이터 유/무 0: 무, 1:유"),
        DIVERTER_STATUS(3, "디버터 UP/DOWN  0: DOWN, 1:UP"),
        STOPPER_STATUS(4, "스토퍼 열림/막힘   0: 막힘, 1:열림"),
        MOTOR_RUN(8, "모터 구동 유/무  0: STOP, 1:START"),
        AUTO_LABEL_PRINT(9, "오토라벨프린터 요구 0: , 1:부착요구"),
        SIZE_CHECKER_ERROR(14, "사이즈체커 에러"),
        TRACK_ERROR(15, "트랙에러");

        private final int bitIndex;
        private final String description;

        ConveyorStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }

    public enum SizeCheckerStatus implements PlcBitEnum {
        FRONT(0, "전방"),
        BACK(1, "후방"),
        RIGHT(2, "우측"),
        LEFT(3, "좌측"),
        TOP(4, "상부");

        private final int bitIndex;
        private final String description;

        SizeCheckerStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }

    public enum ConveyorErrorCode {
        MOTOR_OVERLOAD_INV_FAULT(100, "Motor Over Load (INV Fault)"),
        CV_MOVING_TIME_OVER(101, "CV Moving Time Over"),
        DIV_UP_DOWN_TIME_OVER(102, "DIV UP/DN Time Over"),

        STOPPER_UP_DOWN_TIME_OVER(104, "Stopper UP/DN Time Over"),
        SIZE_CHECK_ERROR(105, "SIZE CHECK ERROR"),
        PALLET_EXISTENCE_DATA_NONE(106, "Pallet Existence, Data 없음"),
        PALLET_NONE_DATA_EXISTENCE(107, "Pallet 없음, Data Existence"),
        TRACKING_BCR_MISMATCH(108, "Tracking Data & BCR Read Data 불일치"),

        CENTERING_OPERATION_ERROR(110, "Centering 동작 Error"),

        DISPENSER_UP_DOWN_ERROR(112, "Dispenser UP/DN Error"),
        DISPENSER_CLAMP_ERROR(113, "Dispenser Clamp Error"),

        CV_SAFETY_DETECTION_ERROR(117, "CV Safety 감지 Error"),
        HEAT_DISSIPATION_DOOR_OPEN_ERROR(118, "방열 도어 OPEN ERROR"),
        HEAT_DISSIPATION_DOOR_CLOSE_ERROR(119, "방열 도어 CLOSE ERROR"),
        LIFTER_INVERTER_FAULT(120, "Lifter Inverter Fault"),

        LIFT_POSITION_ERROR(122, "Lift 정위치 이상"),
        LIFTER_STOPPER_OPEN_CLOSE_ERROR(123, "Lifter Stopper 열림/닫힘 이상"),

        LIFT_CV_SAFETY_DETECTION_ERROR(127, "LIFT CV Safety 감지 Error"),
        LIMIT_SAFETY_SENSOR_DETECTED(128, "한계 안전센서 감지됨"),
        LIFT_PARITY_CHECK_ERROR(129, "LIFT PARITY CHECK ERROR"),

        PLC_CPU_ERROR(1001, "PLC CPU 이상"),
        PLC_BATTERY_LOW(1003, "PLC 밧데리 저하"),
        PLC_COMMUNICATION_CARD_ERROR(1004, "PLC 통신카드 이상"),
        PC_ONLINE_ERROR(1005, "PC On Line 이상"),

        MAIN_EMG(1011, "Main EMG"),
        OP1_EMG(1012, "OP1 EMG"),
        OP2_EMG(1013, "OP2 EMG"),
        OP3_EMG(1014, "OP3 EMG"),

        UNKNOWN(9999, "알 수 없음");

        private final int value;
        private final String description;

        ConveyorErrorCode(int address, String description) {
            this.value = address;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static ConveyorErrorCode find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }
}
