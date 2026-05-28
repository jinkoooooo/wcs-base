package operato.logis.changwon.service.impl.MFC;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.WcsConstants;
import operato.logis.changwon.config.AsyncConfig;
import operato.logis.changwon.consts.DataTransmitStatus;
import operato.logis.changwon.consts.OrderKind;
import operato.logis.changwon.consts.ResultType;
import operato.logis.changwon.entity.MFC.JobOdr;
import operato.logis.changwon.entity.MFC.JobRet;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.entity.WCS.WcsTask;
import operato.logis.changwon.event.MfcTaskEvent;
import operato.logis.changwon.service.impl.WCS.WcsStockAutoService;
import org.springframework.stereotype.Service;
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
public class MfcInterfaceService extends AbstractQueryService {

    private final WcsStockAutoService wcsStockAutoService;
    private final EventPublisher eventPublisher;

    public void createMfcTask(WcsTask order) {
        WcsConstants.setupDomainContext();

        JobOdr task = new JobOdr();
        task.setOrderId(order.getTaskNo());
        task.setWmsOrdNo(order.getTaskId());
        task.setJobNo(getNextJobNumber(order));
        task.setAmKbn("A");
        if (WcsConstants.CANCEL_METHOD.equals(order.getAttributeA())) {
            task.setOrderStatus(7);
        } else {
            task.setOrderStatus(1);
        }
        if (WcsConstants.FORCE_INBOUND.equals(order.getAttributeB())) {
            task.setOrderKind(Integer.parseInt(OrderKind.FORCE_INBOUND.value()));
        } else {
            task.setOrderKind(convertOrderKind(order.getOrderKind()));
        }
        task.setOrderPhase(0);
        task.setOrderTransStatus(0);
        task.setMfcResult(0);
        if (WcsConstants.FORCE_INBOUND.equals(order.getAttributeB())) {
            // 강제 입고 요청
            task.setOrderPhase(1);
            task.setOrderType(101);
            task.setOrderPriority(order.getTaskPriority());
            task.setOrderReceiveDatetime(new Date());
            task.setUpdateDatetime(new Date());

            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(order.getEndPointCd());
            task.setSn(rack.getCraneNo() + WcsConstants.CONVERT_RUNNER_TO_SHUTTLE);
            task.setSz(0);
            task.setSx(0);
            task.setSy(0);
            task.setEz(rack.getLocLevel());
            task.setEx(rack.getLocRow());
            task.setEy(rack.getLocCol());
            task.setSourceId(WcsConstants.FORCE_INBOUND_START_POINT);
            task.setRackNo(rack.getLocCd());
        } else if (OrderKind.INBOUND.value().equals(order.getOrderKind())) {
            // 입고 요청
            task.setOrderType(101);
            task.setOrderPriority(order.getTaskPriority() + task.getJobNo());

            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(order.getEndPointCd());
            task.setSn(rack.getCraneNo() + WcsConstants.CONVERT_RUNNER_TO_SHUTTLE);
            task.setSz(0);
            task.setSx(0);
            task.setSy(0);
            task.setEz(rack.getLocLevel());
            task.setEx(rack.getLocRow());
            task.setEy(rack.getLocCol());
            task.setSourceId(WcsConstants.INBOUND_CONVEYOR);
            task.setRackNo(rack.getLocCd());
        } else if (OrderKind.OUTBOUND.value().equals(order.getOrderKind())) {
            // 출고 요청
            task.setOrderType(201);
            task.setOrderPriority(order.getTaskPriority() + task.getJobNo());

            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(order.getStartPointCd());
            task.setSn(rack.getCraneNo() + WcsConstants.CONVERT_RUNNER_TO_SHUTTLE);
            task.setSz(rack.getLocLevel());
            task.setSx(rack.getLocRow());
            task.setSy(rack.getLocCol());
            task.setEz(0);
            task.setEx(0);
            task.setEy(0);
            task.setSourceId(rack.getLocCd());
            task.setRackNo(WcsConstants.OUTBOUND_CONVEYOR);
        } else if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_1.equals(order.getAttributeB())) {
            // 재고 이동 : 다른 Shuttle, Runner 번호인 경우 중 출고 부분
            task.setOrderKind(Integer.parseInt(OrderKind.OUTBOUND.value()));
            task.setOrderType(201);
            task.setOrderPriority(order.getTaskPriority() + task.getJobNo());

            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(order.getStartPointCd());
            task.setSn(rack.getCraneNo() + WcsConstants.CONVERT_RUNNER_TO_SHUTTLE);
            task.setSz(rack.getLocLevel());
            task.setSx(rack.getLocRow());
            task.setSy(rack.getLocCol());
            task.setEz(0);
            task.setEx(0);
            task.setEy(0);
            task.setSourceId(rack.getLocCd());
            task.setRackNo(WcsConstants.TRANSFER_CONVEYOR);
        } else if (WcsConstants.TRANSFER_BETWEEN_EQUIPMENT_2.equals(order.getAttributeB())) {
            // 재고 이동 : 다른 Shuttle, Runner 번호인 경우 중 입고 부분
            task.setOrderKind(Integer.parseInt(OrderKind.INBOUND.value()));
            task.setOrderType(101);
            task.setOrderPriority(order.getTaskPriority() + task.getJobNo());

            WcsStockAuto rack = wcsStockAutoService.getRackByLocCd(order.getEndPointCd());
            task.setSn(rack.getCraneNo() + WcsConstants.CONVERT_RUNNER_TO_SHUTTLE);
            task.setSz(0);
            task.setSx(0);
            task.setSy(0);
            task.setEz(rack.getLocLevel());
            task.setEx(rack.getLocRow());
            task.setEy(rack.getLocCol());
            task.setSourceId(WcsConstants.TRANSFER_CONVEYOR);
            task.setRackNo(rack.getLocCd());
        } else {
            // 재고 이동 : 같은 Shuttle, Runner 번호인 경우
            task.setOrderType(101);
            task.setOrderPriority(order.getTaskPriority());

            WcsStockAuto startRack = wcsStockAutoService.getRackByLocCd(order.getStartPointCd());
            task.setSn(startRack.getCraneNo() + WcsConstants.CONVERT_RUNNER_TO_SHUTTLE);
            task.setSz(startRack.getLocLevel());
            task.setSx(startRack.getLocRow());
            task.setSy(startRack.getLocCol());
            WcsStockAuto endRack = wcsStockAutoService.getRackByLocCd(order.getEndPointCd());
            task.setEz(endRack.getLocLevel());
            task.setEx(endRack.getLocRow());
            task.setEy(endRack.getLocCol());
            task.setSourceId(startRack.getLocCd());
            task.setRackNo(endRack.getLocCd());
        }
        task.setCurrentMachine(0);
        task.setCurrentPosition(0);
        task.setErrorCount(0);
        // task.setOrderReceiveDatetime
        // task.setOrderMfcDatetime
        // task.setUpdateDatetime
        task.setStorageId("01");
        task.setLuggInfo(Integer.parseInt(order.getStockType()));
        task.setPalletId(order.getStockId());
        // task.setUserdata

