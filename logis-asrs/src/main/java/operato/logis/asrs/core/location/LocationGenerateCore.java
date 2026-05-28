package operato.logis.asrs.core.location;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.asrs.core.common.AcLocationCodeBuilder;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.dto.response.LocationGeneratePreviewResult;
import operato.logis.asrs.dto.response.LocationGenerateResult;
import operato.logis.asrs.entity.TbAcLocation;
import operato.logis.asrs.entity.TbAcLocationProfile;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.enums.AcLocationSide;
import operato.logis.asrs.enums.AcYn;
import operato.logis.asrs.query.location.LocationQueryService;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 로케이션 자동생성 코어.
 *
 * <p>
 * 신규 표준 진입점은 areaCode + profileCode 기반이다.
 * 내부 저장/FK 처리 시에만 id 를 사용한다.
 * </p>
 *
 * <p>
 * 성능 개선 사항:
 * 기존에는 생성 대상 로케이션마다 queryManager.insert()를 호출했으나,
 * 대량 생성 시 DB 왕복이 많아져 성능 저하가 발생한다.
 * 따라서 생성 대상 로케이션을 버퍼에 모은 뒤 일정 건수 단위로 batch insert 처리한다.
 * </p>
 *
 * <p>
 * 주의:
 * queryManager.insert()를 우회하므로 id, domain_id, creator_id, updater_id,
 * created_at, updated_at 공통 컬럼은 batch insert 시 직접 세팅한다.
 * </p>
 */
@Service
public class LocationGenerateCore extends AbstractQueryService {

    private static final int MAX_PREVIEW_SAMPLE_COUNT = 100;
    private static final int MAX_RESULT_SAMPLE_COUNT = 200;

    /**
     * 로케이션 batch insert 단위.
     *
     * <p>
     * 너무 작으면 DB 왕복이 많아지고, 너무 크면 SQL/메모리 부담이 커질 수 있다.
     * 보통 300~1000 사이에서 조정하면 된다.
     * </p>
     */
    private static final int LOCATION_INSERT_BATCH_SIZE = 500;

