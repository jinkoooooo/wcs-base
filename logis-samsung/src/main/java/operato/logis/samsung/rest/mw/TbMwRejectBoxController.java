package operato.logis.samsung.rest.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.InboundStatus;
import operato.logis.samsung.consts.TrackingStatus;
import operato.logis.samsung.consts.TrackingType;
import operato.logis.samsung.dto.mw.BoxInfoRequest;
import operato.logis.samsung.dto.mw.NoReadManualRejectRequest;
import operato.logis.samsung.dto.mw.RejectRequest;
import operato.logis.samsung.entity.mw.*;
import operato.logis.samsung.service.mw.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.ValueUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tb_mw_reject_box")
@ServiceDesc(description="RejectBox Service API")
public class TbMwRejectBoxController extends AbstractRestService {

    private final TbMwXyzOrderService tbMwXyzOrderService;
    private final TbMwItemMasterService tbMwItemMasterService;
    private final TbMwInboundDeliveryService tbMwInboundDeliveryService;
    private final BoxTrackingService boxTrackingService;

    @Autowired
    private final DemoMwDataService demoMwDataService;


    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(TbMwRejectBoxController.class);

    /** 이미지가 저장된 로컬 디렉토리 (Windows) */

    private static final String REJECT_IMAGE_BASE_PATH = "\\\\192.168.100.151\\Upload";


    @Override
    protected Class<?> entityClass() {
        return TbMwRejectBox.class;
    }

