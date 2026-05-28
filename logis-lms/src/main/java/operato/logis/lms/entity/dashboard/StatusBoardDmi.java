package operato.logis.lms.entity.dashboard;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "status_board_dmi", idStrategy = GenerationRule.UUID)
public class StatusBoardDmi extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 20)
    private String lcId;

    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType;

    @Column(name = "model_code", nullable = false, length = 20)
    private String modelCode;

    @Column(name = "group_type", nullable = false, length = 20)
    private String groupType;

    @Column(name = "group_code", nullable = false, length = 20)
    private String groupCode;

    @Column(name = "is_use", nullable = false)
    private Boolean isUse;

    @Column(name = "instance_status", nullable = false)
    private Integer instanceStatus;

    @Column(name = "task_id", length = 100)
    private String taskId;

    @Column(name = "stock_id", length = 100)
    private String stockId;

    @Column(name = "error_code", length = 20)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "render_page_id", nullable = false, length = 40)
    private String renderPageId;

    @JsonProperty("positionX2d")
    @JsonAlias("position_x_2d")
    @Column(name = "position_x_2d", nullable = false)
    private Float positionX2d;

    @JsonProperty("positionY2d")
    @JsonAlias("position_y_2d")
    @Column(name = "position_y_2d", nullable = false)
    private Float positionY2d;

    @JsonProperty("scaleX2d")
    @JsonAlias("scale_x_2d")
    @Column(name = "scale_x_2d", nullable = false)
    private Float scaleX2d;

    @JsonProperty("scaleY2d")
    @JsonAlias("scale_y_2d")
    @Column(name = "scale_y_2d", nullable = false)
    private Float scaleY2d;

    @JsonProperty("rotation2d")
    @JsonAlias("rotation_2d")
    @Column(name = "rotation_2d", nullable = false)
    private Float rotation2d;

    @JsonProperty("flipHorizontal2d")
    @JsonAlias("flip_horizontal_2d")
    @Column(name = "flip_horizontal_2d", nullable = false)
    private Boolean flipHorizontal2d;

    @JsonProperty("flipVertical2d")
    @JsonAlias("flip_vertical_2d")
    @Column(name = "flip_vertical_2d", nullable = false)
    private Boolean flipVertical2d;

    @Column(name = "position_x_3d", nullable = false)
    private Float positionX3d;

    @Column(name = "position_y_3d", nullable = false)
    private Float positionY3d;

    @Column(name = "position_z_3d", nullable = false)
    private Float positionZ3d;

    @Column(name = "scale_x_3d", nullable = false)
    private Float scaleX3d;

    @Column(name = "scale_y_3d", nullable = false)
    private Float scaleY3d;

    @Column(name = "scale_z_3d", nullable = false)
    private Float scaleZ3d;

    @Column(name = "rotation_x_3d", nullable = false)
    private Float rotationX3d;

    @Column(name = "rotation_y_3d", nullable = false)
    private Float rotationY3d;

    @Column(name = "rotation_z_3d", nullable = false)
    private Float rotationZ3d;

    @Column(name = "box_position_x_3d", nullable = false)
    private Float boxPositionX3d;

    @Column(name = "box_position_y_3d", nullable = false)
    private Float boxPositionY3d;

    @Column(name = "box_position_z_3d", nullable = false)
    private Float boxPositionZ3d;

    @Column(name = "box_scale_x_3d", nullable = false)
    private Float boxScaleX3d;

    @Column(name = "box_scale_y_3d", nullable = false)
    private Float boxScaleY3d;

    @Column(name = "box_scale_z_3d", nullable = false)
    private Float boxScaleZ3d;

    @Column(name = "box_rotation_x_3d", nullable = false)
    private Float boxRotationX3d;

    @Column(name = "box_rotation_y_3d", nullable = false)
    private Float boxRotationY3d;

    @Column(name = "box_rotation_z_3d", nullable = false)
    private Float boxRotationZ3d;

    @Column(name = "box_is_use", nullable = false)
    private Boolean boxIsUse;
}