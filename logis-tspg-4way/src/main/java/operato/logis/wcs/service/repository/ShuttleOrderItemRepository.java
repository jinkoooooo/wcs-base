package operato.logis.wcs.service.repository;

import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;

/**
 * TbWcsShuttleOrderItem 영속성 전담 DAO.
 *
 * 셔틀 주문 라인의 조회·생성·line_status 전이를 한 aggregate 단위로 캡슐화한다.
 */
@Repository
public class ShuttleOrderItemRepository extends AbstractQueryService {

    /** PK 로 단건 조회. */
    public TbWcsShuttleOrderItem findById(String id) {
        return this.queryManager.select(TbWcsShuttleOrderItem.class, id);
    }

    /** order_key 로 라인 목록 조회. */
    public List<TbWcsShuttleOrderItem> findByOrderKey(String orderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", orderKey);
        return this.queryManager.selectList(TbWcsShuttleOrderItem.class, condition);
    }

    /** (order_key, line_no) 로 단건 조회. */
    public TbWcsShuttleOrderItem findByLineNo(String orderKey, int lineNo) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", orderKey);
        condition.addFilter("line_no", lineNo);
        return this.queryManager.selectByCondition(TbWcsShuttleOrderItem.class, condition);
    }

    /** 신규 라인 insert. */
    public TbWcsShuttleOrderItem insert(TbWcsShuttleOrderItem entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 전체 update. */
    public TbWcsShuttleOrderItem update(TbWcsShuttleOrderItem entity) {
        this.queryManager.update(entity);
        return entity;
    }

    /** 라인 목록 일괄 insert. */
    public void insertAll(List<TbWcsShuttleOrderItem> items) {
        for (TbWcsShuttleOrderItem item : items) {
            this.insert(item);
        }
    }

    /** 단일 라인 line_status 갱신 (존재할 때만). */
    public void updateLineStatus(String id, int lineStatus) {
        TbWcsShuttleOrderItem entity = this.findById(id);
        if (ValueUtil.isNotEmpty(entity)) {
            entity.setLineStatus(lineStatus);
            this.queryManager.update(entity);
        }
    }

    /** order_key 의 모든 라인 line_status 일괄 갱신. */
    public void updateAllLineStatus(String orderKey, int lineStatus) {
        List<TbWcsShuttleOrderItem> items = this.findByOrderKey(orderKey);
        // 라인별 상태 세팅 후 audit update
        for (TbWcsShuttleOrderItem item : items) {
            item.setLineStatus(lineStatus);
            this.queryManager.update(item);
        }
    }
}
