package operato.logis.wcs.service.repository;

import operato.logis.wcs.entity.TbWcsHostOrderHistory;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

/**
 * TbWcsHostOrderHistory 영속성 전담 DAO.
 *
 * 호스트 주문 이력의 추가·조회를 한 aggregate 단위로 캡슐화한다.
 */
@Repository
public class HostOrderHistoryRepository extends AbstractQueryService {

    /** 이력 1건 insert. */
    public TbWcsHostOrderHistory insert(TbWcsHostOrderHistory entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** (host_system_code, host_order_key) 이력 목록 조회 (생성순). */
    public List<TbWcsHostOrderHistory> findByOrderKey(String hostSystemCode, String hostOrderKey) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("host_system_code", hostSystemCode);
        condition.addFilter("host_order_key", hostOrderKey);
        condition.addOrder("createdAt", true);
        return this.queryManager.selectList(TbWcsHostOrderHistory.class, condition);
    }
}
