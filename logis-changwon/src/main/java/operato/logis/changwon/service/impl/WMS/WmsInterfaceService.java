package operato.logis.changwon.service.impl.WMS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.config.AsyncConfig;
import operato.logis.changwon.consts.OrderKind;
import operato.logis.changwon.consts.ResultType;
import operato.logis.changwon.entity.WCS.WcsTask;
import operato.logis.changwon.entity.WMS.WmsOdr;
import operato.logis.changwon.entity.WMS.WmsResult;
import operato.logis.changwon.event.WcsTaskEvent;
import operato.logis.changwon.event.WmsOrderEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.event.EventPublisher;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WmsInterfaceService extends AbstractQueryService {

    private final EventPublisher eventPublisher;

    /**
     * Job을 통한 WMS 신규 오더 확인
     */
    public void checkNewOrder() {
        WcsConstants.setupDomainContext();
        String sql = "SELECT * FROM C_WMS_ODR WHERE SND_YN='N' ORDER BY ORDER_PRIORITY DESC, REG_DT ASC";
        List<WmsOdr> orderList = this.queryManager.selectListBySql(sql, null, WmsOdr.class, 0, 0);

        for (WmsOdr order : orderList) {
            WcsTask task = new WcsTask();
            task.setOrderId(order.getOrderId());
            task.setOrdSeq(order.getOrdSeq());
            task.setOrderKind(order.getOrderKind());
            task.setCustId(order.getCustId());
            task.setStockId(order.getLotId());
            task.setItemCode(order.getItemCode());
            task.setItemName(order.getItemName());
            task.setStartPointCd(order.getLocId());
            task.setEndPointCd(order.getLocId());
            task.setPlanQty(order.getMenge());
            task.setStockType(order.getLuggInfo());

            order.setSndYn("Y");
            this.queryManager.update(order, "sndYn");

            WcsTaskEvent orderEvent = new WcsTaskEvent();
            orderEvent.setWcsTask(task);
            orderEvent.setOrderKind(order.getOrderKind());
            orderEvent.setMethod("start");
            eventPublisher.publishEvent(orderEvent);

            return;
        }
    }

    /**
     * WMS 신규 오더 생성 중 에러 발생 시 수신
     */
    @Async("wcsTaskExecutor")
    @EventListener(classes = WmsOrderEvent.class)
    public void receiveNewOrderError(WmsOrderEvent event) {
        WcsConstants.setupDomainContext();
        WmsOdr order = event.getWmsOdr();
        logger.error("WMS 신규 오더 생성 중 에러 수신 : OrderId = {}, OrdSeq = {}, OrderKind = {}, LotId = {}", order.getOrderId(), order.getOrdSeq(), order.getOrderKind(), order.getLotId());

        String sql = "UPDATE C_WMS_ODR SET SND_YN='E' WHERE ORDER_ID=:orderId AND ORD_SEQ=:ordSeq AND ORDER_KIND=:orderKind AND CUST_ID=:custId AND LOT_ID=:lotId AND ITEM_CODE=:itemCode AND LOC_ID=:locId";
        Map<String, Object> param = ValueUtil.newMap("orderId,ordSeq,orderKind,custId,lotId,itemCode,locId", order.getOrderId(), order.getOrdSeq(), order.getOrderKind(), order.getCustId(), order.getLotId(), order.getItemCode(), order.getLocId());
        this.queryManager.executeBySql(sql, param);
    }

    /**
     * WMS 오더 결과 전송
     */
    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.method == 'end'")
    public void sendOrderResult(WcsTaskEvent event) {
        WcsConstants.setupDomainContext();
        WcsTask task = event.getWcsTask();
        if (ValueUtil.isEmpty(task.getOrderId())) {
            return;
        } else if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_1.equals(event.getAttribute())) {
            return;
        }

        WmsResult result = new WmsResult();
        result.setOrderId(task.getOrderId());
        result.setOrdSeq(task.getOrdSeq());
        if (OrderKind.SORT.value().equals(task.getOrderKind())) {
            result.setOrderKind(OrderKind.TRANSFER.value());
        } else if (WcsConstants.CANCEL_METHOD.equals(task.getAttributeA())) {
            result.setOrderKind(OrderKind.CANCEL.value());
        } else {
            result.setOrderKind(task.getOrderKind());
        }

        if (OrderKind.FORCE_INBOUND.value().equals(task.getOrderKind())) {
            String sql = "select * from wcs_task where task_id = :taskId and order_kind != :orderKind order by created_at desc limit 1";
            Map<String, Object> param = ValueUtil.newMap("taskId,orderKind", task.getTaskId(), OrderKind.FORCE_INBOUND.value());
            WcsTask lastTask = this.queryManager.selectBySql(sql, param, WcsTask.class);

            if (OrderKind.OUTBOUND.value().equals(lastTask.getOrderKind()) || OrderKind.TRANSFER.value().equals(lastTask.getOrderKind()) || OrderKind.SORT.value().equals(lastTask.getOrderKind())) {
                result.setOrderKind(OrderKind.TRANSFER.value());
            } else if (OrderKind.INBOUND.value().equals(lastTask.getOrderKind())) {
                result.setOrderKind(OrderKind.INBOUND.value());
            }
        }

        result.setCustId(task.getCustId());
        result.setLotId(task.getStockId());
        result.setItemCode(task.getItemCode());
        result.setItemName(task.getItemName());
        if (OrderKind.OUTBOUND.value().equals(result.getOrderKind())) {
            // 출고는 재고 기존 위치
            result.setLocId(task.getStartPointCd());
        } else {
            // 입고, 이송, 정렬은 재고 도착 위치
            result.setLocId(task.getEndPointCd());
        }
        result.setMenge(task.getPlanQty());
        if (OrderKind.CANCEL.value().equals(result.getOrderKind())) {
            result.setResultType(ResultType.TASK_CANCEL.value());
        } else {
            result.setResultType(result.getOrderKind());
        }
        result.setErrorMachine(task.getErrorMsg());
        result.setErrorCode(task.getErrorCode());
        result.setFlag(9);
        result.setWmsSndYn("N");
        result.setRegId("WCS");
        result.setRegDt(new Date());

        this.queryManager.insert(result);
    }

    @Transactional
    public String resetError(List<WmsOdr> orderList) {
        for (WmsOdr order : orderList) {
            order = this.queryManager.select(order);
            if (!order.getSndYn().equals("E")) {
                throw new ElidomRuntimeException("", "에러인 주문만 초기화 가능합니다.");
            }

            order.setSndYn("N");
            this.queryManager.update(order, "sndYn");
        }

        return "success";
    }
}