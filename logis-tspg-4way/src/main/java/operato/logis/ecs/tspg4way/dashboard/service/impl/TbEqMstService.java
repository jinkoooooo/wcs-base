package operato.logis.ecs.tspg4way.dashboard.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.ecs.tspg4way.entity.TbEqMst;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 설비 마스터 Service (대시보드용)
 * TbEqMst 엔터티를 조회하여 대시보드 매핑(RealEqId 등)에 사용
 *
 * [주의]
 * - tb_eq_mst 컬럼은 eq_type/eq_id 가 아니라 type/id 구조(엔티티 기준)
 */
@Service
public class TbEqMstService extends AbstractQueryService {

    /**
     * 설비 그룹별 설비 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqMst> getEquipmentsByGroup(String eqGroupId) {
        String sql = """
            SELECT *
            FROM tb_eq_mst
            WHERE eq_group_id = :eqGroupId
            ORDER BY name ASC, id ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);
        return this.queryManager.selectListBySql(sql, params, TbEqMst.class, 0, 0);
    }

    /**
     * 설비 그룹 + 설비 타입별 설비 목록 조회 (매핑용 콤보박스)
     *
     * - 기존 eq_type -> type 로 변경
     */
    @Transactional(readOnly = true)
    public List<TbEqMst> getEquipmentsByGroupAndType(String eqGroupId, String type) {
        String sql = """
            SELECT *
            FROM tb_eq_mst
            WHERE eq_group_id = :eqGroupId
              AND type = :type
            ORDER BY name ASC, id ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,type", eqGroupId, type);
        return this.queryManager.selectListBySql(sql, params, TbEqMst.class, 0, 0);
    }

    /**
     * 설비 타입별 전체 설비 목록 조회 (설비그룹 무관)
     *
     * - 기존 eq_type -> type 로 변경
     */
    @Transactional(readOnly = true)
    public List<TbEqMst> getEquipmentsByType(Integer type) {
        String sql = """
            SELECT *
            FROM tb_eq_mst
            WHERE type = :type
            ORDER BY eq_group_id ASC, name ASC, id ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("type", type);
        return this.queryManager.selectListBySql(sql, params, TbEqMst.class, 0, 0);
    }

    /**
     * 설비 단건 조회 (PK ID)
     */
    @Transactional(readOnly = true)
    public TbEqMst getEquipment(String id) {
        return this.queryManager.select(TbEqMst.class, id);
    }

    /**
     * 설비 단건 조회 (ID 컬럼)
     *
     * - 기존 getEquipmentByEqId(eq_id) 개념은 엔티티에 없음
     * - 호출부가 "실운영 설비ID"를 넘긴다면, 그게 곧 id 로 보는 게 현재 엔티티 기준에서 가장 안전함
     */
    @Transactional(readOnly = true)
    public TbEqMst getEquipmentById(String id) {
        String sql = "SELECT * FROM tb_eq_mst WHERE id = :id";
        Map<String, Object> params = ValueUtil.newMap("id", id);
        return this.queryManager.selectByCondition(TbEqMst.class, params);
    }

    /**
     * 설비 그룹별 설비 타입 목록 조회 (중복 제거)
     *
     * - 기존 eq_type -> type 로 변경
     */
    @Transactional(readOnly = true)
    public List<String> getEquipmentTypesByGroup(String eqGroupId) {
        String sql = """
            SELECT DISTINCT type
            FROM tb_eq_mst
            WHERE eq_group_id = :eqGroupId
            ORDER BY type ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);
        return this.queryManager.selectListBySql(sql, params, String.class, 0, 0);
    }

    /**
     * 전체 설비 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqMst> getAllEquipments() {
        String sql = """
            SELECT *
            FROM tb_eq_mst
            ORDER BY eq_group_id ASC, name ASC, id ASC
            """;
        return this.queryManager.selectListBySql(sql, null, TbEqMst.class, 0, 0);
    }

    /**
     * PLC별 설비 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqMst> getEquipmentsByPlc(String plcId) {
        String sql = """
            SELECT *
            FROM tb_eq_mst
            WHERE plc_id = :plcId
            ORDER BY name ASC, id ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("plcId", plcId);
        return this.queryManager.selectListBySql(sql, params, TbEqMst.class, 0, 0);
    }
}
