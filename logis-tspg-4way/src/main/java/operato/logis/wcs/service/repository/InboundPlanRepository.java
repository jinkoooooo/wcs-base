package operato.logis.wcs.service.repository;

import operato.logis.wcs.entity.TbWcsInboundPlan;
import org.springframework.stereotype.Repository;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

/**
 * TbWcsInboundPlan 영속성 전담 DAO.
 *
 * 입고 예정 마스터의 생성/삭제는 audit 경로로, 누적 수량 가/감산은
 * 동시성 안전을 위해 단일 원자 UPDATE 로 처리한다.
 */
@Repository
public class InboundPlanRepository extends AbstractQueryService {

    /** PK 단건 조회. */
    public TbWcsInboundPlan findById(String id) {
        if (ValueUtil.isEmpty(id)) return null;
        return this.queryManager.select(TbWcsInboundPlan.class, id);
    }

    /** 신규 예정 insert. */
    public TbWcsInboundPlan insert(TbWcsInboundPlan entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 예정 삭제. */
    public void delete(TbWcsInboundPlan entity) {
        if (ValueUtil.isEmpty(entity)) return;
        this.queryManager.delete(entity);
    }

    /**
     * 누적 입고 주문 수량 원자 가산 — 예정 수량 초과 시 0 row 반환(호출측이 초과 판정).
     * created_at null 이슈 회피를 위해 updated_at 은 SQL 에서 직접 갱신.
     */
    public int addOrderedQty(String id, int qty) {
        String sql = """
                UPDATE tb_wcs_inbound_plan
                   SET ordered_qty = ordered_qty + :qty,
                       updated_at  = now()
                 WHERE id = :id
                   AND ordered_qty + :qty <= planned_qty
                """;
        return this.queryManager.executeBySql(sql, Map.of("id", id, "qty", qty));
    }

    /** 누적 입고 주문 수량 환원(삭제 시) — 0 미만으로 내려가지 않게 보정. */
    public int releaseOrderedQty(String id, int qty) {
        String sql = """
                UPDATE tb_wcs_inbound_plan
                   SET ordered_qty = GREATEST(0, ordered_qty - :qty),
                       updated_at  = now()
                 WHERE id = :id
                """;
        return this.queryManager.executeBySql(sql, Map.of("id", id, "qty", qty));
    }
}
