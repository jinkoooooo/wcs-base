package operato.logis.asrs.query.location;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import operato.logis.asrs.entity.TbAcOperationProfile;
import operato.logis.asrs.query.location.model.OperationProfileListView;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * 오퍼레이션 프로필 조회 서비스.
 */
@Service
public class OperationProfileQueryService extends AbstractQueryService {

    /**
     * 목록 조회.
     */
    public List<OperationProfileListView> search(String profileCode, String profileName, String activeYn) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    op.id as id, ");
        sql.append("    op.profile_code as profile_code, ");
        sql.append("    op.profile_name as profile_name, ");
        sql.append("    op.industry_type as industry_type, ");
        sql.append("    op.description as description, ");
        sql.append("    op.active_yn as active_yn, ");
        sql.append("    ( ");
        sql.append("        select count(*) ");
        sql.append("          from logis_asrs.tb_ac_storage_area sa ");
        sql.append("         where sa.domain_id = op.domain_id ");
        sql.append("           and sa.operation_profile_id = op.id ");
        sql.append("    ) as linked_area_count, ");
        sql.append("    cast(op.created_at as varchar) as created_at, ");
        sql.append("    cast(op.updated_at as varchar) as updated_at ");
        sql.append("from logis_asrs.tb_ac_operation_profile op ");
        sql.append("where op.domain_id = :domainId ");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());

        if (profileCode != null && !profileCode.trim().isEmpty()) {
            sql.append("  and lower(op.profile_code) like lower(:profileCode) ");
            params.put("profileCode", "%" + profileCode.trim() + "%");
        }

        if (profileName != null && !profileName.trim().isEmpty()) {
            sql.append("  and lower(op.profile_name) like lower(:profileName) ");
            params.put("profileName", "%" + profileName.trim() + "%");
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("  and op.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        }

        sql.append("order by op.profile_code asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                OperationProfileListView.class,
                0,
                0
        );
    }

    /**
     * 코드 기준 단건 상세 조회.
     */
    public OperationProfileListView getDetailByCode(String profileCode) {
        String sql = ""
                + "select "
                + "    op.id as id, "
                + "    op.profile_code as profile_code, "
                + "    op.profile_name as profile_name, "
                + "    op.industry_type as industry_type, "
                + "    op.description as description, "
                + "    op.active_yn as active_yn, "
                + "    ( "
                + "        select count(*) "
                + "          from logis_asrs.tb_ac_storage_area sa "
                + "         where sa.domain_id = op.domain_id "
                + "           and sa.operation_profile_id = op.id "
                + "    ) as linked_area_count, "
                + "    cast(op.created_at as varchar) as created_at, "
                + "    cast(op.updated_at as varchar) as updated_at "
                + "from logis_asrs.tb_ac_operation_profile op "
                + "where op.domain_id = :domainId "
                + "  and op.profile_code = :profileCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("profileCode", profileCode);

        return this.queryManager.selectBySql(sql, params, OperationProfileListView.class);
    }

    /**
     * 엔티티 조회.
     */
    public TbAcOperationProfile findEntityByCode(String profileCode) {
        String sql = ""
                + "select * "
                + "from logis_asrs.tb_ac_operation_profile "
                + "where domain_id = :domainId "
                + "  and profile_code = :profileCode ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("profileCode", profileCode);

        List<TbAcOperationProfile> list = this.queryManager.selectListBySql(
                sql,
                params,
                TbAcOperationProfile.class,
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
    public long countLinkedAreas(String operationProfileId) {
        String sql = ""
                + "select count(*) "
                + "from logis_asrs.tb_ac_storage_area "
                + "where domain_id = :domainId "
                + "  and operation_profile_id = :operationProfileId ";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("domainId", Domain.currentDomainId());
        params.put("operationProfileId", operationProfileId);

        Long count = this.queryManager.selectBySql(sql, params, Long.class);
        return count == null ? 0L : count;
    }
}