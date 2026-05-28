package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.service.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.entity.TbEcs2dItem;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 4-Way Shuttle 설비 레이아웃 Service
 */
@Service
public class TbEcs2dItemService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbEcs2dItemService.class);

    private IQueryManager queryManager;

    private IQueryManager getQueryManager() {
        if (this.queryManager == null) {
            this.queryManager = BeanUtil.get(IQueryManager.class);
        }
        return this.queryManager;
    }

    /**
     * 페이지별 레이아웃 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEcs2dItem> getLayoutsByPageId(String lcId, String pageId) {
        String sql = "SELECT * FROM tb_ecs_2d_item WHERE lc_id = :lcId AND page_id = :pageId ORDER BY z_index ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId,pageId", lcId, pageId);
        return this.getQueryManager().selectListBySql(sql, params, TbEcs2dItem.class, 0, 0);
    }

    /**
     * 설비별 레이아웃 조회
     */
    @Transactional(readOnly = true)
    public TbEcs2dItem getLayoutByEquipmentId(String pageId, String equipmentId) {
        Map<String, Object> params = ValueUtil.newMap("pageId,equipmentId", pageId, equipmentId);
        return this.getQueryManager().selectByCondition(TbEcs2dItem.class, params);
    }

    /**
     * 레이아웃 단건 조회
     */
    @Transactional(readOnly = true)
    public TbEcs2dItem getLayout(String id) {
        return this.getQueryManager().select(TbEcs2dItem.class, id);
    }

    /**
     * 레이아웃 생성 (설비 배치)
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItem createLayout(TbEcs2dItem layout) {
        if (layout.getPosX() != null) layout.setPosX((double) Math.round(layout.getPosX()));
        if (layout.getPosY() != null) layout.setPosY((double) Math.round(layout.getPosY()));
        this.getQueryManager().insert(layout);
        return layout;
    }

    /**
     * 레이아웃 수정
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItem updateLayout(TbEcs2dItem layout) {
        this.getQueryManager().update(layout);
        return layout;
    }

    /**
     * 레이아웃 위치 수정 (드래그)
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItem updateLayoutPosition(String id, Double posX, Double posY) {
        TbEcs2dItem layout = this.getLayout(id);
        if (layout != null) {
            // 🔥 전달받은 값을 정수로 반올림하여 설정 (소수점 제거)
            if (posX != null) layout.setPosX((double) Math.round(posX));
            if (posY != null) layout.setPosY((double) Math.round(posY));

            this.getQueryManager().update(layout, "posX", "posY");
        }
        return layout;
    }

    /**
     * 레이아웃 크기 수정
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItem updateLayoutSize(String id, Double width, Double height) {
        TbEcs2dItem layout = this.getLayout(id);
        if (layout != null) {
            layout.setWidth(width);
            layout.setHeight(height);
            this.getQueryManager().update(layout, "width","height");
        }
        return layout;
    }

    /**
     * 레이아웃 변형 수정 (회전, 스케일, 플립)
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItem updateLayoutTransform(String id, Double rotation, Double scaleX, Double scaleY, Boolean flipH, Boolean flipV) {
        TbEcs2dItem layout = this.getLayout(id);
        if (layout != null) {
            if (rotation != null) layout.setRotation(rotation);
            if (scaleX != null) layout.setScaleX(scaleX);
            if (scaleY != null) layout.setScaleY(scaleY);
            if (flipH != null) layout.setFlipH(flipH);
            if (flipV != null) layout.setFlipV(flipV);
            this.getQueryManager().update(layout, "rotation","scaleX","scaleY","flipH","flipV");
        }
        return layout;
    }

    /**
     * 레이아웃 삭제
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLayout(String id) {
        TbEcs2dItem layout = this.getLayout(id);
        if (layout != null) {
            this.getQueryManager().delete(layout);
        }
    }

    /**
     * 페이지의 전체 레이아웃 삭제
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLayoutsByPageId(String pageId) {
        String sql = "DELETE FROM tb_ecs_2d_item WHERE page_id = :pageId";
        Map<String, Object> params = ValueUtil.newMap("pageId", pageId);
        this.getQueryManager().executeBySql(sql, params);
    }

    /**
     * 레이아웃 일괄 저장 (페이지 전체 저장)
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLayouts(String lcId, String pageId, List<TbEcs2dItem> layouts) {
        // 1. 기존 레이아웃 모두 삭제
        this.deleteLayoutsByPageId(pageId);

        // 2. 새 레이아웃 일괄 삽입
        for (TbEcs2dItem layout : layouts) {
            layout.setLcId(lcId);
            layout.setPageId(pageId);
            if (layout.getPosX() != null) layout.setPosX((double) Math.round(layout.getPosX()));
            if (layout.getPosY() != null) layout.setPosY((double) Math.round(layout.getPosY()));
            this.getQueryManager().insert(layout);
        }

        logger.info("Saved {} layouts for lcId: {}, pageId: {}", layouts.size(), lcId, pageId);
    }

    /**
     * 레이아웃 일괄 업데이트 (변경된 항목만)
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateLayouts(List<TbEcs2dItem> layouts) {
        for (TbEcs2dItem layout : layouts) {
            // 🔥 저장 전 좌표 보정
            if (layout.getPosX() != null) layout.setPosX((double) Math.round(layout.getPosX()));
            if (layout.getPosY() != null) layout.setPosY((double) Math.round(layout.getPosY()));

            if (layout.getId() != null) {
                this.getQueryManager().update(layout);
            } else {
                this.getQueryManager().insert(layout);
            }
        }
    }

    /**
     * 레이아웃에 실운영 설비 ID 매핑
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItem updateRealEqMapping(String layoutId, String realEqId, String realEqType) {
        TbEcs2dItem layout = this.getLayout(layoutId);
        if (layout != null) {
            layout.setRealEqId(realEqId);
            layout.setRealEqType(realEqType);
            this.getQueryManager().update(layout, "realEqId","realEqType");
            logger.info("Updated real equipment mapping for layout {}: realEqId={}, realEqType={}",
                layoutId, realEqId, realEqType);
        }
        return layout;
    }

    /**
     * 레이아웃에 실운영 설비 ID 매핑 해제
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dItem clearRealEqMapping(String layoutId) {
        return this.updateRealEqMapping(layoutId, null, null);
    }

    /**
     * 대시보드용 레이아웃 조회 (실시간 상태 결합)
     * - 레이아웃 + 실운영 설비 상태 JOIN
     */
    /**
     * 대시보드용 레이아웃 조회 (실시간 상태 결합)
     * - pageId/lcId로 page를 먼저 1건 확정
     * - page.eq_group_id에 속한 tb_eq_mst만 먼저 제한
     * - 그 eq_mst.id 목록으로 rack/cv/car를 제한해서 다른 page/다른 group 데이터 유입 방지
     */
    @Transactional(readOnly = true)
    public List<Map> getLayoutsWithRealStatus(String lcId, String pageId) {

        String sql = """
        WITH target_page AS (
            SELECT p.id, p.lc_id, p.eq_group_id
            FROM tb_ecs_2d_page p
            WHERE p.id = :pageId
              AND p.lc_id = :lcId
        ),
        target_eq AS (
            SELECT e.id, e.eq_group_id, e.type, e.name
            FROM tb_eq_mst e
            INNER JOIN target_page tp
                    ON tp.eq_group_id = e.eq_group_id
        )

        SELECT
            el.*,
            tp.eq_group_id AS page_eq_group_id,

            /* 공통 eq mst */
            COALESCE(eq_rack.id, eq_cv.id, eq_car.id) AS real_eq_mst_id,
            COALESCE(eq_rack.eq_group_id, eq_cv.eq_group_id, eq_car.eq_group_id) AS real_eq_group_id,
            COALESCE(eq_rack.type, eq_cv.type, eq_car.type) AS real_eq_mst_type,
            COALESCE(eq_rack.name, eq_cv.name, eq_car.name) AS real_eq_mst_name,

            /* CAR realtime */
            car.id                 AS real_car_id,
            car.eq_id              AS real_car_eq_id,
            car.type               AS real_car_type,
            car.row                AS real_car_row,
            car.bay                AS real_car_bay,
            car.level              AS real_car_level,
            car.status             AS real_car_status,
            car.battery_status     AS real_car_battery_status,
            car.cargo_yn           AS real_car_cargo_yn,
            car.error_id           AS real_car_error_id,
            car.error_desc         AS real_car_error_desc,

            /* RACK realtime */
            rack.id                AS real_rack_id,
            rack.eq_id             AS real_rack_eq_id,
            rack.type              AS real_rack_type,
            rack.row               AS real_rack_row,
            rack.bay               AS real_rack_bay,
            rack.level             AS real_rack_level,
            rack.sku_id            AS real_rack_sku_id,
            rack.sku_qty           AS real_rack_sku_qty,
            rack.status            AS real_rack_status,
            rack.error_id          AS real_rack_error_id,
            rack.error_desc        AS real_rack_error_desc,
            rack.cargo_yn          AS real_rack_cargo_yn,
            rack.drive_only_yn     AS real_rack_drive_only_yn,

            /* CONVEYOR / LIFTER realtime */
            cv.id                  AS real_cv_id,
            cv.eq_id               AS real_cv_eq_id,
            cv.type                AS real_cv_type,
            cv.level               AS real_cv_level,
            cv.cargo_yn            AS real_cv_cargo_yn,
            cv.status              AS real_cv_status,
            cv.error_id            AS real_cv_error_id,
            cv.error_desc          AS real_cv_error_desc

        FROM target_page tp
        INNER JOIN tb_ecs_2d_item el
                ON el.page_id = tp.id
               AND el.lc_id = tp.lc_id

        /* =========================
           RACK
           - el.real_eq_id = rack.id
           - rack.eq_id 는 반드시 target_eq.id 중 하나여야 함
           ========================= */
        LEFT JOIN tb_eq_rack_mst rack
               ON el.equipment_type_code = 'RACK'
              AND rack.id = el.real_eq_id
              AND rack.eq_id IN (SELECT id FROM target_eq)

        LEFT JOIN target_eq eq_rack
               ON eq_rack.id = rack.eq_id

        /* =========================
           CONVEYOR / LIFTER
           - el.real_eq_id = cv.id
           - cv.eq_id 는 반드시 target_eq.id 중 하나여야 함
           ========================= */
        LEFT JOIN tb_eq_cv_mst cv
               ON el.equipment_type_code IN ('CONVEYOR', 'LIFTER')
              AND cv.id = el.real_eq_id
              AND cv.eq_id IN (SELECT id FROM target_eq)

        LEFT JOIN target_eq eq_cv
               ON eq_cv.id = cv.eq_id

        /* =========================
           SHUTTLE_CAR
           - el.real_eq_id = car.id
           - car.eq_id 는 반드시 target_eq.id 중 하나여야 함
           ========================= */
        LEFT JOIN tb_eq_car_mst car
               ON el.equipment_type_code = 'SHUTTLE_CAR'
              AND car.id = el.real_eq_id
              AND car.eq_id IN (SELECT id FROM target_eq)

        LEFT JOIN target_eq eq_car
               ON eq_car.id = car.eq_id

        WHERE el.is_visible = true
        ORDER BY el.z_index ASC, el.real_eq_id ASC
        """;

        Map<String, Object> params = ValueUtil.newMap("lcId,pageId", lcId, pageId);
        return this.getQueryManager().selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * realEqId로 레이아웃 조회
     */
    @Transactional(readOnly = true)
    public TbEcs2dItem getLayoutByRealEqId(String lcId, String pageId, String realEqId) {
        String sql = "SELECT * FROM tb_ecs_2d_item WHERE lc_id = :lcId AND page_id = :pageId AND real_eq_id = :realEqId";
        Map<String, Object> params = ValueUtil.newMap("lcId,pageId,realEqId", lcId, pageId, realEqId);
        return this.getQueryManager().selectByCondition(TbEcs2dItem.class, params);
    }

    /**
     * 특정 페이지에서 realEqId가 매핑된 레이아웃 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEcs2dItem> getMappedLayouts(String lcId, String pageId) {
        String sql = "SELECT * FROM tb_ecs_2d_item WHERE lc_id = :lcId AND page_id = :pageId AND real_eq_id IS NOT NULL ORDER BY z_index ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId,pageId", lcId, pageId);
        return this.getQueryManager().selectListBySql(sql, params, TbEcs2dItem.class, 0, 0);
    }
}
