package operato.logis.asrs.query.grade;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.grade.model.ItemActivityDailyView;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * SKU 일별 활동 집계 조회 전용 서비스.
 *
 * <p>
 * 외부 조회는 business key 기준(areaCode, itemCode, activityDate)으로 수행한다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ItemActivityQueryService extends AbstractQueryService {

    private final LocationQueryService locationQueryService;
    private final ItemQueryService itemQueryService;

    /**
     * 영역코드 + 일자 기준 집계 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param activityDate 집계 일자
     * @return 집계 목록
     */
    public List<ItemActivityDailyView> findDailyActivities(String areaCode, LocalDate activityDate) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (activityDate == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "activityDate is null.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);

        String sql =
                "select " +
                        "    d.id as item_activity_daily_id, " +
                        "    d.center_id, " +
                        "    c.center_code, " +
                        "    d.area_id, " +
                        "    a.area_code, " +
                        "    d.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    d.activity_date, " +
                        "    d.inbound_count, " +
                        "    d.outbound_count, " +
                        "    d.outbound_qty, " +
                        "    d.partial_out_count, " +
                        "    d.return_in_count, " +
                        "    d.move_count, " +
                        "    d.avg_dwell_days, " +
                        "    d.demand_tomorrow_qty, " +
                        "    d.score_raw " +
                        "  from logis_asrs.tb_ac_item_activity_daily d " +
                        "  join logis_asrs.tb_ac_center c " +
                        "    on d.center_id = c.id " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on d.area_id = a.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on d.item_id = i.id " +
                        " where d.domain_id = :domainId " +
                        "   and c.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and d.area_id = :areaId " +
                        "   and d.activity_date = :activityDate " +
                        " order by i.item_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,activityDate",
                Domain.currentDomainId(), area.getId(), Date.valueOf(activityDate)
        );

        return this.queryManager.selectListBySql(sql, param, ItemActivityDailyView.class, 0, 0);
    }

    /**
     * 영역코드 + 품목코드 + 일자 기준 집계 단건 조회.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @param activityDate 집계 일자
     * @return 집계 단건
     */
    public ItemActivityDailyView findDailyActivity(String areaCode, String itemCode, LocalDate activityDate) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(itemCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "itemCode is empty.");
        }
        if (activityDate == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "activityDate is null.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        TbAcItemMaster item = itemQueryService.findItemByCode(itemCode);

        String sql =
                "select " +
                        "    d.id as item_activity_daily_id, " +
                        "    d.center_id, " +
                        "    c.center_code, " +
                        "    d.area_id, " +
                        "    a.area_code, " +
                        "    d.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    d.activity_date, " +
                        "    d.inbound_count, " +
                        "    d.outbound_count, " +
                        "    d.outbound_qty, " +
                        "    d.partial_out_count, " +
                        "    d.return_in_count, " +
                        "    d.move_count, " +
                        "    d.avg_dwell_days, " +
                        "    d.demand_tomorrow_qty, " +
                        "    d.score_raw " +
                        "  from logis_asrs.tb_ac_item_activity_daily d " +
                        "  join logis_asrs.tb_ac_center c " +
                        "    on d.center_id = c.id " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on d.area_id = a.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on d.item_id = i.id " +
                        " where d.domain_id = :domainId " +
                        "   and c.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and d.area_id = :areaId " +
                        "   and d.item_id = :itemId " +
                        "   and d.activity_date = :activityDate ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,itemId,activityDate",
                Domain.currentDomainId(), area.getId(), item.getId(), Date.valueOf(activityDate)
        );

        List<ItemActivityDailyView> list =
                this.queryManager.selectListBySql(sql, param, ItemActivityDailyView.class, 0, 0);

        if (list == null || list.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Item activity not found. areaCode=" + areaCode + ", itemCode=" + itemCode + ", activityDate=" + activityDate
            );
        }

        return list.get(0);
    }
}