package operato.logis.posco.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_mcs_location", idStrategy = GenerationRule.UUID)
public class TbMcsLocation extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "location_cd", length = 40)
    private String locationCd;

    @Column(name = "location_nm", length = 40)
    private String locationNm;

    @Column(name = "group_cd", length = 40)
    private String groupCd;

    @Column(name = "location_type", length = 20)
    private String locationType;

    @Column(name = "stock_id", length = 40)
    private String stockId;

    @Column(name = "task_id", length = 40)
    private String taskId;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "pod_code", length = 40)
    private String podCode;

    @Column(name = "pod_type", length = 20)
    private String podType;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(name = "rotation")
    private Integer rotation;
}