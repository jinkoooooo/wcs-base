package operato.logis.lms.rest.center;

import operato.logis.lms.entity.center.LmsCenters;
import operato.logis.lms.service.impl.center.LmsCenterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.ArrayList;
import java.util.List;


@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/lms-centers")
@ServiceDesc(description = "LmsCenters Service API")
public class LmsCentersController extends AbstractRestService {

    private static final Logger logger = LoggerFactory.getLogger(LmsCentersController.class);

    @Override
    protected Class<?> entityClass() {
        return LmsCenters.class;
    }

    private final LmsCenterService lmsCenterService;

    public LmsCentersController(LmsCenterService lmsCenterService) {
        this.lmsCenterService = lmsCenterService;
    }

    /**
     * 유저ID, 권한 기반 센터정보 조회
     * - admin: 전체 센터 조회
     * - 작업자: 소속된 센터 조회
     *
     * @param page
     * @param limit
     * @param select
     * @param sort
     * @param query
     * @return
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Search (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "select", required = false) String select,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "query", required = false) String query) throws Exception {
        Page<?> initResult = this.search(this.entityClass(), page, limit, null, sort, query);
        // 소속된 센터 검사
        lmsCenterService.pageByUser(initResult);
        return initResult;
    }

    /**
     * 유저ID, 소속 기반 센터정보 조회 (POST Method)
     * - admin: 소속된 센터 조회
     * - 작업자: 소속된 센터 조회
     */
    @PostMapping(value = "/id", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Find LmsCenter List by userId")
    public List<LmsCenters> getCenterListByUserId() {
        try {
            return lmsCenterService.lmsCenterListByUser(true);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 센터 정보 업데이트
     */
    // TODO: 암호화 데이터 제외한 로그 저장
    @RequestMapping(value = "/update_multiple", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<LmsCenters> list) {
        // 주소, 엣지서버연결정보 암호화
        lmsCenterService.encryptList(list);
        return this.cudMultipleData(this.entityClass(), list);
    }
}
