package operato.logis.kmat_2026.biz.ecs.tspg4way.service;

import operato.logis.connector.equipment.tspg.shuttle4way.domain.enums.ConveyorReadConsts;
import operato.logis.connector.equipment.tspg.shuttle4way.domain.models.TspgConveyorPlc;
import operato.logis.connector.equipment.tspg.shuttle4way.service.ConveyorReadMap;
import operato.logis.connector.equipment.tspg.shuttle4way.service.ConveyorStatus;
import operato.logis.connector.plc.PlcBitEnum;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecParser;
import lombok.extern.slf4j.Slf4j;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqCvMst;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class TspgConveyorPlcReadService {

    private IQueryManager iQueryManager = BeanUtil.get(IQueryManager.class);

    public void readConveyorMemory(TspgConveyorPlc tspgConveyorPlc){
        if (!tspgConveyorPlc.isReady()) {
            log.warn("[TspgConveyorPlcReadService][{}] Not ready. Skipping task.", tspgConveyorPlc.getId());
            try {
                tspgConveyorPlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else{
            // log.info("TspgConveyorPlcReadService - readConveyorMemory() " + tspgConveyorPlc.getId());
            try {
                MelsecConsts.DeviceCode deviceCode = tspgConveyorPlc.getReadDeviceCode();
                int firstDeviceCode = tspgConveyorPlc.getReadFirstDeviceCode();
                String readStr = "";
                try {
                    readStr = tspgConveyorPlc.readWord(deviceCode, firstDeviceCode, 40);
                    // log.info("[TspgConveyorPlcReadService][{}] Read Result = {}", tspgConveyorPlc.getId(), readStr);
                } catch (Exception e) {
                    tspgConveyorPlc.reStart();
                    throw new Exception(e);
                }
                if (!readStr.isEmpty() && MelsecParser.isSuccessResponse(readStr)) {
                    List<Integer> wordValues = MelsecParser.parseWordValues(tspgConveyorPlc.getPlcType(), readStr);
                    tspgConveyorPlc.setReadValue(wordValues);
                } else {
                    log.warn("[TspgConveyorPlcReadService][{}] 응답 오류: {}", tspgConveyorPlc.getId(), readStr);
                }
            } catch (Exception e) {
                log.error("[TspgConveyorPlcReadService][{}] Error in runTask {}", tspgConveyorPlc.getId(), e);
            }
        }
    }

    public void readConveyorMemoryTest(TspgConveyorPlc tspgConveyorPlc){
        log.info("TspgConveyorPlcReadService - readConveyorMemory() " + tspgConveyorPlc.getId());
        try {

            String readStr = "D00000FFFF03005200000000000000000000000000E1020200000067000000000000000000000000000100000000000000000000000000000000000000010000000000000000000000000000000000000001000000000000000000";
            log.info("[TspgConveyorPlcReadService][{}] Read Result = {}", tspgConveyorPlc.getId(), readStr);

            if (MelsecParser.isSuccessResponse(readStr)) {
                List<Integer> wordValues = MelsecParser.parseWordValues(tspgConveyorPlc.getPlcType(), readStr);
                tspgConveyorPlc.setReadValue(wordValues);
            } else {
                log.warn("[TspgConveyorPlcReadService][{}] 응답 오류: {}", tspgConveyorPlc.getId(), readStr);
            }
        } catch (Exception e) {
            log.error("[TspgConveyorPlcReadService][{}] Error in runTask {}", tspgConveyorPlc.getId(), e);
        }
    }

    public void logInfo(TspgConveyorPlc tspgConveyorPlc) {
        logConveyorStatus(tspgConveyorPlc.getReadMap());
    }

    public void logInfo(int firstDeviceCode, List<Integer> wordValues) {
        ConveyorReadMap conveyorReadMap = new ConveyorReadMap();
        conveyorReadMap.setReadValues(firstDeviceCode, wordValues);
        logConveyorStatus(conveyorReadMap);
    }

    // 로깅
    public void logConveyorStatus(ConveyorReadMap readMap) {
        try {
            Map<String, ConveyorStatus> cvMap =  readMap.getConveyorMaps();

            log.info("==========================" + cvMap.size());

            cvMap.forEach((key, value) -> {
                log.info("==========================");
                log.info("[컨베이어 아이디] {}", key);
                boolean isStopperOpen= ConveyorReadConsts.ConveyorStatus.STOPPER_STATUS.isSet(value.getConveyorStatus());
                if(isStopperOpen)
                    log.info("[스토퍼 오픈]");
                logWordStatus(value.getConveyorStatus(), ConveyorReadConsts.ConveyorStatus.values(), "컨베이어 상태");
                logWordStatus(value.getSizeCheckErrorStatus(), ConveyorReadConsts.ConveyorStatus.values(), "사이즈 체커 에러 상태");
                log.info("[리프트 위치] {}", value.getCurrentLiftConveyorLevel());
                log.info("[컨베이어 에러코드] {}", value.getConveyorError());
            });
        }catch (Exception e){
            log.error("[TspgConveyorPlcReadService] Error {}", e);
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

    public void updatePlcStatus(TspgConveyorPlc cvPlc){
        try {
            Map<String, ConveyorStatus> cvMap =  cvPlc.getReadMap().getConveyorMaps();
            String eqId = cvPlc.getId();
            List<TbEqCvMst> cvMstList = findListById(eqId);
            // log.info("updatePlcStatus cvMstList size : " + cvMstList.size());
            cvMap.forEach((key, value) -> {
                TbEqCvMst cvMst = cvMstList.stream().filter(e -> e.getId().equals(key)).findFirst().orElse(null);
                if(cvMst != null){
                    boolean isAuto = ConveyorReadConsts.ConveyorStatus.TRACK_AUTO.isSet(value.getConveyorStatus());
                    boolean cargoYn = ConveyorReadConsts.ConveyorStatus.CARGO_STATUS.isSet(value.getConveyorStatus());
                    boolean isStopperOpen= ConveyorReadConsts.ConveyorStatus.STOPPER_STATUS.isSet(value.getConveyorStatus());
                    boolean isRun = ConveyorReadConsts.ConveyorStatus.MOTOR_RUN.isSet(value.getConveyorStatus());
                    cvMst.setAutoYn(isAuto);
                    cvMst.setCargoYn(cargoYn);
                    cvMst.setStopperOpenYn(isStopperOpen);
                    cvMst.setRunYn(isRun);
                    cvMst.setErrorId(String.valueOf(value.getConveyorError()));
                    cvMst.setErrorDesc(ConveyorReadConsts.ConveyorErrorCode.find(value.getConveyorError()).getDescription());
                    if(cvMst.getType() == EcsDBConsts.ConveyorType.LIFT.getValue())
                        cvMst.setLevel(value.getCurrentLiftConveyorLevel());
                    // log.info("updatePlcStatus cvMst : " + cvMst.getId());
                    this.iQueryManager.update(cvMst);
                }
            });
        }catch (Exception e){
            log.error(e.toString());
        }
    }

    public List<TbEqCvMst> findListById(String eqId){
        String sql = """
                SELECT *
                FROM tb_eq_cv_mst
                WHERE 1=1
                AND eq_id = :eqId
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId", eqId);
        return this.iQueryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }

}
