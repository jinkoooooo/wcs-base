package operato.logis.asrs.core.stock;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.core.common.AcTxnNoGenerator;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.core.item.ItemPolicyResolveCore;
import operato.logis.asrs.dto.request.FullOutboundRequest;
import operato.logis.asrs.dto.request.PartialOutboundRequest;
import operato.logis.asrs.dto.request.ReturnInboundRequest;
import operato.logis.asrs.dto.response.StockCommandResult;
import operato.logis.asrs.dto.response.StockOutboundResult;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcLocation;
import operato.logis.asrs.entity.TbAcLot;
import operato.logis.asrs.entity.TbAcStockAllocation;
import operato.logis.asrs.entity.TbAcStockTxn;
import operato.logis.asrs.entity.TbAcStockUnit;
import operato.logis.asrs.enums.AcStockStatus;
import operato.logis.asrs.enums.AcTxnType;
import operato.logis.asrs.enums.AcYn;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import operato.logis.asrs.query.stock.StockAllocationQueryService;
import operato.logis.asrs.query.stock.StockQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 재고 출고 / 재입고 코어.
 *
 * <p>
 * 외부 입력은 business key 기준으로 받고,
 * 내부 저장/FK 처리 시에만 row id 를 사용한다.
 * </p>
 *
 * <p>
 * 1차 구현 범위:
 * </p>
 * <ul>
 *   <li>부분출고 (PARTIAL_OUT)</li>
 *   <li>전체출고 (FULL_OUT)</li>
 *   <li>재입고 (RETURN_IN)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockOutboundCore extends AbstractQueryService {

    private final StockQueryService stockQueryService;
    private final StockAllocationQueryService stockAllocationQueryService;
    private final ItemQueryService itemQueryService;
    private final LocationQueryService locationQueryService;
    private final ItemPolicyResolveCore itemPolicyResolveCore;
    private final AcTxnNoGenerator txnNoGenerator;

    /**
     * 재고 부분출고.
     *
     * <p>
     * 1차 기준 부분출고는 기존 stock_unit 의 qty 를 차감하는 방식으로 처리한다.
     * split child stock 생성은 2차 확장으로 둔다.
     * </p>
     *
     * @param request 부분출고 요청
     * @return 처리 결과
     */
    @Transactional
    public StockOutboundResult partialOutbound(PartialOutboundRequest request) {
        validatePartialOutboundRequest(request);

        TbAcStockUnit stockUnit = stockQueryService.findByStockUnitNo(request.getStockUnitNo());
        validateOutboundStockUnit(stockUnit);

        int currentQty = safeInt(stockUnit.getQty());
        int outboundQty = safeInt(request.getOutboundQty());

        if (outboundQty >= currentQty) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_OUTBOUND_QTY,
                    "For full quantity outbound, use full-out API. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        int consumedReservedQty = consumeAllocationsForOutbound(
                stockUnit,
                outboundQty,
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo()
        );

        int newQty = currentQty - outboundQty;
        int newReservedQty = safeInt(stockUnit.getReservedQty()) - consumedReservedQty;

        if (newQty < 0 || newReservedQty < 0) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_OUTBOUND_QTY,
                    "Outbound calculation became negative. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        Date now = new Date();

        stockUnit.setQty(newQty);
        stockUnit.setReservedQty(newReservedQty);
        stockUnit.setStockStatusCode(resolvePostOutboundStatus(newQty, newReservedQty, stockUnit.getHoldYn()));
        stockUnit.setLastMovedAt(now);

        this.queryManager.update(stockUnit, "qty", "reservedQty", "stockStatusCode", "lastMovedAt");

        String txnNo = writeOutboundTxn(
                AcTxnType.PARTIAL_OUT,
                stockUnit,
                outboundQty,
                stockUnit.getCurrentLocationId(),
                null,
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo(),
                request.getReasonCode(),
                request.getRemark(),
                now
        );

        log.info("[AisleCore][PARTIAL_OUT] stockUnitNo={}, qty={}, remainingQty={}",
                stockUnit.getStockUnitNo(), outboundQty, newQty);

        return StockOutboundResult.builder()
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.PARTIAL_OUT.name())
                .outboundQty(outboundQty)
                .remainingQty(stockUnit.getQty())
                .reservedQty(stockUnit.getReservedQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .refDocNo(request.getRefDocNo())
                .message("Partial outbound completed.")
                .build();
    }

    /**
     * 재고 전체출고.
     *
     * <p>
     * 1차 기준 전체출고는 현재 잔량 전체를 출고하고,
     * stock_status_code = OUT, active_yn = N 으로 종료 처리한다.
     * </p>
     *
     * @param request 전체출고 요청
     * @return 처리 결과
     */
    @Transactional
    public StockOutboundResult fullOutbound(FullOutboundRequest request) {
        validateFullOutboundRequest(request);

        TbAcStockUnit stockUnit = stockQueryService.findByStockUnitNo(request.getStockUnitNo());
        validateOutboundStockUnit(stockUnit);

        int currentQty = safeInt(stockUnit.getQty());
        if (currentQty <= 0) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_OUTBOUND_QTY,
                    "No quantity to outbound. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        int consumedReservedQty = consumeAllocationsForOutbound(
                stockUnit,
                currentQty,
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo()
        );

        int newReservedQty = safeInt(stockUnit.getReservedQty()) - consumedReservedQty;
        if (newReservedQty < 0) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_OUTBOUND_QTY,
                    "Reserved quantity became negative. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        Date now = new Date();

        stockUnit.setQty(0);
        stockUnit.setReservedQty(newReservedQty);
        stockUnit.setStockStatusCode(AcStockStatus.OUT.name());
        stockUnit.setActiveYn(AcYn.N.name());
        stockUnit.setLastMovedAt(now);

        this.queryManager.update(stockUnit, "qty", "reservedQty", "stockStatusCode", "activeYn", "lastMovedAt");

        String txnNo = writeOutboundTxn(
                AcTxnType.FULL_OUT,
                stockUnit,
                currentQty,
                stockUnit.getCurrentLocationId(),
                null,
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo(),
                request.getReasonCode(),
                request.getRemark(),
                now
        );

        log.info("[AisleCore][FULL_OUT] stockUnitNo={}, qty={}",
                stockUnit.getStockUnitNo(), currentQty);

        return StockOutboundResult.builder()
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.FULL_OUT.name())
                .outboundQty(currentQty)
                .remainingQty(stockUnit.getQty())
                .reservedQty(stockUnit.getReservedQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .refDocNo(request.getRefDocNo())
                .message("Full outbound completed.")
                .build();
    }

    /**
     * 재고 재입고.
     *
     * <p>
     * 1차 기준 재입고는 신규 stock_unit 생성 방식으로 처리한다.
     * originalStockUnitNo 가 존재하면 parent_stock_unit_id 로 연결한다.
     * </p>
     *
     * @param request 재입고 요청
     * @return 처리 결과
     */
    @Transactional
    public StockCommandResult returnInbound(ReturnInboundRequest request) {
        validateReturnInboundRequest(request);

        if (stockQueryService.findAnyByStockUnitNoOrNull(request.getStockUnitNo()) != null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.DUPLICATE_STOCK_UNIT,
                    "Duplicate stock unit no. stockUnitNo=" + request.getStockUnitNo()
            );
        }

        TbAcItemMaster item = itemQueryService.findItemByCode(request.getItemCode());
        TbAcLocation targetLocation = locationQueryService.findLocationByCode(
                request.getAreaCode(), request.getLocationCode()
        );

        validateReturnInboundTargetLocation(targetLocation);
        validateReturnInboundItemPolicy(targetLocation.getAreaId(), item.getId(), request.getLotNo());
        validateMixedLoadRule(targetLocation, item.getId(), null);

        TbAcLot lot = resolveLotOrNull(request.getItemCode(), request.getLotNo());
        TbAcStockUnit originalStockUnit = resolveOriginalStockUnitOrNull(request.getOriginalStockUnitNo());

        Date now = new Date();

        TbAcStockUnit stockUnit = new TbAcStockUnit();
        stockUnit.setStockUnitNo(request.getStockUnitNo());
        stockUnit.setParentStockUnitId(originalStockUnit == null ? null : originalStockUnit.getId());
        stockUnit.setItemId(item.getId());
        stockUnit.setLotId(lot == null ? null : lot.getId());
        stockUnit.setCurrentLocationId(targetLocation.getId());
        stockUnit.setStockUnitType(request.getStockUnitType());
        stockUnit.setQty(request.getQty());
        stockUnit.setReservedQty(0);
        stockUnit.setStockStatusCode(AcStockStatus.AVAILABLE.name());
        stockUnit.setInboundAt(now);
        stockUnit.setLastMovedAt(now);
        stockUnit.setHoldYn(AcYn.N.name());
        stockUnit.setActiveYn(AcYn.Y.name());

        this.queryManager.insert(stockUnit);

        String txnNo = writeOutboundTxn(
                AcTxnType.RETURN_IN,
                stockUnit,
                request.getQty(),
                null,
                targetLocation.getId(),
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo(),
                request.getReasonCode(),
                request.getRemark(),
                now
        );

        log.info("[AisleCore][RETURN_IN] stockUnitNo={}, itemCode={}, areaCode={}, locationCode={}, qty={}",
                request.getStockUnitNo(), request.getItemCode(), request.getAreaCode(), request.getLocationCode(), request.getQty());

        return StockCommandResult.builder()
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.RETURN_IN.name())
                .fromLocationId(null)
                .toLocationId(targetLocation.getId())
                .qty(stockUnit.getQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .message("Return inbound completed.")
                .build();
    }

    /**
     * 부분출고 요청 기본 검증.
     */
    private void validatePartialOutboundRequest(PartialOutboundRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "PartialOutbound request is null.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitNo is empty.");
        }
        if (request.getOutboundQty() == null || request.getOutboundQty() <= 0) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_OUTBOUND_QTY, "outboundQty must be greater than zero.");
        }
    }

    /**
     * 전체출고 요청 기본 검증.
     */
    private void validateFullOutboundRequest(FullOutboundRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "FullOutbound request is null.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitNo is empty.");
        }
    }

    /**
     * 재입고 요청 기본 검증.
     */
    private void validateReturnInboundRequest(ReturnInboundRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "ReturnInbound request is null.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitNo is empty.");
        }
        if (ValueUtil.isEmpty(request.getItemCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "itemCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getAreaCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getLocationCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "locationCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitType())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitType is empty.");
        }
        if (request.getQty() == null || request.getQty() <= 0) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "qty must be greater than zero.");
        }
    }

    /**
     * 출고 가능한 재고 상태인지 검증.
     *
     * <p>
     * 1차 기준:
     * </p>
     * <ul>
     *   <li>OUT 불가</li>
     *   <li>HOLD 불가</li>
     * </ul>
     */
    private void validateOutboundStockUnit(TbAcStockUnit stockUnit) {
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
     * 재입고 대상 위치 검증.
     */
    private void validateReturnInboundTargetLocation(TbAcLocation targetLocation) {
        if (!"ENABLED".equalsIgnoreCase(targetLocation.getUsageStatusCode())) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_TARGET,
                    "Return target location is not enabled. locationCode=" + targetLocation.getLocationCode()
            );
        }

        if (!AcYn.from(targetLocation.getInboundAllowedYn()).toBoolean()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_TARGET,
                    "Return target location is not allowed. locationCode=" + targetLocation.getLocationCode()
            );
        }
    }

    /**
     * 재입고 시 상품 정책 검증.
     */
    private void validateReturnInboundItemPolicy(String areaId, String itemId, String lotNo) {
        boolean lotRequired = itemPolicyResolveCore.isLotControlRequired(areaId, itemId);
        boolean expiryRequired = itemPolicyResolveCore.isExpiryControlRequired(areaId, itemId);

        if ((lotRequired || expiryRequired) && ValueUtil.isEmpty(lotNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "lotNo is required by item policy. itemId=" + itemId
            );
        }
    }

    /**
     * 혼적 허용 여부 검증.
     *
     * <p>
     * 1차 기준:
     * location mixed_load_yn = N 이거나 item mixed load 미허용이면
     * 빈 위치여야 한다.
     * </p>
     */
    private void validateMixedLoadRule(TbAcLocation targetLocation,
                                       String itemId,
                                       String currentStockUnitId) {

        List<TbAcStockUnit> locatedStocks = stockQueryService.findActiveStockByLocation(targetLocation.getId());

        boolean locationAllowsMixed = AcYn.from(targetLocation.getMixedLoadYn()).toBoolean();
        boolean itemAllowsMixed = itemPolicyResolveCore.isMixedLoadAllowed(targetLocation.getAreaId(), itemId);

        if (locationAllowsMixed && itemAllowsMixed) {
            return;
        }

        for (TbAcStockUnit locatedStock : locatedStocks) {
            if (!ValueUtil.isEmpty(currentStockUnitId) && currentStockUnitId.equals(locatedStock.getId())) {
                continue;
            }

            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_TARGET,
                    "Mixed load is not allowed. targetLocationCode=" + targetLocation.getLocationCode()
            );
        }
    }

    /**
     * 출고 시 활성 할당을 소비한다.
     *
     * <p>
     * 규칙:
     * </p>
     * <ul>
     *   <li>활성 할당이 없으면 reserved_qty 가 0 이어야 direct outbound 허용</li>
     *   <li>활성 할당이 있으면 refDocNo 기반으로 matching allocation 을 찾아 소비</li>
     *   <li>다른 문서가 예약한 수량을 침범하는 출고는 허용하지 않음</li>
     * </ul>
     *
     * @param stockUnit 재고 단위
     * @param outboundQty 출고 수량
     * @param refDocType 참조 문서 유형
     * @param refDocNo 참조 문서 번호
     * @param refLineNo 참조 문서 라인 번호
     * @return 실제 예약수량에서 차감한 수량
     */
    private int consumeAllocationsForOutbound(TbAcStockUnit stockUnit,
                                              Integer outboundQty,
                                              String refDocType,
                                              String refDocNo,
                                              String refLineNo) {

        int reservedQty = safeInt(stockUnit.getReservedQty());
        List<TbAcStockAllocation> activeAllocations =
                stockAllocationQueryService.findActiveAllocationsByStockUnitId(stockUnit.getId());

        if (activeAllocations == null || activeAllocations.isEmpty()) {
            if (reservedQty > 0) {
                throw new AisleCoreException(
                        AisleCoreErrorCode.INVALID_ALLOCATION_QTY,
                        "Reserved quantity exists but active allocation rows do not exist. stockUnitNo=" + stockUnit.getStockUnitNo()
                );
            }
            return 0;
        }

        if (ValueUtil.isEmpty(refDocNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "refDocNo is required when reserved allocation exists. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        List<TbAcStockAllocation> matchingAllocations =
                stockAllocationQueryService.findActiveAllocationsByRefDoc(
                        stockUnit.getId(), refDocType, refDocNo, refLineNo
                );

        if (matchingAllocations == null || matchingAllocations.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Matching active allocation not found. stockUnitNo=" + stockUnit.getStockUnitNo() + ", refDocNo=" + refDocNo
            );
        }

        int matchingAllocatedQty = sumAllocatedQty(matchingAllocations);
        int totalQty = safeInt(stockUnit.getQty());
        int availableQty = totalQty - reservedQty;
        int maxOutboundQtyWithoutOtherReservationViolation = availableQty + matchingAllocatedQty;

        if (outboundQty > maxOutboundQtyWithoutOtherReservationViolation) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INSUFFICIENT_QTY,
                    "Outbound qty exceeds available + matching reserved qty. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        // outbound 요청분 중 matching allocation 에서 실제 소비해야 할 예약수량
        int reservedConsumeQty = Math.min(outboundQty, matchingAllocatedQty);
        int remainingToConsume = reservedConsumeQty;

        for (TbAcStockAllocation allocation : matchingAllocations) {
            if (remainingToConsume <= 0) {
                break;
            }

            int allocQty = safeInt(allocation.getAllocatedQty());

            if (allocQty <= remainingToConsume) {
                allocation.setAllocStatusCode("PICKED");
                this.queryManager.update(allocation, "allocStatusCode");
                remainingToConsume -= allocQty;
            } else {
                // 부분소비 시 남은 예약수량만 allocation row 에 유지
                allocation.setAllocatedQty(allocQty - remainingToConsume);
                this.queryManager.update(allocation, "allocatedQty");
                remainingToConsume = 0;
            }
        }

        return reservedConsumeQty;
    }

    /**
     * 출고 후 재고 상태 계산.
     *
     * @param qty 남은 수량
     * @param reservedQty 남은 예약수량
     * @param holdYn 보류 여부
     * @return 재고 상태 코드
     */
    private String resolvePostOutboundStatus(int qty, int reservedQty, String holdYn) {
        if (qty <= 0) {
            return AcStockStatus.OUT.name();
        }
        if (AcYn.from(holdYn).toBoolean()) {
            return AcStockStatus.HOLD.name();
        }
        if (reservedQty > 0) {
            return AcStockStatus.RESERVED.name();
        }
        return AcStockStatus.AVAILABLE.name();
    }

    /**
     * 기존 출고 재고를 parent 로 연결할 필요가 있을 때 원 재고 조회.
     *
     * @param originalStockUnitNo 원 재고 단위 번호
     * @return TbAcStockUnit 또는 null
     */
    private TbAcStockUnit resolveOriginalStockUnitOrNull(String originalStockUnitNo) {
        if (ValueUtil.isEmpty(originalStockUnitNo)) {
            return null;
        }
        return stockQueryService.findAnyByStockUnitNo(originalStockUnitNo);
    }

    /**
     * LOT번호 기준 LOT row 조회.
     *
     * @param itemCode 품목코드
     * @param lotNo LOT번호
     * @return TbAcLot 또는 null
     */
    private TbAcLot resolveLotOrNull(String itemCode, String lotNo) {
        if (ValueUtil.isEmpty(lotNo)) {
            return null;
        }
        return itemQueryService.findLotByItemCodeAndLotNo(itemCode, lotNo);
    }

    /**
     * allocation 목록의 할당 수량 합계.
     */
    private int sumAllocatedQty(List<TbAcStockAllocation> allocations) {
        int sum = 0;
        if (allocations == null) {
            return sum;
        }

        for (TbAcStockAllocation allocation : allocations) {
            sum += safeInt(allocation.getAllocatedQty());
        }
        return sum;
    }

    /**
     * null-safe integer 변환.
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 출고/재입고 트랜잭션 1건 생성.
     *
     * @param txnType 트랜잭션 유형
     * @param stockUnit 재고 단위
     * @param qty 처리 수량
     * @param fromLocationId 출발 위치
     * @param toLocationId 도착 위치
     * @param refDocType 참조 문서 유형
     * @param refDocNo 참조 문서 번호
     * @param refLineNo 참조 문서 라인 번호
     * @param reasonCode 사유 코드
     * @param remark 비고
     * @param txnAt 처리 시각
     * @return 생성된 txnNo
     */
    private String writeOutboundTxn(AcTxnType txnType,
                                    TbAcStockUnit stockUnit,
                                    Integer qty,
                                    String fromLocationId,
                                    String toLocationId,
                                    String refDocType,
                                    String refDocNo,
                                    String refLineNo,
                                    String reasonCode,
                                    String remark,
                                    Date txnAt) {

        String txnNo = txnNoGenerator.generateTxnNo(txnType.name());

        TbAcStockTxn txn = new TbAcStockTxn();
        txn.setTxnNo(txnNo);
        txn.setTxnType(txnType.name());
        txn.setStockUnitId(stockUnit.getId());
        txn.setItemId(stockUnit.getItemId());
        txn.setLotId(stockUnit.getLotId());
        txn.setFromLocationId(fromLocationId);
        txn.setToLocationId(toLocationId);
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