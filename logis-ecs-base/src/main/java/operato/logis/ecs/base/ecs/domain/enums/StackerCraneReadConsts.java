package operato.logis.ecs.base.ecs.domain.enums;

import operato.logis.connector.plc.PlcBitEnum;

import java.util.Arrays;

// TODO: PLC MAP에 맞게 수정 필요
public class StackerCraneReadConsts {

    public enum StackerCraneReadAddress {
        MODE(151,
                "0: Fork#1 화물 유무, " +
                        "1: Fork#1 작업 유무, " +
                        "2: Fork#2 화물 유무, " +
                        "3: Fork#2 작업 유무, " +
                        "4: Fork#1 작업 완료, " +
                        "5: Fork#2 작업 완료, " +
                        "6: Crane 운전 여부, " +
                        "7: -, " +
                        "8: Crane Online 여부, " +
                        "9: Crane 작업 요청 여부, " +
                        "A: Crane 에러 발생 여부, " +
                        "B: Recoverable 에러, " +
                        "C: Crane 홈위치 상태"),
        ERROR_CODE(152, "Crane 에러 코드"),
        ROW_POSITION(155, "Fork#1 기준 Crane Row 현위치"),
        BAY_POSITION(156, "Fork#1 기준 Crane Bay 현위치"),
        LEVEL_POSITION(157, "Fork#1 기준 Crane Level 현위치"),
        POSITION_STATUS(159, "" +
                "0: Fork#1 좌 정위치, " +
                "1: Fork#1 좌 이동중, " +
                "2: Fork#1 중심 정위치, " +
                "3: Fork#1 우 정위치, " +
                "4: Fork#1 우 이동중, " +
                "8: Fork#2 좌 정위치, " +
                "9: Fork#2 좌 이동중, " +
                "A: Fork#2 중심 정위치, " +
                "B: Fork#2 우 정위치, " +
                "C: Fork#2 우 이동중"),
        OPERATION_STATUS(160, "" +
                         "0: 주행 운전 중, " +
                         "1: 승강 운전 중, " +
                         "2: Fork#1 운전 중, " +
                         "3: Fork#2 운전 중"),
        STATUS_OPTION(161, "" +
                         "0: S/C 이상, " +
                         "1: S/C 반자동 S/W, " +
                         "2: S/C Online S/W, " +
                         "3: S/C 수동 S/W"),
        UNKNOWN(000, "알 수 없음");

        private final int address;
        private final String description;

        StackerCraneReadAddress(int address, String description) {
            this.address = address;
            this.description = description;
        }

        public int getAddress() {
            return address;
        }

        public String getDescription() {
            return description;
        }

        public static StackerCraneReadAddress find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getAddress() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum StackerCraneMode implements PlcBitEnum {
        FORK1_STOCK_EXIST(0, "Fork#1 화물 유무"),
        FORK1_TASK_EXIST(1, "Fork#1 작업 유무"),
        FORK2_STOCK_EXIST(2, "Fork#2 화물 유무"),
        FORK2_TASK_EXIST(3, "Fork#2 작업 유무"),
        FORK1_TASK_COMPLETE(4, "Fork#1 작업 완료"),
        FORK2_TASK_COMPLETE(5, "Fork#2 작업 완료"),
        CRANE_DRIVE_ENABLED(6, "Crane 운전 여부"),
        RESERVED(7, "-"),
        CRANE_ONLINE(8, "Crane Online 여부"),
        CRANE_TASK_REQUEST(9, "Crane 작업 요청 여부"),
        CRANE_ERROR(10, "Crane 에러 발생 여부"),
        RECOVERABLE_ERROR(11, "Recoverable 에러"),
        CRANE_HOME_POSITION(12, "Crane 홈위치 상태"),
        UNKNOWN(99, "알 수 없음");

        private final int bitIndex;
        private final String description;

        StackerCraneMode(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }

        @Override
        public String getDescription() { return description; }
    }

    public enum StackerCranePositionStatus implements PlcBitEnum {

