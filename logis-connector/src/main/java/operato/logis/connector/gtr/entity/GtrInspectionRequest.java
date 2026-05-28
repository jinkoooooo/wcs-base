package operato.logis.connector.gtr.entity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Data
@Table(name = "tb_mw_gtr_inspection_requests", idStrategy = GenerationRule.UUID)
public class GtrInspectionRequest extends ElidomStampHook {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "zone_id")
    private String zoneId;

    @Column(name = "serial_numbers")
    private String serialNumbers;

    @Column(name = "req_timestamp")
    private String reqTimestamp;

    @Column(name = "file_front")
    private String fileFront;

    @Column(name = "file_back")
    private String fileBack;

    @Column(name = "file_left")
    private String fileLeft;

    @Column(name = "file_right")
    private String fileRight;

    @Column(name = "file_top")
    private String fileTop;

    @Column(name = "file_bottom_left")
    private String fileBottomLeft;

    @Column(name = "file_bottom_right")
    private String fileBottomRight;

}