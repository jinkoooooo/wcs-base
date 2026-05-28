package operato.logis.samsung.service.mw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.gtr.dto.InspectionRequestDto;
import operato.logis.connector.gtr.service.InspectionService;
import operato.logis.samsung.WcsConstants;
import operato.logis.samsung.consts.BoxTrackingEventType;
import operato.logis.samsung.consts.TrackingStatus;
import operato.logis.samsung.consts.TrackingType;
import operato.logis.samsung.entity.mw.*;
import operato.logis.samsung.event.BoxArrivedOnConveyorEvent;
import operato.logis.samsung.event.BoxTrackingEvent;
import operato.logis.samsung.service.hokusho.HokushoOrderDispatcherService;
import operato.logis.samsung.service.hokusho.core.HokushoCommandGateway;
import operato.logis.samsung.utils.ParcelIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoxTrackingEventListener extends AbstractQueryService {
    private Logger logger = LoggerFactory.getLogger(BoxTrackingEventListener.class);

    private static final String LOG_KEY_FORMAT =
            "[BOX-EVENT] EquipId[{}], PlcSeq[{}], BoxId[{}], SerialCode[{}] - {}";

    private final BoxTrackingService boxTrackingService;

    private final HokushoOrderDispatcherService hokushoDispatcher;

    private final HokushoCommandGateway hokushoCommandGateway;

    private final ApplicationEventPublisher publisher;

    private final InspectionService inspectionService;

    /* =========================================================
     * Entry Point : INIT / UPDATE 분기
     *
     * ========================================================= */
    @Transactional
    @Async
    @EventListener(classes = BoxTrackingEvent.class)
    public void boxTrackingListener(BoxTrackingEvent event) {
        if (event == null || ValueUtil.isEmpty(event.getPlcSeqNo())) {
            logger.warn("[BoxTrackingEvent] invalid event: {}", event);
            return;
        }

        // 1) 도메인 컨텍스트
        Domain domain = new Domain();
        domain.setId(WcsConstants.DOMAIN_ID);
        domain.setName(WcsConstants.DOMAIN_NAME);
        Domain.setCurrentDomain(domain);

        // 이벤트 내부값 전체 로그
        logger.info(event.toPrettyLog());

        // 2) 공통 파라미터
        final Date   now       = event.getMeasuredAt() != null ? event.getMeasuredAt() : new Date();
        final String barcode   = event.getBarcode();
        final String lineId    = ValueUtil.isNotEmpty(event.getLineId())    ? event.getLineId()    : SettingUtil.getValue("mw.lineId.bcr1", "LINE01");
        final String equipId   = ValueUtil.isNotEmpty(event.getEquipId())   ? event.getEquipId()   : SettingUtil.getValue("mw.equipId.bcr1", "BCR01");
        final String plcSeqNo = ValueUtil.isNotEmpty(event.getPlcSeqNo()) ? event.getPlcSeqNo() : SettingUtil.getValue("mw.plcSeqNo.bcr1", "0000");

        // 3) 타입 분기
        BoxTrackingEventType type = event.getEventType();
        switch (type) {
            case BCR_EVENT, VISION_EVENT -> {
                info(event, null,
                        "[BCR/VISION] ENTRY: eventType=" + type +
                                ", barcode=" + barcode +
                                ", lineId=" + lineId +
                                ", equipId=" + equipId +
                                ", plcSeqNo=" + plcSeqNo);
                handleCognexEvent(event, now, barcode, lineId, equipId, plcSeqNo);
            }
            case HOKUSHO_EVENT -> {
                info(event, null,
                        "[HOKUSHO] ENTRY: rawPlcSeqNo=" + event.getPlcSeqNo());
                handleHokushoEvent(event);
            }
            case XYZ_EVENT -> {
                info(event, null,
                        "[XYZ] ENTRY: rawPlcSeqNo=" + event.getPlcSeqNo());
                handleXyzEvent(event);
            }
            default     -> warn(event, null, "[ENTRY] unsupported eventType=" + type);
        }
    }

    /* =========================================================
     * Cognex 이벤트 처리
     * ========================================================= */
    private void handleCognexEvent(BoxTrackingEvent event, Date now, String barcode,
                            String lineId, String equipId, String plcSeqNo) {

        // PlcSeqNo 우선 → 없으면 boxId로 조회 → 그래도 없으면 발번
        TbMwBox box = null;
        String parcelId;

        if (ValueUtil.isNotEmpty(event.getPlcSeqNo())) {

            if(event.getEventType() == BoxTrackingEventType.BCR_EVENT){
                box = boxTrackingService.findBoxByBoxId(event.getBarcode());
            } else {
                // VISION_EVENT 의 경우
                box = boxTrackingService.findBoxByPlcSeqNo(event.getPlcSeqNo());
            }

            if (box != null) {
                info(event, box,
                        "[BCR/VISION] 기존 BOX 갱신. eventType=" + event.getEventType());

                // 코그닉스 비전 결과 저장
                if (ValueUtil.isNotEmpty(event.getCognexVisionResult())) {
                    Integer result = event.getCognexVisionResult();
                    box.setCognexResult(result);
                    box.setCognexResultRmk(
                            result == 1 ? TrackingStatus.VISION_VALID_OK.getDescription()
                                    : TrackingStatus.VISION_VALID_NG.getDescription());
                    box.setCognexResultAt(now);

                    info(event, box,
                            "[BCR/VISION] Cognex 결과 저장. result=" + result +
                                    ", desc=" + box.getCognexResultRmk());

                    // 세부 Vision 측정 기록 추가
                    boxTrackingService.insertTrackSnapshot(box, event.getPlcSeqNo(), event.getLineId(), event.getEquipId(),
                            now, TrackingType.MEASURE, (result == 1 ? TrackingStatus.VISION_VALID_OK
                                    : TrackingStatus.VISION_VALID_NG));
                }


                // TRACK 히스토리 INSERT-only
                TrackingStatus trackStatus = TrackingStatus.toMeasureStatus(event.getEventType());
                TbMwBoxTrack boxTrack = boxTrackingService.insertTrackSnapshot(box, event.getPlcSeqNo(), event.getLineId(), event.getEquipId(),
                        now, TrackingType.MEASURE, trackStatus);

                box.setTrackingAt(now);
                box.setTrackingStatus(trackStatus.getValue());
                box.setTrackingDesc(trackStatus.getDescription());

                boxTrackingService.updateTbMwBoxByEvent(box, event, lineId, equipId, now);

                info(event, box,
                        "[BCR/VISION] TRACK 스냅샷 생성. trackingStatus=" + trackStatus);

                // 호쿠셔 컨베이어 로직 서비스 호출
                // 최종 커밋 완료시
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { hokushoDispatcher.handle(boxTrack); }
                });
            } else{
                // 최초 PlcSeqNo 들어온 시점. 노리드
                if(ValueUtil.isNotEmpty(event.getBarcode()) && event.getBarcode().equalsIgnoreCase("NoRead")){
                    //20260519 No read 시 PLC Seq No 앞에 "END - " 추가
                    String originalPlcSeqNo = event.getPlcSeqNo() != null ? String.valueOf(event.getPlcSeqNo()) : "";
                    String modifiedPlcSeqNo = "END-" + originalPlcSeqNo;

                    event.setPlcSeqNo(modifiedPlcSeqNo);

                    // 완전 신규
                    parcelId = ParcelIdGenerator.next("NoRead", plcSeqNo, "NoRead");
                    error(event, null,
                            "[BCR/VISION] NoRead BCR 수신. 신규 BOX 생성. parcelId=" + parcelId);


                    // 노리드 중복 제거용
                    event.setBarcode(parcelId);
                    // BOX insert
                    box = boxTrackingService.insertTbMwBox(event, parcelId, lineId, equipId, now);

                    // TRACK 히스토리 INSERT-only
                    TrackingStatus trackStatus = TrackingStatus.toMeasureStatus(event.getEventType());
                    TbMwBoxTrack boxTrack = boxTrackingService.insertTrackSnapshot(
                            box,
                            event.getPlcSeqNo(),
                            event.getLineId(),
                            event.getEquipId(),
                            now,
                            TrackingType.MEASURE,
                            trackStatus
                    );

                    info(event, box,
                            "[BCR/VISION] NoRead BOX 스냅샷 생성. trackingStatus=" + trackStatus);


                    if(event.getEquipId().equals("BCR-02")){
                        // 지시 보내기
                        hokushoCommandGateway.sendTurn(box, boxTrack, 0, 1, "NOREAD");
                        info(event, box,
                                "[BCR] BCR-02 NoRead → TURN 지시. useRotator=0, pickType=1, endPointCd=NOREAD");

                        box.setTrackingStatus(TrackingStatus.BCR_NOREAD.getValue());
                        box.setTrackingDesc(TrackingStatus.BCR_NOREAD.getDescription());
                        box.setTrackingAt(new Date());
                        box.setRejectType("수행대기");
                        box.setRejectDesc("BCR02 - NoRead");

                        boxTrackingService.updateTbMwBox(box);
                    }else{
                        hokushoCommandGateway.sendDivert(box, boxTrack,"NG", "P1DVT1ARV1");
                        info(event, box,
                                "[BCR] NoRead → DIVERT 지시. divertTo=NG, device=P1DVT1ARV1");

                        box.setTrackingStatus(TrackingStatus.BCR_NOREAD.getValue());
                        box.setTrackingDesc(TrackingStatus.BCR_NOREAD.getDescription());
                        box.setTrackingAt(new Date());
                        box.setRejectType("수행대기");
                        box.setRejectDesc("BCR01 - NoRead");

                        boxTrackingService.updateTbMwBox(box);
                    }
                }else if(event.getEventType() == BoxTrackingEventType.BCR_EVENT){
                    // 완전 신규
                    parcelId = ParcelIdGenerator.next(lineId, plcSeqNo, barcode);
                    info(event, null,
                            "[BCR/VISION] 신규 BOX 감지. barcode=" + barcode + ", parcelId=" + parcelId);

                    // TRACK 히스토리 INSERT-only
                    TrackingStatus trackStatus = TrackingStatus.toMeasureStatus(event.getEventType());

                    box = boxTrackingService.findBoxByBoxId(event.getBarcode());

                    if(box != null){
                        box.setFirstEquipId(event.getEquipId());
                        box.setFirstLineId(event.getLineId());
                        box.setTrackingAt(now);
                        box.setTrackingStatus(trackStatus.getValue());
                        box.setTrackingDesc(trackStatus.getDescription());

                        boxTrackingService.updateTbMwBoxByEvent(box, event, lineId, equipId, now);
                        info(event, box,
                                "[BCR/VISION] 기존 BOX 갱신. eventType=" + event.getEventType());

                        info(event, box,
                                "[BCR/VISION] 기존 BOX 재사용. firstEquipId=" + box.getFirstEquipId() +
                                        ", firstLineId=" + box.getFirstLineId());
                    }else{
                        // BOX insert
                        box = boxTrackingService.insertTbMwBox(event, parcelId, lineId, equipId, now);

                        info(event, box,
                                "[BCR/VISION] 신규 BOX insert 완료.");
                    }
                    TbMwBoxTrack boxTrack = boxTrackingService.insertTrackSnapshot(
                            box,
                            event.getPlcSeqNo(),
                            event.getLineId(),
                            event.getEquipId(),
                            now,
                            TrackingType.MEASURE,
                            trackStatus
                    );

                    info(event, box, "[BCR/VISION] 신규 BOX 스냅샷 생성. trackingStatus=" + trackStatus);

                    // 호쿠셔 컨베이어 로직 서비스 호출
                    // 최종 커밋 완료시
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() { hokushoDispatcher.handle(boxTrack); }
                    });
                } else{ // 비전 지시 잘못들어온 경우
                    error(event, null,
                            "[VISION] Vision Report Data ERROR. eventType=" + event.getEventType() + ", event=" + event);
                }
            }
        } else {
            error(event, null,
                    "[BCR/VISION] PlcSeqNo is Null. eventType=" + event.getEventType() + ", event=" + event);
        }
    }

    /* =========================================================
     * 호쿠쇼 이벤트 처리부
     * ========================================================= */
    private void handleHokushoEvent(BoxTrackingEvent event) {

        List<String> plcSeqList = event.getPlcSeqNoList(); // "1001|1002" → ["1001","1002"]

        if (plcSeqList.isEmpty()) {
            error(event, null,
                    "[HOKUSHO] empty plcSeq list. raw=" + event.getPlcSeqNo());
            return;
        }

        info(event, null,
                "[HOKUSHO] 처리 시작. plcSeqCount=" + plcSeqList.size());

        List<TbMwBox> boxList = new ArrayList<>();
        TrackingStatus nowTrackingStatus = null;

        // 1) PLC Seq 개수만큼 처리
        for (String plcSeq : plcSeqList) {

            TbMwBox box = boxTrackingService.findBoxByPlcSeqNo(plcSeq);

            if (box == null) {
                warn(event, null, plcSeq,
                        "[HOKUSHO] box 미존재. skip. plcSeq=" + plcSeq);
                continue;
            }

            // ---  박스 상태 업데이트  ---
            nowTrackingStatus = TrackingStatus.toReportStatus(event.getHokushoData().getResultType());
            boxTrackingService.updateTbMwBoxStatus(box.getParcelId(), nowTrackingStatus);

            // ---  트레킹 히스토리 생성 (INSERT only) ---
            boxTrackingService.insertTrackSnapshot(
                    box,
                    plcSeq,
                    event.getLineId(),
                    event.getEquipId(),
                    new Date(),
                    TrackingType.REPORTING,
                    nowTrackingStatus
            );

            info(event, box, plcSeq,
                    "[HOKUSHO] REPORT 수신. resultType=" + event.getHokushoData().getResultType() +
                            ", trackingStatus=" + nowTrackingStatus);

            boxList.add(box);
        }

        AtomicInteger index = new AtomicInteger();
        // 2) XYZ 로봇팔 호출은 박스 여러 개를 리스트로 한 번만 호출
        if (!boxList.isEmpty() && TrackingStatus.REPORT_PLTZ == nowTrackingStatus) {

            info(event, boxList.get(0),
                    "[HOKUSHO] REPORT_PLTZ 상태. XYZ 호출 준비. boxCount=" + boxList.size());

            List<TbMwBoxConveyorInfo> pickBoxList = boxList.stream()
                    .map(box -> {
                        TbMwBoxConveyorInfo info = new TbMwBoxConveyorInfo();
                        info.setPid(box.getPlcSeqNo());
                        info.setIndex(index.getAndIncrement());
                        info.setItemCode(box.getItemCode());
                        info.setSerialNo(box.getBoxId());
                        return info;
                    })
                    .toList();


            // === XYZ 엔진 호출 ===
            try {
                BoxArrivedOnConveyorEvent boxArrivedOnConveyorEvent = BoxArrivedOnConveyorEvent.builder()
                        .boxConveyorCd("BC01")
                        .boxList(pickBoxList)
                        .build();

                publisher.publishEvent(boxArrivedOnConveyorEvent);
                info(event, boxList.get(0),
                        "[HOKUSHO] XYZ BoxArrivedOnConveyorEvent publish 완료. size=" + pickBoxList.size());

            } catch (Exception e) {
                logger.error("[HOKUSHO] XYZ send box error. size={}, err={}",
                        pickBoxList.size(), e.getMessage(), e);
            }
        }

    }

    /* =========================================================
     * XYZ 이벤트 처리부
     * ========================================================= */
    private void handleXyzEvent(BoxTrackingEvent event) {

        TbMwBox box = null;

        if (ValueUtil.isNotEmpty(event.getPlcSeqNo())) {

            box = boxTrackingService.findBoxByPlcSeqNo(event.getPlcSeqNo());
        } else {
            error(event, null,
                    "[XYZ] no received box PlcSeqNo.");

            return;
        }

        if (box != null){

            TrackingStatus ts = TrackingStatus.STORED;

            // TRACK 히스토리 INSERT-only
            TbMwBoxTrack boxTrack = boxTrackingService.insertTrackSnapshot(box, event.getPlcSeqNo(), event.getLineId(), event.getEquipId(),
                    event.getMeasuredAt(), TrackingType.REPORTING, ts);

            info(event, box,
                    "[XYZ] 적재 완료 REPORT 수신. resultType=");

            box.setTrackingStatus(ts.getValue());
            box.setTrackingAt(event.getMeasuredAt());
            box.setTrackingDesc(ts.getDescription());


            box.setFinalStatus(ts.getValue());
            box.setFinalRemark(ts.getDescription());
            box.setFinalAt(event.getMeasuredAt());

            String originalSeqNo = event.getPlcSeqNo();

            box.setPlcSeqNo("END-" + event.getPlcSeqNo()); // PLC 순환에 걸리지 않게 마무리 업데이트
            // TbMwBox 상태 변경
            boxTrackingService.updateTbMwBox(box);

            invokeInspectionApi(box, originalSeqNo);
        } else {
            error(event, null,
                    "[XYZ] existing box 없음. PlcSeqNo=" + event.getPlcSeqNo());
        }

    }

    /* =========================================================
     * 공통 로그 Helper
     * ========================================================= */
    private void info(BoxTrackingEvent event, TbMwBox box, String message) {
        info(event, box, null, message);
    }

    private void info(BoxTrackingEvent event, TbMwBox box, String plcSeqOverride, String message) {
        if (event == null) {
            logger.info("[BOX-EVENT] (no event) BoxId[{}], SerialCode[{}] - {}",
                    box != null ? box.getBoxId() : null,
                    box != null ? box.getItemCode() : null,
                    message);
            return;
        }

        String equipId = ValueUtil.isNotEmpty(event.getEquipId())
                ? event.getEquipId()
                : "N/A";

        String plcSeq = ValueUtil.isNotEmpty(plcSeqOverride)
                ? plcSeqOverride
                : event.getPlcSeqNo();

        String boxId = (box != null && ValueUtil.isNotEmpty(box.getBoxId()))
                ? box.getBoxId()
                : null;

        String serialCode = null;
        if (box != null && ValueUtil.isNotEmpty(box.getItemCode())) {
            serialCode = box.getItemCode();
        } else if (ValueUtil.isNotEmpty(event.getBarcode())) {
            serialCode = event.getBarcode();
        }

        logger.info(LOG_KEY_FORMAT, equipId, plcSeq, boxId, serialCode, message);
    }

    private void warn(BoxTrackingEvent event, TbMwBox box, String message) {
        warn(event, box, null, message);
    }

    private void warn(BoxTrackingEvent event, TbMwBox box, String plcSeqOverride, String message) {
        if (event == null) {
            logger.warn("[BOX-EVENT] (no event) BoxId[{}], SerialCode[{}] - {}",
                    box != null ? box.getBoxId() : null,
                    box != null ? box.getItemCode() : null,
                    message);
            return;
        }

        String equipId = ValueUtil.isNotEmpty(event.getEquipId())
                ? event.getEquipId()
                : "N/A";

        String plcSeq = ValueUtil.isNotEmpty(plcSeqOverride)
                ? plcSeqOverride
                : event.getPlcSeqNo();

        String boxId = (box != null && ValueUtil.isNotEmpty(box.getBoxId()))
                ? box.getBoxId()
                : null;

        String serialCode = null;
        if (box != null && ValueUtil.isNotEmpty(box.getItemCode())) {
            serialCode = box.getItemCode();
        } else if (ValueUtil.isNotEmpty(event.getBarcode())) {
            serialCode = event.getBarcode();
        }

        logger.warn(LOG_KEY_FORMAT, equipId, plcSeq, boxId, serialCode, message);
    }

    private void error(BoxTrackingEvent event, TbMwBox box, String message) {
        error(event, box, null, message);
    }

    private void error(BoxTrackingEvent event, TbMwBox box, String plcSeqOverride, String message) {
        if (event == null) {
            logger.error("[BOX-EVENT] (no event) BoxId[{}], SerialCode[{}] - {}",
                    box != null ? box.getBoxId() : null,
                    box != null ? box.getItemCode() : null,
                    message);
            return;
        }

        String equipId = ValueUtil.isNotEmpty(event.getEquipId())
                ? event.getEquipId()
                : "N/A";

        String plcSeq = ValueUtil.isNotEmpty(plcSeqOverride)
                ? plcSeqOverride
                : event.getPlcSeqNo();

        String boxId = (box != null && ValueUtil.isNotEmpty(box.getBoxId()))
                ? box.getBoxId()
                : null;

        String serialCode = null;
        if (box != null && ValueUtil.isNotEmpty(box.getItemCode())) {
            serialCode = box.getItemCode();
        } else if (ValueUtil.isNotEmpty(event.getBarcode())) {
            serialCode = event.getBarcode();
        }

        logger.error(LOG_KEY_FORMAT, equipId, plcSeq, boxId, serialCode, message);
    }

    /* =========================================================
     * GTR(생기연) Inspection API 호출 (비동기)
     * ========================================================= */
    private void invokeInspectionApi(TbMwBox box, String seqNo) {
        try {
            String siteId = "hwaseong/damage-detection";
            InspectionRequestDto requestDto = new InspectionRequestDto();

            requestDto.setTransactionId(seqNo);
            log.info("[GTR_CONN] Inspection API 호출 시작. Seq: {}", seqNo);

            // 3. InspectionService 호출 및 비동기 실행 시작
            inspectionService.requestInspection(siteId, requestDto)
                    .subscribe(
                            resultMap -> log.info("[GTR_CONN] Inspection API 호출 성공. Seq: {}", seqNo),
                            error -> log.error("[GTR_CONN] Inspection API 호출 실패. Seq: {}", seqNo, error)
                    );
        } catch (Exception e) {
            // API 호출 전 DTO 세팅 등에서 발생할 수 있는 에러 방어
            log.error("[GTR_CONN] Inspection API 호출 중 예외 발생. Seq: {}", seqNo, e);
        }
    }
}
