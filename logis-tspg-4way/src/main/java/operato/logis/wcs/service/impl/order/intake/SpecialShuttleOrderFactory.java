package operato.logis.wcs.service.impl.order.intake;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.generator.HostOrderKeyGenerator;
import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

/**
 * 특수 목적 OUTBOUND 셔틀 생성 팩토리.
 *
 * ShuttleOrderRegistrar 의 정상 산출/디스패치 흐름을 거치지 않는 마커 성격의 OUTBOUND 셔틀
 * (SAMPLE_OUT, SAMPLE_DISCARD, DISPOSAL_OUT 등) 의 생성을 한 곳으로 모은다.
 *
 * 각 호출자에 흩어져 있던 보일러플레이트 (orderKey 생성, 공통 필드 복사, status 초기화 등) 제거.
 */
@Service
@RequiredArgsConstructor
public class SpecialShuttleOrderFactory {

    private final ShuttleOrderRepository shuttleOrderRepository;
    private final HostOrderKeyGenerator orderKeyGenerator;

    /**
     * SAMPLE_OUT — 시험 대상 stock 출고 셔틀.
     * 초기 상태: CREATED + ecsIfStatus READY (ECS 송신 대기).
     */
    public TbWcsShuttleOrder createSampleOutShuttle(TbWcsHostOrder originHost, String eqGroupId,
                                                    String barcode, String fromLocId, String portCode,
                                                    String carryingStockId) {
        TbWcsShuttleOrder ob = baseShuttle(originHost, eqGroupId, barcode,
                SubOrderType.SAMPLE_OUT,
                ShuttleOrderStatus.CREATED.codeAsIntOrNull());
        ob.setFromLocCode(fromLocId);
        ob.setToLocCode(portCode);
        ob.setCarryingStockId(carryingStockId);
        ob.setTestRequired(Boolean.TRUE);
        shuttleOrderRepository.insert(ob);
        return ob;
    }

    /**
     * SAMPLE_DISCARD — 시험 부적합 파렛트 폐기 마커 셔틀 (실제 이동 없음).
     * 초기 상태: COMPLETED + ecsIfStatus READY (자동 finalize).
     */
    public TbWcsShuttleOrder createSampleDiscardShuttle(TbWcsHostOrder host, String palletBarcode,
                                                        TbWcsShuttleOrder pendingParent, String reason) {
        TbWcsShuttleOrder discard = baseShuttle(host, host.getEqGroupId(), palletBarcode,
                SubOrderType.SAMPLE_DISCARD,
                ShuttleOrderStatus.COMPLETED.codeAsIntOrNull());
        discard.setParentOrderKey(pendingParent.getOrderKey());
        discard.setRemark(ValueUtil.isEmpty(reason) ? "시험 부적합 폐기" : reason);
        shuttleOrderRepository.insert(discard);
        return discard;
    }

    /**
     * 공통 OUTBOUND 셔틀 헤더 — orderKey 생성 + host 공통 필드 복사 + 초기 상태 셋업.
     * 추가 필드(from/to/carrying/parent/remark)는 호출자가 채운다.
     */
    private TbWcsShuttleOrder baseShuttle(TbWcsHostOrder host, String eqGroupId, String barcode,
                                          SubOrderType sub, Integer initialStatus) {
        TbWcsShuttleOrder s = new TbWcsShuttleOrder();
        s.setOrderKey(orderKeyGenerator.generate("ORDER_KEY"));
        s.setOrderType(OrderType.OUTBOUND.codeAsString());
        s.setSubOrderType(sub.code());
        s.setOrderStatus(initialStatus);
        s.setEcsIfStatus(EcsIfStatus.READY.codeAsIntOrNull());
        s.setPriority(host.getPriority());
        s.setEqGroupId(eqGroupId);
        s.setOwnerCode(host.getOwnerCode());
        s.setHostOrderKey(host.getHostOrderKey());
        s.setBarcode(barcode);
        return s;
    }
}
