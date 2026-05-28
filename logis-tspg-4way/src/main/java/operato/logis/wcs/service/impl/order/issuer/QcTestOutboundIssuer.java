package operato.logis.wcs.service.impl.order.issuer;

import lombok.RequiredArgsConstructor;
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
 * QC 시험 대상 재고 출고 (SAMPLE_OUT) Issuer.
 *
 * OperatorSampleOutboundIssuer 와 동일하게 host_order 만 생성하고,
 * 산출/락/셔틀 생성은 HostOrderJobs 스케줄러의 정식 경로
 * (OrderIntakeService → ShuttleOrderRegistrar → handler.lockForOrder) 에 위임한다.
 *
 * SAMPLE_OUT 과의 차이 — comment 대신 시험 식별자(testRequestNo / testNo) 를 item 에 실어 전달.
 */
@Service
@RequiredArgsConstructor
public class QcTestOutboundIssuer extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(QcTestOutboundIssuer.class);

    private static final List<StockType> ALLOWED_TYPES =
            List.of(StockType.NORMAL, StockType.QC_PENDING, StockType.QC_FAIL);

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final HostOrderCreator hostOrderCreator;

    /**
     * QC 시험 출고 발행 진입점.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> issue(String eqGroupId, String stockId, String portCode,
                                     String testRequestNo, String testNo) {
        requireNotEmpty(eqGroupId, "INVALID_PARAMETER", "eqGroupId 가 비어 있습니다.");
        requireNotEmpty(stockId, "INVALID_PARAMETER", "stockId 가 비어 있습니다.");

        // stock 조회 + type 허용 검증
        List<ExtTbInventoryStock> stockList = inventoryStockRepository.findByEqGroupIdAndStockId(eqGroupId, stockId);
        requireFound(stockList, "STOCK_NOT_FOUND", "재고를 찾을 수 없습니다: " + stockId);
        for (ExtTbInventoryStock s : stockList) {
            StockType type = StockType.of(s.getStockType());
            if (!ALLOWED_TYPES.contains(type)) {
                throw new ElidomRuntimeException("STOCK_TYPE_NOT_ALLOWED",
                        "시험 출고 불가 stock_type=" + type.code() + ", sku=" + s.getSku());
            }
        }
        ExtTbInventoryStock firstStock = stockList.get(0);

        // 위치 ID 조회
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByStockId(eqGroupId, stockId);
        requireFound(loc, "LOCATION_NOT_FOUND", "stockId 의 로케이션을 찾을 수 없습니다: " + stockId);
        String fromLocId = loc.getLocId();

        String hostKey = "UI-QC-OUT-" + System.currentTimeMillis();

        // HOST 요청 빌드 — items 에 testRequired/testRequestNo/testNo 동봉
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
                                .testRequired(Boolean.TRUE)
                                .testRequestNo(testRequestNo)
                                .testNo(testNo)
                                .build())
                        .toList())
                .build();

        // host_order 생성 — 내부에서 reserveForHostOrder 호출 (STOCK_BUSY 가능)
        TbWcsHostOrder hostOrder;
        try {
            hostOrder = hostOrderCreator.create(req);
        } catch (ElidomRuntimeException e) {
            if ("STOCK_BUSY".equals(e.getCode())) {
                logger.warn("[ Order ][ Issuer ] qc test outbound busy - stockId={}, hostKey={}",
                        stockId, hostKey, e);
                throw new ElidomRuntimeException("ALREADY_RESERVED",
                        "이미 출고 지시가 등록된 재고입니다. 화면 새로고침 후 확인하세요.");
            }
            throw e;
        }

        logger.info("[ Order ][ Issuer ] qc test outbound issued - stockId={}, fromLocId={}, port={}, host={}, status={}, testReq={}",
                stockId, fromLocId, portCode, hostKey, hostOrder.getOrderStatus(), testRequestNo);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("hostOrderKey", hostKey);
        result.put("hostOrderStatus", hostOrder.getOrderStatus());
        result.put("stockId", stockId);
        result.put("fromLocId", fromLocId);
        result.put("message", "시험 출고 지시 등록 완료 (채취 후 워크스테이션에서 재입고 트리거 필요)");
        return result;
    }
}
