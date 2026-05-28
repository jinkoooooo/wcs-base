package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;

/**
 * [위치 정보 포함 로케이션 VO]
 *
 * WCS 로케이션 마스터(TbWcsLocMst) 정보와 ECS 랙 마스터(tb_eq_rack_mst)의
 * 물리 좌표(row/bay/level)를 통합하여 관리하는 객체입니다.
 *
 * [JOIN 조건 업데이트]
 * tb_wcs_loc_mst.rack_cell_id = tb_eq_rack_mst.id
 * AND tb_wcs_loc_mst.rack_eq_id = tb_eq_rack_mst.eq_id
 *
 * [핵심 용도]
 * - 셔틀 이동 경로 최적화 및 간섭 체크
 * - 입고 시 '인접 셀(Neighbor)' 점유 상태 확인을 통한 설비 안전성 확보
 */
public class LocWithPosition {

    private final TbWcsLocMst loc;

    /** tb_eq_rack_mst.row — X축 (통로 방향) */
    private final int row;

    /** tb_eq_rack_mst.bay — Y축 (깊이 방향) */
    private final int bay;

    /** tb_eq_rack_mst.level — Z축 (층) */
    private final int level;

    /** tb_eq_rack_mst.drive_only_yn */
    private final boolean driveOnlyYn;

    public LocWithPosition(TbWcsLocMst loc, int row, int bay, int level, boolean driveOnlyYn) {
        this.loc = loc;
        this.row = row;
        this.bay = bay;
        this.level = level;
        this.driveOnlyYn = driveOnlyYn;
    }

    // --- [Getter] ---
    public TbWcsLocMst getLoc() { return loc; }
    public int getRow() { return row; }
    public int getBay() { return bay; }
    public int getLevel() { return level; }
    public boolean isDriveOnlyYn() { return driveOnlyYn; }

    // -----------------------------------------------------------------------
    // 인접(Neighbor) 판별 헬퍼 메서드
    // -----------------------------------------------------------------------

    /** 동일 층(level)에서 Row 방향(통로상) 바로 옆 이웃인지 확인 */
    public boolean isRowNeighborOf(LocWithPosition other) {
        if (other == null) return false;
        return this.level == other.level
                && this.bay == other.bay
                && Math.abs(this.row - other.row) == 1;
    }

    /** 동일 층(level)에서 Bay 방향(깊이상) 바로 옆 이웃인지 확인 */
    public boolean isBayNeighborOf(LocWithPosition other) {
        if (other == null) return false;
        return this.level == other.level
                && this.row == other.row
                && Math.abs(this.bay - other.bay) == 1;
    }

    /** 두 위치가 완전히 동일한 물리 좌표인지 확인 */
    public boolean isSamePosition(LocWithPosition other) {
        if (other == null) return false;
        return this.level == other.level
                && this.row == other.row
                && this.bay == other.bay;
    }

    @Override
    public String toString() {
        return String.format("LocWithPosition{code=%s, pos=(R:%d, B:%d, L:%d)}",
                (loc != null ? loc.getLocCode() : "N/A"), row, bay, level);
    }

    public boolean isDriveOnly() {
        return driveOnlyYn;
    }
}