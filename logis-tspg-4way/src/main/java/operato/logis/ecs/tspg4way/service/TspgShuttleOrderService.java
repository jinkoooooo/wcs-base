package operato.logis.ecs.tspg4way.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.ConveyorWriteConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayReadConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayWriteConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Cell;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttleCar;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayPathService;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayWriteMap;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.ecs.tspg4way.entity.*;
import operato.logis.ecs.tspg4way.domain.registry.TspgShuttleMapRegistry;
import operato.logis.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import lombok.extern.slf4j.Slf4j;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class TspgShuttleOrderService {

    private IQueryManager iQueryManager = BeanUtil.get(IQueryManager.class);
    private final TspgShuttlePlcRegistry tspgShuttlePlcRegistry;
    private final TspgShuttleMapRegistry tspgShuttleMapRegistry;
    private final TspgShuttlePlcWriteService tspgShuttlePlcWriteService;
    private final TspgConveyorPlcWriteService tspgConveyorPlcWriteService;
    private String rackEqId;
    private String cvEqId;
    private int floor;

    private Shuttle4WayPathService pathService; // this.rack && this.floor
    private int workId = 1001;

    private int nextWorkId() {
        workId++;
        if (workId > 9999) {
            workId = 1001;
        }
        return workId;
    }

    private ObjectMapper mapper = null;

    public TspgShuttleOrderService(TspgShuttlePlcRegistry tspgShuttlePlcRegistry, TspgShuttleMapRegistry tspgShuttleMapRegistry,
                                   TspgShuttlePlcWriteService tspgShuttlePlcWriteService, TspgConveyorPlcWriteService tspgConveyorPlcWriteService,
                                   String rackEqId, int floor) {
        this.tspgShuttlePlcRegistry = tspgShuttlePlcRegistry;
        this.tspgShuttleMapRegistry = tspgShuttleMapRegistry;
        this.tspgShuttlePlcWriteService = tspgShuttlePlcWriteService;
        this.tspgConveyorPlcWriteService = tspgConveyorPlcWriteService;
        this.rackEqId = rackEqId;
        this.floor = floor;
        this.pathService = tspgShuttleMapRegistry.getMapinfo(this.rackEqId, floor);
        var cvMst = selectEqMst();
        this.cvEqId = cvMst.getId();
        this.mapper = new ObjectMapper();
    }

    public void work() {
        ShuttleCarOrderManager();
    }

    /**
     * 셔틀 카 지시 관리
     */
    private void ShuttleCarOrderManager() {
        List<TbEcsRackOrder> orderList = selectTbEcsRackOrder();

        if (orderList != null && orderList.isEmpty()) {
            // log.info(this.floor+"층 "+"[TspgShuttleOrderService] ShuttleCarOrderManager orderList is Empty");
            return;
        }

        Map<Integer, List<TbEcsRackOrder>> orderMap = orderList.stream().collect(Collectors.groupingBy(TbEcsRackOrder::getOrderStatus));
        List<TbEcsRackOrder> readyOrderList = orderMap.get(EcsDBConsts.OrderStatus.READY.getValue());
        List<TbEcsRackOrder> sendOrderList = orderMap.get(EcsDBConsts.OrderStatus.EQ_SEND.getValue());
        List<TbEcsRackOrder> workingOrderList = orderMap.get(EcsDBConsts.OrderStatus.WORKING.getValue());

        if (sendOrderList != null && !sendOrderList.isEmpty()) {
            // log.info(this.floor+"층 "+"[ShuttleCarOrderManager] sendOrderList : " + sendOrderList.size());
            eqSendOrderManager(sendOrderList);
        }
        if (workingOrderList != null && !workingOrderList.isEmpty()) {
            // log.info(this.floor+"층 "+"[ShuttleCarOrderManager] workingOrderList : " + workingOrderList.size());
            workingOrderManager(workingOrderList);
        }
        if (readyOrderList != null && !readyOrderList.isEmpty()) {
            // log.info(this.floor+"층 "+"[ShuttleCarOrderManager] readyOrderList : " + readyOrderList.size());
            readyOrderManager(readyOrderList);
        }
    }

    /**
     * 설비로 전송된 지시 관리
     * 지시의 설비 작업번호 == 설비가 완료한 작업번호
     * || 설비 가동
     * >> 설비 작업중 업데이트
     */
    private void eqSendOrderManager(List<TbEcsRackOrder> sendOrderList) {
        List<TbEqCarMst> eqCarMstList = selectCarStatus();
        for (TbEcsRackOrder sendOrder : sendOrderList) {
            Optional<TbEqCarMst> carOpt = eqCarMstList.stream()
                    .filter(car -> car.getEqId().equals(sendOrder.getEqCarId())
                            && car.getPlcCmdId() == sendOrder.getPlcCmdId()).findFirst();
            if (carOpt.isPresent()) {
                TbEqCarMst carMst = carOpt.get();
                boolean isRun = carMst.getStatus() == EcsDBConsts.EqCarStatus.RUN.getValue();
                boolean isComplete = sendOrder.getPlcCmdId() == carMst.getPlcCompCmdId();
                // 동작중이거나 완료했으면
                if (isRun || isComplete) {
                    updateTbEcsRackOrder(sendOrder, EcsDBConsts.OrderStatus.WORKING);
                }
            }
        }
    }


    /**
     * 진행중인 지시 관리
     */
    // TODO 공출고, 이중입고 처리 추가, 카 3개이상시 수정필요
    // 공출고 : 상위보고(오더키,에러타입), 현재 작업 삭제
    // 이중입고 : 상위보고(오더키, 에러타입, 버퍼셀), 버퍼셀 언로드
    private void workingOrderManager(List<TbEcsRackOrder> workingOrderList) {
        workingOrderList.sort(Comparator.comparing(TbEcsRackOrder::getPriority));
        List<TbEqCarMst> eqCarMstList = selectCarStatus();
        for (TbEcsRackOrder workingOrder : workingOrderList) {
            // 다른카로 인한 병목을 대기하는 지시인 경우 대기 상태가 해제될 때까지 패스
            if(workingOrder.isWaitYn()){
                log.info(this.floor + "층 workingOrderManager " + workingOrder.getOrderKey() + ": 병목현상 대기");
                // 병목 해소가 되었는지 확인
                if(isPathCleanByOtherCarMove(workingOrder)){
                    avoidMoveComplete(workingOrder);
                }
                continue;
            }

            // 이동 경로 예약 해제
            TbEqCarMst moveCarMst = eqCarMstList.stream() .filter(car -> car.getEqId().equals(workingOrder.getEqCarId())
                    && (car.getPlcCompCmdId() == workingOrder.getPlcCmdId() || car.getPlcCmdId() == workingOrder.getPlcCmdId() ) ).findFirst().orElse(null);
            if(moveCarMst != null)
                carPathCellReadyUpdate2(moveCarMst);

            // 지시에 작업번호가 지시에 해당하는 카의 완료작업번호에 올라왔는지
            TbEqCarMst carMst = eqCarMstList.stream() .filter(car -> car.getEqId().equals(workingOrder.getEqCarId()) && car.getPlcCompCmdId() == workingOrder.getPlcCmdId()).findFirst().orElse(null);
            if (carMst == null) {
                // log.warn(this.floor+"층 "+"workingOrderManager 지시에 해당하는 카와 동일카, 지시의 작업번호와 동일 완료작업번호의 셔틀 카 조회 실패");
            } else {
                log.info(this.floor + "층 " + carMst.getId() + " workingOrderManager : " +workingOrder.getOrderKey() +" : "+ carMst.isCompleteYn());
                // 셔틀카 작업 완료
                if (carMst.isCompleteYn()) {
                    Tspg4WayShuttlePlc carPlc = tspgShuttlePlcRegistry.getEquipment(carMst.getEqId());
                    Tspg4WayShuttleCar car = carPlc.getCar();
                    if (car != null) {
                        // 현재 위치 대기 상태로 변경
                        updateTbEqRackMstStatus(carMst.getRackId(), EcsDBConsts.EqRackStatus.READY);

                        // 경로 남아있음
                        if (car.hasNextCmd()) {
                            log.info(this.floor + "층 " + car.getShuttleCarId() + " 경로 있음");
                            // 다음 경로 지시 송신
                            boolean isSendNextPath = carNextPathSendManager(car, carMst, workingOrder);
                            log.info(this.floor + "층 " + car.getShuttleCarId() + " isSendNextPath : " + isSendNextPath);
                            if(isSendNextPath){
                                log.info(this.floor + "층 " + car.getShuttleCarId() + " workingOrderManager break");
                                break;
                            }
                        }
                        // 남은 경로 없음
                        else {
                            log.info(this.floor + "층 " + car.getShuttleCarId() + " 경로 없음");
                            // 지시의 로드 언로드에 따른 목적지와 현재 위치가 같은 경우는 완료후 다음 스텝
                            boolean isArrivedTarget = false;
                            switch (EcsDBConsts.EcsRackOrderCmdStatus.find(workingOrder.getCmdStatus())) {
                                case LOAD_MOVE, LOAD, CHARGE_MOVE, MOVE_CAR_FROM_RACK_CV ->
                                        isArrivedTarget = workingOrder.getFromLocId().equals(carMst.getRackId());
                                case MOVE_HOME, UNLOAD_MOVE, UNLOAD, MOVE_CAR_TO_RACK_CV ->
                                        isArrivedTarget = workingOrder.getToLocId().equals(carMst.getRackId());
                                case MOVE_CAR_LIFT_MOVE,MOVE_CAR_LIFT_CV -> isArrivedTarget = true;
                            }
                            if (isArrivedTarget) {
                                log.info(this.floor + "층 " + car.getShuttleCarId() + " isArrivedTarget");
                                completeOrderManager(carMst, carPlc, workingOrder);
                            } else {
                                log.info(this.floor + "층 " + car.getShuttleCarId() + " reOrderManager");
                                reOrderManager(carMst, carPlc, workingOrder);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 지시 셔틀카의 계산된 경로와 현재 주행하려는 경로 순번의 병목이 해소되었는지 확인
     */
    private boolean isPathCleanByOtherCarMove(TbEcsRackOrder workingOrder) {
        var waitCarPlc = tspgShuttlePlcRegistry.getEquipment(workingOrder.getEqCarId());
        var waitCar = waitCarPlc.getCar();
        List<Shuttle4WayWriteMap> cmdList = waitCar.getShuttlePathCmdList();
        if(cmdList.size() == 0)
            return true;
        Shuttle4WayWriteMap curCmd = cmdList.get(waitCar.getReserveCmdCurrentIndex());
        List<Cell> path = curCmd.getPath();

        // 현재 대기카의 경로에 다른카가 경로에 있거나, 다른카가 주행경로와 겹치는지 확인.
        List<TbEqCarMst> otherCarMstList = selectOtherCarStatus(waitCar.getShuttleCarId());
        for(var otherCarMst : otherCarMstList){
            var otherCarPlc = tspgShuttlePlcRegistry.getEquipment(otherCarMst.getEqId());
            var otherCar = otherCarPlc.getCar();
            List<Shuttle4WayWriteMap> otherCarCmdList = otherCar.getShuttlePathCmdList();
            Shuttle4WayWriteMap otherCarCurCmd = new  Shuttle4WayWriteMap();
            List<Cell> otherCarPath = new  ArrayList<>();
            if(otherCarCmdList.size() != 0){
                otherCarCurCmd = otherCarCmdList.get(waitCar.getReserveCmdCurrentIndex());
                otherCarPath = otherCarCurCmd.getPath();
            }
            for(var cell: path) {
                var isCarInCell = otherCarMst.getRow() == cell.getLocY() && otherCarMst.getBay() == cell.getLocX() ? true : false;
                if(isCarInCell){
                    return false;
                }
                if(otherCarPath != null && otherCarPath.size() != 0){
                    for(var cell2: otherCarPath) {
                        var isCarPathInCell = cell.getLocX() == cell2.getLocX() && cell.getLocY() == cell2.getLocY();
                        if(isCarPathInCell){
                            return false;
                        }
                    }
                }
            }
        }

        return true;
        // 해당 경로 병목이 해소된 경우
        // boolean isUnLoadMove = workingOrder.getCmdStatus() == EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD_MOVE.getValue()
        //         || workingOrder.getCmdStatus() == EcsDBConsts.EcsRackOrderCmdStatus.LOAD.getValue() ;
        // return canUseCellPathByCellStatus(path, !isUnLoadMove, waitCar);
    }

    /**
     * 셔틀 카 지시의 경로 완료 관리
     */
    private void carPathCellReadyUpdate(Tspg4WayShuttleCar car) {
        log.info(this.floor + "층 " + car.getShuttleCarId() + " carPathCellReadyUpdate");
        List<Shuttle4WayWriteMap> cmdList = car.getShuttlePathCmdList();
        if (cmdList != null && !cmdList.isEmpty()) {
            List<Cell> carPath = cmdList.size() > car.getReserveCmdCurrentIndex() ?  cmdList.get(car.getReserveCmdCurrentIndex()).getPath() : null;
            // 주행한 경로, 주행예약 상태 해제
            if(carPath != null && carPath.size() != 0)
                updateTbEqRackMstStatus(carPath, EcsDBConsts.EqRackStatus.READY);
            log.info(this.floor + "층 " + car.getShuttleCarId() + " carPathCellReadyUpdate comp");
        }
    }

    /**
     * 셔틀 카 지시의 경로 우선 예약헤제  관리
     */
    private void carPathCellReadyUpdate2(TbEqCarMst carMst) {
        Tspg4WayShuttlePlc carPlc = tspgShuttlePlcRegistry.getEquipment(carMst.getEqId());
        Tspg4WayShuttleCar car = carPlc.getCar();
        List<Shuttle4WayWriteMap> cmdList = car.getShuttlePathCmdList();
        if (cmdList != null && !cmdList.isEmpty()) {
            List<Cell> curPath = cmdList.size() > car.getReserveCmdCurrentIndex() ? cmdList.get(car.getReserveCmdCurrentIndex()).getPath() : null;
            // 현재 경로, 주행완료부분 주행예약 상태 해제
            if(curPath != null && curPath.size() != 0){
                var curCmd = cmdList.get(car.getReserveCmdCurrentIndex());
                boolean isRowMove = curCmd.getFromCell().getLocX() == curCmd.getToCell().getLocX();
                boolean isBayMove = curCmd.getFromCell().getLocY() == curCmd.getToCell().getLocY();
                int carBay = carMst.getBay();
                int carRow = carMst.getRow();
                List<Cell> targetCell = new ArrayList<>();
                if(isRowMove){
                    boolean isUpMove = curCmd.getFromCell().getLocY() <= curCmd.getToCell().getLocY();
                    log.info(this.floor + "층 " + car.getShuttleCarId() + " isRowMove - isUpMove:"+isUpMove);
                    if(isUpMove)
                        targetCell = curPath.stream().filter(path -> path.getLocX() == carBay && path.getLocY() < carRow).collect(Collectors.toList());
                    else
                        targetCell = curPath.stream().filter(path -> path.getLocX() == carBay && path.getLocY() > carRow).collect(Collectors.toList());
                }
                else if(isBayMove){
                    boolean isRightMove = curCmd.getFromCell().getLocX() <= curCmd.getToCell().getLocX();
                    log.info(this.floor + "층 " + car.getShuttleCarId() + " isBayMove - isRightMove:"+isRightMove);
                    if(isRightMove)
                        targetCell = curPath.stream().filter(path -> path.getLocY() == carRow && path.getLocX() < carBay).collect(Collectors.toList());
                    else
                        targetCell = curPath.stream().filter(path -> path.getLocY() == carRow && path.getLocX() > carBay).collect(Collectors.toList());
                }
                if(targetCell.size() > 0){
                    log.info(this.floor + "층 " + car.getShuttleCarId() + " targetCell.size() "+targetCell.size());
                    updateTbEqRackMstStatus(targetCell, EcsDBConsts.EqRackStatus.READY);
                }
            }
        }
    }


    /**
     * 셔틀 카 지시의 다음 경로 관리
     */
    private boolean carNextPathSendManager(Tspg4WayShuttleCar car, TbEqCarMst carMst, TbEcsRackOrder workingOrder) {
        boolean isLoadMove = workingOrder.getCmdStatus() == EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE.getValue();
        List<Shuttle4WayWriteMap> cmdList = car.getShuttlePathCmdList();
        Shuttle4WayWriteMap curCmd = new Shuttle4WayWriteMap();
        if (cmdList == null || cmdList.isEmpty()) {
            log.warn(this.floor + "층" + car.getShuttleCarId() + " carPathNextManager cmdList is empty");
            return false;
        }
        // 이전 주행 명령이 있었으면
        var beforeCarLocationToCmd =  cmdList.stream().filter(cmd->cmd.getToCell().getLocX() == carMst.getBay() && cmd.getToCell().getLocY() == carMst.getRow()).findAny().orElse(null);
        if(beforeCarLocationToCmd != null){
            car.updateReserveCmdIndex(cmdList.indexOf(beforeCarLocationToCmd));
        }
        // 현재 주행 명령
        var nowCarLocationFromCmd =  cmdList.stream().filter(cmd->cmd.getFromCell().getLocX() == carMst.getBay() && cmd.getFromCell().getLocY() == carMst.getRow()).findAny().orElse(null);
        if(nowCarLocationFromCmd != null){
            curCmd = nowCarLocationFromCmd;
        }else{
            carPathReset(car.getShuttleCarId(), false);
            return false;
        }
        List<Cell> curPath = curCmd.getPath();
        // 경로 이용 확인, 다른 셔틀카 위치로
        if (canUseCellPathByOtherCar(workingOrder, curPath, isLoadMove, car)) {
            // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
            if (canUseCellPathByCellStatus(curPath, isLoadMove, car)) {
                // 지시 + 완료 초기화 송신
                car.updateReserveCmdIndex(cmdList.indexOf(nowCarLocationFromCmd));
                int[] command = curCmd.getMoveAndClearCommand(nextWorkId(), floor);
                sendPlcCommand(car.getShuttleCarId(), command);
                updateTbEcsRackOrder(workingOrder, workId);
                updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                return true;
            } else {
                log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
                return false;
            }
        } else {
            log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
            return false;
        }
        // TODO 이동 가능한 셀까지 이동
        // Shuttle4WayWriteMap prefixPath = canMoveCellPath(curPath, false);
        // if (prefixPath == null || prefixPath.getPath().isEmpty()) {
        //     log.info(this.floor + "층 " + car.getShuttleCarId() + " canMoveCellPath false prefix 경로 이동 불가. 대기중..");
        //     return false;
        // }
        // car.addPrePixCmd(prefixPath);
    }

    /**
     * 경로 주행 가능한지 확인
     * 다른 셔틀 카가 경로를 막고 있진 않는지.
     */
    private boolean canUseCellPathByOtherCar(TbEcsRackOrder order, List<Cell> path, boolean isLoadMove, Tspg4WayShuttleCar car) {
        log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPathByOtherCar");
        boolean canUseCellPath = false;
        Set<String> pathSet = path.stream().map(c -> c.getLocX() + "|" + c.getLocY()).collect(Collectors.toSet());
        List<TbEqRackMst> cellList = selectCellStatus();
        List<TbEqRackMst> matched = cellList.stream().filter(a -> pathSet.contains(a.getBay() + "|" + a.getRow())).collect(Collectors.toList());
        if (matched == null){
            return canUseCellPath;
        }
        // 다른 카의 현재 위치 상태 확인
        List<TbEqCarMst> otherCarMstList = selectOtherCarStatus(car.getShuttleCarId());
        // 멀티 카인 경우
        if (otherCarMstList != null && !otherCarMstList.isEmpty()) {
            log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPathByOtherCar 멀티카");
            for (TbEqCarMst otherCarMst : otherCarMstList) {
                for (TbEqRackMst cell : matched) {
                    // todo : 정지상태가 아닌경우 구현 필요
                    boolean isOtherCarBlockingPath = cell.getRackId().equals(otherCarMst.getRackId());
                    // 경로를 막고 있는 다른 카
                    if (isOtherCarBlockingPath) {
                        var waitOrder = selectTbEcsRackWaitOrder();
                        if(waitOrder == null || waitOrder.isEmpty()){
                            boolean isOtherSendCreate = otherCarMoveCreate(otherCarMst, car);
                            if(isOtherSendCreate){
                                log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPathByOtherCar  원 지시 대기 요청");
                                // 원 지시 대기 요청
                                order.setWaitYn(true);
                                iQueryManager.update(order);
                            }
                        }else{
                            log.info(this.floor + "층 " + car.getShuttleCarId() + "canUseCellPathByOtherCar 다른 waitYn 오더있음");
                        }
                        return false;
                    } else {
                        canUseCellPath = true;
                    }
                }
            }
        } else {
            log.info(this.floor + "층 " + car.getShuttleCarId() + " 멀티카아님");
            canUseCellPath = true;
        }
        log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPathByOtherCar result : " + canUseCellPath);
        return canUseCellPath;
    }



    private boolean otherCarMoveCreate(TbEqCarMst otherCarMst, Tspg4WayShuttleCar car){
        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate");
        boolean isOtherCarMove = false;
        boolean isStatusReady = otherCarMst.getStatus() == EcsDBConsts.EqCarStatus.READY.getValue() || otherCarMst.getStatus() == EcsDBConsts.EqCarStatus.COMPLETE.getValue();
        boolean isNotCharging = otherCarMst.getBatteryStatus() == EcsDBConsts.EqCarBatteryStatus.CAN_MOVE.getValue();
        TbEqCarMst carMst = selectCarStatus(car.getShuttleCarId());
        var carEqId = carMst.getEqId();
        var carPlc = this.tspgShuttlePlcRegistry.getEquipment(carEqId);
        var carOrder = selectTbEcsRackWorkingOrder(carEqId);
        var otherCarEqId = otherCarMst.getEqId();
        var otherCarPlc = this.tspgShuttlePlcRegistry.getEquipment(otherCarEqId);
        var otherCar = otherCarPlc.getCar();
        var otherCarOrder = selectTbEcsRackWorkingOrder(otherCarEqId);
        var pathList = otherCar.getShuttlePathCmdList();
        Shuttle4WayWriteMap otherCarNowPath = new Shuttle4WayWriteMap();
        Shuttle4WayWriteMap otherCarNextPath = new Shuttle4WayWriteMap();
        if (pathList != null  ) {
            otherCarNowPath = pathList.size() > otherCar.getReserveCmdCurrentIndex() ? pathList.get(otherCar.getReserveCmdCurrentIndex()) : null;
            otherCarNextPath = pathList.size() > otherCar.getReserveCmdCurrentIndex()+1 ? pathList.get(otherCar.getReserveCmdCurrentIndex() + 1) : null ;
        }
        // 정지 상태, 충전 상태 아님, 회피 명령을 받은 상태 아님
        if (isStatusReady && isNotCharging && !otherCarMst.isReqMoveHomeYn()) {
            boolean otherCarHasOrder = otherCarOrder != null;
            boolean isLoadMove = false;
            // 다른 카 지시 수행중이라면,
            if(otherCarHasOrder ){
                // 다른카 화물 로드셀에 도착한 경우, 다른카 로드 기다려줌
                boolean isArrivedLoadCell = otherCarOrder.getFromRow() == otherCarMst.getRow() && otherCarOrder.getFromBay() == otherCarMst.getBay();
                boolean isArrivedUnLoadCell = otherCarOrder.getToRow() == otherCarMst.getRow() && otherCarOrder.getToBay() == otherCarMst.getBay();
                boolean isRowBlock = carMst.getRow() == otherCarMst.getRow();
                boolean isBayBlock = carMst.getBay() == otherCarMst.getBay();
                if(isArrivedLoadCell || isArrivedUnLoadCell) {
                    log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 다른카 출발지or목적지 도착. 작업 대기" + otherCarMst.getEqId());
                    return false;
                }
                // 다른카 다음 경로가 없음(현재 경로가 마지막)
                if(otherCarNextPath == null){
                    // 다른 카의 현재 경로 이동시 병목 해소
                    if(isNotOverMove(carMst, otherCarMst, otherCarNowPath)){
                        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 다른카 현재 경로 이동시 병목 해소이므로 대기" + otherCarMst.getEqId());
                        return false;
                    }
                    // 현재 카 경로 재탐색 및 재지시
                    if(reFindPath(otherCar, carPlc, carOrder, carMst)){
                        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 현재 카 경로 재탐색 정상, 재지시 :" + otherCarMst.getEqId());
                        reOrderManager(carMst, carPlc, carOrder);
                        return false;
                    }else{
                        // 재탐색에 실패한 경우 다른카 다른곳으로 이동지시 (경로 선상과 반대 축 이동)
                        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 현재 카  경로 재탐색 실패!!  :" + otherCarMst.getEqId());
                        avoidPathMove(otherCar, car, carOrder, carMst, isRowBlock, isLoadMove);
                        return false;
                    }
                }
                if(otherCarNowPath != null && otherCarNextPath != null){
                    // 다음 경로가 원래카를 넘어가지 않은 경우  // && !isRowToClose && !isBayToClose){ // 가까워지지 않는 경우는 일단 제외
                    if(isNotOverMove(carMst, otherCarMst, otherCarNowPath, otherCarNextPath, isRowBlock, isBayBlock)) {
                        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 다른카 다음 경로 이동시 병목 해소이므로 대기" + otherCarMst.getEqId());
                        return false;
                    }
                }
                // 경로 재 탐색 전 정지상태가 유지된 상황인지 확인필요
                var otherCarNowMst = selectCarStatus(otherCarEqId);
                boolean isStillReady = otherCarNowMst.getStatus() == EcsDBConsts.EqCarStatus.READY.getValue() || otherCarNowMst.getStatus() == EcsDBConsts.EqCarStatus.COMPLETE.getValue();
                if(!isStillReady){
                    return false;
                }
                // 다른 카 경로 재탐색 및 재지시
                if(reFindPath(car, otherCarPlc,otherCarOrder, otherCarMst)){
                    log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate  경로 재탐색 정상, 재지시 :" + otherCarMst.getEqId());
                    reOrderManager(otherCarMst, otherCarPlc, otherCarOrder);
                }else{
                    // 재탐색에 실패한 경우 다른카 다른곳으로 이동지시 (경로 선상과 반대 축 이동)
                    log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate  경로 재탐색 실패!!  :" + otherCarMst.getEqId());
                    avoidPathMove(car, otherCar, otherCarOrder, otherCarMst, isRowBlock, isLoadMove);
                }
                return true;
            }
            // 작업이 없는 경우
            else{
                // 대기중인 입고지시가 있는 경우, 버퍼 포지션으로 이동
                var inboundOrderList = selectTbEcsRackInboundOrder();
                if(inboundOrderList != null &&  !inboundOrderList.isEmpty()){
                    log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 버퍼 포지션으로 이동 Send :" + otherCarMst.getEqId() + "result: true");
                    otherCarMst.setReqMoveHomeYn(true);
                    carPathCellReadyUpdate(otherCarPlc.getCar());
                    iQueryManager.update(otherCarMst);
                    return true;
                }
                // 입고지시가 없는 경우, 반대 포지션으로 이동, //
                log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 반대 포지션으로 이동 Send :" + otherCarMst.getEqId() + "result: true");
                List<TbEqRackMst> cellMstList = selectCellStatus();
                int maxX = cellMstList.stream().mapToInt(TbEqRackMst::getBay).max().orElse(0);
                int maxY = cellMstList.stream().mapToInt(TbEqRackMst::getRow).max().orElse(0);
                reloadMap(true, otherCarMst.getEqId());
                var cell = this.pathService.findReachableTarget(otherCarMst.getBay(), otherCarMst.getRow(), 1, maxX, 1, maxY, 3);
                carPathCellReadyUpdate(otherCarPlc.getCar());
                createOtherCarMoveOrder(cell.getLocY(), cell.getLocX(), otherCarMst.getEqId());
                return true;
            }
        }
        return isOtherCarMove;
    }

    // 다른카의 다음 경로가 원래카에 막혀있진 않은지
    private boolean isNotOverMove(TbEqCarMst carMst, TbEqCarMst otherCarMst, Shuttle4WayWriteMap otherCarNowPath, Shuttle4WayWriteMap otherCarNextPath, boolean isRowBlock, boolean isBayBlock){
        // 방향 전환으로 인한 정지 상태인 경우
        boolean isNowRowMove = otherCarNowPath.getFromCell().getLocX() == otherCarNowPath.getToCell().getLocX();
        boolean isNowBayMove = otherCarNowPath.getFromCell().getLocY() == otherCarNowPath.getToCell().getLocY();
        boolean isNextRowMove = otherCarNextPath.getFromCell().getLocX() == otherCarNextPath.getToCell().getLocX();
        boolean isNextBayMove = otherCarNextPath.getFromCell().getLocY() == otherCarNextPath.getToCell().getLocY();
        boolean isRowToClose = isRowBlock && Math.abs(carMst.getRow() - otherCarNextPath.getToCell().getLocY()) < Math.abs(carMst.getRow() - otherCarMst.getRow());
        boolean isBayToClose = isBayBlock && Math.abs(carMst.getBay() - otherCarNextPath.getToCell().getLocX()) < Math.abs(carMst.getBay() - otherCarMst.getBay());
        boolean isDeadLock = isBayBlock ? Math.abs(carMst.getRow() - otherCarMst.getRow()) == 1 : Math.abs(carMst.getBay() - otherCarMst.getBay()) == 1;

        boolean isNotOverMove = false;
        if (isNowRowMove && isNextBayMove) {
            int myRow = carMst.getRow();
            int otherRow = otherCarMst.getRow();
            int nextRow = otherCarNextPath.getToCell().getLocY();
            if (otherRow < myRow)
                isNotOverMove = nextRow < myRow;
            else if (otherRow > myRow)
                isNotOverMove = nextRow > myRow;
        } else if (isNowBayMove && isNextRowMove) {
            int myBay = carMst.getBay();
            int otherBay = otherCarMst.getBay();
            int nextBay = otherCarNextPath.getToCell().getLocX();
            if (otherBay < myBay)
                isNotOverMove = nextBay < myBay;
            else if (otherBay > myBay)
                isNotOverMove = nextBay > myBay;
        }
        return isNotOverMove;
    }
    // 다른카의 현재 주행 경로가 원래카에 막혀있는게 아닌지
    private boolean isNotOverMove(TbEqCarMst carMst, TbEqCarMst otherCarMst, Shuttle4WayWriteMap otherCarNowPath){
        // 다른카의 현재 주행 방향
        boolean isNowRowMove = otherCarNowPath.getFromCell().getLocX() == otherCarNowPath.getToCell().getLocX();
        boolean isNowBayMove = otherCarNowPath.getFromCell().getLocY() == otherCarNowPath.getToCell().getLocY();

        boolean isNotOverMove = false;
        if (isNowRowMove) {
            int myBay = carMst.getBay();
            int otherBay = otherCarMst.getBay();
            isNotOverMove = myBay != otherBay;
        } else if (isNowBayMove) {
            int myRow = carMst.getRow();
            int otherRow = otherCarMst.getRow();
            isNotOverMove = myRow != otherRow;
        }
        return isNotOverMove;
    }
    // 다른 카 경로 재탐색 및 재지시
    private boolean reFindPath(Tspg4WayShuttleCar car, Tspg4WayShuttlePlc otherCarPlc, TbEcsRackOrder otherCarOrder, TbEqCarMst otherCarMst){
        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate  경로 재탐색 시작 :" + otherCarMst.getEqId());
        reloadMap( true, otherCarMst.getEqId());
        boolean isSuccessFindPath = false;

        // 재탐색 (화물 있는 경우 언로드 경로, 없는 경우 로드 경로)
        if(otherCarMst.isCargoYn())
            isSuccessFindPath = createUnloadPath(true, otherCarPlc, otherCarOrder);
        else
            isSuccessFindPath = canLoadFindPath(otherCarPlc, otherCarOrder, otherCarMst);
        return isSuccessFindPath;
    }
    // 다른카 다른곳으로 이동지시 (경로 선상과 반대 축 이동)
    private void avoidPathMove(Tspg4WayShuttleCar car, Tspg4WayShuttleCar otherCar, TbEcsRackOrder otherCarOrder, TbEqCarMst otherCarMst, boolean isRowBlock, boolean isLoadMove){
        List<Shuttle4WayWriteMap> pathCmdList = new ArrayList<>();
        int otherX = otherCarMst.getBay();
        int otherY = otherCarMst.getRow();
        int step = 1;
        List<Cell> pathCell = new ArrayList<>();
        while (pathCmdList.isEmpty()) {
            int distance = (step % 2 == 1) ? (step + 1) / 2 : -(step / 2);
            pathCell = isRowBlock ? this.pathService.findPath(!isLoadMove, otherX, otherY,otherX , otherY + distance):
                    this.pathService.findPath(!isLoadMove, otherX, otherY, otherX+ distance, otherY );
            pathCmdList = pathService.buildMoveCommands(pathCell);
            step++;
            if (step > 100) {
                break;
            }
        }
        // todo : 기존 경로 처리
        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate  해당카 다른곳으로 이동지시 (경로 선상과 반대 축 이동)  :" + otherCarMst.getEqId());
        try {
            log.info(this.floor + "층 " + "otherCarMoveCreate move path @@@@");
            log.info(mapper.writeValueAsString(pathCell));
            log.info(this.floor + "층 " + "otherCarMoveCreate pathCmdList @@@@");
            log.info(mapper.writeValueAsString(pathCmdList));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        otherCar.initPathCmdList();
        otherCar.setShuttlePathCmdList(pathCmdList);
        var cmd = pathCmdList.get(0);
        int[] command = cmd.getMoveAndClearCommand(otherCarOrder.getPlcCmdId()+1, floor);
        sendPlcCommand(otherCar.getShuttleCarId(), command);
        updateTbEcsRackOrder(otherCarOrder, otherCarOrder.getPlcCmdId()+1);
    }

//    /**
//     * 다른 셔틀카 이동지시 생성
//     * - 화물을 들고있는경우, 언로드 경로 재지시
//     * - 대기 입고지시가 있는경우, 버퍼 셀 이동지시
//     * - 대기 입고지시가 없는경우, 레이아웃 상 반대 쪽셀로 이동지시
//     * TODO : 3대 경우 수정
//     */
//    private boolean otherCarMoveCreate_ori(TbEqCarMst otherCarMst, Tspg4WayShuttleCar car){
//        log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate");
//        boolean isOtherCarMove = false;
//        boolean isStatusReady = otherCarMst.getStatus() == EcsDBConsts.EqCarStatus.READY.getValue();
//        boolean isNotCharging = otherCarMst.getBatteryStatus() == EcsDBConsts.EqCarBatteryStatus.CAN_MOVE.getValue();
//        // 대기 상태이며, 충전중이 아닌카가 있는 경우 경로 주행을 위해 이동지시 생성 신호 업데이트
//        if (isStatusReady && isNotCharging && !otherCarMst.isReqMoveHomeYn()) {
//            // 화물을 들고 있는 경우
//            if(otherCarMst.isCargoYn()){
//                log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 화물 있는 경우 경로 초기화 및 재지시 :" + otherCarMst.getId());
//                String carId = otherCarMst.getId();
//                var otherCarPlc = this.tspgShuttlePlcRegistry.getEquipment(carId);
//                var otherCarOrder = selectTbEcsRackWorkingOrder(carId);
//                reOrderManager(otherCarMst, otherCarPlc, otherCarOrder);
//                return true;
//            }
//            // 대기중인 입고지시가 있으면 버퍼 포지션으로 이동
//            var inboundOrderList = selectTbEcsRackInboundOrder();
//            if(inboundOrderList != null &&  !inboundOrderList.isEmpty()){
//                log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 버퍼 포지션으로 이동 Send :" + otherCarMst.getId() + "result: true");
//                otherCarMst.setReqMoveHomeYn(true);
//                var otherCarPlc = this.tspgShuttlePlcRegistry.getEquipment(otherCarMst.getId());
//                carPathCellReadyUpdate(otherCarPlc.getCar());
//                iQueryManager.update(otherCarMst);
//                return true;
//            }
//            else{
//                // 반대 포지션으로 이동
//                log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate 반대 포지션으로 이동 Send :" + otherCarMst.getId() + "result: true");
//                List<TbEqRackMst> cellMstList = selectCellStatus();
//                int maxX = cellMstList.stream().mapToInt(TbEqRackMst::getBay).max().orElse(0);
//                int maxY = cellMstList.stream().mapToInt(TbEqRackMst::getRow).max().orElse(0);
//                reloadMap(true, otherCarMst.getId());
//                var cell = this.pathService.findReachableTarget(otherCarMst.getBay(), otherCarMst.getRow(), 1, maxX, 1, maxY, 3);
//                var otherCarPlc = this.tspgShuttlePlcRegistry.getEquipment(otherCarMst.getId());
//                carPathCellReadyUpdate(otherCarPlc.getCar());
//                createOtherCarMoveOrder(cell.getLocY(), cell.getLocX(), otherCarMst.getId());
//                return true;
//            }
//        }
//        return isOtherCarMove;
//    }

    /**
     * 주변으로 이동
     * 횡행 주행 +1 or -1 로 이동
     */
    private boolean nearCellMove(TbEqCarMst otherCarMst, Tspg4WayShuttleCar car){
        TbEqCarMst carMst = selectCarStatus(car.getShuttleCarId());
        if(carMst != null){
            boolean sameX = carMst.getBay() == otherCarMst.getBay();
            boolean sameY = carMst.getRow() == otherCarMst.getRow();
            if(sameX){
                // bay 이동
                log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate other Bay Cell Send :" + otherCarMst.getId());
                var cellMstLsist = selectCellStatus();
                var upRowCell = cellMstLsist.stream().filter(mst -> mst.getRow() == otherCarMst.getRow() && mst.getBay() == otherCarMst.getBay()+1).findAny().orElse(null);
                var downRowCell = cellMstLsist.stream().filter(mst -> mst.getRow() == otherCarMst.getRow() && mst.getBay()  == otherCarMst.getBay()-1).findAny().orElse(null);
                if (downRowCell != null) {
                    createOtherCarMoveOrder(otherCarMst.getRow(), otherCarMst.getBay()-1, otherCarMst.getId());
                    return true;
                }
                if (upRowCell != null) {
                    createOtherCarMoveOrder(otherCarMst.getRow(), otherCarMst.getBay()+1, otherCarMst.getId());
                    return true;
                }
            }
            else if(sameY){
                // Row 이동
                log.info(this.floor + "층 " + car.getShuttleCarId() + " otherCarMoveCreate other Row Cell Send :" + otherCarMst.getId());
                var cellMstLsist = selectCellStatus();
                var upRowCell = cellMstLsist.stream().filter(mst -> mst.getRow() == otherCarMst.getRow()+1 && mst.getBay() == otherCarMst.getBay()).findAny().orElse(null);

                var downRowCell = cellMstLsist.stream().filter(mst -> mst.getRow() == otherCarMst.getRow()-1 && mst.getBay() == otherCarMst.getBay()).findAny().orElse(null);
                if (downRowCell != null) {
                    createOtherCarMoveOrder(otherCarMst.getRow()-1, otherCarMst.getBay(), otherCarMst.getId());
                    return true;
                }
                if (upRowCell != null) {
                    createOtherCarMoveOrder(otherCarMst.getRow()+1, otherCarMst.getBay(), otherCarMst.getId());
                    return true;
                }
            }
        }
        return false;
    }

    private void createOtherCarMoveOrder(int toRow, int toBay, String otherCarId){
        log.info(this.floor + "층 " + otherCarId + " createOtherCarMoveOrder " + toBay +","+toRow);
        var moveOrder = TbEcsRackOrder.otherCarMoveOrder(toRow, toBay, this.floor, this.rackEqId, otherCarId);
        iQueryManager.insert(moveOrder);
    }

    /**
     * 경로 주행 가능한지 확인
     * 셀의 화물 유무와 예약 여부를 확인
     */
    private boolean canUseCellPathByCellStatus(List<Cell> path, boolean isLoadMove, Tspg4WayShuttleCar car) {
        log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPathByCellStatus");
        boolean canUseCellPath = false;
        Set<String> pathSet = path.stream().map(c -> c.getLocX() + "|" + c.getLocY()).collect(Collectors.toSet());
        List<TbEqRackMst> cellList = selectCellStatus();
        List<TbEqRackMst> matched = cellList.stream().filter(a -> pathSet.contains(a.getBay() + "|" + a.getRow())).collect(Collectors.toList());
        if (matched == null)
            return canUseCellPath;
        // 로드인경우
        if (isLoadMove){log.info(this.floor + "층 canUseCellPathByCellStatus isLoadMove");
            canUseCellPath = !matched.isEmpty() && matched.stream().allMatch(cell -> cell.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue()
                    || cell.getStatus() == EcsDBConsts.EqRackStatus.CARGO.getValue());
        }
        // 언로드인경우
        else {
            log.info(this.floor + "층 canUseCellPathByCellStatus isLoadMove else");
            // 출고포트로 언로드인지 확인
            List<TbEqCvMst> cvMstList = selectRackInCvStatus();
            TbEqCvMst rackInCvMst = new TbEqCvMst();
            boolean isOutboundPort = false;
            int i = 0;
            for(var p : path){
                if(i==0){
                    i+=1;
                    continue;
                }
                rackInCvMst = cvMstList.stream().filter(cvMst -> cvMst.getRackRow() == p.getLocY()
                        && cvMst.getRackBay() == p.getLocX() && cvMst.getLevel() == this.floor ).findAny().orElse(null);
                if(rackInCvMst != null){
                    isOutboundPort = true;
                    break;
                }
            }
            // 출고포트로 언로드인경우, 해당 랙단 컨베이어에 화물이 없어야함.
            if(isOutboundPort){
                log.info(this.floor + "층 canUseCellPathByCellStatus isOutboundPort");
                canUseCellPath = !matched.isEmpty() && matched.stream().allMatch(cell -> cell.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue()
                        && !cell.isCargoYn()) && !rackInCvMst.isCargoYn();
                if(canUseCellPath) { // && isOutboundPort
                    // TODO 출고포트에 화물이 있는경우. 상위 알림 또는 경로 초기화 등.
                }
            }else{
                log.info(this.floor + "층 canUseCellPathByCellStatus isOutboundPort else");
                canUseCellPath = !matched.isEmpty() && matched.stream().allMatch(cell -> (cell.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue() && !cell.isCargoYn() ));
            }
        }
        log.info(this.floor + "층 " + car.getShuttleCarId() + " canUseCellPathByCellStatus result: " + canUseCellPath);
        return canUseCellPath;
    }


    /**
     * 재탐색 지시 관리
     */
    private void reOrderManager(TbEqCarMst carMst, Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor + "층 " + carMst.getId() + " ReOrderManager");

        carPathCellReadyUpdate(carPlc.getCar());
        carPathReset(carMst.getId(), true);
        switch (EcsDBConsts.EcsRackOrderCmdStatus.find(workingOrder.getCmdStatus())) {
            case LOAD_MOVE -> {
                switch (EcsDBConsts.OrderType.find(workingOrder.getOrderType())) {
                    case INBOUND -> inboundLoadMoveOrder(carPlc, workingOrder, carMst);
                    case OUTBOUND -> outboundLoadMoveOrder(carPlc, workingOrder, carMst);
                }
            }
            case LOAD,UNLOAD_MOVE -> unloadMoveOrder(carPlc, workingOrder, true);
            case CHARGE_MOVE -> sendChargeOrder(workingOrder);
            case MOVE_HOME -> sendMoveHomeOrder(workingOrder);
        }
    }


    /**
     * 경로가 막혀있는경우, 막혀있지 않은 부분까지 경로 생성
     */
    private Shuttle4WayWriteMap canMoveCellPath(List<Cell> path, boolean isLoad) {
        log.info(this.floor + "층 이동가능한곳 까지만 canMoveCellPath 함수 사용 안함(테스트중)");
        return null;
//        Shuttle4WayWriteMap prefixMap = new Shuttle4WayWriteMap();
//        List<Cell> safePathList = new ArrayList<>();
//        List<TbEqRackMst> cellList = selectCellStatus();
//        Map<String, TbEqRackMst> cellMap = cellList.stream().collect(Collectors.toMap(c -> c.getBay() + "|" + c.getRow(), c -> c));
//        List<TbEqRackMst> matched = path.stream().map(p -> cellMap.get(p.getLocX() + "|" + p.getLocY())).filter(Objects::nonNull).collect(Collectors.toList());
//        for (var rackMst : matched) {
//            if (isLoad) {
//                if (rackMst.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue() || rackMst.getStatus() == EcsDBConsts.EqRackStatus.CARGO.getValue()) {
//                    var cell = path.stream().filter(c -> c.getLocX() == rackMst.getBay() && c.getLocY() == rackMst.getRow()).findAny().orElse(null);
//                    if (cell != null){
//                        log.info(this.floor + "층 canMoveCellPath isLoad path add " + cell.getLocX() +"," + cell.getLocY());
//                        safePathList.add(cell);
//                    }
//                    else
//                        break;
//                }
//            } else {
//                if (rackMst.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue() && !rackMst.isCargoYn()) {
//                    var cell = path.stream().filter(c -> c.getLocX() == rackMst.getBay() && c.getLocY() == rackMst.getRow()).findAny().orElse(null);
//                    if (cell != null){
//                        log.info(this.floor + "층 canMoveCellPath unLoad path add " + cell.getLocX() +"," + cell.getLocY());
//                        safePathList.add(cell);
//                    }
//                    else
//                        break;
//                }
//            }
//        }
//
//        // TODO: 추후 안전거리 DB 연동
//        int safeDistance = 1;
//        if(safePathList.size() > safeDistance){
//            prefixMap.setPath(safePathList);
//            prefixMap.setFromCell(safePathList.get(0));
//            prefixMap.setToCell(safePathList.get(safePathList.size() - safeDistance));
//        }
//        return prefixMap;
    }


    /**
     * 셔틀카 PLC 명령 송신
     */
    private void sendPlcCommand(String shuttleCar, int[] command) {
        log.info(this.floor + "층 " + "[PLC SEND] shuttleCar sendPlcCommand : " + shuttleCar);
        try {
            log.info(this.mapper.writeValueAsString(command));
        } catch (Exception e) {
            log.error(e.toString());
        }
        tspgShuttlePlcWriteService.sendCommandShuttle(shuttleCar, MelsecConsts.DeviceCode.R, Shuttle4WayWriteConsts.ShuttleWriteAddress.WORK_ID.getAddress(), command);
    }

    /**
     * 완료된 지시 관리
     */
    private void completeOrderManager(TbEqCarMst carMst, Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor + "층 " + carMst.getId() + " CompleteOrderManager");
        switch (EcsDBConsts.EcsRackOrderCmdStatus.find(workingOrder.getCmdStatus())) {
            case LOAD_MOVE -> loadOrder(carPlc, workingOrder, carMst);
            case LOAD -> unloadMoveOrder(carPlc, workingOrder, false);
            case UNLOAD_MOVE -> unloadOrder(carPlc, workingOrder, carMst);
            case UNLOAD -> unloadCompleteOrder(carPlc, workingOrder);
            case CHARGE_MOVE -> chargeOrder(carPlc, workingOrder);
            case CHARGE -> chargeComplete(carPlc, workingOrder);
            case MOVE_HOME -> moveHomeComplete(carPlc, workingOrder);
            case MOVE_CAR_FROM_RACK_CV -> moveCarLift(carPlc, workingOrder);
            case MOVE_CAR_LIFT_MOVE -> moveLift(carMst, carPlc, workingOrder);
            case MOVE_CAR_LIFT_CV -> moveCarToRackCv(carPlc, workingOrder);
            case MOVE_CAR_TO_RACK_CV -> moveCarComplete(carMst, carPlc, workingOrder);
        }
    }



    /**
     * 로드지시 전송 + 완료비트 초기화
     */
    private void loadOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder, TbEqCarMst carMst) {
        log.info(this.floor + "층 " + carMst.getId() + " loadOrder");
        // 아직 목적지에 도착하지 못한 경우
        boolean isArrived = carMst.getBay() == workingOrder.getFromBay() && carMst.getRow() == workingOrder.getFromRow();
        if (!isArrived) {
            log.info(this.floor+"층 "+carMst.getId()+" loadOrder !isArrived");

            switch (EcsDBConsts.OrderType.find(workingOrder.getOrderType())) {
                case INBOUND -> inboundLoadMoveOrder(carPlc, workingOrder, carMst);
                case OUTBOUND -> outboundLoadMoveOrder(carPlc, workingOrder, carMst);
            }
        } else {
            // 해당 오더키로 입고 이송 지시가 있는지
            // (있다면, 화물이 없어도 랙단으로 load 이송을 하기 때문에, 화물이 있는지 확인하고 load 명령을 내려야함)
            TbEcsRouteOrder routeOrder = selectTbEcsRouteOrder(workingOrder.getOrderKey());
            boolean isRackInRouteOrder = routeOrder != null;
            if (isRackInRouteOrder) {
                log.info(this.floor+"층 "+carMst.getId()+" loadOrder isRackInRouteOrder");
                boolean isCargoArrived = routeOrder.getCmdStatus() == EcsDBConsts.EcsRouteOrderCmdStatus.RACK_CV_READY.getValue();
                // 랙단 컨베이어에 화물이 도착했는지
                if (isCargoArrived) {
                    int[] command = carPlc.getWriteMap().getLoadAndClearCommand(nextWorkId());
                    sendPlcCommand(carPlc.getId(), command);
                    updateTbEcsRackOrder(carPlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.LOAD, this.workId);
                }
            }
            // 입고대 입고지시의 로드가 아닌 경우
            else {
                log.info(this.floor+"층 "+carMst.getId()+" loadOrder else");

                int[] command = carPlc.getWriteMap().getLoadAndClearCommand(nextWorkId());
                sendPlcCommand(carPlc.getId(), command);
                updateTbEcsRackOrder(carPlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.LOAD, this.workId);
            }
        }

    }

    /**
     * 언로드 이송지시 전송 + 완료비트 초기화
     */
    private void unloadMoveOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder, boolean isReOrder) {
        log.info(this.floor + "층 " + carPlc.getId() + " unloadMoveOrder");
        reloadMap(isReOrder, carPlc.getId());
        createUnloadPath(isReOrder, carPlc, workingOrder);
        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        if (cmd == null)
            return;
        List<Cell> curPath = cmd.getPath();

        if (canUseCellPathByOtherCar(workingOrder, curPath, false, carPlc.getCar())) {
            // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
            if (canUseCellPathByCellStatus(curPath, false, carPlc.getCar())) {
                // 경로 명령 송신
                unloadMoveOrderPathSend(cmd, carPlc, workingOrder);
                return;
            } else {
                log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");

            }
        } else {
            log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
        }
        carPathReset(carPlc.getId(), false);

        // // 이동 가능한 셀까지 이동
        // Shuttle4WayWriteMap prefixPath = canMoveCellPath(curPath, false);
        // if (prefixPath == null || prefixPath.getPath().isEmpty()) {
        //     log.info(this.floor + "층 " + carPlc.getId() + " canMoveCellPath false prefix 경로 이동 불가. 대기중..");
        //     return;
        // }
        // carPlc.getCar().addPrePixCmd(prefixPath);
        // unloadMoveOrderPathSend(prefixPath, carPlc, workingOrder);
    }

    private void unloadMoveOrderPathSend(Shuttle4WayWriteMap cmd, Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor + "층 " + carPlc.getId() + " unloadMoveOrderPathSend");
        int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
        sendPlcCommand(carPlc.getId(), command);
        updateTbEcsRackOrder(workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD_MOVE, workId);
        updateTbEqRackMstStatus(cmd.getPath(), EcsDBConsts.EqRackStatus.MOVE_RESERVE);
        boolean isInboundTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue();
        if (isInboundTypeOrder) {
            // 입고 타입 언로드 할 경우, 목적지 셀 화물 예약으로 변경
            updateTbEqRackMstStatus(workingOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO_RESERVE);
        }
    }

    /**
     * 경로 탐색 전 현재 맵(랙 정보) 재조회
     *
     * 화물이 있거나, 화물이 오는중이거나 (예약상태) 충전 중인 카가 있으면 우회
     * 다른카의 병목을 위한 이동인 경우 예약셀 및 다른 카 위치 우회
     */
    private void reloadMap(boolean isOtherCarMove, String carId) {
        log.info(this.floor + "층 " + "reloadMap");
        List<TbEqRackMst> cellList = selectCellStatus();
        List<TbEqCarMst> carMstList = selectCarStatus();
        TbEqCarMst chargingCar = carMstList.stream().filter(carMst -> carMst.getBatteryStatus() != EcsDBConsts.EqCarBatteryStatus.CAN_MOVE.getValue()).findFirst().orElse(null);
        boolean isOtherCarCharging = chargingCar != null;
        for(var cell : cellList){
            if(cell.getType() == EcsDBConsts.RackType.BAN_CELL.getValue()){
                pathService.updateUseCell(cell.getBay(), cell.getRow(), false);
                continue;
            }
            // 충전셀에 충전중인 카가 있으면 해당 셀 우회(사용 안함으로 변경)
            if (isOtherCarCharging){
                if(cell.getType() == EcsDBConsts.RackType.CHARGE_PORT.getValue()){
                    log.info(this.floor + "층 reloadMap updateUseCell false" +cell.getBay() + ", " + cell.getRow());
                    pathService.updateUseCell(cell.getBay(), cell.getRow(), false);
                    continue;
                }
            }
            boolean hasCargo = false;
            hasCargo = cell.getStatus() == EcsDBConsts.EqRackStatus.CARGO.getValue()
                    || cell.getStatus() == EcsDBConsts.EqRackStatus.CARGO_RESERVE.getValue()
                    || cell.isCargoYn();
            pathService.updateUseCell(cell.getBay(), cell.getRow(), cell.isUseYn());
            pathService.updateCargoCell(cell.getBay(), cell.getRow(), hasCargo);
        }
        // 다른카 병목으로 인한 이송지시인 경우, 다른 카의 위치 셀 우회(사용 안함으로 변경)
        if (isOtherCarMove) {
            // boolean isReservedCell = cell.getStatus() == EcsDBConsts.EqRackStatus.MOVE_RESERVE.getValue();
            var otherCarMstList = selectOtherCarStatus(carId);
            if(otherCarMstList != null && !otherCarMstList.isEmpty()){
                var otherCarMst = otherCarMstList.get(0);
                log.info(this.floor + "층 reloadMap updateUseCell false" +otherCarMst.getBay() + ", " + otherCarMst.getRow());
                pathService.updateUseCell(otherCarMst.getBay(), otherCarMst.getRow(), false);
            }
        }
        log.info(this.floor + "층 " + "reloadMap comp");
    }


    /**
     * 로드할 경로 목적지에 이미 도착했는지
     */
    private boolean isAlreadyLoadTarget(TbEcsRackOrder workingOrder, TbEqCarMst carMst) {
        int fromRow = Integer.parseInt(carMst.getRackId().substring(1, 3));
        int fromBay =  Integer.parseInt(carMst.getRackId().substring(3, 5));
        int toRow = Integer.parseInt(workingOrder.getFromLocId().substring(1, 3));
        int toBay =  Integer.parseInt(workingOrder.getFromLocId().substring(3, 5));
        return pathService.isAlreadyAtTarget(fromRow, fromBay, toRow, toBay);
    }


    /**
     * 경로 생성
     * - 로드/언로드 구분
     * - 충전일 경우 로드경로 사용
     * - 셔틀카 층간 이송 출발인 경우 로드경로 사용
     */
    private boolean canLoadFindPath(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder, TbEqCarMst carMst) {
        boolean canFindPath = false;
        log.info(this.floor + "층 " + "canLoadFindPath");
        int fromRow = Integer.parseInt(carMst.getRackId().substring(1, 3));
        int fromBay =  Integer.parseInt(carMst.getRackId().substring(3, 5));
        int toRow = Integer.parseInt(workingOrder.getFromLocId().substring(1, 3));
        int toBay =  Integer.parseInt(workingOrder.getFromLocId().substring(3, 5));
        log.info(this.floor + "층 " + "PATH from : " + fromRow + ", " + fromBay + ", to" + toRow + ", " + toBay);
        List<Cell> movePath = pathService.findPath(false, fromBay, fromRow, toBay, toRow);
        List<Shuttle4WayWriteMap> pathCmdList = pathService.buildMoveCommands(movePath);
        if (pathCmdList == null || pathCmdList.size() == 0) {
            log.warn(this.floor + "층 @@@@@ find path 실패 @@@@@ ");
            canFindPath = false;
        }
        carPlc.getCar().setShuttlePathCmdList(pathCmdList);
        carPlc.getCar().setReserveCmdSize(pathCmdList.size());
        try {
            log.info(this.floor + "층 " + "LOAD move path @@@@");
            log.info(mapper.writeValueAsString(movePath));
            log.info(this.floor + "층 " + "LOAD pathCmdList @@@@");
            log.info(mapper.writeValueAsString(pathCmdList));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info(this.floor + "층 " + "canLoadFindPath comp");
        canFindPath = true;
        return canFindPath;
    }


    private boolean createUnloadPath(boolean isReorder, Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        boolean isSuccess = false;
        log.info(this.floor + "층 " + carPlc.getId() + " createUnloadPath");
        var carMst = selectCarStatus(carPlc.getId());
        List<Cell> movePath = new ArrayList<>();
        if(isReorder) {
            // 재지시인 경우, 현재 카의 위치에서 경로 탐색
            movePath= pathService.findPath(true, carMst.getBay(), carMst.getRow(), workingOrder.getToBay(), workingOrder.getToRow());
            log.info(this.floor + "층 " + carPlc.getId() + " PATH from : " + carMst.getBay() + ", " + carMst.getRow() + ", to" + workingOrder.getToBay() + ", " + workingOrder.getToRow());

        }else{
            movePath= pathService.findPath(true, workingOrder.getFromBay(), workingOrder.getFromRow(), workingOrder.getToBay(), workingOrder.getToRow());
            log.info(this.floor + "층 " + carPlc.getId() + " PATH from : " + workingOrder.getFromBay() + ", " + workingOrder.getFromRow() + ", to" + workingOrder.getToBay() + ", " + workingOrder.getToRow());

        }

        List<Shuttle4WayWriteMap> pathCmdList = pathService.buildMoveCommands(movePath);
        carPlc.getCar().setShuttlePathCmdList(pathCmdList);
        carPlc.getCar().setReserveCmdSize(pathCmdList.size());

        if (pathCmdList != null && pathCmdList.size() > 0) {
            updateTbEqRackMstStatus(workingOrder.getFromLocId(), EcsDBConsts.EqRackStatus.READY);
            isSuccess = true;
        } else {
            log.warn(this.floor + "층" + carPlc.getId() + " @@@@@ find path 실패 @@@@@ ");
            // TODO : 언로드경로 생성 실패시
        }
        try {
            log.info(this.floor + "층 " + carPlc.getId() + " UNLOAD move path @@@@");
            log.info(mapper.writeValueAsString(movePath));
            log.info(this.floor + "층 " + carPlc.getId() + " UNLOAD pathCmdList @@@@");
            log.info(mapper.writeValueAsString(pathCmdList));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info(this.floor + "층 " + carPlc.getId() + " createUnloadPath COMP");
        return  isSuccess;
    }


    /**
     * 언로드 지시 전송 + 완료비트 초기화
     */
    private void unloadOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder, TbEqCarMst carMst) {
        // TODO 카의 위치와 목적지 위치가 동일하지 않으면, 다시 로드이송지시 경로계산부터

        log.info(this.floor + "층 " + carPlc.getId() + " unloadOrder");
        boolean isArrived = carMst.getBay() == workingOrder.getToBay() && carMst.getRow() == workingOrder.getToRow();
        if (!isArrived) {
            unloadMoveOrder(carPlc, workingOrder, false);
        } else {
            int[] command = carPlc.getWriteMap().getUnLoadAndClearCommand(nextWorkId());
            sendPlcCommand(carPlc.getId(), command);
            updateTbEqRackMstStatus(workingOrder.getFromLocId(), EcsDBConsts.EqRackStatus.READY);
            updateTbEcsRackOrder(workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD, this.workId);
        }


    }

    /**
     * 지시 완료처리
     */
    private void unloadCompleteOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor + "층 " + carPlc.getId() + " unloadCompleteOrder");
        int[] command = carPlc.getWriteMap().getCompleteClearCommand();
        sendPlcCommand(carPlc.getId(), command);
        updateTbEcsRackOrder(workingOrder, EcsDBConsts.OrderStatus.COMPLETE);
        boolean isInboundTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue();
        // TODO 한층 내에서 재고이동인경우 구분

        if (isInboundTypeOrder) {
            // 입고 타입 언로드 완료시, 목적지 셀 화물 적재 상태로 변경
            updateTbEqRackMstStatus(workingOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO);
            // 이송지시  랙단 컨베이어 완료 업데이트
            updateTbEcsRouteOrder(workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.COMPLETE);
        } else {
            // 출고 타입 언로드 완료시,
            boolean isSameFloorMove = Character.getNumericValue(workingOrder.getFromLocId().charAt(0)) == workingOrder.getLevel();
            // 출고가 같은 층내 이루어 졌다면 (층 내 이동), 목적지 셀 화물 적재 상태로 변경
            if (isSameFloorMove)
                updateTbEqRackMstStatus(workingOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO);
            // 출고 타입 언로드 완료시, 출발지 셀 대기 상태로 변경
            updateTbEqRackMstStatus(workingOrder.getFromLocId(), EcsDBConsts.EqRackStatus.READY);
            // 이송지시 목적지 랙단 컨베이어 완료 업데이트
            updateTbEcsRouteOrder(workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.RACK_CV_READY);

        }
    }


// TODO :  추후 DB 설정 값 보고 구분되어 처리하는 로직 필요

    /**
     * 현재 입출고 포트 하나인 경우에 따른 로직
     * 1. 충전 지시 확인
     * 2. 긴급 지시 확인 (홈 이동지시 포함)
     * 3. 랙단에 올라온 입고 지시 확인
     * 4. 랙단에 올라오고 있는 입고 지시 확인
     * 5. 입고대에 올라온 입고 대기 지시 확인
     * 6. 없는경우 출고 지시 확인
     */

    private void readyOrderManager(List<TbEcsRackOrder> readyOrderList) {
        // 충전 지시 확인
        TbEcsRackOrder chargeOrder = readyOrderList.stream().filter(order -> order.getOrderType() == EcsDBConsts.OrderType.CHARGE.getValue()).findFirst().orElse(null);
        if (chargeOrder != null) {
            log.info(this.floor + "층 " + "readyOrderManager chargeOrder!");
            sendChargeOrder(chargeOrder);
            return;
        }
        // 지시 우선순위 정렬
        readyOrderList.sort(Comparator.comparing(TbEcsRackOrder::getPriority));
        // 긴급지시 확인
        TbEcsRackOrder emoOrder = readyOrderList.stream().filter(order -> order.getPriority() < EcsDBConsts.OrderPriority.NORMAL.getValue()).findFirst().orElse(null);
        if (emoOrder != null) {
            log.info(this.floor + "층 " + "readyOrderManager emo Order!");
            sendEmoOrder(emoOrder);
            return;
        }
        // 랙단에 올라와 있는 입고 화물 확인
        List<TbEcsRouteOrder> rackInCvReadyOrderList = selectTbEcsRouteOrder();
        if (rackInCvReadyOrderList != null && !rackInCvReadyOrderList.isEmpty()) {
            rackInCvCargoReadyOrder(readyOrderList, rackInCvReadyOrderList);
            return;
        }
        // 랙단에 올라오고 있는 입고 화물 확인
        List<TbEqCvMst> rackInCvMstList = selectRackInCvMoveReserveStatus();
        if (rackInCvMstList != null && !rackInCvMstList.isEmpty()) {
            rackInCvReserveReadyOrder(readyOrderList, rackInCvMstList);
            return;
        }
        // 입고대기중인 지시 있는지 확인 (있는 경우 출고 못함) TODO : 입출고포트가 하나가 아닌 경우 수정 필요
        List<TbEcsRouteOrder> inboundScanOrderList = selectTbEcsRouteInboundReady();
        if (inboundScanOrderList != null && inboundScanOrderList.isEmpty()) {
            // 없으면 출고지시 진행
            TbEcsRackOrder outboundRandomOrder = readyOrderList.stream().filter(order -> order.getOrderType() == EcsDBConsts.OrderType.OUTBOUND.getValue()).findFirst().orElse(null);
            if (outboundRandomOrder != null){
                Tspg4WayShuttlePlc carPlc = carPlcManager(outboundRandomOrder);
                TbEqCarMst carMst = carMstManager(carPlc.getId());
                if(carPlc == null || carMst == null){
                    log.info(this.floor + "층 readyOrderManager outboundRandomOrder choiceCar || carMst NULL!");
                    return;
                }
                outboundLoadMoveOrder(carPlc, outboundRandomOrder,  carMst);
            }
        }
    }


    private void sendEmoOrder(TbEcsRackOrder emoOrder) {
        Tspg4WayShuttlePlc carPlc = carPlcManager(emoOrder);
        TbEqCarMst carMst = carMstManager(carPlc.getId());
        if(carPlc == null || carMst == null){
            log.info(this.floor + "층 sendEmoOrder choiceCar || carMst NULL!");
            return;
        }
        var orderType = EcsDBConsts.OrderType.find(emoOrder.getOrderType());
        switch (orderType) {
            case INBOUND -> inboundLoadMoveOrder(carPlc, emoOrder, carMst);
            case OUTBOUND -> outboundLoadMoveOrder(carPlc, emoOrder, carMst);
            case MOVE_HOME -> sendMoveHomeOrder(emoOrder);
            case MOVE_CAR_FLOOR -> moveCarFromRackCv(emoOrder);
        }
    }



    /**
     * 랙단 컨베이어에 올라와있는 대기화물 입고 작업 진행
     */
    private void rackInCvCargoReadyOrder(List<TbEcsRackOrder> readyOrderList, List<TbEcsRouteOrder> rackInCvReadyOrderList) {

        log.info(this.floor + "층  rackInCvReadyOrderList size " + rackInCvReadyOrderList.size());
        rackInCvReadyOrderList.sort(Comparator.comparing(TbEcsRouteOrder::getPriority));
        TbEcsRackOrder readyOrder = null;
        for (var routeOrder : rackInCvReadyOrderList) {
            if (routeOrder.getBarcode() == null)
                continue;
            readyOrder = readyOrderList.stream().filter(order -> order.getBarcode().equals(routeOrder.getBarcode())
                    && order.getOrderKey().equals(routeOrder.getOrderKey())).findFirst().orElse(null)
            ;
            if (readyOrder != null) {
                Tspg4WayShuttlePlc carPlc = carPlcManager(readyOrder);
                if(carPlc != null){
                    TbEqCarMst carMst = carMstManager(carPlc.getId());
                    if(carMst != null)
                        inboundLoadMoveOrder(carPlc, readyOrder, carMst);
                }
                return;
            }
        }
        if (readyOrder == null)
            log.info(this.floor + "층 rackInCvCargoReadyOrder readyOrder matched null");
    }

    /**
     * 랙단 컨베이어에 예약한 올라오고 있는 입고 화물 학인
     */
    private void rackInCvReserveReadyOrder(List<TbEcsRackOrder> readyOrderList, List<TbEqCvMst> rackInCvMstList) {
        TbEqCvMst reserveRackInCvMst = rackInCvMstList.get(0);
        // 랙단 컨베이어 예약한 입고 이송 지시 조회
        List<TbEcsRouteOrder> rackInCvReserveOrderList = selectTbEcsRouteReserveOrder(Integer.parseInt(reserveRackInCvMst.getId()));
        if (rackInCvReserveOrderList != null && !rackInCvReserveOrderList.isEmpty()) {
            log.info(this.floor + "층 랙단컨베이어 예약지시 입고지시 있음");
            TbEcsRouteOrder rackInCvReserveOrder = rackInCvReserveOrderList.get(0);
            TbEcsRackOrder reserveOrder = readyOrderList.stream().filter(order -> order.getBarcode().equals(rackInCvReserveOrder.getBarcode())
                    && order.getOrderKey().equals(rackInCvReserveOrder.getOrderKey())).findFirst().orElse(null);
            // 셔틀카 미리 이송지시 송신
            if (reserveOrder != null) {
                // 미리 송신했을대, 화물 보다 카가 먼저 도착한 경우, 로드를 시도하는 문제 발생
                // reserveOrderSend(carPlc, reserveOrder, carMst);
            }
        }

    }

    private void reserveOrderSend(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder reserveOrder, TbEqCarMst carMst) {
        log.info(this.floor + "층" + carMst.getId() + " 랙단컨베이어 예약지시 입고지시 송신 " + reserveOrder.getOrderKey());
        reloadMap(false, carPlc.getId());
        var isAlreadyTarget = isAlreadyLoadTarget(reserveOrder, carMst);
        if (isAlreadyTarget) {
            reserveOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
            loadOrder(carPlc, reserveOrder, carMst);
            return;
        } else {
            boolean canFindPath = canLoadFindPath(carPlc, reserveOrder, carMst);
            if (!canFindPath) {
                // TODO : 경로 생성 실패
                return;
            }
        }
        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        List<Cell> curPath = cmd.getPath();
        if (canUseCellPathByOtherCar(reserveOrder, curPath, true, carPlc.getCar())) {
            // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
            if (canUseCellPathByCellStatus(curPath, true, carPlc.getCar())) {
                int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
                sendPlcCommand(carPlc.getId(), command);
                updateTbEcsRackOrder(carMst.getEqId(), reserveOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
                updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                // updateTbEqRackMstStatus(readOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO_RESERVE);
                return;
            } else {
                log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
            }
        } else {
            log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
        }
        carPathReset(carPlc.getId(), false);

        // 이동 가능한 셀까지 이동
        // Shuttle4WayWriteMap prefixPath = canMoveCellPath(curPath, false);
        // if (prefixPath == null || prefixPath.getPath().isEmpty()) {
        //     log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canMoveCellPath false prefix 경로 이동 불가. 대기중..");
        //     return;
        // }
        // carPlc.getCar().addPrePixCmd(prefixPath);
    }

    /**
     * 입고지시 로드이송 송신
     */
    private void inboundLoadMoveOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder readyOrder, TbEqCarMst carMst) {
        log.info(this.floor+"층 "+carMst.getId()+" inboundLoadMoveOrder");
        List<TbEcsRouteOrder> routeOrderList = selectTbEcsRouteOrder();
        TbEcsRouteOrder routeOrder = routeOrderList.stream().filter(order -> order.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue()
        ||  order.getOrderType() == EcsDBConsts.OrderType.MOVE.getValue() ).findFirst().orElse(null);
        if (routeOrder == null)
            return;
        reloadMap(false, carPlc.getId());
        boolean isAlreadyTarget = isAlreadyLoadTarget(readyOrder, carMst);
        if(isAlreadyTarget){
            readyOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
            loadOrder( carPlc, readyOrder, carMst);
            return;
        }
        else{
            boolean canFindPath = canLoadFindPath(carPlc, readyOrder, carMst);
            if(!canFindPath){
                // TODO : 로드 경로 생성 실패
                return;
            }
        }
        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        List<Cell> curPath = cmd.getPath();

        if(canUseCellPathByOtherCar(readyOrder, curPath, true, carPlc.getCar())) {
            // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
            if (canUseCellPathByCellStatus(curPath, true, carPlc.getCar())) {
                int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
                sendPlcCommand(carPlc.getId(), command);
                updateTbEcsRackOrder(carMst.getEqId() ,readyOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
                updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                // updateTbEqRackMstStatus(readOrder.getToLocId(), EcsDBConsts.EqRackStatus.CARGO_RESERVE);
                return;
            } else {
                log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
            }
        } else {
            log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
        }

        carPathReset(carPlc.getId(), false);

        // 이동 가능한 셀까지 이동
        // Shuttle4WayWriteMap prefixPath = canMoveCellPath(curPath, false);
        // if (prefixPath == null || prefixPath.getPath().isEmpty()) {
        //     log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canMoveCellPath false prefix 경로 이동 불가. 대기중..");
        //     return;
        // }
        // carPlc.getCar().addPrePixCmd(prefixPath);
    }

    /**
     * 출고지시 로드이송 송신
     */
    private void outboundLoadMoveOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder readyOrder, TbEqCarMst carMst){
        log.info(this.floor+"층 "+carMst.getId()+" outboundLoadMoveOrder");
        List<TbEqCvMst> rackInCvMstList = selectRackInCvMoveReserveStatus();
        if(rackInCvMstList!=null && !rackInCvMstList.isEmpty()) {
            // 랙단컨베이어 예약임으로 return;
            return;
        }
        reloadMap(false, carPlc.getId());
        var isAlreadyTarget = isAlreadyLoadTarget(readyOrder, carMst);
        if(isAlreadyTarget){
            readyOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
            loadOrder(carPlc, readyOrder, carMst);
            return;
        }
        else{
            var canFindPath = canLoadFindPath(carPlc, readyOrder, carMst);
            if(!canFindPath){
                // TODO : 로드 경로 생성 실패
                log.info(this.floor+"층 "+carMst.getId()+" outboundLoadMoveOrder 로드 경로 생성 실패");
                return;
            }
        }

        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        List<Cell> curPath = cmd.getPath();

        if(canUseCellPathByOtherCar(readyOrder, curPath, true, carPlc.getCar())) {
            // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
            if (canUseCellPathByCellStatus(curPath, true, carPlc.getCar())) {
                int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
                sendPlcCommand(carPlc.getId(), command);
                updateTbEcsRackOrder(carMst.getEqId(), readyOrder,EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
                updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                return;
            } else {
                log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
            }
        } else {
            log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
        }

        carPathReset(carPlc.getId(), false);

        // 이동 가능한 셀까지 이동
        // Shuttle4WayWriteMap prefixPath = canMoveCellPath(curPath, false);
        // if (prefixPath == null || prefixPath.getPath().isEmpty()) {
        //     log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canMoveCellPath false prefix 경로 이동 불가. 대기중..");
        //     return;
        // }
        // carPlc.getCar().addPrePixCmd(prefixPath);
    }



    private Tspg4WayShuttlePlc carPlcManager(TbEcsRackOrder order){
        log.info(this.floor + "층 carPlcManager");

        List<TbEqCarMst> eqCarMstList = selectCarStatus();
        List<TbEqCarMst> curCarList = eqCarMstList.stream().filter(carMst -> carMst.getRackEqId().equals(this.rackEqId)
                && carMst.getLevel() == this.floor).collect(Collectors.toList());
        // 지시에 셔틀카 지정되어 있을 경우, 별도 셔틀카 선택하지 않음.
        if(order.getEqCarId() != null && !order.getEqCarId().equals("")){
            log.info(this.floor + "층 getEqCarId() != null");
            return tspgShuttlePlcRegistry.getEquipment(order.getEqCarId());
        }else{
            // 셔틀카 선택
            log.info(this.floor + "층 셔틀카 선택");
            int targetRow = 0;
            int targetBay = 0;
            if(order.getCmdStatus() == EcsDBConsts.EcsRackOrderCmdStatus.MOVE_HOME.getValue()){
                targetRow = order.getToRow();
                targetBay  = order.getToBay();
            }else{
                targetRow = order.getFromRow();
                targetBay = order.getFromBay();
            }

            Tspg4WayShuttlePlc carPlc = choiceCar(curCarList, targetBay, targetRow);
            if (carPlc == null) {
                log.info(this.floor + "층 " + "carPlcManager choiceCar NULL!");
                return null;
            }
            return carPlc;
        }
    }

    private TbEqCarMst carMstManager(String carId){
        List<TbEqCarMst> eqCarMstList = selectCarStatus();
        List<TbEqCarMst> curCarList = eqCarMstList.stream().filter(carMst -> carMst.getRackEqId().equals(this.rackEqId)
                && carMst.getLevel() == this.floor).collect(Collectors.toList());
        TbEqCarMst carMst = curCarList.stream().filter(mst -> mst.getEqId().equals(carId)).findFirst().orElse(null);
        if (carMst == null) {
            log.info(this.floor + "층 " + "readyOrderManager carMst NULL!");
            return null;
        }
        // 충전포트에 있는 경우 충전 진입포트로 이송
        if (isChargePort(carMst)) {
            sendChargeEnterPort(carMst.getId());
            return null;
        }
        return carMst;
    }

    /**
     * 셔틀 차 선정
     */
    private Tspg4WayShuttlePlc choiceCar(List<TbEqCarMst> curCarList, int targetBay, int targetRow) {

        log.info(this.floor + "층 choiceCar");
        List<Tspg4WayShuttlePlc> readyCarPLc = new ArrayList<>();
        curCarList.stream().forEach(carMst -> {
            Tspg4WayShuttlePlc carPlc = tspgShuttlePlcRegistry.getEquipment(carMst.getEqId());
            if(carPlc == null) {
                log.info(this.floor+"층 "+carMst.getId()+" choiceCar carPlc is NULL");
                return;
            }
            boolean isAuto = carMst.isAutoYn();
            boolean isReadyStatus = carMst.getStatus() == EcsDBConsts.EqCarStatus.READY.getValue();
            boolean isPathFinish = carPlc.getCar().getReserveCmdSize() == 0;
            boolean canMove = carMst.getBatteryStatus() ==  Shuttle4WayReadConsts.ShuttleChargeStatus.CAN_WORK.getBitIndex();
            boolean hasCargo = carMst.isCargoYn();
            log.info(this.floor+"층 "+carMst.getId()+ " isReadyStatus : " + isReadyStatus);
            log.info(this.floor+"층 "+carMst.getId()+ " isPathFinish : " + isPathFinish);
            if(!isPathFinish)
                log.info(this.floor+"층 "+carMst.getId()+ " path size "+ carPlc.getCar().getReserveCmdSize());
            log.info(this.floor+"층 "+carMst.getId()+" canMove : " + canMove);
            log.info(this.floor+"층 "+carMst.getId()+" hasCargo : " + hasCargo);
            if(isAuto && isReadyStatus && isPathFinish && !hasCargo){
                if(canMove){
                    readyCarPLc.add(carPlc);
                }else{
                    if(isChargePort(carMst)){
                        return;
                    }
                    carChargeOrderManager(carPlc, carMst);
                }
            }
        });
        log.info(this.floor+"층 "+"[choiceCar] readyCarPLc size : " + readyCarPLc.size());
        if(readyCarPLc.size() == 1){
           return readyCarPLc.get(0);
        }else if (readyCarPLc.size() > 1){
            Set<String> readyPlcSet = readyCarPLc.stream()
                    .map(Tspg4WayShuttlePlc::getId)
                    .collect(Collectors.toSet());

            var car = curCarList.stream()
                    .filter(carMst -> readyPlcSet.contains(carMst.getEqId()))
                    .min(Comparator.comparingInt(carMst ->
                            Math.abs(carMst.getBay() - targetBay) +
                                    Math.abs(carMst.getRow() - targetRow)
                    ))
                    .orElse(null);
            if(car == null){
                log.info(this.floor+"층 "+"[choiceCar] car == null");
                return null;
            }

            log.info(this.floor+"층 "+"[choiceCar] " + car.getId());
            return tspgShuttlePlcRegistry.getEquipment(car.getId());
            // TODO : 선정 조건 추가, 현위치로 부터 x,y 거리 등
        }
        return null;
    }

    /**
     * 셔틀 카 주행 경로 초기화
     *
     */
    private void carPathReset(String carId){
        log.info(this.floor + "층 " + carId + " carPathReset 셔틀 카 주행 경로 초기화");
        Tspg4WayShuttlePlc carPlc =  tspgShuttlePlcRegistry.getEquipment(carId);
        // carPathCellReadyUpdate(carPlc.getCar());
        carPlc.getCar().initPathCmdList();
    }
    private void carPathReset(String carId, boolean isStatusReset){
        log.info(this.floor + "층 " + carId + " carPathReset 셔틀 카 주행 경로 초기화");
        Tspg4WayShuttlePlc carPlc =  tspgShuttlePlcRegistry.getEquipment(carId);
        if(isStatusReset)
            carPathCellReadyUpdate(carPlc.getCar());
        carPlc.getCar().initPathCmdList();
    }



    /**
     * 셔틀 차 홈 포지션 이동 지시 송신
     */
    private void sendMoveHomeOrder(TbEcsRackOrder moveHomeOrder) {
        log.info(this.floor + "층 " + moveHomeOrder.getEqCarId() + " sendMoveHomeOrder");
        List<TbEqCarMst> carMstList = selectCarStatus();
        TbEqCarMst carMst = carMstList.stream().filter(mst -> mst.getEqId().equals(moveHomeOrder.getEqCarId())).findFirst().orElse(null);
        Tspg4WayShuttlePlc carPlc = this.tspgShuttlePlcRegistry.getEquipment(moveHomeOrder.getEqCarId());
        if(carMst != null){
            reloadMap(true, carPlc.getId());
            // 이미 홈 위치인지
            var isAlreadyTarget = isAlreadyHome(moveHomeOrder, carMst);
            if(!isAlreadyTarget){
                // 경로 생성
                var isFindPathComplete = moveHomeFindPath(carPlc, moveHomeOrder, carMst);
                if(isFindPathComplete){
                    Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
                    List<Cell> curPath = cmd.getPath();
                    // 경로 주행 가능한지
                    if(canUseCellPathByOtherCar(moveHomeOrder, curPath, true, carPlc.getCar())) {
                        // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
                        if (canUseCellPathByCellStatus(curPath, true, carPlc.getCar())) {
                            int[] command = cmd.getMoveAndClearCommand(nextWorkId(), this.floor);
                            sendPlcCommand(carPlc.getId(), command);
                            updateTbEcsRackOrder(moveHomeOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.MOVE_HOME, workId);
                            updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                            return;
                        } else {
                            log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
                        }
                    } else {
                        log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
                    }
                    // 이동 가능한 셀까지 이동
                    // Shuttle4WayWriteMap prefixPath = canMoveCellPath(curPath, false);
                    // if (prefixPath == null || prefixPath.getPath().isEmpty()) {
                    //     log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canMoveCellPath false prefix 경로 이동 불가. 대기중..");
                    //     return;
                    // }
                    // carPlc.getCar().addPrePixCmd(prefixPath);
                }else{
                    // TODO : 홈이동 경로 생성 실패
                    return;
                }
            }else{
                // 지시 완료처리
                log.info(this.floor+"층 "+carPlc.getCar().getShuttleCarId() + " sendMoveHomeOrder isAlreadyTarget");
                updateTbEcsRackOrder(moveHomeOrder, EcsDBConsts.OrderStatus.COMPLETE);
            }
        }
    }

    /**
     * 홈 이동지시 목적지 도착 여부 확인
     */
    private boolean isAlreadyHome(TbEcsRackOrder moveHomeOrder, TbEqCarMst carMst) {
        log.info(this.floor+"층 "+"createMoveHomePath");
        int fromRow = Integer.parseInt(carMst.getRackId().substring(1, 3));
        int fromBay =  Integer.parseInt(carMst.getRackId().substring(3, 5));
        int toRow = Integer.parseInt(moveHomeOrder.getToLocId().substring(1, 3));
        int toBay =  Integer.parseInt(moveHomeOrder.getToLocId().substring(3, 5));
        log.info(this.floor+"층 "+"PATH from : " + fromRow + ", " + fromBay + ", to" + toRow + ", " + toBay);
        return pathService.isAlreadyAtTarget(fromRow, fromBay, toRow, toBay);
    }

    /**
     * 홈 이동지시 경로 계산
     */
    private boolean moveHomeFindPath(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder moveHomeOrder, TbEqCarMst carMst){
        boolean canFindPath = false;
        int fromRow = Integer.parseInt(carMst.getRackId().substring(1, 3));
        int fromBay =  Integer.parseInt(carMst.getRackId().substring(3, 5));
        int toRow = Integer.parseInt(moveHomeOrder.getToLocId().substring(1, 3));
        int toBay =  Integer.parseInt(moveHomeOrder.getToLocId().substring(3, 5));

        List<Cell> movePath = pathService.findPath(false, fromBay, fromRow, toBay, toRow);
        List<Shuttle4WayWriteMap> pathCmdList = pathService.buildMoveCommands(movePath);
        if(pathCmdList== null || pathCmdList.size() == 0){
            log.warn(this.floor+"층" +carPlc.getId()+" @@@@@ find path 실패 @@@@@ ");
            return canFindPath;
        }
        carPlc.getCar().setShuttlePathCmdList(pathCmdList);
        carPlc.getCar().setReserveCmdSize(pathCmdList.size());
        try {
            log.info(this.floor+"층 "+carPlc.getId()+" move home path @@@@");
            log.info(mapper.writeValueAsString(movePath));
            log.info(this.floor+"층 "+carPlc.getId()+" move home pathCmdList @@@@");
            log.info(mapper.writeValueAsString(pathCmdList));
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info(this.floor+"층 "+carPlc.getId()+" move home find comp");
        canFindPath = true;
        return canFindPath;
    }

    /**
     * 홈포지션 이동 완료시
     * TODO : 3대인경우 수정
     */
    private void moveHomeComplete(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor+"층 "+carPlc.getId()+" moveHomeComplete");
        int[] command = carPlc.getWriteMap().getCompleteClearCommand();
        sendPlcCommand(carPlc.getId(), command);
        updateTbEcsRackOrder(workingOrder, EcsDBConsts.OrderStatus.COMPLETE);
        TbEcsRackOrder originCarOrder = selectTbEcsOrderOtherCarWorking(carPlc.getId());
        if(originCarOrder != null){
            avoidMoveComplete(originCarOrder);
        }
    }


    /**
     * 셔틀 차 충전 관리
     */
    private void carChargeOrderManager(Tspg4WayShuttlePlc carPlc, TbEqCarMst carMst){

        log.info(this.floor+"층" +carMst.getId()+" carChargeOrderManager");
        List<TbEqRackMst> cellList = selectCellStatus();
        List<TbEqRackMst> chargePortList = selectCellChargePort();
        if(cellList == null || chargePortList == null ||  chargePortList.size() == 0 || cellList.size() == 0){
            log.warn("carChargeOrderManager charge port cell select empty");
        }
        TbEqRackMst chargePortCell =  chargePortList.stream().filter(cell->cell.getType() == EcsDBConsts.RackType.CHARGE_PORT.getValue()).findFirst().orElse(null);
        TbEqRackMst chargeEnterPortCell =  cellList.stream().filter(cell->cell.getType() == EcsDBConsts.RackType.CHARGE_ENTER_PORT.getValue()).findFirst().orElse(null);
        if(chargePortCell != null && chargeEnterPortCell != null){
            TbEcsRackOrder chargeOrder = TbEcsRackOrder.chargeOrder(chargePortCell.getRackId(), chargeEnterPortCell.getRackId(), this.rackEqId, carMst.getEqId());
            createTbEcsRackOrder(chargeOrder);
            log.info(this.floor+"층" +carMst.getId()+ " carChargeOrderManager create chargeOrder");
        }
        log.info(this.floor+"층" +carMst.getId()+" carChargeOrderManager comp");
    }

    /**
     * 셔틀 차 충전 지시 전송
     */
    private void sendChargeOrder(TbEcsRackOrder chargeOrder) {
        List<TbEqCarMst> carMstList = selectCarStatus();
        TbEqCarMst carMst = carMstList.stream().filter(mst -> mst.getEqId().equals(chargeOrder.getEqCarId())).findFirst().orElse(null);
        Tspg4WayShuttlePlc carPlc = this.tspgShuttlePlcRegistry.getEquipment(chargeOrder.getEqCarId());
        if(carMst != null){
            reloadMap(false, carPlc.getId());
            boolean isAlreadyTarget = isAlreadyLoadTarget(chargeOrder, carMst);
            if(isAlreadyTarget){
                chargeOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
                chargeOrder(carPlc, chargeOrder);
                return;
            }else{
                boolean canFindPath = canLoadFindPath(carPlc, chargeOrder, carMst);
                if(!canFindPath){
                    // TODO : 로드 경로 생성 실패
                    return;
                }
            }
            Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
            List<Cell> curPath = cmd.getPath();
            if(canUseCellPathByOtherCar(chargeOrder, curPath, true, carPlc.getCar())) {
                // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
                if (canUseCellPathByCellStatus(curPath, true, carPlc.getCar())) {
                    int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
                    sendPlcCommand(carPlc.getId(), command);
                    updateTbEcsRackOrder(chargeOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.CHARGE_MOVE, workId);
                    updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                    updateTbEqRackMstStatus(chargeOrder.getFromLocId(), EcsDBConsts.EqRackStatus.CHARGE_RESERVE);
                    return;
                } else {
                    log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
                }
            } else {
                log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
            }

            carPathReset(carPlc.getId(), false);
            // 이동 가능한 셀까지 이동
        }
    }

    /**
     * 현재 충전 포트에 있는지
     */
    private boolean isChargePort(TbEqCarMst carMst) {
        List<TbEqRackMst> chargePortList = selectCellChargePort();
        if(chargePortList != null && !chargePortList.isEmpty()){
            var chargePort = chargePortList.get(0);
            return carMst.getRow() == chargePort.getRow() && carMst.getBay() == chargePort.getBay();
        }
        return false;
    }

    /**
     * 충전 진입 포트로 이송 (출발지가 충전 포트인 경우에 사용)
     */
    private void sendChargeEnterPort(String carId){
        Tspg4WayShuttlePlc carPlc = tspgShuttlePlcRegistry.getEquipment(carId);
        var a = selectCellChargeEnterPort();
        if(a != null && !a.isEmpty()){
            int x = a.get(0).getBay();
            int y = a.get(0).getRow();
            var cmd = carPlc.getWriteMap().getMoveCommand(nextWorkId(), x, y);
            sendPlcCommand(carPlc.getId() ,cmd);
        }
    }

    /**
     * 충전 진입 포트까지 이송 완료.
     */
    private void chargeOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder chargeOrder){
        // 충전지시
        // 기존 order 완료 업데이트
        log.info(this.floor+"층 "+carPlc.getId()+" chargeOrder");
        List<TbEqRackMst> chargePortList = selectCellChargePort();
        if(chargePortList != null && !chargePortList.isEmpty()) {
            var chargePort = chargePortList.get(0);
            int[] command = carPlc.getWriteMap().getMoveCommand(nextWorkId(), chargePort.getRow(), chargePort.getBay());
            sendPlcCommand(carPlc.getId(), command);
            updateTbEcsRackOrder(carPlc.getId(),  chargeOrder, EcsDBConsts.EcsRackOrderCmdStatus.CHARGE, this.workId);
        }
    }

    /**
     * 충전 포트로 이송 완료.
     */
    private void chargeComplete(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor+"층 "+carPlc.getId()+" chargeComplete");
        int[] command = carPlc.getWriteMap().getCompleteClearCommand();
        sendPlcCommand(carPlc.getId(), command);
        updateTbEcsRackOrder(workingOrder, EcsDBConsts.OrderStatus.COMPLETE);
    }


    /**
     *  셔틀카 층간 이송 지시
     */
    private void moveCarFromRackCv(TbEcsRackOrder carFloorMoveOrder) {
        log.info(this.floor + "층 " + carFloorMoveOrder.getEqCarId() + " moveCarFromRackCv");

        List<TbEqCarMst> carMstList = selectCarStatus();
        TbEqCarMst carMst = carMstList.stream().filter(mst -> mst.getEqId().equals(carFloorMoveOrder.getEqCarId())).findFirst().orElse(null);
        Tspg4WayShuttlePlc carPlc = this.tspgShuttlePlcRegistry.getEquipment(carFloorMoveOrder.getEqCarId());
        if(carMst != null){
            reloadMap(false, carPlc.getId());
            boolean isAlreadyTarget = isAlreadyLoadTarget(carFloorMoveOrder, carMst);
            if(isAlreadyTarget){
                log.info(this.floor + "층 " + carMst.getId() + " isAlreadyLoadTarget");
                carFloorMoveOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
                carFloorMoveOrder.setPlcCmdId(carMst.getPlcCompCmdId());
                carFloorMoveOrder.setCmdStatus(EcsDBConsts.EcsRackOrderCmdStatus.MOVE_CAR_FROM_RACK_CV.getValue());
                int fromLevel =  Character.getNumericValue(carFloorMoveOrder.getFromLocId().charAt(0));
                log.info(this.floor + "층 " + fromLevel + " fromLevel");
                sendPlcLiftCarMoveCommand(fromLevel);
                iQueryManager.update(carFloorMoveOrder);
                return;
            }else{
                boolean canFindPath = canLoadFindPath(carPlc, carFloorMoveOrder, carMst);
                if(!canFindPath){
                    // TODO : 경로 생성 실패
                    log.error(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + "셔틀카 층간 이송 지시 경로생성실패");
                    return;
                }
            }
            Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
            List<Cell> curPath = cmd.getPath();
            if(canUseCellPathByOtherCar(carFloorMoveOrder, curPath, true, carPlc.getCar())) {
                // 경로 이용 확인, 셀의 예약 여부, 화물 유무 상태로
                if (canUseCellPathByCellStatus(curPath, true, carPlc.getCar())) {
                    int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
                    sendPlcCommand(carPlc.getId(), command);
                    int fromLevel =  Character.getNumericValue(carFloorMoveOrder.getFromLocId().charAt(0));
                    log.info(this.floor + "층 " + fromLevel + " fromLevel");
                    sendPlcLiftCarMoveCommand(fromLevel);
                    updateTbEcsRackOrder(carFloorMoveOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.MOVE_CAR_FROM_RACK_CV, workId);
                    updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);

                } else {
                    log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 셀 예약 또는 화물로 인하여, 가능한 곳 까지 이동");
                }
            } else {
                log.info(this.floor + "층 " + carPlc.getCar().getShuttleCarId() + " canUseCellPath false 다른카가 경로를 막고 있음");
            }

            carPathReset(carPlc.getId(), false);
        }
    }


    /**
     * 셔틀카 단이동
     * 랙단 컨베이어 도착시, 리프트 컨베이어로 이동
     */
    private void moveCarLift(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder moveCarOrder) {
        log.info(this.floor + "층 " + carPlc.getId() + " moveCarLift");
        List<TbEqCvMst> cvMstList = selectRackInCvAllStatus();
        if(cvMstList != null && !cvMstList.isEmpty()){
            int liftType = EcsDBConsts.ConveyorType.LIFT.getValue();
            int fromLevel =  Character.getNumericValue(moveCarOrder.getFromLocId().charAt(0));
            // 리프트의 층 == 출발지랙단컨베이어의층
            var liftCv = cvMstList.stream().filter(cvMst -> cvMst.getType() == liftType && cvMst.getLevel() == fromLevel).findFirst().orElse(null);
            // 리프트 컨베이어 상태 스토퍼 == open
            if(liftCv != null && liftCv.isStopperOpenYn()){
                // TODO : 랙단 +1 이 리프트인지 -1 인지 확인 필요
                int[] command  = carPlc.getWriteMap().getMoveCommand(nextWorkId(), moveCarOrder.getFromBay(), moveCarOrder.getFromRow()+1);
                sendPlcCommand(carPlc.getId(), command);
                updateTbEcsRackOrder(moveCarOrder, EcsDBConsts.EcsRackOrderCmdStatus.MOVE_CAR_LIFT_MOVE, workId);
            }else{
                log.info(this.floor + "층 " + carPlc.getId() + " moveCarLift 대기중");
            }
        }

    }

    /**
     * 셔틀카 단이동
     * 리프트 컨베이어 도착시, 리프트 목적지 층 이송
     */
    private void moveLift(TbEqCarMst carMst, Tspg4WayShuttlePlc carPlc, TbEcsRackOrder moveCarOrder) {
        log.info(this.floor + "층 " + carPlc.getId() + " moveLift");
        boolean isArrivedCar = carMst.getRow() == moveCarOrder.getFromRow()+1 && carMst.getBay() == moveCarOrder.getToBay();
        // 셔틀카 리프트 컨베이어 도착
        if(isArrivedCar){
            log.info(this.floor + "층 " + carPlc.getId() + " moveLift isArrivedCar");
            int toLevel = Character.getNumericValue(moveCarOrder.getToLocId().charAt(0));
            // 리프트 목적지단 이송 명령
            sendPlcLiftCarMoveCommand(toLevel);
            updateTbEcsRackOrder(moveCarOrder, EcsDBConsts.EcsRackOrderCmdStatus.MOVE_CAR_LIFT_CV);
        }
    }

    /**
     * 셔틀카 단이동
     * 리프트 목적지 단 도착시, 셔틀카 랙단 컨베이어 이송
     */
    private void moveCarToRackCv(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder moveCarOrder) {
        log.info(this.floor + "층 " + carPlc.getId() + " moveCarToRackCv");
        List<TbEqCvMst> cvMstList = selectRackInCvAllStatus();
        if(cvMstList != null && !cvMstList.isEmpty()){
            int liftType = EcsDBConsts.ConveyorType.LIFT.getValue();
            int toCvTYpe = EcsDBConsts.ConveyorType.RACK_IN.getValue();
            int toLevel = Character.getNumericValue(moveCarOrder.getToLocId().charAt(0));
            // 리프트의 층 == 목적지 랙단 컨베이어의 층
            var liftCv = cvMstList.stream().filter(cvMst -> cvMst.getType() == liftType && cvMst.getLevel() == toLevel).findFirst().orElse(null);
            // 리프트 컨베이어의 상태 스토퍼 == open
            if(liftCv != null && liftCv.isStopperOpenYn()){
                // 셔틀카 랙단 및 층 정보 송신
                int[] command  = carPlc.getWriteMap().getMoveAndFloorCommand(nextWorkId(), moveCarOrder.getToBay(), moveCarOrder.getToRow(), toLevel);

                updateTbEcsRackOrder(moveCarOrder, EcsDBConsts.EcsRackOrderCmdStatus.MOVE_CAR_TO_RACK_CV, workId, toLevel);
                sendPlcCommand(carPlc.getId(), command);
            }else{
                log.info(this.floor + "층 " + carPlc.getId() + " moveCarToRackCv 대기중");
            }
        }
    }

    /**
     * 셔틀카 단이동 완료
     * 지시 완료
     */
    private void moveCarComplete(TbEqCarMst carMst, Tspg4WayShuttlePlc carPlc, TbEcsRackOrder moveCarOrder) {
        log.info(this.floor + "층 " + carPlc.getId() + " moveCarComplete");
        boolean isArrivedCar = carMst.getRow() == moveCarOrder.getToRow() && carMst.getBay() == moveCarOrder.getToBay();
        // 셔틀카 목적지 랙단 컨베이어 도착
        if(isArrivedCar){
            log.info(this.floor + "층 " + carPlc.getId() + " moveCarComplete isArrivedCar");
            updateTbEcsRackOrder(moveCarOrder, EcsDBConsts.OrderStatus.COMPLETE);
        }
    }

    /**
     * 셔틀카 단이동 리프트 컨베이어 명령 송신
     */
    private void sendPlcLiftCarMoveCommand(int toLevel) {
        log.info(this.floor + "층 " + " sendPlc LiftCarMove Command " + toLevel + "층으로");
        int modeWord = 1 << ConveyorWriteConsts.ConveyorLiftMoveMode.SHUTTLE_CAR_MOVE.getBitIndex();
        int[] command = {modeWord, toLevel};
        int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
        tspgConveyorPlcWriteService.sendCommandConveyor(this.cvEqId, MelsecConsts.DeviceCode.R, firstDeviceCode, command);
    }


    // 현재 랙 그룹의 컨베이어 설비 정보를 가져옴
    public TbEqMst selectEqMst() {
        String eqId = this.rackEqId;
        int type = EcsDBConsts.EqType.CONVEYOR.getValue();
        String sql = """
                select * from tb_eq_mst
                where eq_group_id in (
                select eq_group_id from tb_eq_mst
                where id = :eqId)
                and type = :type
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId,type", eqId, type);
        return iQueryManager.selectBySql(sql, params, TbEqMst.class);
    }

    // 완료되지 않은 현재 랙, 현재 층의 보관설비 지시를 조회
    private List<TbEcsRackOrder> selectTbEcsRackOrder() {
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String eqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_ecs_rack_order
                where order_status <> :status
                and eq_id = :eqId
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId,level", status, eqId, level);
        return iQueryManager.selectListBySql(sql, params, TbEcsRackOrder.class, 0,0);
    }
    private TbEcsRackOrder selectTbEcsRackWorkingOrder(String carId) {
        int status = EcsDBConsts.OrderStatus.WORKING.getValue();
        String eqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_ecs_rack_order
                where order_status = :status
                and eq_id = :eqId
                and level = :level
                and eq_car_id = :carId
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId,level,carId", status, eqId, level,carId);
        return iQueryManager.selectBySql(sql, params, TbEcsRackOrder.class);
    }

    private List<TbEcsRackOrder> selectTbEcsRackWaitOrder() {
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String eqId = this.rackEqId;
        int level = this.floor;
        boolean waitYn = true;
        String sql = """
                select * from tb_ecs_rack_order
                where order_status <> :status
                and eq_id = :eqId
                and level = :level
                and wait_yn = :waitYn
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId,level,waitYn", status, eqId, level, waitYn);
        return iQueryManager.selectListBySql(sql, params, TbEcsRackOrder.class, 0,0);
    }

    private List<TbEcsRackOrder> selectTbEcsRackInboundOrder() {
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        int orderType = EcsDBConsts.OrderType.INBOUND.getValue();
        String eqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_ecs_rack_order
                where order_status <> :status
                and order_type = :orderType
                and eq_id = :eqId
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("status,orderType,eqId,level", status,orderType, eqId, level);
        return iQueryManager.selectListBySql(sql, params, TbEcsRackOrder.class, 0,0);
    }

    private TbEcsRackOrder selectTbEcsOrderOtherCarWorking(String carEqId) {
        int status = EcsDBConsts.OrderStatus.WORKING.getValue();
        String eqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_ecs_rack_order
                where order_status = :status
                and eq_id = :eqId
                and level = :level
                and eq_car_id <> :carEqId
                """;
        Map<String, Object> params = ValueUtil.newMap("status,eqId,level,carEqId", status, eqId, level, carEqId);
        return iQueryManager.selectBySql(sql, params, TbEcsRackOrder.class);
    }

    // 이송지시를 조회
    private TbEcsRouteOrder selectTbEcsRouteOrder(String orderKey) {
        String eqId = this.rackEqId;
        String sql = """
                select * from tb_ecs_route_order
                where order_key = :orderKey
                and eq_id = :eqId
                """;
        Map<String, Object> params = ValueUtil.newMap("orderKey,eqId", orderKey, eqId);
        return iQueryManager.selectBySql(sql, params, TbEcsRouteOrder.class);
    }


    private void updateTbEcsRouteOrder(String orderKey, EcsDBConsts.EcsRouteOrderCmdStatus ecsRouteOrderCmdStatus) {
        String eqId = this.cvEqId;
        int status = ecsRouteOrderCmdStatus.getValue();
        String sql = """
                select * from tb_ecs_route_order
                where order_key = :orderKey
                and eq_id = :eqId
                and cmd_status <> :status
                """;
        Map<String, Object> params = ValueUtil.newMap("orderKey,eqId,status", orderKey, eqId, status);
        var routeOrder = iQueryManager.selectBySql(sql, params, TbEcsRouteOrder.class);
        if(routeOrder == null )
            return;
        routeOrder.setCmdStatus(status);
        log.info(this.floor+"층 "+"update route Order Status: " + orderKey+ ", " + ecsRouteOrderCmdStatus.getDescription());
        iQueryManager.update(routeOrder);
    }

    // 현재 랙설비에 연결된 컨베이어 설비 현재 층을 목저지로 하는 랙단컨베이어대기 상태의 지시를 가져옴
    private List<TbEcsRouteOrder> selectTbEcsRouteOrder() {
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String eqCvId = this.cvEqId;
        int level = this.floor;
        int cmdStatus = EcsDBConsts.EcsRouteOrderCmdStatus.RACK_CV_READY.getValue();
        String sql = """
                select * from tb_ecs_route_order
                where order_status = :status
                and cmd_status = :cmdStatus
                and eq_id = :eqCvId
                and to_cv_id in (
                				 select id from tb_eq_cv_mst	
                				 where level = :level )
                """;
        Map<String, Object> params = ValueUtil.newMap("status,cmdStatus,eqCvId,level", status, cmdStatus, eqCvId, level);
        return iQueryManager.selectListBySql(sql, params, TbEcsRouteOrder.class, 0,0);
    }

    private List<TbEcsRouteOrder> selectTbEcsRouteOrder2() {
        String eqCvId = this.cvEqId;
        int level = this.floor;
        int cmdStatus = EcsDBConsts.EcsRouteOrderCmdStatus.RACK_CV_READY.getValue();
        int cmdStatusLift = EcsDBConsts.EcsRouteOrderCmdStatus.LIFT_MOVE.getValue();
        String sql = """
                select * from tb_ecs_route_order
                where cmd_status in (:cmdStatus, :cmdStatusLift)
                and eq_id = :eqCvId
                and to_cv_id in (
                				 select id from tb_eq_cv_mst	
                				 where level = :level )
                """;
        Map<String, Object> params = ValueUtil.newMap("cmdStatus,cmdStatusLift,eqCvId,level", cmdStatus, cmdStatusLift, eqCvId, level);
        return iQueryManager.selectListBySql(sql, params, TbEcsRouteOrder.class, 0,0);
    }

    // 현재 랙에 연결된 컨베이어의 입고대기 상태읜 완료되지 않은 지시 조회
    private List<TbEcsRouteOrder> selectTbEcsRouteInboundReady() {
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String eqCvId = this.cvEqId;
        int cmdStatus = EcsDBConsts.EcsRouteOrderCmdStatus.INBOUND_READY.getValue();
        String sql = """
                select * from tb_ecs_route_order
                where order_status <> :status
                and cmd_status = :cmdStatus
                and eq_id = :eqCvId
                """;
        Map<String, Object> params = ValueUtil.newMap("status,cmdStatus,eqCvId", status, cmdStatus, eqCvId);
        return iQueryManager.selectListBySql(sql, params, TbEcsRouteOrder.class, 0,0);
    }

    // 현재 랙의 연결된 컨베이어의 화물 예약 상태인 랙단컨베이어 조히
    private List<TbEqCvMst> selectRackInCvMoveReserveStatus(){
        String eqCvId = this.cvEqId;
        int type = EcsDBConsts.ConveyorType.RACK_IN.getValue();
        int status = EcsDBConsts.EqConveyorStatus.MOVE_RESERVE.getValue();
        int level = this.floor;
        String sql = """
                select * from tb_eq_cv_mst
                where eq_id = :eqCvId
                and type = :type
                and status = :status
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("eqCvId,type,status,level", eqCvId, type, status, level);
        return iQueryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }

    // 현재 랙의 연결된 컨베이어의 화물 예약 상태인 랙단컨베이어 조히
    private List<TbEqCvMst> selectRackInCvStatus(){
        String eqCvId = this.cvEqId;
        int type = EcsDBConsts.ConveyorType.RACK_IN.getValue();
        int level = this.floor;
        String sql = """
                select * from tb_eq_cv_mst
                where eq_id = :eqCvId
                and type = :type
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("eqCvId,type,level", eqCvId, type, level);
        return iQueryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }

    // 현재 랙의 연결된 컨베이어의 화물 예약 상태인 랙단컨베이어 조히
    private List<TbEqCvMst> selectRackInCvAllStatus(){
        String eqCvId = this.cvEqId;
        String sql = """
                select * from tb_eq_cv_mst
                where eq_id = :eqCvId
                """;
        Map<String, Object> params = ValueUtil.newMap("eqCvId", eqCvId);
        return iQueryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }



    // 현재랙의 연결된 컨베이어 설비의 현재층 랙단 컨베이어로 오고있는 입고지시 조회
    private List<TbEcsRouteOrder> selectTbEcsRouteReserveOrder(int cvId) {
        int orderType = EcsDBConsts.OrderType.INBOUND.getValue();
        int statusEqSend = EcsDBConsts.OrderStatus.EQ_SEND.getValue();
        int statusWorking = EcsDBConsts.OrderStatus.WORKING.getValue();
        String eqCvId = this.cvEqId;
        String rackInCvId = String.valueOf(cvId);
        String sql = """
                select * from tb_ecs_route_order
                where order_type = :orderType
                and order_status in ( :statusEqSend, :statusWorking)
                and eq_id = :eqCvId
                and to_cv_id = :rackInCvId
                """;
        Map<String, Object> params = ValueUtil.newMap("orderType,statusEqSend,statusWorking,eqCvId,rackInCvId", orderType,statusEqSend, statusWorking, eqCvId, rackInCvId);
        return iQueryManager.selectListBySql(sql, params, TbEcsRouteOrder.class, 0,0);
    }

    // 현재랙의 현재 층의 셔틀카 상태 조회
    private List<TbEqCarMst> selectCarStatus(){
        String rackEqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_eq_car_mst
                where rack_eq_id = :rackEqId
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,level", rackEqId,level);
        return iQueryManager.selectListBySql(sql, params, TbEqCarMst.class, 0,0);
    }
    // 현재랙의 현재 층의 셔틀카 외의 셔틀카 상태 조회
    private List<TbEqCarMst> selectOtherCarStatus(String carId){
        String rackEqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_eq_car_mst
                where rack_eq_id = :rackEqId
                and level = :level
                and id <> :carId
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,level,carId", rackEqId,level,carId);
        return iQueryManager.selectListBySql(sql, params, TbEqCarMst.class, 0,0);
    }

    // 현재랙의 현재 층의 셔틀카 상태 조회
    private TbEqCarMst selectCarStatus(String carId){
        String rackEqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_eq_car_mst
                where rack_eq_id = :rackEqId
                and level = :level
                and id = :carId
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,level,carId", rackEqId,level,carId);
        return iQueryManager.selectBySql(sql, params, TbEqCarMst.class);
    }


    // 현재랙의 현재 층의 셀 상태 조회
    private List<TbEqRackMst> selectCellStatus(){
        String rackEqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_eq_rack_mst
                where eq_id = :rackEqId
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,level", rackEqId,level);
        return iQueryManager.selectListBySql(sql, params, TbEqRackMst.class, 0,0);
    }

    private List<TbEqRackMst> selectCellChargePort(){
        String rackEqId = this.rackEqId;
        int type = EcsDBConsts.RackType.CHARGE_PORT.getValue();
        String sql = """
                select * from tb_eq_rack_mst
                where eq_id = :rackEqId
                and type = :type
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,type", rackEqId,type);
        return iQueryManager.selectListBySql(sql, params, TbEqRackMst.class, 0,0);
    }
    private List<TbEqRackMst> selectCellChargeEnterPort(){
        String rackEqId = this.rackEqId;
        int type = EcsDBConsts.RackType.CHARGE_ENTER_PORT.getValue();
        int level = this.floor;
        String sql = """
                select * from tb_eq_rack_mst
                where eq_id = :rackEqId
                and type = :type
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,type,level", rackEqId,type,level);
        return iQueryManager.selectListBySql(sql, params, TbEqRackMst.class, 0,0);
    }
    // 완료되지 않은 현재 랙, 현재 층의 홈포지션 이동지시를 조회
    private List<TbEcsRackOrder> selectTbEcsMoveHomeRackOrder() {
        int orderType = EcsDBConsts.OrderType.MOVE_HOME.getValue();
        int status = EcsDBConsts.OrderStatus.COMPLETE.getValue();
        String eqId = this.rackEqId;
        int level = this.floor;
        String sql = """
                select * from tb_ecs_rack_order
                where order_type = :orderType 
                and order_status <> :status
                and eq_id = :eqId
                and level = :level
                """;
        Map<String, Object> params = ValueUtil.newMap("orderType,status,eqId,level", orderType,status, eqId, level);
        return iQueryManager.selectListBySql(sql, params, TbEcsRackOrder.class, 0,0);
    }

    private void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.OrderStatus orderStatus) {
        rackOrder.setOrderStatus(orderStatus.getValue());
        log.info(this.floor+"층 "+"update rack Order Status: " + rackOrder.getOrderKey()+ ", " + orderStatus.getDescription());

        this.iQueryManager.update(rackOrder);
    }
    private void updateTbEcsRackOrder(String carId, TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus) {
        rackOrder.setEqCarId(carId);
        rackOrder.setCmdStatus(cmdStatus.getValue());
        log.info(this.floor+"층 "+"update rack Order Status: " + rackOrder.getOrderKey()+ ", " + cmdStatus.getDescription());
        this.iQueryManager.update(rackOrder);
    }
    private void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus) {
        rackOrder.setCmdStatus(cmdStatus.getValue());
        log.info(this.floor+"층 "+"update rack Order Status" + rackOrder.getOrderKey()+ ", " + cmdStatus.getDescription());

        this.iQueryManager.update(rackOrder);
    }
    private void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        log.info(this.floor+"층 "+"update rack Order Status"+ "["+plcCmdId+"] : " + rackOrder.getOrderKey()+ ", " + cmdStatus.getDescription());

        this.iQueryManager.update(rackOrder);
    }

    private void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId, int level) {
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        rackOrder.setLevel(level);
        log.info(this.floor+"층  >> " + level +"층 update rack Order Status"+ "["+plcCmdId+"] : " + rackOrder.getOrderKey()+ ", " + cmdStatus.getDescription());

        this.iQueryManager.update(rackOrder);
    }
    private void updateTbEcsRackOrder(String carEqId,  TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setEqCarId(carEqId);
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        log.info(this.floor+"층 "+"update rack Order Status"+ "["+plcCmdId+"] : " + rackOrder.getOrderKey()+ ", " + cmdStatus.getDescription());

        this.iQueryManager.update(rackOrder);
    }



    private void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.OrderStatus orderStatus, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setOrderStatus(orderStatus.getValue());
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        log.info(this.floor+"층 "+"update rack Order Status"+ "["+plcCmdId+"] : " + rackOrder.getOrderKey()+ ", " + orderStatus.getDescription() +  ", " + cmdStatus.getDescription());

        this.iQueryManager.update(rackOrder);
    }
    private void updateTbEcsRackOrder(String carEqId, TbEcsRackOrder rackOrder, EcsDBConsts.OrderStatus orderStatus, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setOrderStatus(orderStatus.getValue());
        rackOrder.setEqCarId(carEqId);
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        log.info(this.floor+"층 "+"update rack Order Status"+ "["+carEqId+","+plcCmdId+"] : " + rackOrder.getOrderKey()+ ", " + orderStatus.getDescription() +  ", " + cmdStatus.getDescription());

        this.iQueryManager.update(rackOrder);
    }
    private void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, int plcCmdId) {
        rackOrder.setPlcCmdId(plcCmdId);

        this.iQueryManager.update(rackOrder);
    }

    private void updateTbEqRackMstStatus(List<Cell> curPath, EcsDBConsts.EqRackStatus eqRackCmdStatus) {
        List<TbEqRackMst> rackMstList =  selectCellStatus();
        curPath.stream().forEach(path->{
            TbEqRackMst cellMst = rackMstList.stream().filter(cell-> cell.getRow() == path.getLocY() && cell.getBay() == path.getLocX()
                    && cell.getStatus() != eqRackCmdStatus.getValue() ).findFirst().orElse(null);
            if(cellMst!=null){
                log.info(this.floor+"층 "+"update cell Status : "+cellMst.getRackId()+ ", " +eqRackCmdStatus.getDescription());
                cellMst.setStatus(eqRackCmdStatus.getValue());
                this.iQueryManager.update(cellMst);
            }
        });
    }

    private void updateTbEqRackMstStatus(String cellId, EcsDBConsts.EqRackStatus eqRackCmdStatus) {
        List<TbEqRackMst> rackMstList =  selectCellStatus();
        TbEqRackMst cellMst = rackMstList.stream().filter(cell-> cell.getRackId().equals(cellId)).findFirst().orElse(null);
        if(cellMst!=null){
            if(eqRackCmdStatus.getValue() == EcsDBConsts.EqRackStatus.CARGO.getValue())
                cellMst.setCargoYn(true);
            else if (eqRackCmdStatus.getValue() == EcsDBConsts.EqRackStatus.READY.getValue())
                cellMst.setCargoYn(false);
            cellMst.setStatus(eqRackCmdStatus.getValue());
            log.info(this.floor+"층 "+"update cell Status"+ "["+cellId+"] : " + eqRackCmdStatus.getDescription());

            this.iQueryManager.update(cellMst);
        }
    }

    private  void createTbEcsRackOrder(TbEcsRackOrder rackOrder){
        this.iQueryManager.insert(rackOrder);
    }

    private void avoidMoveComplete(TbEcsRackOrder originOrder){
        originOrder.setWaitYn(false);
        iQueryManager.update(originOrder);
    }

    public void doSomething() {

    }
}
