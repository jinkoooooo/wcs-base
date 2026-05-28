package operato.logis.asrs.query.location;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.location.model.AccessPointView;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * Location Access Point 조회 서비스.
 *
 * <p>
 * areaCode + purposeCode 기준으로 사용 가능한 Access Point 목록을 조회한다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LocationAccessPointQueryService extends AbstractQueryService {

    private final LocationQueryService locationQueryService;

    /**
     * 영역 + 목적 기준 access point 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param purposeCode 목적 코드
     * @return Access Point 목록
     */
    public List<AccessPointView> findAccessPoints(String areaCode, String purposeCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(purposeCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "purposeCode is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);

        String sql =
                "select " +
                        "    ap.id as access_point_id, " +
                        "    ap.area_id, " +
                        "    ap.point_code, " +
                        "    ap.point_name, " +
                        "    ap.point_type, " +
                        "    ap.aisle_no, " +
                        "    ap.side_code, " +
                        "    ap.bay_no, " +
                        "    ap.level_no, " +
                        "    ap.depth_no, " +
                        "    app.purpose_code, " +
                        "    app.priority_no " +
                        "  from logis_asrs.tb_ac_access_point ap " +
                        "  join logis_asrs.tb_ac_access_point_purpose app " +
                        "    on ap.id = app.access_point_id " +
                        " where ap.domain_id = :domainId " +
                        "   and app.domain_id = :domainId " +
                        "   and ap.area_id = :areaId " +
                        "   and ap.active_yn = 'Y' " +
                        "   and ap.use_for_sort_yn = 'Y' " +
                        "   and app.active_yn = 'Y' " +
                        "   and app.purpose_code = :purposeCode " +
                        " order by app.priority_no asc, ap.point_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,purposeCode",
                Domain.currentDomainId(),
                area.getId(),
                purposeCode
        );

        return this.queryManager.selectListBySql(sql, param, AccessPointView.class, 0, 0);
    }
}