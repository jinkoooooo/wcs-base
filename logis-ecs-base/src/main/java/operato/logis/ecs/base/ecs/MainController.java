package operato.logis.ecs.base.ecs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.ConveyorWriteConsts;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecParser;
import operato.logis.ecs.base.ecs.core.CoreManager;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.domain.enums.StackerCraneWriteConsts;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorPlcManager;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneMapManager;
import operato.logis.ecs.base.ecs.plc.crane.StackerCranePlcManager;
import operato.logis.ecs.base.ecs.service.StackerCranePlcReadService;
import operato.logis.ecs.base.ecs.service.conveyor.ConveyorPlcReadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// todo: 로직검토
@RestController
@Slf4j
@RequestMapping("/api/movex/test")
@RequiredArgsConstructor
public class MainController {

    private final CoreManager coreManager;
    private final StackerCranePlcManager stackerCranePlcManager;
    private final ConveyorPlcManager conveyorPlcManager;
    private final StackerCraneMapManager stackerCraneMapManager;

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

    /** 스태커 크레인 read */
    @PostMapping("/crane/read/command")
    public ResponseEntity<?> readCommand(@RequestBody String craneId) {
        try {
            StackerCranePlc cranePlc = stackerCranePlcManager.getEquipment(craneId);
            log.info("StackerCranePlc : {}", cranePlc);
            StackerCranePlcReadService stackerCranePlcReadService = new StackerCranePlcReadService();
            log.info("StackerCranePlcReadService : {}", stackerCranePlcReadService);
            stackerCranePlcReadService.logInfo(cranePlc);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * MOVEX 스태커 크레인 이동정보 write test api
     *
     * @param craneId  // plcId
     * @param workId   작업번호(1051, "1001~9999"),
     * @param workType 작업유형(1052, ""),
     *                 // bit
     *                 // MOVE(0, "이동"),
     *                 // LOAD(1, "로드"),
     *                 // UNLOAD(2, "언로드"),
     * @param toCellId 도착위치(1055, "0101, 0201, 0302, ...")
     * @param floor    주행 층
     */
    @PostMapping("/crane/write/command")
    public int[] writeCommand(
            @RequestParam String craneId,
            @RequestParam int workId,
            @RequestParam int workType,
            @RequestParam int resetOption,
            @RequestParam int toCellId,
            @RequestParam int floor
    ) {
        int workTypeWord = 1 << workType;
        int resetOptionWord = 1 << resetOption;
        var command = new int[]{ workId, workTypeWord, 0, resetOptionWord, toCellId, floor };
        try {
            StackerCranePlc crane = stackerCranePlcManager.getEquipment(craneId);
            var res = crane.writeWord(MelsecConsts.DeviceCode.R, StackerCraneWriteConsts.StackerCraneWriteAddress.WORK_ID.getAddress(), command);

            var isResSuccess = MelsecParser.isSuccessResponse(res);
            if (!isResSuccess) {
                log.info(res);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return command;
    }


    /**
     * MOVEX 스태커 크레인 상태 리셋 write test api
     *
     * @param craneId     // plcId
     * @param resetOption // 리셋 BIT
     *                    (0, "완료리셋"),
     *                    (1, "알람/에러리셋")
     */
    @PostMapping("/crane/write/command/reset")
    public int[] writeCommandReset(
            @RequestParam String craneId,
            @RequestParam int resetOption
    ) {
        int resetOptionWord = 1 << resetOption;
        var command = new int[]{ resetOptionWord };
        try {
            StackerCranePlc crane = stackerCranePlcManager.getEquipment(craneId);

            //   new int[]{workId, destRow, destBay, destLayer}
            //   new int [] {MelsecParser.buildWordFromBits(ConveyorWriteConsts.ConveyorWorkStatus.DEST_UPDATE_COMPLETE.getBitIndex())}
            var res = crane.writeWord(MelsecConsts.DeviceCode.R, StackerCraneWriteConsts.StackerCraneWriteAddress.CLEAR_OPTION.getAddress(), command);

            var isResSuccess = MelsecParser.isSuccessResponse(res);
            if (!isResSuccess) {
                log.info(res);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return command;
    }


    /** 컨베이어 read */
    @PostMapping("/conveyor/read/command")
    public ResponseEntity<?> conveyorReadCommand(
            @RequestParam String cvPlcId
    ) {
        try {
            ConveyorPlc cvPlc = conveyorPlcManager.getEquipment(cvPlcId);
            ConveyorPlcReadService cvReadService = new ConveyorPlcReadService();
            cvReadService.readConveyorMemory(cvPlc);
            cvReadService.logInfo(cvPlc);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * MOVEX 컨베이어 이동정보 write test api
     *
     * @param workId      작업번호(0, "1001~9999"),
     * @param workType    COMMAND(1, ""),
     *                    // bit
     *                    // (1, "입고"),
     *                    // (2, "출고"),
     *                    // (3, "셔틀카 이동"),
     * @param fromCvId    출발위치(2, "0101, 0201, 0302, ...")
     * @param toCvId      도착위치(3, "0101, 0201, 0302, ...")
     * @param sizeChecker size checker(4, "1:기본(1,2단) 2:소(3단)")
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
        ConveyorPlc cvPlc = conveyorPlcManager.getEquipment(cvPlcId);
        var command1 = new int[]{ 0, 0 };
        if (workType == 2) {
            var modeWord = 1 << 1;
            command1 = new int[]{ modeWord, 0 };
        } else {
            command1 = new int[]{ 0, 0 };
        }
        int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
        var res1 = cvPlc.writeWord(MelsecConsts.DeviceCode.R, firstDeviceCode, command1);
        var isResSuccess1 = MelsecParser.isSuccessResponse(res1);
        if (!isResSuccess1)
            log.info(res1);
        else
            log.info("CV on/off write success");


        int workTypeWord = 1 << workType;
        var command2 = new int[]{ workId, workTypeWord, fromCvId, toCvId, sizeChecker };
        int cvIdFirstCode = cvPlc.getWriteFirstDeviceCode(cvId);
        var res2 = cvPlc.writeWord(MelsecConsts.DeviceCode.R, cvIdFirstCode, command2);

        var isResSuccess2 = MelsecParser.isSuccessResponse(res2);
        if (!isResSuccess2)
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
        var command = new int[]{ modeWord, toLevel };
        try {
            ConveyorPlc cvPlc = conveyorPlcManager.getEquipment(cvPlcId);
            int firstDeviceCode = ConveyorWriteConsts.ConveyorCommonWriteAddress.LIFT_CAR_MOVE_MODE.getAddress();
            var res = cvPlc.writeWord(MelsecConsts.DeviceCode.R, firstDeviceCode, command);

            var isResSuccess = MelsecParser.isSuccessResponse(res);
            if (!isResSuccess) {
                log.info(res);
            }
        } catch (Exception e) {
            log.error(e.toString());
        }
        return command;
    }

    @PostMapping("/conveyor/service/move")
    public ResponseEntity<?> craneMoveService(
            @RequestParam String craneId,
            @RequestParam int orderType,
            @RequestParam String fromCellId,
            @RequestParam String toCellId
    ) {
        try {
            var type = EcsDBConsts.OrderType.find(orderType);
            // var order = TbEcsRackOrder.test(type, fromCellId, toCellId);
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/crane/service/path-reset")
    public ResponseEntity<?> cranePathReset(
            @RequestParam String craneId
    ) {
        try {
            StackerCranePlc cranePlc = stackerCranePlcManager.getEquipment(craneId);
            cranePlc.getCrane().initPathCmdList();
        } catch (Exception e) {
            log.error(e.toString());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}


