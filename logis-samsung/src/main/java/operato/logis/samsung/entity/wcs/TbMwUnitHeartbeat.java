package operato.logis.samsung.entity.wcs;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "tb_mw_unit_heartbeat", idStrategy = GenerationRule.UUID)
public class TbMwUnitHeartbeat extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "unit_type", nullable = false, length = 20)
    private String unitType;

    @Column(name = "unit_code", nullable = false, length = 20)
    private String unitCode;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "msg")
    private String msg;

    @Column(name = "ip")
    private String ip;

    @Column(name = "port")
    private String port;

    @Column(name = "instance_id")
    private String instanceId;

    @Column(name = "attribute1", length = 10)
    private String attribute1;

    @Column(name = "attribute2", length = 100)
    private String attribute2;
}