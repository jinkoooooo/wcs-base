package operato.logis.asrs.query.location;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.dto.request.AccessPointSearchRequest;
import operato.logis.asrs.query.location.model.AccessPointManageRow;
import operato.logis.asrs.query.location.model.AccessPointPurposeRow;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * Access Point 관리 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class AccessPointManageQueryService extends AbstractQueryService {

    /**
     * Access Point 목록 조회.
     *
     * @param request 조회 조건
     * @return Access Point 목록
     */
    public List<AccessPointManageRow> search(AccessPointSearchRequest request) {
        String sql =
                "select " +
                        "    ap.id, " +
                        "    ap.area_id, " +
                        "    a.area_code, " +
                        "    a.area_name, " +
                        "    ap.point_code, " +
                        "    ap.point_name, " +
                        "    ap.point_type, " +
                        "    ap.aisle_no, " +
                        "    ap.side_code, " +
                        "    ap.bay_no, " +
                        "    ap.level_no, " +
                        "    ap.depth_no, " +
                        "    ap.use_for_sort_yn, " +
                        "    ap.active_yn, " +
                        "    ap.description, " +
                        "    coalesce( " +
                        "        string_agg( " +
                        "            case when app.active_yn = 'Y' then app.purpose_code else null end, " +
                        "            ',' order by app.priority_no asc, app.purpose_code asc " +
                        "        ), " +
                        "        '' " +
                        "    ) as purpose_codes, " +
                        "    to_char(ap.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at, " +
                        "    to_char(ap.updated_at, 'YYYY-MM-DD HH24:MI:SS') as updated_at " +
                        "  from logis_asrs.tb_ac_access_point ap " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on a.id = ap.area_id " +
                        "  left join logis_asrs.tb_ac_access_point_purpose app " +
                        "    on app.access_point_id = ap.id " +
                        "   and app.domain_id = :domainId " +
                        " where ap.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and (:areaCode is null or :areaCode = '' or a.area_code = :areaCode) " +
                        "   and (:pointCode is null or :pointCode = '' or upper(ap.point_code) like upper(concat('%', :pointCode, '%'))) " +
                        "   and (:pointName is null or :pointName = '' or upper(ap.point_name) like upper(concat('%', :pointName, '%'))) " +
                        "   and (:pointType is null or :pointType = '' or ap.point_type = :pointType) " +
                        "   and (:useForSortYn is null or :useForSortYn = '' or ap.use_for_sort_yn = :useForSortYn) " +
                        "   and (:activeYn is null or :activeYn = '' or ap.active_yn = :activeYn) " +
                        "   and ( " +
                        "       :purposeCode is null " +
                        "       or :purposeCode = '' " +
                        "       or exists ( " +
                        "           select 1 " +
                        "             from logis_asrs.tb_ac_access_point_purpose app2 " +
                        "            where app2.domain_id = :domainId " +
                        "              and app2.access_point_id = ap.id " +
                        "              and app2.purpose_code = :purposeCode " +
                        "              and app2.active_yn = 'Y' " +
                        "       ) " +
                        "   ) " +
                        " group by " +
                        "    ap.id, ap.area_id, a.area_code, a.area_name, " +
                        "    ap.point_code, ap.point_name, ap.point_type, " +
                        "    ap.aisle_no, ap.side_code, ap.bay_no, ap.level_no, ap.depth_no, " +
                        "    ap.use_for_sort_yn, ap.active_yn, ap.description, ap.created_at, ap.updated_at " +
                        " order by a.area_code asc, ap.point_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaCode,pointCode,pointName,pointType,purposeCode,useForSortYn,activeYn",
                Domain.currentDomainId(),
                request == null ? "" : request.getAreaCode(),
                request == null ? "" : request.getPointCode(),
                request == null ? "" : request.getPointName(),
                request == null ? "" : request.getPointType(),
                request == null ? "" : request.getPurposeCode(),
                request == null ? "" : request.getUseForSortYn(),
                request == null ? "Y" : request.getActiveYn()
        );

        return this.queryManager.selectListBySql(sql, param, AccessPointManageRow.class, 0, 0);
    }

    /**
     * areaCode + pointCode 기준 상세 조회.
     *
     * @param areaCode 영역 코드
     * @param pointCode 포인트 코드
     * @return Access Point, 없으면 null
     */
    public AccessPointManageRow findByCode(String areaCode, String pointCode) {
        AccessPointSearchRequest request = new AccessPointSearchRequest();
        request.setAreaCode(areaCode);
        request.setPointCode(pointCode);
        request.setActiveYn("");

        List<AccessPointManageRow> list = search(request);

        if (list == null || list.isEmpty()) {
            return null;
        }

        for (AccessPointManageRow row : list) {
            if (pointCode != null && pointCode.equals(row.getPointCode())) {
                return row;
            }
        }

        return list.get(0);
    }

    /**
     * areaId + pointCode 기준 조회.
     *
     * @param areaId 영역 ID
     * @param pointCode 포인트 코드
     * @return Access Point, 없으면 null
     */
    public AccessPointManageRow findByAreaIdAndPointCode(String areaId, String pointCode) {
        String sql =
                "select " +
                        "    ap.id, " +
                        "    ap.area_id, " +
                        "    a.area_code, " +
                        "    a.area_name, " +
                        "    ap.point_code, " +
                        "    ap.point_name, " +
                        "    ap.point_type, " +
                        "    ap.aisle_no, " +
                        "    ap.side_code, " +
                        "    ap.bay_no, " +
                        "    ap.level_no, " +
                        "    ap.depth_no, " +
                        "    ap.use_for_sort_yn, " +
                        "    ap.active_yn, " +
                        "    ap.description " +
                        "  from logis_asrs.tb_ac_access_point ap " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on a.id = ap.area_id " +
                        " where ap.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and ap.area_id = :areaId " +
                        "   and ap.point_code = :pointCode ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,pointCode",
                Domain.currentDomainId(),
                areaId,
                pointCode
        );

        List<AccessPointManageRow> list = this.queryManager.selectListBySql(
                sql,
                param,
                AccessPointManageRow.class,
                0,
                0
        );

        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /**
     * Access Point 목적 목록 조회.
     *
     * @param accessPointId Access Point ID
     * @return 목적 목록
     */
    public List<AccessPointPurposeRow> findPurposes(String accessPointId) {
        String sql =
                "select " +
                        "    app.id, " +
                        "    app.access_point_id, " +
                        "    app.purpose_code, " +
                        "    app.priority_no, " +
                        "    app.active_yn, " +
                        "    app.description " +
                        "  from logis_asrs.tb_ac_access_point_purpose app " +
                        " where app.domain_id = :domainId " +
                        "   and app.access_point_id = :accessPointId " +
                        " order by app.priority_no asc, app.purpose_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,accessPointId",
                Domain.currentDomainId(),
                accessPointId
        );

        return this.queryManager.selectListBySql(sql, param, AccessPointPurposeRow.class, 0, 0);
    }
}