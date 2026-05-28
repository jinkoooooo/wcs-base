package operato.logis.connector.gtr.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

@Getter
@Setter
@Table(name = "tb_mw_gtr_inspection_results", idStrategy = GenerationRule.UUID)
public class GtrVision extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "transaction_id", length = 8)
    private String transactionId;

    @Column(name = "zone_id", length = 8)
    private String zoneId;

    @Column(name = "timestamp")
    private Date timestamp;

    @Column(name = "serial_numbers", length = 50)
    private String serialNumbers;

    @Column(name = "overall_result", length = 32)
    private String overallResult;

    @Column(name = "overall_confidence")
    private float overallConfidence;

    @Column(name = "overall_reason", length = 16)
    private String overallReason;

    @Column(name = "overall_damage_classes", length = 32)
    private String overallDamageClasses;

    @Column(name = "reg_dt")
    private Date regDt;

    @Column(name = "reg_no", length = 16)
    private String regNo;
}
