package operato.logis.wcs.service.impl.qctest;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.QcTestStatus;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsQcTestRequest;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QC 누락(오기입) 보정 — QC 대상인데 비-QC 로 등록되어 NORMAL 로 굳은 재고를 QC 대기로 되돌린다.
 *
 * 한 호출로 재고·item·의뢰 마스터·주문상태를 모두 정정한다:
 *   stock NORMAL → HOLD+QC_PENDING, item.test_required/test_status 보정,
 *   qc_test_request DRAFT 생성·연계, 완료된 host_order 는 INBOUND_TEST_WAIT 로 재오픈.
 */
@Service
@RequiredArgsConstructor
public class QcCorrectionService {

    private static final Logger logger = LoggerFactory.getLogger(QcCorrectionService.class);

    private final InventoryStockRepository stockRepository;
    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final InventoryStockStateWriter stockStateWriter;
    private final HostOrderStateWriter hostOrderStateWriter;
    private final QcRequestService qcRequestService;

    /**
     * 누락 보정 진입점 — stockId 기준.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> correctToQcPending(String eqGroupId, String stockId, String comment) {
        // stock 조회
        ExtTbInventoryStock stock = stockRepository.findAnyByStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(stock)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "stock not found");
        }

        // 이미 QC 카테고리면 멱등 no-op
        String currentType = stock.getStockType();
        if (StockType.QC_PENDING.code().equals(currentType) || StockType.QC_FAIL.code().equals(currentType)) {
            logger.warn("[ Qctest ][ Correction ] already qc category (idempotent) - stockId={}, type={}",
                    stockId, currentType);
            return buildResponse(stockId, currentType, false);
        }
        // NORMAL 이외(격리상태)는 전이 불가
        if (!StockType.NORMAL.code().equals(currentType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "stock_type is not NORMAL: " + currentType);
        }

        // host_order_item 매칭 (origin_host_order_key + sku)
        TbWcsHostOrderItem item = resolveHostOrderItem(stock);
        if (ValueUtil.isEmpty(item)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "host_order_item not found for stock (originHostOrderKey=%s, sku=%s)"
                            .formatted(stock.getOriginHostOrderKey(), stock.getSku()));
        }

        // host_order 조회 (입고일·재오픈용)
        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(stock.getOriginHostOrderKey());
        if (ValueUtil.isEmpty(host)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "host_order not found: " + stock.getOriginHostOrderKey());
        }

        // 의뢰 마스터 보장 — 없으면 DRAFT 생성, test_request_no 연계
        String requestNo = ensureQcRequest(host, item);

        // item 보정 — 시험 대상 + 의뢰됨 (지정 필드만 persist)
        Date now = new Date();
        item.setTestRequired(Boolean.TRUE);
        item.setTestStatus(QcTestStatus.REQUESTED.code());
        item.setTestRequestedAt(now);
        item.setTestResultedAt(null);
        item.setTestReason(null);
        item.setTestRequestNo(requestNo);
        hostOrderItemRepository.update(item, "testRequired", "testStatus",
                "testRequestedAt", "testResultedAt", "testReason", "testRequestNo");

        // host_order 재오픈 — 완료(COMPLETED)였으면 INBOUND_TEST_WAIT 로
        reopenHostIfCompleted(host);

        // stock 전이 — NORMAL → HOLD + QC_PENDING (audit reason)
        String reason = ValueUtil.isEmpty(comment) ? "QC 누락 보정" : comment;
        stockStateWriter.markStockQcPendingManual(eqGroupId, stockId, reason);

        logger.warn("[ Qctest ][ Correction ] corrected to qc pending - stockId={}, hostOrderKey={}, sku={}, requestNo={}, comment={}",
                stockId, host.getHostOrderKey(), item.getItemCode(), requestNo, comment);

        return buildResponse(stockId, StockType.QC_PENDING.code(), true);
    }

    /**
     * origin_host_order_key + sku 로 host_order_item 매칭. 없으면 null.
     */
    private TbWcsHostOrderItem resolveHostOrderItem(ExtTbInventoryStock stock) {
        if (ValueUtil.isEmpty(stock.getOriginHostOrderKey())) return null;
        List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByHostOrderKey(stock.getOriginHostOrderKey());
        if (ValueUtil.isEmpty(items)) return null;
        for (TbWcsHostOrderItem it : items) {
            if (ValueUtil.isEqual(it.getItemCode(), stock.getSku())) return it;
        }
        return null;
    }

    /**
     * (입고일, sku, lot) 의뢰 마스터 보장 — 있으면 그 requestNo, 없으면 DRAFT 생성.
     */
    private String ensureQcRequest(TbWcsHostOrder host, TbWcsHostOrderItem item) {
        LocalDate inboundDate = resolveInboundDate(host);
        TbWcsQcTestRequest master = qcRequestService.lookup(inboundDate, item.getItemCode(), item.getLotNo());
        if (ValueUtil.isNotEmpty(master)) {
            return master.getTestRequestNo();
        }
        // DRAFT 신규 — 의뢰구분/내용/제조정보는 비워 둠(추후 의뢰 화면에서 채움)
        TbWcsQcTestRequest created = qcRequestService.createWithPdfId(
                inboundDate, item.getItemCode(), item.getLotNo(),
                null,                   // pdfFileId
                null, null,             // testWfType, testReqDesc
                null, null,             // manufacturedDate, expiryDate
                null, item.getQty(),    // manufacturedQty, incomingQty
                null, null);            // reqDept, submitterOrder
        return created.getTestRequestNo();
    }

    /**
     * host_order 입고일 결정 — received_at 기준, 없으면 오늘.
     */
    private LocalDate resolveInboundDate(TbWcsHostOrder host) {
        return ValueUtil.isNotEmpty(host.getReceivedAt())
                ? LocalDateUtils.toLocalDate(host.getReceivedAt())
                : LocalDate.now();
    }

    /**
     * host_order 가 COMPLETED(종결)면 INBOUND_TEST_WAIT 로 재오픈 + 헤더 test_status=REQUESTED.
     */
    private void reopenHostIfCompleted(TbWcsHostOrder host) {
        if (host.getOrderStatus() != HostOrderStatus.COMPLETED.code()) return;
        hostOrderStateWriter.markInboundTestWait(host);
        host.setTestStatus(QcTestStatus.REQUESTED.code());
        host.setTestRequired(Boolean.TRUE);
        hostOrderRepository.update(host, "testStatus", "testRequired");
        logger.warn("[ Qctest ][ Correction ] host reopened to test wait - hostOrderKey={}", host.getHostOrderKey());
    }

    /**
     * 응답 페이로드 빌더.
     */
    private Map<String, Object> buildResponse(String stockId, String stockType, boolean changed) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stockId", stockId);
        out.put("stockType", stockType);
        out.put("success", true);
        out.put("changed", changed);
        return out;
    }
}
