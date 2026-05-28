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
@Table(name = "tb_mcs_api_request_log", idStrategy = GenerationRule.UUID)
public class TbMcsApiRequestLog extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "ip")
    private String ip;

    @Column(name = "url")
    private String url;

    @Column(name = "param")
    private String param;

    @Column(name = "code")
    private String code;

    @Column(name = "message")
    private String message;
}