package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqGroupMst;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 설비 그룹 마스터 Service (대시보드용)
 *
 * ✅ 최종 엔티티 스펙 기준(tb_eq_group_mst: id, name, type)
 * - tb_eq_group_mst 자체에는 lc_id 컬럼이 없다고 가정
 * - 따라서 "센터(lcId) 기준" 필터는 tb_ecs_2d_page(레이아웃 페이지)와 JOIN해서 처리
 */
@Service
public class TbEqGroupMstService extends AbstractQueryService {

    /**
     * 센터별 설비 그룹 목록 조회
     * - shuttle_layout_page의 lc_id를 기준으로 해당 센터에서 사용중인 eq_group_id(=tb_eq_group_mst.id)만 반환
     */
    @Transactional(readOnly = true)
    public List<TbEqGroupMst> getAllGroups() {
        String sql = """
            SELECT DISTINCT g.*
              FROM tb_eq_group_mst g
             ORDER BY g.id ASC
            """;
        return this.queryManager.selectListBySql(sql, null, TbEqGroupMst.class, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<TbEqGroupMst> getGroupsByLcId(String lcId) {
        String sql = """
        SELECT DISTINCT g.*
          FROM tb_eq_group_mst g
          JOIN tb_ecs_2d_page p
            ON p.eq_group_id = g.id
         WHERE p.lc_id = :lcId
           AND (p.is_active = true OR p.is_active IS NULL)
         ORDER BY g.id ASC
        """;

        Map<String, Object> params = ValueUtil.newMap("lcId", lcId);

        return this.queryManager.selectListBySql(sql, params, TbEqGroupMst.class, 0, 0);
    }

    /**
     * 센터별 설비 그룹 타입으로 필터링 조회
     * - g.type 기준
     */
    @Transactional(readOnly = true)
    public List<TbEqGroupMst> getGroupsByType(String lcId, String eqGroupType) {
        String sql = """
            SELECT DISTINCT g.*
              FROM tb_eq_group_mst g
              JOIN tb_ecs_2d_page p
                ON p.eq_group_id = g.id
             WHERE p.lc_id = :lcId
               AND g.type = :eqGroupType
               AND (p.is_active = true OR p.is_active IS NULL)
             ORDER BY g.id ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupType", lcId, eqGroupType);
        return this.queryManager.selectListBySql(sql, params, TbEqGroupMst.class, 0, 0);
    }

    /**
     * 설비 그룹 단건 조회 (PK: id)
     */
    @Transactional(readOnly = true)
    public TbEqGroupMst getGroup(String id) {
        return this.queryManager.select(TbEqGroupMst.class, id);
    }

    /**
     * 센터 내 설비그룹 단건 조회
     * - eqGroupId는 tb_eq_group_mst.id로 해석
     * - 센터 기준 검증을 위해 shuttle_layout_page로 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public TbEqGroupMst getGroupByEqGroupId(String lcId, String eqGroupId) {
        String sql = """
            SELECT g.*
              FROM tb_eq_group_mst g
              JOIN tb_ecs_2d_page p
                ON p.eq_group_id = g.id
             WHERE p.lc_id = :lcId
               AND g.id = :eqGroupId
             LIMIT 1
            """;
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId", lcId, eqGroupId);
        return this.queryManager.selectBySql(sql, params, TbEqGroupMst.class);
    }

    /**
     * 설비 그룹 내 층 목록 조회 (TbEcs2dPage 기준)
     */
    @Transactional(readOnly = true)
    public List<Integer> getFloorsByEqGroup(String lcId, String eqGroupId) {
        String sql = """
            SELECT DISTINCT floor_level
              FROM tb_ecs_2d_page
             WHERE lc_id = :lcId
               AND eq_group_id = :eqGroupId
               AND (is_active = true OR is_active IS NULL)
             ORDER BY floor_level ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId", lcId, eqGroupId);
        return this.queryManager.selectListBySql(sql, params, Integer.class, 0, 0);
    }
}
