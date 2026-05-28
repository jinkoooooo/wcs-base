package operato.logis.ecs.base.ecs.dashboard.service.impl;

import operato.logis.ecs.base.ecs.dashboard.entity.TbEcs2dItem;
import operato.logis.ecs.base.ecs.dashboard.entity.TbEcs2dPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 4-Way Shuttle 레이아웃 페이지 Service */
@Service
public class TbEcs2dPageService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbEcs2dPageService.class);

    // 캔버스 크기 제한
    private static final int MIN_CANVAS_SIZE = 100;
    private static final int MAX_CANVAS_SIZE = 10000;
    private static final int MIN_GRID_SIZE = 5;
    private static final int MAX_GRID_SIZE = 100;

    /** 센터별 전체 페이지 목록 조회 */
    @Transactional(readOnly = true)
    public List<TbEcs2dPage> getPagesByLcId(String lcId) {
        Map<String, Object> params = ValueUtil.newMap("lcId", lcId);
        return this.queryManager.selectList(TbEcs2dPage.class, params);
    }

    /** 센터별 활성화된 페이지 목록 조회 (정렬 포함) */
    @Transactional(readOnly = true)
    public List<TbEcs2dPage> getActivePagesByLcId(String lcId) {
        String sql = "SELECT * FROM tb_ecs_2d_page WHERE lc_id = :lcId AND is_active = true ORDER BY page_index ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId", lcId);
        return this.queryManager.selectListBySql(sql, params, TbEcs2dPage.class, 0, 0);
    }

    /** 설비그룹별 페이지 목록 조회 */
    @Transactional(readOnly = true)
    public List<TbEcs2dPage> getPagesByEqGroup(String lcId, String eqGroupId) {
        String sql = "SELECT * FROM tb_ecs_2d_page WHERE lc_id = :lcId AND eq_group_id = :eqGroupId AND is_active = true ORDER BY floor_level ASC, page_index ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId", lcId, eqGroupId);
        return this.queryManager.selectListBySql(sql, params, TbEcs2dPage.class, 0, 0);
    }

    /** 설비그룹 + 층별 페이지 목록 조회 */
    @Transactional(readOnly = true)
    public List<TbEcs2dPage> getPagesByEqGroupAndFloor(String lcId, String eqGroupId, Integer floorLevel) {
        String sql = "SELECT * FROM tb_ecs_2d_page WHERE lc_id = :lcId AND eq_group_id = :eqGroupId AND floor_level = :floorLevel AND is_active = true ORDER BY page_index ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId,floorLevel", lcId, eqGroupId, floorLevel);
        return this.queryManager.selectListBySql(sql, params, TbEcs2dPage.class, 0, 0);
    }

    /** 설비그룹별 층 목록 조회 (중복 제거) */
    @Transactional(readOnly = true)
    public List<Integer> getFloorsByEqGroup(String lcId, String eqGroupId) {
        String sql = "SELECT DISTINCT floor_level FROM tb_ecs_2d_page WHERE lc_id = :lcId AND eq_group_id = :eqGroupId AND floor_level IS NOT NULL AND is_active = true ORDER BY floor_level ASC";
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId", lcId, eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Integer.class, 0, 0);
    }

    /** 페이지 단건 조회 */
    @Transactional(readOnly = true)
    public TbEcs2dPage getPage(String id) {
        return this.queryManager.select(TbEcs2dPage.class, id);
    }

    /**
     * 페이지 생성
     * - eqGroupId는 선택사항 (나중에 매핑 가능)
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage createPage(TbEcs2dPage page) {
        logger.info("createPage : {}", page);

        if (ValueUtil.isEmpty(page)) {
            throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, "page");
        }
        if (ValueUtil.isEmpty(page.getLcId())) {
            throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, "lcId");
        }
        if (ValueUtil.isEmpty(page.getPageName())) {
            throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, "pageName");
        }

        // ✅ floorLevel 없으면 1층 기본
        if (page.getFloorLevel() == null) {
            page.setFloorLevel(1);
        }

        // ✅ eqGroupId가 있을 때만 중복 체크 / 자동 산정
        String eqGroupId = page.getEqGroupId();
        if (ValueUtil.isNotEmpty(eqGroupId)) {
            if (page.getPageIndex() != null) {
                if (this.existsPageByIndex(page.getLcId(), eqGroupId, page.getFloorLevel(), page.getPageIndex())) {
                    throw ThrowUtil.newValidationErrorWithNoLog(
                            String.format("Page index %d already exists for lcId=%s eqGroupId=%s floor=%d",
                                    page.getPageIndex(), page.getLcId(), eqGroupId, page.getFloorLevel())
                    );
                }
            } else {
                Integer maxIndex = this.getMaxPageIndex(page.getLcId(), eqGroupId, page.getFloorLevel());
                page.setPageIndex(maxIndex + 1);
            }
        } else {
            // eqGroupId가 없으면 lcId 기준으로만 pageIndex 산정
            if (page.getPageIndex() == null) {
                Integer maxIndex = this.getMaxPageIndexByLcId(page.getLcId());
                page.setPageIndex(maxIndex + 1);
            }
        }

        // 기본값 설정
        if (page.getCanvasWidth() == null) page.setCanvasWidth(1920);
        if (page.getCanvasHeight() == null) page.setCanvasHeight(1080);
        if (page.getGridSize() == null) page.setGridSize(20);
        if (page.getShowGrid() == null) page.setShowGrid(true);
        if (page.getSnapToGrid() == null) page.setSnapToGrid(true);
        if (page.getIsActive() == null) page.setIsActive(true);
        if (page.getBackgroundColor() == null) page.setBackgroundColor("#2d3748");

        logger.info("Creating new page: lcId={}, eqGroupId={}, floor={}, name={}, index={}",
                page.getLcId(), page.getEqGroupId(), page.getFloorLevel(), page.getPageName(), page.getPageIndex());

        this.queryManager.insert(page);
        return page;
    }

    /** lcId 기준 최대 페이지 인덱스 조회 */
    private Integer getMaxPageIndexByLcId(String lcId) {
        String sql = "SELECT COALESCE(MAX(page_index), -1) FROM tb_ecs_2d_page WHERE lc_id = :lcId";
        Map<String, Object> params = ValueUtil.newMap("lcId", lcId);
        Object result = this.queryManager.selectBySql(sql, params, Integer.class);
        return result != null ? (Integer) result : -1;
    }

    /** 페이지 인덱스 존재 여부 확인 (lcId + eqGroupId + floorLevel 범위) */
    private boolean existsPageByIndex(String lcId, String eqGroupId, Integer floorLevel, Integer pageIndex) {
        String sql =
                "SELECT COUNT(*) FROM tb_ecs_2d_page " +
                        "WHERE lc_id = :lcId AND eq_group_id = :eqGroupId AND floor_level = :floorLevel AND page_index = :pageIndex";
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId,floorLevel,pageIndex",
                lcId, eqGroupId, floorLevel, pageIndex);
        Object result = this.queryManager.selectBySql(sql, params, Long.class);
        return result != null && ((Number) result).longValue() > 0;
    }

    /** 페이지 수정 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage updatePage(TbEcs2dPage page) {
        this.queryManager.update(page);
        return page;
    }

    /** 페이지 이름 수정 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage updatePageName(String id, String pageName) {
        TbEcs2dPage page = this.getPage(id);
        if (page != null) {
            page.setPageName(pageName);
            this.queryManager.update(page, "pageName");
        }
        return page;
    }

    /** 페이지 설비그룹 매핑 업데이트 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage updatePageEqGroup(String id, String eqGroupId) {
        TbEcs2dPage page = this.getPage(id);
        if (page == null) {
            throw ThrowUtil.newNotFoundRecord("TbEcs2dPage", id);
        }

        logger.info("Updating eq group mapping for page {}: eqGroupId={}", id, eqGroupId);
        page.setEqGroupId(eqGroupId);
        this.queryManager.update(page, "eqGroupId");
        return page;
    }

    /** 페이지 설비그룹 매핑 해제 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage clearPageEqGroup(String id) {
        TbEcs2dPage page = this.getPage(id);
        if (page == null) {
            throw ThrowUtil.newNotFoundRecord("TbEcs2dPage", id);
        }

        logger.info("Clearing eq group mapping for page {}", id);
        page.setEqGroupId(null);
        this.queryManager.update(page, "eqGroupId");
        return page;
    }

    /** 페이지 캔버스 설정 수정 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage updatePageCanvas(String id, Integer canvasWidth, Integer canvasHeight, String backgroundColor) {
        if (id == null || id.isEmpty()) {
            throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, "id");
        }

        // 캔버스 크기 검증
        if (canvasWidth != null && (canvasWidth < MIN_CANVAS_SIZE || canvasWidth > MAX_CANVAS_SIZE)) {
            throw ThrowUtil.newValidationErrorWithNoLog(
                    String.format("Canvas width must be between %d and %d", MIN_CANVAS_SIZE, MAX_CANVAS_SIZE));
        }
        if (canvasHeight != null && (canvasHeight < MIN_CANVAS_SIZE || canvasHeight > MAX_CANVAS_SIZE)) {
            throw ThrowUtil.newValidationErrorWithNoLog(
                    String.format("Canvas height must be between %d and %d", MIN_CANVAS_SIZE, MAX_CANVAS_SIZE));
        }

        // 배경색 형식 검증 (HEX 색상 코드)
        if (backgroundColor != null && !backgroundColor.isEmpty()) {
            if (!backgroundColor.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                throw ThrowUtil.newValidationErrorWithNoLog("Invalid background color format. Use HEX format like #FFFFFF");
            }
        }

        TbEcs2dPage page = this.getPage(id);
        if (page == null) {
            throw ThrowUtil.newNotFoundRecord("TbEcs2dPage", id);
        }

        logger.info("Updating canvas settings for page {}: width={}, height={}, bgColor={}",
                id, canvasWidth, canvasHeight, backgroundColor);

        if (canvasWidth != null) page.setCanvasWidth(canvasWidth);
        if (canvasHeight != null) page.setCanvasHeight(canvasHeight);
        if (backgroundColor != null) page.setBackgroundColor(backgroundColor);
        this.queryManager.update(page, "canvasWidth", "canvasHeight", "backgroundColor");

        return page;
    }

    /** 페이지 그리드 설정 수정 */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage updatePageGrid(String id, Integer gridSize, Boolean showGrid, Boolean snapToGrid) {
        if (id == null || id.isEmpty()) {
            throw ThrowUtil.newValidationErrorWithNoLog(SysMessageConstants.VALUE_IS_EMPTY, "id");
        }

        // 그리드 크기 검증
        if (gridSize != null && (gridSize < MIN_GRID_SIZE || gridSize > MAX_GRID_SIZE)) {
            throw ThrowUtil.newValidationErrorWithNoLog(
                    String.format("Grid size must be between %d and %d", MIN_GRID_SIZE, MAX_GRID_SIZE));
        }

        TbEcs2dPage page = this.getPage(id);
        if (page == null) {
            throw ThrowUtil.newNotFoundRecord("TbEcs2dPage", id);
        }

        logger.debug("Updating grid settings for page {}: gridSize={}, showGrid={}, snapToGrid={}",
                id, gridSize, showGrid, snapToGrid);

        if (gridSize != null) page.setGridSize(gridSize);
        if (showGrid != null) page.setShowGrid(showGrid);
        if (snapToGrid != null) page.setSnapToGrid(snapToGrid);
        this.queryManager.update(page, "gridSize", "showGrid", "snapToGrid");

        return page;
    }

    /** 페이지 삭제 (연관된 레이아웃도 함께 삭제) */
    @Transactional(rollbackFor = Exception.class)
    public void deletePage(String id) {
        if (id == null || id.isEmpty()) {
            logger.warn("Delete page called with null/empty id");
            return;
        }

        TbEcs2dPage page = this.getPage(id);
        if (page == null) {
            logger.debug("Page not found for deletion: {}", id);
            return;
        }

        String lcId = page.getLcId();
        String eqGroupId = page.getEqGroupId();
        Integer floorLevel = page.getFloorLevel();
        Integer pageIndex = page.getPageIndex();

        logger.info("Deleting page: {} (name: {}, lcId: {}, eqGroupId: {}, floor: {}, index: {})",
                id, page.getPageName(), lcId, eqGroupId, floorLevel, pageIndex);

        // 1) page 내 레이아웃 요소 삭제
        String deleteItemsSql = "DELETE FROM tb_ecs_2d_item WHERE page_id = :pageId";
        Map<String, Object> itemParams = ValueUtil.newMap("pageId", id);
        int deletedItems = this.queryManager.executeBySql(deleteItemsSql, itemParams);
        logger.debug("Deleted {} items for page {}", deletedItems, id);

        // 2) 페이지 삭제
        this.queryManager.delete(page);

        // 3) ✅ 삭제된 페이지 이후의 인덱스 재정렬 (같은 eqGroupId + floorLevel 범위에서만)
        this.reorderPagesAfterDelete(lcId, eqGroupId, floorLevel, pageIndex);
    }

    /** 페이지 삭제 후 인덱스 재정렬 (lcId + eqGroupId + floorLevel 범위) */
    private void reorderPagesAfterDelete(String lcId, String eqGroupId, Integer floorLevel, Integer deletedIndex) {
        if (deletedIndex == null) return;

        String sql =
                "UPDATE tb_ecs_2d_page SET page_index = page_index - 1 " +
                        "WHERE lc_id = :lcId AND eq_group_id = :eqGroupId AND floor_level = :floorLevel " +
                        "AND page_index > :deletedIndex";

        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId,floorLevel,deletedIndex",
                lcId, eqGroupId, floorLevel, deletedIndex);

        this.queryManager.executeBySql(sql, params);
    }

    /** 페이지 순서 변경 */
    @Transactional(rollbackFor = Exception.class)
    public void updatePageIndex(String id, Integer newIndex) {
        TbEcs2dPage page = this.getPage(id);
        if (page != null) {
            page.setPageIndex(newIndex);
            this.queryManager.update(page, "pageIndex");
        }
    }

    /** 최대 페이지 인덱스 조회 (lcId + eqGroupId + floorLevel 범위) */
    private Integer getMaxPageIndex(String lcId, String eqGroupId, Integer floorLevel) {
        String sql =
                "SELECT COALESCE(MAX(page_index), -1) FROM tb_ecs_2d_page " +
                        "WHERE lc_id = :lcId AND eq_group_id = :eqGroupId AND floor_level = :floorLevel";
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId,floorLevel", lcId, eqGroupId, floorLevel);
        Object result = this.queryManager.selectBySql(sql, params, Integer.class);
        return result != null ? (Integer) result : -1;
    }

    /**
     * 페이지 복사 (층 복사용)
     *
     * 원본 페이지의 설정과 모든 레이아웃을 새 페이지로 복사합니다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbEcs2dPage copyPage(String sourcePageId, String newPageName, Integer newFloorLevel) {
        // 1. 원본 페이지 조회
        TbEcs2dPage sourcePage = this.getPage(sourcePageId);
        if (sourcePage == null) {
            throw ThrowUtil.newNotFoundRecord("TbEcs2dPage", sourcePageId);
        }

        // ✅ floorLevel 없으면 원본 그대로, 그래도 null이면 1
        Integer targetFloor = (newFloorLevel != null) ? newFloorLevel : sourcePage.getFloorLevel();
        if (targetFloor == null) targetFloor = 1;

        // 2. 새 페이지 생성
        TbEcs2dPage newPage = new TbEcs2dPage();
        newPage.setLcId(sourcePage.getLcId());
        newPage.setEqGroupId(sourcePage.getEqGroupId()); // ✅ 반드시 복사
        newPage.setPageName(newPageName != null ? newPageName : sourcePage.getPageName() + " (복사)");
        newPage.setFloorLevel(targetFloor);
        newPage.setZoneCode(sourcePage.getZoneCode());
        newPage.setCanvasWidth(sourcePage.getCanvasWidth());
        newPage.setCanvasHeight(sourcePage.getCanvasHeight());
        newPage.setBackgroundColor(sourcePage.getBackgroundColor());
        newPage.setBackgroundImage(sourcePage.getBackgroundImage());
        newPage.setShowGrid(sourcePage.getShowGrid());
        newPage.setGridSize(sourcePage.getGridSize());
        newPage.setSnapToGrid(sourcePage.getSnapToGrid());
        newPage.setIsActive(true);
        newPage.setDescription(sourcePage.getDescription());

        // ✅ pageIndex 자동 설정: lcId + eqGroupId + floorLevel 범위
        Integer maxIndex = this.getMaxPageIndex(sourcePage.getLcId(), sourcePage.getEqGroupId(), targetFloor);
        newPage.setPageIndex(maxIndex + 1);

        this.queryManager.insert(newPage);

        logger.info("Created new page by copy: {} (lcId={}, eqGroupId={}, floor={}) from source: {}",
                newPage.getPageName(), newPage.getLcId(), newPage.getEqGroupId(), newPage.getFloorLevel(), sourcePageId);

        // 3. 원본 페이지의 모든 레이아웃 조회
        String selectSql = "SELECT * FROM tb_ecs_2d_item WHERE page_id = :pageId";
        Map<String, Object> params = ValueUtil.newMap("pageId", sourcePageId);
        List<TbEcs2dItem> sourceLayouts =
                this.queryManager.selectListBySql(selectSql, params, TbEcs2dItem.class, 0, 0);

        // 4. 레이아웃 복사
        int copiedCount = 0;
        for (TbEcs2dItem sourceLayout : sourceLayouts) {
            TbEcs2dItem newLayout = new TbEcs2dItem();
            newLayout.setId(UUID.randomUUID().toString());
            newLayout.setLcId(sourceLayout.getLcId());
            newLayout.setPageId(newPage.getId());
            newLayout.setEquipmentId(sourceLayout.getEquipmentId());
            newLayout.setEquipmentCode(sourceLayout.getEquipmentCode());
            newLayout.setEquipmentTypeCode(sourceLayout.getEquipmentTypeCode());
            newLayout.setPosX(sourceLayout.getPosX());
            newLayout.setPosY(sourceLayout.getPosY());
            newLayout.setWidth(sourceLayout.getWidth());
            newLayout.setHeight(sourceLayout.getHeight());
            newLayout.setRotation(sourceLayout.getRotation());
            newLayout.setScaleX(sourceLayout.getScaleX());
            newLayout.setScaleY(sourceLayout.getScaleY());
            newLayout.setFlipH(sourceLayout.getFlipH());
            newLayout.setFlipV(sourceLayout.getFlipV());
            newLayout.setZIndex(sourceLayout.getZIndex());
            newLayout.setOpacity(sourceLayout.getOpacity());
            newLayout.setShowLabel(sourceLayout.getShowLabel());
            newLayout.setCustomLabel(sourceLayout.getCustomLabel());
            newLayout.setCustomColor(sourceLayout.getCustomColor());
            newLayout.setIsVisible(sourceLayout.getIsVisible());
            newLayout.setIsLocked(sourceLayout.getIsLocked());

            this.queryManager.insert(newLayout);
            copiedCount++;
        }

        logger.info("Copied {} layouts from page {} to new page {}", copiedCount, sourcePageId, newPage.getId());
        return newPage;
    }
}
