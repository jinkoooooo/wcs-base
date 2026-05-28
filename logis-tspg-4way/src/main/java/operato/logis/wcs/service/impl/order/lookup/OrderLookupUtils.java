package operato.logis.wcs.service.impl.order.lookup;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.springframework.stereotype.Service;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

/**
 * host_order / shuttle_order 단건 조회 + null 시 표준 예외 던지기 공통 헬퍼.
 *
 * 각 호출자에 inline 으로 반복되던 "find → null check → throw" 패턴을 한 곳으로 모은다.
 * 예외 코드와 메시지를 통일하기 위함 — 분산 시 오류 코드/문구 불일치 위험.
 */
@Service
@RequiredArgsConstructor
public class OrderLookupUtils {

    private final HostOrderRepository hostOrderRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;

    /**
     * host_order 단건 조회 — 없으면 ORDER_NOT_FOUND.
     */
    public TbWcsHostOrder getHostOrderOrThrow(String hostOrderKey) {
        TbWcsHostOrder h = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(h)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "호스트 주문을 찾을 수 없습니다. (주문번호: " + hostOrderKey + ")");
        }
        return h;
    }

    /**
     * shuttle_order 단건 조회 + (선택) order_type 검증. 없거나 type 불일치 시 예외.
     *
     * @param expectedType null 이면 type 검증 건너뜀.
     * @param label 예외 메시지의 도메인 라벨 (예: "출고", "재입고").
     */
    public TbWcsShuttleOrder getShuttleOrderOrThrow(String orderKey, OrderType expectedType, String label) {
        TbWcsShuttleOrder s = shuttleOrderRepository.findByOrderKey(orderKey);
        if (ValueUtil.isEmpty(s)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    label + " 주문을 찾을 수 없습니다. (주문번호: " + orderKey + ")");
        }
        if (ValueUtil.isNotEmpty(expectedType) && !expectedType.codeAsString().equalsIgnoreCase(s.getOrderType())) {
            throw new ElidomRuntimeException("NOT_AN_" + expectedType.codeAsString(),
                    "주문 타입 불일치. expected=%s, actual=%s".formatted(expectedType.codeAsString(), s.getOrderType()));
        }
        return s;
    }

    /**
     * type 검증 없이 shuttle_order 단건 조회.
     */
    public TbWcsShuttleOrder getShuttleOrderOrThrow(String orderKey, String label) {
        return getShuttleOrderOrThrow(orderKey, null, label);
    }
}
