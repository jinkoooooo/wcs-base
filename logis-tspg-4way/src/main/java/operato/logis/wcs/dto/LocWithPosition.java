package operato.logis.wcs.dto;

import operato.logis.wcs.entity.ExtTbInventoryLocation;
import xyz.elidom.util.ValueUtil;

/**
 * 로케이션 마스터(ExtTbInventoryLocation) + 랙 좌표(row/bay/level) 결합 VO.
 * JOIN: tb_inventory_location.loc_id = tb_eq_rack_mst.id AND .rack_eq_id = tb_eq_rack_mst.eq_id
 */
public class LocWithPosition {

    private final ExtTbInventoryLocation loc;
    private final int row;
    private final int bay;
    private final int level;
    private final boolean driveOnlyYn;

    public LocWithPosition(ExtTbInventoryLocation loc, int row, int bay, int level, boolean driveOnlyYn) {
        this.loc = loc;
        this.row = row;
        this.bay = bay;
        this.level = level;
        this.driveOnlyYn = driveOnlyYn;
    }

    public ExtTbInventoryLocation getLoc() { return loc; }
    public int getRow() { return row; }
    public int getBay() { return bay; }
    public int getLevel() { return level; }
    public boolean isDriveOnlyYn() { return driveOnlyYn; }
    public boolean isDriveOnly() { return driveOnlyYn; }

    /** 같은 층·bay 에서 row 가 1 칸 차이인지 (좌우 인접). */
    public boolean isRowNeighborOf(LocWithPosition other) {
        if (ValueUtil.isEmpty(other)) return false;
        return this.level == other.level
                && this.bay == other.bay
                && Math.abs(this.row - other.row) == 1;
    }

    /** 같은 층·row 에서 bay 가 1 칸 차이인지 (앞뒤 인접). */
    public boolean isBayNeighborOf(LocWithPosition other) {
        if (ValueUtil.isEmpty(other)) return false;
        return this.level == other.level
                && this.row == other.row
                && Math.abs(this.bay - other.bay) == 1;
    }

    /** row/bay/level 좌표가 완전히 동일한지. */
    public boolean isSamePosition(LocWithPosition other) {
        if (ValueUtil.isEmpty(other)) return false;
        return this.level == other.level
                && this.row == other.row
                && this.bay == other.bay;
    }

    @Override
    public String toString() {
        return String.format("LocWithPosition{code=%s, pos=(R:%d, B:%d, L:%d)}",
                (ValueUtil.isNotEmpty(loc) ? loc.getLocId() : "N/A"), row, bay, level);
    }
}
