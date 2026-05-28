package operato.logis.wcs.service.impl.allocation.location;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.location.WcsLocationUtil;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.LocWithPosition;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static operato.logis.wcs.common.util.lang.CommonUtils.orDefault;

/**
 * 이동(MOVE) 출발지/목적지 로케이션 할당기.
 *
 * MOVE 는 "이미 있는 재고를 어딘가로 옮긴다" 도메인이므로 fromLocId 는 필수.
 *
 * 분기:
 *   - from + to 둘 다 지정 : 마스터 검증 후 to 를 Select Lock 으로 선점
 *   - from 만 지정          : Sandwich 회피 우선순위로 빈 RACK 후보 정렬 후 Select Lock (Edge 우선)
 *
 * 방해물 산출은 호출자(ShuttleOrderRegistrar)에서 WcsMoveOrderHandler.resolveObstacles 호출로 수행.
 */
@Service
@RequiredArgsConstructor
public class MoveLocationAllocator {

    private static final Logger logger = LoggerFactory.getLogger(MoveLocationAllocator.class);

    private final InventoryLocationRepository locationRepository;

    /**
     * MOVE 할당 진입점 - from+to 직접 지정 / from 만 지정 분기.
     */
    @Transactional(rollbackFor = Exception.class)
    public AllocationResult allocate(WcsOrderCommand command) {

        // 입력값 검증
        if (ValueUtil.isEmpty(command)) {
            return AllocationResult.fail(WcsError.INVALID_PARAMETER.codeAsString(), "Command is null");
        }
        if (!OrderType.MOVE.matches(command.getOrderType())) {
            return AllocationResult.fail(WcsError.INVALID_PARAMETER.codeAsString(), "Order type is not MOVE");
        }

        String fromLocId = trimToNull(command.getFromLocId());
        String toLocId = trimToNull(command.getToLocId());

        // MOVE 는 옮길 대상이 있어야 성립 - from 필수
        if (ValueUtil.isEmpty(fromLocId)) {
            return AllocationResult.fail(WcsError.INVALID_PARAMETER.codeAsString(),
                    "fromLocId is required for MOVE");
        }

        // to 유무에 따라 분기
        return ValueUtil.isNotEmpty(toLocId)
                ? allocateDirect(command, fromLocId, toLocId)
                : allocateTargetOnly(command, fromLocId);
    }

