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
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.event.EventPublisher;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class WcsOutboundService extends AbstractQueryService {

    private final StockRecommendationService stockRecommendationService;
    private final WcsStockInfoService wcsStockInfoService;
    private final WcsStockAutoService wcsStockAutoService;
    private final TaskManagementService taskManagementService;
    private final EventPublisher eventPublisher;

    @Transactional
    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.orderKind == '2' and #event.method == 'start'")
    public void createOutboundTask(WcsTaskEvent event) {
        /*
			입력 필요
			********************************************************************
			ORDER_ID                  ===                WMS 작업 ID
			ORD_SEQ  			      ===                WMS 작업 SEQ
			STOCK_ID                  ===                Pallet ID
			ITEM_CODE                 ===                품번
			CUST_ID                   ===                화주 코드
			********************************************************************
		*/
        WcsTask task = event.getWcsTask();
        logger.info("출고 작업 수신! OrderId = {}, OrdSeq = {}, StockId = {}", task.getOrderId(), task.getOrdSeq(), task.getStockId());
        try {
            // 지정 출고
            String startPointCd;
            boolean readyFlag = true;
            if (ValueUtil.isNotEmpty(task.getStockId())) {
                WcsStockAuto rack = wcsStockAutoService.getRackByStockId(task.getStockId());
                WcsStockInfo stock = wcsStockInfoService.getStock(task.getStockId());
                // 지정 랙, 재고가 사용 불가능한 경우
                if (!isValidRack(rack)) {
                    throw new ElidomRuntimeException(ErrorCode.INVALID_LOCATION.getErrorCode().toString(), ErrorCode.INVALID_LOCATION.getDescription());
                } else if (!isValidStock(stock)) {
                    throw new ElidomRuntimeException(ErrorCode.INVALID_STOCK.getErrorCode().toString(), ErrorCode.INVALID_STOCK.getDescription());
                }

                // 지정 랙이 다른 작업의 정렬 경로인 경우 대기
                if (!rack.getRackLocked().equals(RackLocked.IDLE.value())) {
                    readyFlag = false;
                }
                startPointCd = rack.getLocCd();
                task.setStockType(stock.getStockType());
            }
            // WCS 추천 재고
            else {
                logger.info("WCS 재고 추천 로직 실행!");
                WcsStockAuto rack = stockRecommendationService.getOutboundStock(task.getItemCode(), task.getCustId());
                if (ValueUtil.isEmpty(rack)) {
                    throw new ElidomRuntimeException(ErrorCode.EMPTY_VALID_STOCK.getErrorCode().toString(), ErrorCode.EMPTY_VALID_STOCK.getDescription());
                }
                // 추천된 재고가 다른 작업의 정렬 경로인 경우 대기
                if (!rack.getRackLocked().equals(RackLocked.IDLE.value())) {
                    readyFlag = false;
                }
                startPointCd = rack.getLocCd();
                task.setStockId(rack.getStockId());
                logger.info("WCS 재고 추천 결과 : StockId = {}", task.getStockId());
            }

            // WCS 작업 속성 할당
            task.setLcId(WcsConstants.LC_ID);
            task.setTaskPriority(WcsConstants.DEFAULT_OUTBOUND_TASK_PRIORITY);
            task.setOrderKind(OrderKind.OUTBOUND.value());
            task.setStartPointCd(startPointCd);
            task.setEndPointCd(WcsConstants.OUTBOUND_CONVEYOR);
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
        order.setOrderKind(OrderKind.OUTBOUND.value());
        order.setCustId(task.getCustId());
        order.setLotId(task.getStockId());
        order.setItemCode(task.getItemCode());
        order.setLocId(task.getStartPointCd());

        WmsOrderEvent wmsEvent = new WmsOrderEvent();
        wmsEvent.setWmsOdr(order);
        eventPublisher.publishEvent(wmsEvent);
    }

    private boolean isValidRack(WcsStockAuto rack) {
        if (ValueUtil.isEmpty(rack)) {
            return false;
        } if (rack.getRackDisabled() == 1) {
            return false;
        } else if (rack.getCraneDisabled() == 1) {
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
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.orderKind == '2' and #event.method == 'end'")
    public void completeTask(WcsTaskEvent event) {
        WcsConstants.setupDomainContext();
        WcsTask task = event.getWcsTask();

        wcsStockAutoService.unlockRack(task.getTaskId());
        wcsStockAutoService.resetStockId(task.getStartPointCd());

        wcsStockInfoService.deleteStock(task.getStockId());
    }
}