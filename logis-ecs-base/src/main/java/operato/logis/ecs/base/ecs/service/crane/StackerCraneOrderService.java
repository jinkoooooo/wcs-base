package operato.logis.ecs.base.ecs.service.crane;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneCommandResult;
import operato.logis.ecs.base.ecs.command.crane.StackerCraneMoveCommand;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.domain.crane.StackerCrane;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEcsRackOrder;
import operato.logis.ecs.base.ecs.entity.TbEcsRouteOrder;
import operato.logis.ecs.base.ecs.entity.TbEqCraneMst;
import operato.logis.ecs.base.ecs.entity.TbEqRackMst;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.crane.IStackerCranePort;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneMapManager;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneWriteMap;
import operato.logis.ecs.base.ecs.service.common.*;
import operato.logis.ecs.base.ecs.service.path.StackerCranePathService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 스태커크레인 작업 흐름 제어 서비스
 * - 작업 선택
 * - 작업 상태 변경
 * - 경로 계산
 * - 명령 결정
 * ---
 * - 작업 조회
 * - 작업 상태 분류
 * - 실행 가능한 작업 선택
 * - path 조회
 * - command 생성
 * - cranePort 호출
 */
// TODO: 전체 로직 수정, 한 랙에 여러 대 설비 확장 추가
@Slf4j
@Service
@RequiredArgsConstructor
public class StackerCraneOrderService {

    /*
     * 현재 입출고 포트 하나인 경우에 따른 로직
     * 1. 충전 지시 확인
     * 2. 긴급 지시 확인 (홈 이동지시 포함)
     * 3. 랙단에 올라온 입고 지시 확인
     * 4. 랙단에 올라오고 있는 입고 지시 확인
     * 5. 입고대에 올라온 입고 대기 지시 확인
     * 6. 없는경우 출고 지시 확인
     */

    private ObjectMapper mapper;

    private final StackerCraneMapManager craneMapManager;
    private final IStackerCranePort cranePort;

    private final TbEcsRackOrderService tbEcsRackOrderService;
    private final TbEcsRouteOrderService tbEcsRouteOrderService;
    private final TbEqCraneMstService tbEqCraneMstService;
    private final TbEqMstService tbEqMstService;
    private final TbEqRackMstService tbEqRackMstService;

    private StackerCranePathService stackerCranePathService; // todo: final 검토

    private int workId = 1001;
    private final boolean SINGLE_PATH_MODE = false; // todo: 로직 개선 / <-> multi_path_mode

    // TODO: workId 범위 확인
    private int nextWorkId() {
        workId++;
        if (workId > 9999) {
            workId = 1001;
        }
        return workId;
    }

    public void work(StackerCraneContext context) {
        this.mapper = new ObjectMapper();
        stackerCranePathService = craneMapManager.getMapinfo(context.getRackEqId(), context.getAsiel1(), context.getAsiel2());

        String cvEqId = tbEqMstService.selectEqMst(context).getId();

        // todo: 로직개선
        String craneId = context.getCraneId();
        String craneStatus = null;
        List<StackerCraneWriteMap> path = new ArrayList<>();
        TbEcsRackOrder order = null;
        String rackEqId = context.getRackEqId();
        int asiel1 = context.getAsiel1();
        int asiel2 = context.getAsiel2();
        StackerCraneContext newContext = new StackerCraneContext(craneId, craneStatus, path, order, rackEqId, cvEqId, asiel1, asiel2);

        CraneOrderManager(newContext);

        //CraneMoveCommand command = createCommand(context);
        //cranePort.move(command);
    }

    /*
     * 검토중) 스태커 크레인 지시 관리
     * 1. 해당 설비에서 완료되지 않은 지시 조회
     * - 모두 완료되었으면 종료
     * 2. 작업 상태별 그룹화
     * 3. 설비 지시 전송 상태 (EQ_SEND)
     * - 작업중인 경우 상태 변경
     * 4. 작업중 상태 (WORKING)
     * ...
     * 5. 대기 상태(READY)
     * ...
     */
    private void CraneOrderManager(StackerCraneContext context) {
        List<TbEcsRackOrder> orderList = tbEcsRackOrderService.selectTbEcsRackOrder(context);

        if (orderList == null || orderList.isEmpty()) {
            // log.info("[CraneOrderManager] not completed order is null");
            return;
        }

        Map<Integer, List<TbEcsRackOrder>> orderMap = orderList.stream().collect(Collectors.groupingBy(TbEcsRackOrder::getOrderStatus));
        List<TbEcsRackOrder> readyOrderList = orderMap.get(EcsDBConsts.OrderStatus.READY.getValue());
        List<TbEcsRackOrder> sendOrderList = orderMap.get(EcsDBConsts.OrderStatus.EQ_SEND.getValue());
        List<TbEcsRackOrder> workingOrderList = orderMap.get(EcsDBConsts.OrderStatus.WORKING.getValue());

        if (sendOrderList != null && !sendOrderList.isEmpty()) {
            // log.info("[CraneOrderManager] sendOrderList : " + sendOrderList.size());
            eqSendOrderManager(context, sendOrderList);
        }
        // todo: eqSendOrderManager에서 WORKING상태로 변경된 작업은 workingOrderList에 넣어야하는 것 아닌 지
        if (workingOrderList != null && !workingOrderList.isEmpty()) {
            // log.info("[CraneOrderManager] workingOrderList : " + workingOrderList.size());
            workingOrderManager(context, workingOrderList);
        }
        if (readyOrderList != null && !readyOrderList.isEmpty()) {
            // log.info("[CraneOrderManager] readyOrderList : " + readyOrderList.size());
            readyOrderManager(context, readyOrderList);
        }
    }

    /*
     * ====================================================
     * 1. 작업 관리
     * ====================================================
     */

