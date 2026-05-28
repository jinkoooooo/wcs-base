package operato.logis.lms.service.impl.center;

import operato.logis.lms.entity.center.LmsCenterUsers;
import operato.logis.lms.entity.center.LmsCenters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LmsCenterUserService extends AbstractQueryService {

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(LmsCenterUserService.class);

    /**
     * 사용자 소속 센터목록 관계 조회
     * - 비로그인 : 빈 목록
     * - 관리자 : 전체 센터 관계 목록(기본값) 또는 소속 센터 조회(relationFilter = true)
     * - 일반사용자 : 소속 센터 관계 목록
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<LmsCenterUsers> lmsCenterUserListByUser(Boolean relationFilter) {
        boolean useFilter = Boolean.TRUE.equals(relationFilter);
        return lmsCenterUserListByUserImpl(useFilter);
    }

    // 사용자 소속 센터목록 관계 조회
    private List<LmsCenterUsers> lmsCenterUserListByUserImpl(boolean relationFilter) {

        User user = User.currentUser();
        if (ObjectUtils.isEmpty(user)) {
            return new ArrayList<>();
        }

        List<LmsCenterUsers> result;

        if (!relationFilter && User.isCurrentUserAdmin()) {
            Query condition = OrmUtil.newConditionForExecution(user.getDomainId());
            result = this.queryManager.selectList(LmsCenterUsers.class, condition);
        } else {
            Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
            condition.addFilter("userId", user.getLogin());
            condition.addOrder("lcId", true);
            result = queryManager.selectList(LmsCenterUsers.class, condition);
        }

        if (CollectionUtils.isEmpty(result)) {
            logger.info("This user's center is empty.");
            return new ArrayList<>();
        }
        return result;
    }

    // 사용자 소속 센터 & 센터 세부 데이터 조회
    @Transactional(readOnly = true)
    public Map<String, Object> lmsCenterListByUser(boolean relationFilter) {
        Map<String, Object> result = new HashMap<>();
        result.put("items", new ArrayList<>());
        result.put("total", 0);

        // 로그아웃 상태면 빈 값 반환
        User user = User.currentUser();
        if (ObjectUtils.isEmpty(user)) {
            return result;
        }

        List<LmsCenterUsers> cuList = this.lmsCenterUserListByUser(relationFilter);
        if (ObjectUtils.isEmpty(cuList)) {
            return result;
        }

        List<String> lcIdList = cuList.stream().map(LmsCenterUsers::getLcId).toList();
        Query condition =  OrmUtil.newConditionForExecution(user.getDomainId());
        condition.addSelect("lcNm","lcId");
        condition.addFilter("lcId", OrmConstants.IN, lcIdList);
        condition.addOrder("lcId", true);
        List<LmsCenters> lcNmList = queryManager.selectList(LmsCenters.class, condition );

        if (ObjectUtils.isEmpty(lcNmList)) return result;

        List<Map<String, Object>> cuWithNameList = new ArrayList<>();
        for (LmsCenters center : lcNmList) {
            Map<String, Object> item = new HashMap<>();
            item.put("lc_nm", center.getLcNm());
            item.put("lc_id", center.getLcId());
            cuWithNameList.add(item);
        }

        result.put("items", cuWithNameList);
        result.put("total", cuWithNameList.size());
        return result;
    }
}
