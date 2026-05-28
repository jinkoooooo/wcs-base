package operato.logis.wcs.service.impl.inventory.state;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

/**
 * inventory_stock 상태/카테고리 변경의 단일 창구.
 *
 * 호출자는 StockStatus / StockType 을 직접 넘기지 않고 비즈니스 의미별 전용 메서드를 호출한다
 * (시험 통과, 입고 완료, 출고 예약, 이동 롤백, 운영자 액션 등).
 * 모든 상태 변경에 자동 로깅 - 운영 추적 일관성 보장.
 */
@Service
@RequiredArgsConstructor
public class InventoryStockStateWriter {

    private static final Logger logger = LoggerFactory.getLogger(InventoryStockStateWriter.class);

    private final InventoryStockRepository stockRepository;

    /** BCR 스캔 - INBOUND_READY → INBOUND. */
    @Transactional(rollbackFor = Exception.class)
    public int markInboundBcrConfirmed(String eqGroupId, String stockId) {
        return applyStatus(eqGroupId, stockId, StockStatus.INBOUND, StockStatus.INBOUND_READY,
                "INBOUND_BCR_CONFIRMED");
    }

    /** 입고 완료 (시험/NIA 불필요) - IDLE + NORMAL. */
    @Transactional(rollbackFor = Exception.class)
    public int markInboundCompleteNormal(String eqGroupId, String stockId) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.IDLE, StockType.NORMAL,
                "INBOUND_COMPLETE_NORMAL");
    }

    /** 입고 완료 (시험 결과 대기) - HOLD + QC_PENDING. */
    @Transactional(rollbackFor = Exception.class)
    public int markInboundCompleteAwaitingTest(String eqGroupId, String stockId) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.QC_PENDING,
                "INBOUND_COMPLETE_AWAITING_TEST");
    }

    /** 입고 완료 (국검 미승인 대기) - HOLD + NIA_PENDING. */
    @Transactional(rollbackFor = Exception.class)
    public int markInboundCompleteAwaitingNia(String eqGroupId, String stockId) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.NIA_PENDING,
                "INBOUND_COMPLETE_AWAITING_NIA");
    }

    /** 반품 입고 완료 - HOLD + RETURN. */
    @Transactional(rollbackFor = Exception.class)
    public int markInboundCompleteReturnGoods(String eqGroupId, String stockId) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.RETURN,
                "INBOUND_COMPLETE_RETURN");
    }

    /**
     * HOST 단계 예약 진입 - IDLE → HOST_PENDING.
     * host_order 에 from 이 명시되어 있을 때 호출 (수동 출고, 지정 위치 입고 등).
     * 셔틀 생성 시 markOutboundReserved / markRelocationReserved 가 이 상태에서 진행 상태로 전이한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markHostPending(String eqGroupId, String stockId) {
        return applyStatus(eqGroupId, stockId, StockStatus.HOST_PENDING, StockStatus.IDLE,
                "HOST_PENDING_RESERVED");
    }

    /**
     * HOST 단계 예약 롤백 - HOST_PENDING → IDLE.
     * host_order 취소/실패 시 호출. 이미 셔틀 단계로 전이됐으면 CAS 실패로 자동 no-op.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markIdleAfterHostPendingRollback(String eqGroupId, String stockId) {
        return applyStatus(eqGroupId, stockId, StockStatus.IDLE, StockStatus.HOST_PENDING,
                "HOST_PENDING_ROLLBACK");
    }

    /** 시험 통과 - IDLE + NORMAL. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockQcPassed(String eqGroupId, String stockId) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.IDLE, StockType.NORMAL,
                "QC_PASSED");
    }

    /** 시험 통과했으나 국검 추가 대기 - HOLD + NIA_PENDING. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockQcPassedAwaitingNia(String eqGroupId, String stockId) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.NIA_PENDING,
                "QC_PASSED_AWAITING_NIA");
    }

    /**
     * 시험 부적합 - HOLD + QC_FAIL.
     * 화면 표시만, 자동 라우팅 없음. 운영자가 직접 반품/폐기 결정.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markStockQcFailed(String eqGroupId, String stockId) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.QC_FAIL,
                "QC_FAILED");
    }

    /**
     * 출고 예약 - IDLE / HOST_PENDING → OUTBOUND.
     * 일반 출고는 IDLE 에서, host 단계 예약을 거친 출고는 HOST_PENDING 에서 진입.
     * CAS 패턴으로 두 시작점을 순차 시도한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markOutboundReserved(String eqGroupId, String stockId) {

        // 1차: IDLE → OUTBOUND (일반 흐름)
        int updated = applyStatus(eqGroupId, stockId, StockStatus.OUTBOUND, StockStatus.IDLE,
                "OUTBOUND_RESERVED");
        if (updated > 0) return updated;

        // 2차: HOST_PENDING → OUTBOUND (host 단계 예약 경유)
        return applyStatus(eqGroupId, stockId, StockStatus.OUTBOUND, StockStatus.HOST_PENDING,
                "OUTBOUND_RESERVED_FROM_HOST_PENDING");
    }

    /** 시험 대상 출고 - OUTBOUND + QC_PENDING (stock_type 유지). */
    @Transactional(rollbackFor = Exception.class)
    public int markStockOutboundForSampleTest(String eqGroupId, String stockId) {
        return markStockOutboundForSampleTest(eqGroupId, stockId, null);
    }

    /**
     * 시험 대상 출고 - OUTBOUND + QC_PENDING + audit reason 동반.
     * 운영자가 단일 stock 을 샘플 출고할 때 사유 기록용.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markStockOutboundForSampleTest(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.OUTBOUND, StockType.QC_PENDING,
                "OUTBOUND_SAMPLE_TEST", reason);
    }

    /**
     * 이동 예약 - IDLE / HOST_PENDING → RELOCATION.
     * 일반 이동은 IDLE 에서, host 단계 예약을 거친 이동은 HOST_PENDING 에서 진입.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markRelocationReserved(String eqGroupId, String stockId) {

        // 1차: IDLE → RELOCATION (일반 흐름)
        int updated = applyStatus(eqGroupId, stockId, StockStatus.RELOCATION, StockStatus.IDLE,
                "RELOCATION_RESERVED");
        if (updated > 0) return updated;

        // 2차: HOST_PENDING → RELOCATION (host 단계 예약 경유)
        return applyStatus(eqGroupId, stockId, StockStatus.RELOCATION, StockStatus.HOST_PENDING,
                "RELOCATION_RESERVED_FROM_HOST_PENDING");
    }

    /** 이동 완료 - RELOCATION → IDLE. */
    @Transactional(rollbackFor = Exception.class)
    public int markRelocationCompleted(String eqGroupId, String stockId) {
        return applyStatus(eqGroupId, stockId, StockStatus.IDLE, StockStatus.RELOCATION,
                "RELOCATION_COMPLETED");
    }

    /** 출고 예약 롤백 - OUTBOUND → IDLE. */
    @Transactional(rollbackFor = Exception.class)
    public int markIdleAfterOutboundRollback(String eqGroupId, String stockId) {
        return applyStatus(eqGroupId, stockId, StockStatus.IDLE, StockStatus.OUTBOUND,
                "OUTBOUND_ROLLBACK");
    }

    /** 이동 예약 롤백 - RELOCATION → IDLE. */
    @Transactional(rollbackFor = Exception.class)
    public int markIdleAfterRelocationRollback(String eqGroupId, String stockId) {
        return applyStatus(eqGroupId, stockId, StockStatus.IDLE, StockStatus.RELOCATION,
                "RELOCATION_ROLLBACK");
    }

    /**
     * 운영자 - 폐기 신청. HOLD + DISPOSAL.
     * reason 은 audit log 의 reason 컬럼에 "ACTION_LABEL: comment" 형태로 기록된다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markStockDisposalRequested(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.DISPOSAL,
                "OPERATOR_DISPOSAL", reason);
    }

    /** 운영자 - 반품 신청. HOLD + RETURN. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockReturnRequested(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.RETURN,
                "OPERATOR_RETURN", reason);
    }

    /** 운영자 - 정상 복귀 (예: 국검 승인). IDLE + NORMAL. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockNormalRestored(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.IDLE, StockType.NORMAL,
                "OPERATOR_NORMAL_RESTORE", reason);
    }

    /** 운영자 - 국검 대기 복귀. HOLD + NIA_PENDING. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockNiaPendingRestored(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.NIA_PENDING,
                "OPERATOR_NIA_PENDING_RESTORE", reason);
    }

    /** 운영자 - 국검 불승인. HOLD + NIA_FAIL. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockNiaRejected(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.NIA_FAIL,
                "OPERATOR_NIA_REJECT", reason);
    }

    /** 운영자 - 수동 시험 대기 지정. HOLD + QC_PENDING. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockQcPendingManual(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.QC_PENDING,
                "OPERATOR_QC_PENDING", reason);
    }

    /** 운영자 - 수동 부적합 지정. HOLD + QC_FAIL. */
    @Transactional(rollbackFor = Exception.class)
    public int markStockQcFailedManual(String eqGroupId, String stockId, String reason) {
        return applyStatusAndType(eqGroupId, stockId, StockStatus.HOLD, StockType.QC_FAIL,
                "OPERATOR_QC_FAIL", reason);
    }

    /**
     * 운영자 - 라인 단위 수량 절대값 보정(행 PK 키). 채취/박스출고 오기입 보정.
     * 기존 status/type 메서드가 (eqGroupId, stockId) 키인 것과 달리, 보정은 단일 라인(id)만 대상으로 한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int adjustStockQty(String stockRowId, int newQty, String reason) {
        int updated = stockRepository.updateItemQtyById(stockRowId, newQty, "OPERATOR_QTY_ADJUST: " + reason);
        logger.warn("[ Inventory ][ Correction ] adjustQty - id={}, newQty={}, rows={}", stockRowId, newQty, updated);
        return updated;
    }

    /** 운영자 - 라인 단위 비활성(이중입고/공출고 결과 0, 행 PK 키). 물리삭제 대신 is_enabled=false. */
    @Transactional(rollbackFor = Exception.class)
    public int disableStock(String stockRowId, String reason) {
        int updated = stockRepository.disableById(stockRowId, "OPERATOR_STOCK_DISABLE: " + reason);
        logger.warn("[ Inventory ][ Correction ] disable - id={}, rows={}", stockRowId, updated);
        return updated;
    }

    // status 단독 변경 + 로깅 (reason 없음 - 시스템 흐름)
    private int applyStatus(String eqGroupId, String stockId,
                            StockStatus nextStatus, StockStatus expectedCurrent, String label) {
        int updated = stockRepository.updateStockStatus(eqGroupId, stockId, nextStatus, expectedCurrent);
        logger.info("[ Inventory ][ State ] eqGroup={}, stockId={}, status->{}, expected={}, rows={}, label={}",
                eqGroupId, stockId, nextStatus.name(),
                ValueUtil.isEmpty(expectedCurrent) ? "*" : expectedCurrent.name(),
                updated, label);
        return updated;
    }

    // status + type 동시 변경 + 로깅 (reason 없음 - 시스템 흐름)
    private int applyStatusAndType(String eqGroupId, String stockId,
                                   StockStatus nextStatus, StockType nextType, String label) {
        return applyStatusAndType(eqGroupId, stockId, nextStatus, nextType, label, null);
    }

    // status + type 동시 변경 + 로깅 (reason 동반 - 운영자 흐름)
    private int applyStatusAndType(String eqGroupId, String stockId,
                                   StockStatus nextStatus, StockType nextType,
                                   String label, String reason) {
        String fullReason = ValueUtil.isEmpty(reason) ? null : label + ": " + reason;
        int updated = stockRepository.updateStockStatusAndType(eqGroupId, stockId, nextStatus, nextType, fullReason);
        logger.info("[ Inventory ][ State ] eqGroup={}, stockId={}, status->{}, type->{}, rows={}, label={}, reason={}",
                eqGroupId, stockId,
                ValueUtil.isEmpty(nextStatus) ? "-" : nextStatus.name(),
                ValueUtil.isEmpty(nextType) ? "-" : nextType.code(),
                updated, label, ValueUtil.isEmpty(reason) ? "-" : reason);
        return updated;
    }
}
