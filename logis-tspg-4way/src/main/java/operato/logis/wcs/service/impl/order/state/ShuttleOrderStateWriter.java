package operato.logis.wcs.service.impl.order.state;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.EcsIfStatus;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.Date;

/**
 * shuttle_order 상태 변경 단일 창구.
 *
 * 모든 order_status 전이는 transition 단일 private 헬퍼를 통과한다.
 * 이 헬퍼는 ShuttleOrderRepository.transitionOrderStatus 의 DB 조건부 UPDATE 로
 * 역행을 원자적으로 차단하고, 성공했을 때만 엔티티 메모리를 동기화한다.
 * 직접 shuttle.setOrderStatus + repository.update 호출은 금지.
 *
 * 상태 분류와 전이 규칙:
 *   - 진행중(0~40) / 완료/취소/중단(90~95) / 에러(100~190).
 *   - 일반 전이(force=false) : 완전 종료(COMPLETED/CANCELLED/ABORTED) 면 DB 에서 0 row → 무시.
 *                              진행중·에러 상태에서는 전이 수행. 늦은 중복 콜백의 역행 차단.
 *   - 운영자 복구(force=true): 모든 가드 무시. 에러 재개·비정상 종료 되돌리기 전용.
 *                              자동 콜백 경로 사용 금지.
 *
 * ecs_if_status 는 order_status 와 별개 축(통신 채널 상태).
 *
 * 본 클래스는 호출자 트랜잭션에 합류한다 (Propagation.REQUIRED 기본).
 * HOST 측 상태 분리가 필요하면 호출자가 HostOrderStateWriter 의 REQUIRES_NEW 메서드를 별도 호출.
 */
@Service
@RequiredArgsConstructor
public class ShuttleOrderStateWriter {

    private static final Logger logger = LoggerFactory.getLogger(ShuttleOrderStateWriter.class);

    private final ShuttleOrderRepository shuttleOrderRepository;
    private final FollowUpPolicy followUpPolicy;

    /**
     * SENT — ECS 전송 완료. order_status 전이(단일 경로) 성공 후 ecs_if_status=SENT 별도 기록.
     * 이미 종결된 오더면 전이 차단되어 false 반환, ecs_if_status 도 건드리지 않는다.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markSent(TbWcsShuttleOrder shuttle) {
        boolean ok = transition(shuttle, ShuttleOrderStatus.SENT, false);
        if (!ok) return false;

        // ecs_if_status 는 order_status 와 독립 축 — 전이 성공 후 별도 기록
        shuttle.setEcsIfStatus(EcsIfStatus.SENT.codeAsIntOrNull());
        shuttleOrderRepository.update(shuttle, "ecsIfStatus");
        return true;
    }

    /**
     * WAITING — 산출 대기 (방해물/포트 미가용 등으로 즉시 전송 불가 시).
     */
    @Transactional(rollbackFor = Exception.class)
    public void markWaiting(TbWcsShuttleOrder shuttle) {
        transition(shuttle, ShuttleOrderStatus.WAITING, false);
    }

