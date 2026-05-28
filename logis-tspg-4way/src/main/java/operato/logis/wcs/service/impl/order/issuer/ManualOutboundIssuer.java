package operato.logis.wcs.service.impl.order.issuer;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.consts.HostOrderType;
import operato.logis.wcs.consts.OrderType;
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
 * 운영자 수동 출고 Issuer.
 * stock_type 에 따라 RETURN_OUTBOUND / DISPOSAL_OUTBOUND / OUTBOUND 로 host_order 분기 생성.
 */
@Service
@RequiredArgsConstructor
public class ManualOutboundIssuer extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ManualOutboundIssuer.class);

    private static final List<StockType> ALLOWED_TYPES =
            List.of(StockType.NORMAL, StockType.RETURN, StockType.DISPOSAL);

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final HostOrderCreator hostOrderCreator;

    /**
     * 운영자 수동 출고 발행 진입점.
     * comment 는 stock audit log 의 reason 으로 영구 기록.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> issueManualOutbound(String eqGroupId, String stockId, String portCode, String comment) {
        requireNotEmpty(eqGroupId, "INVALID_PARAMETER", "eqGroupId 가 비어 있습니다.");
        requireNotEmpty(stockId, "INVALID_PARAMETER", "stockId 가 비어 있습니다.");

        // stock 조회 + type 허용 검증
        List<ExtTbInventoryStock> stockList = inventoryStockRepository.findByEqGroupIdAndStockId(eqGroupId, stockId);
        requireFound(stockList, "STOCK_NOT_FOUND", "재고를 찾을 수 없습니다: " + stockId);
        for (ExtTbInventoryStock s : stockList) {
            StockType type = StockType.of(s.getStockType());
            if (!ALLOWED_TYPES.contains(type)) {
                throw new ElidomRuntimeException("STOCK_TYPE_NOT_ALLOWED",
                        "출고 불가 stock_type=" + type.code() + ", sku=" + s.getSku());
            }
        }
        ExtTbInventoryStock firstStock = stockList.get(0);

        // 위치 ID 조회
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByStockId(eqGroupId, stockId);
        requireFound(loc, "LOCATION_NOT_FOUND", "stockId 의 로케이션을 찾을 수 없습니다: " + stockId);
        String fromLocId = loc.getLocId();

        // stock_type 에 따른 host_order_type 분기
        //   RETURN   → RETURN_OUTBOUND   (sub=RETURN_OUT, 박스 스캔 생략)
        //   DISPOSAL → DISPOSAL_OUTBOUND (sub=DISPOSAL_OUT, 박스 스캔 생략 + 논리 삭제)
        //   NORMAL   → OUTBOUND          (sub=null, NORMAL 스캔 흐름)
        String orderTypeCode = switch (StockType.of(firstStock.getStockType())) {
            case RETURN   -> HostOrderType.RETURN_OUTBOUND.code();
            case DISPOSAL -> HostOrderType.DISPOSAL_OUTBOUND.code();
            default       -> OrderType.OUTBOUND.codeAsString();
        };

        String hostKey = "UI-MANUAL-OUT-" + System.currentTimeMillis();

        // HOST 요청 빌드
        HostOrderApi.Request req = HostOrderApi.Request.builder()
                .hostSystemCode("WCS_UI")
                .hostOrderKey(hostKey)
                .orderType(orderTypeCode)
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
                logger.warn("[ Order ][ Issuer ] manual outbound busy - stockId={}, hostKey={}", stockId, hostKey, e);
                throw new ElidomRuntimeException("ALREADY_RESERVED",
                        "이미 출고 지시가 등록된 재고입니다. 화면 새로고침 후 확인하세요.");
            }
            throw e;
        }

        // 운영자 사유를 stock audit log 에 영구 기록 (값 변경 없는 update — audit 만 생성)
        String operatorReason = "OPERATOR_MANUAL_OUTBOUND: " + comment;
        inventoryStockRepository.updateStockStatusAndType(eqGroupId, stockId,
                StockStatus.HOST_PENDING, null, operatorReason);

        logger.info("[ Order ][ Issuer ] manual outbound issued - stockId={}, fromLocId={}, port={}, host={}, status={}, comment={}",
                stockId, fromLocId, portCode, hostKey, hostOrder.getOrderStatus(), comment);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("hostOrderKey", hostKey);
        result.put("hostOrderStatus", hostOrder.getOrderStatus());
        result.put("stockId", stockId);
        result.put("fromLocId", fromLocId);
        result.put("message", "수동 출고 지시 등록 완료");
        return result;
    }
}
