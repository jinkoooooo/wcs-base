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
@Table(name = "tb_mw_gtr_side_results", idStrategy = GenerationRule.UUID)
public class GtrSideResult extends ElidomStampHook {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @Column(name = "main_transaction_id", length = 8)
    private String mainTransactionId;

    @Column(name = "side_name", length = 50)
    private String sideName;

    @Column(name = "side_result", length = 50)
    private String sideResult;

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