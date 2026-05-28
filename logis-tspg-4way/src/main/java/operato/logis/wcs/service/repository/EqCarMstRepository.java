package operato.logis.wcs.service.repository;

import operato.logis.ecs.tspg4way.entity.TbEqCarMst;
import org.springframework.stereotype.Repository;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

/**
 * TbEqCarMst 영속성 전담 DAO.
 *
 * 셔틀 차량 마스터의 read-only 조회를 캡슐화한다.
 */
@Repository
public class EqCarMstRepository extends AbstractQueryService {

    /** level 로 셔틀 차량 1건 조회. */
    public TbEqCarMst findShuttleByLevel(Integer level) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("level", level);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbEqCarMst.class, condition);
    }
}
