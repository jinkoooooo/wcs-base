package operato.logis.kmat_2026.biz.ecs.tspg4way.core;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayPathService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqPlcMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqRackMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.scheduler.*;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttleMapRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.repository.EcsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.plc.melsec.MelsecConsts;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreManager {
    private final TspgShuttlePlcRegistry tspgShuttlePlcRegistry;
    private final TspgConveyorPlcRegistry tspgConveyorPlcRegistry;
    private final TspgShuttleMapRegistry tspgShuttleMapRegistry;
    private final TspgOrderSchedulerFactory tspgOrderSchedulerFactory;
    private final TspgPlcReadSchedulerFactory tspgPlcReadSchedulerFactory;
    private final TspgEqOrderScheduler tspgEqOrderScheduler;
    private final EcsRepository repository;
    private  Map<String, TspgShuttleOrderScheduler> floorOrderSchedulerMap = new HashMap<>();
    private  Map<String, TspgConveyorOrderScheduler> conveyorOrderSchedulerMap = new HashMap<>();
    private  Map<String, TspgPlcShuttleReadScheduler> shuttlePlcReadSchedulerMap = new HashMap<>();
    private  Map<String, TspgPlcConveyorReadScheduler> cvPlcReadSchedulerMap = new HashMap<>();


    // init() start() 는 세분화 가능.
    @EventListener(ApplicationReadyEvent.class)
    public void init() throws InterruptedException {
        mapInit();
        plcInit();
        plcStatusReadSchedulerInit();
        orderSchedulerInit();
        start();
    }
    public void start() throws InterruptedException {
        plcStart();
        plcReadSchedulerStart();
        orderSchedulerStart();
        orderManagerSchedulerStart();
    }
    public void stop() throws InterruptedException {
        orderManagerSchedulerStop();
        OrderSchedulerStop();
        plcReadSchedulerStop();
        plcStop();
    }


    // TODO : Rack의 기둥 등, 접근 불가능한 Rack 의 정보는 현장별 업데이트 필요. (데이터 생성시)
    public void mapInit(){
        // List<TbEqRackMst> rackList = TbEqRackMst.testDataList();
        List<TbEqRackMst> rackList = repository.findAll(TbEqRackMst.class, "tb_eq_rack_mst");
        Map<String, Map<Integer, List<TbEqRackMst>>> grouped =
                rackList.stream()
                        .collect(Collectors.groupingBy(
                                TbEqRackMst::getEqId,
                                Collectors.groupingBy(TbEqRackMst::getLevel)
                        ));

        grouped.forEach((eqId, levelMap) -> {
            levelMap.forEach((level, cellList) -> {
                if(level != 0) {

                    int maxRow = cellList.stream()
                            .mapToInt(TbEqRackMst::getRow)
                            .max()
                            .orElse(0);

                    int maxBay = cellList.stream()
                            .mapToInt(TbEqRackMst::getBay)
                            .max()
                            .orElse(0);

                    var driveLineList = repository.findDriveOnlyLineByEqIdAndLevel(eqId, level);

                    int[] driveLine = new int[]{};
                    if(driveLineList != null && !driveLineList.isEmpty()) {
                        driveLine = driveLineList.stream()
                                .mapToInt(Integer::intValue)
                                .toArray();
                    }else{
                        log.error("CoreManager mapInit() - driveLineList is empty, 4way 라인 설정 필요");
                    }

                    log.info("[CORE MANAGER]");
                    log.info("eqId :" + eqId + ", level : " + level);
                    log.info("mapInit Shuttle4WayPathService " +maxBay+","+maxRow);
                    log.info("driveLine " + driveLineList.get(0) + "," + driveLineList.get(1));
                    Shuttle4WayPathService pathService = new Shuttle4WayPathService(maxBay, maxRow, driveLine);
                    tspgShuttleMapRegistry.setMapInfo(eqId, level, pathService);
                }
            });
        });
    }
    public void plcInit() {
        try {
            // List<TbEqPlcMst> plcList = TbEqPlcMst.testDataList();
            List<TbEqPlcMst> plcList = repository.findAll(TbEqPlcMst.class, "tb_eq_plc_mst");

            for(TbEqPlcMst plc : plcList){
                switch (EcsDBConsts.PlcEqType.find(plc.getPlcEqType())) {
                    case SHUTTLE_CAR -> {
                        Tspg4WayShuttlePlc tspgShuttlePlc = new Tspg4WayShuttlePlc(plc.getId(), plc.getIp(), plc.getPort(), plc.getPort()+1,
                                MelsecConsts.InterfaceType.find(plc.getPlcIfType()), 801, MelsecConsts.DeviceCode.R, 1051, MelsecConsts.DeviceCode.R);
                        tspgShuttlePlcRegistry.registerEquipment(tspgShuttlePlc.getId(), tspgShuttlePlc);
                        log.info("plcInit() - SHUTTLE_CAR - " + plc.getId());
                    }
                    case CONVEYOR_AND_LIFT -> {
                        TspgConveyorPlc tspgConveyorPlc = new TspgConveyorPlc(plc.getId(), plc.getIp(), plc.getPort(), plc.getPort()+1,
                                MelsecConsts.InterfaceType.find(plc.getPlcIfType()), 10, MelsecConsts.DeviceCode.R, 5, MelsecConsts.DeviceCode.R);
                        tspgConveyorPlcRegistry.registerEquipment(tspgConveyorPlc.getId(), tspgConveyorPlc);
                        log.info("plcInit() - CONVEYOR_AND_LIFT - " + plc.getId());
                    }
                }
            }

        }catch (Exception e){
            log.error(e.toString());
        }
    }
    public void plcStatusReadSchedulerInit(){
        try {
            for (Tspg4WayShuttlePlc shuttle : tspgShuttlePlcRegistry.getAllEquipment()) {
                String plcId = shuttle.getId();
                log.info("plcStatusReadSchedulerInit() - Tspg4WayShuttlePlc - " + plcId);
                if (shuttlePlcReadSchedulerMap.containsKey(plcId)) {
                    continue; // 이미 생성됨
                }
                TspgPlcShuttleReadScheduler scheduler = tspgPlcReadSchedulerFactory.create(shuttle);
                shuttlePlcReadSchedulerMap.put(plcId, scheduler);
                log.info("plcStatusReadSchedulerInit() - TspgPlcShuttleReadScheduler - " + plcId);
            }
            for (TspgConveyorPlc cv : tspgConveyorPlcRegistry.getAllEquipment()) {
                String plcId = cv.getId();
                log.info("plcStatusReadSchedulerInit() - TspgConveyorPlc - " + plcId);
                if (cvPlcReadSchedulerMap.containsKey(plcId)) {
                    continue; // 이미 생성됨
                }
                TspgPlcConveyorReadScheduler scheduler = tspgPlcReadSchedulerFactory.create(cv);
                cvPlcReadSchedulerMap.put(plcId, scheduler);
                log.info("plcStatusReadSchedulerInit() - TspgPlcConveyorReadScheduler - " + plcId);
            }
        }catch (Exception e){
            log.error(e.toString());
        }
    }
    public void orderSchedulerInit(){
        try {
            List<TbEqMst> eqMstList = repository.findAll(TbEqMst.class, "tb_eq_mst");
            List<TbEqRackMst> eqRackMst = repository.findAll(TbEqRackMst.class, "tb_eq_rack_mst");

            eqMstList.stream().filter(eq -> eq.getType() == EcsDBConsts.EqType.CONVEYOR.getValue())
                    .forEach(eq->{
                        TspgConveyorOrderScheduler conveyorOrderScheduler = tspgOrderSchedulerFactory.createConveyorOrderScheduler(eq.getId());
                        conveyorOrderSchedulerMap.put(eq.getId(), conveyorOrderScheduler);
                        log.info("orderSchedulerInit() - conveyorOrderScheduler - " + eq.getId());
                    }
            );

            Map<String, Integer> rackFloorMap = eqRackMst.stream()
                                                        .collect(Collectors.groupingBy(
                                                                TbEqRackMst::getEqId,
                                                                Collectors.collectingAndThen(
                                                                        Collectors.maxBy(Comparator.comparingInt(TbEqRackMst::getLevel)),
                                                                        opt -> opt.map(TbEqRackMst::getLevel).orElse(null))
                                                        ));

            rackFloorMap.forEach((eqid, maxFloor) -> {
                for (int floor = 1; floor <= maxFloor; floor++) {
                    TspgShuttleOrderScheduler shuttleOrderScheduler = tspgOrderSchedulerFactory.createShuttleOrderScheduler(eqid, floor);
                    floorOrderSchedulerMap.put(eqid+floor, shuttleOrderScheduler);
                    log.info("orderSchedulerInit() - shuttleOrderScheduler - " + eqid +"," + floor);
                }
            });


        }catch (Exception e){
            log.error(e.toString());
        }
    }



    public void plcStart() {
        try {
            tspgShuttlePlcRegistry.startAll();
            tspgConveyorPlcRegistry.startAll();
        }catch (Exception e){
            log.error(e.toString());
        }
    }
    public void plcReadSchedulerStart() {
        shuttlePlcReadSchedulerStart();
        cvPlcReadSchedulerStart();
    }
    public void orderSchedulerStart() {
        floorOrderSchedulerStart();
        conveyorOrderSchedulerStart();
    }
    public void orderManagerSchedulerStart() {
        tspgEqOrderScheduler.startScheduler(500);
        log.info("orderManagerSchedulerStart()");
    }


    private void shuttlePlcReadSchedulerStart(){
        for(TspgPlcShuttleReadScheduler scheduler : shuttlePlcReadSchedulerMap.values()){
            scheduler.startScheduler(25);
            log.info("shuttlePlcReadSchedulerStart() - scheduler - " + scheduler.getId());
        }
    }
    private void cvPlcReadSchedulerStart(){
        for(TspgPlcConveyorReadScheduler scheduler : cvPlcReadSchedulerMap.values()){
            scheduler.startScheduler(250);
            log.info("cvPlcReadSchedulerStart() - scheduler - " + scheduler.getId());
        }
    }

    private void floorOrderSchedulerStart() {
        floorOrderSchedulerMap.forEach((key, value) -> {
            value.startScheduler(1000);
        });
    }

    private void conveyorOrderSchedulerStart() {
        conveyorOrderSchedulerMap.forEach((key, value) -> {
            value.startScheduler(1000);
        });
    }

    public void plcStop(){
        try {
            tspgShuttlePlcRegistry.stopAll();
            tspgConveyorPlcRegistry.stopAll();
        }catch (Exception e){
            log.error(e.toString());
        }
    }
    public void plcReadSchedulerStop() {
        shuttlePlcReadSchedulerStop();
        cvPlcReadSchedulerStop();
    }
    public void OrderSchedulerStop() {
        floorOrderSchedulerStop();
        conveyorOrderSchedulerStop();
    }

    public void orderManagerSchedulerStop() {
        tspgEqOrderScheduler.stopScheduler();
    }

    private void shuttlePlcReadSchedulerStop(){
        for(TspgPlcShuttleReadScheduler scheduler : shuttlePlcReadSchedulerMap.values()){
            scheduler.stopScheduler();
        }
    }

    private void cvPlcReadSchedulerStop(){
        for(TspgPlcConveyorReadScheduler scheduler : cvPlcReadSchedulerMap.values()){
            scheduler.stopScheduler();
        }
    }

    private void floorOrderSchedulerStop() {
        floorOrderSchedulerMap.forEach((key, value) -> {
            value.stopScheduler();
        });
    }

    private void conveyorOrderSchedulerStop() {
        conveyorOrderSchedulerMap.forEach((key, value) -> {
            value.stopScheduler();
        });
    }



}
