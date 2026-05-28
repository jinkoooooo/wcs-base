package operato.logis.connector.gtr.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Table(name = "tb_mw_gtr_inspection_results", idStrategy = GenerationRule.UUID)
public class GtrInspectionResult extends ElidomStampHook {

    @Id

    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "transaction_id", length = 8)
    private String transactionId;

    @Column(name = "zone_id", length = 8)
    private String zoneId;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "serial_numbers", length = 50)
    private String serialNumbers;

    @Column(name = "overall_result", length = 32)
    private String overallResult;

    @Column(name = "overall_confidence")
    private Double overallConfidence;

    @Column(name = "overall_reason", length = 16)
    private String overallReason;

    @Column(name = "overall_damage_classes", length = 32)
    private String overallDamageClasses;

//    @Column(name = "domain_id")
//    private Long domainId;
//
//    @Column(name = "created_at")
//    private Date createdAt;
//
//    @Column(name = "creator_id", length = 16)
//    private String creatorId;
//
//    @Column(name = "updated_at")
//    private Date updatedAt;
//
//    @Column(name = "updater_id", length = 16)
//    private String updaterId;

}