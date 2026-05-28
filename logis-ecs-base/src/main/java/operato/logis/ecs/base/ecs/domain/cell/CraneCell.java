package operato.logis.ecs.base.ecs.domain.cell;

import lombok.Data;

@Data
public class CraneCell {
    private String id;

    private Integer stationNo;
    private Integer asiel;
    private Integer bay;
    private Integer level;
    private String side;

    private boolean isUsable; // 사용 여부

    private boolean hasCargo; // 화물 적재 유무
    private boolean enabled; // 셀 활성화 여부
    private boolean blocked; // 셀 막힘 여부
    private boolean reserved; // 셀 작업중 여부

    // 검토완료)
    public CraneCell(int asiel, int bay, int level, boolean blocked) {
        this.asiel = asiel;
        this.bay = bay;
        this.level = level;
        this.blocked = blocked;
    }

    public CraneCell(Integer stationNo, Integer asiel, Integer bay, Integer level) {
        this.stationNo = stationNo == null ? 0 : stationNo;
        this.asiel = asiel == null ? 0 : asiel;
        this.bay = bay == null ? 0 : bay;
        this.level = level == null ? 0 : level;
    }

    /** 입출고대 여부 */
    public boolean isStationType() {
        return stationNo != null && stationNo > 0;
    }

    public int[] toWordArray() {
        if (isStationType()) {
            return new int[]{ stationNo, 0, 0, 0 };
        }
        return new int[]{ 0, asiel, bay, level };
    }

    public static CraneCell empty() {
        return new CraneCell(0, 0, 0, 0);
    }
}