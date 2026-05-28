package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.List;

/**
 * TbWcsHostOrderItem Entity Service (DB 전용)
 */
@Service
public class TbWcsHostOrderItemService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbWcsHostOrderItemService.class);

    /**
     * ID로 조회
     */
    public TbWcsHostOrderItem findById(String id) {
        return this.queryManager.select(TbWcsHostOrderItem.class, id);
    }

    /**
     * hostSystemCode + hostOrderKey로 아이템 목록 조회
     */
    public List<TbWcsHostOrderItem> findByHostOrderKey(String hostSystemCode, String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_system_code", hostSystemCode);
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectList(TbWcsHostOrderItem.class, condition);
    }

    /**
     * hostSystemCode + hostOrderKey + lineNo로 단건 조회
     */
    public TbWcsHostOrderItem findByLineNo(String hostSystemCode, String hostOrderKey, int lineNo) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_system_code", hostSystemCode);
        condition.addFilter("host_order_key", hostOrderKey);
        condition.addFilter("line_no", lineNo);
        return this.queryManager.selectByCondition(TbWcsHostOrderItem.class, condition);
    }

    /**
     * 신규 저장
     */
    public TbWcsHostOrderItem insert(TbWcsHostOrderItem entity) {
        logger.debug("Inserting TbWcsHostOrderItem: hostOrderKey={}, lineNo={}",
                entity.getHostOrderKey(), entity.getLineNo());
        this.queryManager.insert(entity);
        return entity;
    }

    /**
     * 수정
     */
    public TbWcsHostOrderItem update(TbWcsHostOrderItem entity) {
        logger.debug("Updating TbWcsHostOrderItem: id={}", entity.getId());
        this.queryManager.update(entity);
        return entity;
    }

    /**
     * 여러 아이템 일괄 저장
     */
    public void insertAll(List<TbWcsHostOrderItem> items) {
        for (TbWcsHostOrderItem item : items) {
            this.insert(item);
        }
    }

    /**
     * wcsOrderItemId 업데이트
     */
    public void updateWcsOrderItemId(String id, String wcsOrderItemId) {
        TbWcsHostOrderItem entity = this.findById(id);
        if (entity != null) {
            entity.setWcsOrderItemId(wcsOrderItemId);
            this.queryManager.update(entity);
        }
    }

    /**
     * lineStatus 업데이트
     */
    public void updateLineStatus(String id, int lineStatus) {
        TbWcsHostOrderItem entity = this.findById(id);
        if (entity != null) {
            entity.setLineStatus(lineStatus);
            this.queryManager.update(entity);
        }
    }

    /**
     * DTO 요청으로부터 아이템 목록 생성 및 저장
     */
    public List<TbWcsHostOrderItem> createFromRequest(String hostSystemCode, String hostOrderKey,
                                                       List<HostOrderReceiveRequest.HostOrderItemRequest> itemRequests) {
        List<TbWcsHostOrderItem> items = new ArrayList<>();
        for (HostOrderReceiveRequest.HostOrderItemRequest req : itemRequests) {
            TbWcsHostOrderItem item = new TbWcsHostOrderItem();
            item.setHostSystemCode(hostSystemCode);
            item.setHostOrderKey(hostOrderKey);
            item.setLineNo(req.getLineNo());
            item.setSkuCode(req.getSkuCode());
            item.setLotNo(req.getLotNo());
            item.setQty(req.getQty());
            item.setUom(req.getUom());
            item.setLineStatus(0); // 초기 상태

            this.insert(item);
            items.add(item);
        }
        logger.info("Created {} host order items for hostOrderKey={}", items.size(), hostOrderKey);
        return items;
    }
}
