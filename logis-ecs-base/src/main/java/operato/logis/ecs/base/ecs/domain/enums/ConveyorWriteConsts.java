package operato.logis.ecs.base.ecs.domain.enums;

import operato.logis.connector.plc.PlcBitEnum;

import java.util.Arrays;

//TODO: PLC MAP에 맞게 수정 필요
public class ConveyorWriteConsts {
    public enum ConveyorWriteAddress {
        WORK_ID(0, "작업번호 - 1001~9999"),
        WORK_TYPE(1, "작업타입 - 입고/출고/셔틀카 이동 등 "),
        FROM_LOCATION(2, "출발지"),
        TO_LOCATION(3, "목적지"),
        TO_CELL(4, "사이즈체커 명령"),

        UNKNOWN(000, "알 수 없음");

        private final int address;
        private final String description;

        ConveyorWriteAddress(int address, String description) {
            this.address = address;
            this.description = description;
        }

        public int getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public static ConveyorWriteAddress find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getAddress() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum ConveyorWorkType implements PlcBitEnum {
        INBOUND(0, "이동"),
        OUTBOUND(1, "로드"),
        MOVE(2, "언로드"),
        UNKNOWN(000, "알 수 없음");

        private final int bitIndex;
        private final String description;

        ConveyorWorkType(int bitIndex, String description) {
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
        public static ConveyorWorkType find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }
}
