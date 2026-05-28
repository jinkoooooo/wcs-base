package operato.logis.asrs.query.location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.location.model.CenterOptionView;
import operato.logis.asrs.query.location.model.OperationProfileOptionView;
import operato.logis.asrs.query.location.model.StorageAreaListView;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 아레아 조회 서비스.
 */
@Service
public class StorageAreaQueryService extends AbstractQueryService {

    /**
     * 아레아 목록 조회.
     */
    public List<StorageAreaListView> search(
            String centerCode,
            String areaCode,
            String areaName,
            String activeYn
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    sa.id as id, ");
        sql.append("    c.id as center_id, ");
        sql.append("    c.center_code as center_code, ");
        sql.append("    c.center_name as center_name, ");
        sql.append("    sa.area_code as area_code, ");
        sql.append("    sa.area_name as area_name, ");
        sql.append("    sa.area_type as area_type, ");
        sql.append("    op.id as operation_profile_id, ");
        sql.append("    op.profile_code as operation_profile_code, ");
        sql.append("    op.profile_name as operation_profile_name, ");
        sql.append("    sa.description as description, ");
        sql.append("    sa.active_yn as active_yn, ");
        sql.append("    ( ");
        sql.append("        select count(*) ");
        sql.append("          from logis_asrs.tb_ac_location_profile lp ");
        sql.append("         where lp.domain_id = sa.domain_id ");
        sql.append("           and lp.area_id = sa.id ");
        sql.append("    ) as linked_location_profile_count, ");
        sql.append("    ( ");
        sql.append("        select count(*) ");
        sql.append("          from logis_asrs.tb_ac_location loc ");
        sql.append("         where loc.domain_id = sa.domain_id ");
        sql.append("           and loc.area_id = sa.id ");
        sql.append("    ) as linked_location_count, ");
        sql.append("    cast(sa.created_at as varchar) as created_at, ");
        sql.append("    cast(sa.updated_at as varchar) as updated_at ");
        sql.append("from logis_asrs.tb_ac_storage_area sa ");
        sql.append("inner join logis_asrs.tb_ac_center c on c.id = sa.center_id ");
        sql.append("inner join logis_asrs.tb_ac_operation_profile op on op.id = sa.operation_profile_id ");
        sql.append("where sa.domain_id = :domainId ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        if (centerCode != null && !centerCode.trim().isEmpty()) {
            sql.append("  and c.center_code = :centerCode ");
            params.put("centerCode", centerCode.trim());
        }

        if (areaCode != null && !areaCode.trim().isEmpty()) {
            sql.append("  and lower(sa.area_code) like lower(:areaCode) ");
            params.put("areaCode", "%" + areaCode.trim() + "%");
        }

        if (areaName != null && !areaName.trim().isEmpty()) {
            sql.append("  and lower(sa.area_name) like lower(:areaName) ");
            params.put("areaName", "%" + areaName.trim() + "%");
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("  and sa.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        }

        sql.append("order by c.center_code asc, sa.area_code asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                StorageAreaListView.class,
                0,
                0
        );
    }

    /**
     * 상세 조회.
     */
    public StorageAreaListView getDetailByCenterAndAreaCode(String centerCode, String areaCode) {
        String sql = ""
                + "select "
                + "    sa.id as id, "
                + "    c.id as center_id, "
                + "    c.center_code as center_code, "
                + "    c.center_name as center_name, "
                + "    sa.area_code as area_code, "
                + "    sa.area_name as area_name, "
                + "    sa.area_type as area_type, "
                + "    op.id as operation_profile_id, "
                + "    op.profile_code as operation_profile_code, "
                + "    op.profile_name as operation_profile_name, "
                + "    sa.description as description, "
                + "    sa.active_yn as active_yn, "
                + "    ( "
                + "        select count(*) "
                + "          from logis_asrs.tb_ac_location_profile lp "
                + "         where lp.domain_id = sa.domain_id "
                + "           and lp.area_id = sa.id "
                + "    ) as linked_location_profile_count, "
                + "    ( "
                + "        select count(*) "
                + "          from logis_asrs.tb_ac_location loc "
                + "         where loc.domain_id = sa.domain_id "
                + "           and loc.area_id = sa.id "
                + "    ) as linked_location_count, "
                + "    cast(sa.created_at as varchar) as created_at, "
                + "    cast(sa.updated_at as varchar) as updated_at "
                + "from logis_asrs.tb_ac_storage_area sa "
                + "inner join logis_asrs.tb_ac_center c on c.id = sa.center_id "
                + "inner join logis_asrs.tb_ac_operation_profile op on op.id = sa.operation_profile_id "
                + "where sa.domain_id = :domainId "
                + "  and c.center_code = :centerCode "
                + "  and sa.area_code = :areaCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("centerCode", centerCode);
        params.put("areaCode", areaCode);

        return this.queryManager.selectBySql(sql, params, StorageAreaListView.class);
    }

    /**
     * 엔티티 조회.
     */
    public TbAcStorageArea findEntityByCenterAndAreaCode(String centerCode, String areaCode) {
        String sql = ""
                + "select sa.* "
                + "from logis_asrs.tb_ac_storage_area sa "
                + "inner join logis_asrs.tb_ac_center c on c.id = sa.center_id "
                + "where sa.domain_id = :domainId "
                + "  and c.center_code = :centerCode "
                + "  and sa.area_code = :areaCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("centerCode", centerCode);
        params.put("areaCode", areaCode);

        List<TbAcStorageArea> list = this.queryManager.selectListBySql(
                sql,
                params,
                TbAcStorageArea.class,
                0,
                1
        );

        if (list == null || list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }

    /**
     * 활성 센터 옵션 조회.
     */
    public List<CenterOptionView> getCenterOptions() {
        String sql = ""
                + "select "
                + "    c.id as id, "
                + "    c.center_code as center_code, "
                + "    c.center_name as center_name, "
                + "    c.active_yn as active_yn "
                + "from logis_asrs.tb_ac_center c "
                + "where c.domain_id = :domainId "
                + "  and c.active_yn = 'Y' "
                + "order by c.center_code asc ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        return this.queryManager.selectListBySql(sql, params, CenterOptionView.class, 0, 0);
    }

    /**
     * 활성 오퍼레이션 프로필 옵션 조회.
     */
    public List<OperationProfileOptionView> getOperationProfileOptions() {
        String sql = ""
                + "select "
                + "    op.id as id, "
                + "    op.profile_code as profile_code, "
                + "    op.profile_name as profile_name, "
                + "    op.active_yn as active_yn "
                + "from logis_asrs.tb_ac_operation_profile op "
                + "where op.domain_id = :domainId "
                + "  and op.active_yn = 'Y' "
                + "order by op.profile_code asc ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        return this.queryManager.selectListBySql(sql, params, OperationProfileOptionView.class, 0, 0);
    }

    /**
     * 연계 수 조회.
     */
    public long countLinkedLocationProfiles(String areaId) {
        String sql = ""
                + "select count(*) "
                + "from logis_asrs.tb_ac_location_profile "
                + "where domain_id = :domainId "
                + "  and area_id = :areaId ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("areaId", areaId);

        Long count = this.queryManager.selectBySql(sql, params, Long.class);
        return count == null ? 0L : count;
    }

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

    /**
     * center_code -> id resolve
     */
    public String resolveCenterIdByCode(String centerCode) {
        String sql = ""
                + "select c.id "
                + "from logis_asrs.tb_ac_center c "
                + "where c.domain_id = :domainId "
                + "  and c.center_code = :centerCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("centerCode", centerCode);

        return this.queryManager.selectBySql(sql, params, String.class);
    }

    /**
     * profile_code -> id resolve
     */
    public String resolveOperationProfileIdByCode(String profileCode) {
        String sql = ""
                + "select op.id "
                + "from logis_asrs.tb_ac_operation_profile op "
                + "where op.domain_id = :domainId "
                + "  and op.profile_code = :profileCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("profileCode", profileCode);

        return this.queryManager.selectBySql(sql, params, String.class);
    }
}