    private final LocationQueryService locationQueryService;
    private final AcLocationCodeBuilder locationCodeBuilder;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public LocationGenerateCore(LocationQueryService locationQueryService,
                                AcLocationCodeBuilder locationCodeBuilder,
                                NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.locationQueryService = locationQueryService;
        this.locationCodeBuilder = locationCodeBuilder;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    /**
     * 로케이션 프로파일 기준 생성 Preview.
     *
     * @param areaCode 영역 코드
     * @param profileCode 로케이션 프로파일 코드
     * @return preview 결과
     */
    @Transactional(readOnly = true)
    public LocationGeneratePreviewResult previewByProfileCode(String areaCode, String profileCode) {
        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        TbAcLocationProfile profile = locationQueryService.findLocationProfileByCode(areaCode, profileCode);

        validateGenerationTarget(profile, area);

        Set<String> existingCoordinateKeys = buildExistingCoordinateKeySet(area.getAreaCode());

        LocationGeneratePreviewResult result = new LocationGeneratePreviewResult();
        result.setLocationProfileId(profile.getId());
        result.setAreaId(area.getId());

        List<String> sideCodes = parseSideCodes(profile.getSideCodes());

        int totalTargetCount = 0;
        int existingCount = 0;

        for (Integer aisleNo = profile.getAisleStart(); aisleNo <= profile.getAisleEnd(); aisleNo++) {
            for (String sideCode : sideCodes) {
                for (Integer bayNo = profile.getBayStart(); bayNo <= profile.getBayEnd(); bayNo++) {
                    for (Integer levelNo = profile.getLevelStart(); levelNo <= profile.getLevelEnd(); levelNo++) {
                        for (Integer depthNo = profile.getDepthStart(); depthNo <= profile.getDepthEnd(); depthNo++) {

                            totalTargetCount++;

                            String coordinateKey = buildCoordinateKey(
                                    area.getAreaCode(), aisleNo, sideCode, bayNo, levelNo, depthNo
                            );

                            String locationCode = buildLocationCode(
                                    area, aisleNo, sideCode, bayNo, levelNo, depthNo
                            );

                            if (existingCoordinateKeys.contains(coordinateKey)) {
                                existingCount++;
                            }

                            if (result.getPreviewLocationCodes().size() < MAX_PREVIEW_SAMPLE_COUNT) {
                                result.addPreviewLocationCode(locationCode);
                            }
                        }
                    }
                }
            }
        }

        result.setTotalTargetCount(totalTargetCount);
        result.setExistingCount(existingCount);
        result.setCreatableCount(totalTargetCount - existingCount);

        return result;
    }

    /**
     * 로케이션 프로파일 기준 실제 생성.
     *
     * @param areaCode 영역 코드
     * @param profileCode 로케이션 프로파일 코드
     * @return 생성 결과
     */
    @Transactional
    public LocationGenerateResult generateByProfileCode(String areaCode, String profileCode) {
        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        TbAcLocationProfile profile = locationQueryService.findLocationProfileByCode(areaCode, profileCode);

        validateGenerationTarget(profile, area);

        Set<String> existingCoordinateKeys = buildExistingCoordinateKeySet(area.getAreaCode());

        LocationGenerateResult result = new LocationGenerateResult();
        result.setLocationProfileId(profile.getId());
        result.setAreaId(area.getId());

        List<String> sideCodes = parseSideCodes(profile.getSideCodes());

        int requestedCount = 0;
        int createdCount = 0;
        int skippedCount = 0;
        int sortSeq = nextSortSeq(area.getId());

        List<TbAcLocation> insertBuffer = new ArrayList<>(LOCATION_INSERT_BATCH_SIZE);

        for (Integer aisleNo = profile.getAisleStart(); aisleNo <= profile.getAisleEnd(); aisleNo++) {
            for (String sideCode : sideCodes) {
                for (Integer bayNo = profile.getBayStart(); bayNo <= profile.getBayEnd(); bayNo++) {
                    for (Integer levelNo = profile.getLevelStart(); levelNo <= profile.getLevelEnd(); levelNo++) {
                        for (Integer depthNo = profile.getDepthStart(); depthNo <= profile.getDepthEnd(); depthNo++) {

                            requestedCount++;

                            String coordinateKey = buildCoordinateKey(
                                    area.getAreaCode(), aisleNo, sideCode, bayNo, levelNo, depthNo
                            );

                            String locationCode = buildLocationCode(
                                    area, aisleNo, sideCode, bayNo, levelNo, depthNo
                            );

                            if (existingCoordinateKeys.contains(coordinateKey)) {
                                skippedCount++;

                                if (result.getSkippedLocationCodes().size() < MAX_RESULT_SAMPLE_COUNT) {
                                    result.addSkippedLocationCode(locationCode);
                                }

                                continue;
                            }

                            TbAcLocation location = new TbAcLocation();
                            location.setAreaId(area.getId());
                            location.setLocationCode(locationCode);
                            location.setAisleNo(aisleNo);
                            location.setSideCode(sideCode);
                            location.setBayNo(bayNo);
                            location.setLevelNo(levelNo);
                            location.setDepthNo(depthNo);
                            location.setLocationType(profile.getLocationType());
                            location.setUsageStatusCode("ENABLED");
                            location.setInboundAllowedYn(defaultYn(profile.getInboundAllowedYn(), AcYn.Y.name()));
                            location.setOutboundAllowedYn(defaultYn(profile.getOutboundAllowedYn(), AcYn.Y.name()));
                            location.setMixedLoadYn(defaultYn(profile.getMixedLoadYn(), AcYn.N.name()));
                            location.setFrontPriorityYn(depthNo == 1 ? AcYn.Y.name() : AcYn.N.name());
                            location.setSortSeq(sortSeq++);
                            location.setActiveYn(AcYn.Y.name());
                            location.setLocationGrade("D");
                            location.setAccessScore(null);
                            location.setPrimaryAccessPointId(null);

                            /*
                             * 중요:
                             * 실제 DB insert는 바로 하지 않고 버퍼에 담는다.
                             * 단, 같은 생성 요청 안에서 중복 좌표가 다시 생성되지 않도록
                             * existingCoordinateKeys에는 즉시 추가한다.
                             */
                            insertBuffer.add(location);
                            existingCoordinateKeys.add(coordinateKey);

                            if (result.getCreatedLocationCodes().size() < MAX_RESULT_SAMPLE_COUNT) {
                                result.addCreatedLocationCode(locationCode);
                            }

                            /*
                             * 일정 건수마다 batch insert 처리.
                             * batchInsertLocations()가 성공하면 실제 insert 건수를 반환한다.
                             */
                            if (insertBuffer.size() >= LOCATION_INSERT_BATCH_SIZE) {
                                createdCount += flushLocationInsertBuffer(insertBuffer);
                            }
                        }
                    }
                }
            }
        }

        /*
         * 마지막 남은 버퍼 insert.
         */
        if (!insertBuffer.isEmpty()) {
            createdCount += flushLocationInsertBuffer(insertBuffer);
        }

        result.setRequestedCount(requestedCount);
        result.setCreatedCount(createdCount);
        result.setSkippedCount(skippedCount);

        return result;
    }

    public void validateGenerationTarget(TbAcLocationProfile profile, TbAcStorageArea area) {
        if (profile == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "Location profile is null.");
        }

        if (area == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "Storage area is null.");
        }

