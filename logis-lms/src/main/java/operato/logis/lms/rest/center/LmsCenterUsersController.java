package operato.logis.lms.rest.center;

import operato.logis.lms.entity.center.LmsCenterUsers;
import operato.logis.lms.service.impl.center.LmsCenterUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.annotation.LmsUserActivityLog;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SysValueUtil;

import java.util.List;
import java.util.Map;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/centers_users")
@ServiceDesc(description = "Role Service API")
public class LmsCenterUsersController extends AbstractRestService {

    /**
     * userId Field Name - userId
     */
    private static final String FIELD_USER_ID = "userId";
    /**
     * lcId Field Name - lcId
     */
    private static final String FIELD_LC_ID = "lcId";
    /**
     * Users of center SQL
     */
    private static final String USERS_OF_CENTER_SQL = "select id, user_id, lc_id from lms_center_users where lc_id = :lcId;";

    /**
     * Service
     */
    private final LmsCenterUserService lmsCenterUserService;

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(LmsCenterUsers.class);

    public LmsCenterUsersController(LmsCenterUserService lmsCenterUserService) {
        this.lmsCenterUserService = lmsCenterUserService;
    }

    @Override
    protected Class<?> entityClass() {
        return LmsCenterUsers.class;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Search User Center Relations (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "select", required = false) String select,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "query", required = false) String query) {
        return this.search(this.entityClass(), page, limit, null, sort, query);
    }

    /**
     * 로그인 사용자의 센터코드 목록 반환
     *
     * @param page
     * @param limit
     * @param select
     * @param sort
     * @param query
     * @return
     */
    @GetMapping(value = "/current_user", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Get the current user's primary center")
    public Page<?> searchCenters(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "select", required = false) String select,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "query", required = false) String query) {
        User user = User.currentUser();
        if (SysValueUtil.isEmpty(user)) {
            return new Page<>();
        }
        String userId = user.getId();

        if (sort == null || sort.isBlank()) {
            sort = "[{\"field\":\"lc_id\",\"ascending\":false}]";
        }
        String userFilter = String.format(
                "{\"name\":\"user_id\",\"operator\":\"eq\",\"value\":%s,\"relation\":false}", userId
        );

        if (query == null || query.isBlank()) {
            query = "[" + userFilter + "]";
        } else {
            String trimmed = query.trim();
            if (trimmed.endsWith("]")) {
                // 배열 끝에 필터 추가
                query = trimmed.substring(0, trimmed.length() - 1) + "," + userFilter + "]";
            } else {
                // 배열이 아닌 경우 배열로 감싸기
                query = "[" + trimmed + "," + userFilter + "]";
            }
        }
        return this.search(this.entityClass(), page, limit, null, sort, query);
    }

    @GetMapping(value = "/{id}/center_users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Find users by lcId")
    public List<LmsCenterUsers> centerUsers(@PathVariable("id") String id) {
        Map<String, Object> paramMap = SysValueUtil.newMap(FIELD_LC_ID, id);
        return this.queryManager.selectListBySql(USERS_OF_CENTER_SQL, paramMap, LmsCenterUsers.class, 0, 0);
    }

    @PostMapping(value = "/{lcId}/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Update userCenter relation")
    @LmsUserActivityLog(description = "CenterUser - Update center-user mapping")
    public Boolean updateCenterUsers(@PathVariable("lcId") String lcId, @RequestBody List<LmsCenterUsers> lmsCenterUserList) {
        return this.cudMultipleData(this.entityClass(), lmsCenterUserList);
    }

    /**
     * 현재 유저의 소속 센터 목록 조회
     * - 관리자: 전체 센터 조회 또는 소속센터 조회 (useFilterToAdmin = true)
     * - 작업자: 소속 센터 조회
     * @param useFilter 관리자 계정일 때 소속센터 필터링 사용 여부
     * @return lc_id, lc_nm 목록
     */
    @GetMapping(value = "/current_user/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Get the user's center info")
    public Map<String, Object> searchCenterUserAndCenter(
            @RequestParam(name="useFilter", required = false) Boolean useFilter) {
        boolean relationFilter = Boolean.TRUE.equals(useFilter);
        return this.lmsCenterUserService.lmsCenterListByUser(relationFilter);
    }
}