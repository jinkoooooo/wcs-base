package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

/**
 * TbWcsShuttleOrderItem Entity Service (DB 전용)
 */
@Service
public class TbWcsShuttleOrderItemService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbWcsShuttleOrderItemService.class);

    /**
     * ID로 조회
     */
    public TbWcsShuttleOrderItem findById(String id) {
        return this.queryManager.select(TbWcsShuttleOrderItem.class, id);
    }

    /**
     * orderKey로 아이템 목록 조회
     */
    public List<TbWcsShuttleOrderItem> findByOrderKey(String orderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", orderKey);
        return this.queryManager.selectList(TbWcsShuttleOrderItem.class, condition);
    }

    /**
     * orderKey + lineNo로 단건 조회
     */
    public TbWcsShuttleOrderItem findByLineNo(String orderKey, int lineNo) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_key", orderKey);
        condition.addFilter("line_no", lineNo);
        return this.queryManager.selectByCondition(TbWcsShuttleOrderItem.class, condition);
    }

    /**
     * 신규 저장
     */
    public TbWcsShuttleOrderItem insert(TbWcsShuttleOrderItem entity) {
        logger.debug("Inserting TbWcsShuttleOrderItem: orderKey={}, lineNo={}",
                entity.getOrderKey(), entity.getLineNo());
        this.queryManager.insert(entity);
        return entity;
    }

    /**
     * 수정
     */
    public TbWcsShuttleOrderItem update(TbWcsShuttleOrderItem entity) {
        logger.debug("Updating TbWcsShuttleOrderItem: id={}", entity.getId());
        this.queryManager.update(entity);
        return entity;
    }

    /**
     * 여러 아이템 일괄 저장
     */
    public void insertAll(List<TbWcsShuttleOrderItem> items) {
        for (TbWcsShuttleOrderItem item : items) {
            this.insert(item);
        }
    }

    /**
     * lineStatus 업데이트
     */
    public void updateLineStatus(String id, int lineStatus) {
        TbWcsShuttleOrderItem entity = this.findById(id);
        if (entity != null) {
            entity.setLineStatus(lineStatus);
            this.queryManager.update(entity);
        }
    }

    /**
     * orderKey 기준 전체 lineStatus 업데이트
     */
    public void updateAllLineStatus(String orderKey, int lineStatus) {
        List<TbWcsShuttleOrderItem> items = this.findByOrderKey(orderKey);
        for (TbWcsShuttleOrderItem item : items) {
            item.setLineStatus(lineStatus);
            this.queryManager.update(item);
        }
    }
}