    /*
     * 검토완료) 작업관리 - 대기 중 작업 (READY)
     *
     * 1. 작업 우선순위 오름차순 정렬
     * 2. 긴급지시 우선 진행
     * 3. 작업 유형별 분류
     * 3-1. 이동지시 (MOVE)
     * - 작업 가능한 크레인 확인 후 이동 지시
     * 3-2. 입고지시 (INBOUND)
     * - 작업 가능한 크레인 확인
     * - 입고대 상태 확인 후 입고지시
     * 3-3. 출고지시 (OUTBOUND)
     * - ...
     * 3-4. Rack to Rack 이송 지시 (RACK_TO_RACK)
     * - ...
     * 3-5. Station to Station 이송 지시 (STATION_TO_STATION)
     * - ...
     */
    private void readyOrderManager(StackerCraneContext context, List<TbEcsRackOrder> readyOrderList) {
        readyOrderList.sort(Comparator.comparing(TbEcsRackOrder::getPriority));

        TbEcsRackOrder emgOrder = readyOrderList.stream().filter(order ->
                order.getPriority() < EcsDBConsts.OrderPriority.NORMAL.getValue()).findFirst().orElse(null);
        if (emgOrder != null) {
            log.info("readyOrderManager emg Order!");
            sendEmgOrder(context, emgOrder);
            return;
        }

        if (readyOrderList.size() == 0) {
            return;
        }

        // todo: readyorderList 순회하며 실행 가능한 작업 실행
        for (TbEcsRackOrder order : readyOrderList) {
            int orderType = order.getOrderType();

            if (orderType == EcsDBConsts.OrderType.MOVE.getValue()) {

                StackerCranePlc cranePlc = cranePlcManager(context, order);
                if (cranePlc == null) {
                    log.info("readyOrderManager moveOrder choiceCrane || cranePlc NULL!");
                    continue;
                }
                TbEqCraneMst craneMst = tbEqCraneMstService.selectByCraneId(context, cranePlc.getId());
                if (craneMst == null) {
                    log.info("readyOrderManager moveOrder choiceCrane || craneMst NULL!");
                    continue;
                }

                moveAndClearOrder(context, order, craneMst);

            } else if (orderType == EcsDBConsts.OrderType.INBOUND.getValue()) {

                // 입출고대 진입 완료 후 대기 화물 확인 및 입고작업 지시
                List<TbEcsRouteOrder> rackInCvReadyOrderList = tbEcsRouteOrderService.selectTbEcsRouteOrder(context);
                if (rackInCvReadyOrderList != null && !rackInCvReadyOrderList.isEmpty()) {
                    rackInCvCargoReadyOrder(context, readyOrderList, rackInCvReadyOrderList);
                }

            } else if (orderType == EcsDBConsts.OrderType.OUTBOUND.getValue()) {

                StackerCranePlc cranePlc = cranePlcManager(context, order);
                if (cranePlc == null) {
                    log.info("readyOrderManager outboundOrder choiceCrane || cranePlc NULL!");
                    continue;
                }
                TbEqCraneMst craneMst = tbEqCraneMstService.selectByCraneId(context, cranePlc.getId());
                if (craneMst == null) {
                    log.info("readyOrderManager outboundOrder choiceCrane || craneMst NULL!");
                    continue;
                }

                outboundLoadMoveOrder(context, cranePlc, order, craneMst);

            } else if (orderType == EcsDBConsts.OrderType.RACK_TO_RACK.getValue()) {
                //todo: 구현
            } else if (orderType == EcsDBConsts.OrderType.STATION_TO_STATION.getValue()) {
                //todo: 구현
            } else if (orderType == EcsDBConsts.OrderType.DESTINATION_CHANGE.getValue()) {
                //todo: 구현
            }
        }
    }

    /*
     * 검토완료) 작업관리 - 전송된 작업 (EQ_SEND)
     *
     * 1. 현재 랙의 스태커크레인 설비전체 조회
     * 2. 지시와 설비코드, plc명령번호가 일치하는 스태커크레인이 존재할 때 확인
     * 3. 아래 조건 중 하나라도 만족하면 지시를 작업줃(WORKING)으로 수정
     * - 지시 대상 스태커크레인이 RUN 상태일 때
     * - 작업번호가 스태커크레인이 완료한 작업번호와 다를 때
     *
     * todo: tbEcsRackOrder에서 eq_id인지, eq_crane_id인지 확인
     */
    private void eqSendOrderManager(StackerCraneContext context, List<TbEcsRackOrder> sendOrderList) {
        List<TbEqCraneMst> eqCraneMstList = tbEqCraneMstService.selectCraneStatus(context);

        for (TbEcsRackOrder sendOrder : sendOrderList) {

            Optional<TbEqCraneMst> craneOpt = eqCraneMstList.stream()
                    .filter(crane -> crane.getEqId().equals(sendOrder.getEqId())
                            && crane.getPlcCmdId() == sendOrder.getPlcCmdId()).findFirst();

            if (craneOpt.isPresent()) {
                TbEqCraneMst craneMst = craneOpt.get();

                boolean isRun = craneMst.getStatus() == EcsDBConsts.EqCraneStatus.RUN.getValue();
                boolean isComplete = sendOrder.getPlcCmdId() == craneMst.getPlcCompCmdId();
                if (isRun || isComplete) {
                    tbEcsRackOrderService.updateTbEcsRackOrder(sendOrder, EcsDBConsts.OrderStatus.WORKING);
                }
            }
        }
    }

    /*
     * 검토완료) 작업관리 - 진행 중 작업 (WORKING)
     *
     * 1. 작업 우선순위 오름차순 정렬 / TODO: 우선순위 기본값이 NULL이면 NULL이 우선됨
     * 2. 현재 랙의 스태커크레인 중 PLC 완료 작업번호과 일치하는 크레인이 있으면 완료여부 확인
     * 3. PLC메모리가 작업완료상태라면 랙 작업 완료처리
     *
     * // TODO 공출고, 이중입고 처리 추가, 카 3개이상시 수정필요
     * // 공출고 : 상위보고(오더키,에러타입), 현재 작업 삭제
     * // 이중입고 : 상위보고(오더키, 에러타입, 버퍼셀), 버퍼셀 언로드
     */
    private void workingOrderManager(StackerCraneContext context, List<TbEcsRackOrder> workingOrderList) {
        workingOrderList.sort(Comparator.comparing(TbEcsRackOrder::getPriority));
        List<TbEqCraneMst> eqCraneMstList = tbEqCraneMstService.selectCraneStatus(context);

        for (TbEcsRackOrder workingOrder : workingOrderList) {

            TbEqCraneMst craneMst = eqCraneMstList.stream()
                    .filter(crane ->
                            crane.getEqId().equals(workingOrder.getEqCraneId())
                                    && crane.getPlcCompCmdId() == workingOrder.getPlcCmdId()).findFirst().orElse(null);
            if (craneMst != null) {
                log.info(craneMst.getId() + " workingOrderManager : " + workingOrder.getOrderKey() + " : " + craneMst.isCompleteYn());

                if (craneMst.isCompleteYn()) {
                    StackerCrane sc = cranePort.isAvailable(craneMst.getEqId());
                    StackerCranePlc cranePlc = null; // todo: 구조 변경 후 삭제

                    if (sc != null) {
                        log.info(sc.getStackerCraneId() + " 추가 지시 없음. 최종 목적지 도착");
                        completeOrderManager(context, craneMst, cranePlc, workingOrder);
                    }
                }
            }
        }
    }

