package operato.logis.asrs.core.stock;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.core.common.SequenceValueGenerator;
import operato.logis.asrs.core.common.AcTxnNoGenerator;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.core.item.ItemPolicyResolveCore;
import operato.logis.asrs.dto.request.InboundStockRequest;
import operato.logis.asrs.dto.request.MoveStockRequest;
import operato.logis.asrs.dto.request.PutawayStockRequest;
import operato.logis.asrs.dto.response.StockCommandResult;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcLocation;
import operato.logis.asrs.entity.TbAcLot;
import operato.logis.asrs.entity.TbAcStockTxn;
import operato.logis.asrs.entity.TbAcStockUnit;
import operato.logis.asrs.enums.AcStockStatus;
import operato.logis.asrs.enums.AcStockUnitType;
import operato.logis.asrs.enums.AcTxnType;
import operato.logis.asrs.enums.AcYn;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import operato.logis.asrs.query.stock.StockQueryService;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCommandCore extends AbstractQueryService {

    private final StockQueryService stockQueryService;
    private final LocationQueryService locationQueryService;
    private final ItemQueryService itemQueryService;
    private final ItemPolicyResolveCore itemPolicyResolveCore;

    private final AcTxnNoGenerator txnNoGenerator;
    private final SequenceValueGenerator sequenceValueGenerator;

    @Transactional
    public StockCommandResult inbound(InboundStockRequest request) {
        validateInboundRequest(request);

        String resolvedStockUnitNo = resolveStockUnitNo(request);
        String resolvedStockUnitType = resolveStockUnitType(request);
        String resolvedRefDocType = resolveRefDocType(request);
        String resolvedRefDocNo = resolveRefDocNo(request, resolvedRefDocType);

        if (stockQueryService.findAnyByStockUnitNoOrNull(resolvedStockUnitNo) != null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.DUPLICATE_STOCK_UNIT,
                    "Duplicate stock unit no. stockUnitNo=" + resolvedStockUnitNo
            );
        }

        TbAcItemMaster item = itemQueryService.findItemByCode(request.getItemCode());
        TbAcLocation targetLocation = locationQueryService.findLocationByCode(
                request.getAreaCode(), request.getLocationCode()
        );

        validateInboundTargetLocation(targetLocation);
        validateInboundItemPolicy(targetLocation.getAreaId(), item.getId(), request.getLotNo());
        validateMixedLoadRule(targetLocation, item.getId(), null);

        TbAcLot lot = resolveLotOrNull(request.getItemCode(), request.getLotNo());

        Date now = new Date();

        TbAcStockUnit stockUnit = new TbAcStockUnit();
        stockUnit.setStockUnitNo(resolvedStockUnitNo);
        stockUnit.setItemId(item.getId());
        stockUnit.setLotId(lot == null ? null : lot.getId());
        stockUnit.setCurrentLocationId(targetLocation.getId());
        stockUnit.setStockUnitType(resolvedStockUnitType);
        stockUnit.setQty(request.getQty());
        stockUnit.setReservedQty(0);
        stockUnit.setStockStatusCode(AcStockStatus.AVAILABLE.name());
        stockUnit.setInboundAt(now);
        stockUnit.setLastMovedAt(now);
        stockUnit.setHoldYn(AcYn.N.name());
        stockUnit.setActiveYn(AcYn.Y.name());

        this.queryManager.insert(stockUnit);

        String txnNo = writeTxn(
                AcTxnType.INBOUND,
                stockUnit,
                null,
                targetLocation.getId(),
                request.getQty(),
                resolvedRefDocType,
                resolvedRefDocNo,
                request.getRefLineNo(),
                request.getReasonCode(),
                request.getRemark(),
                now
        );

        log.info(
                "[AisleCore][INBOUND] stockUnitNo={}, refDocNo={}, itemCode={}, areaCode={}, locationCode={}, qty={}",
                resolvedStockUnitNo,
                resolvedRefDocNo,
                request.getItemCode(),
                request.getAreaCode(),
                request.getLocationCode(),
                request.getQty()
        );

        return StockCommandResult.builder()
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.INBOUND.name())
                .fromLocationId(null)
                .toLocationId(targetLocation.getId())
                .qty(stockUnit.getQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .message("Inbound completed.")
                .build();
    }

    @Transactional
    public StockCommandResult putaway(PutawayStockRequest request) {
        validatePutawayRequest(request);

        TbAcStockUnit stockUnit = stockQueryService.findByStockUnitNo(request.getStockUnitNo());
        TbAcLocation fromLocation = locationQueryService.findLocation(stockUnit.getCurrentLocationId());
        TbAcLocation targetLocation = locationQueryService.findLocationByCode(
                request.getAreaCode(), request.getTargetLocationCode()
        );

        validateMovableStockUnit(stockUnit);
        validateMoveTargetLocation(targetLocation);

        if (fromLocation.getId().equals(targetLocation.getId())) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_TARGET,
                    "Putaway target is same as current location. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        validateMixedLoadRule(targetLocation, stockUnit.getItemId(), stockUnit.getId());

        Date now = new Date();

        stockUnit.setCurrentLocationId(targetLocation.getId());
        stockUnit.setLastMovedAt(now);
        this.queryManager.update(stockUnit, "currentLocationId", "lastMovedAt");

        String txnNo = writeTxn(
                AcTxnType.PUTAWAY,
                stockUnit,
                fromLocation.getId(),
                targetLocation.getId(),
                stockUnit.getQty(),
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo(),
                request.getReasonCode(),
                request.getRemark(),
                now
        );

        log.info("[AisleCore][PUTAWAY] stockUnitNo={}, fromLocationCode={}, toLocationCode={}",
                stockUnit.getStockUnitNo(), fromLocation.getLocationCode(), targetLocation.getLocationCode());

        return StockCommandResult.builder()
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.PUTAWAY.name())
                .fromLocationId(fromLocation.getId())
                .toLocationId(targetLocation.getId())
                .qty(stockUnit.getQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .message("Putaway completed.")
                .build();
    }

    @Transactional
    public StockCommandResult move(MoveStockRequest request) {
        validateMoveRequest(request);

        TbAcStockUnit stockUnit = stockQueryService.findByStockUnitNo(request.getStockUnitNo());
        TbAcLocation fromLocation = locationQueryService.findLocation(stockUnit.getCurrentLocationId());
        TbAcLocation targetLocation = locationQueryService.findLocationByCode(
                request.getAreaCode(), request.getTargetLocationCode()
        );

        validateMovableStockUnit(stockUnit);
        validateMoveTargetLocation(targetLocation);

        if (fromLocation.getId().equals(targetLocation.getId())) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_TARGET,
                    "Move target is same as current location. stockUnitNo=" + stockUnit.getStockUnitNo()
            );
        }

        validateMixedLoadRule(targetLocation, stockUnit.getItemId(), stockUnit.getId());

        Date now = new Date();

        stockUnit.setCurrentLocationId(targetLocation.getId());
        stockUnit.setLastMovedAt(now);
        this.queryManager.update(stockUnit, "currentLocationId", "lastMovedAt");

        String txnNo = writeTxn(
                AcTxnType.MOVE,
                stockUnit,
                fromLocation.getId(),
                targetLocation.getId(),
                stockUnit.getQty(),
                request.getRefDocType(),
                request.getRefDocNo(),
                request.getRefLineNo(),
                request.getReasonCode(),
                request.getRemark(),
                now
        );

        log.info("[AisleCore][MOVE] stockUnitNo={}, fromLocationCode={}, toLocationCode={}",
                stockUnit.getStockUnitNo(), fromLocation.getLocationCode(), targetLocation.getLocationCode());

        return StockCommandResult.builder()
                .stockUnitId(stockUnit.getId())
                .stockUnitNo(stockUnit.getStockUnitNo())
                .txnNo(txnNo)
                .txnType(AcTxnType.MOVE.name())
                .fromLocationId(fromLocation.getId())
                .toLocationId(targetLocation.getId())
                .qty(stockUnit.getQty())
                .stockStatusCode(stockUnit.getStockStatusCode())
                .message("Move completed.")
                .build();
    }

    private void validateInboundRequest(InboundStockRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "Inbound request is null.");
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
        if (request.getQty() == null || request.getQty() <= 0) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "qty must be greater than zero.");
        }
    }

    private void validatePutawayRequest(PutawayStockRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "Putaway request is null.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitNo is empty.");
        }
        if (ValueUtil.isEmpty(request.getAreaCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getTargetLocationCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "targetLocationCode is empty.");
        }
    }

    private void validateMoveRequest(MoveStockRequest request) {
        if (request == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "Move request is null.");
        }
        if (ValueUtil.isEmpty(request.getStockUnitNo())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "stockUnitNo is empty.");
        }
        if (ValueUtil.isEmpty(request.getAreaCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(request.getTargetLocationCode())) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "targetLocationCode is empty.");
        }
    }

    private String resolveStockUnitNo(InboundStockRequest request) {
        if (!ValueUtil.isEmpty(request.getStockUnitNo())) {
            return request.getStockUnitNo().trim();
        }
        return sequenceValueGenerator.stockUnitNoGenerate(request.getAreaCode());
    }

    private String resolveStockUnitType(InboundStockRequest request) {
        return AcStockUnitType.fromOrDefault(request.getStockUnitType(), AcStockUnitType.PALLET).name();
    }

    private String resolveRefDocType(InboundStockRequest request) {
        if (!ValueUtil.isEmpty(request.getRefDocType())) {
            return request.getRefDocType().trim();
        }
        return "INB";
    }

    private String resolveRefDocNo(InboundStockRequest request, String resolvedRefDocType) {
        if (!ValueUtil.isEmpty(request.getRefDocNo())) {
            return request.getRefDocNo().trim();
        }
        return sequenceValueGenerator.refDocNoGenerate(resolvedRefDocType);
    }

    private void validateInboundTargetLocation(TbAcLocation targetLocation) {
        if (!"ENABLED".equalsIgnoreCase(targetLocation.getUsageStatusCode())) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_TARGET,
                    "Inbound target location is not enabled. locationCode=" + targetLocation.getLocationCode()
            );
        }

        if (!AcYn.from(targetLocation.getInboundAllowedYn()).toBoolean()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_TARGET,
                    "Inbound target location is not allowed. locationCode=" + targetLocation.getLocationCode()
            );
        }
    }

    private void validateMoveTargetLocation(TbAcLocation targetLocation) {
        validateInboundTargetLocation(targetLocation);
    }

    private void validateMovableStockUnit(TbAcStockUnit stockUnit) {
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

    private void validateInboundItemPolicy(String areaId, String itemId, String lotNo) {
        boolean lotRequired = itemPolicyResolveCore.isLotControlRequired(areaId, itemId);
        boolean expiryRequired = itemPolicyResolveCore.isExpiryControlRequired(areaId, itemId);

        if ((lotRequired || expiryRequired) && ValueUtil.isEmpty(lotNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "lotNo is required by item policy. itemId=" + itemId
            );
        }
    }

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

    private TbAcLot resolveLotOrNull(String itemCode, String lotNo) {
        if (ValueUtil.isEmpty(lotNo)) {
            return null;
        }
        return itemQueryService.findLotByItemCodeAndLotNo(itemCode, lotNo);
    }

    private String writeTxn(AcTxnType txnType,
                            TbAcStockUnit stockUnit,
                            String fromLocationId,
                            String toLocationId,
                            Integer qty,
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