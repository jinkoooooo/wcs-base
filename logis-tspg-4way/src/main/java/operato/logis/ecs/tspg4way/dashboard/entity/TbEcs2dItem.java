package operato.logis.ecs.tspg4way.dashboard.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * 4-Way Shuttle 설비 레이아웃 Entity
 * 캔버스에 배치된 설비의 2D 위치/크기/회전 정보
 *
 * 참고: equipmentId는 더 이상 필수가 아님 (ShuttleEquipmentMaster 제거됨)
 * 대신 equipmentTypeCode로 설비 타입을 식별하고, realEqId로 실운영 설비와 매핑
 */
@Getter
@Setter
@Table(
        name = "tb_ecs_2d_item",
        idStrategy = "uuid",
        notnullFields = "lcId,pageId,equipment_type_code",
        indexes = {
                @Index(name = "ix_tb_ecs_2d_item_1", columnList = "lc_id,page_id"),
                @Index(name = "ix_tb_ecs_2d_item_2", columnList = "lc_id,equipment_type_code"),
                @Index(name = "ix_tb_ecs_2d_item_3", columnList = "real_eq_id")
        }
)
public class TbEcs2dItem extends ElidomStampHook {

    private static final long serialVersionUID = 970481448718926155L;

    @PrimaryKey
    @Column(name = "id", length = 40)
    private String id;

    /**
     * 센터 ID
     */
    @Column(name = "lc_id", length = 30)
    private String lcId;

    /**
     * 페이지 ID (FK: tb_ecs_2d_page)
     */
    @Column(name = "page_id", length = 40)
    private String pageId;

    /**
     * 설비 ID (FK: shuttle_equipment_master)
     */
    @Column(name = "equipment_id", length = 40)
    private String equipmentId;

    /**
     * 설비 코드 (빠른 조회용)
     */
    @Column(name = "equipment_code", length = 50)
    private String equipmentCode;

    /**
     * 설비 타입 코드 (빠른 조회용)
     */
    @Column(name = "equipment_type_code", length = 50)
    private String equipmentTypeCode;

    /**
     * 실운영 설비 ID (TbEqMst.eqId 매핑)
     */
    @Column(name = "real_eq_id", length = 50)
    private String realEqId;

    /**
     * 실운영 설비 타입 (TbEqMst.eqType)
     */
    @Column(name = "real_eq_type", length = 50)
    private String realEqType;

    /**
     * X 좌표 (픽셀, 좌측 기준)
     */
    @Column(name = "pos_x")
    private Double posX;

    /**
     * Y 좌표 (픽셀, 하단 기준)
     */
    @Column(name = "pos_y")
    private Double posY;

    /**
     * 너비 (픽셀)
     */
    @Column(name = "width")
    private Double width;

    /**
     * 높이 (픽셀)
     */
    @Column(name = "height")
    private Double height;

    /**
     * 회전 각도 (도)
     */
    @Column(name = "rotation")
    private Double rotation;

    /**
     * X축 스케일
     */
    @Column(name = "scale_x")
    private Double scaleX;

    /**
     * Y축 스케일
     */
    @Column(name = "scale_y")
    private Double scaleY;

    /**
     * 수평 뒤집기
     */
    @Column(name = "flip_h")
    private Boolean flipH;

    /**
     * 수직 뒤집기
     */
    @Column(name = "flip_v")
    private Boolean flipV;

    /**
     * Z-Index (렌더링 순서)
     */
    @Column(name = "z_index")
    private Integer zIndex;

    /**
     * 투명도 (0.0 ~ 1.0)
     */
    @Column(name = "opacity")
    private Double opacity;

    /**
     * 레이블 표시 여부
     */
    @Column(name = "show_label")
    private Boolean showLabel;

    /**
     * 커스텀 레이블
     */
    @Column(name = "custom_label", length = 100)
    private String customLabel;

    /**
     * 커스텀 색상 (상태 무시하고 고정 색상)
     */
    @Column(name = "custom_color", length = 20)
    private String customColor;

    /**
     * 표시 여부
     */
    @Column(name = "is_visible")
    private Boolean isVisible;

    /**
     * 잠금 여부 (편집 불가)
     */
    @Column(name = "is_locked")
    private Boolean isLocked;

    @Override
    public void beforeCreate() {
        super.beforeCreate();
        if (this.posX == null) this.posX = 0.0;
        if (this.posY == null) this.posY = 0.0;
        if (this.width == null) this.width = 100.0;
        if (this.height == null) this.height = 100.0;
        if (this.rotation == null) this.rotation = 0.0;
        if (this.scaleX == null) this.scaleX = 1.0;
        if (this.scaleY == null) this.scaleY = 1.0;
        if (this.flipH == null) this.flipH = false;
        if (this.flipV == null) this.flipV = false;
        if (this.zIndex == null) this.zIndex = 0;
        if (this.opacity == null) this.opacity = 1.0;
        if (this.showLabel == null) this.showLabel = true;
        if (this.isVisible == null) this.isVisible = true;
        if (this.isLocked == null) this.isLocked = false;
    }
}