    /* 검토중) 작업관리 - 완료 작업 (COMPLETE)
     *
     * - FROM_TO 단일 지정 방식인 경우
     * 1. LOAD_MOVE → INBOUND/OUTBOUND/RACK_TO_RACK/STATION_TO_STATION → COMPLETE
     * 2. MOVE → COMPLETE
     *
     * - Step by Step 지정 방식인 경우
     * 1. LOAD_MOVE
     * 2. LOAD
     * 3. UNLOAD_MOVE
     * 4. UNLOAD
     * 5. MOVE_HOME
     * */
    private void completeOrderManager(StackerCraneContext context, TbEqCraneMst craneMst, StackerCranePlc cranePlc, TbEcsRackOrder workingOrder) {
        log.info(craneMst.getId() + " CompleteOrderManager");

        if (SINGLE_PATH_MODE) {
            switch (EcsDBConsts.EcsRackOrderCmdStatus.find(workingOrder.getCmdStatus())) {
                case MOVE -> completeOrder(context, cranePlc, workingOrder);
                case INBOUND, OUTBOUND, RACK_TO_RACK, STATION_TO_STATION ->
                        completeOrder(context, cranePlc, workingOrder);
            }
        } else {
            switch (EcsDBConsts.EcsRackOrderCmdStatus.find(workingOrder.getCmdStatus())) {
                // todo: 로직 구현
                //case LOAD_MOVE -> loadOrder(cranePlc, workingOrder, craneMst);
                //case LOAD -> unloadMoveOrder(cranePlc, workingOrder, false);
                //case UNLOAD_MOVE -> unloadOrder(cranePlc, workingOrder, craneMst);
                //case UNLOAD -> unloadAndCompleteOrder(cranePlc, workingOrder);
                //case MOVE_HOME -> moveHomeComplete(cranePlc, workingOrder);
                //case MOVE_CAR_FROM_RACK_CV -> moveCarLift(cranePlc, workingOrder);
                //case MOVE_CAR_LIFT_MOVE -> moveLift(craneMst, cranePlc, workingOrder);
                //case MOVE_CAR_LIFT_CV -> moveCarToRackCv(cranePlc, workingOrder);
                //case MOVE_CAR_TO_RACK_CV -> moveCarComplete(craneMst, cranePlc, workingOrder);
            }
        }
    }

    /*
     * ====================================================
     * 2. 작업 상세 관리
     * ====================================================
     */

    /*
     * 검토중) 작업 상세 관리 - 대기중인 작업 중, 긴급지시 전달
     * 1. 작업할 스태커크레인 선택 및 조회
     * 2. 작업유형 별 작업지시
     */
    private void sendEmgOrder(StackerCraneContext context, TbEcsRackOrder emgOrder) {
        StackerCranePlc cranePlc = cranePlcManager(context, emgOrder);

        TbEqCraneMst craneMst = tbEqCraneMstService.selectByCraneId(context, cranePlc.getId());
        if (cranePlc == null || craneMst == null) {
            log.info("sendEmgOrder choiceCrane || craneMst NULL!");
            return;
        }

        var orderType = EcsDBConsts.OrderType.find(emgOrder.getOrderType());
        if (SINGLE_PATH_MODE) {
            switch (orderType) {
                case MOVE -> moveAndClearOrder(context, emgOrder, craneMst);
                case INBOUND -> inboundLoadMoveOrder(context, cranePlc, emgOrder, craneMst); // todo: 검토
                case OUTBOUND -> loadOrder(context, cranePlc, emgOrder, craneMst);  // todo: 검토
                case RACK_TO_RACK -> loadOrder(context, cranePlc, emgOrder, craneMst);  // todo: 검토
                case MOVE_HOME -> sendMoveHomeOrder(context, emgOrder);  // todo: 검토
            }
        } else {
            switch (orderType) {
                // todo: 구현
                //case INBOUND -> inboundLoadMoveOrder(context, cranePlc, emgOrder, craneMst);
                //case OUTBOUND -> outboundLoadMoveOrder(context, cranePlc, emgOrder, craneMst);
                //case MOVE_HOME -> sendMoveHomeOrder(emgOrder);
                //case MOVE_CAR_FLOOR -> moveCarFromRackCv(emgOrder);
            }
        }
    }

    /*
     * 검토완료) 작업 상세 관리 - 이동 작성 생성
     * - From To 방식인 경우
     * 1. 경로 생성위한 랙 상테 반영
     * 2. 경로 생성
     * 3. 주행가능 여부 확인
     * 4. 이동 및 이전 완료작업 CLEAR PLC 명령어 송신
     * 5. 랙 작업상태, 명령상태 변경
     * 6. 랙 상태 변경
     * - Step by Step 방식인 경우
     * ...
     */
    private void moveAndClearOrder(StackerCraneContext context, TbEcsRackOrder workingOrder, TbEqCraneMst craneMst) {
        log.info("[MOVE] StackerCrane={}, orderKey={}", craneMst.getEqId(), workingOrder.getOrderKey());

        reloadMap(context, false);

        if (SINGLE_PATH_MODE) {

            CraneCell fromCell = createCurrentCell(workingOrder, craneMst);
            CraneCell toCell = createTargetCell(workingOrder, craneMst);

            StackerCraneMoveCommand command = StackerCraneMoveCommand.builder()
                    .stackerCraneId(context.getCraneId())
                    .fromCell(fromCell)
                    .toCell(toCell)
                    .workId(nextWorkId())
                    .build();
            StackerCraneCommandResult result = cranePort.move(command);
            if (!result.isSuccess()) {
                log.warn("[MOVE FAILED] StackerCrane={}, result={}", craneMst.getEqId(), result.getDescription());
                return;
            }

            tbEcsRackOrderService.updateTbEcsRackOrder(craneMst.getEqId(), workingOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.MOVE, workId);
            tbEqRackMstService.updateTbEqRackMstStatus(context, result.getReservedPath(), EcsDBConsts.EqRackStatus.MOVE_RESERVE);

        } else {
            //todo: step by step 지정 방식 로직 구현
        }
    }

