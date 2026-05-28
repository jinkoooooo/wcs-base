package operato.logis.asrs.core.strategy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.dto.request.InboundLocationSelectRequest;
import operato.logis.asrs.dto.request.OutboundLocationSelectRequest;
import operato.logis.asrs.dto.response.InboundLocationSelectResult;
import operato.logis.asrs.dto.response.OutboundLocationSelectResult;
import operato.logis.asrs.query.strategy.StrategyQueryService;
import operato.logis.asrs.query.strategy.model.InboundLocationCandidateRow;
import operato.logis.asrs.query.strategy.model.OutboundStockCandidateRow;
import xyz.elidom.util.ValueUtil;

/**
 * 입고/출고 위치 선택 전략 코어.
 */
@Service
@RequiredArgsConstructor
public class LocationSelectionStrategyCore {

    private final StrategyQueryService strategyQueryService;

    /**
     * 입고 로케이션 추천.
     *
     * <p>
     * 기본 규칙:
     * - 상품등급과 동일한 로케이션등급 우선
     * - 비어 있는 로케이션만 반환
     * - sort_seq 오름차순 우선
     * </p>
     */
    @Transactional(readOnly = true)
    public InboundLocationSelectResult selectInboundLocation(InboundLocationSelectRequest request) {
        validateInboundRequest(request);

        List<InboundLocationCandidateRow> candidates =
                strategyQueryService.findInboundLocations(request.getAreaCode(), request.getItemCode());

        int limit = request.getLimit() == null || request.getLimit().intValue() <= 0
                ? 10 : request.getLimit().intValue();

        List<InboundLocationCandidateRow> resultRows = new ArrayList<InboundLocationCandidateRow>();
        for (int i = 0; i < candidates.size() && i < limit; i++) {
            resultRows.add(candidates.get(i));
        }

        InboundLocationSelectResult result = new InboundLocationSelectResult();
        result.setAreaCode(request.getAreaCode());
        result.setItemCode(request.getItemCode());
        result.setCandidateCount(Integer.valueOf(resultRows.size()));
        result.setLocations(resultRows);

        return result;
    }

    /**
     * 출고 재고 추천.
     *
     * <p>
     * 기본 규칙:
     * - location_grade 우선
     * - sort_seq 우선
     * - front_priority 우선
     * - 오래된 입고 순 우선
     * </p>
     */
    @Transactional(readOnly = true)
    public OutboundLocationSelectResult selectOutboundStocks(OutboundLocationSelectRequest request) {
        validateOutboundRequest(request);

        List<OutboundStockCandidateRow> candidates =
                strategyQueryService.findOutboundStocks(request.getAreaCode(), request.getItemCode());

        int limit = request.getLimit() == null || request.getLimit().intValue() <= 0
                ? 20 : request.getLimit().intValue();

        List<OutboundStockCandidateRow> resultRows = new ArrayList<OutboundStockCandidateRow>();
        int remainQty = request.getRequiredQty() == null ? Integer.MAX_VALUE : request.getRequiredQty().intValue();

        for (OutboundStockCandidateRow row : candidates) {
            if (resultRows.size() >= limit) {
                break;
            }

            if (row.getAvailableQty() == null || row.getAvailableQty().intValue() <= 0) {
                continue;
            }

            resultRows.add(row);

            if (remainQty != Integer.MAX_VALUE) {
                remainQty -= row.getAvailableQty().intValue();
                if (remainQty <= 0) {
                    break;
                }
            }
        }

        OutboundLocationSelectResult result = new OutboundLocationSelectResult();
        result.setAreaCode(request.getAreaCode());
        result.setItemCode(request.getItemCode());
        result.setRequiredQty(request.getRequiredQty());
        result.setCandidateCount(Integer.valueOf(resultRows.size()));
        result.setStocks(resultRows);

        return result;
    }

    private void validateInboundRequest(InboundLocationSelectRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("InboundLocationSelectRequest is null.");
        }
        if (ValueUtil.isEmpty(request.getAreaCode())) {
            throw new IllegalArgumentException("areaCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getItemCode())) {
            throw new IllegalArgumentException("itemCode is empty.");
        }
    }

    private void validateOutboundRequest(OutboundLocationSelectRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("OutboundLocationSelectRequest is null.");
        }
        if (ValueUtil.isEmpty(request.getAreaCode())) {
            throw new IllegalArgumentException("areaCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getItemCode())) {
            throw new IllegalArgumentException("itemCode is empty.");
        }
    }
}