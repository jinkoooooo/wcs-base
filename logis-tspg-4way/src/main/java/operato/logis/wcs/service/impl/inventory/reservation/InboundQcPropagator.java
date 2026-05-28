package operato.logis.wcs.service.impl.inventory.reservation;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * QC 결과를 stock 카테고리로 전파한다.
 *
 * 전이 정책:
 *   - allPassed && nia_required=false → IDLE + NORMAL
 *   - allPassed && nia_required=true  → HOLD + NIA_PENDING
 *   - anyFailed                       → HOLD + QC_FAIL (자동 라우팅 없음, 운영자 결정)
 */
@Service
@RequiredArgsConstructor
public class InboundQcPropagator {

    private static final Logger logger = LoggerFactory.getLogger(InboundQcPropagator.class);

    private final InventoryStockRepository stockRepository;
    private final HostOrderRepository hostOrderRepository;
    private final InventoryStockStateWriter stockStateWriter;

    /**
     * QC_PENDING 상태의 stock 들을 결과에 따라 상태 전이시킨다.
     * 호스트 오더의 nia_required 플래그가 통과 케이스 분기를 결정한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int propagateQcResultToStocks(String hostOrderKey, boolean allPassed, boolean anyFailed) {

        // 입력값 검증
        if (ValueUtil.isEmpty(hostOrderKey)) return 0;
        if (!allPassed && !anyFailed) return 0;

        // QC_PENDING 상태의 stock 조회
        List<Map> rows = stockRepository.findStockIdsByOriginHostOrderKeyAndType(
                hostOrderKey, StockType.QC_PENDING.code());
        if (ValueUtil.isEmpty(rows)) return 0;

        // 통과 + 국검 필요 여부 판단
        boolean nia = !anyFailed && isNiaRequiredForHostOrder(hostOrderKey);

        // stockId 중복 제거하며 결과별 상태 전이
        int total = 0;
        Set<String> seen = new HashSet<>();
        for (Map row : rows) {
            String eqGroupId = String.valueOf(row.get("eq_group_id"));
            String stockId = String.valueOf(row.get("stock_id"));
            if (!seen.add(eqGroupId + "::" + stockId)) continue;

            // 결과별 카테고리 전이
            if (anyFailed) {
                total += stockStateWriter.markStockQcFailed(eqGroupId, stockId);
            } else if (nia) {
                total += stockStateWriter.markStockQcPassedAwaitingNia(eqGroupId, stockId);
            } else {
                total += stockStateWriter.markStockQcPassed(eqGroupId, stockId);
            }
        }
        logger.info("[ Inventory ][ Reserve ] qc propagated - host={}, allPassed={}, anyFailed={}, nia={}, rows={}",
                hostOrderKey, allPassed, anyFailed, nia, total);
        return total;
    }

    /**
     * 해당 host_order 의 nia_required 여부를 조회한다.
     * WcsInboundOrderHandler.handleCompletion 의 finalize 분기에서도 사용.
     */
    public boolean isNiaRequiredForHostOrder(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) return false;
        TbWcsHostOrder hostOrder = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        return ValueUtil.isNotEmpty(hostOrder) && Boolean.TRUE.equals(hostOrder.getNiaRequired());
    }
}
