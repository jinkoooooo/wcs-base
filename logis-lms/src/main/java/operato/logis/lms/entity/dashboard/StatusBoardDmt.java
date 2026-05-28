package operato.logis.lms.entity.dashboard;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "status_board_dmt", idStrategy = GenerationRule.UUID)
public class StatusBoardDmt extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType;

    @Column(name = "lc_id", nullable = false, length = 20)
    private String lcId;

    @Column(name = "image_2d")
    private String image2d;

    @Column(name = "image_3d")
    private String image3d;

    @Column(name = "is_use", nullable = false)
    private Boolean isUse;
}