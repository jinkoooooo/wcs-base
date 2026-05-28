package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

/**
 * TbWcsHostOrder Entity Service (DB 전용)
 * - select/insert/update만 담당
 * - 비즈니스 로직 금지
 */
@Service
public class TbWcsHostOrderService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbWcsHostOrderService.class);

    /**
     * ID로 조회
     */
    public TbWcsHostOrder findById(String id) {
        return this.queryManager.select(TbWcsHostOrder.class, id);
    }

    /**
     * hostSystemCode + hostOrderKey로 조회 (멱등 체크용)
     */
    public TbWcsHostOrder findByHostOrderKey(String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectByCondition(TbWcsHostOrder.class, condition);
    }

    /**
     * hostSystemCode + hostOrderKey로 조회 (멱등 체크용)
     */
    public TbWcsHostOrder findByHostOrderKey(String hostSystemCode, String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_system_code", hostSystemCode);
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectByCondition(TbWcsHostOrder.class, condition);
    }

    /**
     * wcsOrderKey로 조회
     */
    public TbWcsHostOrder findByWcsOrderKey(String wcsOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("wcs_order_key", wcsOrderKey);
        return this.queryManager.selectByCondition(TbWcsHostOrder.class, condition);
    }

    /**
     * 상태별 조회
     */
    public List<TbWcsHostOrder> findByStatus(int orderStatus) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", orderStatus);
        return this.queryManager.selectList(TbWcsHostOrder.class, condition);
    }

    /**
     * 신규 저장
     */
    public TbWcsHostOrder insert(TbWcsHostOrder entity) {
        logger.debug("Inserting TbWcsHostOrder: hostOrderKey={}", entity.getHostOrderKey());
        this.queryManager.insert(entity);
        return entity;
    }

    /**
     * 수정
     */
    public TbWcsHostOrder update(TbWcsHostOrder entity) {
        logger.debug("Updating TbWcsHostOrder: id={}, orderStatus={}", entity.getId(), entity.getOrderStatus());
        this.queryManager.update(entity);
        return entity;
    }

    /**
     * 상태 업데이트
     */
    public void updateStatus(String id, int orderStatus) {
        TbWcsHostOrder entity = this.findById(id);
        if (entity != null) {
            entity.setOrderStatus(orderStatus);
            this.queryManager.update(entity);
        }
    }

    /**
     * 에러 상태 업데이트
     */
    public void updateError(String id, int orderStatus, String errorCode, String errorDesc) {
        TbWcsHostOrder entity = this.findById(id);
        if (entity != null) {
            entity.setOrderStatus(orderStatus);
            entity.setErrorCode(errorCode);
            entity.setErrorDesc(errorDesc);
            this.queryManager.update(entity);
        }
    }

    /**
     * wcsOrderKey 업데이트
     */
    public void updateWcsOrderKey(String id, String wcsOrderKey) {
        TbWcsHostOrder entity = this.findById(id);
        if (entity != null) {
            entity.setWcsOrderKey(wcsOrderKey);
            this.queryManager.update(entity);
        }
    }
}
