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
@Table(name = "tb_mw_gtr_inspection_findings", idStrategy = GenerationRule.UUID)
public class GtrInspectionFinding extends ElidomStampHook {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "transaction_id", length = 40)
    private String TransactionId;

    @Column(name = "damage_class", length = 50)
    private String damageClass;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "reason", length = 16)
    private String reason;

    // Bounding Box (List<Integer> -> 4 Columns)
    @Column(name = "bbox_x_min")
    private Integer bboxXMin;

    @Column(name = "bbox_y_min")
    private Integer bboxYMin;

    @Column(name = "bbox_x_max")
    private Integer bboxXMax;

    @Column(name = "bbox_y_max")
    private Integer bboxYMax;

    @Column(name = "side_result_id", length = 40)
    private String sideResultId;

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