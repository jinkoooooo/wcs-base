package operato.logis.asrs.core.stock;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.core.common.AcTxnNoGenerator;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.request.AllocateStockRequest;
import operato.logis.asrs.dto.request.ReleaseAllocationRequest;
import operato.logis.asrs.dto.response.StockAllocationResult;
import operato.logis.asrs.entity.TbAcStockAllocation;
import operato.logis.asrs.entity.TbAcStockTxn;
import operato.logis.asrs.entity.TbAcStockUnit;
import operato.logis.asrs.enums.AcStockStatus;
import operato.logis.asrs.enums.AcTxnType;
import operato.logis.asrs.query.stock.StockAllocationQueryService;
import operato.logis.asrs.query.stock.StockQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 재고 할당/할당해제 코어.
 *
 * <p>
 * 외부 입력은 business key 기준(stockUnitNo, refDocNo)으로 받고,
 * 내부 저장은 row id 기반으로 처리한다.
 * </p>
 *
 * <p>
 * 1차 구현 범위:
 * </p>
 * <ul>
 *   <li>ALLOCATE: 예약수량 증가 + allocation row 생성 + txn 기록</li>
 *   <li>RELEASE_ALLOC: 활성 allocation row 해제 + 예약수량 감소 + txn 기록</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAllocationCore extends AbstractQueryService {

    private final StockQueryService stockQueryService;
    private final StockAllocationQueryService stockAllocationQueryService;
    private final AcTxnNoGenerator txnNoGenerator;

    /**
     * 재고 할당.
     *
     * @param request 할당 요청
     * @return 처리 결과
     */
    @Transactional
    public StockAllocationResult allocate(AllocateStockRequest request) {
        validateAllocateRequest(request);

        TbAcStockUnit stockUnit = stockQueryService.findByStockUnitNo(request.getStockUnitNo());

        validateAllocatableStockUnit(stockUnit);
        validateAllocationQty(stockUnit, request.getAllocatedQty());

        java.util.Date now = new java.util.Date();

        TbAcStockAllocation allocation = new TbAcStockAllocation();
        allocation.setStockUnitId(stockUnit.getId());
        allocation.setItemId(stockUnit.getItemId());
        allocation.setAllocatedQty(request.getAllocatedQty());
        allocation.setAllocStatusCode("ALLOCATED");
        allocation.setRefDocType(request.getRefDocType());
        allocation.setRefDocNo(request.getRefDocNo());
        allocation.setRefLineNo(request.getRefLineNo());
        allocation.setDueDate(parseDueDate(request.getDueDate()));
        allocation.setAllocatedAt(now);

        this.queryManager.insert(allocation);

        int newReservedQty = safeInt(stockUnit.getReservedQty()) + request.getAllocatedQty();
        stockUnit.setReservedQty(newReservedQty);

        // 예약이 발생하면 상태를 RESERVED 로 승격
        if (newReservedQty > 0) {
            stockUnit.setStockStatusCode(AcStockStatus.RESERVED.name());
            this.queryManager.update(stockUnit, "reservedQty", "stockStatusCode");
        } else {
            this.queryManager.update(stockUnit, "reservedQty");
        }

        String txnNo = writeTxn(
                AcTxnType.ALLOCATE,
                stockUnit,
                request.getAllocatedQty(),
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo(),
                null,
                request.getRemark(),
                now
        );

        log.info("[AisleCore][ALLOCATE] stockUnitNo={}, qty={}, refDocNo={}",
                stockUnit.getStockUnitNo(), request.getAllocatedQty(), request.getRefDocNo());

        return StockAllocationResult.builder()
                .allocationId(allocation.getId())
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.ALLOCATE.name())
                .qty(request.getAllocatedQty())
                .reservedQty(stockUnit.getReservedQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .refDocNo(request.getRefDocNo())
                .message("Allocation completed.")
                .build();
    }

    /**
     * 재고 할당 해제.
     *
     * <p>
     * 1차는 stockUnitNo + refDocNo(+refLineNo) 기준으로
     * 활성 allocation row 를 찾아 일괄 해제한다.
     * </p>
     *
     * @param request 해제 요청
     * @return 처리 결과
     */
    @Transactional
    public StockAllocationResult releaseAllocation(ReleaseAllocationRequest request) {
        validateReleaseRequest(request);

        TbAcStockUnit stockUnit = stockQueryService.findByStockUnitNo(request.getStockUnitNo());

        List<TbAcStockAllocation> allocations = stockAllocationQueryService.findActiveAllocationsByRefDoc(
                stockUnit.getId(),
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo()
        );

        if (allocations == null || allocations.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Active allocation not found. stockUnitNo=" + request.getStockUnitNo()
                            + ", refDocNo=" + request.getRefDocNo()
            );
        }

        int releasedQty = 0;
        java.util.Date now = new java.util.Date();
        String lastAllocationId = null;

        for (TbAcStockAllocation allocation : allocations) {
            allocation.setAllocStatusCode("RELEASED");
            this.queryManager.update(allocation, "allocStatusCode");

            releasedQty += safeInt(allocation.getAllocatedQty());
            lastAllocationId = allocation.getId();
        }

        int newReservedQty = safeInt(stockUnit.getReservedQty()) - releasedQty;
        if (newReservedQty < 0) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_ALLOCATION_QTY,
                    "Reserved quantity became negative. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        stockUnit.setReservedQty(newReservedQty);

        // 예약이 전부 해제되면 상태를 AVAILABLE 로 복귀
        if (newReservedQty == 0 && !AcStockStatus.HOLD.name().equalsIgnoreCase(stockUnit.getStockStatusCode())) {
            stockUnit.setStockStatusCode(AcStockStatus.AVAILABLE.name());
            this.queryManager.update(stockUnit, "reservedQty", "stockStatusCode");
        } else {
            this.queryManager.update(stockUnit, "reservedQty");
        }

        String txnNo = writeTxn(
                AcTxnType.RELEASE_ALLOC,
                stockUnit,
                releasedQty,
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo(),
                request.getReasonCode(),
                request.getRemark(),
                now
        );

        log.info("[AisleCore][RELEASE_ALLOC] stockUnitNo={}, qty={}, refDocNo={}",
                stockUnit.getStockUnitNo(), releasedQty, request.getRefDocNo());

        return StockAllocationResult.builder()
                .allocationId(lastAllocationId)
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.RELEASE_ALLOC.name())
                .qty(releasedQty)
                .reservedQty(stockUnit.getReservedQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .refDocNo(request.getRefDocNo())
                .message("Allocation released.")
                .build();
    }

    /**
     * 할당 요청 기본 검증.
     */
    private void validateAllocateRequest(AllocateStockRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "Allocate request is null.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitNo is empty.");
        }
        if (request.getAllocatedQty() == null || request.getAllocatedQty() <= 0) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_ALLOCATION_QTY, "allocatedQty must be greater than zero.");
        }
        if (ValueUtil.isEmpty(request.getRefDocNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "refDocNo is empty.");
        }
    }

    /**
     * 해제 요청 기본 검증.
     */
    private void validateReleaseRequest(ReleaseAllocationRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "Release request is null.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitNo is empty.");
        }
        if (ValueUtil.isEmpty(request.getRefDocNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "refDocNo is empty.");
        }
    }

    /**
     * 할당 가능한 재고 상태인지 검증.
     *
     * <p>
     * 1차 기준:
     * </p>
     * <ul>
     *   <li>OUT 불가</li>
     *   <li>HOLD 불가</li>
     * </ul>
     */
    private void validateAllocatableStockUnit(TbAcStockUnit stockUnit) {
        AcStockStatus stockStatus = AcStockStatus.from(stockUnit.getStockStatusCode());

        if (stockStatus.isOut()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_STOCK_STATUS,
                    "Stock unit already out. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        if (stockStatus.isHold()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_STOCK_STATUS,
                    "Stock unit is hold. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }
    }

    /**
     * 할당 수량 검증.
     *
     * <p>
     * 가용수량 = qty - reserved_qty
     * </p>
     *
     * @param stockUnit 재고 단위
     * @param allocateQty 할당 수량
     */
    private void validateAllocationQty(TbAcStockUnit stockUnit, Integer allocateQty) {
        int qty = safeInt(stockUnit.getQty());
        int reservedQty = safeInt(stockUnit.getReservedQty());
        int availableQty = qty - reservedQty;

        if (allocateQty == null || allocateQty <= 0) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_ALLOCATION_QTY,
                    "allocatedQty must be greater than zero."
            );
        }

        if (availableQty < allocateQty) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INSUFFICIENT_QTY,
                    "Insufficient available quantity. stockUnitNo=" + stockUnit.getStockUnitNo()
                            + ", availableQty=" + availableQty
                            + ", requestQty=" + allocateQty
            );
        }
    }

    /**
     * dueDate 문자열(yyyy-MM-dd)을 Date 로 변환한다.
     *
     * <p>
     * 없으면 null 반환.
     * </p>
     */
    private Date parseDueDate(String dueDate) {
        if (ValueUtil.isEmpty(dueDate)) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(dueDate));
    }

    /**
     * null-safe integer 변환.
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 할당/해제 이력 1건 생성.
     *
     * <p>
     * allocation 은 물리적 위치 이동이 아니므로 from/to location 은 기록하지 않는다.
     * </p>
     */
    private String writeTxn(AcTxnType txnType,
                            TbAcStockUnit stockUnit,
                            Integer qty,
                            String refDocType,
                            String refDocNo,
                            String refLineNo,
                            String reasonCode,
                            String remark,
                            java.util.Date txnAt) {

        String txnNo = txnNoGenerator.generateTxnNo(txnType.name());

        TbAcStockTxn txn = new TbAcStockTxn();
        txn.setTxnNo(txnNo);
        txn.setTxnType(txnType.name());
        txn.setStockUnitId(stockUnit.getId());
        txn.setItemId(stockUnit.getItemId());
        txn.setLotId(stockUnit.getLotId());
        txn.setFromLocationId(null);
        txn.setToLocationId(null);
        txn.setQty(qty);
        txn.setRefDocType(refDocType);
        txn.setRefDocNo(refDocNo);
        txn.setRefLineNo(refLineNo);
        txn.setReasonCode(reasonCode);
        txn.setRemark(remark);
        txn.setTxnAt(txnAt);

        this.queryManager.insert(txn);
        return txnNo;
    }
}