        if (ValueUtil.isEmpty(area.getAreaCode())) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "Area code is empty. areaId=" + area.getId()
            );
        }

        validateRange("aisle", profile.getAisleStart(), profile.getAisleEnd());
        validateRange("bay", profile.getBayStart(), profile.getBayEnd());
        validateRange("level", profile.getLevelStart(), profile.getLevelEnd());
        validateRange("depth", profile.getDepthStart(), profile.getDepthEnd());

        List<String> sideCodes = parseSideCodes(profile.getSideCodes());
        if (sideCodes.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_SIDE,
                    "No valid side code exists in location profile. profileCode=" + profile.getProfileCode()
            );
        }
    }

    public String buildLocationCode(TbAcStorageArea area,
                                    Integer aisleNo,
                                    String sideCode,
                                    Integer bayNo,
                                    Integer levelNo,
                                    Integer depthNo) {

        return locationCodeBuilder.build(
                area.getAreaCode(),
                aisleNo,
                sideCode,
                bayNo,
                levelNo,
                depthNo
        );
    }

    /**
     * 로케이션 insert 버퍼를 DB에 반영하고 버퍼를 비운다.
     *
     * @param insertBuffer insert 대상 버퍼
     * @return 실제 insert 처리 건수
     */
    private int flushLocationInsertBuffer(List<TbAcLocation> insertBuffer) {
        if (insertBuffer == null || insertBuffer.isEmpty()) {
            return 0;
        }

        int insertedCount = batchInsertLocations(insertBuffer);
        insertBuffer.clear();

        return insertedCount;
    }

    /**
     * 로케이션 일괄 insert.
     *
     * <p>
     * queryManager.insert()를 사용하지 않기 때문에,
     * id / domain_id / creator_id / updater_id / created_at / updated_at 공통 컬럼을 직접 세팅한다.
     * </p>
     *
     * @param locations 생성할 로케이션 목록
     * @return 실제 insert 처리 건수
     */
    private int batchInsertLocations(List<TbAcLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            return 0;
        }

        String sql =
                "insert into logis_asrs.tb_ac_location ( " +
                        "    id, " +
                        "    domain_id, " +
                        "    creator_id, " +
                        "    updater_id, " +
                        "    created_at, " +
                        "    updated_at, " +
                        "    area_id, " +
                        "    location_code, " +
                        "    aisle_no, " +
                        "    side_code, " +
                        "    bay_no, " +
                        "    level_no, " +
                        "    depth_no, " +
                        "    location_type, " +
                        "    usage_status_code, " +
                        "    inbound_allowed_yn, " +
                        "    outbound_allowed_yn, " +
                        "    mixed_load_yn, " +
                        "    front_priority_yn, " +
                        "    sort_seq, " +
                        "    active_yn, " +
                        "    location_grade, " +
                        "    access_score, " +
                        "    primary_access_point_id " +
                        ") values ( " +
                        "    :id, " +
                        "    :domainId, " +
                        "    :creatorId, " +
                        "    :updaterId, " +
                        "    :createdAt, " +
                        "    :updatedAt, " +
                        "    :areaId, " +
                        "    :locationCode, " +
                        "    :aisleNo, " +
                        "    :sideCode, " +
                        "    :bayNo, " +
                        "    :levelNo, " +
                        "    :depthNo, " +
                        "    :locationType, " +
                        "    :usageStatusCode, " +
                        "    :inboundAllowedYn, " +
                        "    :outboundAllowedYn, " +
                        "    :mixedLoadYn, " +
                        "    :frontPriorityYn, " +
                        "    :sortSeq, " +
                        "    :activeYn, " +
                        "    :locationGrade, " +
                        "    :accessScore, " +
                        "    :primaryAccessPointId " +
                        ")";

        Long domainId = Domain.currentDomainId();
        String userId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        MapSqlParameterSource[] params = new MapSqlParameterSource[locations.size()];

        for (int i = 0; i < locations.size(); i++) {
            TbAcLocation location = locations.get(i);

            /*
             * queryManager.insert()가 자동 생성하던 id를 직접 생성한다.
             * 기존 id가 이미 세팅되어 있으면 그대로 사용한다.
             */
            String id = ensureLocationId(location);

            params[i] = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("domainId", domainId)
                    .addValue("creatorId", userId)
                    .addValue("updaterId", userId)
                    .addValue("createdAt", now)
                    .addValue("updatedAt", now)
                    .addValue("areaId", location.getAreaId())
                    .addValue("locationCode", location.getLocationCode())
                    .addValue("aisleNo", location.getAisleNo())
                    .addValue("sideCode", location.getSideCode())
                    .addValue("bayNo", location.getBayNo())
                    .addValue("levelNo", location.getLevelNo())
                    .addValue("depthNo", location.getDepthNo())
                    .addValue("locationType", location.getLocationType())
                    .addValue("usageStatusCode", location.getUsageStatusCode())
                    .addValue("inboundAllowedYn", location.getInboundAllowedYn())
                    .addValue("outboundAllowedYn", location.getOutboundAllowedYn())
                    .addValue("mixedLoadYn", location.getMixedLoadYn())
                    .addValue("frontPriorityYn", location.getFrontPriorityYn())
                    .addValue("sortSeq", location.getSortSeq())
                    .addValue("activeYn", location.getActiveYn())
                    .addValue("locationGrade", location.getLocationGrade())
                    .addValue("accessScore", location.getAccessScore())
                    .addValue("primaryAccessPointId", location.getPrimaryAccessPointId());
        }

        int[] batchResult = namedParameterJdbcTemplate.batchUpdate(sql, params);

        return sumBatchResult(batchResult);
    }

    /**
     * 로케이션 ID 보정.
     *
     * <p>
     * queryManager.insert()를 사용하지 않으므로 UUID를 직접 생성한다.
     * tb_ac_location.id 컬럼 길이가 varchar(40) 기준이면 UUID 문자열 36자는 저장 가능하다.
     * </p>
     *
     * @param location 로케이션 엔티티
     * @return 로케이션 ID
     */
    private String ensureLocationId(TbAcLocation location) {
        if (!ValueUtil.isEmpty(location.getId())) {
            return location.getId();
        }

        String id = UUID.randomUUID().toString();
        location.setId(id);

        return id;
    }

    /**
     * batchUpdate 결과 건수 합산.
     *
     * <p>
     * JDBC 드라이버에 따라 성공했지만 정확한 건수를 알 수 없는 경우
     * Statement.SUCCESS_NO_INFO(-2)가 반환될 수 있다.
     * 이 경우 요청 1건 성공으로 간주한다.
     * </p>
     *
     * @param batchResult batchUpdate 결과 배열
     * @return 처리 건수
     */
    private int sumBatchResult(int[] batchResult) {
        if (batchResult == null || batchResult.length == 0) {
            return 0;
        }

        int total = 0;

        for (int count : batchResult) {
            if (count > 0) {
                total += count;
            } else if (count == Statement.SUCCESS_NO_INFO) {
                total++;
            }
        }

        return total;
    }

    /**
     * 공통 컬럼 creator_id / updater_id 입력용 사용자 ID 조회.
     *
     * <p>
     * Spring Security 인증 정보가 있으면 authentication name을 사용한다.
     * 배치, Quartz, 초기화 작업 등 사용자 컨텍스트가 없는 경우 system을 사용한다.
     * </p>
     *
     * <p>
     * 현장 elidom User.currentUser() 기준으로 넣어야 한다면
     * 이 메서드만 프로젝트 사용자 컨텍스트에 맞게 교체하면 된다.
     * </p>
     *
     * @return 사용자 ID
     */
    private String currentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return "system";
            }

            String name = authentication.getName();

            if (ValueUtil.isEmpty(name) || "anonymousUser".equals(name)) {
                return "system";
            }

            /*
             * creator_id / updater_id 컬럼이 varchar(32)인 경우가 많아 방어적으로 자른다.
             */
            return normalizeAuditUserId(name);
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 공통 컬럼 사용자 ID 길이 보정.
     *
     * @param userId 사용자 ID
     * @return 보정된 사용자 ID
     */
    private String normalizeAuditUserId(String userId) {
        if (ValueUtil.isEmpty(userId)) {
            return "system";
        }

        return userId.length() > 32 ? userId.substring(0, 32) : userId;
    }

    private List<String> parseSideCodes(String rawSideCodes) {
        List<String> result = new ArrayList<>();

        if (ValueUtil.isEmpty(rawSideCodes)) {
            return result;
        }

        String[] tokens = rawSideCodes.split(",");
        for (String token : tokens) {
            if (ValueUtil.isEmpty(token)) {
                continue;
            }

            String side = token.trim().toUpperCase();

            /*
             * 유효하지 않은 side code인 경우 enum 검증에서 예외 발생.
             */
            AcLocationSide.from(side);

            result.add(side);
        }

        return result;
    }

    private Set<String> buildExistingCoordinateKeySet(String areaCode) {
        Set<String> keys = new HashSet<>();
        List<TbAcLocation> locations = locationQueryService.findAllLocationsByAreaCode(areaCode);

        for (TbAcLocation location : locations) {
            keys.add(buildCoordinateKey(
                    areaCode,
                    location.getAisleNo(),
                    location.getSideCode(),
                    location.getBayNo(),
                    location.getLevelNo(),
                    location.getDepthNo()
            ));
        }

        return keys;
    }

    private String buildCoordinateKey(String areaCode,
                                      Integer aisleNo,
                                      String sideCode,
                                      Integer bayNo,
                                      Integer levelNo,
                                      Integer depthNo) {

        return areaCode + "|" + aisleNo + "|" + sideCode + "|" + bayNo + "|" + levelNo + "|" + depthNo;
    }

    private void validateRange(String name, Integer start, Integer end) {
        if (start == null || end == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_COORDINATE,
                    "Range is null. name=" + name
            );
        }

        if (start < 0 || end < 0 || start > end) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_LOCATION_COORDINATE,
                    "Invalid range. name=" + name + ", start=" + start + ", end=" + end
            );
        }
    }

    private String defaultYn(String value, String defaultValue) {
        return ValueUtil.isEmpty(value) ? defaultValue : value;
    }

    private int nextSortSeq(String areaId) {
        String sql =
                "select coalesce(max(sort_seq), 0) + 1 as next_seq " +
                        "  from logis_asrs.tb_ac_location " +
                        " where domain_id = :domainId " +
                        "   and area_id = :areaId ";

        java.util.Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId",
                Domain.currentDomainId(), areaId
        );

        Integer nextSeq = this.queryManager.selectBySql(sql, param, Integer.class);

        return nextSeq == null ? 1 : nextSeq;
    }
}