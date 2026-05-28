package operato.logis.ecs.tspg4way.service;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.Shuttle4WayReadConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.Tspg4WayShuttlePlc;
import operato.logis.connector.equipment.tspg.shuttle4way.service.Shuttle4WayReadMap;
import operato.logis.connector.plc.PlcBitEnum;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecParser;
import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.ecs.tspg4way.entity.TbEqCarMst;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class TspgShuttlePlcReadService{

    private IQueryManager iQueryManager = BeanUtil.get(IQueryManager.class);

    public void readShuttleMemory(Tspg4WayShuttlePlc tspg4WayShuttlePlc){
        if (!tspg4WayShuttlePlc.isReady()) {
            log.warn("[TspgShuttlePlcReadService][{}] Not ready. Skipping task.", tspg4WayShuttlePlc.getId());
            try {
                tspg4WayShuttlePlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else{
            // log.info("TspgShuttlePlcReadService - readShuttleMemory() " + tspg4WayShuttlePlc.getId());
            try {
                MelsecConsts.DeviceCode deviceCode = tspg4WayShuttlePlc.getReadDeviceCode();
                int firstDeviceCode = tspg4WayShuttlePlc.getReadFirstDeviceCode();
                String readStr = "";
                try {
                    //  log.info("TspgShuttlePlcReadService - readStr");
                    readStr = tspg4WayShuttlePlc.readWord(deviceCode, firstDeviceCode, 200);
                } catch (Exception ex) {
                    tspg4WayShuttlePlc.reStart();
                    throw new Exception(ex);
                }
                if (!readStr.isEmpty() && MelsecParser.isSuccessResponse(readStr)) {
                    List<Integer> wordValues = MelsecParser.parseWordValues(tspg4WayShuttlePlc.getPlcType(), readStr);
                    tspg4WayShuttlePlc.setReadValue(wordValues);
                } else {
                    log.warn("[TspgShuttlePlcReadService][{}] 응답 오류: {}", tspg4WayShuttlePlc.getId(), readStr);
                }
            } catch (Exception e) {
                log.error("[TspgShuttlePlcReadService][{}] Error in runTask {}", tspg4WayShuttlePlc.getId(), e);
            }
        }
    }
    public void readShuttleMemoryTest(Tspg4WayShuttlePlc tspg4WayShuttlePlc){
        log.info("TspgShuttlePlcReadService - readShuttleMemory() " + tspg4WayShuttlePlc.getId());
        try {
            MelsecConsts.DeviceCode deviceCode = tspg4WayShuttlePlc.getReadDeviceCode();
            int firstDeviceCode = tspg4WayShuttlePlc.getReadFirstDeviceCode();

            log.info("TspgShuttlePlcReadService - readStr");
            String readStr = "D00000FFFF030092010000020001000200EF0300000000010000000000000091010000EF030000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
            log.info("[TspgShuttlePlcReadService][{}] Read Result = {}", tspg4WayShuttlePlc.getId(), readStr);

            if (MelsecParser.isSuccessResponse(readStr)) {
                List<Integer> wordValues = MelsecParser.parseWordValues(tspg4WayShuttlePlc.getPlcType(), readStr);
                tspg4WayShuttlePlc.setReadValue(wordValues);
            } else {
                log.warn("[TspgShuttlePlcReadService][{}] 응답 오류: {}", tspg4WayShuttlePlc.getId(), readStr);
            }
        } catch (Exception e) {
            log.error("[TspgShuttlePlcReadService][{}] Error in runTask {}", tspg4WayShuttlePlc.getId(), e);
        }
    }


    public void logInfo(Tspg4WayShuttlePlc tspg4WayShuttlePlc) {
        logShuttleStatus(tspg4WayShuttlePlc.getReadMap());
    }


    public void logInfo(int firstDeviceCode, List<Integer> wordValues) {
        Shuttle4WayReadMap  shuttle4WayReadMap = new Shuttle4WayReadMap();
        shuttle4WayReadMap.setReadValues(firstDeviceCode, wordValues);
        logShuttleStatus(shuttle4WayReadMap);
    }

    // 로깅
    public void logShuttleStatus(Shuttle4WayReadMap readMap) {
        try {
            logWordStatus(readMap.getShuttleMode(), Shuttle4WayReadConsts.ShuttleMode.values(), "동작모드");
            logWordStatus(readMap.getShuttleWorkingStatus(), Shuttle4WayReadConsts.ShuttleWorkingStatus.values(), "작업유무");
            logWordStatus(readMap.getShuttleRunStatus(), Shuttle4WayReadConsts.ShuttleRunStatus.values(), "운전상태");
            log.info("[작업번호] {}", readMap.getShuttleWorkId());
            logWordStatus(readMap.getShuttleInterLock(), Shuttle4WayReadConsts.ShuttleInterLock.values(), "인터록");
            logWordStatus(readMap.getShuttleChargeStatus(), Shuttle4WayReadConsts.ShuttleChargeStatus.values(), "충전필요상태");
            log.info("[현재 층 위치] {}", readMap.getShuttleFloor());
            log.info("[주행위치] {}",  MelsecParser.convertTwoDecWordToInt( readMap.getShuttleLocation(), readMap.getShuttleLocation2()));
            log.info("[셀 위치] {}", readMap.getShuttleCellLocation());
            logWordStatus(readMap.getShuttleCompleteFlag(), Shuttle4WayReadConsts.ShuttleCompleteFlag.values(), "완료플래그");
            log.info("[완료작업번호] {}", readMap.getShuttleCompleteWorkId());
            log.info("[에러코드] {}", readMap.getShuttlErrorCode() == 0 ? "정상" : "이상, 이상 코드 : "+ readMap.getShuttlErrorCode());
            logWordStatus(readMap.getShuttleCargoStatus(), Shuttle4WayReadConsts.ShuttleCargoStatus.values(), "적재상태");
            logWordStatus(readMap.getShuttleCenserStatus(), Shuttle4WayReadConsts.ShuttleCenSerStatus.values(), "정보");


        }catch (Exception e){
            log.error("[TspgShuttlePlcReadService] Error {}", e);
        }

    }
    public static <E extends Enum<E> & PlcBitEnum> void logWordStatus(int wordValue, E[] enums, String prefix) {
        List<E> activeBits = Arrays.stream(enums)
                .filter(e -> e.isSet(wordValue))
                .collect(Collectors.toList());
        if (!activeBits.isEmpty()) {
            activeBits.forEach(e -> log.info("[{}] 1 {}", prefix, e.getDescription()));
        } else {
            Arrays.stream(enums)
                    .forEach(e -> log.info("[{}] 0 ({})", prefix, e.getDescription()));
        }
    }

    public void doSomething() {

    }

    public void updatePlcStatus(Tspg4WayShuttlePlc carPlc){
        Shuttle4WayReadMap readMap = carPlc.getReadMap();
        String eqId =  carPlc.getId();
        TbEqCarMst entityTbEqCarMst = findById(eqId);
        int level = readMap.getShuttleFloor();
        if(readMap.getShuttleCellLocation() == 0) return;
        int row = 0;
        int bay = 0;
        if (String.valueOf(readMap.getShuttleCellLocation()).length() == 3){
            row = Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(0, 1));
            if (Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(1, 2)) == 0)
                bay =  Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(2, 3));
            else
                bay = Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(1, 3));
        }else if (String.valueOf(readMap.getShuttleCellLocation()).length() == 4){
            row = Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(0, 2));
            if (Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(2, 3)) == 0)
                bay =  Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(3, 4));
            else
                bay = Integer.parseInt(String.valueOf(readMap.getShuttleCellLocation()).substring(2, 4));
        }

        boolean isAuto = Shuttle4WayReadConsts.ShuttleMode.AUTO.isSet(readMap.getShuttleMode());
        boolean isReady = Shuttle4WayReadConsts.ShuttleWorkStep.READY.isSet(readMap.getShuttleRunStatus());
        boolean isRun = Shuttle4WayReadConsts.ShuttleWorkStep.STARTED.isSet(readMap.getShuttleRunStatus());
        boolean isEmrStop = Shuttle4WayReadConsts.ShuttleWorkStep.NG.isSet(readMap.getShuttleRunStatus());
        boolean isCanWork = Shuttle4WayReadConsts.ShuttleChargeStatus.CAN_WORK.isSet(readMap.getShuttleChargeStatus());
        boolean isCharging = Shuttle4WayReadConsts.ShuttleChargeStatus.CHARGING.isSet(readMap.getShuttleChargeStatus());
        boolean isNeedCharge = Shuttle4WayReadConsts.ShuttleChargeStatus.NEED_CHARGE.isSet(readMap.getShuttleChargeStatus());
        boolean isComplete = Shuttle4WayReadConsts.ShuttleCompleteFlag.COMPLETE.isSet(readMap.getShuttleCompleteFlag());
        boolean hasCargo = Shuttle4WayReadConsts.ShuttleCargoStatus.CARGO.isSet(readMap.getShuttleCargoStatus());


        entityTbEqCarMst.setRow(row);
        entityTbEqCarMst.setBay(bay);
        entityTbEqCarMst.setLevel(level);
        entityTbEqCarMst.setRackId(level+"0"+ readMap.getShuttleCellLocation());
        entityTbEqCarMst.setAutoYn(isAuto);
        entityTbEqCarMst.setPlcCmdId(readMap.getShuttleWorkId());
        entityTbEqCarMst.setPlcCompCmdId(readMap.getShuttleCompleteWorkId());
        if(isReady) entityTbEqCarMst.setStatus(EcsDBConsts.EqCarStatus.READY.getValue());
        else if(isRun) entityTbEqCarMst.setStatus(EcsDBConsts.EqCarStatus.RUN.getValue());
        else if(isEmrStop) entityTbEqCarMst.setStatus(EcsDBConsts.EqCarStatus.EMR_STOP.getValue());

        entityTbEqCarMst.setCompleteYn(isComplete);

        if(isCanWork) entityTbEqCarMst.setBatteryStatus(EcsDBConsts.EqCarBatteryStatus.CAN_MOVE.getValue());
        else if(isCharging) entityTbEqCarMst.setBatteryStatus(EcsDBConsts.EqCarBatteryStatus.CHARGING.getValue());
        else if(isNeedCharge) entityTbEqCarMst.setBatteryStatus(EcsDBConsts.EqCarBatteryStatus.NEED_CHARGE.getValue());

        entityTbEqCarMst.setCargoYn(hasCargo);
        entityTbEqCarMst.setErrorId(String.valueOf(readMap.getShuttlErrorCode()));
        entityTbEqCarMst.setErrorDesc(Shuttle4WayReadConsts.ShuttleErrorCode.find(readMap.getShuttlErrorCode()).getDescription());

        // log.info("updatePlcStatus carMst : " + entityTbEqCarMst.getId());
        this.iQueryManager.update(entityTbEqCarMst);
    }

    public TbEqCarMst findById(String eqId){
        String sql = """
                SELECT *
                FROM tb_eq_car_mst
                WHERE 1=1
                AND eq_id = :eqId
                limit 1
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId", eqId);
        return this.iQueryManager.selectBySql(sql, params, TbEqCarMst.class);
    }
}
