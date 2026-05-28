package operato.logis.kmat_2026.biz.wcs.kmat_2026.service;

import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqCarMst;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.consts.KMat2026LocationMapping.CycleMode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.service.impl.TbEqCarMstService;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.List;

@Service
public class KMat2026CyclePlanService {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026CyclePlanService.class);

    @Autowired
    private TbWcsLocMstService tbWcsLocMstService;

    @Autowired
    private TbEqCarMstService tbEqCarMstService;

    /**
     * 현재 2층 셔틀 이동 가능 여부 판단
     */
    @Transactional(readOnly = true)
    public CycleMode resolveCycleMode() {
        TbEqCarMst shuttle = tbEqCarMstService.findShuttleByLevel(2);

        if (ValueUtil.isEmpty(shuttle)) {
            logger.warn("[KMat2026CyclePlanService] level=2 shuttle 없음 → CAN_NOT_MOVE");
            return CycleMode.FLOOR_2_SHUTTLE_CAN_NOT_MOVE;
        }

        boolean canMoveFloor2Shuttle =
                shuttle.getBatteryStatus() == EcsDBConsts.EqCarBatteryStatus.CAN_MOVE.getValue();

        CycleMode mode = canMoveFloor2Shuttle
                ? CycleMode.FLOOR_2_SHUTTLE_CAN_MOVE
                : CycleMode.FLOOR_2_SHUTTLE_CAN_NOT_MOVE;

        logger.info("[KMat2026CyclePlanService] resolveCycleMode - shuttleId={}, batteryStatus={}, mode={}",
                shuttle.getId(), shuttle.getBatteryStatus(), mode);

        return mode;
    }

    /**
     * 1층 출고 source 조회
     * - 후보 locCode 목록 안에서 OCCUPIED
     * - useYn=1, lockYn=0
     * - loc_seq 기준 cursor 순환
     */
    @Transactional(readOnly = true)
    public List<TbWcsLocMst> findNextFloor1OutboundSources(Integer lastSeq, int count) {
        List<TbWcsLocMst> occupied = tbWcsLocMstService.findOccupiedByEqGroupIdAndLocCodes(
                KMat2026LocationMapping.EQ_GROUP_ID,
                KMat2026LocationMapping.FLOOR_1_POINTS
        );

        List<TbWcsLocMst> result = tbWcsLocMstService.selectNextByCursor(occupied, lastSeq, count);

        logger.info("[KMat2026CyclePlanService] findNextFloor1OutboundSources - lastSeq={}, count={}, resultSize={}",
                lastSeq, count, result.size());

        return result;
    }

    /**
     * 2층 move source 조회
     * - 후보 locCode 목록 안에서 OCCUPIED
     * - useYn=1, lockYn=0
     * - loc_seq 기준 cursor 순환
     */
    @Transactional(readOnly = true)
    public List<TbWcsLocMst> findNextFloor2MoveSources(Integer lastSeq, int count) {
        List<TbWcsLocMst> occupied = tbWcsLocMstService.findOccupiedByEqGroupIdAndLocCodes(
                KMat2026LocationMapping.EQ_GROUP_ID,
                KMat2026LocationMapping.FLOOR_2_POINTS
        );

        List<TbWcsLocMst> result = tbWcsLocMstService.selectNextByCursor(occupied, lastSeq, count);

        logger.info("[KMat2026CyclePlanService] findNextFloor2MoveSources - lastSeq={}, count={}, resultSize={}",
                lastSeq, count, result.size());

        return result;
    }

    /**
     * 2층 move source 조회
     * - 후보 locCode 목록 안에서 OCCUPIED
     * - useYn=1, lockYn=0
     * - loc_seq 기준 cursor 순환
     */
    @Transactional(readOnly = true)
    public List<TbWcsLocMst> findNextFloor2InboundSources(Integer lastSeq, int count) {
        List<TbWcsLocMst> empty = tbWcsLocMstService.findEmptyByEqGroupIdAndLocCodes(
                KMat2026LocationMapping.EQ_GROUP_ID,
                KMat2026LocationMapping.FLOOR_2_POINTS
        );

        List<TbWcsLocMst> result = tbWcsLocMstService.selectNextByCursor(empty, lastSeq, count);

        logger.info("[KMat2026CyclePlanService] findNextFloor2InboundSources - lastSeq={}, count={}, resultSize={}",
                lastSeq, count, result.size());

        return result;
    }

    /**
     * 1층 empty target 조회
     * - 후보 locCode 목록 안에서 EMPTY
     * - useYn=1, lockYn=0
     * - loc_seq 오름차순
     */
    @Transactional(readOnly = true)
    public List<TbWcsLocMst> findSmallestEmptyFloor1(int count) {
        List<TbWcsLocMst> empty = tbWcsLocMstService.findEmptyByEqGroupIdAndLocCodes(
                KMat2026LocationMapping.EQ_GROUP_ID,
                KMat2026LocationMapping.FLOOR_1_POINTS
        );

        List<TbWcsLocMst> result = tbWcsLocMstService.selectSmallestByLocSeq(empty, count);

        logger.info("[KMat2026CyclePlanService] findSmallestEmptyFloor1 - count={}, resultSize={}",
                count, result.size());

        return result;
    }

    /**
     * 2층 empty target 조회
     * - 후보 locCode 목록 안에서 EMPTY
     * - useYn=1, lockYn=0
     * - loc_seq 오름차순
     */
    @Transactional(readOnly = true)
    public List<TbWcsLocMst> findSmallestEmptyFloor2(int count) {
        List<TbWcsLocMst> empty = tbWcsLocMstService.findEmptyByEqGroupIdAndLocCodes(
                KMat2026LocationMapping.EQ_GROUP_ID,
                KMat2026LocationMapping.FLOOR_2_POINTS
        );

        List<TbWcsLocMst> result = tbWcsLocMstService.selectSmallestByLocSeq(empty, count);

        logger.info("[KMat2026CyclePlanService] findSmallestEmptyFloor2 - count={}, resultSize={}",
                count, result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public TbWcsLocMst findLargestOccupiedFloor1() {
        List<TbWcsLocMst> occupied = tbWcsLocMstService.findOccupiedByEqGroupIdAndLocCodes(
                KMat2026LocationMapping.EQ_GROUP_ID,
                KMat2026LocationMapping.FLOOR_1_POINTS
        );

        if (occupied == null || occupied.isEmpty()) {
            logger.warn("[KMat2026CyclePlanService] findLargestOccupiedFloor1 - occupied 없음");
            return null;
        }

        TbWcsLocMst result = occupied.stream()
                .filter(loc -> loc.getLocSeq() != null)
                .max((a, b) -> Integer.compare(a.getLocSeq(), b.getLocSeq()))
                .orElse(null);

        logger.info("[KMat2026CyclePlanService] findLargestOccupiedFloor1 - result={}",
                result != null ? result.getLocCode() + "(" + result.getLocSeq() + ")" : null);

        return result;
    }
}