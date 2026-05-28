package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqCarMst;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsIfStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Service
public class TbEqCarMstService extends AbstractQueryService {

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(TbEcsLocMstService.class);

    /**
     * 층 간 shuttle 조회
     */
    public TbEqCarMst findShuttleByLevel(Integer level) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("level", level);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbEqCarMst.class, condition);
    }
}
