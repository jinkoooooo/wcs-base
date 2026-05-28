package operato.logis.lms.service.impl.monitoring;

import operato.logis.lms.entity.monitoring.LmsAlarmStatusDev;
import operato.logis.lms.service.impl.center.LmsCenterUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.List;

@Service
public class LmsAlarmStatusDevService extends AbstractQueryService {

    private final LmsCenterUserService lmsCenterUserListByUser;

    public LmsAlarmStatusDevService(LmsCenterUserService lmsCenterUserListByUser) {
        this.lmsCenterUserListByUser = lmsCenterUserListByUser;
    }

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(LmsAlarmStatusDevService.class);

    /**
     * 사용자 소속 센터의 알람 조회
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<LmsAlarmStatusDev> alarmListByUser() {
        try {
            return new ArrayList<>();
            //// 로그아웃 상태면 빈 값 반환
            //User user = User.currentUser();
            //if (ObjectUtils.isEmpty(user)) {
            //    return new ArrayList<>();
            //}
            //
            //List<LmsCenterUsers> result;
            //
            //if (User.isCurrentUserAdmin()) {
            //    // superuser, admin은 전체 조회
            //    Query condition = OrmUtil.newConditionForExecution(user.getDomainId());
            //    result = this.queryManager.selectList(LmsCenterUsers.class, condition);
            //} else {
            //    // 사용자는 소속된 정보만 조회
            //    String query = "SELECT * " +
            //            "FROM lms_center_users " +
            //            "WHERE user_id = :userId AND domain_id = :domainId " +
            //            "ORDER BY lc_id;";
            //    Map<String, Object> params = ValueUtil.newMap("userId,domainId", user.getLogin(), user.getDomainId());
            //    result = queryManager.selectListBySql(query, params, LmsCenterUsers.class, 0, 0);
            //}
            //
            //if (CollectionUtils.isEmpty(result)) return new ArrayList<>();
            //return result;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 사용자 소속 센터목록 필터링
     * - admin: 전체 센터 조회
     * - 작업자: 소속 센터 조회
     * @param page
     */
    //@Transactional(readOnly = true)
    //public void pageByUser(Page<?> page) {
    //    List<LmsAlarmStatusDev> cuList = lmsCenterUserService.lmsCenterUserListByUser();
    //    if (CollectionUtils.isEmpty(cuList)) {
    //        page.setList(new ArrayList<>());
    //        return;
    //    }
    //
    //    if (User.isCurrentUserAdmin()) {
    //        return;
    //    }
    //
    //    Set<String> allowedLcIdSet = cuList.stream().map(LmsCenterUsers::getLcId).collect(Collectors.toSet());
    //
    //    @SuppressWarnings("unchecked")
    //    Page<LmsCenters> typedPage = (Page<LmsCenters>) page;
    //
    //    List<LmsCenters> centers = typedPage.getList();
    //    if (!CollectionUtils.isEmpty(centers) && !CollectionUtils.isEmpty(allowedLcIdSet)) {
    //        List<LmsCenters> newCenters = centers.stream().filter(center -> allowedLcIdSet.contains(center.getLcId())).map(this::descryptAll).toList();
    //        typedPage.setList(newCenters);
    //        typedPage.setTotalSize(newCenters.size());
    //    }
    //}
}
