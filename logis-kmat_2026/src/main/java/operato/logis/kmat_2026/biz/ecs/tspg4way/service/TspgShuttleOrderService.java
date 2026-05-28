package operato.logis.kmat_2026.biz.ecs.tspg4way.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayReadConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayWriteConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Cell;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttleCar;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayPathService;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayWriteMap;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.*;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttleMapRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
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
                                   TspgShuttlePlcWriteService tspgShuttlePlcWriteService, String rackEqId, int floor){
        this.tspgShuttlePlcRegistry = tspgShuttlePlcRegistry;
        this.tspgShuttleMapRegistry = tspgShuttleMapRegistry;
        this.tspgShuttlePlcWriteService = tspgShuttlePlcWriteService;
        this.rackEqId = rackEqId;
        this.floor = floor;
        this.pathService = tspgShuttleMapRegistry.getMapinfo(this.rackEqId, floor);
        var cvMst = selectEqMst();
        this.cvEqId = cvMst.getId();
        this.mapper = new ObjectMapper();
    }

    public void work(){
        ShuttleCarOrderManager();
    }

    /**
     * 셔틀 카 지시 관리
     */
    private void ShuttleCarOrderManager() {
        List<TbEcsRackOrder> orderList = selectTbEcsRackOrder();

        if(orderList!=null && orderList.isEmpty()) {
            // log.info(this.floor+"층 "+"[TspgShuttleOrderService] ShuttleCarOrderManager orderList is Empty");
            return;
        }

        Map<Integer, List<TbEcsRackOrder>> orderMap = orderList.stream().collect(Collectors.groupingBy(TbEcsRackOrder::getOrderStatus));
        List<TbEcsRackOrder> readyOrderList = orderMap.get(EcsDBConsts.OrderStatus.READY.getValue());
        List<TbEcsRackOrder> sendOrderList = orderMap.get(EcsDBConsts.OrderStatus.EQ_SEND.getValue());
        List<TbEcsRackOrder> workingOrderList = orderMap.get(EcsDBConsts.OrderStatus.WORKING.getValue());

        if(sendOrderList != null && !sendOrderList.isEmpty()){
            // log.info(this.floor+"층 "+"[ShuttleCarOrderManager] sendOrderList : " + sendOrderList.size());
            eqSendOrderManager(sendOrderList);
        }
        else if(workingOrderList != null && !workingOrderList.isEmpty()){
            // log.info(this.floor+"층 "+"[ShuttleCarOrderManager] workingOrderList : " + workingOrderList.size());
            workingOrderManager(workingOrderList);
        }
        else if(readyOrderList != null && !readyOrderList.isEmpty()){
            // log.info(this.floor+"층 "+"[ShuttleCarOrderManager] readyOrderList : " + readyOrderList.size());
            readyOrderManager(readyOrderList);
        }
    }

    /**
     * 설비로 전송된 지시 관리
     */
    private void eqSendOrderManager(List<TbEcsRackOrder> sendOrderList) {
        List<TbEqCarMst> eqCarMstList = selectCarStatus();
        for(TbEcsRackOrder sendOrder : sendOrderList){
            Optional<TbEqCarMst> carOpt = eqCarMstList.stream()
                                                            .filter(car-> car.getEqId().equals(sendOrder.getEqCarId())
                                                                                && car.getPlcCmdId() == sendOrder.getPlcCmdId() ).findFirst();
            if(carOpt.isPresent()){
                TbEqCarMst carMst =  carOpt.get();
                boolean isRun = carMst.getStatus() == EcsDBConsts.EqCarStatus.RUN.getValue();
                boolean isComplete = sendOrder.getPlcCmdId() == carMst.getPlcCompCmdId();
                if(isRun || isComplete){
                    updateTbEcsRackOrder(sendOrder, EcsDBConsts.OrderStatus.WORKING);
                }
            }
        }
    }

    /**
     * 진행중인 지시 관리
     */
    private void workingOrderManager(List<TbEcsRackOrder> workingOrderList) {
        List<TbEqCarMst> eqCarMstList = selectCarStatus();
        for(TbEcsRackOrder workingOrder : workingOrderList){
            // 지시에 해당하는 카와 동일카, 지시의 작업번호와 동일 완료작업번호의 셔틀 카 조회
            TbEqCarMst carMst = eqCarMstList.stream()
                                                            .filter(car-> car.getEqId().equals(workingOrder.getEqCarId())
                                                                    && car.getPlcCompCmdId() == workingOrder.getPlcCmdId() ).findFirst().orElse(null);
            if(carMst == null){
                // log.warn(this.floor+"층 "+"workingOrderManager 지시에 해당하는 카와 동일카, 지시의 작업번호와 동일 완료작업번호의 셔틀 카 조회 실패");
            }
            else{
                log.info(this.floor+"층 "+"workingOrderManager : " + carMst.isCompleteYn());
                if(carMst.isCompleteYn()) {
                    Tspg4WayShuttlePlc carPlc =  tspgShuttlePlcRegistry.getEquipment(carMst.getEqId());
                    Tspg4WayShuttleCar car = carPlc.getCar();
                    if(car != null){
                        carPathCompleteManager(car);
                        if(car.hasNextCmd()){
                            log.info(this.floor+"층 "+"hasNextCmd true");
                            boolean isPathFinish = !carPathNextManager(car, workingOrder);
                            log.info(this.floor+"층 "+"workingOrderManager() isPathFinish : " + isPathFinish);
                            if(isPathFinish){
                                // 지시에 대한 경로 남지 않았으면, 지시 완료처리
                                CompleteOrderManager(carMst, carPlc, workingOrder);
                            }else{
                                // 경로주행 대기
                                return;
                            }
                        }else{
                            log.info(this.floor+"층 "+"workingOrderManager() hasNextCmd false isPathFinish true");
                            CompleteOrderManager(carMst, carPlc, workingOrder);
                            return;
                        }

                    }
                }
            }
        }
    }

    /**
     * 셔틀 카 지시의 경로 완료 관리
     */
    private void carPathCompleteManager(Tspg4WayShuttleCar car){
        log.info(this.floor+"층 "+"carPathCompleteManager");
        List<Shuttle4WayWriteMap> cmdList = car.getShuttlePathCmdList();
        if(cmdList != null && !cmdList.isEmpty()) {
            List<Cell> carPath = cmdList.get(car.getReserveCmdCurrentIndex()).getPath();
            // 주행한 경로, 주행예약 상태 해제
            updateTbEqRackMstStatus(carPath, EcsDBConsts.EqRackStatus.READY);
            log.info(this.floor+"층 "+"carPathCompleteManager comp");
        }
    }

    /**
     * 셔틀 카 지시의 다음 경로 관리
     */
    private boolean carPathNextManager(Tspg4WayShuttleCar carPlc, TbEcsRackOrder workingOrder) {
        boolean isLastPath = false;
        boolean isLoadMove = false;
        carPlc.updateReserveCmdCurrentIndex();
        List<Shuttle4WayWriteMap> cmdList = carPlc.getShuttlePathCmdList();
        if(cmdList == null || cmdList.isEmpty()) {
            log.warn(this.floor+"층 carPathNextManager cmdList is empty");
            return false;
        }
        Shuttle4WayWriteMap curCmd = cmdList.get(carPlc.getReserveCmdCurrentIndex());
        List<Cell> curPath = curCmd.getPath();
        // 마지막 언로드의 마지막 경로인지
        if(carPlc.getShuttlePathCmdList().size() == carPlc.getReserveCmdCurrentIndex()+1
                && workingOrder.getCmdStatus() == EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD_MOVE.getValue())
            isLastPath = true;
        else{
            isLoadMove = workingOrder.getCmdStatus() == EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE.getValue();
        }
        // 경로 이용 가능한지
        if(canUseCellPath(curPath, isLastPath, isLoadMove)){
            // 셀 예약
            updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
            // 지시 + 완료 초기화 송신
            int[] command = curCmd.getMoveAndClearCommand(nextWorkId(), floor);
            sendPlcCommand(carPlc.getShuttleCarId(), command);
            updateTbEcsRackOrder(workingOrder, workId);
        }else{
            log.info(this.floor+"층 "+"canUseCellPath false 경로 이용 불가중 @@@@@");
            carPlc.rollBackReserveCmdCurrentIndex();
            return false;
        }
        return true;
    }

    /**
     * 경로 주행 가능한지 주행 예약 여부 확인
     */
    private boolean canUseCellPath(List<Cell> path, boolean isLastUnload, boolean isLoadMove){
        log.info(this.floor+"층 "+"canUseCellPath");
        boolean result = false;
        Set<String> pathSet = path.stream().map(c -> c.getLocX() + "|" + c.getLocY()).collect(Collectors.toSet());
        List<TbEqRackMst> cellList = selectCellStatus();
        List<TbEqRackMst> matched = cellList.stream().filter(a -> pathSet.contains(a.getBay()+ "|" + a.getRow() )).collect(Collectors.toList());
        if(matched == null)
            return result;
        if(isLastUnload)
            result = !matched.isEmpty() && matched.stream().allMatch(cell -> cell.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue()
            ||  cell.getStatus() == EcsDBConsts.EqRackStatus.CARGO_RESERVE.getValue() );
        else if(isLoadMove)
            result = !matched.isEmpty() && matched.stream().allMatch(cell -> cell.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue()
            ||  cell.getStatus() == EcsDBConsts.EqRackStatus.CARGO.getValue() || cell.isCargo_yn());
        else
            result = !matched.isEmpty() && matched.stream().allMatch(cell -> cell.getStatus() == EcsDBConsts.EqRackStatus.READY.getValue());
        log.info(this.floor+"층 "+"canUseCellPath result: " + result);
        return result;
    }

    /**
     * 셔틀카 PLC 명령 송신
     */
    private void sendPlcCommand(String shuttleCar, int[] command) {
        log.info(this.floor+"층 "+"[PLC SEND] shuttleCar sendPlcCommand : " + shuttleCar);
        try {
            log.info(this.mapper.writeValueAsString(command));
        }catch (Exception e){
            log.error(e.toString());
        }
        tspgShuttlePlcWriteService.sendCommandShuttle(shuttleCar, MelsecConsts.DeviceCode.R, Shuttle4WayWriteConsts.ShuttleWriteAddress.WORK_ID.getAddress(), command);
    }


    /**
     * 완료된 지시 관리
     */
    private void CompleteOrderManager(TbEqCarMst carMst, Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder){
        log.info(this.floor+"층 "+"CompleteOrderManager");
        switch (EcsDBConsts.EcsRackOrderCmdStatus.find(workingOrder.getCmdStatus())){
            case LOAD_MOVE -> loadOrder(carPlc, workingOrder, carMst);
            case LOAD -> unloadMoveOrder(carPlc, workingOrder);
            case UNLOAD_MOVE -> unloadOrder(carPlc, workingOrder, carMst);
            case UNLOAD -> unloadCompleteOrder(carPlc, workingOrder);
            case CHARGE_MOVE -> chargeOrder(carPlc, workingOrder);
            case CHARGE -> chargeComplete(carPlc, workingOrder);
        }
    }

    /**
     * 로드지시 전송 + 완료비트 초기화
     */
    private void loadOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder, TbEqCarMst carMst){
        log.info(this.floor+"층 "+"loadOrder");
        // 아직 목적지에 도착하지 못한 경우
        boolean isArrived = carMst.getBay() == workingOrder.getFromBay() && carMst.getRow() == workingOrder.getFromRow();
        if(!isArrived){
            switch (EcsDBConsts.OrderType.find(workingOrder.getOrderType())){
                case INBOUND -> inboundLoadMoveOrder(carPlc, workingOrder, carMst);
                case OUTBOUND ->  outboundLoadMoveOrder(carPlc, workingOrder, carMst);
            }
        }else{
            // 해당 오더키로 입고 이송 지시가 있는지
            // (있다면, 화물이 없어도 랙단으로 load 이송을 하기 때문에, 화물이 있는지 확인하고 load 명령을 내려야함)
            TbEcsRouteOrder routeOrder = selectTbEcsRouteOrder(workingOrder.getOrderKey());
            boolean isRackInRouteOrder = routeOrder != null;
            if(isRackInRouteOrder){
                boolean isCargoArrived = routeOrder.getCmdStatus() == EcsDBConsts.EcsRouteOrderCmdStatus.RACK_CV_READY.getValue();
                // 랙단 컨베이어에 화물이 도착했는지
                if(isCargoArrived){
                    int[] command = carPlc.getWriteMap().getLoadAndClearCommand(nextWorkId());
                    sendPlcCommand(carPlc.getId(), command);
                    updateTbEcsRackOrder(carPlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.LOAD, this.workId);
                }
            }
            // 입고대 입고지시의 로드가 아닌 경우
            else{
                int[] command = carPlc.getWriteMap().getLoadAndClearCommand(nextWorkId());
                sendPlcCommand(carPlc.getId(), command);
                updateTbEcsRackOrder(carPlc.getId(), workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.LOAD, this.workId);
            }
        }

    }

    /**
     * 언로드 이송지시 전송 + 완료비트 초기화
     */
    private void unloadMoveOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor+"층 "+"unloadMoveOrder");
        reloadMap();
        createUnloadPath(carPlc, workingOrder);
        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        if(cmd == null)
            return;
        List<Cell> curPath = cmd.getPath();
        if(canUseCellPath(curPath, false, false)){
            updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
            int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
            sendPlcCommand(carPlc.getId(), command);
            updateTbEcsRackOrder(workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD_MOVE, workId);
            boolean isInboundTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue();
            if(isInboundTypeOrder){
                // 입고 타입 언로드 할 경우, 목적지 셀 화물 예약으로 변경
                updateTbEqRackMstStatus(workingOrder.getToLocCode(), EcsDBConsts.EqRackStatus.CARGO_RESERVE);
            }
        }
    }

    /**
     * 경로 탐색 전 현재 맵(랙 정보) 재조회
     */
    private void reloadMap(){
        log.info(this.floor+"층 "+"reloadMap");
        List<TbEqRackMst> cellList = selectCellStatus();
        cellList.stream().forEach( cell ->{
            // log.info(cell.getBay() + ", " + cell.getRow());
            pathService.updateUseCell(cell.getBay(), cell.getRow(),  cell.isUseYn());
            boolean hasCargo = cell.getStatus() == EcsDBConsts.EqRackStatus.CARGO.getValue()
                    || cell.getStatus() ==  EcsDBConsts.EqRackStatus.CARGO_RESERVE.getValue()
                    || cell.isCargo_yn();
            pathService.updateCargoCell(cell.getBay(), cell.getRow(), hasCargo);
        });
        log.info(this.floor+"층 "+"reloadMap comp");
    }

    /**
     * 경로 생성
     * - 로드/언로드 구분
     * - 충전일 경우 로드경로 사용
     */
    private boolean createLoadPath(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder, TbEqCarMst carMst) {
        log.info(this.floor+"층 "+"createLoadPath");
        int fromRow = Character.getNumericValue(carMst.getRackId().charAt(2));
        int fromBay = Character.getNumericValue(carMst.getRackId().charAt(4));
        int toRow = Character.getNumericValue(workingOrder.getFromLocCode().charAt(2));
        int toBay = Character.getNumericValue(workingOrder.getFromLocCode().charAt(4));
        log.info(this.floor+"층 "+"PATH from : " + fromRow + ", " + fromBay + ", to" + toRow + ", " + toBay);
        if(!pathService.isAlreadyAtTarget(fromRow, fromBay, toRow, toBay)){
            List<Cell> movePath = pathService.findPath(false, fromBay, fromRow, toBay, toRow);
            List<Shuttle4WayWriteMap> pathCmdList = pathService.buildMoveCommands(movePath);
            if(pathCmdList== null || pathCmdList.size() == 0){
               log.warn(this.floor+"층 @@@@@ find path 실패 @@@@@ ");
               return false;
            }
            carPlc.getCar().setShuttlePathCmdList(pathCmdList);
            carPlc.getCar().setReserveCmdSize(pathCmdList.size());
            try {
                log.info(this.floor+"층 "+"LOAD move path @@@@");
                log.info(mapper.writeValueAsString(movePath));
                log.info(this.floor+"층 "+"LOAD pathCmdList @@@@");
                log.info(mapper.writeValueAsString(pathCmdList));
            }
            catch (Exception e) {
                log.error(e.getMessage());
            }
            log.info(this.floor+"층 "+"createLoadPath comp");
            return false;
        }else{
            return true;
        }
    }
    private void createUnloadPath(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor+"층 "+"createUnloadPath");
        List<Cell> movePath = pathService.findPath(true,  workingOrder.getFromBay(), workingOrder.getFromRow(), workingOrder.getToBay(), workingOrder.getToRow());
        List<Shuttle4WayWriteMap> pathCmdList = pathService.buildMoveCommands(movePath);
        carPlc.getCar().setShuttlePathCmdList(pathCmdList);
        carPlc.getCar().setReserveCmdSize(pathCmdList.size());
        log.info(this.floor+"층 "+"PATH from : " + workingOrder.getFromBay() + ", " + workingOrder.getFromRow() + ", to" + workingOrder.getToBay() + ", " + workingOrder.getToRow());

        if(pathCmdList != null && pathCmdList.size() > 0){
            updateTbEqRackMstStatus(workingOrder.getFromLocCode(), EcsDBConsts.EqRackStatus.READY);
        }else{
            log.warn(this.floor+"층 @@@@@ find path 실패 @@@@@ ");
        }
        try {
            log.info(this.floor+"층 "+"UNLOAD move path @@@@");
            log.info(mapper.writeValueAsString(movePath));
            log.info(this.floor+"층 "+"UNLOAD pathCmdList @@@@");
            log.info(mapper.writeValueAsString(pathCmdList));
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info(this.floor+"층 "+"createUnloadPath COMP");
    }


    /**
     * 언로드 지시 전송 + 완료비트 초기화
     */
    private void unloadOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder, TbEqCarMst carMst){
        // TODO 카의 위치와 목적지 위치가 동일하지 않으면, 다시 로드이송지시 경로계산부터

        log.info(this.floor+"층 "+"unloadOrder");
        boolean isArrived = carMst.getBay() == workingOrder.getToBay() && carMst.getRow() == workingOrder.getToRow();
        if(!isArrived){
            unloadMoveOrder(carPlc, workingOrder);
        }else{
            int[] command = carPlc.getWriteMap().getUnLoadAndClearCommand(nextWorkId());
            sendPlcCommand(carPlc.getId(), command);
            updateTbEqRackMstStatus(workingOrder.getFromLocCode(), EcsDBConsts.EqRackStatus.READY);
            updateTbEcsRackOrder(workingOrder, EcsDBConsts.EcsRackOrderCmdStatus.UNLOAD, this.workId);
        }


    }

    /**
     * 지시 완료처리
     */
    private void unloadCompleteOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder){
        log.info(this.floor+"층 "+"unloadCompleteOrder");
        int[] command = carPlc.getWriteMap().getCompleteClearCommand();
        sendPlcCommand(carPlc.getId(), command);
        updateTbEcsRackOrder(workingOrder, EcsDBConsts.OrderStatus.COMPLETE);
        boolean isInboundTypeOrder = workingOrder.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue();
        // TODO 한층 내에서 재고이동인경우 구분

        if(isInboundTypeOrder){
            // 입고 타입 언로드 완료시, 목적지 셀 화물 적재 상태로 변경
            updateTbEqRackMstStatus(workingOrder.getToLocCode(), EcsDBConsts.EqRackStatus.CARGO);
            // 이송지시  랙단 컨베이어 완료 업데이트
            updateTbEcsRouteOrder(workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.COMPLETE);
        }else{
            boolean isTestCase = workingOrder.getFromRow() == 6 && workingOrder.getFromBay() == 3;
            if(isTestCase){
                // 입고 타입 언로드 완료시, 목적지 셀 화물 적재 상태로 변경
                updateTbEqRackMstStatus(workingOrder.getToLocCode(), EcsDBConsts.EqRackStatus.CARGO);
            }else{ // 출고 타입 언로드 완료시, 출발지 셀 대기 상태로 변경
                updateTbEqRackMstStatus(workingOrder.getFromLocCode(), EcsDBConsts.EqRackStatus.READY);
                // 이송지시 목적지 랙단 컨베이어 완료 업데이트
                updateTbEcsRouteOrder(workingOrder.getOrderKey(), EcsDBConsts.EcsRouteOrderCmdStatus.RACK_CV_READY);
            }

        }
    }


