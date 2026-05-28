package operato.logis.wcs.service.impl.order.intake;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.wcs.common.util.generator.HostOrderKeyGenerator;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.handler.WcsOrderHandler;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shuttle 주문 등록의 트랜잭션 코어.
 *
 * 할당 → 락 선점 → 방해물 MOVE 산출/등록 → 재고 예약 → shuttle_order 생성을
 * 한 트랜잭션으로 묶는다. 단일 / 다중 파렛트 출고 모두 지원.
 *
 * 방해물 산출/필터/등록의 세부 책임은 ObstacleMoveRegistrar 에 위임한다.
 *
 * handleSendFailure 는 본 트랜잭션과 독립적인 ECS 송신 실패 마킹용으로 REQUIRES_NEW 격리.
 */
@Service
@RequiredArgsConstructor
public class ShuttleOrderRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(ShuttleOrderRegistrar.class);

    private final HostOrderStateWriter hostOrderStateWriter;
    private final ShuttleOrderStateWriter shuttleOrderStateWriter;
    private final HostOrderRepository hostOrderRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final HostOrderKeyGenerator orderKeyGenerator;
    private final ObstacleMoveRegistrar obstacleMoveRegistrar;

    /**
     * 주문 등록 진입점 — 할당 결과의 다중 여부에 따라 단일/다중 경로 분기.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderContext registerOrder(WcsOrderHandler handler, WcsOrderCommand command) {
        AllocationResult allocation = handler.allocateLocation(command);
        if (ValueUtil.isEmpty(allocation) || !allocation.isSuccess()) {
            throw new ElidomRuntimeException("가용 로케이션 또는 재고 할당에 실패했습니다.");
        }
        logger.info("[ Order ][ Shuttle ] location allocated");

        TbWcsHostOrder hostOrder = resolveHostOrder(command);
        List<AllocationResult.LocationAllocation> locAllocations = allocation.getLocationAllocations();

        if (ValueUtil.isNotEmpty(locAllocations)) {
            obstacleMoveRegistrar.sortByDeepAscWithinSameLine(locAllocations);
            return registerMultiLocationOrders(handler, command, hostOrder, locAllocations);
        }
        return registerSingleOrder(handler, command, allocation, hostOrder);
    }

    /**
     * 시뮬레이션/단순 insert 흐름 — 방해물·예약·락 없이 shuttle insert 만 수행.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderContext executeInsertOnly(WcsOrderHandler handler, WcsOrderCommand command) {
        AllocationResult allocation = handler.allocateLocation(command);
        if (ValueUtil.isEmpty(allocation) || !allocation.isSuccess()) {
            throw new ElidomRuntimeException("가용 로케이션 할당에 실패했습니다.");
        }

        // 가상 host 주문 생성 (옵션)
        TbWcsHostOrder virtualHostOrder = null;
        if (command.isPersistHostOrder()) {
            virtualHostOrder = hostOrderStateWriter.saveVirtualHostOrderWithItems(command);
        }

        List<AllocationResult.LocationAllocation> locAllocations = allocation.getLocationAllocations();
        List<ShuttleOrderUnit> units = new ArrayList<>();

        if (ValueUtil.isNotEmpty(locAllocations)) {
            // 다중 로케이션 — 각 로케이션마다 shuttle 1건
            for (AllocationResult.LocationAllocation locAlloc : locAllocations) {
                String wcsOrderKey = orderKeyGenerator.generate("ORDER_KEY");

                WcsOrderCommand subCommand = command.toBuilder()
                        .wcsOrderKey(wcsOrderKey)
                        .fromLocId(locAlloc.getFromLocId())
                        .toLocId(locAlloc.getToLocId())
                        .hostOrderKey(command.getHostOrderKey())
                        .items(toCommandItems(locAlloc.getItems()))
                        .build();

                TbWcsShuttleOrder order = handler.createShuttleOrder(subCommand, AllocationResult.success(
                        locAlloc.getFromLocId(), locAlloc.getToLocId(), locAlloc.getEqGroupId()));
                List<TbWcsShuttleOrderItem> items = handler.createShuttleOrderItems(subCommand, order);
                units.add(new ShuttleOrderUnit(order, items));
            }
        } else {
            // 단일 로케이션
            if (ValueUtil.isEmpty(command.getWcsOrderKey())) {
                command.setWcsOrderKey(orderKeyGenerator.generate("ORDER_KEY"));
            }
            TbWcsShuttleOrder order = handler.createShuttleOrder(command, allocation);
            List<TbWcsShuttleOrderItem> items = handler.createShuttleOrderItems(command, order);
            units.add(new ShuttleOrderUnit(order, items));
        }

        // 가상 host 주문이면 첫 shuttle 키로 ALLOCATED 마킹
        if (ValueUtil.isNotEmpty(virtualHostOrder) && ValueUtil.isNotEmpty(units)) {
            hostOrderStateWriter.markAllocated(virtualHostOrder, units.get(0).order().getOrderKey());
        }
        return new OrderContext(units);
    }

    /**
     * 다중 파렛트 출고 시 sibling shuttle 들이 모두 완료되었는지 확인.
     * EcsCallbackProcessor / 스케줄러에서 개별 완료 시점에 호출.
     */
    public boolean checkHostOrderCompletion(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) return false;

        List<TbWcsShuttleOrder> siblings = shuttleOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(siblings)) return false;

        Integer completedCode = ShuttleOrderStatus.COMPLETED.codeAsIntOrNull();
        return siblings.stream()
                .allMatch(o -> ValueUtil.isNotEmpty(completedCode) && completedCode.equals(o.getOrderStatus()));
    }

    /**
     * ECS 송신 실패 마킹 — 본 트랜잭션 롤백과 독립적으로 기록.
     * REQUIRES_NEW + rollbackFor 보강.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleSendFailure(String orderKey, String errorMsg) {
        TbWcsShuttleOrder order = shuttleOrderRepository.findByOrderKey(orderKey);
        if (ValueUtil.isNotEmpty(order)) {
            shuttleOrderStateWriter.markErrorSendFail(order, "ECS 전송 실패: " + errorMsg);
        }
        logger.info("[ Order ][ Shuttle ] send failed - retry required, orderKey={}", orderKey);
    }

    /**
     * 단일 파렛트 등록 — 락 선점 + 방해물 산출/등록 + 재고 예약 + shuttle 생성.
     */
    private OrderContext registerSingleOrder(WcsOrderHandler handler, WcsOrderCommand command,
                                             AllocationResult allocation, TbWcsHostOrder hostOrder) {
        String wcsOrderKey = orderKeyGenerator.generate("ORDER_KEY");
        command.setWcsOrderKey(wcsOrderKey);

        // 락 선점
        handler.lockOrderLocationForShuttleOrderCalculate(command, allocation, wcsOrderKey);
        logger.info("[ Order ][ Shuttle ] location task locked - wcsOrderKey={}", wcsOrderKey);

        // 방해물 산출/등록 (재귀 호출 회피 옵션 처리)
        boolean hasObstacles = false;
        String obstaclePrereqKey = null;
        if (!command.isObstacleCalculate()) {
            List<RelocationTaskDto> obstaclePlan = handler.resolveObstacles(command, allocation);
            obstacleMoveRegistrar.ensureNoWaitInPath(obstaclePlan);

            hasObstacles = !ValueUtil.isEmpty(obstaclePlan);
            if (hasObstacles) {
                allocation.setObstacleMoves(obstaclePlan);
                obstaclePrereqKey = obstacleMoveRegistrar.registerObstacleMoves(command, wcsOrderKey, obstaclePlan);
            }
            logger.info("[ Order ][ Shuttle ] obstacle resolved - hasObstacles={}", hasObstacles);
        }

        // 재고 예약
        handler.reserveInventory(command, allocation);
        logger.info("[ Order ][ Shuttle ] inventory reserved");

        // shuttle 주문 + items 생성
        TbWcsShuttleOrder order = handler.createShuttleOrder(command, allocation);
        List<TbWcsShuttleOrderItem> items = handler.createShuttleOrderItems(command, order);
        logger.info("[ Order ][ Shuttle ] created - wcsOrderKey={}", wcsOrderKey);

        // 방해물 있으면 prereq 설정 + WAITING 마킹
        if (hasObstacles) {
            if (ValueUtil.isNotEmpty(obstaclePrereqKey)) {
                order.setPrerequisiteOrderKey(obstaclePrereqKey);
                shuttleOrderRepository.update(order, "prerequisiteOrderKey");
                logger.info("[ Order ][ Shuttle ] prereq set - order={}, prereq={}",
                        order.getOrderKey(), obstaclePrereqKey);
            }
            shuttleOrderStateWriter.markWaiting(order);
        }

        // host 주문이 있고 시험대기 상태가 아니면 ALLOCATED 마킹
        if (ValueUtil.isNotEmpty(hostOrder) && !isInboundTestWait(hostOrder)) {
            hostOrderStateWriter.markAllocated(hostOrder, order.getOrderKey());
        }

        return new OrderContext(new ShuttleOrderUnit(order, items));
    }

    /**
     * 다중 파렛트 등록 — 형제 chain prereq + 방해물 chain prereq 직렬화 처리.
     */
    private OrderContext registerMultiLocationOrders(WcsOrderHandler handler, WcsOrderCommand command,
                                                     TbWcsHostOrder hostOrder,
                                                     List<AllocationResult.LocationAllocation> locAllocations) {
        List<ShuttleOrderUnit> units = new ArrayList<>();

        // 형제 outbound 의 fromLocId → orderKey 매핑 (삽입 순서 보존)
        // - 후속 iter 의 방해물 화이트리스트
        // - 형제발 WAIT 감지 시 가장 최근 형제(마지막 entry) 로 prereq 직렬화 (A→B→C 안전 체인)
        Map<String, String> siblingOrderKeyByLocId = new LinkedHashMap<>();

        for (AllocationResult.LocationAllocation locAlloc : locAllocations) {
            if (ValueUtil.isEmpty(locAlloc.getItems())) {
                throw new ElidomRuntimeException("locationAlloc.items is empty");
            }

            String wcsOrderKey = orderKeyGenerator.generate("ORDER_KEY");
            AllocationResult singleAlloc = AllocationResult.success(
                    locAlloc.getFromLocId(), locAlloc.getToLocId(), locAlloc.getEqGroupId());

            // 락 선점
            handler.lockOrderLocationForShuttleOrderCalculate(command, singleAlloc, wcsOrderKey);

            // 방해물 산출 + 형제 필터링
            boolean hasObstacles = false;
            boolean siblingWaitDetected = false;
            String obstaclePrereqKey = null;
            if (!command.isObstacleCalculate()) {
                List<RelocationTaskDto> obstaclePlan = handler.resolveObstacles(command, singleAlloc);
                siblingWaitDetected = obstacleMoveRegistrar.filterOutSiblingLocs(
                        obstaclePlan, siblingOrderKeyByLocId, command.getHostOrderKey());
                obstacleMoveRegistrar.ensureNoWaitInPath(obstaclePlan);

                hasObstacles = ValueUtil.isNotEmpty(obstaclePlan);
                if (hasObstacles) {
                    locAlloc.setObstacleMoves(obstaclePlan);
                    locAlloc.setHasObstacles(true);
                    obstaclePrereqKey = obstacleMoveRegistrar.registerObstacleMoves(command, wcsOrderKey, obstaclePlan);
                }
            }

            // 자식 command 구성
            WcsOrderCommand subCommand = command.toBuilder()
                    .wcsOrderKey(wcsOrderKey)
                    .fromLocId(locAlloc.getFromLocId())
                    .toLocId(locAlloc.getToLocId())
                    .hostOrderKey(command.getHostOrderKey())
                    .items(toCommandItems(locAlloc.getItems()))
                    .subOrderType(resolveSubType(command, locAlloc))
                    .build();

            // 재고 예약 + shuttle 생성
            handler.reserveInventory(subCommand, singleAlloc);
            TbWcsShuttleOrder order = handler.createShuttleOrder(subCommand, singleAlloc);
            List<TbWcsShuttleOrderItem> items = handler.createShuttleOrderItems(subCommand, order);

            // prereq 결정 — 형제발 WAIT 면 가장 최근 형제, 외부 방해물만 있으면 obstacle child.
            // 같은 라인 deep ASC 정렬 가정 → 마지막 형제 = 가장 안쪽 blocker → 직렬 체인 안전.
            String prereqKey = null;
            String prereqKind = null;
            if (siblingWaitDetected) {
                prereqKey = lastValue(siblingOrderKeyByLocId);
                prereqKind = "SIBLING";
            } else if (hasObstacles && ValueUtil.isNotEmpty(obstaclePrereqKey)) {
                prereqKey = obstaclePrereqKey;
                prereqKind = "OBSTACLE";
            }

            if (ValueUtil.isNotEmpty(prereqKey)) {
                order.setPrerequisiteOrderKey(prereqKey);
                shuttleOrderRepository.update(order, "prerequisiteOrderKey");
                logger.info("[ Order ][ Shuttle ] outbound prereq set - order={}, prereq={}, kind={}",
                        order.getOrderKey(), prereqKey, prereqKind);
            }

            // WAITING 마킹 조건: 외부 방해물 OR 형제발 WAIT
            if (locAlloc.isHasObstacles() || siblingWaitDetected) {
                shuttleOrderStateWriter.markWaiting(order);
            }

            units.add(new ShuttleOrderUnit(order, items));

            // 후속 iter 를 위한 형제 매핑 저장 (LinkedHashMap → 삽입 순서 = deep ASC 처리 순서)
            siblingOrderKeyByLocId.put(locAlloc.getFromLocId(), order.getOrderKey());
        }

        if (ValueUtil.isEmpty(units)) {
            throw new ElidomRuntimeException("등록된 shuttle 이 없습니다.");
        }

        // host 주문 ALLOCATED 마킹 (시험대기 아닐 때만)
        if (ValueUtil.isNotEmpty(command.getHostOrderKey()) && ValueUtil.isNotEmpty(hostOrder)
                && !isInboundTestWait(hostOrder)) {
            hostOrderStateWriter.markAllocated(hostOrder, units.get(0).order().getOrderKey());
        }
        logger.info("[ Order ][ Shuttle ] multi-pallet registered - hostOrderKey={}, type={}, shuttleCount={}",
                command.getHostOrderKey(), command.getOrderType(), units.size());

        return new OrderContext(units);
    }

    /**
     * INBOUND_TEST_WAIT 상태 여부 — 시험 대기 host 는 ALLOCATED 마킹 보류.
     */
    private boolean isInboundTestWait(TbWcsHostOrder host) {
        if (ValueUtil.isEmpty(host)) return false;
        Integer code = HostOrderStatus.INBOUND_TEST_WAIT.codeAsIntOrNull();
        return ValueUtil.isNotEmpty(code) && code.equals(host.getOrderStatus());
    }

    /**
     * 입력 command 의 hostOrderKey 로 host 주문 조회 (없으면 생성/예외).
     * persistHostOrder=true 면 가상 host 생성, MOVE 면 hostOrderKey 없이도 허용.
     */
    private TbWcsHostOrder resolveHostOrder(WcsOrderCommand command) {
        if (command.isPersistHostOrder()) {
            TbWcsHostOrder hostOrder = hostOrderStateWriter.saveVirtualHostOrderWithItems(command);
            logger.info("[ Order ][ Shuttle ] virtual host order inserted - hostOrderKey={}",
                    hostOrder.getHostOrderKey());
            return hostOrder;
        }

        String hostOrderKey = command.getHostOrderKey();
        boolean isMove = OrderType.MOVE.matches(command.getOrderType());

        if (ValueUtil.isEmpty(hostOrderKey)) {
            if (isMove) return null;
            throw new ElidomRuntimeException("hostOrderKey가 필요하지만 null입니다.");
        }

        TbWcsHostOrder hostOrder = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(hostOrder) && !isMove) {
            throw new ElidomRuntimeException("HostOrder를 찾을 수 없습니다: " + hostOrderKey);
        }
        return hostOrder;
    }

    /**
     * AllocationResult.Item → WcsOrderCommand.Item 변환.
     */
    private List<WcsOrderCommand.Item> toCommandItems(List<AllocationResult.Item> items) {
        List<WcsOrderCommand.Item> result = new ArrayList<>();
        if (ValueUtil.isEmpty(items)) return result;
        for (AllocationResult.Item ai : items) {
            result.add(WcsOrderCommand.Item.builder()
                    .itemCode(ai.getItemCode())
                    .lotNo(ai.getLotNo())
                    .qty(ai.getQty())
                    .build());
        }
        return result;
    }

    /**
     * partialPicking 이면 PARTIAL_OUT 강제, 아니면 command 의 기존 subType 유지.
     */
    private String resolveSubType(WcsOrderCommand command, AllocationResult.LocationAllocation locAlloc) {
        if (ValueUtil.isNotEmpty(locAlloc) && locAlloc.isPartialPicking()) {
            return SubOrderType.PARTIAL_OUT.codeAsString();
        }
        return command.getSubOrderType();
    }

    /**
     * LinkedHashMap 가정 — 가장 최근 삽입된 value 반환.
     */
    private static <K, V> V lastValue(Map<K, V> map) {
        V last = null;
        for (V v : map.values()) last = v;
        return last;
    }
}
