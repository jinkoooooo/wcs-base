package operato.logis.ecs.tspg4way.dashboard.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * 4-Way Shuttle 설비 타입 Entity
 * 컨베이어, 리프터, BCR, 랙, 셔틀 등 설비 종류 마스터
 */
@Getter
@Setter
@Table(
        name = "tb_ecs_2d_item_type",
        idStrategy = "uuid",
        notnullFields = "lcId,typeCode,typeName",
        uniqueFields = "lcId,typeCode",
        indexes = {
                @Index(name = "ix_tb_ecs_2d_item_type_1", columnList = "lc_id,type_code")
        }
)
public class TbEcs2dItemType extends ElidomStampHook {

    private static final long serialVersionUID = 731564098217345902L;

    @PrimaryKey
    @Column(name = "id", length = 40)
    private String id;

    /**
     * 센터 ID
     */
    @Column(name = "lc_id", length = 30)
    private String lcId;

    /**
     * 설비 타입 코드
     * - 정적 설비 (MapEditor용): CONVEYOR, LIFTER, BCR, RACK, PILLAR
     * - 동적 설비 (Dashboard용): SHUTTLE, STV, CRANE
     */
    @Column(name = "type_code", length = 50)
    private String typeCode;

    /**
     * 설비 타입명
     */
    @Column(name = "type_name", length = 100)
    private String typeName;

    /**
     * 설비 카테고리 (이동설비, 보관설비, 검증설비 등)
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 2D 아이콘 이미지 URL
     */
    @Column(name = "icon_url_2d", length = 500)
    private String iconUrl2d;

    /**
     * 2D 아이콘 Base64 데이터
     */
    @Column(name = "icon_data_2d", type = xyz.elidom.dbist.annotation.ColumnType.TEXT)
    private String iconData2d;

    /**
     * 기본 너비 (픽셀)
     */
    @Column(name = "default_width")
    private Integer defaultWidth;

    /**
     * 기본 높이 (픽셀)
     */
    @Column(name = "default_height")
    private Integer defaultHeight;

    /**
     * 회전 가능 여부
     */
    @Column(name = "rotatable")
    private Boolean rotatable;

    /**
     * 크기 조절 가능 여부
     */
    @Column(name = "resizable")
    private Boolean resizable;

    /**
     * 실시간 상태 표시 여부
     */
    @Column(name = "show_status")
    private Boolean showStatus;

    /**
     * 클릭 시 팝업 표시 여부
     */
    @Column(name = "show_popup")
    private Boolean showPopup;

    /**
     * 정렬 순서 (팔레트 표시 순서)
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * 설명
     */
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon_file_name", length = 200)
    private String iconFileName;

    /**
     * 레이어 타입
     * - static  : 정적 설비 (MapEditor 캔버스에 고정)
     * - dynamic : 동적 설비 (Dashboard에서 실시간 이동)
     * - overlay : 오버레이 (기둥, 레일 등 구조물)
     */
    @Column(name = "layer_type", length = 20)
    private String layerType;

    /**
     * 실운영 EqType 숫자 코드 (tb_eq_mst.type)
     * - RACK=11, CONVEYOR/LIFTER=21, SHUTTLE_CAR=22
     * - null이면 실운영 설비 매핑 불가
     */
    @Column(name = "real_eq_type_num")
    private Integer realEqTypeNum;

    /**
     * 화물 데이터 연동 여부
     */
    @Column(name = "has_cargo")
    private Boolean hasCargo;

    /**
     * 재고 데이터 연동 여부 (RACK 등)
     */
    @Column(name = "has_inventory")
    private Boolean hasInventory;

    @Override
    public void beforeCreate() {
        super.beforeCreate();
        if (this.defaultWidth == null) this.defaultWidth = 100;
        if (this.defaultHeight == null) this.defaultHeight = 100;
        if (this.rotatable == null) this.rotatable = true;
        if (this.resizable == null) this.resizable = true;
        if (this.showStatus == null) this.showStatus = true;
        if (this.showPopup == null) this.showPopup = true;
        if (this.isActive == null) this.isActive = true;
        if (this.sortOrder == null) this.sortOrder = 0;
    }
}
