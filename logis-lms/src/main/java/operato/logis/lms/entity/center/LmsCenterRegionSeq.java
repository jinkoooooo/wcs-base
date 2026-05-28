package operato.logis.lms.entity.center;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.UpdateStampHook;

import java.io.Serial;

@Table(name = "lms_center_region_seq", idStrategy = GenerationRule.NONE, notnullFields = "regionId,regionNm,currentSeq")
public class LmsCenterRegionSeq extends UpdateStampHook {
    @Serial
    private static final long serialVersionUID = 5566956735264953072L;

    @PrimaryKey
    @Column(name = "region_id", nullable = false, length = 10)
    private String regionId;

    @Column(name = "region_nm", nullable = false, length = 10)
    private String regionNm;

    @Column(name = "current_seq", nullable = false)
    private Integer currentSeq;

    public String getRegionId() { return regionId; }

    public String getRegionNm() { return regionNm; }

    public Integer getCurrentSeq() { return currentSeq; }

    public void setRegionId(String regionId) { this.regionId = regionId; }

    public void setRegionNm(String regionNm) { this.regionNm = regionNm; }

    public void setCurrentSeq(Integer currentSeq) { this.currentSeq = currentSeq; }
}