    @RequestMapping(method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name="page", required=false) Integer page,
            @RequestParam(name="limit", required=false) Integer limit,
            @RequestParam(name="select", required=false) String select,
            @RequestParam(name="sort", required=false) String sort,
            @RequestParam(name="query", required=false) String query) {
        return this.search(this.entityClass(), page, limit, select, sort, query);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Find one by ID")
    public TbMwRejectBox findOne(@PathVariable("id") String id) {
        return this.getOne(this.entityClass(), id);
    }

    @RequestMapping(value="/{id}/exist", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check exists By ID")
    public Boolean isExist(@PathVariable("id") String id) {
        return this.isExistOne(this.entityClass(), id);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public TbMwRejectBox update(@PathVariable("id") String id, @RequestBody TbMwRejectBox input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<TbMwRejectBox> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }


    /**
     * 20251210 JJG : 최종 작업자 판단 리젝 여부 결정 컬럼(Final_status) 사용 로직 으로 변경.
     */
    @RequestMapping(value="/reject", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Reject 박스 추가")
    public Boolean reject(@RequestBody RejectRequest request) {
        TbMwBox param = request.getParam();

        // 요청값 없으면 종료
        if (ValueUtil.isEmpty(param) || ValueUtil.isEmpty(param.getBoxId())) {
            return false;
        }

        // 기존 박스 정보 조회
        TbMwBox box = boxTrackingService.findBoxByBoxId(param.getBoxId());

        // 기존 박스 없으면 종료
        if (ValueUtil.isEmpty(box)) {
            return false;
        }

        Integer currentFinalStatus = box.getFinalStatus();
        Integer requestFinalStatus = param.getFinalStatus();

        // 적치 완료(700) 상태는 변경 불가
        if (Objects.equals(TrackingStatus.STORED.getValue(), currentFinalStatus)) {
            return false;
        }

        // 중복 클릭 등으로 같은 상태 재요청 들어온 경우 차단
        if (Objects.equals(TrackingStatus.FINAL_VALID_OK.getValue(), currentFinalStatus)
                && Objects.equals(TrackingStatus.FINAL_VALID_OK.getValue(), requestFinalStatus)) {
            return false;
        }

        if (Objects.equals(TrackingStatus.FINAL_VALID_NG.getValue(), currentFinalStatus)
                && Objects.equals(TrackingStatus.FINAL_VALID_NG.getValue(), requestFinalStatus)) {
            return false;
        }

        // 기존 상태 기준으로 신규 Reject / Reject 원복 여부 먼저 계산
        boolean isNewReject = !Objects.equals(TrackingStatus.FINAL_VALID_NG.getValue(), currentFinalStatus)
                && Objects.equals(TrackingStatus.FINAL_VALID_NG.getValue(), requestFinalStatus);

        boolean isRejectCancel = Objects.equals(TrackingStatus.FINAL_VALID_NG.getValue(), currentFinalStatus)
                && Objects.equals(TrackingStatus.FINAL_VALID_OK.getValue(), requestFinalStatus);

        // 박스 상태 업데이트
        box.setFinalRemark(param.getFinalRemark());
        box.setFinalStatus(requestFinalStatus);
        box.setRejectType(param.getRejectType());
        box.setRejectDesc(param.getFinalRemark());
        box.setFinalAt(new Date());

        // 작업자 히스토리 스냅샷 추가
        boxTrackingService.insertTrackSnapshot(
                box,
                box.getPlcSeqNo(),
                "REJECT",
                "PC",
                new Date(),
                TrackingType.VALIDATION,
                TrackingStatus.fromValue(requestFinalStatus)
        );

        // 박스 상태 업데이트 저장
        boxTrackingService.updateTbMwBox(box);

        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(param.getItemCode());
        if (ValueUtil.isEmpty(itemMaster)) {
            return null;
        }

        // 진행중인 모든 적재오더 조회
        List<TbMwXyzOrder> orderList = tbMwXyzOrderService.getActiveAndPendingOrderList(param.getItemCode());
        if (ValueUtil.isEmpty(orderList)) {
            return null;
        }

        int palletCapacity = Integer.parseInt(itemMaster.getPalletCapacity());

        TbMwXyzOrder targetOrder = orderList.get(0);

        if (orderList.size() > 1) {
            for (TbMwXyzOrder order : orderList) {
                int remainQtyInPallet = (order.getTargetNum() - order.getNgQty()) % palletCapacity;
                if (remainQtyInPallet > 0) {
                    targetOrder = order;
                    break;
                }
            }
        }

        // 새로운 Reject일 경우 NG 수량 반영
        if (isNewReject) {
            tbMwXyzOrderService.updateResultQty(targetOrder, 0, 1);
        }
        // Reject 원복일 경우 NG 수량 차감
        else if (isRejectCancel) {
            tbMwXyzOrderService.updateResultQty(targetOrder, 0, -1);
        }

        return true;
    }

    @RequestMapping(value="/get_box_info/{barcode}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="특정 Box 정보 조회")
    public TbMwRejectBox getBoxInfo(@PathVariable("barcode") String barcode) {
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(barcode);
        if (ValueUtil.isEmpty(itemMaster)) {
            return null;
        }

        TbMwInboundDelivery delivery = tbMwInboundDeliveryService.getRunningInboundDeliveryByItemCode(itemMaster.getItemCode(), InboundStatus.RUNNING);
        if (ValueUtil.isEmpty(delivery)) {
            return null;
        }

        TbMwRejectBox rejectBox = new TbMwRejectBox();
        rejectBox.setInnerItemCode(itemMaster.getInnerItemCode());
        rejectBox.setItemCode(itemMaster.getItemCode());
        rejectBox.setItemName(itemMaster.getItemName());
        rejectBox.setCntrNo(delivery.getCntrNo());
        rejectBox.setInboundDate(delivery.getInboundDate());
        return rejectBox;
    }

    @RequestMapping(
            value = "/get_box_info",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "특정 Box 정보 조회(바코드/시리얼/리젝타입 포함)")
    public List<TbMwBox> getBoxInfo(@RequestBody BoxInfoRequest req) {

        String dateMode = req.getDateMode();     // day | range
        String date = req.getDate();             // 하루치 조회용
        String startDate = req.getStartDate();   // 기간 조회용
        String endDate = req.getEndDate();       // 기간 조회용
        String barcode = req.getBarcode();
        String serial = req.getSerial();
        String rejectType = req.getRejectType();


        // 조회 모드가 비어 있으면 기본값은 하루치
        if (ValueUtil.isEmpty(dateMode)) {
            dateMode = "day";
        }

        // 1) 기간 조회
        if ("range".equalsIgnoreCase(dateMode)) {
            if (ValueUtil.isEmpty(startDate) || ValueUtil.isEmpty(endDate)) {
                throw new ElidomRuntimeException("기간 조회 시 시작일과 종료일은 반드시 입력해야 합니다.");
            }

            // boxTrackingService 를 통해 조건에 맞는 TbMwBox 리스트 조회

            return boxTrackingService.findBoxesByCondition(
                    date,
                    dateMode,
                    startDate,
                    endDate,
                    barcode,
                    serial,
                    rejectType
            );
        }

        // 2) 하루치 조회
        if (ValueUtil.isEmpty(date)) {
            throw new ElidomRuntimeException("하루 조회 시 날짜는 반드시 입력해야 합니다.");
        }

        // boxTrackingService 를 통해 조건에 맞는 TbMwBox 리스트 조회
        return boxTrackingService.findBoxesByCondition(
                date,
                dateMode,
                startDate,
                endDate,
                barcode,
                serial,
                rejectType
        );
    }

    /**
     * 리젝 박스 이미지 조회
     * ex) GET /rest/tb_mw_reject_box/image/20251125140853_1706_TP_01.jpg
     */
    @RequestMapping(
            value = "/image/{fileName:.+}",
            method = RequestMethod.GET
    )
    public ResponseEntity<Resource> getRejectImage(@PathVariable("fileName") String fileName) {

        try {
            // 1) 파일명에서 날짜 폴더 추출 (예: 2025-11-25)
            String dateFolder = extractDateFolder(fileName);
            if (dateFolder == null) {
                logger.warn("잘못된 파일명(날짜 추출 실패): {}", fileName);
                return ResponseEntity.badRequest().build();
            }

            // 2) 기본 경로
            Path basePath = Paths.get(REJECT_IMAGE_BASE_PATH)
                    .toAbsolutePath()
                    .normalize();

            // 3) 날짜 폴더까지 포함한 경로
            Path datePath = basePath.resolve(dateFolder).normalize();

            // 4) 최종 파일 경로
            Path targetPath = datePath.resolve(fileName).normalize();

            // 5) 디렉토리 탈출(../ 등) 방지
            if (!targetPath.startsWith(basePath)) {
                logger.warn("잘못된 이미지 경로 접근: {}", targetPath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FileSystemResource resource = new FileSystemResource(targetPath.toFile());
            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("이미지 파일 없음 또는 읽기 불가: {}", targetPath);
                return ResponseEntity.notFound().build();
            }

            // 6) Content-Type 추론
            String contentType = Files.probeContentType(targetPath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            logger.info("✅ 이미지 전송 => {}", targetPath);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (IOException e) {
            logger.error("이미지 조회 중 오류: fileName={}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 파일명(예: 20251125140853_1706_TP_01.jpg) 에서
     * 앞 8자리(YYYYMMDD)를 뽑아 "YYYY-MM-DD" 폴더 이름으로 변환
     */
    private String extractDateFolder(String fileName) {
        if (fileName == null || fileName.length() < 8) {
            return null;
        }

        String yyyymmdd = fileName.substring(0, 8); // 20251125
        if (!yyyymmdd.matches("\\d{8}")) {
            return null;
        }

        String yyyy = yyyymmdd.substring(0, 4); // 2025
        String mm   = yyyymmdd.substring(4, 6); // 11
        String dd   = yyyymmdd.substring(6, 8); // 25

        return yyyy + "-" + mm + "-" + dd;      // 2025-11-25
    }

    /**
     * 슈트, XYZ 오더 리셋처리
     * 파일럿 테스트 용도
     * @return
     */
    @RequestMapping(value="/dataReset", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="resetData")
    public Object demoBatchCreate() {
        logger.info("dataReset Start");
        return this.demoMwDataService.resetDemoDataSet();
    }

    @RequestMapping(
            value = "/noread_manual_reject",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "NOREAD 박스 수동 정보 보정 및 최종 불량 처리")
    public Boolean noreadManualReject(@RequestBody NoReadManualRejectRequest request) {

        if (ValueUtil.isEmpty(request.getId())) {
            throw new ElidomRuntimeException("박스 ID는 필수입니다.");
        }

        if (ValueUtil.isEmpty(request.getSerialNo())) {
            throw new ElidomRuntimeException("시리얼번호는 필수입니다.");
        }

        if (ValueUtil.isEmpty(request.getBlNo())) {
            throw new ElidomRuntimeException("주문번호는 필수입니다.");
        }

        if (ValueUtil.isEmpty(request.getCntrNo())) {
            throw new ElidomRuntimeException("컨테이너번호는 필수입니다.");
        }

        if (ValueUtil.isEmpty(request.getItemCode())) {
            throw new ElidomRuntimeException("상품코드는 필수입니다.");
        }

        // 중요: 변경될 box_id가 아니라 기존 row.id 기준으로 조회
        TbMwBox box = (TbMwBox) this.getOne(TbMwBox.class, request.getId());

        if (ValueUtil.isEmpty(box)) {
            throw new ElidomRuntimeException("대상 박스 정보를 찾을 수 없습니다.");
        }

        // NOREAD 대상만 처리되도록 방어
        boolean isNoRead =
                "NOREAD".equalsIgnoreCase(box.getRejectType()) ||
                        "NOREAD".equalsIgnoreCase(box.getRejectDesc()) ||
                        String.valueOf(box.getBoxId()).toUpperCase().contains("NOREAD");

        if (!isNoRead) {
            throw new ElidomRuntimeException("NOREAD 박스만 수동 보정 처리할 수 있습니다.");
        }

        // 상품마스터 확인
        TbMwItemMaster itemMaster = tbMwItemMasterService.getItemMaster(request.getItemCode());

        if (ValueUtil.isEmpty(itemMaster)) {
            throw new ElidomRuntimeException("상품마스터에 존재하지 않는 상품코드입니다. itemCode=" + request.getItemCode());
        }

        // 작업자 입력값으로 박스 정보 보정
        box.setBoxId(request.getSerialNo());
        box.setBlNo(request.getBlNo());
        box.setCntrNo(request.getCntrNo());
        box.setItemCode(itemMaster.getInnerItemCode());
        box.setItemName(itemMaster.getItemName());
        box.setManualResult(1);

        // 최종 리젝 처리
        box.setRejectType("외관불량");
        box.setRejectDesc(request.getFinalRemark());
        box.setFinalRemark(request.getFinalRemark());
        box.setFinalStatus(TrackingStatus.FINAL_VALID_NG.getValue());
        box.setFinalAt(new Date());

        // 2026-05-19 No read 시 PLC Seq No 앞에 "END - " 추가
        String currentPlcSeq = box.getPlcSeqNo() != null ? String.valueOf(box.getPlcSeqNo()) : "";
        box.setPlcSeqNo("END-" + currentPlcSeq);

        // 작업자 처리 이력 저장
        boxTrackingService.insertTrackSnapshot(
                box,
                box.getPlcSeqNo(),
                "NOREAD_MANUAL_REJECT",
                "PC",
                new Date(),
                TrackingType.VALIDATION,
                TrackingStatus.FINAL_VALID_NG
        );

        // 박스 업데이트
        boxTrackingService.updateTbMwBox(box);

        // 적재 오더 NG 수량 반영
        List<TbMwXyzOrder> orderList = tbMwXyzOrderService.getActiveAndPendingOrderList(itemMaster.getItemCode());

        if (!ValueUtil.isEmpty(orderList)) {
            TbMwXyzOrder targetOrder = orderList.get(0);
            tbMwXyzOrderService.updateResultQty(targetOrder, 0, 1);
        }

        return true;
    }

}