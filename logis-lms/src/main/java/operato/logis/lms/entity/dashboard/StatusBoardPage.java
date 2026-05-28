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
@Table(name = "status_board_page", idStrategy = GenerationRule.UUID)
public class StatusBoardPage extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 20)
    private String lcId;

    @Column(name = "page_index", nullable = false)
    private Integer pageIndex;

    @Column(name = "page_name", nullable = false, length = 20)
    private String pageName;

    @Column(name = "background_color", nullable = false, length = 10)
    private String backgroundColor;

    @Column(name = "length_x", nullable = false)
    private Integer lengthX;

    @Column(name = "length_y", nullable = false)
    private Integer lengthY;
}