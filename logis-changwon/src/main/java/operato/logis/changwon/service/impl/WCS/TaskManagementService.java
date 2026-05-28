package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.config.AsyncConfig;
import operato.logis.changwon.consts.*;
import operato.logis.changwon.entity.MFC.JobRet;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.entity.WCS.WcsStockInfo;
import operato.logis.changwon.entity.WCS.WcsTask;
import operato.logis.changwon.entity.WMS.WmsOdr;
import operato.logis.changwon.event.MfcTaskEvent;
import operato.logis.changwon.event.WcsTaskEvent;
import operato.logis.changwon.event.WmsOrderEvent;
import operato.logis.changwon.query.store.TaskQueryStore;
import operato.logis.changwon.service.impl.MFC.MfcInterfaceService;
import operato.logis.changwon.service.impl.MultiDatabaseJobService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.event.EventPublisher;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskManagementService extends AbstractQueryService {

    private final WcsStockAutoService wcsStockAutoService;
    private final WcsStockInfoService wcsStockInfoService;
    private final LocationRecommendationService locationRecommendationService;
    private final MfcInterfaceService mfcInterfaceService;
    private final TaskQueryStore taskQueryStore;
    private final EventPublisher eventPublisher;
    private final MultiDatabaseJobService multiDatabaseJobService;

    /**
     * WCS 작업 생성 후 MFC 작업 지시
     */
    @Transactional
    public void createWcsTask(WcsTask wcsTask) {
        /*
			입력 필요
			********************************************************************
			ORDER_KIND                ===                작업 유형
			TASK_PRIORITY  	          ===                작업 우선순위
			STOCK_ID                  ===                Pallet ID
			START_POINT_CD            ===                작업 시작 지점
			END_POINT_CD              ===                작업 도착 지점
			********************************************************************
		*/
        // 파라미터 유효성 검사
        if (!isParameterValid(wcsTask)) {
            throw new ElidomRuntimeException(ErrorCode.INVALID_PARAMETER.getErrorCode().toString(), ErrorCode.INVALID_PARAMETER.getDescription());
        }

        // 기본 속성 할당
        WcsConstants.setupDomainContext();
        wcsTask.setLcId(WcsConstants.LC_ID);
        if (ValueUtil.isEmpty(wcsTask.getTaskId())) {
            wcsTask.setTaskId(createTaskId(wcsTask));
        }
        if (ValueUtil.isEmpty(wcsTask.getTaskNo())) {
            wcsTask.setTaskNo(getCurrentTimeString());
        }
        if (ValueUtil.isEmpty(wcsTask.getAcceptDatetime())) {
            wcsTask.setAcceptDatetime(new Date());
        }
        wcsTask.setRoundNo((wcsTask.getRoundNo() == null ? 0 : wcsTask.getRoundNo()) + 1);

        wcsTask.setProcessStatus(ProcessStatus.TASK_READY.value());
        boolean isReady = preventInboundOutboundMixing(wcsTask);
        if (!isReady) {
            wcsTask.setProcessStatus(ProcessStatus.WAITING_FOR_PREV_TASK.value());
        }

        // 출고, 이송 작업의 경우 경로에 진행 중인 작업이 있으면 대기
        if (isReady && (OrderKind.OUTBOUND.value().equals(wcsTask.getOrderKind()) || OrderKind.TRANSFER.value().equals(wcsTask.getOrderKind()))) {
            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(wcsTask.getStartPointCd());
            List<WcsStockAuto> taskPath = wcsStockAutoService.getTaskPath(rack);
            taskPath.add(rack);
            // 이송 작업의 경우 도착 지점의 경로까지 모두 조회
            if (OrderKind.TRANSFER.value().equals(wcsTask.getOrderKind())) {
                WcsStockAuto endRack = wcsStockAutoService.getRackByLocCd(wcsTask.getEndPointCd());
                List<WcsStockAuto> endTaskPath = wcsStockAutoService.getTaskPath(endRack);
                taskPath.addAll(endTaskPath);
            }

            for (WcsStockAuto path : taskPath ) {
                if (!RackLocked.IDLE.value().equals(path.getRackLocked()) || ValueUtil.isNotEmpty(path.getTaskId())) {
                    isReady = false;
                    wcsTask.setProcessStatus(ProcessStatus.WAITING_FOR_PREV_TASK.value());
                    break;
                }
            }
        }

        // 테이블 저장 or 업데이트
        if (ValueUtil.isEmpty(wcsTask.getId())) {
            logger.info("New Task Inserted : {}", wcsTask.getTaskId());
            this.queryManager.insert(wcsTask);
        } else {
            logger.info("Exist Task Updated : {}", wcsTask.getTaskId());
            this.queryManager.update(wcsTask);
        }
        // LMS DB 작업 정보 입력
        multiDatabaseJobService.syncWcsToLmsTask(wcsTask);

        if (isReady) {
            // 경로에 다른 재고가 있는 경우 해당 재고 재정렬
            sortInventoryOnPath(wcsTask);
            // MFC 작업 지시
            mfcInterfaceService.createMfcTask(wcsTask);
            // WCS 작업 지시 시간 설정
            if (WcsConstants.FORCE_INBOUND.equals(wcsTask.getAttributeB())) {
                wcsTask.setOrderKind(OrderKind.FORCE_INBOUND.value());
                wcsTask.setProcessStatus(ProcessStatus.TASK_STARTING.value());
                wcsTask.setStartDatetime(new Date());
                this.queryManager.update(wcsTask, "orderKind", "processStatus", "startDatetime");
            } else {
                wcsTask.setProcessStatus(ProcessStatus.TASK_STARTING.value());
                wcsTask.setStartDatetime(new Date());
                this.queryManager.update(wcsTask, "processStatus", "startDatetime");
            }
        }
        else {
            logger.info("Task is not ready to start : {}", wcsTask.getTaskId());
        }

        // 재고 Locked
        if (isReady && !OrderKind.INBOUND.value().equals(wcsTask.getOrderKind())) {
            if (OrderKind.OUTBOUND.value().equals(wcsTask.getOrderKind())) {
                wcsStockInfoService.lockStock(wcsTask.getStockId(), StockLocked.OUTBOUND_IN_PROGRESS);
            } else {
                wcsStockInfoService.lockStock(wcsTask.getStockId(), StockLocked.SORT_IN_PROGRESS);
            }
        }

        // 작업 포인트 및 작업 경로 잠금 처리
        if (isReady && OrderKind.INBOUND.value().equals(wcsTask.getOrderKind())) {
            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(wcsTask.getEndPointCd());
            wcsStockAutoService.lockRack(rack, RackLocked.INBOUND_RESERVED, wcsTask.getTaskId());
            wcsStockAutoService.lockPath(rack, RackLocked.PATH_RESERVED, wcsTask.getTaskId());
            wcsStockAutoService.lockBack(rack, RackLocked.INBOUND_RESERVED, wcsTask.getTaskId());
        }
        // 작업이 즉시 실행되는 경우에만 작업 경로 잠금 처리
        else if (isReady && OrderKind.OUTBOUND.value().equals(wcsTask.getOrderKind())) {
            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(wcsTask.getStartPointCd());
            wcsStockAutoService.lockRack(rack, RackLocked.OUTBOUND_RESERVED, wcsTask.getTaskId());
            wcsStockAutoService.lockPath(rack, RackLocked.PATH_RESERVED, wcsTask.getTaskId());
        }
        else if (isReady && OrderKind.TRANSFER.value().equals(wcsTask.getOrderKind())) {
            WcsStockAuto startRack = wcsStockAutoService.getRackByLocCd(wcsTask.getStartPointCd());
            wcsStockAutoService.lockRack(startRack, RackLocked.OUTBOUND_RESERVED, wcsTask.getTaskId());
            wcsStockAutoService.lockPath(startRack, RackLocked.PATH_RESERVED, wcsTask.getTaskId());

            WcsStockAuto endRack = wcsStockAutoService.getRackByLocCd(wcsTask.getEndPointCd());
            wcsStockAutoService.lockRack(endRack, RackLocked.INBOUND_RESERVED, wcsTask.getTaskId());
            wcsStockAutoService.lockPath(endRack, RackLocked.PATH_RESERVED, wcsTask.getTaskId());
            wcsStockAutoService.lockBack(endRack, RackLocked.INBOUND_RESERVED, wcsTask.getTaskId());
        }
    }

    /**
     * 작업 경로에 존재하는 재고 정렬
     */
    public void sortInventoryOnPath(WcsTask task) {
        if (OrderKind.INBOUND.value().equals(task.getOrderKind())) {
            createSortTask(task, task.getEndPointCd());
        } else if (OrderKind.OUTBOUND.value().equals(task.getOrderKind())) {
            createSortTask(task, task.getStartPointCd());
        } else if (OrderKind.TRANSFER.value().equals(task.getOrderKind()) || OrderKind.SORT.value().equals(task.getOrderKind())) {
            createSortTask(task, task.getStartPointCd());
            createSortTask(task, task.getEndPointCd());
        }
    }

    private void createSortTask(WcsTask task, String locCd) {
        logger.info("Check if alignment is required");
        // 작업 시작 or 도착 포인트에서 중앙 통로까지 경로 조회
        WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(locCd);
        List<WcsStockAuto> taskPath = wcsStockAutoService.getTaskPath(rack);

        // 작업 경로 중 재고 정렬이 필요한 목록 산출
        List<WcsStockAuto> sortingRequiredRackList = new ArrayList<>();
        for (WcsStockAuto path : taskPath) {
            // 재고 이동에서 바깥쪽 -> 안쪽으로 이동시킬 때 자기 자신은 정렬 대상에서 제외
            if (ValueUtil.isNotEmpty(path.getStockId()) && path.getStockId().equals(task.getStockId())) {
                continue;
            }

            if (ValueUtil.isNotEmpty(path.getStockId()) && path.getRackLocked() <= RackLocked.PATH_RESERVED.value()) {
                logger.info("Sorting Required Rack : {}", path.getLocCd());
                sortingRequiredRackList.add(path);
            }
        }

        // 재고 정렬을 위한 도착 포인트 조회
        List<WcsStockAuto> destinationList = locationRecommendationService.getSortLocationList(rack);
        if (sortingRequiredRackList.size() > (destinationList == null ? 0 : destinationList.size())) {
            throw new ElidomRuntimeException(ErrorCode.EMPTY_LOCATION_FOR_SORTING.getErrorCode().toString(), ErrorCode.EMPTY_LOCATION_FOR_SORTING.getDescription());
        }

        // 재고 정렬 작업 생성
        int index = 0;
        List<WcsTask> sortTaskList = new ArrayList<>();
        for (WcsStockAuto departureRack : sortingRequiredRackList) {
            logger.info("Create Sort Task : {} -> {}", departureRack.getLocCd(), destinationList.get(index).getLocCd());
            WcsTask sortTask = new WcsTask();
            sortTask.setOrderKind(OrderKind.SORT.value());
            sortTask.setTaskPriority(WcsConstants.DEFAULT_SORT_TASK_PRIORITY - 100 * task.getRoundNo() + index);
            sortTask.setStockId(departureRack.getStockId());
            sortTask.setTaskId(createTaskId(sortTask));
            sortTask.setStartPointCd(departureRack.getLocCd());
            sortTask.setEndPointCd(destinationList.get(index).getLocCd());
            sortTask.setHighRankTaskId(task.getTaskId());
            sortTask.setHighRankTaskNo(task.getTaskNo());
            sortTask.setRoundNo(task.getRoundNo());

            // 작업 시작, 도착 포인트 잠금 처리
            wcsStockAutoService.lockRack(departureRack, RackLocked.OUTBOUND_RESERVED, sortTask.getTaskId());
            wcsStockAutoService.lockRack(destinationList.get(index), RackLocked.INBOUND_RESERVED, sortTask.getTaskId());

            sortTaskList.add(sortTask);
            index++;
        }

        for (WcsTask sortTask : sortTaskList) {
            logger.info("Sort Task Inserted : {}", sortTask.getTaskId());
            createWcsTask(sortTask);
        }
    }

    private String getCurrentTimeString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return LocalDateTime.now().format(formatter);
    }

    private String createTaskId(WcsTask task) {
        if (ValueUtil.isEmpty(task.getOrderId())) {
            String sql = "SELECT * FROM c_wms_odr WHERE lot_id = :lotId AND order_kind = '1' ORDER BY created_at DESC LIMIT 1";
            Map<String, Object> param = ValueUtil.newMap("lotId", task.getStockId());
            WmsOdr order = this.queryManager.selectBySql(sql, param, WmsOdr.class);
            task.setOrderId(order.getOrderId());
            task.setOrdSeq(order.getOrdSeq());
            task.setCustId(order.getCustId());
            task.setItemCode(order.getItemCode());
            task.setItemName(order.getItemName());
            task.setPlanQty(order.getMenge());
            task.setStockType(order.getLuggInfo());
        }

        return String.join(WcsConstants.TASK_ID_DELIMITER, task.getOrderId(), task.getOrdSeq());
    }

    private boolean isParameterValid(WcsTask wcsTask) {
        if (ValueUtil.isEmpty(wcsTask.getOrderKind())) {
            return false;
        } else if (ValueUtil.isEmpty(wcsTask.getTaskPriority())) {
            return false;
        } else if (ValueUtil.isEmpty(wcsTask.getStockId())) {
            return false;
        } else if (ValueUtil.isEmpty(wcsTask.getStartPointCd())) {
            return false;
        } else if (ValueUtil.isEmpty(wcsTask.getEndPointCd())) {
            return false;
        }
        return true;
    }

    @Async("wcsTaskExecutor")
    @EventListener(classes = MfcTaskEvent.class)
    public void receiveTaskResult(MfcTaskEvent mfcEvent) {
        WcsConstants.setupDomainContext();
        JobRet result = mfcEvent.getResult();

        String sql = "SELECT * FROM wcs_task WHERE task_id = :taskId AND task_no = :taskNo FOR UPDATE";
        Map<String, Object> param = ValueUtil.newMap("taskId,taskNo", result.getWmsOrdNo(), result.getOrderId());
        WcsTask task = this.queryManager.selectBySql(sql, param, WcsTask.class);

        if (ValueUtil.isEmpty(task)) {
            logger.info("존재하지 않는 작업입니다. TaskId : {}, TaskNo : {}", result.getWmsOrdNo(), result.getOrderId());
            return;
        }
        if (task.getProcessStatus() > ProcessStatus.TASK_STARTING.value()) {
            logger.info("이미 처리된 작업 TaskId : {}, TaskNo : {}, ResultType : {}", task.getTaskId(), task.getTaskNo(), result.getResultType());
            return;
        }
        logger.info("작업 결과 수신 TaskId : {}, TaskNo : {}, ResultType : {}", task.getTaskId(), task.getTaskNo(), result.getResultType());

        if (ResultType.TASK_COMPLETE.value().equals(result.getResultType())) {
            WcsTaskEvent wcsEvent = new WcsTaskEvent();
            wcsEvent.setWcsTask(task);
            wcsEvent.setOrderKind(task.getOrderKind());
            wcsEvent.setMethod("end");
            wcsEvent.setAttribute(task.getAttributeB());

            task.setProcessStatus(ProcessStatus.TASK_COMPLETE.value());
            task.setCompleteDatetime(new Date());
            this.queryManager.update(task, "processStatus", "completeDatetime");

            eventPublisher.publishEvent(wcsEvent);
        } else if (ResultType.TASK_CANCEL.value().equals(result.getResultType())) {
            WcsTaskEvent wcsEvent = new WcsTaskEvent();
            wcsEvent.setWcsTask(task);
            wcsEvent.setOrderKind(OrderKind.CANCEL.value());
            wcsEvent.setMethod("error");

            task.setProcessStatus(ProcessStatus.TASK_ERROR.value());
            task.setCompleteDatetime(new Date());
            this.queryManager.update(task, "processStatus", "completeDatetime");

            eventPublisher.publishEvent(wcsEvent);
        } else if (ResultType.CHANGE_DESTINATION.value().equals(result.getResultType())) {
            WcsTaskEvent wcsEvent = new WcsTaskEvent();
            wcsEvent.setWcsTask(task);
            wcsEvent.setOrderKind(task.getOrderKind());
            wcsEvent.setMethod("end");

            task.setProcessStatus(ProcessStatus.TASK_COMPLETE.value());
            task.setCompleteDatetime(new Date());
            task.setEndPointCd(mfcEvent.getEndPointCd());
            this.queryManager.update(task, "processStatus", "completeDatetime", "endPointCd");

            eventPublisher.publishEvent(wcsEvent);
        } else if (ResultType.MANUAL_CANCEL.value().equals(result.getResultType())) {
            cancelTask(task, ErrorCode.ORDER_CANCELLATION_BY_USER.getErrorCode().toString(), ErrorCode.ORDER_CANCELLATION_BY_USER.getDescription());
        }
    }

    @Transactional
    public void processPendingTasks() {
        WcsConstants.setupDomainContext();
        String sql = "SELECT * FROM wcs_task WHERE process_status = :processStatus ORDER BY task_priority ASC, created_at ASC FOR UPDATE";
        Map<String, Object> param = ValueUtil.newMap("processStatus", ProcessStatus.WAITING_FOR_PREV_TASK.value());
        List<WcsTask> taskList = this.queryManager.selectListBySql(sql, param, WcsTask.class, 0, 0);

        for (WcsTask task : taskList) {
            if (task.getOrderKind().equals(OrderKind.INBOUND.value())) {
                task.setRoundNo(0);
                createWcsTask(task);
            }
            else {
                WcsStockInfo stock = wcsStockInfoService.getStock(task.getStockId());
                if (ValueUtil.isEmpty(stock) || stock.getStockLocked() > StockLocked.SORT_IN_PROGRESS.value() || stock.getStockDisabled() == 1) {
                    cancelTask(task, ErrorCode.INVALID_STOCK.getErrorCode().toString(), ErrorCode.INVALID_STOCK.getDescription());
                    return;
                } else if (StockLocked.SORT_IN_PROGRESS.value().equals(stock.getStockLocked())) {
                    continue;
                }

                WcsStockAuto startRack = wcsStockAutoService.getRackByStockId(task.getStockId());
                if (ValueUtil.isEmpty(startRack)) {
                    cancelTask(task, ErrorCode.INVALID_LOCATION.getErrorCode().toString(), ErrorCode.INVALID_LOCATION.getDescription());
                    return;
                } else if (startRack.getRackLocked() > RackLocked.IDLE.value()) {
                    continue;
                }

                if (OrderKind.TRANSFER.value().equals(task.getOrderKind())) {
                    WcsStockAuto endRack = wcsStockAutoService.getRackByLocCd(task.getEndPointCd());
                    if (ValueUtil.isNotEmpty(endRack.getStockId())) {
                        cancelTask(task, ErrorCode.INVALID_LOCATION.getErrorCode().toString(), ErrorCode.INVALID_LOCATION.getDescription());
                        return;
                    } else if (endRack.getRackLocked() > RackLocked.IDLE.value()) {
                        continue;
                    }
                }

                task.setStartPointCd(startRack.getLocCd());
                task.setRoundNo(0);
                createWcsTask(task);
            }
        }
    }

    public boolean preventInboundOutboundMixing(WcsTask task) {
        String sql = taskQueryStore.getSelectInboundOutboundMixingSql();
        Map<String, Object> param;
        int result = 0;
        if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_1.equals(task.getAttributeB())) {
            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(task.getEndPointCd());
            param = ValueUtil.newMap("orderKind,craneNo", OrderKind.INBOUND.value(), rack.getCraneNo());
            result = this.queryManager.selectBySql(sql, param, Integer.class);
            if (result > 0) {
                return false;
            }

            rack = wcsStockAutoService.getRackByLocCd(task.getStartPointCd());
            param = ValueUtil.newMap("orderKind,craneNo", OrderKind.OUTBOUND.value(), rack.getCraneNo());
            result = this.queryManager.selectBySql(sql, param, Integer.class);
        } else if (task.getOrderKind().equals(OrderKind.INBOUND.value())) {
            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(task.getEndPointCd());
            param = ValueUtil.newMap("orderKind,craneNo", OrderKind.INBOUND.value(), rack.getCraneNo());
            result = this.queryManager.selectBySql(sql, param, Integer.class);
        } else if (task.getOrderKind().equals(OrderKind.OUTBOUND.value())) {
            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(task.getStartPointCd());
            param = ValueUtil.newMap("orderKind,craneNo", OrderKind.OUTBOUND.value(), rack.getCraneNo());
            result = this.queryManager.selectBySql(sql, param, Integer.class);
        } else {
            return true;
        }

        return result <= 0;
    }

    public void cancelTask(WcsTask task, String errorCode, String errorMsg) {
        logger.error("OrderId : {}, OrdSeq : {} 대기 중 에러 발생!", task.getOrderId(), task.getOrdSeq());
        logger.error("Error Message : {}", errorMsg);

        wcsStockAutoService.unlockRack(task.getTaskId());
        wcsStockInfoService.unlockStock(task.getStockId());

        task.setProcessStatus(ProcessStatus.TASK_ERROR.value());
        task.setCompleteDatetime(new Date());
        task.setErrorCode(Integer.parseInt(errorCode));
        task.setErrorMsg(errorMsg);
        this.queryManager.update(task, "processStatus", "completeDatetime", "errorCode", "errorMsg");

        WmsOdr order = new WmsOdr();
        order.setOrderId(task.getOrderId());
        order.setOrdSeq(task.getOrdSeq());
        if (OrderKind.SORT.value().equals(task.getOrderKind())) {
            return;
        } else {
            order.setOrderKind(task.getOrderKind());
        }
        order.setCustId(task.getCustId());
        order.setLotId(task.getStockId());
        order.setItemCode(task.getItemCode());

        WmsOrderEvent wmsEvent = new WmsOrderEvent();
        wmsEvent.setWmsOdr(order);
        eventPublisher.publishEvent(wmsEvent);
    }
}