    /*
     * 검토완료) 작업 상세 관리 - 입출고대 컨베이어에 올라와있는 대기화물 입고 작업 진행
     * 1. 입출고대 입고대기 작업 우선순위 정렬
     * 2. 바코드 인식된 작업 (도착한 작업) 우선 진행
     */
    private void rackInCvCargoReadyOrder(StackerCraneContext context, List<TbEcsRackOrder> readyOrderList, List<TbEcsRouteOrder> rackInCvReadyOrderList) {
        log.info("rackInCvReadyOrderList size " + rackInCvReadyOrderList.size());

        rackInCvReadyOrderList.sort(Comparator.comparing(TbEcsRouteOrder::getPriority));

        TbEcsRackOrder readyOrder = null;
        for (TbEcsRouteOrder routeOrder : rackInCvReadyOrderList) {
            if (routeOrder.getBarcode() == null)
                continue;

            readyOrder = readyOrderList.stream().filter(order ->
                    order.getBarcode().equals(routeOrder.getBarcode())
                            && order.getOrderKey().equals(routeOrder.getOrderKey())).findFirst().orElse(null);
            if (readyOrder != null) {
                StackerCranePlc cranePlc = cranePlcManager(context, readyOrder);
                if (cranePlc != null) {
                    TbEqCraneMst craneMst = tbEqCraneMstService.selectByCraneId(context, cranePlc.getId());
                    if (craneMst != null)
                        inboundLoadMoveOrder(context, cranePlc, readyOrder, craneMst);
                }
                return;
            }
        }

        if (readyOrder == null)
            log.info("rackInCvCargoReadyOrder readyOrder matched null");
    }

