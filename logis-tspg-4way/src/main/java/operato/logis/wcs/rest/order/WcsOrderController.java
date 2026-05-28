package operato.logis.wcs.rest.order;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.intake.OrderIntakeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.Map;

/**
 * Direct WCS 주문 실행 컨트롤러.
 * 테스트 인프라 setup/clear 는 {@link WcsOrderTestSetupController} (prod 격리) 로 분리되어 있다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/direct-order")
public class WcsOrderController extends AbstractRestService {

    private static final Logger logger = LoggerFactory.getLogger(WcsOrderController.class);

    private final OrderIntakeService wcsOrderService;

    /**
     * Direct 주문 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<HostOrderApi.Response> execute(@RequestBody WcsOrderCommand command) {
        logger.info("[ Order ][ Direct ] execute - hostOrderKey={}, orderType={}",
                command == null ? null : command.getHostOrderKey(),
                command == null ? null : command.getOrderType());

        try {
            HostOrderApi.Response response = wcsOrderService.execute(command);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("[ Order ][ Direct ] execute failed - hostOrderKey={}",
                    command == null ? null : command.getHostOrderKey(), e);
            return ResponseEntity.internalServerError().body(HostOrderApi.Response.fail(null, "ERR_INTERNAL", e.getMessage()));
        }
    }

    /**
     * 로케이션 코드를 이용한 자동 출고 실행 (Map 파라미터 사용)
     */
    @PostMapping("/execute-by-location")
    public ResponseEntity<HostOrderApi.Response> executeByLocation(@RequestBody Map<String, Object> reqMap) {

        // 1. Map에서 파라미터 추출
        String executeType = (String) reqMap.get("executeType");
        String locGroup = (String) reqMap.get("locGroup");
        String fromLocId = (String) reqMap.get("fromLocId");
        String toLocId = (String) reqMap.get("toLocId");

        // priority 파싱 (기본값 5)
        Integer priority = 5;
        if (ValueUtil.isNotEmpty(reqMap.get("priority"))) {
            priority = Integer.valueOf(reqMap.get("priority").toString());
        }

        // 파라미터 검증
        if (ValueUtil.isEmpty(locGroup)) {
            return ResponseEntity.badRequest().body(HostOrderApi.Response.fail(null, "ERR_INVALID_PARAM", "locGroup, locId, toLocId are required."));
        }

        // 2. 로케이션 조회 (ExtTbInventoryLocation 활용)
        ExtTbInventoryLocation fromLoc = ValueUtil.isNotEmpty(fromLocId) ? queryManager.selectByCondition(ExtTbInventoryLocation.class,
                ValueUtil.newMap("locGroup,locId", locGroup, fromLocId)) : null;

        ExtTbInventoryLocation toLoc = ValueUtil.isNotEmpty(toLocId) ? queryManager.selectByCondition(ExtTbInventoryLocation.class,
                ValueUtil.newMap("locGroup,locId", locGroup, toLocId)) : null;

        ExtTbInventoryStock outboundStock = null;

        if(executeType.equals("OUTBOUND") && ValueUtil.isNotEmpty(fromLoc)){
            if(ValueUtil.isNotEmpty(fromLoc.getStockId())){
                outboundStock = queryManager.selectByCondition(ExtTbInventoryStock.class,
                        ValueUtil.newMap("stockId", fromLoc.getStockId()));
            }
        }

        // 4. WcsOrderCommand 자동 생성
        WcsOrderCommand command = WcsOrderCommand.builder()
                .hostSystemCode("DIRECT")
                .persistHostOrder(true)
                .orderType(executeType)
                .eqGroupId(locGroup)
                .fromLocId(ValueUtil.isNotEmpty(fromLoc) ? fromLoc.getLocId() : null)
                .toLocId(ValueUtil.isNotEmpty(toLoc) ? toLoc.getLocId() : null)
                .ownerCode(outboundStock.getItemOwner())
                .priority(priority)
                .items(Collections.singletonList(
                        WcsOrderCommand.Item.builder()
                                .itemCode(outboundStock.getItemCode())
                                .lotNo(outboundStock.getLotNo())
                                .qty(outboundStock.getItemQty()) // 현재 재고 전량 출고
                                .uom("EA")
                                .build()
                ))
                .build();

        // 5. 기존 서비스의 execute 호출
        try {
            HostOrderApi.Response response = wcsOrderService.execute(command);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("[ Order ][ Direct ] executeByLocation failed - locGroup={}, executeType={}",
                    locGroup, executeType, e);
            return ResponseEntity.internalServerError().body(HostOrderApi.Response.fail(null, "ERR_INTERNAL", e.getMessage()));
        }
    }

    @Override
    protected Class<?> entityClass() {
        return TbWcsShuttleOrder.class;
    }
}
