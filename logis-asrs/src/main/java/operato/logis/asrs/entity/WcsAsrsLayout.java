package operato.logis.asrs.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "tb_wcs_asrs_layout", idStrategy = GenerationRule.UUID)
@Data
public class WcsAsrsLayout extends ElidomStampHook {

    private static final long serialVersionUID = 249851945630483765L;

    @PrimaryKey
    @Column (name = "id", nullable = false, length = 40)
    private String id;

//    @Column(name = "domain_id", nullable = false)
//    private Long domainId;

    @Column(name = "center_id", length = 100, nullable = false)
    private String centerId;

    @Column(name = "zone_id", length = 100, nullable = false)
    private String zoneId;

    @Column(name = "layout_version", length = 50)
    private String layoutVersion;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "layout_data", type = ColumnType.TEXT, nullable = false)
    private String layoutData;

}