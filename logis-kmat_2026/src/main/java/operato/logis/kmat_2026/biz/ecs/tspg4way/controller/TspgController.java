package operato.logis.kmat_2026.biz.ecs.tspg4way.controller;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.ConveyorWriteConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayWriteConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecParser;
import operato.logis.kmat_2026.biz.ecs.tspg4way.core.CoreManager;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEcsRackOrder;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgConveyorPlcRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttleMapRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.registry.TspgShuttlePlcRegistry;
import operato.logis.kmat_2026.biz.ecs.tspg4way.service.TspgConveyorPlcReadService;
import operato.logis.kmat_2026.biz.ecs.tspg4way.service.TspgShuttlePlcReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.context.DomainContext;

@RestController
@Slf4j
@RequestMapping("/api/tspg/test")
@RequiredArgsConstructor
public class TspgController {

    private final CoreManager coreManager;
    private final TspgShuttlePlcRegistry tspgShuttlePlcRegistry;
    private final TspgConveyorPlcRegistry tspgConveyorPlcRegistry;
    private final TspgShuttleMapRegistry tspgShuttleMapRegistry;
    @GetMapping("/boot")
    public ResponseEntity<?> boot() {
        try {
           log.info("=================boot=================");
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/init/map")
    public ResponseEntity<?> initMap() {
        try {
            coreManager.mapInit();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/init/plc")
    public ResponseEntity<?> initPlc() {
        try {
            coreManager.plcInit();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/start/plc")
    public ResponseEntity<?> startPlc() {
        try {
            coreManager.plcStart();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/stop/plc")
    public ResponseEntity<?> stopPlc() {
        try {
            coreManager.plcStop();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/init/plc-read-scheduler")
    public ResponseEntity<?> initPlcReadScheduler() {
        try {
            coreManager.orderSchedulerInit();
            coreManager.plcStatusReadSchedulerInit();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/start/plc-read-scheduler")
    public ResponseEntity<?> startPlcReadScheduler() {
        try {
            coreManager.plcReadSchedulerStart();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/stop/plc-read-scheduler")
    public ResponseEntity<?> stopPlcReadScheduler() {
        try {
            coreManager.plcReadSchedulerStop();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @GetMapping("/start/cv-order-manager-scheduler")
    public ResponseEntity<?> startEqCvOrderManagerScheduler() {
        try {
            coreManager.orderSchedulerStart();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/stop/cv-order-manager-scheduler")
    public ResponseEntity<?> stopEqCvOrderManagerScheduler() {
        try {
            coreManager.OrderSchedulerStop();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @GetMapping("/start/order-manager-scheduler")
    public ResponseEntity<?> startEqOrderManagerScheduler() {
        try {
            coreManager.orderManagerSchedulerStart();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/stop/order-manager-scheduler")
    public ResponseEntity<?> stopEqOrderManagerScheduler() {
        try {
            coreManager.orderManagerSchedulerStop();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * TSPG 셔틀 read
     */
    @PostMapping("/shuttle/read/command")
    public ResponseEntity<?> readCommand(
                @RequestBody String shuttleCarId
                ) {
        try {
            Tspg4WayShuttlePlc shuttleCarPlc = tspgShuttlePlcRegistry.getEquipment(shuttleCarId);
            log.info("shuttleCarPlc : {}", shuttleCarPlc);
            TspgShuttlePlcReadService tspgShuttlePlcReadService = new TspgShuttlePlcReadService();
            log.info("tspgShuttlePlcReadService : {}", tspgShuttlePlcReadService);
            tspgShuttlePlcReadService.readShuttleMemory(shuttleCarPlc);
            log.info("tspgShuttlePlcReadService : {}", tspgShuttlePlcReadService);
            tspgShuttlePlcReadService.logInfo(shuttleCarPlc);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * TSPG 셔틀 이동정보 write test api
     *
     * @param shuttleCarId // plcId
     * @param workId        작업번호(1051, "1001~9999"),
     * @param workType      작업유형(1052, ""),
     *                            // bit
     *                            // MOVE(0, "이동"),
     *                            // LOAD(1, "로드"),
     *                            // UNLOAD(2, "언로드"),
     * @param toCellId      도착위치(1055, "0101, 0201, 0302, ...")
     * @param floor         주행 층
     */
    @PostMapping("/shuttle/write/command")
    public int[] writeCommand(
            @RequestParam String shuttleCarId,
            @RequestParam int workId,
            @RequestParam int workType,
            @RequestParam int resetOption,
            @RequestParam int toCellId,
            @RequestParam int floor
    ) {
        int workTypeWord = 1 << workType;
        int resetOptionWord = 1 << resetOption;
        var command = new int[]{workId, workTypeWord, 0,  resetOptionWord, toCellId, floor};
        try {
            Tspg4WayShuttlePlc shuttle = tspgShuttlePlcRegistry.getEquipment(shuttleCarId);
            var res = shuttle.writeWord(MelsecConsts.DeviceCode.R, Shuttle4WayWriteConsts.ShuttleWriteAddress.WORK_ID.getAddress(), command);

            var isResSuccess =  MelsecParser.isSuccessResponse(res);
            if(!isResSuccess){
                log.info(res);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return command;
    }


    /**
     * TSPG 셔틀 상태 리셋 write test api
     * @param shuttleCarId // plcId
     * @param resetOption // 리셋 BIT
                            (0, "완료리셋"),
                            (1, "알람/에러리셋")
     */
    @PostMapping("/shuttle/write/command/reset")
    public int[] writeCommandReset(
            @RequestParam String shuttleCarId,
            @RequestParam int resetOption
    ) {
        int resetOptionWord = 1 << resetOption;
        var command = new int[]{resetOptionWord};
        try {
            Tspg4WayShuttlePlc shuttle = tspgShuttlePlcRegistry.getEquipment(shuttleCarId);

            //   new int[]{workId, destRow, destBay, destLayer}
            //   new int [] {MelsecParser.buildWordFromBits(ConveyorWriteConsts.ConveyorWorkStatus.DEST_UPDATE_COMPLETE.getBitIndex())}
            var res = shuttle.writeWord(MelsecConsts.DeviceCode.R, Shuttle4WayWriteConsts.ShuttleWriteAddress.CLEAR_OPTION.getAddress(), command);

            var isResSuccess =  MelsecParser.isSuccessResponse(res);
            if(!isResSuccess){
                log.info(res);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return command;
    }



    /**
     * TSPG 컨베이어 read
     */
    @PostMapping("/conveyor/read/command")
    public ResponseEntity<?> conveyorReadCommand(
            @RequestParam String cvPlcId
    ) {
        try {
            TspgConveyorPlc cvPlc = tspgConveyorPlcRegistry.getEquipment(cvPlcId);
            TspgConveyorPlcReadService cvReadService = new TspgConveyorPlcReadService();
            cvReadService.readConveyorMemory(cvPlc);
            cvReadService.logInfo(cvPlc);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * TSPG 컨베이어 이동정보 write test api
     *
     * @param workId        작업번호(0, "1001~9999"),
     * @param workType      COMMAND(1, ""),
     *                            // bit
     *                            // (1, "입고"),
     *                            // (2, "출고"),
     *                            // (3, "셔틀카 이동"),
     * @param fromCvId      출발위치(2, "0101, 0201, 0302, ...")
     * @param toCvId        도착위치(3, "0101, 0201, 0302, ...")
     * @param sizeChecker   size checker(4, "1:기본(1,2단) 2:소(3단)")
     */
    @PostMapping("/conveyor/write/command")
    public int[] cvWriteCommand(
            @RequestParam String cvPlcId,
            @RequestParam int cvId,
            @RequestParam int workId,
            @RequestParam int workType,
            @RequestParam int fromCvId,
            @RequestParam int toCvId,
            @RequestParam int sizeChecker
    ) throws Exception {
        TspgConveyorPlc cvPlc = tspgConveyorPlcRegistry.getEquipment(cvPlcId);
        var command1 = new int[]{0,0};
        if(workType == 2){
            var modeWord = 1 << 1;
            command1 = new int[]{modeWord, 0};
        }else{
            command1 = new int[]{0, 0};
        }
        int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
        var res1 = cvPlc.writeWord(MelsecConsts.DeviceCode.R, firstDeviceCode, command1);
        var isResSuccess1 =  MelsecParser.isSuccessResponse(res1);
        if(!isResSuccess1)
            log.info(res1);
        else
            log.info("CV on/off write success");


        int workTypeWord = 1 << workType;
        var command2 = new int[]{workId, workTypeWord, fromCvId, toCvId, sizeChecker};
        int cvIdFirstCode = cvPlc.getWriteFirstDeviceCode(cvId);
        var res2 = cvPlc.writeWord(MelsecConsts.DeviceCode.R, cvIdFirstCode, command2);

        var isResSuccess2 =  MelsecParser.isSuccessResponse(res2);
        if(!isResSuccess2)
            log.info(res2);
        else
            log.info("CV move write success");

        return command2;

    }

    @PostMapping("/conveyor/lift/write/command")
    public int[] cvLiftWriteCommand(
            @RequestParam String cvPlcId,
            @RequestParam int mode,
            @RequestParam int toLevel
    ) {

        int modeWord = 1 << mode;
        var command = new int[]{modeWord, toLevel};
        try {
            TspgConveyorPlc cvPlc = tspgConveyorPlcRegistry.getEquipment(cvPlcId);
            int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
            var res = cvPlc.writeWord(MelsecConsts.DeviceCode.R, firstDeviceCode, command);

            var isResSuccess =  MelsecParser.isSuccessResponse(res);
            if(!isResSuccess){
                log.info(res);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return command;
    }

    @PostMapping("/conveyor/service/move")
    public ResponseEntity<?>  shuttleMoveService(
            @RequestParam String shuttleCarId,
            @RequestParam int orderType,
            @RequestParam String fromCellId,
            @RequestParam String toCellId
    ) {
        try {
            var type = EcsDBConsts.OrderType.find(orderType);
            var order = TbEcsRackOrder.test(type, fromCellId, toCellId);
            Tspg4WayShuttlePlc shuttle = tspgShuttlePlcRegistry.getEquipment(shuttleCarId);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

}


