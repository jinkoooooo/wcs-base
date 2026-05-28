package operato.logis.wcs.service.impl.order.host;

import operato.logis.wcs.consts.HostOrderType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.UomType;
import operato.logis.wcs.dto.HostOrderApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Set;

import static operato.logis.wcs.common.util.lang.CommonUtils.nz;

/**
 * HOST 요청 정규화 + 형식/필수 검증 (stateless).
 *
 * normalize : Alias 코드를 표준 코드로 치환.
 * validate  : 필수값/형식 검증 — null=OK, 그 외=에러 메시지.
 */
@Service
public class HostOrderFormatValidator {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderFormatValidator.class);

    // 바코드 필수인 주문 유형
    private static final Set<String> BARCODE_REQUIRED_TYPES = Set.of(OrderType.INBOUND.code());

    /**
     * HOST 페이로드의 OrderType/UOM Alias 와 누락값을 표준 코드로 치환한다.
     */
    public void normalize(HostOrderApi.Request request) {
        if (ValueUtil.isEmpty(request)) return;
        normalizeOrderType(request);
        normalizeItems(request.getItems());
    }

    /**
     * OrderType 정규화 — HostOrderType alias 우선, 없으면 OrderType 직접 매칭.
     */
    private void normalizeOrderType(HostOrderApi.Request request) {
        if (!StringUtils.hasText(request.getOrderType())) return;

        HostOrderType hostType = HostOrderType.from(request.getOrderType());
        if (ValueUtil.isNotEmpty(hostType)) {
            request.setOrderType(hostType.code());
            return;
        }
        OrderType type = OrderType.from(request.getOrderType());
        if (ValueUtil.isNotEmpty(type)) request.setOrderType(type.code());
    }

    /**
     * 아이템 UOM 정규화 — 누락은 EA, alias 는 표준 코드로.
     */
    private void normalizeItems(List<HostOrderApi.Item> items) {
        if (ValueUtil.isEmpty(items)) return;

        for (HostOrderApi.Item item : items) {
            if (!StringUtils.hasText(item.getUom())) {
                item.setUom(UomType.EA.code());
                continue;
            }
            UomType uom = UomType.from(item.getUom());
            if (ValueUtil.isNotEmpty(uom)) item.setUom(uom.code());
        }
    }

    /**
     * 형식/필수 검증 진입점. null = OK, 그 외 = 에러 메시지.
     */
    public String validate(HostOrderApi.Request request) {

        // 공통 검증 실패 시 즉시 반환
        String commonError = validateCommon(request);
        if (ValueUtil.isNotEmpty(commonError)) return commonError;

        // 유형별 분기 검증
        OrderType type = resolveBaseOrderType(request.getOrderType());
        logger.info("[ Order ][ Host ] validate - orderType={}, hostOrderKey={}", type, request.getHostOrderKey());

        return switch (type) {
            case INBOUND  -> validateInbound(request);
            case OUTBOUND -> validateOutbound(request);
            default       -> validateNonItemBased(request);
        };
    }

    /**
     * HostOrderType alias → 표준 OrderType 해석 (외부에서도 사용).
     */
    public static OrderType resolveBaseOrderType(String orderTypeCode) {
        if (ValueUtil.isEmpty(orderTypeCode)) return null;
        HostOrderType hostType = HostOrderType.from(orderTypeCode);
        return ValueUtil.isNotEmpty(hostType) ? hostType.baseOrderType() : OrderType.from(orderTypeCode);
    }

    /**
     * 공통 필드 검증 (hostSystemCode, hostOrderKey, orderType, eqGroupId, barcode).
     */
    private String validateCommon(HostOrderApi.Request request) {
        if (ValueUtil.isEmpty(request.getHostSystemCode())) return "hostSystemCode is required";
        if (ValueUtil.isEmpty(request.getHostOrderKey()))   return "hostOrderKey is required";
        if (ValueUtil.isEmpty(request.getOrderType()))      return "orderType is required";
        if (ValueUtil.isEmpty(request.getEqGroupId()))      return "eqGroupId is required";

        if (ValueUtil.isEmpty(resolveBaseOrderType(request.getOrderType()))) {
            return "Invalid orderType: '" + request.getOrderType() + "'";
        }
        if (ValueUtil.isEmpty(request.getBarcode())
                && BARCODE_REQUIRED_TYPES.contains(request.getOrderType())) {
            return "barCode is required";
        }
        return null;
    }

    /**
     * INBOUND 검증 — owner + 아이템(테스트 정보 포함).
     */
    private String validateInbound(HostOrderApi.Request request) {
        if (ValueUtil.isEmpty(request.getOwnerCode())) {
            return OrderType.INBOUND.code() + ": ownerCode is required";
        }
        return validateItems(request.getItems(), OrderType.INBOUND, request.getTestRequired());
    }

    /**
     * OUTBOUND 검증 — owner + 아이템.
     */
    private String validateOutbound(HostOrderApi.Request request) {
        if (ValueUtil.isEmpty(request.getOwnerCode())) {
            return OrderType.OUTBOUND.code() + ": ownerCode is required";
        }
        return validateItems(request.getItems(), OrderType.OUTBOUND, null);
    }

    /**
     * MOVE 등 아이템 비기반 주문 — fromLocId 필수.
     */
    private String validateNonItemBased(HostOrderApi.Request request) {
        if (ValueUtil.isEmpty(request.getFromLocId())) {
            return request.getOrderType() + ": fromLocId is required (source location of stock)";
        }
        return null;
    }

    /**
     * 아이템 리스트 검증 — 필수값/UOM/테스트 정보.
     */
    private String validateItems(List<HostOrderApi.Item> items, OrderType type, Boolean headerTestRequired) {
        if (ValueUtil.isEmpty(items)) {
            return type.code() + ": items is required and must not be empty";
        }
        boolean headerDefault = Boolean.TRUE.equals(headerTestRequired) && type == OrderType.INBOUND;

        for (int i = 0; i < items.size(); i++) {
            String error = validateSingleItem(items.get(i), i, type, headerDefault);
            if (ValueUtil.isNotEmpty(error)) return error;
        }
        return null;
    }

    /**
     * 아이템 1건 검증 — 필수/숫자/UOM/테스트 의뢰번호.
     */
    private String validateSingleItem(HostOrderApi.Item item, int index, OrderType type, boolean headerTest) {
        if (ValueUtil.isEmpty(item)) return "Item[" + index + "]: item is null";
        if (ValueUtil.isEmpty(item.getItemCode())) return "Item[" + index + "]: itemCode is required";
        if (nz(item.getQty()) <= 0) return "Item[" + index + "]: qty is required and must be positive";

        // INBOUND 는 lotNo 필수
        if (type == OrderType.INBOUND && ValueUtil.isEmpty(item.getLotNo())) {
            return "Item[" + index + "]: lotNo is required";
        }

        // UOM 형식 검증
        if (StringUtils.hasText(item.getUom()) && ValueUtil.isEmpty(UomType.from(item.getUom()))) {
            return "Item[" + index + "]: invalid uom '" + item.getUom() + "'. Must be one of EA/BOX/PLT (or aliases)";
        }

        // 시험 의뢰 시 의뢰번호 필수
        boolean itemTest = ValueUtil.isNotEmpty(item.getTestRequired()) ? item.getTestRequired() : headerTest;
        if (itemTest && type == OrderType.INBOUND && !StringUtils.hasText(item.getTestRequestNo())) {
            return "Item[" + index + "]: testRequestNo is required (testRequired=true)";
        }
        return null;
    }
}
