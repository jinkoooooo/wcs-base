package operato.logis.asrs.core.location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.request.LocationAccessPreviewRequest;
import operato.logis.asrs.dto.request.LocationAccessRecalculateRequest;
import operato.logis.asrs.dto.response.LocationAccessPreviewResult;
import operato.logis.asrs.dto.response.LocationAccessPreviewRow;
import operato.logis.asrs.dto.response.LocationAccessRecalculateResult;
import operato.logis.asrs.entity.TbAcLocation;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.location.LocationAccessPointQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import operato.logis.asrs.query.location.model.AccessPointView;
import operato.logis.asrs.query.location.model.LocationAccessCalcRow;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 로케이션 접근성 재산출 코어.
 *
 * <p>
 * 목적:
 * - front_priority_yn 재계산
 * - access_score 계산
 * - primary_access_point_id 계산
 * - sort_seq 재부여
 * - location_grade 재부여
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LocationAccessRecalculateCore extends AbstractQueryService {

    private final LocationQueryService locationQueryService;
    private final LocationAccessPointQueryService locationAccessPointQueryService;

    /**
     * 로케이션 접근성 미리보기.
     *
     * @param request 미리보기 요청
     * @return 미리보기 결과
     */
    @Transactional(readOnly = true)
    public LocationAccessPreviewResult preview(LocationAccessPreviewRequest request) {
        validateRequest(request.getAreaCode(), request.getPurposeCode());

        List<LocationAccessCalcRow> rows = calculateInternal(
                request.getAreaCode(),
                request.getPurposeCode(),
                request.getGradeARatio(),
                request.getGradeBRatio(),
                request.getGradeCRatio()
        );

        int limit = request.getLimit() == null || request.getLimit() <= 0 ? 100 : request.getLimit();
        List<LocationAccessPreviewRow> previewRows = new ArrayList<LocationAccessPreviewRow>();

        for (int i = 0; i < rows.size() && i < limit; i++) {
            LocationAccessCalcRow row = rows.get(i);
            LocationAccessPreviewRow preview = new LocationAccessPreviewRow();

            preview.setLocationId(row.getLocationId());
            preview.setLocationCode(row.getLocationCode());
            preview.setAisleNo(row.getAisleNo());
            preview.setSideCode(row.getSideCode());
            preview.setBayNo(row.getBayNo());
            preview.setLevelNo(row.getLevelNo());
            preview.setDepthNo(row.getDepthNo());
            preview.setFrontPriorityYn(row.getFrontPriorityYn());
            preview.setAccessScore(row.getAccessScore());
            preview.setNewSortSeq(row.getNewSortSeq());
            preview.setNewLocationGrade(row.getNewLocationGrade());
            preview.setPrimaryAccessPointId(row.getPrimaryAccessPointId());
            preview.setPrimaryAccessPointCode(row.getPrimaryAccessPointCode());

            previewRows.add(preview);
        }

        LocationAccessPreviewResult result = new LocationAccessPreviewResult();
        result.setAreaCode(request.getAreaCode());
        result.setPurposeCode(request.getPurposeCode());
        result.setTotalCount(rows.size());
        result.setPreviewCount(previewRows.size());
        result.setRows(previewRows);

        return result;
    }

    /**
     * 로케이션 접근성 재산출 실행.
     *
     * @param request 실행 요청
     * @return 실행 결과
     */
    @Transactional
    public LocationAccessRecalculateResult execute(LocationAccessRecalculateRequest request) {
        validateRequest(request.getAreaCode(), request.getPurposeCode());

        List<LocationAccessCalcRow> rows = calculateInternal(
                request.getAreaCode(),
                request.getPurposeCode(),
                request.getGradeARatio(),
                request.getGradeBRatio(),
                request.getGradeCRatio()
        );

        int updatedCount = 0;
        int aCount = 0;
        int bCount = 0;
        int cCount = 0;
        int dCount = 0;

        for (LocationAccessCalcRow calc : rows) {
            TbAcLocation location = this.queryManager.select(TbAcLocation.class, calc.getLocationId());
            if (location == null) {
                continue;
            }

            location.setFrontPriorityYn(calc.getFrontPriorityYn());
            location.setAccessScore(calc.getAccessScore());
            location.setSortSeq(calc.getNewSortSeq());
            location.setLocationGrade(calc.getNewLocationGrade());
            location.setPrimaryAccessPointId(calc.getPrimaryAccessPointId());

            this.queryManager.update(
                    location,
                    "frontPriorityYn",
                    "accessScore",
                    "sortSeq",
                    "locationGrade",
                    "primaryAccessPointId"
            );
            updatedCount++;

            if ("A".equals(calc.getNewLocationGrade())) {
                aCount++;
            } else if ("B".equals(calc.getNewLocationGrade())) {
                bCount++;
            } else if ("C".equals(calc.getNewLocationGrade())) {
                cCount++;
            } else {
                dCount++;
            }
        }

        LocationAccessRecalculateResult result = new LocationAccessRecalculateResult();
        result.setAreaCode(request.getAreaCode());
        result.setPurposeCode(request.getPurposeCode());
        result.setTargetLocationCount(rows.size());
        result.setUpdatedCount(updatedCount);
        result.setGradeACount(aCount);
        result.setGradeBCount(bCount);
        result.setGradeCCount(cCount);
        result.setGradeDCount(dCount);
        result.setMessage("Location access recalculation completed.");

        return result;
    }

    /**
     * 공통 계산 로직.
     *
     * @param areaCode 영역 코드
     * @param purposeCode 접근 목적 코드
     * @param gradeARatio A 비율
     * @param gradeBRatio B 누적 비율
     * @param gradeCRatio C 누적 비율
     * @return 계산 결과 목록
     */
    private List<LocationAccessCalcRow> calculateInternal(String areaCode,
                                                          String purposeCode,
                                                          Double gradeARatio,
                                                          Double gradeBRatio,
                                                          Double gradeCRatio) {
        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        List<AccessPointView> accessPoints = locationAccessPointQueryService.findAccessPoints(areaCode, purposeCode);

        if (accessPoints == null || accessPoints.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Access point not found for purposeCode=" + purposeCode
            );
        }

        List<LocationAccessCalcRow> locations = findActiveLocations(area.getId());
        if (locations == null || locations.isEmpty()) {
            return locations;
        }

        // 1) front_priority_yn 재계산
        calculateFrontPriority(locations);

        // 2) access_score / 대표 access point 계산
        for (LocationAccessCalcRow location : locations) {
            int bestScore = Integer.MAX_VALUE;
            String bestAccessPointId = null;
            String bestAccessPointCode = null;

            for (AccessPointView point : accessPoints) {
                int score =
                        Math.abs(safeInt(location.getAisleNo()) - safeInt(point.getAisleNo())) * 1000
                                + Math.abs(safeInt(location.getLevelNo()) - safeInt(point.getLevelNo())) * 100
                                + Math.abs(safeInt(location.getBayNo()) - safeInt(point.getBayNo())) * 10
                                + (equalsNullable(location.getSideCode(), point.getSideCode()) ? 0 : 30)
                                + ("Y".equals(location.getFrontPriorityYn()) ? 0 : 50);

                if (score < bestScore) {
                    bestScore = score;
                    bestAccessPointId = point.getAccessPointId();
                    bestAccessPointCode = point.getPointCode();
                }
            }

            location.setAccessScore(Integer.valueOf(bestScore));
            location.setPrimaryAccessPointId(bestAccessPointId);
            location.setPrimaryAccessPointCode(bestAccessPointCode);
        }

        // 3) sort_seq 부여
        locations.sort(
                Comparator.comparing(LocationAccessCalcRow::getAccessScore, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LocationAccessCalcRow::getLevelNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LocationAccessCalcRow::getAisleNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LocationAccessCalcRow::getBayNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LocationAccessCalcRow::getDepthNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LocationAccessCalcRow::getLocationCode, Comparator.nullsLast(String::compareTo))
        );

        for (int i = 0; i < locations.size(); i++) {
            locations.get(i).setNewSortSeq(Integer.valueOf(i + 1));
        }

        // 4) location_grade 부여
        applyLocationGrade(locations, gradeARatio, gradeBRatio, gradeCRatio);

        return locations;
    }

    /**
     * 활성 로케이션 조회.
     *
     * @param areaId 영역 id
     * @return 로케이션 목록
     */
    private List<LocationAccessCalcRow> findActiveLocations(String areaId) {
        String sql =
                "select " +
                        "    l.id as location_id, " +
                        "    l.location_code, " +
                        "    l.area_id, " +
                        "    l.aisle_no, " +
                        "    l.side_code, " +
                        "    l.bay_no, " +
                        "    l.level_no, " +
                        "    l.depth_no, " +
                        "    l.active_yn " +
                        "  from logis_asrs.tb_ac_location l " +
                        " where l.domain_id = :domainId " +
                        "   and l.area_id = :areaId " +
                        "   and l.active_yn = 'Y' ";

        java.util.Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId",
                xyz.elidom.sys.entity.Domain.currentDomainId(),
                areaId
        );

        return this.queryManager.selectListBySql(sql, param, LocationAccessCalcRow.class, 0, 0);
    }

    /**
     * Deep 기준 front_priority_yn 계산.
     *
     * <p>
     * 동일 line(area+aisle+side+bay+level) 내 최소 depth_no만 Y.
     * </p>
     */
    private void calculateFrontPriority(List<LocationAccessCalcRow> locations) {
        for (LocationAccessCalcRow location : locations) {
            int minDepth = Integer.MAX_VALUE;

            for (LocationAccessCalcRow compare : locations) {
                if (equalsNullable(location.getAreaId(), compare.getAreaId())
                        && safeInt(location.getAisleNo()) == safeInt(compare.getAisleNo())
                        && equalsNullable(location.getSideCode(), compare.getSideCode())
                        && safeInt(location.getBayNo()) == safeInt(compare.getBayNo())
                        && safeInt(location.getLevelNo()) == safeInt(compare.getLevelNo())) {

                    if (safeInt(compare.getDepthNo()) < minDepth) {
                        minDepth = safeInt(compare.getDepthNo());
                    }
                }
            }

            if (safeInt(location.getDepthNo()) == minDepth) {
                location.setFrontPriorityYn("Y");
            } else {
                location.setFrontPriorityYn("N");
            }
        }
    }

    /**
     * location_grade 적용.
     *
     * <p>
     * 기본값:
     * A = 15%
     * B = 50% 누적
     * C = 80% 누적
     * 나머지 D
     * </p>
     */
    private void applyLocationGrade(List<LocationAccessCalcRow> locations,
                                    Double gradeARatio,
                                    Double gradeBRatio,
                                    Double gradeCRatio) {

        double aRatio = gradeARatio == null ? 0.15d : gradeARatio.doubleValue();
        double bRatio = gradeBRatio == null ? 0.50d : gradeBRatio.doubleValue();
        double cRatio = gradeCRatio == null ? 0.80d : gradeCRatio.doubleValue();

        int totalCount = locations.size();
        int aLimit = (int) Math.ceil(totalCount * aRatio);
        int bLimit = (int) Math.ceil(totalCount * bRatio);
        int cLimit = (int) Math.ceil(totalCount * cRatio);

        for (LocationAccessCalcRow row : locations) {
            int seq = safeInt(row.getNewSortSeq());

            if (seq <= aLimit) {
                row.setNewLocationGrade("A");
            } else if (seq <= bLimit) {
                row.setNewLocationGrade("B");
            } else if (seq <= cLimit) {
                row.setNewLocationGrade("C");
            } else {
                row.setNewLocationGrade("D");
            }
        }
    }

    /**
     * 공통 요청 검증.
     *
     * @param areaCode 영역 코드
     * @param purposeCode 목적 코드
     */
    private void validateRequest(String areaCode, String purposeCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(purposeCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "purposeCode is empty.");
        }
    }

    /**
     * null-safe integer.
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    /**
     * null-safe equals.
     */
    private boolean equalsNullable(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}