    /**
     * 지정된 to 로케이션을 검증하고 Select Lock 으로 선점한다.
     */
    private AllocationResult allocateDirect(WcsOrderCommand command, String fromLocId, String toLocId) {

        // from 과 to 동일하면 실패
        if (fromLocId.equals(toLocId)) {
            logger.info("[ Allocation ][ Loc ] move from=to same - rejected. locId={}", fromLocId);
            return AllocationResult.fail(WcsError.INVALID_PARAMETER.codeAsString(), "From and To are same");
        }

        // from/to 마스터 존재 검증
        String eqGroupId = command.getEqGroupId();
        ExtTbInventoryLocation fromLoc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, fromLocId);
        ExtTbInventoryLocation toLoc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, toLocId);

        if (ValueUtil.isEmpty(fromLoc) || ValueUtil.isEmpty(toLoc)) {
            logger.info("[ Allocation ][ Loc ] move master missing - fromLoc({})={}, toLoc({})={}",
                    fromLocId, fromLoc, toLocId, toLoc);
            return AllocationResult.fail(WcsError.INVALID_PARAMETER.codeAsString(),
                    "Location not found in RACK master");
        }

        // from 에 옮길 재고가 있어야 MOVE 가 성립
        if (ValueUtil.isEmpty(fromLoc.getStockId())) {
            logger.info("[ Allocation ][ Loc ] move fromLoc no stock - fromLocId={}", fromLocId);
            return AllocationResult.fail(WcsError.NO_AVAILABLE_STOCK.codeAsString(),
                    "fromLoc has no stock to move: " + fromLocId);
        }

        // 지정 목적지 락 시도
        ExtTbInventoryLocation locked = locationRepository.findAndLockBestOne(eqGroupId, List.of(toLocId));
        if (ValueUtil.isEmpty(locked)) {
            logger.info("[ Allocation ][ Loc ] move target lock fail - toLocId={}", toLocId);
            return AllocationResult.fail(WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                    "Target location is already locked by another process.");
        }

        logger.info("[ Allocation ][ Loc ] move direct allocated - hostOrderKey={}, from={}, to={}",
                command.getHostOrderKey(), fromLocId, locked.getLocId());
        return AllocationResult.success(fromLocId, locked.getLocId(), eqGroupId);
    }

    /**
     * from 만 지정된 경우 to 를 산출한다 (Edge 우선, Sandwich 후순위).
     */
    private AllocationResult allocateTargetOnly(WcsOrderCommand command, String fromLocId) {
        String eqGroupId = command.getEqGroupId();
        String myHostKey = command.getHostOrderKey();

        // 전체 RACK 조회 (위치 정보 포함)
        List<LocWithPosition> allRacks =
                locationRepository.findMultipleWithPosition(eqGroupId, null, LocType.RACK);

        // from 마스터 존재 검증
        LocWithPosition fromCandidate = allRacks.stream()
                .filter(l -> l.getLoc().getLocId().equals(fromLocId))
                .findFirst()
                .orElse(null);
        if (ValueUtil.isEmpty(fromCandidate)) {
            return AllocationResult.fail(WcsError.INVALID_PARAMETER.codeAsString(),
                    "Invalid fromLocId: " + fromLocId);
        }

        // from 에 재고가 있어야 함
        if (ValueUtil.isEmpty(fromCandidate.getLoc().getStockId())) {
            return AllocationResult.fail(WcsError.NO_AVAILABLE_STOCK.codeAsString(),
                    "fromLoc has no stock to move: " + fromLocId);
        }

        // Sandwich 분류 맵 + 후보 필터링 (task_id 비었거나 자기 hostKey)
        Map<String, Boolean> sandwichMap = WcsLocationUtil.buildSandwichMap(allRacks);
        List<LocWithPosition> candidates = allRacks.stream()
                .filter(lwp -> ValueUtil.isEmpty(lwp.getLoc().getStockId())
                        && isFreeForMe(lwp.getLoc().getTaskId(), myHostKey)
                        && !lwp.getLoc().getLocId().equals(fromLocId))
                .toList();

        if (ValueUtil.isEmpty(candidates)) {
            return AllocationResult.fail(WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                    "No available target RACK");
        }

        // Edge/Sandwich 우선순위로 정렬 후 Select Lock
        List<String> orderedCodes = orderCandidates(candidates, sandwichMap);
        ExtTbInventoryLocation locked = locationRepository.findAndLockBestOne(eqGroupId, orderedCodes);
        if (ValueUtil.isEmpty(locked)) {
            return AllocationResult.fail(WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                    "Target selection failed due to concurrent lock");
        }

        String tier = Boolean.TRUE.equals(sandwichMap.get(locked.getLocId())) ? "Sandwiched" : "Edge";
        logger.info("[ Allocation ][ Loc ] move target allocated - hostOrderKey={}, from={}, to={}, tier={}",
                command.getHostOrderKey(), fromLocId, locked.getLocId(), tier);

        return AllocationResult.success(fromLocId, locked.getLocId(), eqGroupId);
    }

    /**
     * 해당 task_id 가 비어있거나(자유) 자기 hostKey 의 soft reservation 인지 판단한다.
     */
    private boolean isFreeForMe(String taskId, String myHostKey) {
        if (ValueUtil.isEmpty(taskId)) return true;
        return ValueUtil.isNotEmpty(myHostKey) && taskId.equals(myHostKey);
    }

    /**
     * 후보를 Edge/Sandwich 로 분류한 뒤 각각 locId 사전순 정렬해 합친다.
     */
    private List<String> orderCandidates(List<LocWithPosition> candidates,
                                         Map<String, Boolean> sandwichMap) {
        List<LocWithPosition> edges = new ArrayList<>();
        List<LocWithPosition> sandwiches = new ArrayList<>();

        // Edge / Sandwich 분류
        for (LocWithPosition cp : candidates) {
            if (Boolean.TRUE.equals(sandwichMap.get(cp.getLoc().getLocId()))) {
                sandwiches.add(cp);
            } else {
                edges.add(cp);
            }
        }

        // 그룹별 사전순 정렬 후 Edge 먼저 합치기
        Comparator<LocWithPosition> byLocId =
                Comparator.comparing(a -> orDefault(a.getLoc().getLocId(), "ZZZZ"));

        return Stream.concat(
                edges.stream().sorted(byLocId),
                sandwiches.stream().sorted(byLocId)
        ).map(lwp -> lwp.getLoc().getLocId()).toList();
    }

    // 문자열 trim - 비었거나 공백만 있으면 null
    private String trimToNull(String value) {
        return ValueUtil.isEmpty(value) ? null : value.trim();
    }
}
