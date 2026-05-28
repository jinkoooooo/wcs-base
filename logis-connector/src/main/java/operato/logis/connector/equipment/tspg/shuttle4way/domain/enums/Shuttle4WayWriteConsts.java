
package operato.logis.connector.equipment.tspg.shuttle4way.domain.enums;


import operato.logis.connector.plc.PlcBitEnum;

import java.util.Arrays;

public class Shuttle4WayWriteConsts {
    public enum ShuttleWriteAddress {
        WORK_ID(1051, "작업번호 - 1001~9999"),
        WORK_TYPE(1052, "작업유형"),
        EMERGENCY_STOP(1053, "비상정지(즉시정지), 일시정지, 작업삭제"),
        CLEAR_OPTION(1054, "초기화 옵션"),
        TO_CELL(1055, "도착위치, 작업위치정보"),
        CURRENT_FLOOR(1056, "현재층위치"),

        UNKNOWN(000, "알 수 없음");

        private final int address;
        private final String description;

        ShuttleWriteAddress(int address, String description) {
            this.address = address;
            this.description = description;
        }

        public int getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public static ShuttleWriteAddress find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getAddress() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ShuttleWorkType implements PlcBitEnum {
        MOVE(0, "이동"),
        LOAD(1, "로드"),
        UNLOAD(2, "언로드"),
        UNKNOWN(000, "알 수 없음");

        private final int bitIndex;
        private final String description;

        ShuttleWorkType(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() {
            return bitIndex;
        }
        @Override
        public String getDescription() {
            return description;
        }
        public static ShuttleWorkType find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ShuttleDirection {
        GO(1, "앞"),
        BACK(2, "뒤"),
        LEFT(3, "왼쪽"),
        RIGHT(4, "오른쪽"),
        UNKNOWN(99, "알 수 없음");
        private final int value;
        private final String description;
        ShuttleDirection(int value, String description) {
            this.value = value;
            this.description = description;
        }
        public int getValue() { return value; }
        public String getDescription() { return description; }

        public static ShuttleDirection find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }

        public static ShuttleDirection getDirection(int x1, int y1, int x2, int y2) {
            if (x2 == x1 + 1) return RIGHT;
            if (x2 == x1 - 1) return LEFT;
            if (y2 == y1 + 1) return GO;
            if (y2 == y1 - 1) return BACK;
            throw new IllegalArgumentException("Invalid move");
        }
    }



    public enum ShuttleEmergencyOption implements PlcBitEnum {
        EMO_STOP(0, "비상정지, 즉시정지"),
        STOP(1, "일시정지, 정위치 정지"),
        DELETE_WORK(2, "작업삭제"),
        UNKNOWN(99, "UNKNOWN");
        private final int bitIndex;
        private final String description;
        ShuttleEmergencyOption(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }

        public static ShuttleEmergencyOption find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ShuttleClearOption implements PlcBitEnum {
        CLEAR_COMP(0, "완료리셋"),
        CLEAR_ERROR(1, "에러리셋"),
        UNKNOWN(99, "UNKNOWN");
        private final int bitIndex;
        private final String description;
        ShuttleClearOption(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }
        @Override
        public int getBitIndex() { return bitIndex; }
        @Override
        public String getDescription() { return description; }

        public static ShuttleClearOption find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ShuttleDistance implements PlcBitEnum {
        CM150(0, "1.5M 이하"),
        CM150190(1, "1.5~1.9M"),
        CM190(2, "1.9M 이상"),
        UNKNOWN(99, "알 수 없음");

        private final int bitIndex;
        private final String description;

        ShuttleDistance(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() {
            return bitIndex;
        }

        @Override
        public String getDescription() {
            return description;
        }
        public static ShuttleDistance find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

}