    /*
     * 검토중) 작업 상세 관리 - 입고지시 로드이송 송신
     * 1. 입고 대기 중인 컨베이어 작업 조회
     * 2. 경로탐색 전 셀 상태 업데이트
     * 3. from 위치 도착 확인
     * 4-1. 도착시, 로드 지시 생성 후 종료
     * 4-2. 미도착시, 경로 생성 후 경로 설정
     */
    private void inboundLoadMoveOrder(StackerCraneContext context, StackerCranePlc cranePlc, TbEcsRackOrder readyOrder, TbEqCraneMst craneMst) {
        log.info(craneMst.getId() + " inboundLoadMoveOrder");

        List<TbEcsRouteOrder> routeOrderList = tbEcsRouteOrderService.selectTbEcsRouteOrder(context);
        TbEcsRouteOrder routeOrder = routeOrderList.stream()
                .filter(order -> order.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue()).findFirst().orElse(null);
        if (routeOrder == null) return;

        reloadMap(context, false);

        if (SINGLE_PATH_MODE) {
            CraneCell fromCell = createCurrentCell(readyOrder, craneMst);
            CraneCell toCell = createTargetCell(readyOrder, craneMst);

            List<CraneCell> movePaths = stackerCranePathService.findPath(fromCell, toCell);
            List<StackerCraneWriteMap> pathCmsList = stackerCranePathService.buildMovePath(movePaths);
            cranePlc.getCrane().setCranePathCmdList(pathCmsList);
        } else {
            boolean arrivedLoadTarget = isLoadTarget(readyOrder, craneMst);
            if (arrivedLoadTarget) {
                readyOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
                // todo: readyOrder status 변경내역 update 안하는지
                loadOrder(context, cranePlc, readyOrder, craneMst);
                return;
            } else {
                boolean canFindPath = true; // canLoadFindPath(cranePlc, readyOrder, craneMst);
                if (!canFindPath) {
                    // TODO : 로드 경로 생성 실패시 로직
                    return;
                } else {
                    CraneCell fromCell = createCurrentCell(readyOrder, craneMst);
                    CraneCell toCell = createTargetCell(readyOrder, craneMst);

                    List<CraneCell> movePaths = stackerCranePathService.findPath(fromCell, toCell);
                    List<StackerCraneWriteMap> pathCmsList = stackerCranePathService.buildMovePath(movePaths);
                    cranePlc.getCrane().setCranePathCmdList(pathCmsList);
                }
            }
        }

        StackerCraneWriteMap cmd = cranePlc.getCrane().getCranePathCmdList().get(cranePlc.getCrane().getReserveCmdCurrentIndex());
        List<CraneCell> curPath = cmd.getPath();

        // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
        if (stackerCranePathService.canUseCellPathByCellStatus(context, curPath, true, cranePlc.getCrane())) {
            int[] command = cmd.getMoveAndClearCommand(nextWorkId());
            cranePort.sendPlcCommand(cranePlc.getId(), command);
            tbEcsRackOrderService.updateTbEcsRackOrder(craneMst.getEqId(), readyOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
            tbEqRackMstService.updateTbEqRackMstStatus(context, curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
            return;
        } else {
            log.info(cranePlc.getCrane().getStackerCraneId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
        }
    }

    /*
     * 검토중) 작업 상세 관리 - 출고지시 로드이송 송신
     * 1. 경로탐색 전 셀 상태 업데이트
     * 2. 지시 상태 작업중으로 변경 (READY→WORKING)
     * 3.
     */
    private void outboundLoadMoveOrder(StackerCraneContext context, StackerCranePlc cranePlc, TbEcsRackOrder readyOrder, TbEqCraneMst craneMst) {
        log.info(craneMst.getId() + " outboundLoadMoveOrder");

        reloadMap(context, false);

        var arrivedLoadTarget = isLoadTarget(readyOrder, craneMst);
        if (arrivedLoadTarget) {
            readyOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
            // todo: readyOrder status 변경내역 update 안하는지
            loadOrder(context, cranePlc, readyOrder, craneMst);
            return;
        } else {
            var canFindPath = true; // canLoadFindPath(cranePlc, readyOrder, craneMst);
            if (!canFindPath) {
                // TODO : 로드 경로 생성 실패
                log.info(craneMst.getId() + " outboundLoadMoveOrder 로드 경로 생성 실패");
                return;
            }
        }

        StackerCraneWriteMap cmd = cranePlc.getCrane().getCranePathCmdList().get(cranePlc.getCrane().getReserveCmdCurrentIndex());
        List<CraneCell> curPath = cmd.getPath();

        // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
        if (stackerCranePathService.canUseCellPathByCellStatus(context, curPath, true, cranePlc.getCrane())) {
            int[] command = cmd.getMoveAndClearCommand(nextWorkId());
            cranePort.sendPlcCommand(cranePlc.getId(), command);
            tbEcsRackOrderService.updateTbEcsRackOrder(craneMst.getEqId(), readyOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
            tbEqRackMstService.updateTbEqRackMstStatus(context, curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
            return;
        } else {
            log.info(cranePlc.getCrane().getStackerCraneId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
        }
    }

    /*
     * 검토중) 작업 상세 관리 - 로드지시 전송 + 완료비트 초기화
     * - From To 방식 인 경우
     * 1. 출발지 화물 확인
     *
     * - Step by Step 방식인 경우
     *
     * 1. 목적지에 도착하지 못한 경우, 입출고 지시 재확인
     *
     * 2. 목적지에 도착한 경우, 작업번호로 이송 지시 확인
     * 3. 입출고대 작업인 경우
     * 3-1. 입출고대 컨베이어의 화물 유무 확인
     * 3-2. PLC 명령어 생성
     * 3-3. 작업 상태 "INBOUND" 도는 "OUTBOUND"로 변경 // TODO: INBOUND/OUTBOUND 상태 확인. 기존은 LOAD 상태로 변경
     *
     * 4. 입고대 작업이 아닌 경우
     * 2.1. PLC 명령어 생성
     * 2.4. 작업 상태 "RACK_TO_RACK"으로 변경  // TODO: 상태 확인. 기존은 LOAD 상태로 변경
     */
    private void loadOrder(StackerCraneContext context, StackerCranePlc cranePlc, TbEcsRackOrder workingOrder, TbEqCraneMst craneMst) {
        log.info(craneMst.getId() + " loadOrder");

        if (SINGLE_PATH_MODE) {
            int orderType = workingOrder.getOrderType();
            if (orderType == EcsDBConsts.OrderType.INBOUND.getValue()) {

            } else if (orderType == EcsDBConsts.OrderType.OUTBOUND.getValue()) {

            } else if (orderType == EcsDBConsts.OrderType.RACK_TO_RACK.getValue()) {

            } else if (orderType == EcsDBConsts.OrderType.MOVE.getValue()) {

            }


            TbEcsRouteOrder routeOrder = tbEcsRouteOrderService.selectTbEcsRouteOrder(context.getRackEqId(), workingOrder.getOrderKey());
            if (routeOrder != null) {
                // 입출고대 작업인 경우
                log.info(craneMst.getId() + " loadOrder isRackInRouteOrder");

                boolean isCargoArrived = routeOrder.getCmdStatus() == EcsDBConsts.EcsRouteOrderCmdStatus.STATION_READY.getValue();
                if (isCargoArrived) {
                    int[] command = cranePlc.getWriteMap().getPutawayAndClearCommand(nextWorkId());
                    cranePort.sendPlcCommand(cranePlc.getId(), command);

                    tbEcsRackOrderService.updateTbEcsRackOrder(cranePlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.LOAD, this.workId);
                }

            } else {
                // Rack to Rack 작업인 경우
                log.info(craneMst.getId() + " loadOrder else");

                int[] command = cranePlc.getWriteMap().getPutawayAndClearCommand(nextWorkId());
                cranePort.sendPlcCommand(cranePlc.getId(), command);
                tbEcsRackOrderService.updateTbEcsRackOrder(cranePlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.RACK_TO_RACK, this.workId);
            }
        } else {
            boolean isArrived = craneMst.getBay() == workingOrder.getFromBay() && craneMst.getLevel() == workingOrder.getFromAsiel(); // todo: 검토. 기존 getFromLevel();
            if (!isArrived) {
                log.info(craneMst.getId() + " loadOrder !isArrived");

                switch (EcsDBConsts.OrderType.find(workingOrder.getOrderType())) {
                    case INBOUND -> inboundLoadMoveOrder(context, cranePlc, workingOrder, craneMst);
                    case OUTBOUND -> outboundLoadMoveOrder(context, cranePlc, workingOrder, craneMst);
                }
            } else {
                TbEcsRouteOrder routeOrder = tbEcsRouteOrderService.selectTbEcsRouteOrder(context.getRackEqId(), workingOrder.getOrderKey());
                if (routeOrder != null) {
                    // 입출고대 작업인 경우
                    log.info(craneMst.getId() + " loadOrder isRackInRouteOrder");

                    boolean isCargoArrived = routeOrder.getCmdStatus() == EcsDBConsts.EcsRouteOrderCmdStatus.STATION_READY.getValue();
                    if (isCargoArrived) {
                        int[] command = cranePlc.getWriteMap().getPutawayAndClearCommand(nextWorkId());
                        cranePort.sendPlcCommand(cranePlc.getId(), command);

                        tbEcsRackOrderService.updateTbEcsRackOrder(cranePlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.LOAD, this.workId);
                    }

                } else {
                    // Rack to Rack 작업인 경우
                    log.info(craneMst.getId() + " loadOrder else");

                    int[] command = cranePlc.getWriteMap().getPutawayAndClearCommand(nextWorkId());
                    cranePort.sendPlcCommand(cranePlc.getId(), command);
                    tbEcsRackOrderService.updateTbEcsRackOrder(cranePlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.RACK_TO_RACK, this.workId);
                }
            }
        }
    }

    /* 작업 상세 관리 - 언로드 이송지시 전송 + 완료비트 초기화 */
    private void unloadMoveOrder(StackerCraneContext context, StackerCranePlc cranePlc, TbEcsRackOrder workingOrder, boolean isReOrder) {
        log.info(cranePlc.getId() + " unloadMoveOrder");
        reloadMap(context, isReOrder);
        //createUnloadPath(isReOrder, cranePlc, workingOrder);
        StackerCraneWriteMap cmd = cranePlc.getCrane().getCranePathCmdList().get(cranePlc.getCrane().getReserveCmdCurrentIndex());
        if (cmd == null)
            return;
        List<CraneCell> curPath = cmd.getPath();

        // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
        if (stackerCranePathService.canUseCellPathByCellStatus(context, curPath, false, cranePlc.getCrane())) {
            // 경로 명령 송신
            unloadMoveOrderPathSend(context, cmd, cranePlc, workingOrder);
            return;
        } else {
            log.info(cranePlc.getCrane().getStackerCraneId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
        }

        // // 이동 가능한 셀까지 이동
        // StackerCraneWriteMap prefixPath = canMoveCellPath(curPath, false);
        // if (prefixPath == null || prefixPath.getPath().isEmpty()) {
        //     log.info(cranePlc.getId() + " canMoveCellPath false prefix 경로 이동 불가. 대기중..");
        //     return;
        // }
        // cranePlc.getCrane().addPrePixCmd(prefixPath);
        // unloadMoveOrderPathSend(prefixPath, cranePlc, workingOrder);
    }

    /* 작업 상세 관리 - */
    private void unloadMoveOrderPathSend(StackerCraneContext context, StackerCraneWriteMap cmd, StackerCranePlc cranePlc, TbEcsRackOrder workingOrder) {
        log.info(cranePlc.getId() + " unloadMoveOrderPathSend");
        int[] command = cmd.getMoveAndClearCommand(nextWorkId());
        cranePort.sendPlcCommand(cranePlc.getId(), command);
        tbEcsRackOrderService.updateTbEcsRackOrder(workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD_MOVE, workId);
        tbEqRackMstService.updateTbEqRackMstStatus(context, cmd.getPath(), EcsDBConsts.EqRackStatus.MOVE_RESERVE);
        boolean isInboundTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue();
        if (isInboundTypeOrder) {
            // 입고 타입 언로드 할 경우, 목적지 셀 화물 예약으로 변경
            tbEqRackMstService.updateTbEqRackMstStatus(context, workingOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO_RESERVE);
        }
    }

    /* 작업 상세 관리 - 언로드 지시 전송 + 완료비트 초기화 */
    private void unloadOrder(StackerCraneContext context, StackerCranePlc cranePlc, TbEcsRackOrder workingOrder, TbEqCraneMst craneMst) {
        // TODO 카의 위치와 목적지 위치가 동일하지 않으면, 다시 로드이송지시 경로계산부터

        log.info(cranePlc.getId() + " unloadOrder");
        boolean isArrived = craneMst.getBay() == workingOrder.getToBay() && craneMst.getLevel() == workingOrder.getToAsiel(); // todo: 검토. 기존 getToLevel();
        if (!isArrived) {
            unloadMoveOrder(context, cranePlc, workingOrder, false);
        } else {
            int[] command = cranePlc.getWriteMap().getUnloadAndClearCommand(nextWorkId());
            cranePort.sendPlcCommand(cranePlc.getId(), command);
            tbEqRackMstService.updateTbEqRackMstStatus(context, workingOrder.getFromLocId(), EcsDBConsts.EqRackStatus.READY);
            tbEcsRackOrderService.updateTbEcsRackOrder(workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD, this.workId);
        }
    }

    /*
     * 검토완료) 작업 상세 관리 - 지시 완료처리 // todo: 기존: unloadAndCompleteOrder
     * 1. fork1 완료 작업 clear PLC 명령 송신
     * 2. 랙 작업 완료 처리
     * 3. 입고 작업이면
     * 3-1. 목적위치 - 적재로 변경
     * 3-2. 출발위치 - 대기로 변경
     * 4. 출고 작업이면
     * 4-1. 목적위치 - station 대기로 변경
     * 4-2. 출발위치 - 대기로 변경
     * 5. Station to Station 작업이면
     * 5-1. 목적위치 - station 대기로 변경
     * 5-2. 출발위치 - 대기로 변경
     * 6. Rack to Rack 작업이면
     * 6-1. 목적위치 - 적재로 변경
     * 6-2. 출발위치 - 대기로 변경
     */
    private void completeOrder(StackerCraneContext context, StackerCranePlc cranePlc, TbEcsRackOrder workingOrder) {
        log.info(cranePlc.getId() + " unloadAndCompleteOrder");

        int[] command = cranePlc.getWriteMap().getCompleteClearCommand();
        cranePort.sendPlcCommand(cranePlc.getId(), command);
        // TODO: CLEAR FAIL 시 로직 추가

        tbEcsRackOrderService.updateTbEcsRackOrder(workingOrder, EcsDBConsts.OrderStatus.COMPLETE);

        boolean isInboundTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue();
        boolean isOutboundTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.OUTBOUND.getValue();
        boolean isStationTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.STATION_TO_STATION.getValue();
        boolean isRackTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.RACK_TO_RACK.getValue();

        if (isInboundTypeOrder) {
            tbEqRackMstService.updateTbEqRackMstStatus(context, workingOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO);
            tbEcsRouteOrderService.updateTbEcsRouteOrder(context, workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.COMPLETE);
        } else if (isOutboundTypeOrder) {
            tbEqRackMstService.updateTbEqRackMstStatus(context, workingOrder.getFromLocId(), EcsDBConsts.EqRackStatus.READY);
            tbEcsRouteOrderService.updateTbEcsRouteOrder(context, workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.STATION_READY);
        } else if (isStationTypeOrder) {
            tbEcsRouteOrderService.updateTbEcsRouteOrder(context, workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.READY);
            tbEcsRouteOrderService.updateTbEcsRouteOrder(context, workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.STATION_READY);
        } else if (isRackTypeOrder) {
            tbEqRackMstService.updateTbEqRackMstStatus(context, workingOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO);
            tbEqRackMstService.updateTbEqRackMstStatus(context, workingOrder.getFromLocId(), EcsDBConsts.EqRackStatus.READY);
        }
    }

    /*
     * 검토중) 작업 상세 관리 - 크레인 홈 이동 송신
     */
    private void sendMoveHomeOrder(StackerCraneContext context, TbEcsRackOrder moveHomeOrder) {
        log.info(moveHomeOrder.getEqCraneId() + " sendMoveHomeOrder");

        List<TbEqCraneMst> craneMstList = tbEqCraneMstService.selectCraneStatus(context);
        TbEqCraneMst craneMst = craneMstList.stream().filter(crane -> crane.getEqId().equals(moveHomeOrder.getEqCraneId())).findFirst().orElse(null);
        StackerCranePlc cranePlc = cranePort.getStackerCranePlc(moveHomeOrder.getEqCraneId());

        if (craneMst != null) {
            reloadMap(context, true);
            // 이미 홈 위치인지
            var isAlreadyTarget = isAlreadyHome(moveHomeOrder, craneMst);
            if (!isAlreadyTarget) {
                // 경로 생성
                var isFindPathComplete = true; // moveHomeFindPath(cranePlc, moveHomeOrder, craneMst);
                if (isFindPathComplete) {
                    StackerCraneWriteMap cmd = cranePlc.getCrane().getCranePathCmdList().get(cranePlc.getCrane().getReserveCmdCurrentIndex());
                    List<CraneCell> curPath = cmd.getPath();
                    // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
                    if (stackerCranePathService.canUseCellPathByCellStatus(context, curPath, true, cranePlc.getCrane())) {
                        int[] command = cmd.getMoveAndClearCommand(nextWorkId());
                        cranePort.sendPlcCommand(cranePlc.getId(), command);
                        tbEcsRackOrderService.updateTbEcsRackOrder(moveHomeOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.MOVE_HOME, workId);
                        tbEqRackMstService.updateTbEqRackMstStatus(context, curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                    } else {
                        log.info(cranePlc.getCrane().getStackerCraneId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
                    }
                } else {
                    // TODO : 홈이동 경로 생성 실패
                }
            } else {
                // 지시 완료처리
                log.info(cranePlc.getCrane().getStackerCraneId() + " sendMoveHomeOrder isAlreadyTarget");
                tbEcsRackOrderService.updateTbEcsRackOrder(moveHomeOrder, EcsDBConsts.OrderStatus.COMPLETE);
            }
        }
    }

    /*
     * 검토중) 작업 상세 관리 - 홈 이동지시 목적지 도착 여부 확인
     * - todo: rackId 구조 확인, ReadMap 추가 필요, home 위치 order에 지정 필요
     */
    private boolean isAlreadyHome(TbEcsRackOrder moveHomeOrder, TbEqCraneMst craneMst) {
        int fromBay = Integer.parseInt(craneMst.getRackId().substring(1, 3));
        int fromLevel = Integer.parseInt(craneMst.getRackId().substring(3, 5));
        int toBay = Integer.parseInt(moveHomeOrder.getToLocId().substring(1, 3));
        int toLevel = Integer.parseInt(moveHomeOrder.getToLocId().substring(3, 5));
        log.info("PATH from : " + fromBay + ", " + fromLevel + ", to" + toBay + ", " + toLevel);
        return stackerCranePathService.isTarget(fromBay, fromLevel, toBay, toLevel);
    }

    /*
     * 검토완료) 작업 상세 관리 - 홈포지션 이동 완료시,
     * 1. PLC 완료작업 Clear
     * 2. 랙 작업 완료
     */
    private void moveHomeComplete(StackerCranePlc cranePlc, TbEcsRackOrder workingOrder) {
        log.info(cranePlc.getId() + " moveHomeComplete");

        tbEcsRackOrderService.updateTbEcsRackOrder(workingOrder, EcsDBConsts.OrderStatus.COMPLETE);

        int[] command = cranePlc.getWriteMap().getCompleteClearCommand();
        cranePort.sendPlcCommand(cranePlc.getId(), command);
    }

    /*
     * ====================================================
     * 3. 랙 관리
     * ====================================================
     */

    /*
     * 검토완료) 랙 관리 - 스태커크레인 마지막 지시 셀의 점유 해제
     * - 호출시점 : 작업 완료 후
     */
    private void completedCellUpdateStatus(StackerCraneContext context, StackerCrane crane) {
        log.info(crane.getStackerCraneId() + " completedCellUpdateStatus");

        List<StackerCraneWriteMap> commands = crane.getCranePathCmdList();
        if (commands != null && !commands.isEmpty()) {
            List<CraneCell> completePaths = commands.size() > crane.getReserveCmdCurrentIndex()
                    ? commands.get(crane.getReserveCmdCurrentIndex()).getPath()
                    : null;

            if (completePaths != null && !completePaths.isEmpty()) {
                tbEqRackMstService.updateTbEqRackMstStatus(context, completePaths, EcsDBConsts.EqRackStatus.READY);
            }
            log.info(crane.getStackerCraneId() + " completedCellUpdateStatus complete");
        }
    }

    /*
     * 검토완료) 랙 관리 - 경로 탐색 전 현재 랙 정보 조회
     * 1. 랙 설비, asiel이 동일한 셀 정보 조회
     * 2. 랙 설비, asiel이 동일한 크레인 전체 조회
     * 2-1. 점검중인 크레인 조회
     * 3. 영구금지 셀은 경로로 사용 불가처리 / todo : 지나는 것 가능하도록
     * 4. 화물 적재되어있거나, 적재 예정인 셀은 경로로 사용 불가 처리
     * 5. 다른 크레인이 위치한 셀은 경로로 사용 불가 처리 (다른카의 병목을 위한 이동인 경우 예약셀 및 다른 카 위치 우회) // todo: 주석 해석 필요
     */
    private void reloadMap(StackerCraneContext context, boolean isOtherCraneMove) {
        log.info("reloadMap");

        List<TbEqRackMst> racks = tbEqRackMstService.selectCellStatus(context);
        List<TbEqCraneMst> cranes = tbEqCraneMstService.selectCraneStatus(context);

        // todo: 설비 점검중인 크레인 제외
        TbEqCraneMst disabledCrane = cranes.stream()
                .filter(crane -> crane.getStatus() == 99)
                .findFirst().orElse(null);

        for (TbEqRackMst rack : racks) {
            if (rack.getType() == EcsDBConsts.RackType.BAN_CELL.getValue()) {
                log.info("reloadMap updateEnabledCell false" + rack.getAsiel() + ", " + rack.getBay() + ", " + rack.getLevel());
                stackerCranePathService.updateEnabledCell(rack.getAsiel(), rack.getBay(), rack.getLevel(), false);
                continue;
            }

            // todo: isCargoYn과 status의 Cargo상태 동일한 상태인지 확인 후, 컬럼 삭제 고려
            boolean hasCargo = rack.getStatus() == EcsDBConsts.EqRackStatus.CARGO.getValue()
                    || rack.getStatus() == EcsDBConsts.EqRackStatus.CARGO_RESERVE.getValue()
                    || rack.isCargoYn();
            stackerCranePathService.updateCargoCell(rack.getAsiel(), rack.getBay(), rack.getLevel(), hasCargo);

            stackerCranePathService.updateEnabledCell(rack.getAsiel(), rack.getBay(), rack.getLevel(), rack.isUseYn());
        }

        // 다른카 병목으로 인한 이송지시인 경우, 다른 카의 위치 셀 우회(사용 안함으로 변경)
        if (isOtherCraneMove) {
            List<TbEqCraneMst> otherCraneMsts = tbEqCraneMstService.selectOtherCraneStatus(context, context.getCraneId());

            if (otherCraneMsts != null && !otherCraneMsts.isEmpty()) {
                TbEqCraneMst otheCrane = otherCraneMsts.get(0);
                log.info("reloadMap updateEnabledCell false" + otheCrane.getAsiel() + ", " + otheCrane.getBay() + ", " + otheCrane.getLevel());
                stackerCranePathService.updateEnabledCell(otheCrane.getAsiel(), otheCrane.getBay(), otheCrane.getLevel(), false);
            }
        }
        log.info("reloadMap complete");
    }

    /* 검토완료) 랙 관리 - 로드할 목적지 도착 확인 */
    private boolean isLoadTarget(TbEcsRackOrder workingOrder, TbEqCraneMst craneMst) {
        int fromBay = Integer.parseInt(craneMst.getRackId().substring(1, 3));
        int fromLevel = Integer.parseInt(craneMst.getRackId().substring(3, 5));
        int toBay = Integer.parseInt(workingOrder.getFromLocId().substring(1, 3));
        int toLevel = Integer.parseInt(workingOrder.getFromLocId().substring(3, 5));
        return stackerCranePathService.isTarget(fromBay, fromLevel, toBay, toLevel);
    }

    /*
     * ====================================================
     * 4. 설비 관리
     * ====================================================
     */

    /*
     * 검토완료) 설비 관리 - 작업할 크레인 조회 및 선택
     * 1. 현재 랙의 크레인 조회
     * 2. 지시에 크레인 지정되어있는 경우, 크레인 조회
     * 3. 지시에 크레인 지정되어있지 않은 경우, 크레인 선택 후 조회
     */
    private StackerCranePlc cranePlcManager(StackerCraneContext context, TbEcsRackOrder order) {
        log.info("cranePlcManager");

        List<TbEqCraneMst> craneMsts = tbEqCraneMstService.selectCraneStatus(context);
        List<TbEqCraneMst> curCranes = craneMsts.stream().filter(crane ->
                        crane.getRackEqId().equals(context.getRackEqId()))
                .collect(Collectors.toList()); // todo: asiel 조건 재확인

        if (order.getEqCraneId() != null && !order.getEqCraneId().equals("")) {
            log.info("getEqCraneId()() != null");
            return cranePort.getStackerCranePlc(order.getEqCraneId());
        } else {
            log.info("크레인 선택");
            // todo: MOVE_HOME target 분기해야되는 지 확인
            int targetBay = order.getFromBay();
            int targetLevel = order.getFromlevel(); // todo: 검토. 기존 getFromLevel();

            StackerCranePlc cranePlc = choiceCrane(curCranes, targetBay, targetLevel);
            if (cranePlc == null) {
                log.info("cranePlcManager choiceCrane NULL!");
                return null;
            }

            return cranePlc;
        }
    }

    /*
     * 검토완료) 설비 관리 - 크레인 선정 (상세)
     * 1. 작업 가능한 스태커크레인 필터링
     * - 자동모드
     * - 스태커크레인 대기 상태
     * - 할당된 경로 없는 상태
     * - 화물 없는 상태
     * 2. 작업 출발지와 가장 가까운 크레인 선정
     */
    private StackerCranePlc choiceCrane(List<TbEqCraneMst> curCranes, int targetBay, int targetLevel) {
        log.info("choiceCrane");

        List<StackerCranePlc> readyCranePLc = new ArrayList<>();

        curCranes.stream().forEach(craneMst -> {
            StackerCranePlc cranePlc = cranePort.getStackerCranePlc(craneMst.getEqId());
            if (cranePlc == null) {
                log.info(craneMst.getId() + " choiceCrane cranePlc is NULL");
                return;
            }

            boolean isAuto = craneMst.isAutoYn();
            boolean isReadyStatus = craneMst.getStatus() == EcsDBConsts.EqCraneStatus.READY.getValue();
            boolean isPathFinish = cranePlc.getCrane().getReserveCmdSize() == 0;
            boolean hasCargo = craneMst.isCargoYn();
            log.info(craneMst.getId() + " isReadyStatus : " + isReadyStatus);
            log.info(craneMst.getId() + " isPathFinish : " + isPathFinish);
            if (!isPathFinish) {
                log.info(craneMst.getId() + " path size " + cranePlc.getCrane().getReserveCmdSize());
            }
            log.info(craneMst.getId() + " hasCargo : " + hasCargo);
            if (isAuto && isReadyStatus && isPathFinish && !hasCargo) {
                readyCranePLc.add(cranePlc);
            }
        });

        log.info("[choiceCrane] readyCranePLc size : " + readyCranePLc.size());
        if (readyCranePLc.size() == 1) {
            return readyCranePLc.get(0);
        } else if (readyCranePLc.size() > 1) {
            Set<String> readyPlcSet = readyCranePLc.stream()
                    .map(StackerCranePlc::getId)
                    .collect(Collectors.toSet());

            TbEqCraneMst crane = curCranes.stream()
                    .filter(craneMst -> readyPlcSet.contains(craneMst.getEqId()))
                    .min(Comparator.comparingInt(craneMst ->
                            Math.abs(craneMst.getBay() - targetBay) +
                                    Math.abs(craneMst.getLevel() - targetLevel)
                    ))
                    .orElse(null);
            if (crane == null) {
                log.info("[choiceCrane] crane == null");
                return null;
            }

            log.info("[choiceCrane] " + crane.getId());
            return cranePort.getStackerCranePlc(crane.getId());
        }
        return null;
    }

    private CraneCell createCurrentCell(TbEcsRackOrder order, TbEqCraneMst craneMst) {
        if (order == null || craneMst == null) {
            return null;
        }

        //todo: asiel, bay, level 추출 로직 검토
        int fromAsiel = order.getFromAsiel();
        int fromBay = Integer.parseInt(craneMst.getRackId().substring(1, 3));
        int fromLevel = Integer.parseInt(craneMst.getRackId().substring(3, 5));
        return new CraneCell(null, fromAsiel, fromBay, fromLevel);
    }

    private CraneCell createTargetCell(TbEcsRackOrder order, TbEqCraneMst craneMst) {
        //todo: asiel, bay, level 추출 로직 검토
        int toAsiel = order.getToAsiel();
        int toBay = Integer.parseInt(order.getFromLocId().substring(1, 3));
        int toLevel = Integer.parseInt(order.getFromLocId().substring(3, 5));
        return new CraneCell(null, toAsiel, toBay, toLevel);
    }
}