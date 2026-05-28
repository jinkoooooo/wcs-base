package operato.logis.connector.equipment.tspg.shuttle4way.domain.enums;

import operato.logis.connector.plc.PlcBitEnum;

import java.util.Arrays;

public class ConveyorWriteConsts {
    public enum ConveyorCommonWriteAddress{
        LIFT_CAR_MOVE_MODE(5,"단이동 모드 0:기본, 1단이동모드 "),
        LIFT_CAR_MOVE_TO_LEVEL(6,"단이동 목적지"),
        UNKNOWN(000, "알 수 없음");
        private final int address;
        private final String description;

        ConveyorCommonWriteAddress(int address, String description) {
            this.address = address;
            this.description = description;
        }

        public int getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public static ConveyorCommonWriteAddress find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getAddress() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

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
        INBOUND(0, "입고"),
        OUTBOUND(1, "출고"),
        CAR_MOVE(2, "셔틀 카 이동"),
        MOVE(3, "재고 이동"),
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


    public enum ConveyorLiftMoveMode implements PlcBitEnum {
        SHUTTLE_CAR_MOVE(0, "셔틀 단이동"),
        CARGO_MOVE(1, "재고 단이동"),
        UNKNOWN(000, "알 수 없음");

        private final int bitIndex;
        private final String description;

        ConveyorLiftMoveMode(int bitIndex, String description) {
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
        public static ConveyorLiftMoveMode find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getBitIndex() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }
}
