package operato.logis.samsung.service.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.samsung.WcsUtils;
import operato.logis.samsung.consts.BoxTrackingEventType;
import operato.logis.samsung.consts.ProcessStatus;
import operato.logis.samsung.entity.mw.TbMwBoxConveyorInfo;
import operato.logis.samsung.entity.mw.TbMwChute;
import operato.logis.samsung.entity.mw.TbMwXyzOrder;
import operato.logis.samsung.entity.xyz.TbMwIfXyzCycle;
import operato.logis.samsung.entity.xyz.TbMwIfXyzOrder;
import operato.logis.samsung.event.BoxTrackingEvent;
import operato.logis.samsung.service.mw.TbMwBoxConveyorInfoService;
import operato.logis.samsung.service.mw.TbMwChuteManagementService;
import operato.logis.samsung.service.mw.TbMwXyzOrderService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class XyzOrderReceiveService extends AbstractQueryService {

    private final TbMwChuteManagementService tbMwChuteManagementService;
    private final TbMwXyzOrderService tbMwXyzOrderService;
    private final TbMwBoxConveyorInfoService tbMwBoxConveyorInfoService;
    private final XyzPalletService xyzPalletService;

    private final ApplicationEventPublisher publisher;

    /**
     * XYZ -> MW Cycle 완료 보고
     */
    @Transactional
    public CommonApiResponse receiveCycleResult(TbMwIfXyzCycle cycle) {
        // 중복 수신 여부 확인
//        TbMwIfXyzCycle oldCycle = getCycle(cycle.getCycleId());
//        if (ValueUtil.isNotEmpty(oldCycle)) {
//            logger.error("[XYZ] 이미 수신된 작업 결과 Cycle ID : {}", cycle.getCycleId());
//            return CommonApiResponse.success();
//        }

        TbMwIfXyzCycle oldCycle = getCycle(cycle.getCycleId());
        if (ValueUtil.isNotEmpty(oldCycle)) {
            // 기존 작업이 있을 경우 알림 로그 (로직을 멈추지 않고 로깅만 진행)
            logger.info("[XYZ] 기존 작업 내역 존재 (추가 수신) - Cycle ID : {}, 기존 PickNum : {}, 현재 수신 PickNum : {}",
                    cycle.getCycleId(), oldCycle.getPickNum(), cycle.getPickNum());
        } else {
            // 처음 들어온 작업일 경우
            logger.info("[XYZ] 신규 작업 결과 정상 수신 완료 (Cycle ID : {}, PickNum : {})",
                    cycle.getCycleId(), cycle.getPickNum());
        }

        // 사이클 이력 저장
        logger.info("[XYZ] Cycle Receive : {}", WcsUtils.logRequestBody(cycle));
        this.queryManager.insert(cycle);

        // Order ID 추출
        String[] parts = cycle.getCycleId().split("_");
        if (ValueUtil.isEmpty(parts)) {
            logger.error("[XYZ] 유효하지 않은 Cycle ID 형식 : {}", cycle.getCycleId());
            return CommonApiResponse.success();
        }
        String orderId = parts[0];

        // 이미 완료된 작업인지 확인
        TbMwXyzOrder order = tbMwXyzOrderService.getOrderWithLock(orderId);
        if (ValueUtil.isEmpty(order)) {
            logger.error("[XYZ] 존재하지 않는 Order : {}", cycle.getCycleId());
            return CommonApiResponse.success();
        } else if (ProcessStatus.ORDER_COMPLETE.value().equals(order.getProcessStatus())) {
            logger.error("[XYZ] 이미 완료된 Order에 대한 Cycle 수신 : {}", cycle.getCycleId());
            return CommonApiResponse.success();
        }

        // 20260430.JJG 해당 작업 XYZ 자체 취소 된 경우.
        if(cycle.getPickNum() == 0){
            // TbMwIfXyzOrder 삭제 필요
            getXyzIfOrder(cycle.getCycleId());
        }

        // BoxConveyor 처리
        TbMwChute chute = tbMwChuteManagementService.getChute(cycle.getPalletId());
        List<TbMwBoxConveyorInfo> resultBoxList = tbMwBoxConveyorInfoService.pickBoxConveyor(cycle.getCycleId(), chute.getPalletSequence(), cycle.getPickNum());

        // 수량 업데이트
        tbMwChuteManagementService.updateCycleResult(cycle.getPalletId(), cycle.getPickNum());
        tbMwXyzOrderService.updateResultQty(order, cycle.getPickNum(), 0);

        // 251209 JJG : 최종 BoxTracking 완료 이벤트 호출
        if(resultBoxList != null){
            for(TbMwBoxConveyorInfo resultBox : resultBoxList){
                BoxTrackingEvent event = BoxTrackingEvent.builder()
                        .eventType(BoxTrackingEventType.XYZ_EVENT)
                        .plcSeqNo(resultBox.getPid())
                        .barcode(resultBox.getSerialNo())
                        .itemCode(order.getItemCode())
                        .lineId(order.getStartPointCd())
                        .equipId(order.getEndPointCd())
                        .measuredAt(new Date())
                        .build();

                publisher.publishEvent(event);
            }
        }

        return CommonApiResponse.success();
    }

    private TbMwIfXyzCycle getCycle(String cycleId) {
        String sql = "select * from tb_mw_if_xyz_cycle where cycle_id = :cycleId order by created_at desc limit 1";
        Map<String, Object> param = ValueUtil.newMap("cycleId", cycleId);
        return this.queryManager.selectBySql(sql, param, TbMwIfXyzCycle.class);
    }
    private void getXyzIfOrder(String taskId) {
        String sql = "delete from tb_mw_if_xyz_order where task_id = :taskId";

        Map<String, Object> param = ValueUtil.newMap("taskId", taskId);

        int count = this.queryManager.executeBySql(sql, param);
        logger.info("[" + count + "] record count tb_mw_if_xyz_order of taskId [" + taskId + "] deleted successfully!");
    }
}