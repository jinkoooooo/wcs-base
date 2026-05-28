package operato.logis.wcs.facade;


import operato.logis.wcs.consts.*;
import operato.logis.wcs.dto.*;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.handler.WcsOrderHandler;
import operato.logis.wcs.service.impl.order.host.HostOperationalDataValidator;
import operato.logis.wcs.service.impl.order.host.HostOrderCreator;
import operato.logis.wcs.service.impl.order.host.HostOrderFormatValidator;
import operato.logis.wcs.service.impl.ecs.EcsCallbackProcessor;
import operato.logis.wcs.service.impl.allocation.port.PortService;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import java.util.List;

/**
 * WCS 주문 수신·ECS 콜백 오케스트레이션 진입점.
 *
 * 형식·운영 데이터 검증을 거쳐 주문 생성 창구로 위임하고,
 * ECS 콜백은 주문 타입별 핸들러로 라우팅한다.
 */
@Service
@RequiredArgsConstructor
public class Tspg4WayShuttleWcsFacade {

    private static final Logger logger = LoggerFactory.getLogger(Tspg4WayShuttleWcsFacade.class);

    private final List<WcsOrderHandler> orderHandlers;
    private final HostOrderFormatValidator formatValidator;
    private final HostOperationalDataValidator operationalValidator;
    private final EcsCallbackProcessor ecsCallbackProcessor;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;
    private final HostOrderCreator hostOrderCreator;
    private final PortService portService;

    /**
     * HOST 주문 수신. 형식·중복·가용성 사전 검증 후 생성 창구로 위임한다.
     * 실제 산출은 스케줄러가 READY_FOR_ALLOC 주문을 집어 처리한다.
     */
    public HostOrderApi.Response receiveHostOrder(HostOrderApi.Request request) {
        logger.info("[ Order ][ Host ] receive start - hostOrderKey={}", request == null ? null : request.getHostOrderKey());

        // 사전 점검 — 정규화 + 형식/멱등성/운영 데이터 검증
        formatValidator.normalize(request);
        String fmtError = formatValidator.validate(request);
        if (ValueUtil.isNotEmpty(fmtError)) {
            logger.warn("[ Order ][ Host ] format rejected - hostOrderKey={}, errorDesc={}",
                    request.getHostOrderKey(), fmtError);
            return HostOrderApi.Response.fail(
                    request.getHostOrderKey(), WcsError.INVALID_REQUEST.code(), fmtError);
        }

        TbWcsHostOrder existing = hostOrderCreator.findExisting(request);
        if (ValueUtil.isNotEmpty(existing)) {
            logger.info("[ Order ][ Host ] duplicate skipped - hostOrderKey={}, wcsOrderKey={}",
                    existing.getHostOrderKey(), existing.getWcsOrderKey());
            return HostOrderApi.Response.duplicate(
                    existing.getWcsOrderKey(), existing.getHostOrderKey());
        }

        // 가용성 사전 검증 — 실패 시 pattern matching 으로 코드/설명 추출
        HostOperationalDataValidator.FeasibilityResult feasibility = operationalValidator.check(request);
        if (feasibility instanceof HostOperationalDataValidator.FeasibilityResult.Failure f) {
            logger.warn("[ Order ][ Host ] feasibility rejected - hostOrderKey={}, errorCode={}, errorDesc={}",
                    request.getHostOrderKey(), f.errorCode(), f.errorDesc());
            return HostOrderApi.Response.fail(
                    request.getHostOrderKey(), f.errorCode(), f.errorDesc());
        }

        // 주문 생성 단일 창구 — persist + 외부 알림 + 초기 상태 결정
        HostOrderApi.Request createReq = toCreateRequest(request);
        TbWcsHostOrder hostOrder = hostOrderCreator.create(createReq);

        logger.info("[ Order ][ Host ] receive completed - hostOrderKey={}, initialStatus={}",
                hostOrder.getHostOrderKey(), hostOrder.getOrderStatus());
        return HostOrderApi.Response.success(null, request.getHostOrderKey());
    }

