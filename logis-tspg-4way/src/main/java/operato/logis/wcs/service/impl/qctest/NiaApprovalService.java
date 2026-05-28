package operato.logis.wcs.service.impl.qctest;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.dto.NiaApprovalRequest;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 국검(NIA) 승인 처리.
 *
 * 동작:
 *   1. stockId 로 stock 조회 → origin_host_order_key + sku 로 host_order_item 매칭
 *   2. 멱등성/충돌 검사 (이미 처리됐는지, 다른 승인번호인지)
 *   3. NIA_PENDING 이면 host_order_item.nia_approval_no 저장 + stock_type=NORMAL 전이
 */
@Service
@RequiredArgsConstructor
public class NiaApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(NiaApprovalService.class);

    private final InventoryStockRepository stockRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final InventoryStockStateWriter stockStateWriter;

    /**
     * NIA 승인 처리 — 멱등성/충돌 검사 후 stock_type=NORMAL 로 복원.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyApproval(String eqGroupId, String stockId, NiaApprovalRequest req) {
        // stock 조회
        ExtTbInventoryStock stock = stockRepository.findAnyByStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(stock)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "stock not found");
        }

        String currentType = stock.getStockType();
        String approvalNo = req.getNiaApprovalNo().trim();

        // host_order_item 매칭 (origin_host_order_key + sku)
        TbWcsHostOrderItem item = resolveHostOrderItem(stock);
        if (ValueUtil.isEmpty(item)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "host_order_item not found for stock (originHostOrderKey=%s, sku=%s)"
                            .formatted(stock.getOriginHostOrderKey(), stock.getSku()));
        }

        // 멱등성/충돌 — 이미 NORMAL 인 경우
        if (StockType.NORMAL.code().equals(currentType)) {
            if (ValueUtil.isEqual(item.getNiaApprovalNo(), approvalNo)) {
                // 같은 stockId + 같은 승인번호 재스캔 → no-op
                logger.info("[ Qctest ][ Nia ] approved no-op (idempotent) - stockId={}, approvalNo={}",
                        stockId, approvalNo);
                return buildResponse(stockId, item.getNiaApprovalNo(), currentType, true);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "already approved with different number: existing=%s, incoming=%s"
                            .formatted(item.getNiaApprovalNo(), approvalNo));
        }

        // NIA_PENDING 이외 — 전이 불가
        if (!StockType.NIA_PENDING.code().equals(currentType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "stock_type is not NIA_PENDING: " + currentType);
        }

        // NIA_PENDING → NORMAL 전이 + 승인번호 저장
        item.setNiaApprovalNo(approvalNo);
        hostOrderItemRepository.update(item);

        String reason = "승인번호=" + approvalNo + "; " + req.getComment();
        stockStateWriter.markStockNormalRestored(eqGroupId, stockId, reason);

        logger.info("[ Qctest ][ Nia ] approved - stockId={}, hostOrderKey={}, sku={}, approvalNo={}, comment={}",
                stockId, stock.getOriginHostOrderKey(), stock.getSku(), approvalNo, req.getComment());

        return buildResponse(stockId, approvalNo, StockType.NORMAL.code(), true);
    }

    /**
     * NIA 불승인 처리 — NIA_PENDING → HOLD + NIA_FAIL. 승인번호 미저장, 사유는 audit reason 으로 기록.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyRejection(String eqGroupId, String stockId, NiaApprovalRequest req) {
        // stock 조회
        ExtTbInventoryStock stock = stockRepository.findAnyByStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(stock)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "stock not found");
        }

        String currentType = stock.getStockType();

        // 멱등 — 이미 NIA_FAIL
        if (StockType.NIA_FAIL.code().equals(currentType)) {
            logger.info("[ Qctest ][ Nia ] rejected no-op (idempotent) - stockId={}", stockId);
            return buildResponse(stockId, null, currentType, true);
        }
        // NIA_PENDING 이외 — 전이 불가
        if (!StockType.NIA_PENDING.code().equals(currentType)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "stock_type is not NIA_PENDING: " + currentType);
        }

        // NIA_PENDING → NIA_FAIL 전이 (승인번호 저장 안 함)
        String reason = "국검 불승인; " + req.getComment();
        stockStateWriter.markStockNiaRejected(eqGroupId, stockId, reason);

        logger.warn("[ Qctest ][ Nia ] rejected - stockId={}, hostOrderKey={}, sku={}, comment={}",
                stockId, stock.getOriginHostOrderKey(), stock.getSku(), req.getComment());

        return buildResponse(stockId, null, StockType.NIA_FAIL.code(), true);
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
     * 응답 페이로드 빌더.
     */
    private Map<String, Object> buildResponse(String stockId, String approvalNo, String stockType, boolean success) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stockId", stockId);
        out.put("niaApprovalNo", approvalNo);
        out.put("stockType", stockType);
        out.put("success", success);
        return out;
    }
}