// TODO : 현장 운영에 따른 입고지시, 출고지시 선정 기준
    /**
     * ========================================
     * 입고지시, 출고지시 선정 기준(2026.03.11)
     * 기존 재고이동 X, 입고/출고 동시에 운영 X
     * 전시회 기준 재고이동 O, 입고/재고이동 동시에 운영 O
     * ========================================
     *
     * TB_ECS_ROUTE_ORDER에 랙단 대기 인 지시 확인
     * 해당 지시 입고 타입 > 입고지시 진행
     * 해당 지시 출고 타입 > 다음 출고 지시 미리 진행
     *
     * 랙단 대기 인 지시 없음
     * TB_EQ_CV_MST 컨베이어 상태 예약인 경우 (입고대에서 화물이 올라오고 있는 경우)
     * TB_ECS_ROUTE_ORDER에 해당층 랙단 컨베이어가 목적지인 설비지시전송 또는 작업중인 지시 확인
     * 위 지시와 같은 rack_order 입고 지시 시작
     *
     * 셔틀이 입고대에 화물이 없어 출고를 진행하는 도중 입고대 화물 올려두면 운영이슈.
     *
     * 대기중인 지시를 입고/출고 운영 모드로 관리할경우 다른 함수 사용 필요
     */

    private void readyOrderManager(List<TbEcsRackOrder> readyOrderList) {
        // 충전 지시 확인
        TbEcsRackOrder chargeOrder =  readyOrderList.stream().filter(order-> order.getOrderType() == EcsDBConsts.OrderType.CHARGE.getValue()).findFirst().orElse(null   );
        if(chargeOrder != null){
            log.info(this.floor+"층 "+"readyOrderManager chargeOrder!");
            sendChargeOrder(chargeOrder);
            return;
        }
        // 현재 설비(랙)의 현재 층의 셔틀 카 조회
        List<TbEqCarMst> eqCarMstList = selectCarStatus();
        List<TbEqCarMst> curCarList = eqCarMstList.stream().filter(carMst -> carMst.getRackEqId().equals(this.rackEqId)
                && carMst.getLevel() == this.floor).collect(Collectors.toList());
        // 셔틀 카 선정
        Tspg4WayShuttlePlc carPlc = choiceCar(curCarList);
        if(carPlc == null){
            log.info(this.floor+"층 "+"readyOrderManager choiceCar NULL!");
            return;
        }
        TbEqCarMst carMst = curCarList.stream().filter(mst -> mst.getEqId().equals(carPlc.getCar().getShuttleCarId())).findFirst().orElse(null);
        if(carMst == null) {
            log.info(this.floor+"층 "+"readyOrderManager carMst NULL!");
            return;
        }
        // TODO : DB에서 불러오는 것으로 수정 필요
        boolean isChargePort = carMst.getRow() == 5 && carMst.getBay() == 0;
        if(isChargePort){
            var cmd = carPlc.getWriteMap().getChargeOutCommand(nextWorkId());
            sendPlcCommand(carPlc.getId() ,cmd);
        }

        readyOrderList.sort(Comparator.comparing(TbEcsRackOrder::getPriority));
        // 랙단에 올라와 있는 화물 확인
        List<TbEcsRouteOrder> rackInCvReadyOrderList = selectTbEcsRouteOrder();
        rackInCvReadyOrderList.sort(Comparator.comparing(TbEcsRouteOrder::getPriority));
        log.info(this.floor+"층 "+"rackInCvReadyOrderList size "  + rackInCvReadyOrderList.size());
        if(rackInCvReadyOrderList!=null && !rackInCvReadyOrderList.isEmpty()){
            TbEcsRackOrder readyOrder = null;
            EcsDBConsts.OrderType rackCvReadyOrderType = null;
            for(var routeOrder : rackInCvReadyOrderList){
                if(routeOrder.getBarcode() == null)
                    continue;
                readyOrder = readyOrderList.stream().filter(order->order.getBarcode().equals(routeOrder.getBarcode())
                        && order.getOrderKey().equals(routeOrder.getOrderKey())).findFirst().orElse(null)
                ;
                if(readyOrder != null) {
                    rackCvReadyOrderType = EcsDBConsts.OrderType.find(readyOrder.getOrderType());
                    break;
                }

            }
            if(readyOrder == null || rackCvReadyOrderType == null){
                log.info(this.floor+"층 "+"readyOrder null");
                return;
            }
            switch(rackCvReadyOrderType){
                case INBOUND -> inboundLoadMoveOrder(carPlc, readyOrder, carMst);
                case OUTBOUND -> outboundLoadMoveOrder(carPlc, readyOrder, carMst);
            }
        }else{
            // 랙단 컨베이어 예약인지 확인
            List<TbEqCvMst> rackInCvMstList = selectRackInCvStatus();
            if(rackInCvMstList!= null && !rackInCvMstList.isEmpty()){
                TbEqCvMst reserveRackInCvMst = rackInCvMstList.get(0);
                // 랙단 컨베이어 예약한 입고 이송 지시 조회
                List<TbEcsRouteOrder> rackInCvReserveOrderList = selectTbEcsRouteReserveOrder(Integer.parseInt(reserveRackInCvMst.getId()));
                if(rackInCvReserveOrderList!=null && !rackInCvReserveOrderList.isEmpty()){
                    log.info(this.floor+"층 랙단컨베이어 예약지시 입고지시 있음");
                    TbEcsRouteOrder rackInCvReserveOrder = rackInCvReserveOrderList.get(0);
                    TbEcsRackOrder reserveOrder = readyOrderList.stream().filter(order->order.getBarcode().equals(rackInCvReserveOrder.getBarcode())
                            && order.getOrderKey().equals(rackInCvReserveOrder.getOrderKey())).findFirst().orElse(null);
                    // 셔틀카 미리 이송지시 송신
                    if(reserveOrder != null){
                        // reserveOrderSend(carPlc, reserveOrder, carMst);
                    }
                }

            }else{
                List<TbEcsRouteOrder> inboundScanOrderList = selectTbEcsRouteInboundReady();
                if(inboundScanOrderList!=null && inboundScanOrderList.isEmpty()){
                    // 없으면 출고지시 진행
                    TbEcsRackOrder outboundRandomOrder = readyOrderList.stream().filter(order->order.getOrderType()==EcsDBConsts.OrderType.OUTBOUND.getValue()).findFirst().orElse(null);
                    if(outboundRandomOrder != null)
                        outboundLoadMoveOrder(carPlc, outboundRandomOrder, carMst);
                }
            }
        }
    }

    private void reserveOrderSend(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder reserveOrder, TbEqCarMst carMst){
        log.info(this.floor+"층 랙단컨베이어 예약지시 입고지시 송신 "+reserveOrder.getOrderKey());
        reloadMap();
        var isComplete = createLoadPath(carPlc, reserveOrder, carMst);
        if(isComplete){
            reserveOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
            loadOrder( carPlc, reserveOrder, carMst);
            return;
        }
        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        List<Cell> curPath = cmd.getPath();
        if (canUseCellPath(curPath, false, true)) {
            try {
                log.info(this.floor+"층 "+"curPath ::::");
                log.info(mapper.writeValueAsString(curPath));
            }catch (Exception e){
                log.error(e.toString());
            }
            updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
            int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
            sendPlcCommand(carPlc.getId(), command);
            updateTbEcsRackOrder(carMst.getEqId() ,reserveOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
            // updateTbEqRackMstStatus(readOrder.getToLocCode(), EcsDBConsts.EqRackStatus.CARGO_RESERVE);
        }
    }

    /**
     * 입고지시 로드이송 송신
     */
    private void inboundLoadMoveOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder readOrder, TbEqCarMst carMst) {
        log.info(this.floor+"층 "+"inboundLoadMoveOrder");
        List<TbEcsRouteOrder> routeOrderList = selectTbEcsRouteOrder();
        TbEcsRouteOrder routeOrder = routeOrderList.stream().filter(order -> order.getOrderType() == EcsDBConsts.OrderType.INBOUND.getValue()
        ||  order.getOrderType() == EcsDBConsts.OrderType.MOVE.getValue() ).findFirst().orElse(null);
        if (routeOrder == null)
            return;
        reloadMap();
        var isComplete = createLoadPath(carPlc, readOrder, carMst);
        if(isComplete){
            readOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
            loadOrder( carPlc, readOrder, carMst);
            return;
        }
        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        List<Cell> curPath = cmd.getPath();
        if (canUseCellPath(curPath, false, true)) {
            try {
                log.info(this.floor+"층 "+"curPath ::::");
                log.info(mapper.writeValueAsString(curPath));
            }catch (Exception e){
                log.error(e.toString());
            }
            updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
            int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
            sendPlcCommand(carPlc.getId(), command);
            updateTbEcsRackOrder(carMst.getEqId() ,readOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
            // updateTbEqRackMstStatus(readOrder.getToLocCode(), EcsDBConsts.EqRackStatus.CARGO_RESERVE);
        }
    }

    /**
     * 출고지시 로드이송 송신
     */
    private void outboundLoadMoveOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder readOrder, TbEqCarMst carMst){
        log.info(this.floor+"층 "+"outboundLoadMoveOrder");
        List<TbEqCvMst> rackInCvMstList = selectRackInCvStatus();
        if(rackInCvMstList!=null && !rackInCvMstList.isEmpty()) {
            // 랙단컨베이어 예약임으로 return;
            return;
        }

        reloadMap();
        var isComplete = createLoadPath(carPlc, readOrder, carMst);
        if(isComplete){
            readOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
            loadOrder(carPlc, readOrder, carMst);
            return;
        }
        Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
        List<Cell> curPath = cmd.getPath();
        if(canUseCellPath(curPath, false, true)){
            updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
            int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
            sendPlcCommand(carPlc.getId(), command);
            updateTbEcsRackOrder(carMst.getEqId(), readOrder,EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.LOAD_MOVE, workId);
        }
    }

    /**
     * 셔틀 차 선정
     */
    private Tspg4WayShuttlePlc choiceCar(List<TbEqCarMst> curCarList) {
        List<Tspg4WayShuttlePlc> readyCarPLc = new ArrayList<>();
        curCarList.stream().forEach(carMst -> {
            Tspg4WayShuttlePlc carPlc = tspgShuttlePlcRegistry.getEquipment(carMst.getEqId());
            if(carPlc == null) {
                log.info(this.floor+"층 "+"choiceCar carPlc is NULL");
                return;
            }
            boolean isReadyStatus = carMst.getStatus() == EcsDBConsts.EqCarStatus.READY.getValue();
            boolean isPathFinish = carPlc.getCar().getReserveCmdSize() == 0;
            boolean canMove = carMst.getBatteryStatus() ==  Shuttle4WayReadConsts.ShuttleChargeStatus.CAN_WORK.getBitIndex();
            log.info(this.floor+"층 "+"isReadyStatus : " + isReadyStatus);
            log.info(this.floor+"층 "+"isPathFinish : " + isPathFinish);
            if(!isPathFinish)
                log.info(this.floor+"층 "+"path size "+ carPlc.getCar().getReserveCmdSize());
            log.info(this.floor+"층 "+"canMove : " + canMove);
            if(isReadyStatus && isPathFinish){
                if(canMove){
                    readyCarPLc.add(carPlc);
                }else{
                    // TODO : DB에서 불러오는 것으로 수정 필요
                    boolean isAlreadyChargePort = carMst.getRow() == 5 && carMst.getBay() == 0;
                    if(isAlreadyChargePort){
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
            return readyCarPLc.get(0);
            // TODO : 선정 조건 추가, 현위치로 부터 x,y 거리 등
        }
        return null;
    }

    /**
     * 셔틀 차 충전 관리
     */
    private void carChargeOrderManager(Tspg4WayShuttlePlc carPlc, TbEqCarMst carMst){

        log.info("carChargeOrderManager");
        List<TbEqRackMst> cellList = selectCellStatus();
        List<TbEqRackMst> chargePortList = selectCellChargePort();
        if(cellList == null || chargePortList == null ||  chargePortList.size() == 0 || cellList.size() == 0){
            log.warn("carChargeOrderManager charge port cell select empty");
        }
        TbEqRackMst chargePortCell =  chargePortList.stream().filter(cell->cell.getType() == EcsDBConsts.RackType.CHARGE_PORT.getValue()).findFirst().orElse(null);
        TbEqRackMst chargeEnterPortCell =  cellList.stream().filter(cell->cell.getType() == EcsDBConsts.RackType.CHARGE_ENTER_PORT.getValue()).findFirst().orElse(null);
        if(chargePortCell != null && chargeEnterPortCell != null){
            TbEcsRackOrder chargeOrder = TbEcsRackOrder.chargeOrder(chargePortCell.getId(), chargeEnterPortCell.getId(), this.rackEqId, carMst.getEqId());
            createTbEcsRackOrder(chargeOrder);
            log.info("carChargeOrderManager create chargeOrder");
        }
        log.info("carChargeOrderManager comp");
    }

    /**
     * 셔틀 차 충전 지시 전송
     */
    private void sendChargeOrder(TbEcsRackOrder chargeOrder) {
        List<TbEqCarMst> carMstList = selectCarStatus();
        TbEqCarMst carMst = carMstList.stream().filter(mst -> mst.getEqId().equals(chargeOrder.getEqCarId())).findFirst().orElse(null);
        Tspg4WayShuttlePlc carPlc = this.tspgShuttlePlcRegistry.getEquipment(chargeOrder.getEqCarId());
        if(carMst != null){
            reloadMap();
            var isComplete = createLoadPath(carPlc, chargeOrder, carMst);
            if(isComplete){
                chargeOrder.setOrderStatus(EcsDBConsts.OrderStatus.WORKING.getValue());
                chargeOrder(carPlc, chargeOrder);
                return;
            }
            Shuttle4WayWriteMap cmd = carPlc.getCar().getShuttlePathCmdList().get(carPlc.getCar().getReserveCmdCurrentIndex());
            List<Cell> curPath = cmd.getPath();
            if(canUseCellPath(curPath, false, true)){
                updateTbEqRackMstStatus(curPath, EcsDBConsts.EqRackStatus.MOVE_RESERVE);
                int[] command = cmd.getMoveAndClearCommand(nextWorkId(), floor);
                sendPlcCommand(carPlc.getId(), command);
                updateTbEcsRackOrder(chargeOrder, EcsDBConsts.OrderStatus.EQ_SEND, EcsDBConsts.EcsRackOrderCmdStatus.CHARGE_MOVE, workId);
                updateTbEqRackMstStatus(chargeOrder.getFromLocCode(), EcsDBConsts.EqRackStatus.CHARGE_RESERVE);
            }
        }
    }

    /**
     * 충전 진입 포트까지 이송 완료.
     */
    private void chargeOrder(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder chargeOrder){
        // 충전지시
        // 기존 order 완료 업데이트
        log.info(this.floor+"층 "+"chargeOrder");
        int[] command = carPlc.getWriteMap().getChargeCommand(nextWorkId());
        sendPlcCommand(carPlc.getId(), command);
        updateTbEcsRackOrder(carPlc.getId(),  chargeOrder, EcsDBConsts.EcsRackOrderCmdStatus.CHARGE, this.workId);
    }


    /**
     * 충전 포트로 이송 완료.
     */
    private void chargeComplete(Tspg4WayShuttlePlc carPlc, TbEcsRackOrder workingOrder) {
        log.info(this.floor+"층 "+"chargeComplete");
        int[] command = carPlc.getWriteMap().getCompleteClearCommand();
        sendPlcCommand(carPlc.getId(), command);
        updateTbEcsRackOrder(workingOrder, EcsDBConsts.OrderStatus.COMPLETE);
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
    private List<TbEqCvMst> selectRackInCvStatus(){
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

    private void updateTbEcsRackOrder(TbEcsRackOrder rackOrder, EcsDBConsts.EcsRackOrderCmdStatus cmdStatus, int plcCmdId) {
        rackOrder.setCmdStatus(cmdStatus.getValue());
        rackOrder.setPlcCmdId(plcCmdId);
        log.info(this.floor+"층 "+"update rack Order Status"+ "["+plcCmdId+"] : " + rackOrder.getOrderKey()+ ", " + cmdStatus.getDescription());

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
            TbEqRackMst cellMst = rackMstList.stream().filter(cell-> cell.getRow() == path.getLocY() && cell.getBay() == path.getLocX()).findFirst().orElse(null);
            if(cellMst!=null){
                log.info(this.floor+"층 "+"update cell Status : "+cellMst.getId()+ ", " +eqRackCmdStatus.getDescription());
                cellMst.setStatus(eqRackCmdStatus.getValue());
                this.iQueryManager.update(cellMst);
            }
        });
    }
    private void updateTbEqRackMstStatus(String cellId, EcsDBConsts.EqRackStatus eqRackCmdStatus) {
        List<TbEqRackMst> rackMstList =  selectCellStatus();
        TbEqRackMst cellMst = rackMstList.stream().filter(cell-> cell.getId().equals(cellId)).findFirst().orElse(null);
        if(cellMst!=null){
            if(eqRackCmdStatus.getValue() == EcsDBConsts.EqRackStatus.CARGO.getValue())
                cellMst.setCargo_yn(true);
            else if (eqRackCmdStatus.getValue() == EcsDBConsts.EqRackStatus.READY.getValue())
                cellMst.setCargo_yn(false);
            cellMst.setStatus(eqRackCmdStatus.getValue());
            log.info(this.floor+"층 "+"update cell Status"+ "["+cellId+"] : " + eqRackCmdStatus.getDescription());

            this.iQueryManager.update(cellMst);
        }
    }

    private  void createTbEcsRackOrder(TbEcsRackOrder rackOrder){
        this.iQueryManager.insert(rackOrder);
    }


    public void doSomething() {

    }
}
