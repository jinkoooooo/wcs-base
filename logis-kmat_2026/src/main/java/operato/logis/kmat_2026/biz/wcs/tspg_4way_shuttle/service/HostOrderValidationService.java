package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ====================================================================
 * Host Order Validation Service
 * ====================================================================
 *
 * [역할]
 * - HOST 주문 수신 DTO 검증
 * - HOST 주문 아이템 검증
 * - 저장된 엔티티 최소 정합성 검증
 *
 * [원칙]
 * - 이 서비스는 "입력 유효성"만 검증한다.
 * - 재고 존재 여부, 로케이션 가능 여부 같은 운영 데이터 검증은
 *   별도 비즈니스 서비스에서 처리한다.
 *
 * [반환 정책]
 * - null 이면 유효
 * - 문자열이면 첫 번째 검증 오류 메시지
 */
@Service
public class HostOrderValidationService {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderValidationService.class);

    /**
     * 요청 DTO 기본 검증.
     *
     * [검증 범위]
     * - 필수 필드 존재 여부
     * - 허용된 orderType 여부
     * - items 존재 여부
     */
    public String validateRequest(HostOrderReceiveRequest request) {
        if (request == null) {
            return "request is required";
        }

        if (!StringUtils.hasText(request.getHostSystemCode())) {
            return "hostSystemCode is required";
        }

        if (!StringUtils.hasText(request.getHostOrderKey())) {
            return "hostOrderKey is required";
        }

        if (!StringUtils.hasText(request.getOrderType())) {
            return "orderType is required";
        }

        if (!StringUtils.hasText(request.getOwnerCode())) {
            return "ownerCode is required";
        }

        if (!isValidOrderType(request.getOrderType())) {
            return "Invalid orderType: " + request.getOrderType() + ". Must be INBOUND, OUTBOUND, or MOVE";
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return "items is required and must not be empty";
        }

        return null;
    }

    /**
     * 요청 아이템 검증.
     *
     * [검증 범위]
     * - lineNo 양수 여부
     * - skuCode 존재 여부
     * - qty 양수 여부
     * - lineNo 중복 여부
     */
    public String validateItems(List<HostOrderReceiveRequest.HostOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return "items is required and must not be empty";
        }

        Set<Integer> lineNos = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            HostOrderReceiveRequest.HostOrderItemRequest item = items.get(i);

            if (item == null) {
                return "Item[" + i + "]: item is null";
            }

            if (item.getLineNo() == null || item.getLineNo() <= 0) {
                return "Item[" + i + "]: lineNo is required and must be positive";
            }

            if (!StringUtils.hasText(item.getSkuCode())) {
                return "Item[" + i + "]: skuCode is required";
            }

            if (item.getQty() == null || item.getQty() <= 0) {
                return "Item[" + i + "]: qty is required and must be positive";
            }

            if (lineNos.contains(item.getLineNo())) {
                return "Item[" + i + "]: duplicate lineNo " + item.getLineNo();
            }

            lineNos.add(item.getLineNo());
        }

        return null;
    }

    /**
     * 저장된 엔티티 최소 정합성 검증.
     *
     * [용도]
     * - DTO 검증이 아니라 이미 저장된 HostOrder / HostOrderItem 상태를
     *   다음 처리 단계로 넘기기 전에 점검할 때 사용한다.
     */
    public String validateEntity(TbWcsHostOrder order, List<TbWcsHostOrderItem> items) {
        if (order == null) {
            return "Host order is null";
        }

        if (!StringUtils.hasText(order.getHostSystemCode())) {
            return "Host order hostSystemCode is empty";
        }

        if (!StringUtils.hasText(order.getHostOrderKey())) {
            return "Host order hostOrderKey is empty";
        }

        if (!StringUtils.hasText(order.getOrderType())) {
            return "Host order orderType is empty";
        }

        if (!StringUtils.hasText(order.getOwnerCode())) {
            return "Host order ownerCode is empty";
        }

        if (items == null || items.isEmpty()) {
            return "Host order items is empty";
        }

        return null;
    }

    /**
     * 허용된 주문 유형인지 확인.
     */
    public boolean isValidOrderType(String orderType) {
        if (!StringUtils.hasText(orderType)) {
            return false;
        }

        return OrderTypeEnumCode.INBOUND.codeAsString().equalsIgnoreCase(orderType)
                || OrderTypeEnumCode.OUTBOUND.codeAsString().equalsIgnoreCase(orderType)
                || OrderTypeEnumCode.MOVE.codeAsString().equalsIgnoreCase(orderType);
    }

    /**
     * 입고 유형인지 확인.
     */
    public boolean isInbound(String orderType) {
        return OrderTypeEnumCode.INBOUND.codeAsString().equalsIgnoreCase(orderType);
    }

    /**
     * 출고 유형인지 확인.
     */
    public boolean isOutbound(String orderType) {
        return OrderTypeEnumCode.OUTBOUND.codeAsString().equalsIgnoreCase(orderType);
    }

    /**
     * 이동 유형인지 확인.
     */
    public boolean isMove(String orderType) {
        return OrderTypeEnumCode.MOVE.codeAsString().equalsIgnoreCase(orderType);
    }
}