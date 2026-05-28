package operato.logis.asrs.query.location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import operato.logis.asrs.entity.TbAcStockUnit;
import org.springframework.stereotype.Service;

import operato.logis.asrs.entity.TbAcLocation;
import operato.logis.asrs.query.location.model.AccessPointOptionView;
import operato.logis.asrs.query.location.model.ItemCategoryOptionView;
import operato.logis.asrs.query.location.model.LocationListView;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 로케이션 관리 조회 서비스.
 */
@Service
public class LocationManageQueryService extends AbstractQueryService {

    public List<LocationListView> search(
            String areaCode,
            String locationCode,
            String locationType,
            String activeYn
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    l.id as id, ");
        sql.append("    sa.id as area_id, ");
        sql.append("    sa.area_code as area_code, ");
        sql.append("    sa.area_name as area_name, ");
        sql.append("    l.location_code as location_code, ");
        sql.append("    l.aisle_no as aisle_no, ");
        sql.append("    l.side_code as side_code, ");
        sql.append("    l.bay_no as bay_no, ");
        sql.append("    l.level_no as level_no, ");
        sql.append("    l.depth_no as depth_no, ");
        sql.append("    l.location_type as location_type, ");
        sql.append("    l.usage_status_code as usage_status_code, ");
        sql.append("    l.inbound_allowed_yn as inbound_allowed_yn, ");
        sql.append("    l.outbound_allowed_yn as outbound_allowed_yn, ");
        sql.append("    l.mixed_load_yn as mixed_load_yn, ");
        sql.append("    l.front_priority_yn as front_priority_yn, ");
        sql.append("    ic.id as dedicated_item_category_id, ");
        sql.append("    ic.category_code as dedicated_item_category_code, ");
        sql.append("    ic.category_name as dedicated_item_category_name, ");
        sql.append("    l.max_weight_g as max_weight_g, ");
        sql.append("    l.max_volume_mm3 as max_volume_mm3, ");
        sql.append("    l.sort_seq as sort_seq, ");
        sql.append("    l.active_yn as active_yn, ");
        sql.append("    l.location_grade as location_grade, ");
        sql.append("    l.access_score as access_score, ");
        sql.append("    ap.id as primary_access_point_id, ");
        sql.append("    ap.point_code as primary_access_point_code, ");
        sql.append("    ap.point_name as primary_access_point_name, ");
        sql.append("    cast(l.created_at as varchar) as created_at, ");
        sql.append("    cast(l.updated_at as varchar) as updated_at ");
        sql.append("from logis_asrs.tb_ac_location l ");
        sql.append("inner join logis_asrs.tb_ac_storage_area sa on sa.id = l.area_id ");
        sql.append("left join logis_asrs.tb_ac_item_category ic on ic.id = l.dedicated_item_category_id ");
        sql.append("left join logis_asrs.tb_ac_access_point ap on ap.id = l.primary_access_point_id ");
        sql.append("where l.domain_id = :domainId ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        if (areaCode != null && !areaCode.trim().isEmpty()) {
            sql.append("  and sa.area_code = :areaCode ");
            params.put("areaCode", areaCode.trim());
        }

        if (locationCode != null && !locationCode.trim().isEmpty()) {
            sql.append("  and lower(l.location_code) like lower(:locationCode) ");
            params.put("locationCode", "%" + locationCode.trim() + "%");
        }

        if (locationType != null && !locationType.trim().isEmpty()) {
            sql.append("  and l.location_type = :locationType ");
            params.put("locationType", locationType.trim());
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("  and l.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        }

        sql.append("order by sa.area_code asc, l.sort_seq asc, l.location_code asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                LocationListView.class,
                0,
                0
        );
    }

    public LocationListView getDetailByAreaAndLocationCode(String areaCode, String locationCode) {
        String sql = ""
                + "select "
                + "    l.id as id, "
                + "    sa.id as area_id, "
                + "    sa.area_code as area_code, "
                + "    sa.area_name as area_name, "
                + "    l.location_code as location_code, "
                + "    l.aisle_no as aisle_no, "
                + "    l.side_code as side_code, "
                + "    l.bay_no as bay_no, "
                + "    l.level_no as level_no, "
                + "    l.depth_no as depth_no, "
                + "    l.location_type as location_type, "
                + "    l.usage_status_code as usage_status_code, "
                + "    l.inbound_allowed_yn as inbound_allowed_yn, "
                + "    l.outbound_allowed_yn as outbound_allowed_yn, "
                + "    l.mixed_load_yn as mixed_load_yn, "
                + "    l.front_priority_yn as front_priority_yn, "
                + "    ic.id as dedicated_item_category_id, "
                + "    ic.category_code as dedicated_item_category_code, "
                + "    ic.category_name as dedicated_item_category_name, "
                + "    l.max_weight_g as max_weight_g, "
                + "    l.max_volume_mm3 as max_volume_mm3, "
                + "    l.sort_seq as sort_seq, "
                + "    l.active_yn as active_yn, "
                + "    l.location_grade as location_grade, "
                + "    l.access_score as access_score, "
                + "    ap.id as primary_access_point_id, "
                + "    ap.point_code as primary_access_point_code, "
                + "    ap.point_name as primary_access_point_name, "
                + "    cast(l.created_at as varchar) as created_at, "
                + "    cast(l.updated_at as varchar) as updated_at "
                + "from logis_asrs.tb_ac_location l "
                + "inner join logis_asrs.tb_ac_storage_area sa on sa.id = l.area_id "
                + "left join logis_asrs.tb_ac_item_category ic on ic.id = l.dedicated_item_category_id "
                + "left join logis_asrs.tb_ac_access_point ap on ap.id = l.primary_access_point_id "
                + "where l.domain_id = :domainId "
                + "  and sa.area_code = :areaCode "
                + "  and l.location_code = :locationCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaCode", areaCode);
        params.put("locationCode", locationCode);

        return this.queryManager.selectBySql(sql, params, LocationListView.class);
    }

    public TbAcLocation findEntityByAreaAndLocationCode(String areaCode, String locationCode) {
        String sql = ""
                + "select l.* "
                + "from logis_asrs.tb_ac_location l "
                + "inner join logis_asrs.tb_ac_storage_area sa on sa.id = l.area_id "
                + "where l.domain_id = :domainId "
                + "  and sa.area_code = :areaCode "
                + "  and l.location_code = :locationCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaCode", areaCode);
        params.put("locationCode", locationCode);

        List<TbAcLocation> list = this.queryManager.selectListBySql(
                sql,
                params,
                TbAcLocation.class,
                0,
                1
        );

        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public String resolveAreaIdByCode(String areaCode) {
        String sql = ""
                + "select sa.id "
                + "from logis_asrs.tb_ac_storage_area sa "
                + "where sa.domain_id = :domainId "
                + "  and sa.area_code = :areaCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaCode", areaCode);

        return this.queryManager.selectBySql(sql, params, String.class);
    }

