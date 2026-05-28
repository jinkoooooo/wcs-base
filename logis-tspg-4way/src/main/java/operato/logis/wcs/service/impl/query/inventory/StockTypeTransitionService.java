package operato.logis.wcs.service.impl.query.inventory;

import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsQcTestRequest;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import operato.logis.wcs.service.repository.QcTestRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard2D 셀 액션 — 재고 카테고리(stock_type) 전이 처리.
 *
 * 호출: 운영자가 셀 클릭 메뉴에서 폐기/반품/국검 승인·미승인 등의 액션을 누르면 진입.
 *
 * comment 는 컨트롤러에서 CommentValidator 로 이미 검증된 후 본 서비스로 들어온다.
 * testRequestNo 는 to=NIA_PENDING 인 경우 선택적으로 받아 audit 에 매칭 결과를 기록.
 */
@Service
@RequiredArgsConstructor
public class StockTypeTransitionService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(StockTypeTransitionService.class);

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryStockStateWriter stockStateWriter;
    private final QcTestRequestRepository qcTestRequestRepository;

    /**
     * stock_type 전이.
     *
     * 분기:
     *   - DISPOSAL → stock_status=HOLD, stock_type=DISPOSAL
     *   - RETURN   → stock_status=HOLD, stock_type=RETURN
     *   - NORMAL   → stock_status=IDLE, stock_type=NORMAL (국검 승인 등)
     *   - NIA_PENDING → stock_status=HOLD, stock_type=NIA_PENDING (국검 미승인 복귀 등)
     *   - NIA_FAIL → stock_status=HOLD, stock_type=NIA_FAIL (국검 불승인)
     *   - QC_PENDING / QC_FAIL → stock_status=HOLD, stock_type=대상
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> transition(String stockId, String toCode, String comment, String testRequestNo) {
        // 입력 검증
        requireNotEmpty(stockId, "INVALID_PARAMETER", "stockId 가 비어 있습니다.");
        StockType to = StockType.from(toCode);
        if (ValueUtil.isEmpty(to)) {
            throw new ElidomRuntimeException("INVALID_STOCK_TYPE", "지원하지 않는 stock_type: " + toCode);
        }

        // stock 조회 (eqGroupId 미지정 → 안전망 SQL 로 확보)
        ExtTbInventoryStock stock = inventoryStockRepository.findAnyByStockId(null, stockId);
        if (ValueUtil.isEmpty(stock)) {
            String sql = "SELECT eq_group_id FROM tb_inventory_stock WHERE stock_id = :stockId";
            Map<String, Object> p = ValueUtil.newMap("stockId", stockId);
            @SuppressWarnings("rawtypes")
            List<Map> rows = this.queryManager.selectListBySql(sql, p, Map.class, 0, 1);
            requireFound(rows, "STOCK_NOT_FOUND", "재고를 찾을 수 없습니다: " + stockId);
            String eqGroupId = String.valueOf(rows.get(0).get("eq_group_id"));
            stock = inventoryStockRepository.findAnyByStockId(eqGroupId, stockId);
            requireFound(stock, "STOCK_NOT_FOUND", "재고를 찾을 수 없습니다: " + stockId);
        }

        // audit reason 구성 + 분기별 StateWriter 호출
        String reasonText = buildReason(to, comment, testRequestNo, stock);

        int updated = switch (to) {
            case DISPOSAL    -> stockStateWriter.markStockDisposalRequested(stock.getEqGroupId(), stockId, reasonText);
            case RETURN      -> stockStateWriter.markStockReturnRequested(stock.getEqGroupId(), stockId, reasonText);
            case NORMAL      -> stockStateWriter.markStockNormalRestored(stock.getEqGroupId(), stockId, reasonText);
            case NIA_PENDING -> stockStateWriter.markStockNiaPendingRestored(stock.getEqGroupId(), stockId, reasonText);
            case QC_PENDING  -> stockStateWriter.markStockQcPendingManual(stock.getEqGroupId(), stockId, reasonText);
            case QC_FAIL     -> stockStateWriter.markStockQcFailedManual(stock.getEqGroupId(), stockId, reasonText);
            case NIA_FAIL    -> stockStateWriter.markStockNiaRejected(stock.getEqGroupId(), stockId, reasonText);
        };
        StockStatus nextStatus = (to == StockType.NORMAL) ? StockStatus.IDLE : StockStatus.HOLD;

        logger.info("[ Inventory ][ StockType ] transition - stockId={}, eqGroupId={}, to={}, status={}, updatedRows={}",
                stockId, stock.getEqGroupId(), to.code(), nextStatus.value(), updated);

        Map<String, Object> result = new HashMap<>();
        result.put("success", updated > 0);
        result.put("stockId", stockId);
        result.put("eqGroupId", stock.getEqGroupId());
        result.put("stockType", to.code());
        result.put("stockStatus", nextStatus.value());
        result.put("updatedRows", updated);
        return result;
    }

    /**
     * audit reason 텍스트 생성. NIA_PENDING 전환 시 의뢰번호 매칭 결과를 함께 기록.
     * 형식 (NIA_PENDING): "의뢰=QC-...; 매칭=OK|미매칭|미입력; 사유=..."
     * 형식 (그 외): comment 원문.
     */
    private String buildReason(StockType to, String comment, String testRequestNo, ExtTbInventoryStock stock) {
        if (to != StockType.NIA_PENDING) {
            return comment;
        }
        String matchTag;
        String requestTag;
        if (ValueUtil.isEmpty(testRequestNo)) {
            requestTag = "(미입력)";
            matchTag = "미입력";
        } else {
            TbWcsQcTestRequest found = qcTestRequestRepository.findByTestRequestNo(testRequestNo.trim());
            requestTag = testRequestNo.trim();
            matchTag = ValueUtil.isEmpty(found) ? "미매칭" : "OK";
        }
        return "의뢰=" + requestTag + "; 매칭=" + matchTag + "; 사유=" + comment;
    }
}
