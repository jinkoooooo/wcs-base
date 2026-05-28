package operato.logis.samsung.service.hokusho.proc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.samsung.consts.InboundStatus;
import operato.logis.samsung.consts.TrackingStatus;
import operato.logis.samsung.consts.TrackingType;
import operato.logis.samsung.consts.VisionJudgeResult;
import operato.logis.samsung.entity.mw.*;
import operato.logis.samsung.service.hokusho.core.BoxDecisionService;
import operato.logis.samsung.service.hokusho.core.HokushoCommandGateway;
import operato.logis.samsung.service.mw.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeasureTrackProcessor implements TrackProcessor {

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(MeasureTrackProcessor.class);

    private static final String LOG_KEY_FORMAT =
            "[BCR][MEASURE] EquipId[{}], PlcSeq[{}], BoxId[{}], SerialCode[{}] - {}";

    private final HokushoCommandGateway hokushoCommandGateway;
    private final BoxDecisionService boxDecisionService;
    private final TbMwItemMasterService itemMasterService;
    private final BoxTrackingService boxTrackingService;
    private final TbMwChuteManagementService tbMwChuteManagementService;
    private final TbMwXyzOrderService tbMwXyzOrderService;
    private final TbMwInboundDeliveryService tbMwInboundDeliveryService;

    @Override
    public TrackingType supports() { return TrackingType.MEASURE; }

    @Override
    public void process(TbMwBoxTrack tr, TbMwBox box) {
        TrackingStatus st = TrackingStatus.fromValue(tr.getTrackingStatus());
        // 공통 시작 로그
        info(tr, box, "process() start. trackingStatus=" + st);

        switch (st) {
            case BCR_MEASURED -> {
                // 상품코드 기반 상품마스터 조회
                TbMwItemMaster item = itemMasterService.getItemMaster(box.getItemCode());

                info(tr, box, "BCR_MEASURED: ItemMaster 조회 완료. exists=" + (item != null));


                if(tr.getEquipId().equals("BCR-01")){

                    info(tr, box, "BCR-01 분기 진입.");

                    if(item == null){
                        error(tr, box, "BCR-01: ItemMaster 미존재. itemCode=" + box.getItemCode());

                        boxTrackingService.updateTbMwBoxStatus(box.getParcelId(), TrackingStatus.ERROR);
                    }else{
                        // 입고지시 체크
                        TbMwInboundDelivery inbDel = tbMwInboundDeliveryService.getRunningInboundDeliveryByItemCode(item.getItemCode(), InboundStatus.RUNNING);

                        // 박스 주문정보 셋팅
                        if(inbDel != null) {
                            box.setBlNo(inbDel.getBlNo());
                            box.setInvoice(inbDel.getInvoice());
                            box.setCntrNo(inbDel.getCntrNo());
                        }else {
                            TbMwInboundDelivery waitInbDel = tbMwInboundDeliveryService.getRunningInboundDeliveryByItemCode(
                                    item.getItemCode(), InboundStatus.READY);

                            if(waitInbDel != null){
                                box.setBlNo(waitInbDel.getBlNo());
                                box.setInvoice(waitInbDel.getInvoice());
                                box.setCntrNo(waitInbDel.getCntrNo());
                                if(waitInbDel.isManualFlag()){
                                    // 메뉴얼 박스 표시
                                    box.setAttribute4("true");
                                } else{
                                    box.setAttribute4("false");
                                }
                            }
                        }

                        // 박스 기준정보 상품명 업데이트 // 삼성전자 제품명 컬럼 따로 없어서 타입 & 코드로 대체
                        box.setItemName(item.getItemType() + "/" + item.getItemCode());
                        boxTrackingService.updateTbMwBox(box);

                        info(tr, box, "BCR-01: ItemName 업데이트. itemName=" + box.getItemName());

                        // 체적/방향 판정
                        BoxDecisionService.DimCheckResult dimResult = boxDecisionService.checkDimensionAndOrientation(box, item);

                        info(tr, box, String.format(
                                "BCR-01: DimCheck 결과. reject=%s, reason=%s, direction=%s",
                                dimResult.isReject(), dimResult.getReason(), dimResult.getDirection()
                        ));

                        if(dimResult.isReject()){
                            // 체적 불량 업데이트
                            box.setManualResult(0);
                            box.setManualResultAt(new Date());
                            box.setManualResultRmk(dimResult.getReason());
                            box.setTrackingStatus(TrackingStatus.HEIGHT_VALID_NG.getValue());
                            box.setTrackingDesc(TrackingStatus.HEIGHT_VALID_NG.getDescription());
                            box.setTrackingAt(new Date());
                            // 상품마스터 기준체적 비교를 위한 데이터 입력
                            box.setAttribute1(item.getItemLength().toString());
                            box.setAttribute2(item.getItemHeight().toString());
                            box.setAttribute3(item.getItemWidth().toString());

                            box.setRejectType("체적불량");
                            box.setRejectDesc("BCR01 - 체적불량");

                            boxTrackingService.updateTbMwBox(box);

                            // 리젝 스냅샷
                            boxTrackingService.insertTrackSnapshot(box, tr.getPlcSeqNo(), tr.getLineId(), tr.getEquipId(),
                                    new Date(), TrackingType.VALIDATION, TrackingStatus.HEIGHT_VALID_NG);

                            info(tr, box, "BCR-01: HEIGHT_VALID_NG 설정 및 스냅샷 저장 완료.");
                        }else{
                            box.setManualResult(1);
                            box.setManualResultAt(new Date());
                            box.setManualResultRmk(dimResult.getReason());
                            box.setTrackingStatus(TrackingStatus.HEIGHT_VALID_OK.getValue());
                            box.setTrackingDesc(TrackingStatus.HEIGHT_VALID_OK.getDescription());
                            box.setTrackingAt(new Date());
                            // 상품마스터 기준체적 비교를 위한 데이터 입력
                            box.setAttribute1(item.getItemLength().toString());
                            box.setAttribute2(item.getItemHeight().toString());
                            box.setAttribute3(item.getItemWidth().toString());

                            box.setRejectType("정상");
                            box.setRejectDesc("BCR01 - 정상");

                            boxTrackingService.updateTbMwBox(box);

                            // 리젝 스냅샷
                            boxTrackingService.insertTrackSnapshot(box, tr.getPlcSeqNo(), tr.getLineId(), tr.getEquipId(),
                                    new Date(), TrackingType.VALIDATION, TrackingStatus.HEIGHT_VALID_OK);

                            info(tr, box, "BCR-01: HEIGHT_VALID_OK 설정 및 스냅샷 저장 완료.");
                        }
                    }
                }
                else if (tr.getEquipId().equals("BCR-02")) {
                    info(tr, box, "BCR-02 분기 진입.");

                    // 해당 상품 파렛타이징 되는 파렛트 포인트 번호와 함께 Turn 지시 날리기
                    TbMwChute chute = tbMwChuteManagementService.getChuteByItemCode(box.getItemCode());

                    if(chute != null){
                        info(tr, box, "BCR-02: Chute 매핑 성공. endPointCd=" + chute.getEndPointCd());

                        if(item == null){
                            error(tr, box, "BCR-02: ItemMaster 미존재. itemCode=" + box.getItemCode());

                            boxTrackingService.updateTbMwBoxStatus(box.getParcelId(), TrackingStatus.ERROR);
                        }
                        else{
                            // 박스 기준정보 상품명 업데이트 // 삼성전자 제품명 컬럼 따로 없어서 타입 & 코드로 대체
                            box.setItemName(item.getItemType() + "/" + item.getItemCode());
                            boxTrackingService.updateTbMwBox(box);

                            info(tr, box, "BCR-02: ItemName 업데이트. itemName=" + box.getItemName());

                            // 체적/방향 판정
                            BoxDecisionService.DimCheckResult dimResult = boxDecisionService.checkDimensionAndOrientation(box, item);

                            info(tr, box, String.format(
                                    "BCR-02: DimCheck 결과. reject=%s, reason=%s, direction=%s",
                                    dimResult.isReject(), dimResult.getReason(), dimResult.getDirection()
                            ));

                            if(dimResult.isReject()){
                                // 체적 불량 업데이트
                                box.setManualResult(0);
                                box.setManualResultAt(new Date());
                                box.setManualResultRmk(dimResult.getReason());
                                box.setTrackingStatus(TrackingStatus.HEIGHT_VALID_NG.getValue());
                                box.setTrackingDesc(TrackingStatus.HEIGHT_VALID_NG.getDescription());
                                box.setTrackingAt(new Date());

                                box.setRejectType("체적불량");
                                box.setRejectDesc("BCR02 - 체적불량");

                                boxTrackingService.updateTbMwBox(box);

                                // 리젝 스냅샷
                                boxTrackingService.insertTrackSnapshot(box, tr.getPlcSeqNo(), tr.getLineId(), tr.getEquipId(),
                                        new Date(), TrackingType.VALIDATION, TrackingStatus.HEIGHT_VALID_NG);

                                info(tr, box, "BCR-02: Dim REJECT → COMMANDED_TURN_STOP 설정 및 스냅샷 저장.");


                                // 2번 BCR 체적 불량인 경우엔 VOL 지시 보내기
                                hokushoCommandGateway.sendTurn(box, tr, 1, 1, "VOL");
                                info(tr, box, "BCR-02: TURN 지시 전송. useRotator=1, pickType=1, endPointCd=VOL");


                            }
                            else{
                                // === 체적 OK 인 경우 처리 ===
                                // 2026.04.23.JJG 추가
                                // 해당 박스가 1번 BCR/Vision 또는 작업자 최종 통과했었는지 검증 필요.
                                // TODO : 로직 검증 필요.
                                if(
                                        ValueUtil.isNotEmpty(boxTrackingService.findBoxTrackByStatus(
                                                box.getBoxId(), TrackingStatus.VISION_MEASURED)) &&
                                                // (비전이 측정됨 AND (비전OK OR 작업자OK))
                                                (
                                                        ValueUtil.isNotEmpty(boxTrackingService.findBoxTrackByStatus(
                                                                box.getBoxId(), TrackingStatus.VISION_VALID_OK)) ||
                                                                ValueUtil.isNotEmpty(boxTrackingService.findBoxTrackByStatus(
                                                                        box.getBoxId(), TrackingStatus.FINAL_VALID_OK))
                                                )
                                ){
                                    box.setManualResult(1);
                                    box.setManualResultAt(new Date());
                                    box.setEndPointCd(chute.getEndPointCd());

                                    // 방향 정보에 따라 메모 남기기 (선택)
                                    BoxDecisionService.BoxLongSideDirection dir = dimResult.getDirection();
                                    String okMsg;
                                    int turnResult = 0;

                                    if (dir == BoxDecisionService.BoxLongSideDirection.LONG_SIDE_FORWARD) {
                                        okMsg = String.format(
                                                "[%s] BOX DIM OK: 장변 전진 방향 통과 (Long side forward - in tolerance)",
                                                box.getBoxId()
                                        );
                                    } else if (dir == BoxDecisionService.BoxLongSideDirection.LONG_SIDE_SIDEWAYS) {
                                        okMsg = String.format(
                                                "[%s] BOX DIM OK: 장변 횡방향 통과 (Long side sideways - in tolerance)",
                                                box.getBoxId()
                                        );

                                        // 횡방향 직진의 경우 터닝
                                        turnResult = 1;
                                    } else {
                                        okMsg = String.format(
                                                "[%s] BOX DIM OK: 기준 대비 허용오차 이내 (Dimension in tolerance)",
                                                box.getBoxId()
                                        );
                                    }

                                    box.setManualResultRmk(okMsg);

                                    // 트래킹 상태도 OK 쪽으로 갱신
                                    box.setTrackingStatus(TrackingStatus.HEIGHT_VALID_OK.getValue());
                                    box.setTrackingDesc(TrackingStatus.HEIGHT_VALID_OK.getDescription());
                                    box.setTrackingAt(new Date());

                                    box.setRejectType("정상");
                                    box.setRejectDesc("BCR02 - 정상");

                                    boxTrackingService.updateTbMwBox(box);

                                    // OK 히스토리 스냅샷
                                    boxTrackingService.insertTrackSnapshot(
                                            box,
                                            tr.getPlcSeqNo(),
                                            tr.getLineId(),
                                            tr.getEquipId(),
                                            new Date(),
                                            TrackingType.VALIDATION,
                                            TrackingStatus.HEIGHT_VALID_OK
                                    );

                                    info(tr, box, String.format(
                                            "BCR-02: Dim OK 처리 완료. direction=%s, turnResult=%d",
                                            dir, turnResult
                                    ));

                                    int storeType = Integer.parseInt(item.getStoreType());

                                    // 1Pick 의 경우 미회전 추가
                                    hokushoCommandGateway.sendTurn(box, tr, storeType == 1 ? 0 : turnResult, storeType, chute.getEndPointCd());
                                    logger.info("[MEASURE] BCR02 -> TURN (useRotator={}, pickType={}, endPointCd={}). boxId={}",
                                            1, 1, chute.getEndPointCd(), box.getBoxId());

                                    info(tr, box, String.format(
                                            "BCR-02: TURN 지시 전송. useRotator=%d, pickType=%s, endPointCd=%s",
                                            turnResult, item.getStoreType(), chute.getEndPointCd()
                                    ));
                                }else{
                                    warn(tr, box, "BCR-02: 판단대기 박스 / VALID_WAIT BOX . itemCode=" + box.getItemCode()
                                            + " → UNKNOWN 으로 TURN 지시.");

                                    // 지시 보내기
                                    hokushoCommandGateway.sendTurn(box, tr, 1, 1, "UNKNOWN");
                                    info(tr, box, "BCR-02: TURN 지시 전송. useRotator=1, pickType=1, endPointCd=UNKNOWN");

                                    box.setTrackingStatus(TrackingStatus.VISION_VALID_WAIT.getValue());
                                    box.setTrackingDesc(TrackingStatus.VISION_VALID_WAIT.getDescription());
                                    box.setTrackingAt(new Date());
                                    box.setRejectType("판단대기");
                                    box.setRejectDesc("BCR02 - 판단대기 또는 BCR01 미통과");

                                    boxTrackingService.updateTbMwBox(box);
                                }
                            }
                        }

                    }else{
                        warn(tr, box, "BCR-02: Chute 매핑 실패. itemCode=" + box.getItemCode()
                                + " → UNKNOWN 으로 TURN 지시.");

                        // 지시 보내기
                        hokushoCommandGateway.sendTurn(box, tr, 1, 1, "UNKNOWN");
                        info(tr, box, "BCR-02: TURN 지시 전송. useRotator=1, pickType=1, endPointCd=UNKNOWN");

                        box.setTrackingStatus(TrackingStatus.CHUTE_NOT_ASSIGN.getValue());
                        box.setTrackingDesc(TrackingStatus.CHUTE_NOT_ASSIGN.getDescription());
                        box.setTrackingAt(new Date());
                        box.setRejectType("수행대기");
                        box.setRejectDesc("BCR02 - 미 진행 제품");

                        boxTrackingService.updateTbMwBox(box);

                    }

                } else {
                    error(tr, box, "BCR_MEASURED: 지원하지 않는 EquipId. equipId=" + tr.getEquipId());

                    box.setTrackingStatus(TrackingStatus.ERROR.getValue());
                    box.setTrackingDesc(TrackingStatus.ERROR.getDescription());
                    box.setTrackingAt(new Date());
                    box.setRejectType("ERROR");
                    box.setRejectDesc("BCR02 - 지원하지 않는 EquipId");

                    boxTrackingService.updateTbMwBox(box);
                }
            }
            case VISION_MEASURED -> {
                if (box == null) {
                    warn(tr, null, "VISION_MEASURED: box is null. parcelId=" + tr.getParcelId()
                            + ", boxId=" + tr.getBoxId());
                    return;
                }
                info(tr, box, "VISION_MEASURED 분기 진입.");

                TbMwChute chute = tbMwChuteManagementService.getChuteByItemCode(box.getItemCode());
                VisionJudgeResult judge = boxDecisionService.judgeForVisionDivert(box);

                info(tr, box, "VISION: judge 결과=" + judge);


                if(chute != null){
                    info(tr, box, "VISION: Chute 진행중 상품 확인. endPointCd=" + chute.getEndPointCd());

                    // judge → 디버트 방향 매핑 (PATH / NG / 대기)
                    String divertTo = switch (judge) {
                        case ALL_OK -> "PATH";
                        case HAS_NG -> "NG";
                        default     -> null;   // WAIT, UNKNOWN 등은 지시 안 날림
                    };

                    if (divertTo != null) {
                        // 지시 보내기
                        hokushoCommandGateway.sendDivert(box, tr, divertTo, "P1DVT1ARV1");
                        info(tr, box, String.format(
                                "VISION: DIVERT 지시 전송. divertTo=%s, deviceCode=P1DVT1ARV1, judge=%s",
                                divertTo, judge
                        ));

                        TrackingStatus visionResult = TrackingStatus.toVisionStatus(judge);

                        String resultType = !"NG".equals(divertTo)
                                ? "정상"
                                : box.getManualResult() == 0
                                ? "체적불량"
                                : box.getCognexResult() == 0
                                ? "외관불량"
                                : "정상";

                        box.setTrackingStatus(visionResult.getValue());
                        box.setTrackingDesc(visionResult.getDescription());
                        box.setTrackingAt(new Date());
                        box.setRejectType(resultType);
                        box.setRejectDesc(visionResult.getDescription());

                        boxTrackingService.updateTbMwBox(box);

                        boxTrackingService.updateTbMwBoxStatus(box.getParcelId(), TrackingStatus.toVisionStatus(judge));
                    } else {
                        // 대기 또는 기타 상태
                        info(tr, box, "VISION: divert 지시 없음. judge=" + judge
                                + ", status=VISION_VALID_WAIT 로 유지/갱신.");


                        boxTrackingService.updateTbMwBoxStatus(box.getParcelId(), TrackingStatus.VISION_VALID_WAIT);
                    }
                }else{
                    if("true".equals(box.getAttribute4())){
                        warn(tr, box, "VISION: Manual Flag Item. itemCode=" + box.getItemCode()
                                + " → NG 라인으로 DIVERT.");
                        // 지시 보내기
                        hokushoCommandGateway.sendDivert(box, tr, "NG", "P1DVT1ARV1");
                        info(tr, box, "VISION: DIVERT 지시 전송. divertTo=NG, deviceCode=P1DVT1ARV1");

                        box.setTrackingStatus(TrackingStatus.MANUAL_FLAG_TRUE.getValue());
                        box.setTrackingDesc(TrackingStatus.MANUAL_FLAG_TRUE.getDescription());
                        box.setTrackingAt(new Date());
                        if(box.getBoxId().toUpperCase().contains("NOREAD")){
                            box.setRejectType("NOREAD");
                            box.setRejectDesc("BCR01 - NoRead");
                        }else{
                            box.setRejectType("수동적치");
                            box.setRejectDesc("BCR01 - 수동 적치 상품");
                        }
                    } else {
                        warn(tr, box, "VISION: Chute 미지정. itemCode=" + box.getItemCode()
                                + " → NG 라인으로 DIVERT.");
                        // 지시 보내기
                        hokushoCommandGateway.sendDivert(box, tr, "NG", "P1DVT1ARV1");
                        info(tr, box, "VISION: DIVERT 지시 전송. divertTo=NG, deviceCode=P1DVT1ARV1");

                        box.setTrackingStatus(TrackingStatus.CHUTE_NOT_ASSIGN.getValue());
                        box.setTrackingDesc(TrackingStatus.CHUTE_NOT_ASSIGN.getDescription());
                        box.setTrackingAt(new Date());
                        if(box.getBoxId().toUpperCase().contains("NOREAD")){
                            box.setRejectType("NOREAD");
                            box.setRejectDesc("BCR01 - NoRead");
                        }else{
                            box.setRejectType("수행대기");
                            box.setRejectDesc("BCR01 - 미 진행 상품");
                        }
                    }

                    boxTrackingService.updateTbMwBox(box);
                }

            }
            default -> info(tr, box, "UNKNOWN TrackingStatus=" + st + ", boxId=" + tr.getBoxId());
        }
    }

    // ========= 공통 로그 Helper =========

    private void info(TbMwBoxTrack tr, TbMwBox box, String message) {
        logger.info(
                LOG_KEY_FORMAT,
                tr.getEquipId(),
                tr.getPlcSeqNo(),
                (box != null && box.getBoxId() != null) ? box.getBoxId() : tr.getBoxId(),
                (box != null) ? box.getItemCode() : null,
                message
        );
    }

    private void warn(TbMwBoxTrack tr, TbMwBox box, String message) {
        logger.warn(
                LOG_KEY_FORMAT,
                tr.getEquipId(),
                tr.getPlcSeqNo(),
                (box != null && box.getBoxId() != null) ? box.getBoxId() : tr.getBoxId(),
                (box != null) ? box.getItemCode() : null,
                message
        );
    }

    private void error(TbMwBoxTrack tr, TbMwBox box, String message) {
        logger.error(
                LOG_KEY_FORMAT,
                tr.getEquipId(),
                tr.getPlcSeqNo(),
                (box != null && box.getBoxId() != null) ? box.getBoxId() : tr.getBoxId(),
                (box != null) ? box.getItemCode() : null,
                message
        );
    }
}