        FORK1_LEFT_IN_POSITION(0, "Fork#1 좌 정위치"),
        FORK1_LEFT_MOVING(1, "Fork#1 좌 이동중"),
        FORK1_CENTER_IN_POSITION(2, "Fork#1 중심 정위치"),
        FORK1_RIGHT_IN_POSITION(3, "Fork#1 우 정위치"),
        FORK1_RIGHT_MOVING(4, "Fork#1 우 이동중"),

        FORK2_LEFT_IN_POSITION(8, "Fork#2 좌 정위치"),
        FORK2_LEFT_MOVING(9, "Fork#2 좌 이동중"),
        FORK2_CENTER_IN_POSITION(10, "Fork#2 중심 정위치"),
        FORK2_RIGHT_IN_POSITION(11, "Fork#2 우 정위치"),
        FORK2_RIGHT_MOVING(12, "Fork#2 우 이동중"),

        UNKNOWN(99, "알 수 없음");

        private final int bitIndex;
        private final String description;

        StackerCranePositionStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }

        @Override
        public String getDescription() { return description; }
    }

    public enum StackerCraneOperationStatus implements PlcBitEnum {
        TRAVELING(0, "주행 운전 중"),
        LIFTING(1, "승강 운전 중"),
        FORK1_MOVING(2, "Fork#1 운전 중"),
        FORK2_MOVING(3, "Fork#2 운전 중"),
        UNKNOWN(99, "알 수 없음");

        private final int bitIndex;
        private final String description;

        StackerCraneOperationStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }

