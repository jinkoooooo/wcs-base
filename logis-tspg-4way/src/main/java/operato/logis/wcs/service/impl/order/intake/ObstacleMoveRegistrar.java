package operato.logis.wcs.service.impl.order.intake;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.RetrevalPlanTaskType;
import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.handler.WcsOrderHandler;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 방해물 MOVE 자식 주문 등록 + 형제/대기 충돌 필터링 책임.
 *
 * - registerObstacleMoves : 방해물 RelocationTaskDto 들을 MOVE 자식으로 등록.
 * - filterOutSiblingLocs  : 같은 host 의 형제 셔틀이 이미 잡은 위치는 방해물 후보에서 제외.
 * - sortByDeepAscWithinSameLine : 같은 라인 안 deep 작은 셀 먼저 빠지도록 정렬해 충돌 차단.
 * - ensureNoWaitInPath    : WAIT 잔존 시 예외 발생 (호출자 차단).
 *
 * 등록은 ShuttleOrderRegistrar 의 재귀를 다시 호출 — 트랜잭션 동참.
 */
@Component
@RequiredArgsConstructor
public class ObstacleMoveRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(ObstacleMoveRegistrar.class);

    private static final int DEFAULT_PRIORITY = 5;
    // 방해물 자식이 부모보다 우선순위 한참 위로 올라가도록 -1000 오프셋
    private static final int PRIORITY_OFFSET = 1000;
    private static final int MAX_STEP_PRIORITY = 999;

    private final List<WcsOrderHandler> orderHandlers;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;

    // 순환 의존 회피 — registerObstacleMoves 가 다시 registrar.registerOrder 를 호출
    private final ObjectProvider<ShuttleOrderRegistrar> registrarProvider;

    /**
     * 방해물 MOVE 들을 등록하고, 부모가 prereq 로 잡아야 할 "마지막 송신 자식" 의 order_key 반환.
     *
     * 송신 순서: priority ASC (= 통로 입구부터) → 마지막은 stepOrder 가장 큰 안쪽 자식.
     * 부모는 이 마지막 자식이 COMPLETED 되어야 wake.
     *
     * @return 마지막 송신 자식의 order_key. 자식 0건이면 null.
     */
    public String registerObstacleMoves(WcsOrderCommand command, String parentOrderKey,
                                        List<RelocationTaskDto> obstacleMoves) {
        WcsOrderHandler moveHandler = orderHandlers.stream()
                .filter(h -> h.supports(OrderType.MOVE.codeAsString()))
                .findFirst()
                .orElseThrow(() -> new ElidomRuntimeException("이동(MOVE) 처리 핸들러를 찾을 수 없습니다."));

        int parentPriority = ValueUtil.isNotEmpty(command.getPriority()) ? command.getPriority() : DEFAULT_PRIORITY;

        // 안쪽 방해물부터 먼저 산출/등록 (stepOrder 큰 순)
        obstacleMoves.sort((a, b) -> b.getStepOrder().compareTo(a.getStepOrder()));

        Integer maxStepOrder = null;
        String lastChildOrderKey = null;

        for (RelocationTaskDto task : obstacleMoves) {
            // taskType=1 (MOVE) 만 처리, 그 외(WAIT 등)는 skip
            if (ValueUtil.isEmpty(task.getTaskType()) || task.getTaskType() != 1) continue;

            int childPriority = parentPriority - PRIORITY_OFFSET + Math.min(task.getStepOrder(), MAX_STEP_PRIORITY);

            // 위치 ID/바코드 해석
            ExtTbInventoryLocation fromLocation = inventoryLocationRepository.findByLocCode(task.getFromLocCode());
            String fromLocId = ValueUtil.isNotEmpty(fromLocation) ? fromLocation.getLocId() : null;
            ExtTbInventoryLocation toLocation = inventoryLocationRepository.findByLocCode(task.getToLocCode());
            String toLocId = ValueUtil.isNotEmpty(toLocation) ? toLocation.getLocId() : null;

            // source 의 barcode 를 그대로 가져감 — pallet 라벨은 위치만 바뀌고 유지
            String fromBarcode = ValueUtil.isNotEmpty(fromLocation) ? fromLocation.getBarcode() : null;

            WcsOrderCommand moveCmd = WcsOrderCommand.builder()
                    .orderType(OrderType.MOVE.codeAsString())
                    .parentOrderKey(parentOrderKey)
                    .ownerCode(command.getOwnerCode())
                    .eqGroupId(command.getEqGroupId())
                    .fromLocId(fromLocId)
                    .toLocId(toLocId)
                    .barCode(fromBarcode)
                    .priority(childPriority)
                    .persistHostOrder(false)
                    .isObstacleCalculate(true)
                    .build();

            logger.info("[ Order ][ Shuttle ] obstacle child move - parent={}, parentPri={}, stepOrder={}, childPri={}",
                    parentOrderKey, parentPriority, task.getStepOrder(), childPriority);

            OrderContext childCtx = registrarProvider.getObject().registerOrder(moveHandler, moveCmd);

            // stepOrder 가장 큰 (마지막 송신·완료될) 자식 추적 — 부모의 prereq 후보
            if (maxStepOrder == null || task.getStepOrder() > maxStepOrder) {
                maxStepOrder = task.getStepOrder();
                lastChildOrderKey = childCtx == null ? null : childCtx.firstOrderKey();
            }
        }
        return lastChildOrderKey;
    }

    /**
     * 경로에 WAIT 가 남아있으면 예외로 거부 — 진행 중인 작업과 충돌하는 경우.
     */
    public void ensureNoWaitInPath(List<RelocationTaskDto> obstaclePlan) {
        boolean hasWait = ValueUtil.isNotEmpty(obstaclePlan) && obstaclePlan.stream()
                .anyMatch(t -> ValueUtil.isNotEmpty(t.getTaskType()) && t.getTaskType().equals(0));
        if (hasWait) {
            throw new ElidomRuntimeException("경로에 진행 중인 작업이 있어 대기 필요");
        }
    }

    /**
     * 같은 host 의 작업이 잡고 있는 loc 은 방해물 후보에서 제외한다.
     *
     * 제거 조건 (둘 중 하나):
     *   1) 형제 outbound 의 fromLocId 매칭 — 같은 multi-pallet 산출의 형제가 잡은 자기 fromLoc.
     *   2) WAIT holder 가 같은 host 의 shuttle — blocker 위치의 task_id 가 가리키는 shuttle 이
     *      현재 host 의 outbound 이거나, 그 outbound 의 MOVE child (parent_order_key chain 1단계) 인 경우.
     *      MOVE child 는 host_order_key=null 이라 parent 거슬러 올라가 host 매칭.
     *
     * 외부 host 의 WAIT 는 두 조건 모두 false 라 남고, 이어 호출되는 ensureNoWaitInPath 에서 거부.
     *
     * @return 제거된 항목 중 WAIT 가 있었으면 true — sibling chain prereq 적용 트리거.
     */
    public boolean filterOutSiblingLocs(List<RelocationTaskDto> obstaclePlan,
                                        Map<String, String> siblingOrderKeyByLocId,
                                        String currentHostOrderKey) {
        if (ValueUtil.isEmpty(obstaclePlan)) return false;

        List<RelocationTaskDto> removed = new ArrayList<>();
        obstaclePlan.removeIf(t -> {
            // (1) 형제 outbound 의 fromLoc 직접 매칭
            if (isSibling(t, siblingOrderKeyByLocId)) {
                removed.add(t);
                return true;
            }
            // (2) WAIT 의 holder 가 같은 host 의 shuttle (parent chain 1단계 포함) 이면 통과
            if (isWait(t) && isHeldBySameHost(t, currentHostOrderKey)) {
                removed.add(t);
                return true;
            }
            return false;
        });

        if (removed.isEmpty()) return false;

        boolean waitRemoved = removed.stream().anyMatch(this::isWait);
        logger.info("[ Order ][ Shuttle ] obstacle same-host filtered - removed={}, waitRemoved={}",
                removed.size(), waitRemoved);
        return waitRemoved;
    }

    /**
     * 다중 로케이션 출고 시 방해물 충돌 방지 사전 정렬.
     *
     * 같은 라인(같은 group/level/side + COLUMN 기준 동일 row | ROW 기준 동일 col) 안에서는
     * deep 작은(통로 안쪽) 것부터 먼저 빠지도록 정렬.
     * 다른 라인끼리는 원래 순서(FIFO) 유지.
     *
     * 같은 라인의 deep 큰 재고가 deep 작은 재고의 방해물로 잡히는 충돌을 원천 차단.
     */
    public void sortByDeepAscWithinSameLine(List<operato.logis.wcs.dto.AllocationResult.LocationAllocation> locAllocations) {
        if (ValueUtil.isEmpty(locAllocations) || locAllocations.size() < 2) return;

        // fromLocId → location 캐시 일괄 조회
        Map<String, ExtTbInventoryLocation> locCache = new HashMap<>();
        for (operato.logis.wcs.dto.AllocationResult.LocationAllocation la : locAllocations) {
            String eqGroupId = la.getEqGroupId();
            String fromLocId = la.getFromLocId();
            if (ValueUtil.isNotEmpty(fromLocId) && !locCache.containsKey(fromLocId)) {
                ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, fromLocId);
                if (ValueUtil.isNotEmpty(loc)) locCache.put(fromLocId, loc);
            }
        }

        // 안정 정렬: 같은 라인이면 deep ASC, 다른 라인이면 원래 순서 유지
        locAllocations.sort((a, b) -> {
            ExtTbInventoryLocation locA = locCache.get(a.getFromLocId());
            ExtTbInventoryLocation locB = locCache.get(b.getFromLocId());
            if (ValueUtil.isEmpty(locA) || ValueUtil.isEmpty(locB)) return 0;
            if (!isSameLine(locA, locB)) return 0;
            return Integer.compare(
                    ValueUtil.isEmpty(locA.getLocDeep()) ? 0 : locA.getLocDeep(),
                    ValueUtil.isEmpty(locB.getLocDeep()) ? 0 : locB.getLocDeep()
            );
        });

        logger.info("[ Order ][ Shuttle ] obstacle sort - deep asc within same line, count={}",
                locAllocations.size());
    }

    /**
     * 두 로케이션이 같은 라인(같은 통로 깊이축)인지 판정.
     * 보수적으로 row/col 둘 다 같은 경우만 같은 라인으로 본다.
     */
    private boolean isSameLine(ExtTbInventoryLocation a, ExtTbInventoryLocation b) {
        return Objects.equals(a.getLocGroup(), b.getLocGroup())
                && Objects.equals(a.getLocLevel(), b.getLocLevel())
                && Objects.equals(a.getLocSide(), b.getLocSide())
                && (Objects.equals(a.getLocRow(), b.getLocRow())
                || Objects.equals(a.getLocCol(), b.getLocCol()));
    }

    /**
     * 형제 outbound 의 fromLoc 직접 매칭 여부.
     */
    private boolean isSibling(RelocationTaskDto t, Map<String, String> siblingOrderKeyByLocId) {
        if (ValueUtil.isEmpty(siblingOrderKeyByLocId)) return false;
        if (ValueUtil.isEmpty(t.getFromLocCode())) return false;
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByLocCode(t.getFromLocCode());
        return ValueUtil.isNotEmpty(loc) && siblingOrderKeyByLocId.containsKey(loc.getLocId());
    }

    /**
     * WAIT blocker 위치를 잡고 있는 shuttle 이 현재 host 의 작업인지 확인.
     * MOVE child (host_order_key=null) 인 경우 parent 거슬러 올라가 1단계 매칭.
     */
    private boolean isHeldBySameHost(RelocationTaskDto t, String currentHostOrderKey) {
        if (ValueUtil.isEmpty(t.getFromLocCode()) || ValueUtil.isEmpty(currentHostOrderKey)) return false;

        ExtTbInventoryLocation loc = inventoryLocationRepository.findByLocCode(t.getFromLocCode());
        if (ValueUtil.isEmpty(loc) || ValueUtil.isEmpty(loc.getTaskId())) return false;

        TbWcsShuttleOrder holder = shuttleOrderRepository.findByOrderKey(loc.getTaskId());
        if (ValueUtil.isEmpty(holder)) return false;

        // 직접 host 매칭
        if (currentHostOrderKey.equals(holder.getHostOrderKey())) return true;

        // MOVE child → parent 로 1단계 거슬러 올라가 host 매칭
        if (ValueUtil.isNotEmpty(holder.getParentOrderKey())) {
            TbWcsShuttleOrder parent = shuttleOrderRepository.findByOrderKey(holder.getParentOrderKey());
            return ValueUtil.isNotEmpty(parent) && currentHostOrderKey.equals(parent.getHostOrderKey());
        }
        return false;
    }

    /**
     * WAIT 타입 여부.
     */
    private boolean isWait(RelocationTaskDto t) {
        return Objects.equals(t.getTaskType(), RetrevalPlanTaskType.WAIT.value());
    }
}
