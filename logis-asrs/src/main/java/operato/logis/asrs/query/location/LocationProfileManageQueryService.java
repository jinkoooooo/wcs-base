package operato.logis.asrs.query.location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import operato.logis.asrs.entity.TbAcLocationProfile;
import operato.logis.asrs.query.location.model.LocationProfileListView;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 로케이션 프로필 관리 조회 서비스.
 */
@Service
public class LocationProfileManageQueryService extends AbstractQueryService {

    /**
     * 목록 조회.
     */
    public List<LocationProfileListView> search(
            String areaCode,
            String profileCode,
            String profileName,
            String activeYn
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    lp.id as id, ");
        sql.append("    sa.id as area_id, ");
        sql.append("    sa.area_code as area_code, ");
        sql.append("    sa.area_name as area_name, ");
        sql.append("    lp.profile_code as profile_code, ");
        sql.append("    lp.profile_name as profile_name, ");
        sql.append("    lp.aisle_start as aisle_start, ");
        sql.append("    lp.aisle_end as aisle_end, ");
        sql.append("    lp.side_codes as side_codes, ");
        sql.append("    lp.bay_start as bay_start, ");
        sql.append("    lp.bay_end as bay_end, ");
        sql.append("    lp.level_start as level_start, ");
        sql.append("    lp.level_end as level_end, ");
        sql.append("    lp.depth_start as depth_start, ");
        sql.append("    lp.depth_end as depth_end, ");
        sql.append("    lp.location_type as location_type, ");
        sql.append("    lp.code_pattern as code_pattern, ");
        sql.append("    lp.mixed_load_yn as mixed_load_yn, ");
        sql.append("    lp.inbound_allowed_yn as inbound_allowed_yn, ");
        sql.append("    lp.outbound_allowed_yn as outbound_allowed_yn, ");
        sql.append("    lp.active_yn as active_yn, ");
        sql.append("    ( ");
        sql.append("        select count(*) ");
        sql.append("          from logis_asrs.tb_ac_location l ");
        sql.append("         where l.domain_id = lp.domain_id ");
        sql.append("           and l.area_id = lp.area_id ");
        sql.append("    ) as linked_location_count, ");
        sql.append("    cast(lp.created_at as varchar) as created_at, ");
        sql.append("    cast(lp.updated_at as varchar) as updated_at ");
        sql.append("from logis_asrs.tb_ac_location_profile lp ");
        sql.append("inner join logis_asrs.tb_ac_storage_area sa on sa.id = lp.area_id ");
        sql.append("where lp.domain_id = :domainId ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        if (areaCode != null && !areaCode.trim().isEmpty()) {
            sql.append("  and sa.area_code = :areaCode ");
            params.put("areaCode", areaCode.trim());
        }

        if (profileCode != null && !profileCode.trim().isEmpty()) {
            sql.append("  and lower(lp.profile_code) like lower(:profileCode) ");
            params.put("profileCode", "%" + profileCode.trim() + "%");
        }

        if (profileName != null && !profileName.trim().isEmpty()) {
            sql.append("  and lower(lp.profile_name) like lower(:profileName) ");
            params.put("profileName", "%" + profileName.trim() + "%");
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("  and lp.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        }

        sql.append("order by sa.area_code asc, lp.profile_code asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                LocationProfileListView.class,
                0,
                0
        );
    }

    /**
     * 상세 조회.
     */
    public LocationProfileListView getDetailByAreaAndProfileCode(String areaCode, String profileCode) {
        String sql = ""
                + "select "
                + "    lp.id as id, "
                + "    sa.id as area_id, "
                + "    sa.area_code as area_code, "
                + "    sa.area_name as area_name, "
                + "    lp.profile_code as profile_code, "
                + "    lp.profile_name as profile_name, "
                + "    lp.aisle_start as aisle_start, "
                + "    lp.aisle_end as aisle_end, "
                + "    lp.side_codes as side_codes, "
                + "    lp.bay_start as bay_start, "
                + "    lp.bay_end as bay_end, "
                + "    lp.level_start as level_start, "
                + "    lp.level_end as level_end, "
                + "    lp.depth_start as depth_start, "
                + "    lp.depth_end as depth_end, "
                + "    lp.location_type as location_type, "
                + "    lp.code_pattern as code_pattern, "
                + "    lp.mixed_load_yn as mixed_load_yn, "
                + "    lp.inbound_allowed_yn as inbound_allowed_yn, "
                + "    lp.outbound_allowed_yn as outbound_allowed_yn, "
                + "    lp.active_yn as active_yn, "
                + "    ( "
                + "        select count(*) "
                + "          from logis_asrs.tb_ac_location l "
                + "         where l.domain_id = lp.domain_id "
                + "           and l.area_id = lp.area_id "
                + "    ) as linked_location_count, "
                + "    cast(lp.created_at as varchar) as created_at, "
                + "    cast(lp.updated_at as varchar) as updated_at "
                + "from logis_asrs.tb_ac_location_profile lp "
                + "inner join logis_asrs.tb_ac_storage_area sa on sa.id = lp.area_id "
                + "where lp.domain_id = :domainId "
                + "  and sa.area_code = :areaCode "
                + "  and lp.profile_code = :profileCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaCode", areaCode);
        params.put("profileCode", profileCode);

        return this.queryManager.selectBySql(sql, params, LocationProfileListView.class);
    }

    /**
     * 엔티티 조회.
     */
    public TbAcLocationProfile findEntityByAreaAndProfileCode(String areaCode, String profileCode) {
        String sql = ""
                + "select lp.* "
                + "from logis_asrs.tb_ac_location_profile lp "
                + "inner join logis_asrs.tb_ac_storage_area sa on sa.id = lp.area_id "
                + "where lp.domain_id = :domainId "
                + "  and sa.area_code = :areaCode "
                + "  and lp.profile_code = :profileCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaCode", areaCode);
        params.put("profileCode", profileCode);

        List<TbAcLocationProfile> list = this.queryManager.selectListBySql(
                sql,
                params,
                TbAcLocationProfile.class,
                0,
                1
        );

        if (list == null || list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }

    /**
     * area_code -> area_id resolve
     */
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

    /**
     * 참조 로케이션 수 조회.
     */
    public long countLinkedLocations(String areaId) {
        String sql = ""
                + "select count(*) "
                + "from logis_asrs.tb_ac_location "
                + "where domain_id = :domainId "
                + "  and area_id = :areaId ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaId", areaId);

        Long count = this.queryManager.selectBySql(sql, params, Long.class);
        return count == null ? 0L : count;
    }
}