        @Override
        public String getDescription() { return description; }
    }

    //todo: 에러코드 재확인
    public enum StackerCraneErrorCode {
        NORMAL(0, "정상"),

        CRANE_EMERGENCY_STOP_TOTAL(1, "Stacker Crane 지상반 비상정지"),
        GROUND_PANEL_SAFETY_DOOR_ACTIVE(2, "Stacker Crane 지상반 안전도어동작"),
        GROUND_PANEL_LIGHT_CURTAIN_ACTIVE(3, "Stacker Crane 지상반 라이트 커튼 동작"),
        CCLINK_COMMUNICATION_ERROR(4, "Stacker Crane CC-LINK 통신 이상"),
        CARRIAGE_MAINTENANCE_PIN_HOME_ERROR(5, "Stacker Crane 캐리지 보수 점검 핀 이상(HOME)"),
        CARRIAGE_MAINTENANCE_PIN_END_ERROR(6, "Stacker Crane 캐리지 보수 점검 핀 이상(END)"),
        FORK_TOP_PLATE_LEFT_DEVIATION(7, "Stacker Crane 포크 상판 좌측 이탈"),
        FORK_TOP_PLATE_RIGHT_DEVIATION(8, "Stacker Crane 포크 상판 우측 이탈"),
        HOIST_WIRE_ROPE_TENSION_HOME_ERROR(9, "Stacker Crane 승강 와이어 로프 텐션 이상(HOME)"),
        HOIST_WIRE_ROPE_TENSION_END_ERROR(10, "Stacker Crane 승강 와이어 로프 텐션 이상(END)"),
        HOIST_DECELERATION_SENSOR_IO_ERROR(11, "Stacker Crane 승강 감속센서 I/O 이상"),
        TRAVEL_DECELERATION_SENSOR_IO_ERROR(12, "Stacker Crane 주행 감속센서 I/O 이상"),
        HOIST_EMERGENCY_OVERRUN(13, "Stacker Crane 승강 비상정지 OVER RUN"),
        TRAVEL_EMERGENCY_OVERRUN(14, "Stacker Crane 주행 비상정지 OVER RUN"),
        LEFT_CARGO_WIDTH_HOME_ERROR(15, "Stacker Crane 좌측 화물 폭 이상(HOME 방향)"),
        LEFT_CARGO_WIDTH_END_ERROR(16, "Stacker Crane 좌측 화물 폭 이상(END 방향)"),
        RIGHT_CARGO_WIDTH_HOME_ERROR(17, "Stacker Crane 우측 화물 폭 이상(HOME 방향)"),
        RIGHT_CARGO_WIDTH_END_ERROR(18, "Stacker Crane 우측 화물 폭 이상(END 방향)"),
        OPTICAL_MODEM_ERROR(19, "Stacker Crane 광모뎀 이상"),
        HOME_RETURN_MEMORY_CLEAR_ERROR(20, "Stacker Crane 주행/승강 홈 복귀 기억해제 이상"),
        FORK1_LEFT_CARGO_DEVIATION(21, "Stacker Crane 포크1 좌측 화물이탈"),
        FORK1_RIGHT_CARGO_DEVIATION(22, "Stacker Crane 포크1 우측 화물이탈"),
        FORK2_LEFT_CARGO_DEVIATION(23, "Stacker Crane 포크2 좌측 화물이탈"),
        FORK2_RIGHT_CARGO_DEVIATION(24, "Stacker Crane 포크2 우측 화물이탈"),
        LEFT_CARGO_HEIGHT_ERROR(25, "Stacker Crane 좌측 화물 높이 이상"),
        RIGHT_CARGO_HEIGHT_ERROR(26, "Stacker Crane 우측 화물 높이 이상"),
        CARGO_DETECTION_SENSOR_ERROR(27, "Stacker Crane 화물 검출 센서 이상"),
        RESERVED_28(28, "-"),
        RESERVED_29(29, "-"),
        FORK_MOVING_HOIST_POSITION_DEVIATION(30, "Stacker Crane 포크 이동중 승강 정위치 이탈"),
        FORK1_EDGE_HOIST_POSITION_DEVIATION(31, "Stacker Crane 포크1 끝단에서 승강 정위치 이탈"),
        FORK1_EDGE_TRAVEL_POSITION_DEVIATION(32, "Stacker Crane 포크1 끝단에서 주행 정위치 이탈"),
        FORK2_EDGE_HOIST_POSITION_DEVIATION(33, "Stacker Crane 포크2 끝단에서 승강 정위치 이탈"),
        FORK2_EDGE_TRAVEL_POSITION_DEVIATION(34, "Stacker Crane 포크2 끝단에서 주행 정위치 이탈"),
        TRAVEL_POSITION_SENSOR_COMPARE_ERROR(35, "Stacker Crane 주행 위치값 & 정위치 센서 비교 이상"),
        HOIST_POSITION_SENSOR_COMPARE_ERROR(36, "Stacker Crane 승강 위치값 & 정위치 센서 비교 이상"),
        FORK1_LEFT_POSITION_SENSOR_ERROR(37, "Stacker Crane 포크1 좌 정위치 센서 이상"),
        FORK1_CENTER_POSITION_SENSOR_ERROR(38, "Stacker Crane 포크1 센터 정위치 센서 이상"),
        FORK1_RIGHT_POSITION_SENSOR_ERROR(39, "Stacker Crane 포크1 우 정위치 센서 이상"),
        FORK1_POSITION_SENSOR_DUPLICATE_ERROR(40, "Stacker Crane 포크1 정위치 센서 동시 검출 이상"),
        FORK2_LEFT_POSITION_SENSOR_ERROR(41, "Stacker Crane 포크2 좌 정위치 센서 이상"),
        FORK2_CENTER_POSITION_SENSOR_ERROR(42, "Stacker Crane 포크2 센터 정위치 센서 이상"),
        FORK2_RIGHT_POSITION_SENSOR_ERROR(43, "Stacker Crane 포크2 우 정위치 센서 이상"),
        FORK2_POSITION_SENSOR_DUPLICATE_ERROR(44, "Stacker Crane 포크2 정위치 센서 동시 검출 이상"),
        FORK1_LEFT_DOUBLE_STORAGE_SENSOR_ERROR(45, "Stacker Crane 포크1 좌 이중입고 센서 고장"),
        FORK2_LEFT_DOUBLE_STORAGE_SENSOR_ERROR(46, "Stacker Crane 포크2 좌 이중입고 센서 고장"),
        FORK1_RIGHT_DOUBLE_STORAGE_SENSOR_ERROR(47, "Stacker Crane 포크1 우 이중입고 센서 고장"),
        FORK2_RIGHT_DOUBLE_STORAGE_SENSOR_ERROR(48, "Stacker Crane 포크2 우 이중입고 센서 고장"),
        RESERVED_49(49, "-"),
        AUTO_TRAVEL_HOIST_STOP_POSITION_ERROR(50, "Stacker Crane 자동중 주행승강정지위치이상"),
        AUTO_POSITIONING_STOP_ERROR(51, "Stacker Crane 자동 정위치 정지이상(보정3회)"),
        AUTO_FORK1_STOP_POSITION_ERROR(52, "Stacker Crane 자동중 포크1 정지 위치 이상"),
        AUTO_FORK2_STOP_POSITION_ERROR(53, "Stacker Crane 자동중 포크2 정지 위치 이상"),
        FORK1_LOADING_NO_CARGO(54, "Stacker Crane 포크1 로딩후 화물 없음"),
        FORK1_UNLOADING_CARGO_REMAIN(55, "Stacker Crane 포크1 언로딩후 화물 있음"),
        TRAVEL_COMMAND_DATA_RANGE_ERROR(56, "Stacker Crane 주행 명령 데이터 범위 이상"),
        HOIST_COMMAND_DATA_RANGE_ERROR(57, "Stacker Crane 승강 명령 데이터 범위 이상"),
        FORK_COMMAND_DATA_RANGE_ERROR(58, "Stacker Crane 포크 명령 데이터 범위 이상"),
        PC_RECEIVED_DATA_ERROR(59, "Stacker Crane PC 수신 데이터 이상"),
        FORK1_DOUBLE_STORAGE_ERROR(60, "Stacker Crane 포크1 이중입고 이상"),
        FORK2_DOUBLE_STORAGE_ERROR(61, "Stacker Crane 포크2 이중입고 이상"),
        FORK12_DOUBLE_STORAGE_ERROR(62, "Stacker Crane 포크1,2 이중입고 이상"),
        FORK12_OUTPUT_STATION_DOUBLE_STORAGE(63, "Stacker Crane 포크1,2 출고 스테이션 이중입고"),
        FORK1_EMPTY_RETRIEVAL_ERROR(64, "Stacker Crane 포크1 공출고 이상"),
        FORK2_EMPTY_RETRIEVAL_ERROR(65, "Stacker Crane 포크2 공출고 이상"),
        FORK12_EMPTY_RETRIEVAL_ERROR(66, "Stacker Crane 포크1,2 공출고 이상"),
        MACHINE_ROOM_HOIST_MOTOR_EMERGENCY_STOP(67, "Stacker Crane 기상반 승강모터측 비상정지 이상"),
        FORBIDDEN_RACK_COMMAND_DATA_ERROR(68, "Stacker Crane 금지랙 명령 데이터 이상"),
        SC_CV_JOB_NUMBER_MISMATCH(69, "SC 작업번호 & CV 작업번호 비교 이상"),
        FORK2_LOADING_NO_CARGO(70, "Stacker Crane 포크2 로딩후 화물 없음"),
        FORK2_UNLOADING_CARGO_REMAIN(71, "Stacker Crane 포크2 언로딩후 화물 있음"),
        HOIST_DOG_COUNT_ERROR(72, "Stacker Crane 승강 DOG 카운트 이상"),
        TRAVEL_DOG_COUNT_ERROR(73, "Stacker Crane 주행 DOG 카운트 이상"),
        HOIST_POSITIONING_TOLERANCE_EXCEEDED(74, "Stacker Crane 승강 정지후 정위치 오차범위 초과"),
        TRAVEL_POSITIONING_TOLERANCE_EXCEEDED(75, "Stacker Crane 주행 정지후 정위치 오차범위 초과"),
        TRAVEL_DISTANCE_SENSOR_INTEGRATION_ERROR(76, "Stacker Crane 주행 거리센서 통합 이상"),
        HOIST_DISTANCE_SENSOR_INTEGRATION_ERROR(77, "Stacker Crane 승강 거리센서 통합 이상"),
        TRAVEL_MOVEMENT_TIMEOUT(78, "Stacker Crane 주행 이동시간 초과"),
        HOIST_MOVEMENT_TIMEOUT(79, "Stacker Crane 승강 이동시간 초과"),
        FORK1_MOVEMENT_TIMEOUT(80, "Stacker Crane 포크1 이동시간 초과"),
        FORK2_MOVEMENT_TIMEOUT(81, "Stacker Crane 포크2 이동시간 초과"),
        INOUT_STATION_WAIT_TIMEOUT(82, "Stacker Crane 입/출고대 대기시간 초과이상"),
        STACKER_CRANE_JOB_START_TIMEOUT(83, "Stacker Crane 작업시작 시간 초과"),
        TRAVEL_HIGH_SPEED_DECELERATION_OVERSPEED(84, "Stacker Crane 주행고속 감속구간 속도 초과(mm/s)"),
        TRAVEL_MIDDLE_SPEED_DECELERATION_OVERSPEED(85, "Stacker Crane 주행중속 감속구간 속도 초과(mm/s)"),
        HOIST_HIGH_SPEED_DECELERATION_OVERSPEED(86, "Stacker Crane 승강고속 감속구간 속도 초과(mm/s)"),
        HOIST_MIDDLE_SPEED_DECELERATION_OVERSPEED(87, "Stacker Crane 승강중속 감속구간 속도 초과(mm/s)"),
        FORK1_LOADING_MEMORY_CARGO_LOST(88, "Stacker Crane 포크1 로딩 기억 후 화물 없어짐"),
        FORK2_LOADING_MEMORY_CARGO_LOST(89, "Stacker Crane 포크2 로딩 기억 후 화물 없어짐"),
        HOIST_POSITION_VALUE_CHANGED_WHILE_STOPPED(90, "Stacker Crane 승강 위치값 정지중 변화 이상"),
        HOIST_POSITION_REVERSE_INPUT_ERROR(91, "Stacker Crane 승강 위치값 역방향 입력 이상"),
        HOIST_POSITION_NO_CHANGE_DURING_OPERATION(92, "Stacker Crane 승강 위치값 운전중 무변화"),
        PC_JOB_COMPLETE_PROCESS_TIMEOUT(93, "Stacker Crane 데이터 PC 작업완료 처리시간 초과"),
        TRAVEL_POSITION_VALUE_CHANGED_WHILE_STOPPED(94, "Stacker Crane 주행 위치값 정지중 변화 이상"),
        TRAVEL_POSITION_REVERSE_INPUT_ERROR(95, "Stacker Crane 주행 위치값 역방향 입력 이상"),
        TRAVEL_POSITION_NO_CHANGE_DURING_OPERATION(96, "Stacker Crane 주행 위치값 운전중 무변화"),
        ONLINE_INOUT_FORCE_PERMISSION_MODE(97, "Stacker Crane 온라인중 입출고 허가 강제모드ON"),
        ONLINE_MANUAL_MODE_SWITCH(98, "Stacker Crane 온라인 운전중 수동전환"),
        MACHINE_ROOM_EMERGENCY_STOP(99, "Stacker Crane 기상반 비상정지"),
        RESERVED_100(100, "-"),
        HOIST_INVERTER_MCCB_OFF(101, "Stacker Crane 승강 인버터 전원 MCCB OFF"),
        TRAVEL_INVERTER_MCCB_OFF(102, "Stacker Crane 주행 인버터 전원 MCCB OFF"),
        FORK1_INVERTER_MCCB_OFF(103, "Stacker Crane 포크1 인버터 전원 MCCB OFF"),
        FORK2_INVERTER_MCCB_OFF(104, "Stacker Crane 포크2 인버터 전원 MCCB OFF"),
        TRAVEL_INVERTER_ERROR(105, "Stacker Crane 주행 인버터 이상"),
        HOIST_INVERTER_ERROR(106, "Stacker Crane 승강 인버터 이상"),
        FORK1_INVERTER_ERROR(107, "Stacker Crane 포크1 인버터 이상"),
        FORK2_INVERTER_ERROR(108, "Stacker Crane 포크2 인버터 이상"),
        HOIST_MOTOR_TEMPERATURE_ERROR(109, "Stacker Crane 승강 모터 온도 이상"),
        TRAVEL_MOTOR_TEMPERATURE_ERROR(110, "Stacker Crane 주행 모터 온도 이상"),
        HOIST_INVERTER_BRAKE_RELEASE_SIGNAL_ERROR(111, "Stacker Crane 승강 인버터 브레이크 개방 요구신호 출력 이상"),
        TRAVEL_INVERTER_RUN_SIGNAL_ERROR(112, "Stacker Crane 주행 인버터 RUN 신호 출력 이상"),
        FORK1_INVERTER_RUN_SIGNAL_ERROR(113, "Stacker Crane 포크1 인버터 RUN 신호 출력 이상"),
        FORK2_INVERTER_RUN_SIGNAL_ERROR(114, "Stacker Crane 포크2 인버터 RUN 신호 출력 이상"),
        HOIST_INVERTER_BRAKING_RESISTOR_TEMPERATURE_ERROR(115, "Stacker Crane 승강 인버터 제동 저항 온도 이상"),
        TRAVEL_INVERTER_BRAKING_RESISTOR_TEMPERATURE_ERROR(116, "Stacker Crane 주행 인버터 제동 저항 온도 이상"),
        HOIST_INVERTER_BRAKE_UNIT_ERROR(117, "Stacker Crane 승강 인버터 브레이크 유닛 이상"),
        RESERVED_118(118, "-"),
        TRAVEL_BRAKE_RELAY_OPERATION_ERROR(119, "Stacker Crane 주행 브레이크 열림 제어 RELAY 동작이상"),
        HOIST_BRAKE_RELAY_OPERATION_ERROR(120, "Stacker Crane 승강 브레이크 열림 제어 RELAY 동작이상"),
        FORK1_BRAKE_MC_OPERATION_ERROR(121, "Stacker Crane 포크1 브레이크 열림 제어 M.C 동작이상"),
        FORK2_BRAKE_MC_OPERATION_ERROR(122, "Stacker Crane 포크2 브레이크 열림 제어 M.C 동작이상"),
        HOIST_BRAKE_POWER_MC_ERROR(123, "Stacker Crane 승강 모터 브레이크 전원 M.C 동작 이상"),
        TRAVEL_BRAKE_POWER_MC_ERROR(124, "Stacker Crane 주행 모터 브레이크 전원 M.C 동작 이상"),
        HOIST_BRAKE_MMS_OVERLOAD(125, "Stacker Crane 승강 모터 브레이크 MMS 과부하"),
        TRAVEL_BRAKE_MMS_OVERLOAD(126, "Stacker Crane 주행 모터 브레이크 MMS 과부하"),
        FORK1_BRAKE_MMS_OVERLOAD(127, "Stacker Crane 포크1 모터 브레이크 MMS 과부하"),
        FORK2_BRAKE_MMS_OVERLOAD(128, "Stacker Crane 포크2 모터 브레이크 MMS 과부하"),
        HOIST_BRAKE_RELEASE_CONFIRM_ERROR(129, "Stacker Crane 승강 모터 브레이크 개방 확인 이상(DUE)"),
        TRAVEL_BRAKE_RELEASE_CONFIRM_ERROR(130, "Stacker Crane 주행 모터 브레이크 개방 확인 이상(DUE)"),
        HOIST_MOTOR_EOCR_OVERLOAD(131, "Stacker Crane 승강 모터 EOCR 과부하"),
        TRAVEL_MOTOR_EOCR_OVERLOAD(132, "Stacker Crane 주행 모터 EOCR 과부하"),
        FORK1_MOTOR_EOCR_OVERLOAD(133, "Stacker Crane 포크1 모터 EOCR 과부하"),
        FORK2_MOTOR_EOCR_OVERLOAD(134, "Stacker Crane 포크2 모터 EOCR 과부하"),
        DC24V_CONTROL_INPUT_CP_OFF(135, "Stacker Crane DC24V 제어 입력 CP OFF"),
        DC24V_CONTROL_OUTPUT_CP_OFF(136, "Stacker Crane DC24V 제어 출력 CP OFF"),
        BRAKE_POWER_CP_OFF(137, "Stacker Crane 브레이크 전원 CP OFF"),
        RESERVED_138(138, "-"),
        POWER_SUPPLY1_LOW_VOLTAGE_ALARM(139, "Stacker Crane 파워 서플라이1 저전압 알람"),
        POWER_SUPPLY1_LIFE_ALARM(140, "Stacker Crane 파워 서플라이1 수명 알람"),
        POWER_SUPPLY2_LOW_VOLTAGE_ALARM(141, "Stacker Crane 파워 서플라이2 저전압 알람"),
        POWER_SUPPLY2_LIFE_ALARM(142, "Stacker Crane 파워 서플라이2 수명 알람"),
        PLC_POWER_CP_OFF(143, "Stacker Crane PLC 전원 CP OFF"),
        PS1_PRIMARY_POWER_CP_OFF(144, "Stacker Crane PS1 1차 전원 CP OFF"),
        PS2_PRIMARY_POWER_CP_OFF(145, "Stacker Crane PS2 1차 전원 CP OFF"),
        SENSOR_POWER_DC24V_CP_OFF(146, "Stacker Crane 센서 전원 DC24V CP OFF"),
        HUB_POWER_DC24V_CP_OFF(147, "Stacker Crane HUB 전원 DC24V CP OFF"),
        RESERVED_148(148, "-"),
        RESERVED_149(149, "-"),
        RESERVED_150(150, "-"),
        MACHINE_ROOM_PLC_BATTERY_LOW(151, "Stacker Crane 기상반 PLC 배터리 저하"),
        HMI_POWER_CP_OFF(152, "Stacker Crane HMI 전원 CP OFF"),
        LIVE_LINE_220V_MCCB_OFF(153, "Stacker Crane 220V 활선LINE MCCB OFF"),
        GROUND_PANEL_EMERGENCY_STOP(201, "Stacker Crane 지상반 비상정지"),
        GROUND_PANEL_SAFETY_DOOR_OPEN(202, "Stacker Crane 지상반 안전도어 열림"),
        GROUND_PANEL_OPTICAL_MODEM_COMMUNICATION_ERROR(203, "Stacker Crane 지상반 광모뎀 통신이상"),
        GROUND_PANEL_PLC_BATTERY_LOW(204, "Stacker Crane 지상반 PLC 배터리 LOW"),
        GROUND_PANEL_PLC_DATA_PC_COMMUNICATION_ERROR(205, "Stacker Crane 지상반 PLC <-> DATA PC 통신이상"),

        UNKNOWN(999, "알 수 없음");

        private final int value;
        private final String description;

        StackerCraneErrorCode(int address, String description) {
            this.value = address;
            this.description = description;
        }

        public int getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static StackerCraneErrorCode find(int address) {
            return Arrays.stream(values())
                    .filter(v -> v.getValue() == address)
                    .findAny()
                    .orElse(UNKNOWN);
        }
    }

    public enum StackerCraneCargoStatus implements PlcBitEnum {
        CARGO(0, "적재 상태 - 0: 없음, 1: 적재");

        private final int bitIndex;
        private final String description;

        StackerCraneCargoStatus(int bitIndex, String description) {
            this.bitIndex = bitIndex;
            this.description = description;
        }

        @Override
        public int getBitIndex() { return bitIndex; }

        @Override
        public String getDescription() { return description; }
    }
}