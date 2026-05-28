package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.consts.ErrorCode;
import operato.logis.changwon.consts.LoadChk;
import operato.logis.changwon.consts.OrderKind;
import operato.logis.changwon.consts.ProcessStatus;
import operato.logis.changwon.entity.MFC.PrsJobSts;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.entity.WCS.WcsTask;
import operato.logis.changwon.entity.WMS.WmsOdr;
import operato.logis.changwon.event.WcsTaskEvent;
import operato.logis.changwon.event.WmsOrderEvent;
import operato.logis.changwon.query.store.LocationQueryStore;
import operato.logis.changwon.service.impl.MFC.MfcInterfaceService;
import operato.logis.changwon.service.impl.MFC.PrsJobStsService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.event.EventPublisher;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WcsTaskCancelService extends AbstractQueryService {

    private final EventPublisher eventPublisher;
    private final MfcInterfaceService mfcInterfaceService;
    private final WcsStockAutoService wcsStockAutoService;
    private final WcsStockInfoService wcsStockInfoService;
    private final LocationQueryStore locationQueryStore;
    private final LocationRecommendationService locationRecommendationService;
    private final PrsJobStsService prsJobStsService;

    @Transactional
    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.orderKind == '4' and #event.method == 'start'")
    public void requestCancelTask(WcsTaskEvent event) {
        WcsTask task = event.getWcsTask();
        try {
            String sql = "select * from wcs_task where order_id = :orderId and ord_seq = :ordSeq order by created_at desc limit 1";
            Map<String, Object> param = ValueUtil.newMap("orderId,ordSeq", task.getOrderId(), task.getOrdSeq());
            task = this.queryManager.selectBySql(sql, param, WcsTask.class);
            logger.info("==> 작업 취소 요청 수신! TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());

            if (ValueUtil.isEmpty(task)) {
                throw new ElidomRuntimeException(ErrorCode.NOT_EXIST_TASK.getErrorCode().toString(), ErrorCode.NOT_EXIST_TASK.getDescription());
            } else if (task.getProcessStatus() >= ProcessStatus.TASK_COMPLETE.value()) {
                throw new ElidomRuntimeException(ErrorCode.COMPLETED_TASK.getErrorCode().toString(), ErrorCode.COMPLETED_TASK.getDescription());
            }

            task.setAttributeA(WcsConstants.CANCEL_METHOD);
            this.queryManager.update(task, "attributeA");
            // MFC 작업 진행 중인 경우
            if (task.getProcessStatus().equals(ProcessStatus.TASK_STARTING.value())) {
                // MFC 작업 삭제 요청
                mfcInterfaceService.createMfcTask(task);
                logger.info("<== MFC 작업 취소 요청 완료! TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
            }
            // WCS 작업만 취소하면 되는 경우
            else {
                // WCS 작업 취소 처리
                task.setProcessStatus(ProcessStatus.TASK_ERROR.value());
                task.setCompleteDatetime(new Date());

                this.queryManager.update(task, "processStatus", "completeDatetime");

                wcsStockAutoService.unlockRack(task.getTaskId());
                wcsStockInfoService.unlockStock(task.getTaskId());

                // WMS 취소 결과 보고
                reportCancelTask(task);
            }
        }
        catch (ElidomRuntimeException e) {
            handleErrorResponse(e, task);
        }
    }

    @Transactional
    @Async("wcsTaskExecutor")
    @EventListener(classes = WcsTaskEvent.class, condition = "#event.method == 'error'")
    public void cancelTask(WcsTaskEvent event) {
        WcsTask task = event.getWcsTask();
        PrsJobSts shuttle;
        WcsStockAuto rack;
        if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_2.equals(task.getAttributeB()) || task.getStartPointCd().startsWith("C")) {
            rack = wcsStockAutoService.getRackByLocCd(task.getEndPointCd());
        } else {
            rack = wcsStockAutoService.getRackByLocCd(task.getStartPointCd());
        }
        shuttle = prsJobStsService.getShuttleRunnerStatus(rack.getCraneNo());

        // WCS 작업 취소 처리 (재고는 Shuttle/Runner에 있는 상황)
        if (shuttle.getLoadChk().equals(LoadChk.STOCK_AND_RUNNER.value())) {
            logger.info("작업 취소 처리 : Shuttle/Runner에 재고가 있습니다. TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
            wcsStockAutoService.unlockRack(task.getTaskId());
            if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_2.equals(task.getAttributeB()) || task.getStartPointCd().startsWith("C")) {
                wcsStockAutoService.resetStockId(task.getEndPointCd());
            } else {
                wcsStockAutoService.resetStockId(task.getStartPointCd());
            }

            // MFC 강제 입고 요청
            createForceInbound(task);
        }
        // WCS 작업 취소 처리 (재고는 원위치에 있는 상황)
        else {
            logger.info("작업 취소 처리 : 재고는 작업 출발지에 있습니다. TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
            wcsStockAutoService.unlockRack(task.getTaskId());
            wcsStockInfoService.unlockStock(task.getTaskId());

            // WMS 취소 결과 보고
            reportCancelTask(task);
        }
    }

    public void createForceInbound(WcsTask task) {
        logger.info("==> 강제 입고 요청 시작! TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
        WcsStockAuto rack;
        if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_2.equals(task.getAttributeB()) || task.getStartPointCd().startsWith("C")) {
            rack = wcsStockAutoService.getRackByLocCd(task.getEndPointCd());
        } else {
            rack = wcsStockAutoService.getRackByLocCd(task.getStartPointCd());
        }
        String sql = locationQueryStore.getRecommendForceInboundLocationSql();
        Map<String, Object> param = ValueUtil.newMap("craneNo", rack.getCraneNo());
        WcsStockAuto forceInboundRack = this.queryManager.selectBySql(sql, param, WcsStockAuto.class);
        if (ValueUtil.isEmpty(forceInboundRack)) {
            logger.info("강제 입고 전용 공간이 없습니다! 추천 정렬 공간으로 대체합니다. TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
            forceInboundRack = locationRecommendationService.getSortLocationList(rack).get(0);
        }

        WcsTask forceInboundTask = new WcsTask();
        forceInboundTask.setOrderId(task.getOrderId());
        forceInboundTask.setOrdSeq(task.getOrdSeq());
        forceInboundTask.setOrderKind(OrderKind.INBOUND.value());
        forceInboundTask.setCustId(task.getCustId());
        forceInboundTask.setStockId(task.getStockId());
        forceInboundTask.setItemCode(task.getItemCode());
        forceInboundTask.setItemName(task.getItemName());
        forceInboundTask.setStartPointCd(WcsConstants.FORCE_INBOUND_START_POINT);
        forceInboundTask.setEndPointCd(forceInboundRack.getLocCd());
        forceInboundTask.setPlanQty(task.getPlanQty());
        forceInboundTask.setStockType(task.getStockType());
        forceInboundTask.setAttributeB(WcsConstants.FORCE_INBOUND);

        WcsTaskEvent orderEvent = new WcsTaskEvent();
        orderEvent.setWcsTask(forceInboundTask);
        orderEvent.setOrderKind(forceInboundTask.getOrderKind());
        orderEvent.setMethod("start");
        eventPublisher.publishEvent(orderEvent);
        logger.info("<== 강제 입고 요청 완료! TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
    }

    private void reportCancelTask(WcsTask task) {
        WcsTaskEvent wcsEvent = new WcsTaskEvent();
        wcsEvent.setWcsTask(task);
        wcsEvent.setOrderKind(task.getOrderKind());
        wcsEvent.setMethod("end");

        eventPublisher.publishEvent(wcsEvent);
        logger.info("<== WCS 작업 취소 처리 완료! TaskId = {}, TaskNo = {}, StockId = {}", task.getTaskId(), task.getTaskNo(), task.getStockId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleErrorResponse(ElidomRuntimeException e, WcsTask task) {
        logger.error("OrderId : {}, OrdSeq : {} 수신 후 에러 발생!", task.getOrderId(), task.getOrdSeq());
        logger.error("Error Message : {}", e.getMessage());

        WmsOdr order = new WmsOdr();
        order.setOrderId(task.getOrderId());
        order.setOrdSeq(task.getOrdSeq());
        order.setOrderKind(OrderKind.CANCEL.value());
        order.setCustId(task.getCustId());
        order.setLotId(task.getStockId());
        order.setItemCode(task.getItemCode());

        WmsOrderEvent wmsEvent = new WmsOrderEvent();
        wmsEvent.setWmsOdr(order);
        eventPublisher.publishEvent(wmsEvent);
    }
}