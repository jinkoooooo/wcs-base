package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.LocationStatus;
import operato.logis.kmat_2026.entity.TbEcsLocMst;
import operato.logis.kmat_2026.util.InventoryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TbEcsLocMstService extends AbstractQueryService {

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(TbEcsLocMstService.class);

    public TbEcsLocMst selectLocationPointsByProjectCdAndGroupCdAndLocationRole(String groupCd, String locationRole){
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("groupCd",groupCd);
        condition.addFilter("locationRole",locationRole);
        condition.setMaxResultSize(1);

        return this.queryManager.selectByCondition(TbEcsLocMst.class, condition);
    }

    public String selectLastRobotArmTaskId(){
        String sql = "select task_id from tb_ecs_amhs_task teat where to_side = 'AGFrobotarm' order by updated_at desc limit 1;";

        return this.queryManager.selectBySql(sql, null, String.class);
    }

    public TbEcsLocMst selectLocationPointsByLocationCd(String locationCd){
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("locationCd",locationCd);
        condition.setMaxResultSize(1);

        return this.queryManager.selectByCondition(TbEcsLocMst.class, condition);
    }

    public List<TbEcsLocMst> selectLocationByGroupCd(String groupCd){
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("groupCd", groupCd); // 기존 조건 유지
        condition.addFilter("locationCd", OrmConstants.IS_NOT_NULL, null);
        condition.addFilter("positionX", OrmConstants.IS_NOT_NULL, null);
        condition.addFilter("positionY", OrmConstants.IS_NOT_NULL, null);

        return this.queryManager.selectList(TbEcsLocMst.class, condition);
    }

    public Boolean isAvailableLocation(TbEcsLocMst locationPoint){
        if(ValueUtil.isEmpty(locationPoint)){
            throw new ElidomRuntimeException("포인트 객체가 존재하지 않습니다.");
        }

        return ValueUtil.isEqual(locationPoint.getLocationUseYn(), 1)
                && ValueUtil.isEqual(locationPoint.getLockedYn(), 0);
    }

    @Transactional
    public void updateLocationStatusByLocationCd(String locationCd, String locationStatus){
        String sql = "UPDATE tb_ecs_loc_mst " +
                "SET location_status = :locationStatus " +
                "WHERE domain_id = :domainId AND location_cd = :locationCd";

        this.queryManager.executeBySql(
                sql,
                ValueUtil.newMap(
                        "domainId,locationStatus,locationCd",
                        InventoryConstants.DOMAIN,
                        locationStatus,
                        locationCd
                )
        );
    }

    @Transactional
    public void lockLocation(String locationCd) {
        String sql = "update tb_ecs_loc_mst set locked_yn = 1 where domain_id = :domainId and location_cd = :locationCd and locked_yn = 0";

        this.queryManager.executeBySql(sql,ValueUtil.newMap("domainId,locationCd",7L,locationCd));
    }

    @Transactional
    public void unlockLocation(String locationCd) {
        String sql = "update tb_ecs_loc_mst set locked_yn = 0, lock_order_id = null where domain_id = :domainId and location_cd = :locationCd and locked_yn != 0";

        this.queryManager.executeBySql(sql,ValueUtil.newMap("domainId,locationCd",7L,locationCd));
    }

    @Transactional
    public void unlockAndUpdateLocationStatus(String locationCd, LocationStatus locationStatus) {
        String sql = "update tb_ecs_loc_mst set location_status = :locationStatus, locked_yn = 0, lock_order_id = null " +
                "where domain_id = :domainId and location_cd = :locationCd";

        Map<String, Object> condiction = ValueUtil.newMap("locationStatus,domainId,locationCd", locationStatus.getCode(), 7L, locationCd);
        this.queryManager.executeBySql(sql,condiction);
    }

    /**
     * GroupCd 내 EMPTY location 조회
     */
    public List<TbEcsLocMst> findEmptyLocationInZone(String zoneCd, Long domainId) {
        Query query = OrmUtil.newConditionForExecution(domainId);

        // 필수 조건
        query.addFilter("groupCd", zoneCd);                // Zone 구분 (예: MPS_ZONE)
        query.addFilter("locationStatus", "EMPTY");        // 상태: 비어 있어야 함
        query.addFilter("locationUseYn", 1);             // 사용 가능 상태
        query.addFilter("lockedYn", 0);                    // 잠겨 있지 않아야 함

        return queryManager.selectList(TbEcsLocMst.class, query);
    }

    /**
     * GroupCd 내 전체 location 조회
     */
    public List<TbEcsLocMst> findAllLocListByGroupCd(String groupCd) {
        Query query = OrmUtil.newConditionForExecution(7L);

        // 필수 조건
        query.addFilter("groupCd", groupCd);                // Zone 구분 (예: MPS_ZONE)
        query.addFilter("locationUseYn", 1);             // 사용 가능 상태
        query.addFilter("locked_yn", 0);             // 선점 안된 상태

        return queryManager.selectList(TbEcsLocMst.class, query);
    }

    /**
     * GroupCd 내 전체 location 조회
     */
    public List<TbEcsLocMst> findAllLocListByGroupCdExceptLocked(String groupCd) {
        Query query = OrmUtil.newConditionForExecution(7L);

        // 필수 조건
        query.addFilter("groupCd", groupCd);                // Zone 구분 (예: MPS_ZONE)
        query.addFilter("locationUseYn", 1);             // 사용 가능 상태         // 선점 안된 상태

        return queryManager.selectList(TbEcsLocMst.class, query);
    }

    /**
     * location_cd 기준으로 pod_cd와 location_status를 함께 업데이트
     *
     * @param locationCd     위치 코드 (포지션)
     * @param podCd          위치에 배치할 POD 코드 (또는 null로 제거)
     * @param locationStatus 설정할 위치 상태값 (예: EMPTY, FULL, OCCUPIED)
     */
    @Transactional
    public void updatePodCdAndStatus(String locationCd, String podCd, String equipId, LocationStatus locationStatus, String currentTaskId, String currentOrderId) {
        String sql = "UPDATE tb_ecs_loc_mst " +
                "SET pod_cd = :podCd, location_status = :locationStatus, equip_id = :equipId, updated_at = NOW(), location_item_info = :locationItemInfo " +
                "WHERE domain_id = :domainId AND location_cd = :locationCd ";

        Map<String, Object> params = ValueUtil.newMap(
                "podCd,locationStatus,domainId,locationCd,equipId,locationItemInfo,currentOrderId",
                podCd,
                locationStatus.getCode(),
                7L,
                locationCd,
                equipId,
                currentTaskId,
                currentOrderId
        );

        this.queryManager.executeBySql(sql, params);
    }

    @Transactional
    public void clearEquipIdByLocationCd(String locationCd) {
        String sql = "UPDATE tb_ecs_loc_mst " +
                "SET equip_id = NULL " +
                "WHERE domain_id = :domainId AND location_cd = :locationCd";

        Map<String, Object> params = ValueUtil.newMap("domainId,locationCd", 7L, locationCd);
        this.queryManager.executeBySql(sql, params);
    }

    public TbEcsLocMst selectLocationByPodCdAndGroupCdList(String podCd, List<String> groupCdList){
        if(ValueUtil.isEmpty(podCd) || ValueUtil.isEmpty(groupCdList)){
            return null;
        }

        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("podCd",podCd);
        condition.addFilter("groupCd",OrmConstants.IN,groupCdList);
        condition.setMaxResultSize(1);

        return this.queryManager.selectByCondition(TbEcsLocMst.class, condition);
    }

    public TbEcsLocMst selectLocationByPodCd(String podCd){
        if(ValueUtil.isEmpty(podCd)){
            return null;
        }

        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("podCd",podCd);
        condition.setMaxResultSize(1);

        return this.queryManager.selectByCondition(TbEcsLocMst.class, condition);
    }

    public Integer countLocationListByGroupCd(String groupCd){
        Query condition = OrmUtil.newConditionForExecution(7L);
        condition.addFilter("groupCd",groupCd);
        condition.setMaxResultSize(1);

        return this.queryManager.selectSize(TbEcsLocMst.class, condition);
    }

    @Transactional
    public void updateAgfPobLeftLocationSeq(){
        String sql = "" +
                "UPDATE tb_ecs_loc_mst\n" +
                "SET location_seq = (\n" +
                "    (4 - (((regexp_replace(location_cd, '.*-', '', 'g')::INT - 1) % 4) + 1)) * 4\n" +
                "  + (4 - (((regexp_replace(location_cd, '.*-', '', 'g')::INT - 1) / 4) + 1))\n" +
                "  + 1\n" +
                ")::VARCHAR\n" +
                "WHERE group_cd = 'AGFPOB_LEFT'";

        this.queryManager.executeBySql(sql,null);
    }

    @Transactional
    public void updateAgfPotLocationSeq(){
        String sql = "UPDATE tb_ecs_loc_mst\n" +
                "    SET location_seq = (regexp_replace(location_cd, '\\D', '', 'g')::INT)::VARCHAR\n" +
                "    WHERE group_cd = 'AGFPOT'";

        this.queryManager.executeBySql(sql,null);
    }

    /**
     * update location EquipId
     * @param locationCd
     * @param equipId
     */
    @Transactional
    public void updateLocationAmrId(String locationCd, String equipId) {
        String sql = "UPDATE tb_ecs_loc_mst " +
                "SET equip_id = :equipId " +
                "WHERE domain_id = :domainId AND location_cd = :locationCd";

        Map<String, Object> params = ValueUtil.newMap("domainId,locationCd,equipId", 7L, locationCd,equipId);
        this.queryManager.executeBySql(sql, params);
    }

    /**
     * 여러 groupCd 목록을 받아,
     * 각 그룹에서 POD 또는 FULL 상태(locationStatus)이며
     * podCd 가 존재하는 로케이션을 모두 조회한다.
     *
     * @param groupCdList AMRSRT, AMRDST 등 그룹 코드 리스트
     * @return 대상 로케이션 목록
     */
    public List<TbEcsLocMst> findLocationsWithPodOrFullByGroupCdList(List<String> groupCdList) {

        if (ValueUtil.isEmpty(groupCdList)) {
            return Collections.emptyList();
        }

        Query query = OrmUtil.newConditionForExecution(7L);

        // groupCd IN (...)
        query.addFilter("groupCd", OrmConstants.IN, groupCdList);

        // locationStatus = FULL 또는 POD
        query.addFilter("locationStatus",
                OrmConstants.IN,
                Arrays.asList(
                        LocationStatus.FULL.getCode(),
                        LocationStatus.POD.getCode()
                )
        );

        // podCd NOT NULL (POD 이 존재하는 로케이션만)
        query.addFilter("podCd", OrmConstants.IS_NOT_NULL, null);

        // lockedYn = 0 (사용 가능한 위치만)
        query.addFilter("lockedYn", 0);

        // 조회 실행
        return this.queryManager.selectList(TbEcsLocMst.class, query);
    }

    @Transactional
    public boolean tryLockLocation(String locationCd) {
        String sql =
                "UPDATE tb_ecs_loc_mst " +
                        "   SET locked_yn = 1, updated_at = now() " +
                        " WHERE domain_id = :domainId " +
                        "   AND location_cd = :locationCd " +
                        "   AND locked_yn = 0 " +
                        " RETURNING location_cd";

        Map<String, Object> p = ValueUtil.newMap("domainId,locationCd", 7L, locationCd);

        String locked = this.queryManager.selectBySql(sql, p, String.class);
        return ValueUtil.isNotEmpty(locked);
    }

    /**
     * locationCd 기준 단건 조회
     *
     * [용도]
     * - processor / callback / cancel 처리 시
     *   현재 위치 마스터 정보를 바로 확인할 때 사용
     */
    @Transactional(readOnly = true)
    public TbEcsLocMst findLocationByCode(String locationCd) {
        if (ValueUtil.isEmpty(locationCd)) {
            return null;
        }

        return queryManager.selectByCondition(
                TbEcsLocMst.class,
                ValueUtil.newMap(
                        "domainId,locationCd",
                        InventoryConstants.DOMAIN,
                        locationCd
                )
        );
    }

    /**
     * ============================================================================
     * 단건 후보 조회 (SELECT only)
     * ============================================================================
     *
     * [역할]
     * - groupCd / locationStatus / 정렬 조건으로
     *   선점 후보 1건만 조회한다.
     *
     * [중요]
     * - 여기서는 SELECT ... FOR UPDATE SKIP LOCKED 까지만 수행한다.
     * - locked_yn / lock_order_id 를 실제로 갱신하지 않는다.
     * - 즉, "후보 조회" 전용이다.
     *
     * [호출 위치]
     * - RouteSelectService 에서 호출 후
     *   LocationLockService.tryLockLocation(...) 으로 최종 lock 확정
     */
    @Transactional
    public TbEcsLocMst selectOneCandidateWithLockSkip(
            String groupCd,
            String locationStatus,
            boolean asc
    ) {
        return selectOneCandidateWithLockSkip(groupCd, locationStatus, asc, null);
    }

    /**
     * ============================================================================
     * 단건 후보 조회 (podCdList 포함, SELECT only)
     * ============================================================================
     *
     * [역할]
     * - 특정 groupCd / status 조건에서
     *   podCdList 에 포함된 POD 만 대상으로 후보 1건 조회
     *
     * [용도]
     * - 현재 배치 POD 우선 선택
     * - 특정 POD 집합 안에서만 위치 선택
     */
    @Transactional
    public TbEcsLocMst selectOneCandidateWithLockSkip(
            String groupCd,
            String locationStatus,
            boolean asc,
            List<String> podCdList
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * ");
        sql.append("  FROM tb_ecs_loc_mst ");
        sql.append(" WHERE domain_id = :domainId ");
        sql.append("   AND group_cd = :groupCd ");
        sql.append("   AND location_status = :locationStatus ");
        sql.append("   AND location_use_yn = 1 ");
        sql.append("   AND locked_yn = 0 ");

        if (ValueUtil.isNotEmpty(podCdList)) {
            sql.append("   AND pod_cd IN (:podCdList) ");
        }

        sql.append(" ORDER BY location_seq ").append(asc ? "ASC " : "DESC ");
        sql.append(" LIMIT 1 FOR UPDATE SKIP LOCKED");

        Map<String, Object> params = ValueUtil.newMap(
                "domainId,groupCd,locationStatus",
                InventoryConstants.DOMAIN,
                groupCd,
                locationStatus
        );

        if (ValueUtil.isNotEmpty(podCdList)) {
            params.put("podCdList", podCdList);
        }

        TbEcsLocMst candidate = queryManager.selectBySql(
                sql.toString(),
                params,
                TbEcsLocMst.class
        );

        if (ValueUtil.isEmpty(candidate) || ValueUtil.isEmpty(candidate.getLocationCd())) {
            return null;
        }

        return candidate;
    }

    /**
     * ============================================================================
     * 다건 후보 조회 (SELECT only)
     * ============================================================================
     *
     * [역할]
     * - 여러 groupCd 중에서 조건에 맞는 후보들을 조회한다.
     * - 여기서는 row 들을 SELECT ... FOR UPDATE SKIP LOCKED 로만 잡고,
     *   실제 business lock 은 하지 않는다.
     *
     * [중요]
     * - 반환된 목록은 "후보"일 뿐이다.
     * - 실제 선점은 상위에서 row 별 tryLockLocation(...) 으로 확정해야 한다.
     */
    @Transactional
    public List<TbEcsLocMst> selectListCandidateWithLockSkip(
            List<String> groupCdList,
            String locationStatus,
            int size,
            boolean asc
    ) {
        if (ValueUtil.isEmpty(groupCdList) || size <= 0) {
            return Collections.emptyList();
        }

        String sql =
                "SELECT * " +
                        "  FROM tb_ecs_loc_mst " +
                        " WHERE domain_id = :domainId " +
                        "   AND group_cd IN (:groupCd) " +
                        "   AND location_status = :locationStatus " +
                        "   AND location_use_yn = 1 " +
                        "   AND locked_yn = 0 " +
                        " ORDER BY location_seq " + (asc ? "ASC " : "DESC ") +
                        " LIMIT :limit FOR UPDATE SKIP LOCKED";

        List<TbEcsLocMst> candidates = queryManager.selectListBySql(
                sql,
                ValueUtil.newMap(
                        "domainId,groupCd,locationStatus,limit",
                        InventoryConstants.DOMAIN,
                        groupCdList,
                        locationStatus,
                        size
                ),
                TbEcsLocMst.class,
                0,
                0
        );

        if (ValueUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }

        return candidates;
    }

    /**
     * ============================================================================
     * 다건 후보 조회 (CSV groupCd, SELECT only)
     * ============================================================================
     *
     * [예시]
     * - "AMRBUF,AMRMDB"
     *
     * [용도]
     * - 상위 서비스에서 CSV 형태로 groupCd 를 넘길 때 사용
     */
    @Transactional
    public List<TbEcsLocMst> selectListCandidateWithLockSkip(
            String groupCdCsv,
            String locationStatus,
            int size,
            boolean asc
    ) {
        if (ValueUtil.isEmpty(groupCdCsv) || size <= 0) {
            return Collections.emptyList();
        }

        List<String> groupCdList = Arrays.stream(groupCdCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (ValueUtil.isEmpty(groupCdList)) {
            return Collections.emptyList();
        }

        return selectListCandidateWithLockSkip(
                groupCdList,
                locationStatus,
                size,
                asc
        );
    }
}