    /**
     * 외부 HOST 수신 요청을 생성 창구 공통 DTO 로 변환.
     */
    private HostOrderApi.Request toCreateRequest(HostOrderApi.Request src) {
        HostOrderApi.Request dst = new HostOrderApi.Request();
        dst.setHostSystemCode(src.getHostSystemCode());
        dst.setHostOrderKey(src.getHostOrderKey());
        dst.setOrderType(src.getOrderType());
        dst.setEqGroupId(src.getEqGroupId());
        dst.setOwnerCode(src.getOwnerCode());
        dst.setFromLocId(src.getFromLocId());
        dst.setToLocId(src.getToLocId());
        dst.setBarcode(src.getBarcode());
        dst.setPriority(src.getPriority());
        dst.setRawPayload(src.getRawPayload());
        dst.setScheduledDate(src.getScheduledDate());
        dst.setTestRequired(src.getTestRequired());
        dst.setItems(src.getItems());
        return dst;
    }

    /**
     * ECS 콜백 수신. 주문 조회·멱등 검사 후 타입별 핸들러로 위임한다.
     */
    public EcsCallbackApi.Response processEcsCallback(EcsCallbackApi.Request request) {
        logger.info("[ Ecs ][ Callback ] receive start - orderKey={}, status={}, errorCode={}",
                request == null ? null : request.getOrderKey(),
                request == null ? null : request.getStatus(),
                request == null ? null : request.getErrorCode());

        // 필수값 검증
        if (ValueUtil.isEmpty(request) || !StringUtils.hasText(request.getOrderKey())) {
            return EcsCallbackApi.Response.fail(WcsError.INVALID_REQUEST.codeAsString(), "orderKey is required");
        }

        // 대상 주문 조회
        TbWcsShuttleOrder shuttleOrder = shuttleOrderRepository.findByOrderKey(request.getOrderKey());
        if (ValueUtil.isEmpty(shuttleOrder)) {
            return EcsCallbackApi.Response.fail(WcsError.ORDER_NOT_FOUND.codeAsString(), "Order not found");
        }

        // 멱등 처리 — 이미 종료된 주문은 성공으로 즉시 반환
        if (ShuttleOrderStatus.isFinalStatus(shuttleOrder.getOrderStatus())) {
            return EcsCallbackApi.Response.success("Order already processed (idempotent)");
        }

        List<TbWcsShuttleOrderItem> items = shuttleOrderItemRepository.findByOrderKey(shuttleOrder.getOrderKey());

        // 주문 타입별 핸들러 라우팅
        WcsOrderHandler handler = findHandler(shuttleOrder.getOrderType());
        if (ValueUtil.isEmpty(handler)) {
            return EcsCallbackApi.Response.fail(WcsError.INVALID_ORDER_TYPE.codeAsString(), "No handler for order type: " + shuttleOrder.getOrderType());
        }

        // 콜백 트랜잭션 처리는 EcsCallbackProcessor 로 위임
        return ecsCallbackProcessor.process(request, shuttleOrder, items, handler);
    }

    /** 주문 타입을 지원하는 핸들러를 찾는다. 없으면 null. */
    private WcsOrderHandler findHandler(String orderType) {
        if (!StringUtils.hasText(orderType)) return null;
        return orderHandlers.stream().filter(handler -> handler.supports(orderType)).findFirst().orElse(null);
    }

    /** 포트 배차 락 강제 해제 패스스루 (관리자 전용). */
    public void forceUnlockPort(String eqGroupId, String portCode, String operator, String reason) {
        portService.forceUnlock(eqGroupId, portCode, operator, reason);
    }

    /** 포트 모드 전환 패스스루. */
    public PortService.ChangeResult changePortMode(String eqGroupId, String portCode,
                                                       PortMode newMode, String operator, String reason) {
        return portService.changePortMode(eqGroupId, portCode, newMode, operator, reason);
    }
}