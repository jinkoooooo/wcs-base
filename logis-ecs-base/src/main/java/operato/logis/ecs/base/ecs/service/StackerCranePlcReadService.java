package operato.logis.ecs.base.ecs.service;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.plc.PlcBitEnum;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecParser;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.domain.enums.StackerCraneReadConsts;
import operato.logis.ecs.base.ecs.entity.TbEqCraneMst;
import operato.logis.ecs.base.ecs.equipment.StackerCranePlc;
import operato.logis.ecs.base.ecs.plc.crane.StackerCraneReadMap;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//TODO: StackerCraneReadConsts, StackerCraneReadMap, StackerCranePlc 변경
@Slf4j
public class StackerCranePlcReadService {

    private IQueryManager iQueryManager = BeanUtil.get(IQueryManager.class);

    public void readCraneMemory(StackerCranePlc cranePlc) {
        if (!cranePlc.isReady()) {
            log.warn("[MovexCranePlcReadService][{}] Not ready. Skipping task.", cranePlc.getId());
            try {
                cranePlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            // log.info("MovexCranePlcReadService - readCraneMemory() " + cranePlc.getId());
            try {
                MelsecConsts.DeviceCode deviceCode = cranePlc.getReadDeviceCode();
                int firstDeviceCode = cranePlc.getReadFirstDeviceCode();
                String readStr = "";
                try {
                    //  log.info("MovexCranePlcReadService - readStr");
                    readStr = cranePlc.readWord(deviceCode, firstDeviceCode, 200);
                } catch (Exception ex) {
                    cranePlc.reStart();
                    throw new Exception(ex);
                }
                if (!readStr.isEmpty() && MelsecParser.isSuccessResponse(readStr)) {
                    List<Integer> wordValues = MelsecParser.parseWordValues(cranePlc.getPlcType(), readStr);
                    cranePlc.setReadValue(wordValues);
                } else {
                    log.warn("[MovexCranePlcReadService][{}] 응답 오류: {}", cranePlc.getId(), readStr);
                }
            } catch (Exception e) {
                log.error("[MovexCranePlcReadService][{}] Error in runTask {}", cranePlc.getId(), e);
            }
        }
    }

    public void readCraneMemoryTest(StackerCranePlc cranePlc) {
        log.info("MovexCranePlcReadService - readCraneMemory() " + cranePlc.getId());
        try {
            MelsecConsts.DeviceCode deviceCode = cranePlc.getReadDeviceCode();
            int firstDeviceCode = cranePlc.getReadFirstDeviceCode();

            log.info("MovexCranePlcReadService - readStr");
            String readStr = "D00000FFFF030092010000020001000200EF0300000000010000000000000091010000EF030000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
            log.info("[MovexCranePlcReadService][{}] Read Result = {}", cranePlc.getId(), readStr);

            if (MelsecParser.isSuccessResponse(readStr)) {
                List<Integer> wordValues = MelsecParser.parseWordValues(cranePlc.getPlcType(), readStr);
                cranePlc.setReadValue(wordValues);
            } else {
                log.warn("[MovexCranePlcReadService][{}] 응답 오류: {}", cranePlc.getId(), readStr);
            }
        } catch (Exception e) {
            log.error("[MovexCranePlcReadService][{}] Error in runTask {}", cranePlc.getId(), e);
        }
    }

    public void logInfo(StackerCranePlc cranePlc) {
        logCraneStatus(cranePlc.getReadMap());
    }

    public void logInfo(int firstDeviceCode, List<Integer> wordValues) {
        StackerCraneReadMap craneReadMap = new StackerCraneReadMap();
        craneReadMap.setReadValues(firstDeviceCode, wordValues);
        logCraneStatus(craneReadMap);
    }

    // 로깅
    // todo: PLC MAP 참고 후 변경
    public void logCraneStatus(StackerCraneReadMap readMap) {
        try {
            // todo: 매핑
            logWordStatus(readMap.getCraneMode(), StackerCraneReadConsts.StackerCraneMode.values(), "동작모드");
            //logWordStatus(readMap.getCraneWorkingStatus(), StackerCraneReadConsts.StackerCraneWorkingStatus.values(), "작업유무");
            //logWordStatus(readMap.getCraneRunStatus(), StackerCraneReadConsts.StackerCraneRunStatus.values(), "운전상태");
            log.info("[작업번호] {}", readMap.getCraneWorkId());
            //logWordStatus(readMap.getCraneInterLock(), StackerCraneReadConsts.StackerCraneInterLock.values(), "인터록");
            //logWordStatus(readMap.getCraneChargeStatus(), StackerCraneReadConsts.StackerCraneChargeStatus.values(), "충전필요상태");
            log.info("[현재 층 위치] {}", readMap.getCraneFloor());
            log.info("[주행위치] {}", MelsecParser.convertTwoDecWordToInt(readMap.getCraneLocation(), readMap.getCraneLocation2()));
            log.info("[셀 위치] {}", readMap.getCraneCellLocation());
            //logWordStatus(readMap.getCraneCompleteFlag(), StackerCraneReadConsts.StackerCraneCompleteFlag.values(), "완료플래그");
            log.info("[완료작업번호] {}", readMap.getCraneCompleteWorkId());
            log.info("[에러코드] {}", readMap.getCraneErrorCode() == 0 ? "정상" : "이상, 이상 코드 : " + readMap.getCraneErrorCode());
            logWordStatus(readMap.getCraneCargoStatus(), StackerCraneReadConsts.StackerCraneCargoStatus.values(), "적재상태");
            //logWordStatus(readMap.getCraneCenserStatus(), StackerCraneReadConsts.StackerCraneCenSerStatus.values(), "정보");
        } catch (Exception e) {
            log.error("[MovexCranePlcReadService] Error {}", e);
        }
    }

    public static <E extends Enum<E> & PlcBitEnum> void logWordStatus(int wordValue, E[] enums, String prefix) {
        List<E> activeBits = Arrays.stream(enums)
                .filter(e -> e.isSet(wordValue))
                .collect(Collectors.toList());
        if (!activeBits.isEmpty()) {
            activeBits.forEach(e -> log.info("[{}] 1 {}", prefix, e.getDescription()));
        } else {
            Arrays.stream(enums).forEach(e -> log.info("[{}] 0 ({})", prefix, e.getDescription()));
        }
    }

    // TODO: PLC MAP 참고 후, 로직 수정
    public void updatePlcStatus(StackerCranePlc carPlc) {
        StackerCraneReadMap readMap = carPlc.getReadMap();
        String eqId = carPlc.getId();
        TbEqCraneMst entityTbEqCraneMst = findById(eqId);
        int level = readMap.getCraneFloor();
        if (readMap.getCraneCellLocation() == 0) return;
        int row = 0;
        int bay = 0;
        if (String.valueOf(readMap.getCraneCellLocation()).length() == 3) {
            row = Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(0, 1));
            if (Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(1, 2)) == 0)
                bay = Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(2, 3));
            else
                bay = Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(1, 3));
        } else if (String.valueOf(readMap.getCraneCellLocation()).length() == 4) {
            row = Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(0, 2));
            if (Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(2, 3)) == 0)
                bay = Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(3, 4));
            else
                bay = Integer.parseInt(String.valueOf(readMap.getCraneCellLocation()).substring(2, 4));
        }

        //todo: mapping 변경
        boolean isAuto = true; // StackerCraneReadConsts.StackerCraneMode.AUTO.isSet(readMap.getCraneMode());
        boolean isReady = true; // StackerCraneReadConsts.StackerCraneWorkStep.READY.isSet(readMap.getCraneRunStatus());
        boolean isRun = true; // StackerCraneReadConsts.StackerCraneWorkStep.STARTED.isSet(readMap.getCraneRunStatus());
        boolean isEmrStop = false; // StackerCraneReadConsts.StackerCraneWorkStep.NG.isSet(readMap.getCraneRunStatus());
        boolean isCanWork = true; // StackerCraneReadConsts.StackerCraneChargeStatus.CAN_WORK.isSet(readMap.getCraneChargeStatus());
        boolean isComplete = true; // StackerCraneReadConsts.StackerCraneCompleteFlag.COMPLETE.isSet(readMap.getCraneCompleteFlag());
        boolean hasCargo = false; // StackerCraneReadConsts.StackerCraneCargoStatus.CARGO.isSet(readMap.getCraneCargoStatus());

        entityTbEqCraneMst.setAsiel(row); // todo: setAsiel
        entityTbEqCraneMst.setBay(bay);
        entityTbEqCraneMst.setLevel(level);
        entityTbEqCraneMst.setRackId(level + "0" + readMap.getCraneCellLocation());
        entityTbEqCraneMst.setAutoYn(isAuto);
        entityTbEqCraneMst.setPlcCmdId(readMap.getCraneWorkId());
        entityTbEqCraneMst.setPlcCompCmdId(readMap.getCraneCompleteWorkId());
        if (isReady) entityTbEqCraneMst.setStatus(EcsDBConsts.EqCraneStatus.READY.getValue());
        else if (isRun) entityTbEqCraneMst.setStatus(EcsDBConsts.EqCraneStatus.RUN.getValue());
        else if (isEmrStop) entityTbEqCraneMst.setStatus(EcsDBConsts.EqCraneStatus.EMR_STOP.getValue());

        entityTbEqCraneMst.setCompleteYn(isComplete);

        //if (isCanWork) entityTbEqCraneMst.setBatteryStatus(EcsDBConsts.EqCarBatteryStatus.CAN_MOVE.getValue());
        //else if (isCharging) entityTbEqCraneMst.setBatteryStatus(EcsDBConsts.EqCarBatteryStatus.CHARGING.getValue());
        //else if (isNeedCharge) entityTbEqCraneMst.setBatteryStatus(EcsDBConsts.EqCarBatteryStatus.NEED_CHARGE.getValue());

        entityTbEqCraneMst.setCargoYn(hasCargo);
        entityTbEqCraneMst.setErrorId(String.valueOf(readMap.getCraneErrorCode()));
        entityTbEqCraneMst.setErrorDesc(StackerCraneReadConsts.StackerCraneErrorCode.find(readMap.getCraneErrorCode()).getDescription());

        // log.info("updatePlcStatus craneMst : " + entityTbEqCraneMst.getId());
        this.iQueryManager.update(entityTbEqCraneMst);
    }

    public TbEqCraneMst findById(String eqId) {
        String sql = """
                SELECT * 
                FROM tb_eq_crane_mst 
                WHERE eq_id = :eqId 
                LIMIT 1;
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId", eqId);
        return this.iQueryManager.selectBySql(sql, params, TbEqCraneMst.class);
    }

    public void doSomething() { }
}
