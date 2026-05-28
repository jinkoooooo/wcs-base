package operato.logis.samsung.entity.wcs;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "tb_mw_unit_error_log", idStrategy = GenerationRule.UUID)
public class TbMwUnitErrorLog extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "unit_type", nullable = false, length = 20)
    private String unitType;

    @Column(name = "unit_code", nullable = false, length = 20)
    private String unitCode;

    @Column(name = "error_code", nullable = false, length = 20)
    private String errorCode;

    @Column(name = "error_msg")
    private String errorMsg;
}