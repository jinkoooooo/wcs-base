
package operato.logis.ecs.base.ecs.domain.enums;


import operato.logis.connector.plc.PlcBitEnum;

import java.util.Arrays;

/*
Stacker Crane PLC Write 시 사용할 enum
- TODO: PLC MAP에 맞게 수정 필요
 */
public class StackerCraneWriteConsts {
    public enum StackerCraneWriteAddress {
        WORK_ID(101, "작업번호 - 1001~9999"), // TODO: 작업번호 범위 확인

        WORK_TYPE(103, "작업유형"),

        FROM_STATION(104, "From Station 번호 - 0~9"),
        FROM_ROW(105, "From Asiel 번호 - 1,2(좌측), 3,4(우측)"),
        FROM_BAY(106, "From Bay 번호 - 000~999"),
        FROM_LEVEL(107, "From Level 번호 - 00~99"),

        TO_STATION(110, "To Station 번호 - 0~9"),
        TO_ROW(111, "To Asiel 번호 - 1,2(좌측), 3,4(우측)"),
        TO_BAY(112, "To Bay 번호 - 000~999"),
        TO_LEVEL(113, "To Level 번호 - 00~99"),

        CLEAR_OPTION(127, "heartbeat, 홈복귀, 이상리셋, 전체 작업 삭제, S/C Online 시작"),
        HEARTBEAT(128, "Heartbeat"),
        ST1_STATION_WORK_ID(129, "ST1 입/출고대 현재 작업번호"),
        ST2_STATION_WORK_ID(130, "ST2 입/출고대 현재 작업번호"),
        ST3_STATION_WORK_ID(131, "ST3 입/출고대 현재 작업번호"),

        UNKNOWN(000, "알 수 없음");

        private final int address;
        private final String description;

        StackerCraneWriteAddress(int address, String description) {
            this.address = address;
            this.description = description;
        }

        public int getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public static StackerCraneWriteAddress find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getAddress() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum StackerCraneWorkType implements PlcBitEnum {
        MOVE(0, "이동"),
        PUTAWAY(1, "입고"),
        PICKING(2, "출고"),
        RACK_TO_RACK(2, "Rack to Rack"),
        DESTINATION_CHANGE(2, "목적지 변경(Cell)"),
        STATION_TO_STATION(2, "Station to Station"),
        UNKNOWN(000, "알 수 없음");

        private final int bitIndex;
        private final String description;

        StackerCraneWorkType(int bitIndex, String description) {
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

        public static StackerCraneWorkType find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum StackerCraneAsielDirection {
        CENTER(0, "정위치"),
        LEFT1(1, "왼쪽1"),
        LEFT2(2, "왼쪽2"),
        RIGHT1(3, "오른쪽1"),
        RIGHT2(4, "오른쪽2"),
        UNKNOWN(99, "알 수 없음");
        private final int value;
        private final String description;

        StackerCraneAsielDirection(int value, String description) {
            this.value = value;
            this.description = description;
        }

        public int getValue() { return value; }

        public String getDescription() { return description; }

        public static StackerCraneAsielDirection find(int value) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }

        public static StackerCraneAsielDirection getDirection(int craneNo, int x1, int y1, int x2, int y2) {

            // todo: 방향 수정

            if (x1 == x2) {
                return CENTER;
            }

            if (craneNo == 1) {
                if (x2 == x1 + 1) return RIGHT1;
                if (x2 == x1 - 1) return LEFT1;
            } else if (craneNo == 2) {
                if (x2 == x1 + 1) return RIGHT2;
                if (x2 == x1 - 1) return LEFT2;
            }
            throw new IllegalArgumentException("Invalid move");
        }
    }

    public enum StackerCraneClearOption implements PlcBitEnum {
        HEART_BEAT(0, "Heart beat"),
        CLEAR_RETURN(1, "홈 복귀"),
        ERROR_RESET(2, "알람 리셋"),
        CLEAR_ALL_WORKS(3, "전체 작업 삭제"),
        CLEAR_FORK1_WORK(4, "Fork#1 작업 삭제"),
        CLEAR_FORK2_WORK(5, "Fork#2 작업 삭제"),
        START_SC_ONLINE(6, "S/C Online 전환"),
        UNKNOWN(99, "UNKNOWN");

        private final int bitIndex;
        private final String description;

        StackerCraneClearOption(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }

        @Override
        public String getDescription() { return description; }

        public static StackerCraneClearOption find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }
}