    /**
     * RUNNING — STARTED 콜백 수신 시.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRunning(TbWcsShuttleOrder shuttle) {
        transition(shuttle, ShuttleOrderStatus.RUNNING, false);
    }

    /**
     * ARRIVED — OUTBOUND NORMAL 도착 후 작업자 finalize 대기.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markArrived(TbWcsShuttleOrder shuttle) {
        transition(shuttle, ShuttleOrderStatus.ARRIVED, false);
    }

    /**
     * COMPLETED — 정상 종료. 전이 성공 시에만 FollowUpPolicy 평가 후 follow_up_since set.
     *
     * 전이 성공을 확인하고 follow_up 을 찍으므로, "order_status 는 안 바뀌었는데
     * follow_up_since 만 남는" 정합성 깨짐이 구조적으로 불가능하다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCompleted(TbWcsShuttleOrder shuttle) {
        boolean ok = transition(shuttle, ShuttleOrderStatus.COMPLETED, false);
        if (ok && followUpPolicy.requiresFollowUp(shuttle)) {
            shuttle.setFollowUpSince(new Date());
            shuttleOrderRepository.update(shuttle, "followUpSince");
            logger.info("[ Order ][ Shuttle ] follow-up marked - orderKey={}, since={}",
                    shuttle.getOrderKey(), shuttle.getFollowUpSince());
        }
    }

    /**
     * CANCELLED — 외부/수동 취소.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCancelled(TbWcsShuttleOrder shuttle) {
        transition(shuttle, ShuttleOrderStatus.CANCELLED, false);
    }

    /**
     * ERROR_HARDWARE — ECS 하드웨어 결함 콜백.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markErrorHardware(TbWcsShuttleOrder shuttle) {
        transition(shuttle, ShuttleOrderStatus.ERROR_HARDWARE, false);
    }

    /**
     * ERROR_INVENTORY — DOUBLE_IN / EMPTY_OUT 등 논리 에러.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markErrorInventory(TbWcsShuttleOrder shuttle) {
        transition(shuttle, ShuttleOrderStatus.ERROR_INVENTORY, false);
    }

    /**
     * ERROR_SEND_FAIL — ECS 전송 실패.
     * order_status 전이는 단일 경로(transition), ecs_if_status=FAIL + remark 는 별개 축이므로
     * 다음 별도 부분 UPDATE 로 기록한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void markErrorSendFail(TbWcsShuttleOrder shuttle, String remark) {
        if (ValueUtil.isEmpty(shuttle)) return;

        // 이미 완전 종료된 오더면 에러 마킹도 의미 없음
        boolean ok = transition(shuttle, ShuttleOrderStatus.ERROR_SEND_FAIL, false);
        if (!ok) return;

        // ecs_if_status / remark 는 order_status 와 독립 축 — 전이 성공 후 별도 기록
        shuttle.setEcsIfStatus(EcsIfStatus.FAIL.codeAsIntOrNull());
        if (ValueUtil.isNotEmpty(remark)) {
            shuttle.setRemark(remark);
            shuttleOrderRepository.update(shuttle, "ecsIfStatus", "remark");
        } else {
            shuttleOrderRepository.update(shuttle, "ecsIfStatus");
        }
        logger.info("[ Order ][ Shuttle ] ecs send failed - orderKey={}, ecsIfStatus=FAIL", shuttle.getOrderKey());
    }

    /**
     * ecs_if_status = SENDING — order_status 는 보존, ECS 전송 진행 마커 (별개 축).
     */
    @Transactional(rollbackFor = Exception.class)
    public void markEcsIfStatusSending(TbWcsShuttleOrder shuttle) {
        if (ValueUtil.isEmpty(shuttle)) return;
        shuttle.setEcsIfStatus(EcsIfStatus.SENDING.codeAsIntOrNull());
        shuttleOrderRepository.update(shuttle, "ecsIfStatus");
        logger.info("[ Order ][ Shuttle ] ecs sending - orderKey={}, orderStatus={}",
                shuttle.getOrderKey(), shuttle.getOrderStatus());
    }

