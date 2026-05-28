package operato.logis.lms.rest.support;

import operato.logis.lms.dto.support.SupportResponseDto;
import operato.logis.lms.entity.support.LmsSupportResponse;
import operato.logis.lms.service.impl.support.LmsSupportResponseService;
import operato.logis.lms.service.impl.support.MailService;
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
import xyz.elidom.sys.system.service.AbstractRestService;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/support-response")
@ServiceDesc(description = "SupportRequest Service API")
public class LmsSupportResponseController extends AbstractRestService {

    private final Logger logger = LoggerFactory.getLogger(LmsSupportResponseController.class);

    private LmsSupportResponseService lmsSupportResponseService;
    private MailService mailService;

    public LmsSupportResponseController(LmsSupportResponseService lmsSupportResponseService, MailService mailService) {
        this.lmsSupportResponseService = lmsSupportResponseService;
        this.mailService = mailService;
    }

    @Override
    protected Class<?> entityClass() {
        return LmsSupportResponse.class;
    }

    /**
     * 유지보수 답변 조회
     *
     * @param page
     * @param limit
     * @param select
     * @param sort
     * @param query
     * @return
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Search (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "select", required = false) String select,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "query", required = false) String query) {
        Page<?> data = this.search(this.entityClass(), page, limit, select, sort, query);
        lmsSupportResponseService.decryptPage(data);
        return data;
    }

    /**
     * 유지보수 답변 단일 건 생성/수정/삭제
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/update_one", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Create, Update or Delete SupportResponse data")
    @LmsUserActivityLog(description = "SupportRes - Create/Update/Delete support-response data", maskFields = {"creator_nm", "email"})
    public Boolean supResponseUpdateSub(@RequestBody SupportResponseDto request) {
        LmsSupportResponse sr = request.getSr();
        Boolean needSentMail = request.getNeedSentMail();

        logger.info("Sent mail option = {}", needSentMail);
        if (Boolean.TRUE.equals(needSentMail)) {
            this.mailService.sendMail(sr);
        } else {
            logger.info("Do not sent mail. needSentMail = {}", needSentMail);
        }

        return lmsSupportResponseService.cudOne(sr);
    }
}