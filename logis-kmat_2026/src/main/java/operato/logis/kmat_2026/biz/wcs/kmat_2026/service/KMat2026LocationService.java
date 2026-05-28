package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.LocationStatus;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * KMAT 2026 로케이션 서비스
 * ============================================================================
 */
@Service
public class KMat2026LocationService {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026LocationService.class);

    @Autowired
    private TbWcsLocMstService tbWcsLocMstService;

    @Autowired
    private TbEcsLocMstService tbEcsLocMstService;

    // ========================================================================
    // 입고단 상태
    // ========================================================================

    @Transactional(readOnly = true)
    public boolean hasCargoAtInboundPort() {
        TbWcsLocMst loc = findLoc(KMat2026LocationMapping.INBOUND_PORT);
        if (loc == null) {
            logger.warn("[KMat2026LocationService] 입고단 없음: {}", KMat2026LocationMapping.INBOUND_PORT);
            return false;
        }
        boolean hasCargo = LocStatusEnumCode.OCCUPIED.code().equals(loc.getStatus());
        logger.info("[KMat2026LocationService] hasCargoAtInboundPort={} (status={})", hasCargo, loc.getStatus());
        return hasCargo;
    }

    // ========================================================================
    // 출고단 상태
    // ========================================================================

    @Transactional(readOnly = true)
    public List<String> findAvailableOutboundPorts() {
        List<String> available = new ArrayList<>();
        for (String portCode : KMat2026LocationMapping.OUTBOUND_PORTS) {
            TbWcsLocMst loc = findLoc(portCode);
            if (loc != null && isLocEmpty(loc)) available.add(portCode);
        }
        return available;
    }

    // ========================================================================
    // ECS 상태 업데이트
    // ========================================================================

    @Transactional
    public void updateEcsOutboundLocToFull(String wcsOutboundPortCode) {
        String ecsLocCode = KMat2026LocationMapping.toEcsOutboundLoc(wcsOutboundPortCode);
        if (ValueUtil.isEmpty(ecsLocCode)) {
            logger.warn("[KMat2026LocationService] ECS 출고단 매핑 없음: {}", wcsOutboundPortCode);
            return;
        }
        tbEcsLocMstService.updateLocationStatusByLocationCd(ecsLocCode, LocationStatus.FULL.getCode());
        logger.info("[KMat2026LocationService] ECS 출고단 FULL: {} → {}", wcsOutboundPortCode, ecsLocCode);
    }

    @Transactional
    public void updateEcsInboundLocToEmpty(String wcsInboundPortCode) {
        String ecsLocCode = KMat2026LocationMapping.toEcsInboundLoc(wcsInboundPortCode);
        if (ValueUtil.isEmpty(ecsLocCode)) {
            logger.warn("[KMat2026LocationService] ECS 입고단 매핑 없음: {}", wcsInboundPortCode);
            return;
        }
        tbEcsLocMstService.updateLocationStatusByLocationCd(ecsLocCode, LocationStatus.EMPTY.getCode());
        logger.info("[KMat2026LocationService] ECS 입고단 EMPTY: {} → {}", wcsInboundPortCode, ecsLocCode);
    }

    // ========================================================================
    // WCS 출고단 상태 업데이트 (AGF가 파렛트를 집어갔을 때)
    // ========================================================================

    /**
     * AGF가 출고단(10601/10602)에서 파렛트를 집어갔을 때 tb_wcs_loc_mst를 EMPTY로 업데이트
     * @param ecsOutboundLocCode ECS 출고단 코드 (TSPG_CONV_OUT_01, TSPG_CONV_OUT_02)
     */
    @Transactional
    public void updateWcsOutboundLocToEmptyByEcsLoc(String ecsOutboundLocCode) {
        String wcsLocCode = KMat2026LocationMapping.toWcsOutboundLoc(ecsOutboundLocCode);
        if (ValueUtil.isEmpty(wcsLocCode)) {
            logger.debug("[KMat2026LocationService] WCS 출고단 매핑 없음 (KMAT 외 주문): {}", ecsOutboundLocCode);
            return;
        }
        updateWcsOutboundLocToEmpty(wcsLocCode);
    }

    /**
     * WCS 출고단(10601/10602)을 EMPTY로 업데이트
     * @param wcsOutboundPortCode WCS 출고단 코드 (10601, 10602)
     */
    @Transactional
    public void updateWcsOutboundLocToEmpty(String wcsOutboundPortCode) {
        tbWcsLocMstService.updateLocStatus(
                KMat2026LocationMapping.EQ_GROUP_ID,
                wcsOutboundPortCode,
                LocStatusEnumCode.EMPTY
        );
        logger.info("[KMat2026LocationService] WCS 출고단 EMPTY: {}", wcsOutboundPortCode);
    }

    // ========================================================================
    // 상태 조회 (REST API용)
    // ========================================================================

    @Transactional(readOnly = true)
    public List<LocStatusInfo> getFloor1LocStatuses() {
        return KMat2026LocationMapping.FLOOR_1_POINTS.stream()
                .map(this::buildLocStatusInfo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocStatusInfo> getFloor2LocStatuses() {
        return KMat2026LocationMapping.FLOOR_2_POINTS.stream()
                .map(this::buildLocStatusInfo)
                .collect(Collectors.toList());
    }

    private LocStatusInfo buildLocStatusInfo(String locCode) {
        TbWcsLocMst loc = findLoc(locCode);
        if (loc == null) return new LocStatusInfo(locCode, null, false, false);
        return new LocStatusInfo(locCode, loc.getStatus(), loc.getLockYn() == 1, loc.getUseYn() == 1);
    }

    // ========================================================================
    // 헬퍼
    // ========================================================================

    private TbWcsLocMst findLoc(String locCode) {
        return tbWcsLocMstService.findByEqGroupIdAndLocCode(
                KMat2026LocationMapping.EQ_GROUP_ID, locCode);
    }

    private boolean isLocEmpty(TbWcsLocMst loc) {
        return loc.getUseYn() == 1
                && loc.getLockYn() == 0
                && LocStatusEnumCode.EMPTY.code().equals(loc.getStatus());
    }

    // ========================================================================
    // LocStatusInfo DTO
    // ========================================================================

    public static class LocStatusInfo {
        private final String  locCode;
        private final Integer  status;
        private final boolean locked;
        private final boolean useYn;

        public LocStatusInfo(String locCode, Integer status, boolean locked, boolean useYn) {
            this.locCode = locCode;
            this.status  = status;
            this.locked  = locked;
            this.useYn   = useYn;
        }

        public String  getLocCode() { return locCode; }
        public Integer  getStatus()  { return status; }
        public boolean isLocked()   { return locked; }
        public boolean isUseYn()    { return useYn; }

        @Override
        public String toString() {
            return locCode + "(" + status + ",lock=" + locked + ")";
        }
    }
}