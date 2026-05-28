package operato.logis.wcs.service.impl.order.host;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.InventoryConstants;
import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.query.InventoryQueryStore;
import operato.logis.inventory.service.InventoryItemGroupService;
import operato.logis.inventory.service.InventorySettingService;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.consts.UomType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static operato.logis.wcs.common.util.lang.CommonUtils.nullToEmpty;
import static operato.logis.wcs.common.util.lang.CommonUtils.nz;
import static operato.logis.wcs.common.util.lang.CommonUtils.toInt;

/**
 * host_order 생성 전 가용성(Feasibility) 사전 검증.
 *
 * 재고 모듈 산출 SQL 의 AVAILABLE_CANDIDATES CTE 와 동일한 필터를 사용해
 * "검증은 통과했는데 산출에서 실패" 케이스를 사전에 차단한다.
 *
 * 검증 분기:
 *   - INBOUND  : 입고 가능 셀(무게/타입/그룹) 0건이면 거부
 *   - OUTBOUND : (owner, sku, lot) 별 가용 EA 합 < 요청 EA 면 거부
 *   - 그 외    : fromLocId 의 재고 존재 여부
 */
@Service
@RequiredArgsConstructor
public class HostOperationalDataValidator extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(HostOperationalDataValidator.class);

    // 마스터에 item_type 누락 시 fallback (InboundLocationAllocator 와 동일)
    private static final String DEFAULT_ITEM_TYPE = "PALLET";

    private final InventoryItemMasterRepository itemMasterRepository;
    private final InventoryQueryStore inventoryQueryStore;
    private final InventoryItemGroupService inventoryItemGroupService;
    private final InventorySettingService inventorySettingService;

    /**
     * 주문 타입에 맞는 가용성 검증 진입점.
     * INBOUND / OUTBOUND / 그 외(MOVE 등) 로 분기.
     */
    public FeasibilityResult check(HostOrderApi.Request request) {
        OrderType type = HostOrderFormatValidator.resolveBaseOrderType(request.getOrderType());
        return switch (type) {
            case INBOUND  -> checkInbound(request);
            case OUTBOUND -> checkOutbound(request);
            default       -> checkNonItemBased(request);
        };
    }

    /**
     * INBOUND 가용성 검증.
     * 무게/타입/그룹 필터로 입고 가능 셀이 1건 이상인지 확인.
     */
    private FeasibilityResult checkInbound(HostOrderApi.Request request) {
        // 마스터 일괄 로드 + 누락 검증
        ItemMasterValidation v = validateAndLoadMasters(request);
        if (v.isFailed()) return v.failure();

        // 산출 SQL 입력값 계산 (InboundLocationAllocator.buildRequest 와 동일 공식)
        Map<String, ExtTbInventoryItemMaster> masterMap = v.masterMap();
        String itemType = resolveItemType(request.getItems(), masterMap);
        int stockWeight = Math.max(calcTotalWeightKg(request, masterMap), 1);
        int stockHeight = 1;

        // 그룹 필터 결정 (overflow 옵션·dedicated 부재 시 PUBLIC 추가)
        List<ItemIdentifierDto> itemList = toItemIdentifierList(request);
        List<String> dedicatedList = resolveDedicatedGroupList(itemList);
        List<String> forbiddenList = resolveForbiddenGroupList(itemList);

        // 가용 셀 카운트 조회
        int cellCount = countAvailableCells(request.getEqGroupId(), itemType,
                stockHeight, stockWeight, dedicatedList, forbiddenList);

        // 0건이면 거부, 1건 이상이면 통과
        if (cellCount <= 0) {
            String desc = String.format(
                    "no available cell - eqGroupId=%s, itemType=%s, stockWeight=%dkg, dedicated=%s",
                    request.getEqGroupId(), itemType, stockWeight, dedicatedList);
            logger.warn("[ Feasibility ][ Inbound ] rejected - {}", desc);
            return FeasibilityResult.fail(WcsError.NO_AVAILABLE_LOCATION.code(), desc);
        }
        logger.info("[ Feasibility ][ Inbound ] passed - eqGroupId={}, itemType={}, stockWeight={}kg, cellCount={}",
                request.getEqGroupId(), itemType, stockWeight, cellCount);
        return FeasibilityResult.success();
    }

    /**
     * 입고 가능 셀 조회 시 사용할 item_type 을 결정한다.
     * 첫 번째 유효 아이템의 마스터 값 우선, 누락이면 DEFAULT_ITEM_TYPE(PALLET) 로 fallback.
     */
    private String resolveItemType(List<HostOrderApi.Item> items,
                                   Map<String, ExtTbInventoryItemMaster> masterMap) {
        if (ValueUtil.isEmpty(items)) return DEFAULT_ITEM_TYPE;
        for (HostOrderApi.Item item : items) {
            if (ValueUtil.isEmpty(item) || ValueUtil.isEmpty(item.getItemCode())) continue;
            ExtTbInventoryItemMaster master = masterMap.get(item.getItemCode());
            if (ValueUtil.isNotEmpty(master) && ValueUtil.isNotEmpty(master.getItemType())) {
                return master.getItemType();
            }
            break;
        }
        return DEFAULT_ITEM_TYPE;
    }

    /**
     * 총 무게(kg) 계산.
     * ceil(sum(item_weight × ea_qty)) — UOM 환산 포함.
     */
    private int calcTotalWeightKg(HostOrderApi.Request request,
                                  Map<String, ExtTbInventoryItemMaster> masterMap) {
        int total = 0;
        String ownerCode = request.getOwnerCode();
        for (HostOrderApi.Item item : request.getItems()) {
            // 잘못된 항목·수량 0 이하 스킵
            if (ValueUtil.isEmpty(item) || ValueUtil.isEmpty(item.getItemCode())) continue;
            int qty = nz(item.getQty());
            if (qty <= 0) continue;

            ExtTbInventoryItemMaster master = masterMap.get(item.getItemCode());
            if (ValueUtil.isEmpty(master)) continue;

            // UOM → EA 환산 후 마스터 단위 무게 곱
            int eaQty = convertToEaSafely(ownerCode, item.getItemCode(), item.getUom(), qty, masterMap);
            if (eaQty <= 0) continue;
            total += (int) Math.ceil(master.calculateWeightByEa(eaQty));
        }
        return total;
    }

    /**
     * dedicated 그룹 목록 결정.
     * overflow 옵션이 켜져있거나 dedicated 가 없으면 PUBLIC 그룹을 추가한다 (재고 모듈 산출 로직과 동일).
     */
    private List<String> resolveDedicatedGroupList(List<ItemIdentifierDto> itemList) {
        List<String> list = inventoryItemGroupService.getDedicatedGroupList(itemList);
        if (ValueUtil.isEmpty(list)) list = new ArrayList<>();

        boolean overflowAllowed = InventoryConstants.OPTION_VALUE_TRUE.equals(
                inventorySettingService.getOptionValue(InventoryConstants.DEDICATED_OVERFLOW));
        if (list.isEmpty() || overflowAllowed) {
            list.add(InventoryConstants.PUBLIC_ITEM_GROUP);
        }
        return list;
    }

    /** 금지 그룹 목록 (없으면 빈 리스트). */
    private List<String> resolveForbiddenGroupList(List<ItemIdentifierDto> itemList) {
        List<String> list = inventoryItemGroupService.getForbiddenGroupList(itemList);
        return ValueUtil.isEmpty(list) ? new ArrayList<>() : list;
    }

    /** 입고 가능 셀 카운트 — 재고 모듈의 산출 SQL 호출. */
    private int countAvailableCells(String eqGroupId, String itemType,
                                    int stockHeight, int stockWeight,
                                    List<String> dedicatedList, List<String> forbiddenList) {
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,itemType,stockHeight,stockWeight,dedicatedGroupList,forbiddenGroupList",
                eqGroupId, itemType, stockHeight, stockWeight,
                String.join(",", dedicatedList), String.join(",", forbiddenList));

        String sql = """
                SELECT COUNT(*) AS cell_count
                FROM TB_INVENTORY_LOCATION L
                WHERE L.IS_INBOUND_ENABLED = TRUE
                  AND L.IS_ENABLED = TRUE
                  AND L.IS_PATH = FALSE
                  AND L.LOC_TYPE != 'Pillar'
                  AND (L.STOCK_ID IS NULL OR L.STOCK_ID = '')
                  AND (L.TASK_ID IS NULL OR L.TASK_ID = '')
                  AND L.LOC_GROUP = :eqGroupId
                  AND L.ITEM_TYPE = :itemType
                  AND L.MAX_HEIGHT >= :stockHeight
                  AND L.MAX_WEIGHT >= :stockWeight
                  AND L.ITEM_GROUP = ANY(string_to_array(:dedicatedGroupList::text, ','))
                  AND (
                    :forbiddenGroupList::text = ''
                    OR L.ITEM_GROUP != ALL(string_to_array(:forbiddenGroupList::text, ','))
                  )
                """;
        return queryIntColumn(sql, params, "cell_count");
    }

    /**
     * 그룹 조회용 ItemIdentifier 목록 변환.
     * 빈 항목·코드 누락은 제외.
     */
    private List<ItemIdentifierDto> toItemIdentifierList(HostOrderApi.Request request) {
        List<ItemIdentifierDto> result = new ArrayList<>();
        String ownerCode = request.getOwnerCode();
        if (ValueUtil.isEmpty(request.getItems())) return result;
        for (HostOrderApi.Item item : request.getItems()) {
            if (ValueUtil.isEmpty(item) || ValueUtil.isEmpty(item.getItemCode())) continue;
            ItemIdentifierDto dto = new ItemIdentifierDto();
            dto.setItemOwner(ownerCode);
            dto.setItemCode(item.getItemCode());
            dto.setItemQty(nz(item.getQty()));
            result.add(dto);
        }
        return result;
    }

    /**
     * OUTBOUND 가용성 검증.
     * (owner, sku, lot) 별 가용 EA 합 ≥ 요청 EA 인지 확인.
     */
    private FeasibilityResult checkOutbound(HostOrderApi.Request request) {
        // 마스터 일괄 로드 + 누락 검증
        ItemMasterValidation v = validateAndLoadMasters(request);
        if (v.isFailed()) return v.failure();

        // 요청 EA 를 (owner, sku, lot) 기준으로 합산
        Map<ItemKey, Integer> requested = aggregateRequestedEa(request, v.masterMap());
        String eqGroupId = request.getEqGroupId();

        // 한 키라도 가용 < 요청이면 거부
        for (Map.Entry<ItemKey, Integer> entry : requested.entrySet()) {
            ItemKey k = entry.getKey();
            int requestedEa = entry.getValue();
            int availableEa = sumAvailableEa(eqGroupId, k.owner(), k.sku(), k.lot());

            if (availableEa < requestedEa) {
                String desc = String.format(
                        "insufficient stock - owner=%s, sku=%s, lot=%s, requestedEa=%d, availableEa=%d",
                        k.owner(), k.sku(), nullToEmpty(k.lot()), requestedEa, availableEa);
                logger.warn("[ Feasibility ][ Outbound ] rejected - {}", desc);
                return FeasibilityResult.fail(WcsError.INSUFFICIENT_STOCK.code(), desc);
            }
        }
        logger.info("[ Feasibility ][ Outbound ] passed - eqGroupId={}, itemKinds={}", eqGroupId, requested.size());
        return FeasibilityResult.success();
    }

    /**
     * 요청 항목의 EA 합산.
     * UOM 환산 후 (owner, sku, lot) 키로 합친다.
     */
    private Map<ItemKey, Integer> aggregateRequestedEa(HostOrderApi.Request request,
                                                       Map<String, ExtTbInventoryItemMaster> masterMap) {
        Map<ItemKey, Integer> result = new HashMap<>();
        String ownerCode = request.getOwnerCode();

        for (HostOrderApi.Item item : request.getItems()) {
            // 잘못된 항목·수량 0 이하 스킵
            if (ValueUtil.isEmpty(item) || !StringUtils.hasText(item.getItemCode())) continue;
            int qty = nz(item.getQty());
            if (qty <= 0) continue;

            // EA 환산 후 키 단위 누적
            int eaQty = convertToEaSafely(ownerCode, item.getItemCode(), item.getUom(), qty, masterMap);
            if (eaQty <= 0) continue;

            result.merge(new ItemKey(ownerCode, item.getItemCode(), item.getLotNo()), eaQty, Integer::sum);
        }
        return result;
    }

    /** (owner, sku, lot) 별 가용 EA 합 조회. */
    private int sumAvailableEa(String eqGroupId, String ownerCode, String itemCode, String lotNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("eqGroupId", eqGroupId);
        params.put("itemOwner", ownerCode);
        params.put("itemCode", itemCode);
        params.put("lotNo", StringUtils.hasText(lotNo) ? lotNo : null);

        String sql = """
                SELECT COALESCE(SUM(S.ITEM_QTY), 0) AS total_qty
                FROM TB_INVENTORY_STOCK S
                JOIN TB_INVENTORY_LOCATION L ON S.STOCK_ID = L.STOCK_ID
                WHERE S.ITEM_OWNER = :itemOwner
                  AND S.ITEM_CODE  = :itemCode
                  AND S.ITEM_QTY   > 0
                  AND S.IS_ENABLED = TRUE
                  AND S.STOCK_STATUS = 0
                  AND (S.ATTRIBUTE_A IS NULL OR S.ATTRIBUTE_A = '')
                  AND L.LOC_GROUP  = :eqGroupId
                  AND L.IS_OUTBOUND_ENABLED = TRUE
                  AND L.IS_ENABLED = TRUE
                  AND (L.TASK_ID IS NULL OR L.TASK_ID = '')
                  AND (:lotNo::varchar IS NULL OR :lotNo::varchar = '' OR S.LOT_NO = :lotNo::varchar)
                """;
        return queryIntColumn(sql, params, "total_qty");
    }

    /**
     * 아이템 기반이 아닌 주문(MOVE 등) 의 가용성 검증.
     * fromLocId 에 재고가 존재하는지 확인.
     */
    private FeasibilityResult checkNonItemBased(HostOrderApi.Request request) {
        int stockAtFrom = countStockAt(request.getEqGroupId(), request.getFromLocId());
        if (stockAtFrom <= 0) {
            String desc = String.format("no stock at source - orderType=%s, fromLocId=%s",
                    request.getOrderType(), request.getFromLocId());
            logger.warn("[ Feasibility ][ {} ] rejected - {}", request.getOrderType(), desc);
            return FeasibilityResult.fail(WcsError.INSUFFICIENT_STOCK.code(), desc);
        }
        return FeasibilityResult.success();
    }

    /** 특정 위치의 가용(stock_status=0, NORMAL) 재고 합계. */
    private int countStockAt(String eqGroupId, String locId) {
        String sql = """
                SELECT COALESCE(SUM(s.item_qty), 0) AS total_qty
                  FROM tb_inventory_stock s
                  JOIN tb_inventory_location l
                    ON l.stock_id = s.stock_id AND l.loc_group = :eqGroupId
                 WHERE l.loc_id       = :locId
                   AND l.task_id      IS NULL
                   AND s.item_qty     > 0
                   AND s.stock_status = 0
                   AND s.stock_type   = :normalStockType
                   AND (s.is_enabled IS NULL OR s.is_enabled = true)
                """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,locId,normalStockType",
                eqGroupId, locId, StockType.NORMAL.code());
        return queryIntColumn(sql, params, "total_qty");
    }

    /**
     * 요청 항목들의 마스터를 일괄 로드하고 누락 여부 검증.
     * 항목·소유주 누락 시 skipped, 일부라도 마스터 누락 시 fail, 모두 OK 면 마스터맵 반환.
     */
    private ItemMasterValidation validateAndLoadMasters(HostOrderApi.Request request) {
        List<HostOrderApi.Item> items = request.getItems();
        String ownerCode = request.getOwnerCode();
        if (ValueUtil.isEmpty(items) || !StringUtils.hasText(ownerCode)) {
            return ItemMasterValidation.skipped();
        }

        // SKU 코드 중복 제거 후 일괄 조회
        List<String> skuCodes = items.stream()
                .filter(i -> ValueUtil.isNotEmpty(i) && StringUtils.hasText(i.getItemCode()))
                .map(HostOrderApi.Item::getItemCode)
                .distinct()
                .toList();

        Map<String, ExtTbInventoryItemMaster> masterMap =
                itemMasterRepository.findAsMapByOwnerAndCodes(ownerCode, skuCodes);

        // 누락 SKU 수집 → 한 건이라도 있으면 실패
        List<String> missing = new ArrayList<>();
        for (String sku : skuCodes) {
            if (!masterMap.containsKey(sku)) missing.add(sku);
        }
        if (ValueUtil.isNotEmpty(missing)) {
            String desc = String.format(
                    "item master not found - owner=%s, missingSkuCodes=%s",
                    ownerCode, missing);
            logger.warn("[ Feasibility ] master not found - {}", desc);
            return ItemMasterValidation.fail(
                    FeasibilityResult.fail(WcsError.INVALID_ORDER_ITEM.code(), desc));
        }
        return ItemMasterValidation.ok(masterMap);
    }

    /**
     * UOM 수량을 EA 로 환산.
     * EA 면 그대로, 아니면 마스터의 box_qty/pallet_qty 적용.
     * 환산 실패는 풀스택 ERROR 후 0 반환(거부 흐름으로 이어짐).
     */
    private int convertToEaSafely(String ownerCode, String itemCode, String uomCode, int qty,
                                  Map<String, ExtTbInventoryItemMaster> masterMap) {
        if (qty <= 0) return 0;
        UomType uom = UomType.fromOrDefault(uomCode);
        if (uom == UomType.EA) return qty;

        ExtTbInventoryItemMaster master = masterMap.get(itemCode);
        if (ValueUtil.isEmpty(master)) {
            logger.warn("[ Feasibility ] master not found - owner={}, sku={}, uom={}", ownerCode, itemCode, uomCode);
            return 0;
        }
        try {
            return master.toEaQty(uom, qty);
        } catch (IllegalStateException e) {
            // 스택트레이스 보존 — e 를 마지막 인자로
            logger.error("[ Feasibility ] EA conversion failed - sku={}, uom={}, qty={}", itemCode, uomCode, qty, e);
            return 0;
        }
    }

    /** SQL 의 단일 숫자 컬럼 조회. 결과 없으면 0. */
    private int queryIntColumn(String sql, Map<String, Object> params, String columnName) {
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return 0;
        return toInt(rows.get(0).get(columnName));
    }

    /**
     * 가용성 검증 결과.
     * Success / Failure 만 허용하는 sealed 타입 — 호출측은 pattern matching 으로 분기 가능.
     */
    public sealed interface FeasibilityResult permits FeasibilityResult.Success, FeasibilityResult.Failure {

        boolean isSuccess();

        /** 검증 통과. */
        record Success() implements FeasibilityResult {
            @Override public boolean isSuccess() { return true; }
        }

        /** 검증 실패 — 에러 코드와 상세 메시지 동반. */
        record Failure(String errorCode, String errorDesc) implements FeasibilityResult {
            @Override public boolean isSuccess() { return false; }
        }

        static FeasibilityResult success() {
            return new Success();
        }

        static FeasibilityResult fail(String errorCode, String errorDesc) {
            return new Failure(errorCode, errorDesc);
        }
    }

    /** OUTBOUND 합산 키 — lot 미지정은 null 로 정규화. */
    private record ItemKey(String owner, String sku, String lot) {
        ItemKey(String owner, String sku, String lot) {
            this.owner = owner;
            this.sku = sku;
            this.lot = StringUtils.hasText(lot) ? lot : null;
        }
    }

    /** 마스터 로드/검증 중간 결과 — 실패 시 failure, 성공 시 masterMap 보유. */
    private record ItemMasterValidation(FeasibilityResult failure, Map<String, ExtTbInventoryItemMaster> masterMap) {

        static ItemMasterValidation ok(Map<String, ExtTbInventoryItemMaster> masterMap) {
            return new ItemMasterValidation(null, masterMap);
        }

        static ItemMasterValidation fail(FeasibilityResult failure) {
            return new ItemMasterValidation(failure, Map.of());
        }

        static ItemMasterValidation skipped() {
            return new ItemMasterValidation(null, Map.of());
        }

        boolean isFailed() {
            return ValueUtil.isNotEmpty(failure);
        }
    }
}
