package operato.logis.kmat_2026.biz.wcs.kmat_2026.consts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class KMat2026LocationMapping {

    private KMat2026LocationMapping() {}

    public static final String EQ_GROUP_ID        = "K_MAT_TSPG";
    public static final String SOURCE_SYSTEM_CODE = "KMAT_2026";
    public static final String SOURCE_TYPE        = "DIRECT";

    public static final String OUTBOUND_PORT_1 = "10601";
    public static final String OUTBOUND_PORT_2 = "10602";
    public static final List<String> OUTBOUND_PORTS = Arrays.asList(OUTBOUND_PORT_1, OUTBOUND_PORT_2);

    public static final String INBOUND_PORT = "INBOUND_01";
    public static final String POD_BUFFER   = "POD_BUFFER_01";

    public static final Map<String, String> WCS_TO_ECS_OUTBOUND = Map.of(
            "10601", "TSPG_CONV_OUT_01",
            "10602", "TSPG_CONV_OUT_02"
    );

    public static final Map<String, String> WCS_TO_ECS_INBOUND = Map.of(
            "INBOUND_01", "TSPG_CONV_IN_01"
    );

    public static final Map<String, String> ECS_TO_WCS_OUTBOUND = Map.of(
            "TSPG_CONV_OUT_01", "10601",
            "TSPG_CONV_OUT_02", "10602"
    );

    public static final Map<String, String> ECS_TO_WCS_INBOUND = Map.of(
            "TSPG_CONV_IN_01", "INBOUND_01"
    );

    /**
     * 1층에서 사용할 후보 포인트
     * 실제 선택 순서는 DB loc_seq 오름차순 기준
     */
    public static final List<String> FLOOR_1_POINTS = Arrays.asList(
            "10401", "10301", "10201",
            "10402", "10302", "10202",
            "10403", "10303", "10203"
    );

    /**
     * 2층에서 사용할 후보 포인트
     * 실제 선택 순서는 DB loc_seq 오름차순 기준
     */
    public static final List<String> FLOOR_2_POINTS = Arrays.asList(
            "20401", "20301", "20201",
            "20402", "20302", "20202",
            "20403", "20303", "20203"
    );

    public enum CycleMode {
        FLOOR_2_SHUTTLE_CAN_MOVE,       // level=2 car battery_status == 0
        FLOOR_2_SHUTTLE_CAN_NOT_MOVE    // level=2 car battery_status != 0
    }

    // ========================================================================
    // 라운드/사이클 상수
    // ========================================================================
    public static final int CYCLES_PER_ROUND  = 1;
    public static final int PALLETS_PER_CYCLE = 2;
    public static final int PALLETS_PER_ROUND = CYCLES_PER_ROUND * PALLETS_PER_CYCLE; // 6

    // ========================================================================
    // 2층 ↔ 1층 매핑
    // ========================================================================
    public static final Map<String, String> FLOOR_2_TO_1 = Map.of(
            "20401", "10401", "20402", "10402", "20403", "10403",
            "20301", "10301", "20302", "10302", "20303", "10303",
            "20201", "10201", "20202", "10202", "20203", "10203"
    );

    // ========================================================================
    // CyclePoint VO
    // - 1 사이클에서 처리할 모든 포인트 정보
    // - 라운드 시작 시 KMat2026RoundPlanService 가 DB 조회 후 동적 생성
    // ========================================================================
    public static class CyclePoint {
        public final String outbound1Loc;
        public final Integer outbound1LocSeq;

        public final String outbound2Loc;
        public final Integer outbound2LocSeq;

        public final String move1From;
        public final Integer move1FromSeq;
        public final String move1To;
        public final Integer move1ToSeq;

        public final String move2From;
        public final Integer move2FromSeq;
        public final String move2To;
        public final Integer move2ToSeq;

        public final String inbound1ToLoc;
        public final Integer inbound1ToLocSeq;

        public final String inbound2ToLoc;
        public final Integer inbound2ToLocSeq;

        public final CycleMode mode;

        public CyclePoint(
                String outbound1Loc, Integer outbound1LocSeq,
                String outbound2Loc, Integer outbound2LocSeq,
                String move1From, Integer move1FromSeq,
                String move1To, Integer move1ToSeq,
                String move2From, Integer move2FromSeq,
                String move2To, Integer move2ToSeq,
                String inbound1ToLoc, Integer inbound1ToLocSeq,
                String inbound2ToLoc, Integer inbound2ToLocSeq,
                CycleMode mode
        ) {
            this.outbound1Loc = outbound1Loc;
            this.outbound1LocSeq = outbound1LocSeq;
            this.outbound2Loc = outbound2Loc;
            this.outbound2LocSeq = outbound2LocSeq;
            this.move1From = move1From;
            this.move1FromSeq = move1FromSeq;
            this.move1To = move1To;
            this.move1ToSeq = move1ToSeq;
            this.move2From = move2From;
            this.move2FromSeq = move2FromSeq;
            this.move2To = move2To;
            this.move2ToSeq = move2ToSeq;
            this.inbound1ToLoc = inbound1ToLoc;
            this.inbound1ToLocSeq = inbound1ToLocSeq;
            this.inbound2ToLoc = inbound2ToLoc;
            this.inbound2ToLocSeq = inbound2ToLocSeq;
            this.mode = mode;
        }

        public boolean hasMove() {
            return mode == CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE;
        }

        @Override
        public String toString() {
            return "CyclePoint{" +
                    "mode=" + mode +
                    ", out1='" + outbound1Loc + '\'' +
                    ", out1Seq=" + outbound1LocSeq +
                    ", out2='" + outbound2Loc + '\'' +
                    ", out2Seq=" + outbound2LocSeq +
                    ", move1From='" + move1From + '\'' +
                    ", move1To='" + move1To + '\'' +
                    ", move2From='" + move2From + '\'' +
                    ", move2To='" + move2To + '\'' +
                    ", inbound1ToLoc='" + inbound1ToLoc + '\'' +
                    ", inbound2ToLoc='" + inbound2ToLoc + '\'' +
                    '}';
        }
    }

    public static String toEcsOutboundLoc(String wcsLocCode) {
        return WCS_TO_ECS_OUTBOUND.get(wcsLocCode);
    }

    public static String toEcsInboundLoc(String wcsLocCode) {
        return WCS_TO_ECS_INBOUND.get(wcsLocCode);
    }

    public static String toWcsOutboundLoc(String ecsLocCode) {
        return ECS_TO_WCS_OUTBOUND.get(ecsLocCode);
    }

    public static String toWcsInboundLoc(String ecsLocCode) {
        return ECS_TO_WCS_INBOUND.get(ecsLocCode);
    }

    public static boolean isEcsOutboundLoc(String ecsLocCode) {
        return ECS_TO_WCS_OUTBOUND.containsKey(ecsLocCode);
    }

    public static boolean isOutboundPort(String loc) {
        return OUTBOUND_PORTS.contains(loc);
    }

    public static String generateOrderKey(String prefix, String locCode) {
        return prefix + "_" + System.currentTimeMillis() + "_" + locCode;
    }
}