        // WCS -> MFC 전송 완료 여부
        task.setDataTransmitStatus(DataTransmitStatus.NEW.value());

        // WCS 테이블 입력, 추후 Job을 통해 MFC 테이블 입력
        this.queryManager.insert(task);
    }

    public void receiveTaskResult() {
        WcsConstants.setupDomainContext();
        String sql = "SELECT * FROM c_job_ret WHERE flag = 0 ORDER BY created_at asc";
        List<JobRet> resultList = this.queryManager.selectListBySql(sql, null, JobRet.class, 0, 0);

        for (JobRet result : resultList) {
            result.setFlag(9);
            this.queryManager.update(result, "flag");

            // 최종 완료인 경우에만 이벤트 발생
            if (!(ResultType.TASK_COMPLETE.value().equals(result.getResultType()) && result.getCompleteType() == 3) && !ResultType.TASK_CANCEL.value().equals(result.getResultType())) {
                continue;
            }

            MfcTaskEvent event = new MfcTaskEvent();
            event.setResult(result);
            eventPublisher.publishEvent(event);
        }
    }

    private int convertOrderKind(String wcsOrderKind) {
        return switch (wcsOrderKind) {
            case "1" -> 1; // 입고
            case "2" -> 2; // 출고
            case "3", "9" -> 7; // 재고 이동
            case "4" -> 4; // 작업 취소
            case "8" -> 8; // 강제 입고
            default -> throw new ElidomRuntimeException("Invalid order kind. Cannot convert order kind."); // Error
        };
    }

    private int getNextJobNumber(WcsTask order) {
        if (WcsConstants.CANCEL_METHOD.equals(order.getAttributeA())) {
            String sql = "select job_no from c_job_odr where wms_ord_no = :wmsOrdNo and order_id = :orderId order by job_no desc limit 1";
            Map<String, Object> param = ValueUtil.newMap("orderId,wmsOrdNo", order.getTaskNo(), order.getTaskId());
            return this.queryManager.selectBySql(sql, param, Integer.class);
        }

        String sql = """
                INSERT INTO job_no_counter (biz_date, last_no)
                VALUES (CURRENT_DATE, 1)
                ON CONFLICT (biz_date) DO UPDATE SET last_no = job_no_counter.last_no + 1
                RETURNING last_no
                """;
        return this.queryManager.selectBySql(sql, null, Integer.class);
    }
}