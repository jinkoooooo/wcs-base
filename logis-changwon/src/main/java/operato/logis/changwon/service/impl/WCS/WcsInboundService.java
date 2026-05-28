package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.config.AsyncConfig;
import operato.logis.changwon.consts.ErrorCode;
import operato.logis.changwon.consts.OrderKind;
import operato.logis.changwon.consts.RackLocked;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class WcsInboundService extends AbstractQueryService {

    private final LocationRecommendationService locationRecommendationService;
    private final WcsStockAutoService wcsStockAutoService;
    private final WcsStockInfoService wcsStockInfoService;
    private final TaskManagementService taskManagementService;
    private final EventPublisher eventPublisher;

    @Transactional
    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.orderKind == '1' and #event.method == 'start'")
    public void createInboundTask(WcsTaskEvent event) {
        /*
			입력 필요
			********************************************************************
			ORDER_ID                  ===                WMS 작업 ID
			ORD_SEQ  			      ===                WMS 작업 SEQ
			STOCK_ID                  ===                Pallet ID
			********************************************************************
		*/
        WcsTask task = event.getWcsTask();
        logger.info("입고 작업 수신! OrderId = {}, OrdSeq = {}, StockId = {}", task.getOrderId(), task.getOrdSeq(), task.getStockId());
        try {
            String endPointCd = task.getEndPointCd();
            WcsStockAuto endRack = new WcsStockAuto();

            // 지정 로케이션 입고인 경우 유효성 검사 실시
            if (ValueUtil.isNotEmpty(endPointCd) && !endPointCd.equals(WcsConstants.WMS_EMPTY_LOCATION_VALUE)) {
                WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(endPointCd);
                List<WcsStockAuto> groupList = wcsStockAutoService.getListByCraneNo(rack.getCraneNo());
                int count = 0;
                for (WcsStockAuto group : groupList) {
                    if (isValidRack(group)) {
                        count++;
                    }
                }

                if (!isValidRack(rack) || count < WcsConstants.SAFETY_MARGIN) {
                    logger.info("입고 지정 로케이션 사용 불가! LocationCode : {}", rack.getLocCd());
                    endPointCd = null;
                    endRack = rack;
                }
            }

            // WMS 지정 입고지만 해당 로케이션이 사용 불가능한 경우
            if (ValueUtil.isEmpty(endPointCd)) {
                List<WcsStockAuto> rackList = locationRecommendationService.getSortLocationList(endRack);
                if (ValueUtil.isEmpty(rackList)) {
                    throw new ElidomRuntimeException(ErrorCode.EMPTY_VALID_LOCATION.getErrorCode().toString(), ErrorCode.EMPTY_VALID_LOCATION.getDescription());
                }
                WcsStockAuto rack = rackList.get(0);
                logger.info("입고 로케이션 추천 결과! LocationCode : {}", rack.getLocCd());
                endPointCd = rack.getLocCd();
            }
            // WCS 로케이션 추천
            else if (endPointCd.equals(WcsConstants.WMS_EMPTY_LOCATION_VALUE)) {
                WcsStockAuto rack = locationRecommendationService.getInboundLocation(task.getCustId());
                if (ValueUtil.isEmpty(rack)) {
                    throw new ElidomRuntimeException(ErrorCode.EMPTY_VALID_LOCATION.getErrorCode().toString(), ErrorCode.EMPTY_VALID_LOCATION.getDescription());
                }
                logger.info("입고 로케이션 추천 결과! LocationCode : {}", rack.getLocCd());
                endPointCd = rack.getLocCd();
            }

            // WCS 작업 생성
            if (WcsConstants.FORCE_INBOUND.equals(task.getAttributeB())) {
                task.setTaskPriority(WcsConstants.HIGHEST_PRIORITY);
                task.setStartPointCd(WcsConstants.FORCE_INBOUND_START_POINT);
            } else {
                task.setTaskPriority(WcsConstants.DEFAULT_INBOUND_TASK_PRIORITY);
                task.setStartPointCd(WcsConstants.INBOUND_CONVEYOR);
            }
            task.setOrderKind(OrderKind.INBOUND.value());
            task.setEndPointCd(endPointCd);

            // MFC 작업 지시
            taskManagementService.createWcsTask(task);
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
        order.setOrderKind(OrderKind.INBOUND.value());
        order.setCustId(task.getCustId());
        order.setLotId(task.getStockId());
        order.setItemCode(task.getItemCode());
        order.setLocId(task.getEndPointCd());

        WmsOrderEvent wmsEvent = new WmsOrderEvent();
        wmsEvent.setWmsOdr(order);
        eventPublisher.publishEvent(wmsEvent);
    }

    private boolean isValidRack(WcsStockAuto rack) {
        if (rack.getRackLocked() > RackLocked.PATH_RESERVED.value()) {
            return false;
        } else if (rack.getRackDisabled() == 1) {
            return false;
        } else if (ValueUtil.isNotEmpty(rack.getStockId())) {
            return false;
        } else if (rack.getCraneDisabled() == 1) {
            return false;
        }
        return true;
    }

    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.orderKind == '1' and #event.method == 'end'")
    public void completeTask(WcsTaskEvent event) {
        WcsConstants.setupDomainContext();
        WcsTask task = event.getWcsTask();

        wcsStockAutoService.unlockRack(task.getTaskId());
        wcsStockAutoService.setStockId(task.getEndPointCd(), task.getStockId());

        wcsStockInfoService.createStock(task);
    }
}