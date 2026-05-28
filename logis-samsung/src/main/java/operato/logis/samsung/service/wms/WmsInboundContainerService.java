package operato.logis.samsung.service.wms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.samsung.consts.InboundStatus;
import operato.logis.samsung.consts.WmsIFCode;
import operato.logis.samsung.dto.wms.InboundDeliveryItemRequest;
import operato.logis.samsung.dto.wms.InboundDeliveryRequest;
import operato.logis.samsung.dto.wms.WmsIFResponse;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.service.mw.InboundImportService;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.List;

/**
 * WMS-WCS 입고 컨테이너 서비스
 * - Legacy 시스템 입고지시 수신 후 TbMwInboundDelivery 엔티티 변환 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundContainerService extends AbstractQueryService {

    private InboundImportService inboundImportService;

    public WmsIFResponse receive(InboundDeliveryRequest request) {
        try {
            validateRequest(request);
            processItems(request);
            return WmsIFResponse.success(request.getRegId(), null);
        } catch (WmsInboundException e) {
            return WmsIFResponse.fail(e.getErrorCode(), request.getRegId(), e.getMessage());
        } catch (Exception e) {
            return WmsIFResponse.fail(WmsIFCode.ERR_SYSTEM, request.getRegId(), "시스템 오류: " + e.getMessage());
        }
    }

    // 입고 상세 로직 - 1. 유효성 검증
    private void validateRequest(InboundDeliveryRequest request) {
        if (request == null) {
            throw new WmsInboundException(WmsIFCode.ERR_INVALID_FORMAT, "요청이 null입니다.");
        }
        if (isBlank(request.getLcId())) {
            throw new WmsInboundException(WmsIFCode.ERR_INVALID_FORMAT, "lcId는 필수입니다.");
        }
        if (isBlank(request.getRegId())) {
            throw new WmsInboundException(WmsIFCode.ERR_MISSING_PARAMS, "regId는 필수입니다.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new WmsInboundException(WmsIFCode.ERR_MISSING_PARAMS, "items는 필수입니다.");
        }

        for (InboundDeliveryItemRequest item : request.getItems()) {
            // todo: I/F협의 후 검증로직 확정
            if (isBlank(item.getCntrNo())) {
                throw new WmsInboundException(WmsIFCode.ERR_INVALID_FORMAT, "cntrNo(컨테이너 번호)는 필수입니다.");
            }
            if (isBlank(item.getBlNo())) {
                throw new WmsInboundException(WmsIFCode.ERR_INVALID_FORMAT, "blNo(B/L 번호)는 필수입니다.");
            }
        }
    }

    // 입고 상세 로직 - 3. 입고 처리
    protected void processItems(InboundDeliveryRequest request) {
        List<TbMwInboundDelivery> entities = new ArrayList<>();
        for (InboundDeliveryItemRequest item : request.getItems()) {
            TbMwInboundDelivery entity = toEntity(request, item);
            entities.add(entity);
        }

        try {
            inboundImportService.insertImportData(entities);
        } catch (Exception e) {
            log.error("[WMS] 저장 실패 - regId={}", request.getRegId());
            throw new WmsInboundException(WmsIFCode.UNKNOWN, "[WMS] 저장 실패 - RegId=" + request.getRegId() + ": " + e.getMessage());
        }
    }

    /**
     * 입고컨테이너 아이템 DTO -> Entity 변환
     * - default 값은 inboundImportService에서 기입
     */
    private TbMwInboundDelivery toEntity(InboundDeliveryRequest req, InboundDeliveryItemRequest item) {
        TbMwInboundDelivery entity = new TbMwInboundDelivery();

        entity.setLcId(req.getLcId());
        entity.setLcNm(req.getLcNm());
        entity.setRemark(req.getRemark());
        entity.setRegId(req.getRegId());
        entity.setRegTime(req.getRegTime() != null ? req.getRegTime().toString() : null);

        entity.setInboundStatus(InboundStatus.READY.value());

        //entity.setInboundSeq(item.getInboundSeq());
        entity.setCntrNo(item.getCntrNo());
        //entity.setDockId(item.getDockId());
        //entity.setCustId(item.getCustId());
        //entity.setCustNm(item.getCustNm());
        entity.setBlNo(item.getBlNo());
        //entity.setInvoice(item.getInvoice());
        entity.setInboundDate(item.getInboundDate()); // YYYY-MM-DD format
        //entity.setLotNo(item.getLotNo());
        //entity.setBoxBarcode(item.getBoxBarcode());
        //entity.setRealOrderNo(item.getRealOrderNo());
        //entity.setItemOrderNo(item.getItemOrderNo());
        entity.setItemCode(item.getItemCode());
        //entity.setOwnerItemCode(item.getOwnerItemCode());
        //entity.setInnerItemCode(item.getInnerItemCode());
        entity.setInnerItemCode(item.getItemCode()); // note: 검토
        //entity.setItemName(item.getItemName());
        entity.setItemType(item.getItemType());
        entity.setItemDesc(item.getItemDesc());
        entity.setItemQty(item.getItemQty());
        //entity.setItemCbm(item.getItemCbm());
        //entity.setItemPriority(item.getItemPriority());
        //entity.setTotalCbm(item.getTotalCbm());
        //entity.setItemPriority(item.getItemPriority());

        return entity;
    }

    /**
     * 유틸
     */

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}