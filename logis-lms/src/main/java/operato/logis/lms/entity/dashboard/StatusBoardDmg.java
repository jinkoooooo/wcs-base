package operato.logis.lms.entity.dashboard;

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
@Table(name = "status_board_dmg", idStrategy = GenerationRule.UUID)
public class StatusBoardDmg extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 20)
    private String lcId;

    @Column(name = "group_code", nullable = false, length = 20)
    private String groupCode;

    @Column(name = "group_type", nullable = false, length = 20)
    private String groupType;

    @Column(name = "model_type", nullable = false, length = 20)
    private String modelType;

    @Column(name = "render_order", nullable = false)
    private Integer renderOrder;

    @Column(name = "is_use", nullable = false)
    private Boolean isUse;

    @JsonProperty("positionX2d")
    @Column(name = "position_x_2d", nullable = false)
    private Float positionX2d;

    @JsonProperty("positionY2d")
    @Column(name = "position_y_2d", nullable = false)
    private Float positionY2d;

    @JsonProperty("scaleX2d")
    @Column(name = "scale_x_2d", nullable = false)
    private Float scaleX2d;

    @JsonProperty("scaleY2d")
    @Column(name = "scale_y_2d", nullable = false)
    private Float scaleY2d;

    @JsonProperty("rotation2d")
    @Column(name = "rotation_2d", nullable = false)
    private Float rotation2d;

    @JsonProperty("flipHorizontal2d")
    @Column(name = "flip_horizontal_2d", nullable = false)
    private Boolean flipHorizontal2d;

    @JsonProperty("flipVertical2d")
    @Column(name = "flip_vertical_2d", nullable = false)
    private Boolean flipVertical2d;

    @JsonProperty("positionX3d")
    @Column(name = "position_x_3d", nullable = false)
    private Float positionX3d;

    @JsonProperty("positionY3d")
    @Column(name = "position_y_3d", nullable = false)
    private Float positionY3d;

    @JsonProperty("positionZ3d")
    @Column(name = "position_z_3d", nullable = false)
    private Float positionZ3d;

    @JsonProperty("scaleX3d")
    @Column(name = "scale_x_3d", nullable = false)
    private Float scaleX3d;

    @JsonProperty("scaleY3d")
    @Column(name = "scale_y_3d", nullable = false)
    private Float scaleY3d;

    @JsonProperty("scaleZ3d")
    @Column(name = "scale_z_3d", nullable = false)
    private Float scaleZ3d;

    @JsonProperty("rotationX3d")
    @Column(name = "rotation_x_3d", nullable = false)
    private Float rotationX3d;

    @JsonProperty("rotationY3d")
    @Column(name = "rotation_y_3d", nullable = false)
    private Float rotationY3d;

    @JsonProperty("rotationZ3d")
    @Column(name = "rotation_z_3d", nullable = false)
    private Float rotationZ3d;

    @JsonProperty("boxPositionX3d")
    @Column(name = "box_position_x_3d", nullable = false)
    private Float boxPositionX3d;

    @JsonProperty("boxPositionY3d")
    @Column(name = "box_position_y_3d", nullable = false)
    private Float boxPositionY3d;

    @JsonProperty("boxPositionZ3d")
    @Column(name = "box_position_z_3d", nullable = false)
    private Float boxPositionZ3d;

    @JsonProperty("boxScaleX3d")
    @Column(name = "box_scale_x_3d", nullable = false)
    private Float boxScaleX3d;

    @JsonProperty("boxScaleY3d")
    @Column(name = "box_scale_y_3d", nullable = false)
    private Float boxScaleY3d;

    @JsonProperty("boxScaleZ3d")
    @Column(name = "box_scale_z_3d", nullable = false)
    private Float boxScaleZ3d;

    @JsonProperty("boxRotationX3d")
    @Column(name = "box_rotation_x_3d", nullable = false)
    private Float boxRotationX3d;

    @JsonProperty("boxRotationY3d")
    @Column(name = "box_rotation_y_3d", nullable = false)
    private Float boxRotationY3d;

    @JsonProperty("boxRotationZ3d")
    @Column(name = "box_rotation_z_3d", nullable = false)
    private Float boxRotationZ3d;

    @Column(name = "box_is_use", nullable = false)
    private Boolean boxIsUse;
}
