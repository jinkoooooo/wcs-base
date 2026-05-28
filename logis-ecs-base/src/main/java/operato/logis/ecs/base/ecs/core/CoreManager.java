package operato.logis.ecs.base.ecs.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEqMst;
import operato.logis.ecs.base.ecs.entity.TbEqPlcMst;
import operato.logis.ecs.base.ecs.entity.TbEqRackMst;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneMapManager;
import operato.logis.ecs.base.ecs.plc.crane.StackerCranePlcManager;
import operato.logis.ecs.base.ecs.repository.EcsRepository;
import operato.logis.ecs.base.ecs.scheduler.*;
import operato.logis.ecs.base.ecs.scheduler.conveyor.ConveyorOrderScheduler;
import operato.logis.ecs.base.ecs.scheduler.crane.StackerCraneOrderScheduler;
import operato.logis.ecs.base.ecs.service.path.StackerCranePathService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreManager {
    private final StackerCranePlcManager cranePlcManager;
    private final ConveyorPlcManager cvPlcManager;
    private final StackerCraneMapManager craneMapManager;
    private final OrderSchedulerFactory orderSchedulerFactory;
    private final PlcReadSchedulerFactory plcReadSchedulerFactory;
    private final EqOrderScheduler eqOrderScheduler;
    private final StackerCranePathService stackerCranePathService;
    private final EcsRepository repository;
    private Map<String, StackerCraneOrderScheduler> floorOrderSchedulerMap = new HashMap<>();
    private Map<String, ConveyorOrderScheduler> conveyorOrderSchedulerMap = new HashMap<>();
    private Map<String, PlcCraneReadScheduler> shuttlePlcReadSchedulerMap = new HashMap<>();
    private Map<String, PlcConveyorReadScheduler> cvPlcReadSchedulerMap = new HashMap<>();

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
    public void mapInit() {
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
                if (level != 0) {

                    int maxRow = cellList.stream()
                            .mapToInt(TbEqRackMst::getAsiel) // todo: asiel 검토
                            .max()
                            .orElse(0);

                    int maxBay = cellList.stream()
                            .mapToInt(TbEqRackMst::getBay)
                            .max()
                            .orElse(0);

                    var driveLineList = repository.findDriveOnlyLineByEqIdAndLevel(eqId, level);

                    int[] driveLine = new int[]{};
                    if (driveLineList != null && !driveLineList.isEmpty()) {
                        driveLine = driveLineList.stream()
                                .mapToInt(Integer::intValue)
                                .toArray();
                    } else {
                        log.error("CoreManager mapInit() - driveLineList is empty, 4way 라인 설정 필요");
                    }

                    log.info("[CORE MANAGER]");
                    log.info("eqId :" + eqId + ", level : " + level);
                    log.info("mapInit CranePathService " + maxBay + "," + maxRow);
                    log.info("driveLine " + driveLineList.get(0) + "," + driveLineList.get(1));
                    //CranePathService pathService = new CranePathService(maxBay, maxRow, driveLine); // todo: 기존 CranePathService DTO -> Service 변경에 따른 호출 방식 수정
                    craneMapManager.setMapInfo(eqId, level, level, stackerCranePathService);
                }
            });
        });
    }

    public void plcInit() {
        try {
            // List<TbEqPlcMst> plcList = TbEqPlcMst.testDataList();
            List<TbEqPlcMst> plcList = repository.findAll(TbEqPlcMst.class, "tb_eq_plc_mst");

            for (TbEqPlcMst plc : plcList) {
                switch (EcsDBConsts.PlcEqType.find(plc.getPlcEqType())) {
                    case SHUTTLE_CAR -> {
                        int readFirstDeviceCode = 801; // todo: StackerCraneReadMap 기반 세팅 검토. 매 프로젝트 별로 변경 필요
                        int writeFirstDeviceCode = 1051; // todo: StackerCraneWriteMap 기반 세팅 검토. 매 프로젝트 별로 변경 필요
                        MelsecConsts.DeviceCode readMemory = MelsecConsts.DeviceCode.R;
                        MelsecConsts.DeviceCode writeMemory = MelsecConsts.DeviceCode.R;
                        StackerCranePlc cranePlc = new StackerCranePlc(plc.getId(), plc.getIp(), plc.getPort(), plc.getPort() + 1, MelsecConsts.InterfaceType.find(plc.getPlcIfType()), readFirstDeviceCode, readMemory, writeFirstDeviceCode, writeMemory);

                        new StackerCranePlc(plc.getId(), plc.getIp(), plc.getPort(), plc.getPort() + 1,
                                MelsecConsts.InterfaceType.find(plc.getPlcIfType()), 801, MelsecConsts.DeviceCode.R, 1051, MelsecConsts.DeviceCode.R);
                        cranePlcManager.registerEquipment(cranePlc.getId(), cranePlc);
                        log.info("plcInit() - SHUTTLE_CAR - " + plc.getId());
                    }
                    case CONVEYOR_AND_LIFT -> {
                        ConveyorPlc cvPlc = new ConveyorPlc(plc.getId(), plc.getIp(), plc.getPort(), plc.getPort() + 1,
                                MelsecConsts.InterfaceType.find(plc.getPlcIfType()), 10, MelsecConsts.DeviceCode.R, 5, MelsecConsts.DeviceCode.R);
                        cvPlcManager.registerEquipment(cvPlc.getId(), cvPlc);
                        log.info("plcInit() - CONVEYOR_AND_LIFT - " + plc.getId());
                    }
                }
            }

        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    public void plcStatusReadSchedulerInit() {
        try {
            for (StackerCranePlc cranePlc : cranePlcManager.getAllEquipment()) {
                String plcId = cranePlc.getId();
                log.info("plcStatusReadSchedulerInit() - StackerCranePlc - " + plcId);
                if (shuttlePlcReadSchedulerMap.containsKey(plcId)) {
                    continue; // 이미 생성됨
                }
                PlcCraneReadScheduler scheduler = plcReadSchedulerFactory.create(cranePlc);
                shuttlePlcReadSchedulerMap.put(plcId, scheduler);
                log.info("plcStatusReadSchedulerInit() - MovexPlcShuttleReadScheduler - " + plcId);
            }
            for (ConveyorPlc cv : cvPlcManager.getAllEquipment()) {
                String plcId = cv.getId();
                log.info("plcStatusReadSchedulerInit() - TspgConveyorPlc - " + plcId);
                if (cvPlcReadSchedulerMap.containsKey(plcId)) {
                    continue; // 이미 생성됨
                }
                PlcConveyorReadScheduler scheduler = plcReadSchedulerFactory.create(cv);
                cvPlcReadSchedulerMap.put(plcId, scheduler);
                log.info("plcStatusReadSchedulerInit() - TspgPlcConveyorReadScheduler - " + plcId);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    public void orderSchedulerInit() {
        try {
            List<TbEqMst> eqMstList = repository.findAll(TbEqMst.class, "tb_eq_mst");
            List<TbEqRackMst> eqRackMst = repository.findAll(TbEqRackMst.class, "tb_eq_rack_mst");

            eqMstList.stream().filter(eq -> eq.getType() == EcsDBConsts.EqType.CONVEYOR.getValue())
                    .forEach(eq -> {
                                ConveyorOrderScheduler conveyorOrderScheduler = orderSchedulerFactory.createConveyorOrderScheduler(eq.getId());
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
                    StackerCraneOrderScheduler stackerCraneOrderScheduler = orderSchedulerFactory.createShuttleOrderScheduler(eqid, floor);
                    floorOrderSchedulerMap.put(eqid + floor, stackerCraneOrderScheduler);
                    log.info("orderSchedulerInit() - craneOrderScheduler - " + eqid + "," + floor);
                }
            });


        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    /*-------------------------------------------------------------------------------------*/

    public void plcStart() {
        try {
            cranePlcManager.startAll();
            cvPlcManager.startAll();
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    public void plcReadSchedulerStart() {
        cranePlcReadSchedulerStart();
        cvPlcReadSchedulerStart();
    }

    public void orderSchedulerStart() {
        floorOrderSchedulerStart();
        conveyorOrderSchedulerStart();
    }

    public void orderManagerSchedulerStart() {
        eqOrderScheduler.startScheduler(500);
        log.info("orderManagerSchedulerStart()");
    }

    /*-------------------------------------------------------------------------------------*/

    private void cranePlcReadSchedulerStart() {
        for (PlcCraneReadScheduler scheduler : shuttlePlcReadSchedulerMap.values()) {
            scheduler.startScheduler(25);
            log.info("cranePlcReadSchedulerStart() - scheduler - " + scheduler.getId());
        }
    }

    private void cvPlcReadSchedulerStart() {
        for (PlcConveyorReadScheduler scheduler : cvPlcReadSchedulerMap.values()) {
            scheduler.startScheduler(250);
            log.info("cvPlcReadSchedulerStart() - scheduler - " + scheduler.getId());
        }
    }

    /*-------------------------------------------------------------------------------------*/

    private void floorOrderSchedulerStart() {
        floorOrderSchedulerMap.forEach((key, value) -> {
            value.startScheduler(1000);
        });
    }
    /*-------------------------------------------------------------------------------------*/

    private void conveyorOrderSchedulerStart() {
        conveyorOrderSchedulerMap.forEach((key, value) -> {
            value.startScheduler(1000);
        });
    }

    /*-------------------------------------------------------------------------------------*/

    public void plcStop() {
        try {
            cranePlcManager.stopAll();
            cvPlcManager.stopAll();
        } catch (Exception e) {
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

    /*-------------------------------------------------------------------------------------*/

    public void orderManagerSchedulerStop() {
        eqOrderScheduler.stopScheduler();
    }

    private void shuttlePlcReadSchedulerStop() {
        for (PlcCraneReadScheduler scheduler : shuttlePlcReadSchedulerMap.values()) {
            scheduler.stopScheduler();
        }
    }

    private void cvPlcReadSchedulerStop() {
        for (PlcConveyorReadScheduler scheduler : cvPlcReadSchedulerMap.values()) {
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