    public String resolveItemCategoryIdByCode(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            return null;
        }

        String sql = ""
                + "select ic.id "
                + "from logis_asrs.tb_ac_item_category ic "
                + "where ic.domain_id = :domainId "
                + "  and ic.category_code = :categoryCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("categoryCode", categoryCode.trim());

        return this.queryManager.selectBySql(sql, params, String.class);
    }

    public String resolveAccessPointIdByCode(String areaCode, String pointCode) {
        if (pointCode == null || pointCode.trim().isEmpty()) {
            return null;
        }

        String sql = ""
                + "select ap.id "
                + "from logis_asrs.tb_ac_access_point ap "
                + "inner join logis_asrs.tb_ac_storage_area sa on sa.id = ap.area_id "
                + "where ap.domain_id = :domainId "
                + "  and sa.area_code = :areaCode "
                + "  and ap.point_code = :pointCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaCode", areaCode);
        params.put("pointCode", pointCode.trim());

        return this.queryManager.selectBySql(sql, params, String.class);
    }

    public List<ItemCategoryOptionView> getItemCategoryOptions() {
        String sql = ""
                + "select "
                + "    ic.id as id, "
                + "    ic.category_code as category_code, "
                + "    ic.category_name as category_name, "
                + "    ic.active_yn as active_yn "
                + "from logis_asrs.tb_ac_item_category ic "
                + "where ic.domain_id = :domainId "
                + "  and ic.active_yn = 'Y' "
                + "order by ic.category_code asc ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        return this.queryManager.selectListBySql(sql, params, ItemCategoryOptionView.class, 0, 0);
    }

    public List<AccessPointOptionView> getAccessPointOptions(String areaCode) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    ap.id as id, ");
        sql.append("    sa.area_code as area_code, ");
        sql.append("    ap.point_code as point_code, ");
        sql.append("    ap.point_name as point_name, ");
        sql.append("    ap.active_yn as active_yn ");
        sql.append("from logis_asrs.tb_ac_access_point ap ");
        sql.append("inner join logis_asrs.tb_ac_storage_area sa on sa.id = ap.area_id ");
        sql.append("where ap.domain_id = :domainId ");
        sql.append("  and ap.active_yn = 'Y' ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        if (areaCode != null && !areaCode.trim().isEmpty()) {
            sql.append("  and sa.area_code = :areaCode ");
            params.put("areaCode", areaCode.trim());
        }

        sql.append("order by sa.area_code asc, ap.point_code asc ");

        return this.queryManager.selectListBySql(sql.toString(), params, AccessPointOptionView.class, 0, 0);
    }

    public int nextSortSeq(String areaId) {
        String sql =
                "select coalesce(max(sort_seq), 0) + 1 as next_seq "
                        + "from logis_asrs.tb_ac_location "
                        + "where domain_id = :domainId "
                        + "  and area_id = :areaId ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaId", areaId);

        Integer nextSeq = this.queryManager.selectBySql(sql, params, Integer.class);
        return nextSeq == null ? 1 : nextSeq;
    }

    /**
     * 단건 삭제 대상 location id 조회.
     */
    public List<String> findLocationIdsForSingleDelete(String areaCode, String locationCode) {
        String sql = ""
                + "select l.id "
                + "from logis_asrs.tb_ac_location l "
                + "inner join logis_asrs.tb_ac_storage_area sa on sa.id = l.area_id "
                + "where l.domain_id = :domainId "
                + "  and sa.area_code = :areaCode "
                + "  and l.location_code = :locationCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaCode", areaCode);
        params.put("locationCode", locationCode);

        return this.queryManager.selectListBySql(sql, params, String.class, 0, 0);
    }

    /**
     * 조회조건 기준 bulk delete 대상 location id 조회.
     */
    public List<String> findLocationIdsForBulkDelete(
            String areaCode,
            String locationCode,
            String locationType,
            String activeYn
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("select l.id ");
        sql.append("from logis_asrs.tb_ac_location l ");
        sql.append("inner join logis_asrs.tb_ac_storage_area sa on sa.id = l.area_id ");
        sql.append("where l.domain_id = :domainId ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        if (areaCode != null && !areaCode.trim().isEmpty()) {
            sql.append("  and sa.area_code = :areaCode ");
            params.put("areaCode", areaCode.trim());
        }

        if (locationCode != null && !locationCode.trim().isEmpty()) {
            sql.append("  and lower(l.location_code) like lower(:locationCode) ");
            params.put("locationCode", "%" + locationCode.trim() + "%");
        }

        if (locationType != null && !locationType.trim().isEmpty()) {
            sql.append("  and l.location_type = :locationType ");
            params.put("locationType", locationType.trim());
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("  and l.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        }

        sql.append("order by l.id asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                String.class,
                0,
                0
        );
    }

    /**
     * 삭제 대상 location 들의 재고 수 조회.
     */
    public int countStockUnitsByLocationIds(List<String> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return 0;
        }

        String sql = ""
                + "select count(*) "
                + "from logis_asrs.tb_ac_stock_unit su "
                + "where su.domain_id = :domainId "
                + "  and su.current_location_id in (:locationIds) ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("locationIds", locationIds);

        Integer count = this.queryManager.selectBySql(sql, params, Integer.class);
        return count == null ? 0 : count;
    }
}