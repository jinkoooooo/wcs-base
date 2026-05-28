package operato.logis.connector.equipment.tspg.shuttle4way.domain.enums;


import operato.logis.connector.plc.PlcBitEnum;

import java.util.Arrays;

public class Shuttle4WayReadConsts {

    public enum ShuttleReadAddress {
        MODE(801, "동작모드"),
        WORKING_STATUS(802, "작업유무"),
        RUN_STATUS(803, "운전상태"),
        WORK_ID(804, "작업번호 - ECS 지시 받은 오더 ID"),
        INTER_LOCK(805, "인터록 - 셔틀 지시 수행 가능한지"),
        CHARGE_STATUS(806, "충전상태"),
        FLOOR_INFO(807, "현재 층위치"),
        LOCATION(809, "주행위치 - 마지막 인식 QR 코드"),
        LOCATION2(810, "주행위치"),
        CELL_LOCATION(811, "현재위치 CELL "),
        COMPLETE_FLAG(812, "완료플래그"),
        COMPLETE_WORK_ID(813, "완료작업번호 - 완료된 작업 번호"),
        BLOCK_CODE(814, "에러코드 - 0: 정상, 그 외: 장애/이상 코드"),

        CARGO_STATUS(815, "적재 상태"),
        CENSER_STATUS(820, "센서 상태"),

        UNKNOWN(000, "알 수 없음");

        private final int address;
        private final String description;

        ShuttleReadAddress(int address, String description) {
            this.address = address;
            this.description = description;
        }

        public int getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public static ShuttleReadAddress find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getAddress() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ShuttleMode implements PlcBitEnum {
        MANUAL(0, "수동"),
        AUTO(1, "자동"),
        UNKNOWN(99, "알 수 없음");
        private final int bitIndex;
        private final String description;
        ShuttleMode(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }

    public enum ShuttleRunStatus implements PlcBitEnum  {
        STOP(0, "STOP"),
        RUN(1, "RUN"),
        EMG(2, "비상정지"),
        UNKNOWN(99, "알 수 없음");

        private final int bitIndex;
        private final String description;

        ShuttleRunStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }

    public enum ShuttleChargeStatus implements PlcBitEnum {
        CAN_WORK(0, "작업가능"),
        NEED_CHARGE(1, "충전필요"),
        CHARGING(2, "충전중"),
        UNKNOWN(99, "알 수 없음");
        private final int bitIndex;
        private final String description;
        ShuttleChargeStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }
    public enum ShuttleWorkStep implements PlcBitEnum {
        READY(0, "READY"),
        STARTED(1, "STARTED"),
        COMPLETE(2, "COMP"),
        NG(3, "NG"),
        UNKNOWN(99, "알 수 없음");

        private final int bitIndex;
        private final String description;

        ShuttleWorkStep(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }

    }



    public enum ShuttleInterLock implements PlcBitEnum {
        INTERLOCK(0, "0: 정상, 1: 인터록 (명령 차단)");
        private final int bitIndex;
        private final String description;
        ShuttleInterLock(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }



    public enum ShuttleWorkingStatus implements PlcBitEnum {
        WORKING(0, "작업유무 - 0: 작업무, 1: 작업유");
        private final int bitIndex;
        private final String description;
        ShuttleWorkingStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }


    public enum ShuttleCompleteFlag implements PlcBitEnum {
        COMPLETE(0, "완료플래그 - 0: 대기/작업중, 1: 이번 지령 완료");
        private final int bitIndex;
        private final String description;
        ShuttleCompleteFlag(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }


    public enum ShuttleErrorCode {
        NORMAL(0, "정상"),
        SHUTTLE_TRAVEL_CENSOR_ERROR(1, "충돌 감지 이상"),
        SHUTTLE_DUPLICATE_ERROR(2, "이중입고 이상"),
        SHUTTLE_CARGO_SENSOR_ERROR(3, "화물감지 이상"),
        SHUTTLE_TOP_PLATE_SENSOR_ERROR(4, "SHUTTLE 상판감지 이상"),
        SHUTTLE_PGV_SENSOR_ERROR(5, "SHUTTLE PGV 센서이상 (QR 리더기)"),
        SHUTTLE_SERVO_ERROR(6, "SHUTTLE servo driver 이상"),
        SHUTTLE_MOVE_RANGE_ERROR(7, "SHUTTLE 이동범위 이상"),
        SHUTTLE_QR_NO_READ_ERROR(8, "SHUTTLE NO READ 순서 지나침이상"),
        SHUTTLE_QR_NO_READ_ERROR2(9, "SHUTTLE NO READ 시간내 못 읽음 이상"),
        SHUTTLE_RACK_CV_ERROR(9, "SHUTTLE 랙단 컨베어 SAFETY 이상 감지"),

        SHUTTLE_OUTBOUND_EMPTY_ERROR(11, "SHUTTLE 공출고 감지"),
        SHUTTLE_TRAVEL_TIMER_OVER(12, "SHUTTLE 상승하강 이상 Timer Over"),

        SHUTTLE_EMERGENCY_STOP_ERROR(99, "SHUTTLE 비상정지 이상"),
        UNKNOWN(999, "알 수 없음");

        private final int value;
        private final String description;

        ShuttleErrorCode(int address, String description) {
            this.value = address;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static ShuttleErrorCode find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ShuttleCargoStatus implements PlcBitEnum {
        CARGO(0, "적재 상태 - 0: 없음, 1: 적재");
        private final int bitIndex;
        private final String description;
        ShuttleCargoStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }
    }

    public enum ShuttleCenSerStatus implements PlcBitEnum {
        OBSTACLE_FRONT(8, "장애물(전방) - 0: 없음, 1: 감지됨"),
        OBSTACLE_BACK(9, "장애물(후방) - 0: 없음, 1: 감지됨"),
        CARGO_COLLAPSE_FRONT(10, "공출고 - 0: 없음, 1: 감지됨"),
        DUPLICATE_INBOUND_LEFT(12, "이중입고 - 0: 없음, 1: 감지됨"),
        WORKTOP_SENSOR(14, "상판 감지 - 0: 없음, 1: 감지됨");

        private final int bitIndex;
        private final String description;

        ShuttleCenSerStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }

    }

}