    /**
     * 운영자 명시 복구 — 에러 상태에서 재개하거나 비정상 종료 상태를 되돌린다.
     *
     * force=true 로 모든 역행 차단 가드를 무시한다. 운영자 복구 화면/도구에서만 호출.
     * 자동 콜백 처리 경로에서는 절대 호출 금지 — 잘못 쓰면 정상 COMPLETED 오더를 진행중으로 되돌릴 수 있다.
     *
     * @param to       복구 후 전이할 상태
     * @param operator 복구 수행 운영자 (감사 로그용)
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean recoverByOperator(TbWcsShuttleOrder shuttle, ShuttleOrderStatus to, String operator) {
        if (ValueUtil.isEmpty(shuttle)) return false;
        Integer prev = shuttle.getOrderStatus();
        boolean ok = transition(shuttle, to, true);
        if (ok) {
            logger.warn("[ Order ][ Shuttle ] operator recovered - orderKey={}, prev={}, new={}, operator={}",
                    shuttle.getOrderKey(), prev, to.name(), operator);
        }
        return ok;
    }

    /**
     * 의존성 깨우기 — 본 오더가 막 COMPLETED 된 직후 자동 호출 (EcsCallbackProcessor.doCompleteShuttle).
     * prereq + own children 안전 조건 동시 충족하는 WAITING 후속 오더를 CREATED 로 전이.
     */
    @Transactional(rollbackFor = Exception.class)
    public void wakeUpDependents(String justCompletedKey) {
        if (ValueUtil.isEmpty(justCompletedKey)) return;
        shuttleOrderRepository.wakeUpDependentsByCompletion(justCompletedKey);
    }

    /**
     * 운영자 명시 호출 전용 — 본 오더의 prerequisite_order_key 를 NULL 로 끊고
     * own obstacle children 까지 해소된 상태면 즉시 WAITING→CREATED.
     */
    @Transactional(rollbackFor = Exception.class)
    public int severPrerequisite(String orderKey) {
        if (ValueUtil.isEmpty(orderKey)) return 0;
        int severed = shuttleOrderRepository.severPrerequisiteByOrderKey(orderKey);
        if (severed > 0) {
            int woken = shuttleOrderRepository.wakeUpSelfIfReady(orderKey);
            logger.info("[ Order ][ Shuttle ] prereq severed + woke - orderKey={}, severed={}, woken={}",
                    orderKey, severed, woken);
        }
        return severed;
    }

    /**
     * 재입고 BCR 스캔 시점에 parent 의 follow_up_since 를 클리어한다.
     */
    public void clearParentFollowUpOnReInboundScan(TbWcsShuttleOrder inbound) {
        String parentKey = inbound.getParentOrderKey();
        if (ValueUtil.isEmpty(parentKey)) return;

        TbWcsShuttleOrder parent = shuttleOrderRepository.findByOrderKey(parentKey);
        if (ValueUtil.isEmpty(parent)) return;

        parent.setFollowUpSince(null);
        shuttleOrderRepository.update(parent, "followUpSince");
        logger.info("[ Order ][ Shuttle ] follow-up cleared - parent={}, inbound={}",
                parent.getOrderKey(), inbound.getOrderKey());
    }

    /**
     * order_status 전이의 유일한 내부 통로.
     *
     * DB 조건부 UPDATE 로 역행을 원자적으로 차단하고, 갱신 성공(updated > 0) 시만 엔티티 동기화.
     * stale 엔티티의 늦은 콜백은 DB 에서 0 row 로 걸러져 메모리도 안 바뀐다.
     *
     * @return 실제 전이 성공 여부 (false = 차단되어 무시됨)
     */
    private boolean transition(TbWcsShuttleOrder shuttle, ShuttleOrderStatus to, boolean force) {
        if (ValueUtil.isEmpty(shuttle)) return false;
        Integer prev = shuttle.getOrderStatus();
        Integer next = to.codeAsIntOrNull();

        int updated = shuttleOrderRepository.transitionOrderStatus(shuttle.getOrderKey(), next, force);
        if (updated == 0) {
            logger.warn("[ Order ][ Shuttle ] transition blocked - orderKey={}, prev={}, target={}, force={}",
                    shuttle.getOrderKey(), prev, to.name(), force);
            return false;
        }
        // DB 성공 후에만 메모리 동기화
        shuttle.setOrderStatus(next);
        logger.info("[ Order ][ Shuttle ] state transition - orderKey={}, prev={}, new={}",
                shuttle.getOrderKey(), prev, to.name());
        return true;
    }
}
