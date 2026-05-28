package operato.logis.asrs.core.grade;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.response.ItemActivityAggregationResult;
import operato.logis.asrs.entity.TbAcDemandPlan;
import operato.logis.asrs.entity.TbAcItemActivityDaily;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.location.LocationQueryService;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * SKU 일별 활동 집계 코어.
 *
 * <p>
 * 실제 DB 스키마 기준:
 * tb_ac_item_activity_daily(center_id, area_id, item_id, activity_date,
 * inbound_count, outbound_count, outbound_qty, partial_out_count,
 * return_in_count, move_count, avg_dwell_days, demand_tomorrow_qty, score_raw)
 * 를 사용한다.
 * </p>
 *
 * <p>
 * insert/update 는 공통 컬럼 자동반영을 위해 queryManager.insert/update 기준으로 처리한다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemActivityAggregationCore extends AbstractQueryService {

    private final LocationQueryService locationQueryService;

    /**
     * 영역 기준 일 집계 실행.
     *
     * @param areaCode 영역 코드
     * @param activityDate 집계 일자
     * @return 집계 결과
     */
    @Transactional
    public ItemActivityAggregationResult aggregateArea(String areaCode, LocalDate activityDate) {
        validateAggregateRequest(areaCode, activityDate);

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        List<ItemIdRow> targetItems = findTargetItems(area.getId(), activityDate);

        int upsertedCount = 0;
        for (ItemIdRow row : targetItems) {
            upsertActivityDaily(area, row.getItemId(), activityDate);
            upsertedCount++;
        }

        log.info("[AisleCore][ACTIVITY_AGG][AREA] areaCode={}, activityDate={}, targetItemCount={}, upsertedCount={}",
                areaCode, activityDate, targetItems.size(), upsertedCount);

        return ItemActivityAggregationResult.builder()
                .areaCode(areaCode)
                .itemCode(null)
                .activityDate(activityDate.toString())
                .targetItemCount(targetItems.size())
                .upsertedCount(upsertedCount)
                .message("Item activity aggregation completed by area.")
                .build();
    }

    /**
     * 영역 + 품목 기준 일 집계 실행.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @param activityDate 집계 일자
     * @return 집계 결과
     */
    @Transactional
    public ItemActivityAggregationResult aggregateItem(String areaCode, String itemCode, LocalDate activityDate) {
        validateAggregateRequest(areaCode, activityDate);

        if (ValueUtil.isEmpty(itemCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "itemCode is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        ItemIdRow itemRow = findItemIdByCode(itemCode);

        upsertActivityDaily(area, itemRow.getItemId(), activityDate);

        log.info("[AisleCore][ACTIVITY_AGG][ITEM] areaCode={}, itemCode={}, activityDate={}",
                areaCode, itemCode, activityDate);

        return ItemActivityAggregationResult.builder()
                .areaCode(areaCode)
                .itemCode(itemCode)
                .activityDate(activityDate.toString())
                .targetItemCount(1)
                .upsertedCount(1)
                .message("Item activity aggregation completed by item.")
                .build();
    }

    /**
     * 집계 대상 품목 목록 조회.
     *
     * <p>
     * 1차는 아래 2가지 소스에서 품목을 모은다.
     * </p>
     * <ul>
     *   <li>당일 stock txn 발생 품목</li>
     *   <li>명일 demand plan 존재 품목</li>
     * </ul>
     *
     * @param areaId 영역 row id
     * @param activityDate 집계 일자
     * @return 품목 목록
     */
    private List<ItemIdRow> findTargetItems(String areaId, LocalDate activityDate) {
        String sql =
                "select distinct z.item_id " +
                        "  from ( " +
                        "        select t.item_id " +
                        "          from logis_asrs.tb_ac_stock_txn t " +
                        "          left join logis_asrs.tb_ac_location lf " +
                        "            on t.from_location_id = lf.id " +
                        "          left join logis_asrs.tb_ac_location lt " +
                        "            on t.to_location_id = lt.id " +
                        "         where t.domain_id = :domainId " +
                        "           and coalesce(lt.area_id, lf.area_id) = :areaId " +
                        "           and t.txn_type in ('INBOUND', 'PUTAWAY', 'MOVE', 'PARTIAL_OUT', 'FULL_OUT', 'RETURN_IN') " +
                        "           and t.txn_at::date = :activityDate " +
                        "        union " +
                        "        select d.item_id " +
                        "          from logis_asrs.tb_ac_demand_plan d " +
                        "         where d.domain_id = :domainId " +
                        "           and d.area_id = :areaId " +
                        "           and d.active_yn = 'Y' " +
                        "           and d.demand_date = :tomorrowDate " +
                        "       ) z " +
                        " order by z.item_id ";

        java.util.Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,activityDate,tomorrowDate",
                Domain.currentDomainId(),
                areaId,
                Date.valueOf(activityDate),
                Date.valueOf(activityDate.plusDays(1))
        );

        return this.queryManager.selectListBySql(sql, param, ItemIdRow.class, 0, 0);
    }

    /**
     * 품목코드로 품목 row id 조회.
     *
     * @param itemCode 품목 코드
     * @return itemId row
     */
    private ItemIdRow findItemIdByCode(String itemCode) {
        String sql =
                "select i.id as item_id " +
                        "  from logis_asrs.tb_ac_item_master i " +
                        " where i.domain_id = :domainId " +
                        "   and i.item_code = :itemCode " +
                        "   and i.active_yn = 'Y' ";

        java.util.Map<String, Object> param = ValueUtil.newMap(
                "domainId,itemCode",
                Domain.currentDomainId(), itemCode
        );

        List<ItemIdRow> list = this.queryManager.selectListBySql(sql, param, ItemIdRow.class, 0, 0);
        if (list == null || list.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Item not found. itemCode=" + itemCode
            );
        }

        return list.get(0);
    }

    /**
     * 일 집계 row 생성/갱신.
     *
     * @param area 영역 엔티티
     * @param itemId 품목 row id
     * @param activityDate 집계 일자
     */
    private void upsertActivityDaily(TbAcStorageArea area, String itemId, LocalDate activityDate) {
        ItemActivityMetricsRow metrics = calculateMetrics(area.getId(), itemId, activityDate);

        TbAcItemActivityDaily daily = findExistingActivityDaily(area.getId(), itemId, activityDate);

        if (daily == null) {
            daily = new TbAcItemActivityDaily();
            daily.setCenterId(area.getCenterId());
            daily.setAreaId(area.getId());
            daily.setItemId(itemId);
            daily.setActivityDate(Date.valueOf(activityDate));

            daily.setInboundCount(safeInt(metrics.getInboundCount()));
            daily.setOutboundCount(safeInt(metrics.getOutboundCount()));
            daily.setOutboundQty(safeInt(metrics.getOutboundQty()));
            daily.setPartialOutCount(safeInt(metrics.getPartialOutCount()));
            daily.setReturnInCount(safeInt(metrics.getReturnInCount()));
            daily.setMoveCount(safeInt(metrics.getMoveCount()));
            daily.setAvgDwellDays(safeInt(metrics.getAvgDwellDays()));
            daily.setDemandTomorrowQty(safeInt(metrics.getDemandTomorrowQty()));
            daily.setScoreRaw(safeInt(metrics.getScoreRaw()));

            this.queryManager.insert(daily);
        } else {
            daily.setCenterId(area.getCenterId());
            daily.setInboundCount(safeInt(metrics.getInboundCount()));
            daily.setOutboundCount(safeInt(metrics.getOutboundCount()));
            daily.setOutboundQty(safeInt(metrics.getOutboundQty()));
            daily.setPartialOutCount(safeInt(metrics.getPartialOutCount()));
            daily.setReturnInCount(safeInt(metrics.getReturnInCount()));
            daily.setMoveCount(safeInt(metrics.getMoveCount()));
            daily.setAvgDwellDays(safeInt(metrics.getAvgDwellDays()));
            daily.setDemandTomorrowQty(safeInt(metrics.getDemandTomorrowQty()));
            daily.setScoreRaw(safeInt(metrics.getScoreRaw()));

            this.queryManager.update(
                    daily,
                    "centerId",
                    "inboundCount",
                    "outboundCount",
                    "outboundQty",
                    "partialOutCount",
                    "returnInCount",
                    "moveCount",
                    "avgDwellDays",
                    "demandTomorrowQty",
                    "scoreRaw"
            );
        }
    }

    /**
     * 기존 집계 row 조회.
     *
     * @param areaId 영역 row id
     * @param itemId 품목 row id
     * @param activityDate 집계 일자
     * @return 기존 집계 row 또는 null
     */
    private TbAcItemActivityDaily findExistingActivityDaily(String areaId, String itemId, LocalDate activityDate) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", areaId);
        condition.addFilter("item_id", itemId);
        condition.addFilter("activity_date", Date.valueOf(activityDate));

        return this.queryManager.select(TbAcItemActivityDaily.class, condition);
    }

    /**
     * 집계 지표 계산.
     *
     * @param areaId 영역 row id
     * @param itemId 품목 row id
     * @param activityDate 집계 일자
     * @return 계산 지표
     */
    private ItemActivityMetricsRow calculateMetrics(String areaId, String itemId, LocalDate activityDate) {
        String sql =
                "select " +
                        "    coalesce(sum(case when t.txn_type = 'INBOUND' then 1 else 0 end), 0) as inbound_count, " +
                        "    coalesce(sum(case when t.txn_type in ('PARTIAL_OUT', 'FULL_OUT') then 1 else 0 end), 0) as outbound_count, " +
                        "    coalesce(sum(case when t.txn_type in ('PARTIAL_OUT', 'FULL_OUT') then t.qty else 0 end), 0) as outbound_qty, " +
                        "    coalesce(sum(case when t.txn_type = 'PARTIAL_OUT' then 1 else 0 end), 0) as partial_out_count, " +
                        "    coalesce(sum(case when t.txn_type = 'RETURN_IN' then 1 else 0 end), 0) as return_in_count, " +
                        "    coalesce(sum(case when t.txn_type = 'MOVE' then 1 else 0 end), 0) as move_count, " +
                        "    coalesce(round(avg(case " +
                        "        when t.txn_type in ('PARTIAL_OUT', 'FULL_OUT') and su.inbound_at is not null " +
                        "        then extract(epoch from (t.txn_at - su.inbound_at)) / 86400.0 " +
                        "        else null end))::int, 0) as avg_dwell_days " +
                        "  from logis_asrs.tb_ac_stock_txn t " +
                        "  join logis_asrs.tb_ac_stock_unit su " +
                        "    on t.stock_unit_id = su.id " +
                        "  left join logis_asrs.tb_ac_location lf " +
                        "    on t.from_location_id = lf.id " +
                        "  left join logis_asrs.tb_ac_location lt " +
                        "    on t.to_location_id = lt.id " +
                        " where t.domain_id = :domainId " +
                        "   and su.domain_id = :domainId " +
                        "   and t.item_id = :itemId " +
                        "   and coalesce(lt.area_id, lf.area_id) = :areaId " +
                        "   and t.txn_type in ('INBOUND', 'PUTAWAY', 'MOVE', 'PARTIAL_OUT', 'FULL_OUT', 'RETURN_IN') " +
                        "   and t.txn_at::date = :activityDate ";

        java.util.Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,itemId,activityDate",
                Domain.currentDomainId(),
                areaId,
                itemId,
                Date.valueOf(activityDate)
        );

        List<BaseMetricsRow> baseList = this.queryManager.selectListBySql(sql, param, BaseMetricsRow.class, 0, 0);
        BaseMetricsRow base = (baseList == null || baseList.isEmpty()) ? new BaseMetricsRow() : baseList.get(0);

        int demandTomorrowQty = findDemandTomorrowQty(areaId, itemId, activityDate.plusDays(1));

        ItemActivityMetricsRow row = new ItemActivityMetricsRow();
        row.setInboundCount(safeInt(base.getInboundCount()));
        row.setOutboundCount(safeInt(base.getOutboundCount()));
        row.setOutboundQty(safeInt(base.getOutboundQty()));
        row.setPartialOutCount(safeInt(base.getPartialOutCount()));
        row.setReturnInCount(safeInt(base.getReturnInCount()));
        row.setMoveCount(safeInt(base.getMoveCount()));
        row.setAvgDwellDays(safeInt(base.getAvgDwellDays()));
        row.setDemandTomorrowQty(demandTomorrowQty);

        // 1차 raw score
        int scoreRaw =
                row.getOutboundQty() * 100
                        + row.getDemandTomorrowQty() * 50
                        + row.getOutboundCount() * 20
                        + row.getPartialOutCount() * 10
                        + row.getMoveCount() * 5
                        + row.getInboundCount() * 2
                        + row.getReturnInCount() * 5;

        row.setScoreRaw(scoreRaw);
        return row;
    }

    /**
     * 명일 수요 수량 조회.
     *
     * @param areaId 영역 row id
     * @param itemId 품목 row id
     * @param demandDate 수요 일자
     * @return 수요 수량
     */
    private int findDemandTomorrowQty(String areaId, String itemId, LocalDate demandDate) {
        String sql =
                "select coalesce(sum(d.demand_qty), 0) as demand_qty " +
                        "  from logis_asrs.tb_ac_demand_plan d " +
                        " where d.domain_id = :domainId " +
                        "   and d.area_id = :areaId " +
                        "   and d.item_id = :itemId " +
                        "   and d.active_yn = 'Y' " +
                        "   and d.demand_date = :demandDate ";

        java.util.Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,itemId,demandDate",
                Domain.currentDomainId(),
                areaId,
                itemId,
                Date.valueOf(demandDate)
        );

        List<DemandQtyRow> list = this.queryManager.selectListBySql(sql, param, DemandQtyRow.class, 0, 0);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        return safeInt(list.get(0).getDemandQty());
    }

    /**
     * 요청 기본 검증.
     */
    private void validateAggregateRequest(String areaCode, LocalDate activityDate) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (activityDate == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "activityDate is null.");
        }
    }

    /**
     * null-safe integer 변환.
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    @Getter
    @Setter
    public static class ItemIdRow {
        private String itemId;
    }

    @Getter
    @Setter
    public static class BaseMetricsRow {
        private Integer inboundCount;
        private Integer outboundCount;
        private Integer outboundQty;
        private Integer partialOutCount;
        private Integer returnInCount;
        private Integer moveCount;
        private Integer avgDwellDays;
    }

    @Getter
    @Setter
    public static class DemandQtyRow {
        private Integer demandQty;
    }

    @Getter
    @Setter
    public static class ItemActivityMetricsRow {
        private Integer inboundCount;
        private Integer outboundCount;
        private Integer outboundQty;
        private Integer partialOutCount;
        private Integer returnInCount;
        private Integer moveCount;
        private Integer avgDwellDays;
        private Integer demandTomorrowQty;
        private Integer scoreRaw;
    }
}