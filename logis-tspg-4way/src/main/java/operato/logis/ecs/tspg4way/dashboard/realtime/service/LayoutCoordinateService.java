package operato.logis.ecs.tspg4way.dashboard.realtime.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import operato.logis.ecs.tspg4way.dashboard.entity.TbEcs2dItem;
import operato.logis.ecs.tspg4way.dashboard.service.impl.TbEcs2dItemService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 레이아웃 좌표 변환 서비스. 장비/셀 ID → 레이아웃 중앙 픽셀 좌표 매핑을 페이지 단위로 캐싱.
 */
@Service
public class LayoutCoordinateService {

    private static final Logger logger = LoggerFactory.getLogger(LayoutCoordinateService.class);

    @Autowired
    private TbEcs2dItemService layoutService;

    /**
     * 좌표 캐시: realEqId → Point(posX, posY)
     * Key: "pageId:realEqId"
     */
    private final Map<String, double[]> coordinateCache = new ConcurrentHashMap<>();

    /**
     * 현재 로드된 페이지 ID
     */
    private volatile String currentPageId;

    /**
     * 현재 로드된 LC ID
     */
    private volatile String currentLcId;

    /**
     * 레이아웃 좌표 데이터 로드
     * Dashboard 페이지 선택 시 호출
     *
     * @param lcId   센터 ID
     * @param pageId 페이지 ID
     */
    public void loadLayoutCoordinates(String lcId, String pageId) {
        logger.info("[ Realtime ][ Layout ] loading: lcId={}, pageId={}", lcId, pageId);

        // 기존 캐시 클리어 (다른 페이지)
        if (!pageId.equals(currentPageId)) {
            coordinateCache.clear();
        }

        try {
            // 레이아웃 아이템 조회
            List<TbEcs2dItem> layouts = layoutService.getLayoutsByPageId(lcId, pageId);

            int mappedCount = 0;
            for (TbEcs2dItem layout : layouts) {
                String realEqId = layout.getRealEqId();
                if (realEqId != null && !realEqId.isEmpty()) {
                    String cacheKey = buildCacheKey(pageId, realEqId);
                    double posX = layout.getPosX() != null ? layout.getPosX() : 0;
                    double posY = layout.getPosY() != null ? layout.getPosY() : 0;

                    double width = layout.getWidth() != null ? layout.getWidth() : 0;
                    double height = layout.getHeight() != null ? layout.getHeight() : 0;

                    // 좌측하단 → 중앙 좌표 변환
                    double centerX = posX + width / 2.0;
                    double centerY = posY + height / 2.0;

                    double[] coords = new double[]{
                            centerX,
                            centerY
                    };
                    coordinateCache.put(cacheKey, coords);
                    mappedCount++;
                }
            }

            currentLcId = lcId;
            currentPageId = pageId;

            logger.info("[ Realtime ][ Layout ] loaded: count={}, pageId={}", mappedCount, pageId);
        } catch (Exception e) {
            logger.error("[ Realtime ][ Layout ] load failed: pageId={}", pageId, e);
        }
    }

    /**
     * 장비/셀 X 좌표 조회
     *
     * @param eqId 장비 또는 셀 ID
     * @return X 좌표 (매핑 없으면 0)
     */
    public double getCellPosX(String eqId) {
        if (eqId == null || currentPageId == null) {
            return 0;
        }
        String cacheKey = buildCacheKey(currentPageId, eqId);
        double[] coords = coordinateCache.get(cacheKey);
        return coords != null ? coords[0] : 0;
    }

    /**
     * 장비/셀 Y 좌표 조회
     *
     * @param eqId 장비 또는 셀 ID
     * @return Y 좌표 (매핑 없으면 0)
     */
    public double getCellPosY(String eqId) {
        if (eqId == null || currentPageId == null) {
            return 0;
        }
        String cacheKey = buildCacheKey(currentPageId, eqId);
        double[] coords = coordinateCache.get(cacheKey);
        return coords != null ? coords[1] : 0;
    }

    /**
     * 장비 X 좌표 조회 (getCellPosX 별칭)
     */
    public double getEquipmentPosX(String eqId) {
        return getCellPosX(eqId);
    }

    /**
     * 장비 Y 좌표 조회 (getCellPosY 별칭)
     */
    public double getEquipmentPosY(String eqId) {
        return getCellPosY(eqId);
    }

    /**
     * 좌표 매핑 존재 여부
     */
    public boolean hasCoordinate(String eqId) {
        if (eqId == null || currentPageId == null) {
            return false;
        }
        String cacheKey = buildCacheKey(currentPageId, eqId);
        return coordinateCache.containsKey(cacheKey);
    }

    /**
     * 현재 캐시된 좌표 수
     */
    public int getCachedCoordinateCount() {
        return coordinateCache.size();
    }

    /**
     * 현재 페이지 ID
     */
    public String getCurrentPageId() {
        return currentPageId;
    }

    /**
     * 캐시 클리어
     */
    public void clearCache() {
        coordinateCache.clear();
        currentPageId = null;
        currentLcId = null;
        logger.debug("[ Realtime ][ Layout ] cache cleared");
    }

    /**
     * 캐시 키 생성
     */
    private String buildCacheKey(String pageId, String eqId) {
        return pageId + ":" + eqId;
    }

    /**
     * 현재 페이지에 매핑된 모든 realEqId 목록 반환
     * CargoDataProvider 등에서 페이지별 필터링에 사용
     *
     * @return 현재 페이지에 매핑된 realEqId Set (없으면 빈 Set)
     */
    public Set<String> getMappedRealEqIds() {
        if (currentPageId == null) {
            return java.util.Collections.emptySet();
        }

        Set<String> result = new java.util.HashSet<>();
        String prefix = currentPageId + ":";

        for (String key : coordinateCache.keySet()) {
            if (key.startsWith(prefix)) {
                // "pageId:realEqId" 형태에서 realEqId 추출
                String realEqId = key.substring(prefix.length());
                result.add(realEqId);
            }
        }

        logger.debug("[ Realtime ][ Layout ] mapped realEqIds: pageId={}, count={}", currentPageId, result.size());

        return result;
    }

    /**
     * 특정 eqId가 현재 페이지에 매핑되어 있는지 확인
     *
     * @param eqId 확인할 설비 ID
     * @return 매핑 여부
     */
    public boolean isMappedToCurrentPage(String eqId) {
        if (eqId == null || currentPageId == null) {
            return false;
        }
        return hasCoordinate(eqId);
    }
}
