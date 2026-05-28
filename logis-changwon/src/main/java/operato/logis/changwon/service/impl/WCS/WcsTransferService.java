package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.config.AsyncConfig;
import operato.logis.changwon.consts.*;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.entity.WCS.WcsStockInfo;
import operato.logis.changwon.entity.WCS.WcsTask;
import operato.logis.changwon.entity.WMS.WmsOdr;
import operato.logis.changwon.event.WcsTaskEvent;
import operato.logis.changwon.event.WmsOrderEvent;
import operato.logis.changwon.service.impl.MFC.MfcInterfaceService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.event.EventPublisher;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WcsTransferService extends AbstractQueryService {

    private final WcsStockInfoService wcsStockInfoService;
    private final WcsStockAutoService wcsStockAutoService;
    private final TaskManagementService taskManagementService;
    private final MfcInterfaceService mfcInterfaceService;
    private final EventPublisher eventPublisher;

    @Transactional
    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.orderKind == '3' and #event.method == 'start'")
    public void createTransferTask(WcsTaskEvent event) {
        /*
			입력 필요
			********************************************************************
			ORDER_ID                  ===                WMS 작업 ID
			ORD_SEQ  			      ===                WMS 작업 SEQ
			STOCK_ID                  ===                Pallet ID
			END_POINT_CD              ===                작업 도착 지점
			ITEM_CODE                 ===                품번
			CUST_ID                   ===                화주 코드
			********************************************************************
		*/
        WcsTask task = event.getWcsTask();
        logger.info("재고 이동 작업 수신! OrderId = {}, OrdSeq = {}, StockId = {}", task.getOrderId(), task.getOrdSeq(), task.getStockId());
        try {
            boolean readyFlag = true;
            WcsStockAuto startRack = wcsStockAutoService.getRackByStockId(task.getStockId());
            WcsStockAuto endRack = wcsStockAutoService.getRackByLocCd(task.getEndPointCd());
            WcsStockInfo stock = wcsStockInfoService.getStock(task.getStockId());
            // 지정 랙, 재고가 사용 불가능한 경우
            if (!isValidStartRack(startRack)) {
                throw new ElidomRuntimeException(ErrorCode.INVALID_LOCATION.getErrorCode().toString(), ErrorCode.INVALID_LOCATION.getDescription());
            } else if (!isValidEndRack(endRack)) {
                throw new ElidomRuntimeException(ErrorCode.INVALID_LOCATION.getErrorCode().toString(), ErrorCode.INVALID_LOCATION.getDescription());
            } else if (!isValidStock(stock)) {
                throw new ElidomRuntimeException(ErrorCode.INVALID_STOCK.getErrorCode().toString(), ErrorCode.INVALID_STOCK.getDescription());
            }

            // 시작점과 도착점의 Shuttle, Runner 번호가 다른 경우
            if (!startRack.getCraneNo().equals(endRack.getCraneNo())) {
                task.setAttributeB(WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_1);
            }

            // 지정 랙이 다른 작업의 정렬 경로인 경우 대기
            if (!startRack.getRackLocked().equals(RackLocked.IDLE.value())) {
                readyFlag = false;
            } else if (!endRack.getRackLocked().equals(RackLocked.IDLE.value())) {
                readyFlag = false;
            }

            // WCS 작업 속성 할당
            task.setLcId(WcsConstants.LC_ID);
            task.setTaskPriority(WcsConstants.DEFAULT_TRANSFER_TASK_PRIORITY);
            task.setOrderKind(OrderKind.TRANSFER.value());
            task.setStartPointCd(startRack.getLocCd());
            task.setEndPointCd(endRack.getLocCd());
            task.setStockType(stock.getStockType());
            if (readyFlag) {
                // MFC 작업 생성
                taskManagementService.createWcsTask(task);
            } else {
                // 작업 대기
                task.setAcceptDatetime(new Date());
                task.setProcessStatus(ProcessStatus.WAITING_FOR_PREV_TASK.value());
                this.queryManager.insert(task);
            }
        }
        catch (ElidomRuntimeException e) {
            handleErrorResponse(e, task);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleErrorResponse(ElidomRuntimeException e, WcsTask task) {
        logger.error("OrderId : {}, OrdSeq : {} 수신 후 에러 발생!", task.getOrderId(), task.getOrdSeq());
        logger.error("Error Message : {}", e.getMessage());

        WmsOdr order = new WmsOdr();
        order.setOrderId(task.getOrderId());
        order.setOrdSeq(task.getOrdSeq());
        order.setOrderKind(OrderKind.TRANSFER.value());
        order.setCustId(task.getCustId());
        order.setLotId(task.getStockId());
        order.setItemCode(task.getItemCode());
        order.setLocId(task.getEndPointCd());

        WmsOrderEvent wmsEvent = new WmsOrderEvent();
        wmsEvent.setWmsOdr(order);
        eventPublisher.publishEvent(wmsEvent);
    }

    private boolean isValidStartRack(WcsStockAuto rack) {
        if (ValueUtil.isEmpty(rack)) {
            return false;
        } if (rack.getRackDisabled() == 1) {
            return false;
        } else if (rack.getCraneDisabled() == 1) {
            return false;
        }
        return true;
    }

    private boolean isValidEndRack(WcsStockAuto rack) {
        if (ValueUtil.isEmpty(rack)) {
            return false;
        } if (rack.getRackDisabled() == 1) {
            return false;
        } else if (rack.getCraneDisabled() == 1) {
            return false;
        } else if (rack.getRackLocked() > RackLocked.PATH_RESERVED.value()) {
            return false;
        } else if (ValueUtil.isNotEmpty(rack.getStockId())) {
            return false;
        }
        return true;
    }

    private boolean isValidStock(WcsStockInfo stock) {
        if (ValueUtil.isEmpty(stock)) {
            return false;
        } if (stock.getStockLocked() > StockLocked.SORT_IN_PROGRESS.value()) {
            return false;
        } else if (stock.getStockDisabled() == 1) {
            return false;
        }
        return true;
    }

    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "(#event.orderKind == '3' or #event.orderKind == '8' or #event.orderKind == '9') and #event.method == 'end'")
    public void completeTask(WcsTaskEvent event) {
        WcsConstants.setupDomainContext();
        WcsTask task = event.getWcsTask();

        if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_1.equals(task.getAttributeB())) {
            task.setAttributeB(WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_2);

            mfcInterfaceService.createMfcTask(task);

            task.setProcessStatus(ProcessStatus.TASK_STARTING.value());
            task.setCompleteDatetime(null);
            this.queryManager.update(task, "processStatus", "completeDatetime", "attributeB");
            return;
        }

        wcsStockAutoService.unlockRack(task.getTaskId());
        wcsStockAutoService.setStockId(task.getEndPointCd(), task.getStockId());
        if (OrderKind.FORCE_INBOUND.value().equals(task.getOrderKind())) {
            wcsStockInfoService.completeForceInbound(task);

            String sql = "select * from wcs_task where task_id = :taskId and order_kind != :orderKind order by created_at desc limit 1";
            Map<String, Object> param = ValueUtil.newMap("taskId,orderKind", task.getTaskId(), OrderKind.FORCE_INBOUND.value());
            WcsTask lastTask = this.queryManager.selectBySql(sql, param, WcsTask.class);
            if (OrderKind.OUTBOUND.value().equals(lastTask.getOrderKind())) {
                logger.info("강제 입고 완료 후 자동 출고를 진행합니다. TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
                WcsTask outboundTask = new WcsTask();
                outboundTask.setOrderId(lastTask.getOrderId());
                outboundTask.setOrdSeq(lastTask.getOrdSeq());
                outboundTask.setOrderKind(lastTask.getOrderKind());
                outboundTask.setCustId(lastTask.getCustId());
                outboundTask.setStockId(lastTask.getStockId());
                outboundTask.setItemCode(lastTask.getItemCode());
                outboundTask.setItemName(lastTask.getItemName());
                outboundTask.setStartPointCd(lastTask.getStartPointCd());
                outboundTask.setEndPointCd(lastTask.getEndPointCd());
                outboundTask.setPlanQty(lastTask.getPlanQty());
                outboundTask.setStockType(lastTask.getStockType());

                WcsTaskEvent orderEvent = new WcsTaskEvent();
                orderEvent.setWcsTask(outboundTask);
                orderEvent.setOrderKind(outboundTask.getOrderKind());
                orderEvent.setMethod("start");
                eventPublisher.publishEvent(orderEvent);
            }
        } else {
            wcsStockAutoService.resetStockId(task.getStartPointCd());
            wcsStockInfoService.unlockStock(task.getStockId());
        }
    }
}