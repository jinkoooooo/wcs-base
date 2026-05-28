package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.AllocationResult;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.LocWithPosition;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;

/**
 * ====================================================================
 * WCS Location 통합 서비스 (최종 리팩토링 버전)
 * ====================================================================
 */
@Service
public class WcsLocationService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(WcsLocationService.class);

    @Autowired
    private TbWcsLocMstService locMstService;

    // -----------------------------------------------------------------------
    // [1] 위치 기반 조회 로직 (JOIN tb_eq_rack_mst)
    // -----------------------------------------------------------------------

    /**
     * 입고 가능 로케이션 목록 조회 (위치 정보 포함)
     */
    public List<LocWithPosition> findAvailableForInbound(String eqGroupId) {
        if (!StringUtils.hasText(eqGroupId)) return Collections.emptyList();
        List<TbWcsLocMst> locs = locMstService.findAvailableRacks(eqGroupId, LocTypeEnumCode.RACK,LocStatusEnumCode.EMPTY);
        return attachPositions(eqGroupId, locs);
    }

    // -----------------------------------------------------------------------
    // [2] 상태 관리 로직 (Lock / Unlock)
    // -----------------------------------------------------------------------

    /**
     * 주문 유형에 따른 로케이션 잠금 처리 (lockBy 기록 포함)
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockForOrder(AllocationResult allocation, String orderType, String orderKey) {
        String from = allocation.getFromLocCode();
        String to = allocation.getToLocCode();
        String eqGroupId = allocation.getEqGroupId();

        if (OrderTypeEnumCode.INBOUND.codeAsString().equalsIgnoreCase(orderType)) {
            lock(eqGroupId, to, orderKey);
        } else if (OrderTypeEnumCode.OUTBOUND.codeAsString().equalsIgnoreCase(orderType)) {
            lock(eqGroupId, from, orderKey);
        } else if (OrderTypeEnumCode.MOVE.codeAsString().equalsIgnoreCase(orderType)) {
            lockBoth(eqGroupId, from, to, orderKey);
        } else {
            throw new ElidomRuntimeException("알 수 없는 주문 유형입니다: " + orderType);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlockForOrder(AllocationResult allocation, String orderType) {
        String from = allocation.getFromLocCode();
        String to = allocation.getToLocCode();
        String eqGroupId = allocation.getEqGroupId();

        if (OrderTypeEnumCode.MOVE.codeAsString().equalsIgnoreCase(orderType)) {
            unlockBoth(eqGroupId,
                    from, LocStatusEnumCode.OCCUPIED,
                    to, LocStatusEnumCode.EMPTY);
        } else if (OrderTypeEnumCode.INBOUND.codeAsString().equalsIgnoreCase(orderType)) {
            unlock(eqGroupId, to, LocStatusEnumCode.EMPTY);
        } else if (OrderTypeEnumCode.OUTBOUND.codeAsString().equalsIgnoreCase(orderType)) {
            unlock(eqGroupId, from, LocStatusEnumCode.OCCUPIED);
        }
    }

    /**
     * 로케이션 잠금 (Native SQL Atomic Update)
     * lockBy 컬럼에 주문번호 기록
     */
    @Transactional(rollbackFor = Exception.class)
    public void lock(String eqGroupId, String locCode, String orderKey) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locCode)) {
            throw new ElidomRuntimeException("로케이션 락 실패: 파라미터가 누락되었습니다.");
        }

        TbWcsLocMst loc = locMstService.findByEqGroupIdAndLocCode(eqGroupId, locCode);
        if (loc != null && !LocTypeEnumCode.isLockable(loc.getLocType())) return;

        boolean lock = locMstService.lock(eqGroupId, locCode, orderKey);

        if(!lock){
            throw new ElidomRuntimeException("로케이션 락 실패");
        }
    }

    /**
     * 로케이션 잠금 해제 (Native SQL)
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlock(String eqGroupId, String locCode, LocStatusEnumCode locStatusEnumCode) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locCode)) return;

        TbWcsLocMst loc = locMstService.findByEqGroupIdAndLocCode(eqGroupId, locCode);
        if (loc != null && !LocTypeEnumCode.isLockable(loc.getLocType())) return;

        locMstService.unlock(eqGroupId,locCode,locStatusEnumCode);

        logger.info("[LOC_UNLOCK_SUCCESS] 구역: {}, 코드: {}, 상태: {}", eqGroupId, locCode, locStatusEnumCode);
    }

    /**
     * 이중 로케이션 락 (데드락 방지 정렬 적용)
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockBoth(String eqGroupId, String loc1, String loc2, String orderKey) {
        if (Objects.equals(loc1, loc2)) {
            lock(eqGroupId, loc1, orderKey);
            return;
        }

        // 1. 데드락 방지: 항상 일정한 순서(알파벳 순)로 락을 시도함
        List<String> sortedLocs = Arrays.asList(loc1, loc2);
        Collections.sort(sortedLocs);

        // 2. 순차 락 시도
        for (String locCode : sortedLocs) {
            lock(eqGroupId, locCode, orderKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlockBoth(String eqGroupId, String fLoc, LocStatusEnumCode fStat, String tLoc, LocStatusEnumCode tStat) {
        unlock(eqGroupId, fLoc, fStat);
        unlock(eqGroupId, tLoc, tStat);
    }


    // -----------------------------------------------------------------------
    // [3] 내부 헬퍼 메서드
    // -----------------------------------------------------------------------

    private List<LocWithPosition> attachPositions(String eqGroupId, List<TbWcsLocMst> locs) {
        if (ValueUtil.isEmpty(locs)) return Collections.emptyList();

        List<String> locCodes = locs.stream()
                .map(TbWcsLocMst::getLocCode)
                .filter(StringUtils::hasText)
                .toList();

        List<LocWithPosition> positions = locMstService.findMultipleWithPosition(eqGroupId, locCodes,null);

        Map<String, LocWithPosition> posMap = new HashMap<>();
        for (LocWithPosition p : positions) {
            if (p.getLoc() != null && StringUtils.hasText(p.getLoc().getLocCode())) {
                posMap.put(p.getLoc().getLocCode(), p);
            }
        }

        List<LocWithPosition> result = new ArrayList<>(locs.size());
        for (TbWcsLocMst loc : locs) {
            if (loc == null) continue;

            LocWithPosition pos = posMap.get(loc.getLocCode());
            if (pos != null) {
                result.add(pos);
            } else {
                result.add(new LocWithPosition(loc, 0, 0, 0,false));
            }
        }
        return result;
    }
}