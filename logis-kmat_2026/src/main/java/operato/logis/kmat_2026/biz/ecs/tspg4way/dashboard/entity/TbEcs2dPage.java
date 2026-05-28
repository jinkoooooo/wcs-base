package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * TB_ECS_2D_PAGE
 *
 * 2D 레이아웃 페이지(층/구역) 정의
 * - 센터/설비그룹/층/구역 기준으로 여러 페이지 구성 가능
 */
@Getter
@Setter
@Table(
        name = "tb_ecs_2d_page",
        idStrategy = "uuid",
        notnullFields = "lcId,pageName",
        indexes = {
                @Index(name = "ix_tb_ecs_2d_page_1", columnList = "lc_id,page_index"),
                @Index(name = "ix_tb_ecs_2d_page_2", columnList = "lc_id,eq_group_id,floor_level")
        }
)
public class TbEcs2dPage extends ElidomStampHook {

    private static final long serialVersionUID = 159837402918374650L;

    @PrimaryKey
    @Column(name = "id", length = 40)
    private String id;

    /**
     * 센터 ID (물류센터 식별자)
     */
    @Column(name = "lc_id", length = 30)
    private String lcId;

    /**
     * 설비 그룹 ID (TbEqGroupMst.eqGroupId 참조)
     */
    @Column(name = "eq_group_id", length = 50)
    private String eqGroupId;

    /**
     * 페이지 인덱스 (정렬 순서)
     */
    @Column(name = "page_index")
    private Integer pageIndex;

    /**
     * 페이지 이름 (예: "1층", "2층", "A구역")
     */
    @Column(name = "page_name", length = 100)
    private String pageName;

    /**
     * 층 번호 (필터링용)
     */
    @Column(name = "floor_level")
    private Integer floorLevel;

    /**
     * 구역 코드 (필터링용)
     */
    @Column(name = "zone_code", length = 50)
    private String zoneCode;

    /**
     * 캔버스 가로 길이 (픽셀)
     */
    @Column(name = "canvas_width")
    private Integer canvasWidth;

    /**
     * 캔버스 세로 길이 (픽셀)
     */
    @Column(name = "canvas_height")
    private Integer canvasHeight;

    /**
     * 배경 색상 (HEX)
     */
    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    /**
     * 배경 이미지 URL
     */
    @Column(name = "background_image", length = 500)
    private String backgroundImage;

    /**
     * 그리드 표시 여부
     */
    @Column(name = "show_grid")
    private Boolean showGrid;

    /**
     * 그리드 크기 (픽셀)
     */
    @Column(name = "grid_size")
    private Integer gridSize;

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

    /**
     * 설명
     */
    @Column(name = "snap_to_grid", length = 1)
    private Boolean snapToGrid;

    /**
     * 생성 전 기본값 설정
     */
    @Override
    public void beforeCreate() {
        super.beforeCreate();
        if (this.canvasWidth == null) this.canvasWidth = 1920;
        if (this.canvasHeight == null) this.canvasHeight = 1080;
        if (this.backgroundColor == null) this.backgroundColor = "#FFFFFF";
        if (this.showGrid == null) this.showGrid = true;
        if (this.gridSize == null) this.gridSize = 50;
        if (this.isActive == null) this.isActive = true;
        if (this.pageIndex == null) this.pageIndex = 0;
    }
}
