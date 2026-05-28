package operato.logis.wcs.service.repository;

import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * TbWcsHostOrderItem 영속성 전담 DAO.
 *
 * 호스트 주문 라인의 조회·생성·수정을 한 aggregate 단위로 캡슐화한다.
 */
@Repository
public class HostOrderItemRepository extends AbstractQueryService {

    /** PK 로 단건 조회. */
    public TbWcsHostOrderItem findById(String id) {
        return this.queryManager.select(TbWcsHostOrderItem.class, id);
    }

    /** (host_system_code, host_order_key) 로 라인 목록 조회. */
    public List<TbWcsHostOrderItem> findByHostOrderKey(String hostSystemCode, String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_system_code", hostSystemCode);
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectList(TbWcsHostOrderItem.class, condition);
    }

    /** host_order_key 로 라인 목록 조회. */
    public List<TbWcsHostOrderItem> findByHostOrderKey(String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        return this.queryManager.selectList(TbWcsHostOrderItem.class, condition);
    }

    /** test_request_no 로 라인 목록 조회. */
    public List<TbWcsHostOrderItem> findByTestRequestNo(String testRequestNo) {
        if (ValueUtil.isEmpty(testRequestNo)) return null;
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("test_request_no", testRequestNo);
        return this.queryManager.selectList(TbWcsHostOrderItem.class, condition);
    }

    /** host_order_key 의 시험대상(test_required=true) 라인 목록 조회. */
    public List<TbWcsHostOrderItem> findTestTargetItems(String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_order_key", hostOrderKey);
        condition.addFilter("test_required", Boolean.TRUE);
        return this.queryManager.selectList(TbWcsHostOrderItem.class, condition);
    }

    /** 신규 라인 insert. */
    public TbWcsHostOrderItem insert(TbWcsHostOrderItem entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** 전체 update. */
    public TbWcsHostOrderItem update(TbWcsHostOrderItem entity) {
        this.queryManager.update(entity);
        return entity;
    }

    /** 지정 필드 제외 update. */
    public TbWcsHostOrderItem update(TbWcsHostOrderItem entity, String... excludeFields) {
        this.queryManager.update(entity, excludeFields);
        return entity;
    }

    /** 라인 목록 일괄 insert. */
    public void insertAll(List<TbWcsHostOrderItem> items) {
        for (TbWcsHostOrderItem item : items) this.insert(item);
    }

    /** 요청 DTO 의 아이템 목록으로 라인 엔티티 생성·insert 후 반환. */
    public List<TbWcsHostOrderItem> createFromRequest(String hostSystemCode, String hostOrderKey,
                                                      List<HostOrderApi.Item> itemRequests) {
        List<TbWcsHostOrderItem> items = new ArrayList<>();
        // 요청 아이템마다 라인 엔티티로 매핑 후 insert
        for (HostOrderApi.Item req : itemRequests) {
            TbWcsHostOrderItem item = new TbWcsHostOrderItem();
            item.setHostSystemCode(hostSystemCode);
            item.setHostOrderKey(hostOrderKey);
            item.setItemCode(req.getItemCode());
            item.setLotNo(req.getLotNo());
            item.setQty(req.getQty());
            item.setUom(req.getUom());
            item.setLineStatus(0);
            this.insert(item);
            items.add(item);
        }
        return items;
    }
}
