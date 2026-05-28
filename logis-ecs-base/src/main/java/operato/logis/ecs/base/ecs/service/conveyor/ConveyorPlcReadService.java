package operato.logis.ecs.base.ecs.service.conveyor;

import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.plc.PlcBitEnum;
import operato.logis.connector.plc.melsec.MelsecConsts;
import operato.logis.connector.plc.melsec.MelsecParser;
import operato.logis.ecs.base.ecs.domain.conveyor.ConveyorStatus;
import operato.logis.ecs.base.ecs.domain.enums.ConveyorReadConsts;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEqCvMst;
import operato.logis.ecs.base.ecs.equipment.ConveyorPlc;
import operato.logis.ecs.base.ecs.plc.conveyor.ConveyorReadMap;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ConveyorPlcReadService {

    private IQueryManager iQueryManager = BeanUtil.get(IQueryManager.class);

    public void readConveyorMemory(ConveyorPlc conveyorPlc) {
        if (!conveyorPlc.isReady()) {
            log.warn("[ConveyorPlcReadService][{}] Not ready. Skipping task.", conveyorPlc.getId());
            try {
                conveyorPlc.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            // log.info("ConveyorPlcReadService - readConveyorMemory() " + conveyorPlc.getId());
            try {
                MelsecConsts.DeviceCode deviceCode = conveyorPlc.getReadDeviceCode();
                int firstDeviceCode = conveyorPlc.getReadFirstDeviceCode();
                String readStr = "";
                try {
                    readStr = conveyorPlc.readWord(deviceCode, firstDeviceCode, 40);
                    // log.info("[ConveyorPlcReadService][{}] Read Result = {}", conveyorPlc.getId(), readStr);
                } catch (Exception e) {
                    conveyorPlc.reStart();
                    throw new Exception(e);
                }
                if (!readStr.isEmpty() && MelsecParser.isSuccessResponse(readStr)) {
                    List<Integer> wordValues = MelsecParser.parseWordValues(conveyorPlc.getPlcType(), readStr);
                    conveyorPlc.setReadValue(wordValues);
                } else {
                    log.warn("[ConveyorPlcReadService][{}] 응답 오류: {}", conveyorPlc.getId(), readStr);
                }
            } catch (Exception e) {
                log.error("[ConveyorPlcReadService][{}] Error in runTask {}", conveyorPlc.getId(), e);
            }
        }
    }

    public void readConveyorMemoryTest(ConveyorPlc conveyorPlc) {
        log.info("ConveyorPlcReadService - readConveyorMemory() " + conveyorPlc.getId());
        try {

            String readStr = "D00000FFFF03005200000000000000000000000000E1020200000067000000000000000000000000000100000000000000000000000000000000000000010000000000000000000000000000000000000001000000000000000000";
            log.info("[ConveyorPlcReadService][{}] Read Result = {}", conveyorPlc.getId(), readStr);

            if (MelsecParser.isSuccessResponse(readStr)) {
                List<Integer> wordValues = MelsecParser.parseWordValues(conveyorPlc.getPlcType(), readStr);
                conveyorPlc.setReadValue(wordValues);
            } else {
                log.warn("[ConveyorPlcReadService][{}] 응답 오류: {}", conveyorPlc.getId(), readStr);
            }
        } catch (Exception e) {
            log.error("[ConveyorPlcReadService][{}] Error in runTask {}", conveyorPlc.getId(), e);
        }
    }

    public void logInfo(ConveyorPlc conveyorPlc) {
        logConveyorStatus(conveyorPlc.getReadMap());
    }

    public void logInfo(int firstDeviceCode, List<Integer> wordValues) {
        ConveyorReadMap conveyorReadMap = new ConveyorReadMap();
        conveyorReadMap.setReadValues(firstDeviceCode, wordValues);
        logConveyorStatus(conveyorReadMap);
    }

    // 로깅
    public void logConveyorStatus(ConveyorReadMap readMap) {
        try {
            Map<Integer, ConveyorStatus> cvMap = readMap.getConveyorMaps(); // todo: string -> integer 영향 검토

            log.info("==========================" + cvMap.size());

            cvMap.forEach((key, value) -> {
                log.info("==========================");
                log.info("[컨베이어 아이디] {}", key);
                boolean isStopperOpen = false; // todo: 기존 로직 검토. ConveyorReadConsts.ConveyorStatus.STOPPER_STATUS.isSet(value.getConveyorStatus());
                if (isStopperOpen)
                    log.info("[스토퍼 오픈]");
                logWordStatus(value.getConveyorStatus(), ConveyorReadConsts.ConveyorStatus.values(), "컨베이어 상태");
                logWordStatus(value.getSizeCheckErrorStatus(), ConveyorReadConsts.ConveyorStatus.values(), "사이즈 체커 에러 상태");
                log.info("[리프트 위치] {}", value.getCurrentLiftConveyorLevel());
                log.info("[컨베이어 에러코드] {}", value.getConveyorError());
            });
        } catch (Exception e) {
            log.error("[ConveyorPlcReadService] Error {}", e);
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

    public void updatePlcStatus(ConveyorPlc cvPlc) {
        try {
            Map<Integer, ConveyorStatus> cvMap = cvPlc.getReadMap().getConveyorMaps(); // todo: string -> integer 영향 검토
            String eqId = cvPlc.getId();
            List<TbEqCvMst> cvMstList = findListById(eqId);
            // log.info("updatePlcStatus cvMstList size : " + cvMstList.size());
            cvMap.forEach((key, value) -> {
                TbEqCvMst cvMst = cvMstList.stream().filter(e -> e.getId().equals(key)).findFirst().orElse(null);
                if (cvMst != null) {
                    boolean isAuto = ConveyorReadConsts.ConveyorStatus.TRACK_AUTO.isSet(value.getConveyorStatus());
                    boolean cargoYn = ConveyorReadConsts.ConveyorStatus.CARGO_STATUS.isSet(value.getConveyorStatus());
                    boolean isStopperOpen = false; // TODO: 기존 로직 검토 ConveyorReadConsts.ConveyorStatus.STOPPER_STATUS.isSet(value.getConveyorStatus());
                    boolean isRun = ConveyorReadConsts.ConveyorStatus.MOTOR_RUN.isSet(value.getConveyorStatus());
                    cvMst.setAutoYn(isAuto);
                    cvMst.setCargoYn(cargoYn);
                    cvMst.setStopperOpenYn(isStopperOpen);
                    cvMst.setRunYn(isRun);
                    cvMst.setErrorId(String.valueOf(value.getConveyorError()));
                    cvMst.setErrorDesc(ConveyorReadConsts.ConveyorErrorCode.find(value.getConveyorError()).getDescription());
                    if (cvMst.getType() == EcsDBConsts.ConveyorType.LIFT.getValue())
                        cvMst.setAsiel(value.getCurrentLiftConveyorLevel()); // todo: setLevel -> setAsiel 변경 후 로직 확인
                    // log.info("updatePlcStatus cvMst : " + cvMst.getId());
                    this.iQueryManager.update(cvMst);
                }
            });
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    public List<TbEqCvMst> findListById(String eqId) {
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
