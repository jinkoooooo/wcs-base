package operato.logis.asrs.query.location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import operato.logis.asrs.entity.TbAcCenter;
import operato.logis.asrs.query.location.model.CenterListView;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 센터 조회 서비스.
 */
@Service
public class CenterQueryService extends AbstractQueryService {

    /**
     * 센터 목록 조회.
     */
    public List<CenterListView> search(String centerCode, String centerName, String activeYn) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    c.id as id, ");
        sql.append("    c.center_code as center_code, ");
        sql.append("    c.center_name as center_name, ");
        sql.append("    c.center_type as center_type, ");
        sql.append("    c.timezone as timezone, ");
        sql.append("    c.description as description, ");
        sql.append("    c.active_yn as active_yn, ");
        sql.append("    ( ");
        sql.append("        select count(*) ");
        sql.append("          from logis_asrs.tb_ac_storage_area sa ");
        sql.append("         where sa.domain_id = c.domain_id ");
        sql.append("           and sa.center_id = c.id ");
        sql.append("    ) as linked_area_count, ");
        sql.append("    cast(c.created_at as varchar) as created_at, ");
        sql.append("    cast(c.updated_at as varchar) as updated_at ");
        sql.append("from logis_asrs.tb_ac_center c ");
        sql.append("where c.domain_id = :domainId ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        if (centerCode != null && !centerCode.trim().isEmpty()) {
            sql.append("  and lower(c.center_code) like lower(:centerCode) ");
            params.put("centerCode", "%" + centerCode.trim() + "%");
        }

        if (centerName != null && !centerName.trim().isEmpty()) {
            sql.append("  and lower(c.center_name) like lower(:centerName) ");
            params.put("centerName", "%" + centerName.trim() + "%");
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("  and c.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        }

        sql.append("order by c.center_code asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                CenterListView.class,
                0,
                0
        );
    }

    /**
     * 센터 상세 조회.
     */
    public CenterListView getDetailByCode(String centerCode) {
        String sql = ""
                + "select "
                + "    c.id as id, "
                + "    c.center_code as center_code, "
                + "    c.center_name as center_name, "
                + "    c.center_type as center_type, "
                + "    c.timezone as timezone, "
                + "    c.description as description, "
                + "    c.active_yn as active_yn, "
                + "    ( "
                + "        select count(*) "
                + "          from logis_asrs.tb_ac_storage_area sa "
                + "         where sa.domain_id = c.domain_id "
                + "           and sa.center_id = c.id "
                + "    ) as linked_area_count, "
                + "    cast(c.created_at as varchar) as created_at, "
                + "    cast(c.updated_at as varchar) as updated_at "
                + "from logis_asrs.tb_ac_center c "
                + "where c.domain_id = :domainId "
                + "  and c.center_code = :centerCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("centerCode", centerCode);

        return this.queryManager.selectBySql(sql, params, CenterListView.class);
    }

    /**
     * 엔티티 조회.
     */
    public TbAcCenter findEntityByCode(String centerCode) {
        String sql = ""
                + "select * "
                + "from logis_asrs.tb_ac_center "
                + "where domain_id = :domainId "
                + "  and center_code = :centerCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("centerCode", centerCode);

        List<TbAcCenter> list = this.queryManager.selectListBySql(
                sql,
                params,
                TbAcCenter.class,
                0,
                1
        );

        if (list == null || list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }

    /**
     * 참조 중인 아레아 수 조회.
     */
    public long countLinkedAreas(String centerId) {
        String sql = ""
                + "select count(*) "
                + "from logis_asrs.tb_ac_storage_area "
                + "where domain_id = :domainId "
                + "  and center_id = :centerId ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("centerId", centerId);

        Long count = this.queryManager.selectBySql(sql, params, Long.class);
        return count == null ? 0L : count;
    }
}