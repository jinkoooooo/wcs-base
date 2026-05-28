package operato.logis.wcs.service.impl.order.issuer;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.consts.HostOrderType;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.consts.UomType;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.service.impl.order.host.HostOrderCreator;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
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
 * 운영자가 Dashboard2D 셀 메뉴에서 트리거하는 샘플 출고 (SAMPLE_OUT) Issuer.
 *
 * ManualOutboundIssuer 와 거의 동일한 흐름이지만 차이점:
 *   - 가시성       : stock_type 이 NORMAL / QC_PENDING / QC_FAIL 인 단일 stock 만 가능
 *   - host 유형    : HostOrderType.SAMPLE_OUTBOUND (baseOrderType=OUTBOUND, subOrderType=SAMPLE_OUT)
 *   - 후속        : 운영자가 PalletWorkstation 에서 채취 → 남은 양은 ReinboundIssuer 가 재입고
 *   - follow_up   : shuttle COMPLETED 시점에 FollowUpPolicy 가 SAMPLE_OUT 인지하여 자동 set
 *   - 포트        : lock 없음. 일반 OUTBOUND 와 동일한 alloc 흐름
 *   - comment 필수: audit log 의 reason 으로 영구 기록
 */
@Service
@RequiredArgsConstructor
public class OperatorSampleOutboundIssuer extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(OperatorSampleOutboundIssuer.class);

    private static final List<StockType> ALLOWED_TYPES =
            List.of(StockType.NORMAL, StockType.QC_PENDING, StockType.QC_FAIL);

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final HostOrderCreator hostOrderCreator;

    /**
     * 샘플 출고 발행 진입점.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> issue(String eqGroupId, String stockId, String portCode, String comment) {
        requireNotEmpty(eqGroupId, "INVALID_PARAMETER", "eqGroupId 가 비어 있습니다.");
        requireNotEmpty(stockId, "INVALID_PARAMETER", "stockId 가 비어 있습니다.");

        // stock 조회 + type 허용 검증
        List<ExtTbInventoryStock> stockList = inventoryStockRepository.findByEqGroupIdAndStockId(eqGroupId, stockId);
        requireFound(stockList, "STOCK_NOT_FOUND", "재고를 찾을 수 없습니다: " + stockId);
        for (ExtTbInventoryStock s : stockList) {
            StockType type = StockType.of(s.getStockType());
            if (!ALLOWED_TYPES.contains(type)) {
                throw new ElidomRuntimeException("STOCK_TYPE_NOT_ALLOWED",
                        "샘플 출고 불가 stock_type=" + type.code() + ", sku=" + s.getSku());
            }
        }
        ExtTbInventoryStock firstStock = stockList.get(0);

        // 위치 ID 조회
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByStockId(eqGroupId, stockId);
        requireFound(loc, "LOCATION_NOT_FOUND", "stockId 의 로케이션을 찾을 수 없습니다: " + stockId);
        String fromLocId = loc.getLocId();

        String hostKey = "UI-SAMPLE-OUT-" + System.currentTimeMillis();

        // HOST 요청 빌드
        HostOrderApi.Request req = HostOrderApi.Request.builder()
                .hostSystemCode("WCS_UI")
                .hostOrderKey(hostKey)
                .orderType(HostOrderType.SAMPLE_OUTBOUND.code())
                .ownerCode(ValueUtil.isNotEmpty(firstStock.getItemOwner()) ? firstStock.getItemOwner() : "OWN001")
                .eqGroupId(firstStock.getEqGroupId())
                .priority(5)
                .fromLocId(fromLocId)
                .toLocId(portCode)
                .items(stockList.stream()
                        .map(s -> HostOrderApi.Item.builder()
                                .itemCode(s.getSku())
                                .lotNo(s.getLotNo())
                                .qty(s.getItemQty())
                                .uom(UomType.EA.code())
                                .build())
                        .toList())
                .build();

        // host_order 생성 — 내부에서 reserveForHostOrder 호출 (STOCK_BUSY 가능)
        TbWcsHostOrder hostOrder;
        try {
            hostOrder = hostOrderCreator.create(req);
        } catch (ElidomRuntimeException e) {
            if ("STOCK_BUSY".equals(e.getCode())) {
                logger.warn("[ Order ][ Issuer ] sample outbound busy - stockId={}, hostKey={}", stockId, hostKey, e);
                throw new ElidomRuntimeException("ALREADY_RESERVED",
                        "이미 출고 지시가 등록된 재고입니다. 화면 새로고침 후 확인하세요.");
            }
            throw e;
        }

        // 운영자 사유 → stock audit log 영구 기록
        String operatorReason = "OPERATOR_SAMPLE_OUTBOUND: " + comment;
        inventoryStockRepository.updateStockStatusAndType(eqGroupId, stockId,
                StockStatus.HOST_PENDING, null, operatorReason);

        logger.info("[ Order ][ Issuer ] sample outbound issued - stockId={}, fromLocId={}, port={}, host={}, status={}, comment={}",
                stockId, fromLocId, portCode, hostKey, hostOrder.getOrderStatus(), comment);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("hostOrderKey", hostKey);
        result.put("hostOrderStatus", hostOrder.getOrderStatus());
        result.put("stockId", stockId);
        result.put("fromLocId", fromLocId);
        result.put("message", "샘플 출고 지시 등록 완료 (채취 후 워크스테이션에서 재입고 트리거 필요)");
        return result;
    }
}
