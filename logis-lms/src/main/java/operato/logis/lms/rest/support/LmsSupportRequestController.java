package operato.logis.lms.rest.support;

import operato.logis.lms.dto.support.SupportFileUploadDto;
import operato.logis.lms.entity.support.LmsSupportRequest;
import operato.logis.lms.service.impl.support.LmsSupportAttachmentService;
import operato.logis.lms.service.impl.support.LmsSupportRequestService;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/support-request")
@ServiceDesc(description = "SupportRequest Service API")
public class LmsSupportRequestController extends AbstractRestService {

    private LmsSupportRequestService lmsSupportRequestService;
    private LmsSupportAttachmentService attachmentService;

    public LmsSupportRequestController(LmsSupportRequestService lmsSupportRequestService, LmsSupportAttachmentService attachmentService) {
        this.lmsSupportRequestService = lmsSupportRequestService;
        this.attachmentService = attachmentService;
    }

    @Override
    protected Class<?> entityClass() {
        return LmsSupportRequest.class;
    }

    /**
     * 유지보수 요청 조회
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
        lmsSupportRequestService.decryptRequestPage(data);
        return data;
    }

    @GetMapping(value = "/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Find SupportRequest By ID")
    public LmsSupportRequest findOne(@PathVariable("id") String id) {
        LmsSupportRequest data = this.getOne(true, this.entityClass(), id);
        if (data == null) {
            return new LmsSupportRequest();
        }
        lmsSupportRequestService.decryptRequest(data);
        return data;
    }

    /**
     * 유지보수 요청 단일 건 생성/수정/삭제
     * * cudMultipleData에서는 데이터가 있는 컬럼 -> null로 변경 불가
     * * equip_id, alarm_id 수정 가능
     *
     * @param data
     * @return
     */
    @PostMapping(value = "/update_one", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Create, Update or Delete SupportRequest data")
    @LmsUserActivityLog(description = "Support - Create/Update/Delete support request data", maskFields = {"assignee_id"})
    public Boolean supRequestUpdate(@RequestBody SupportFileUploadDto data) {
        return this.lmsSupportRequestService.cudSupportRequestWithFiles(data);
    }

    /**
     * 유지보수 요청목록 생성/수정/삭제
     *
     * @param list
     * @return
     */
    @PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Create, Update or Delete multiple at one time")
    @LmsUserActivityLog(description = "Support - Create/Update/Delete support request data list", maskFields = {"assignee_id", "email"})
    public Boolean multipleUpdate(@RequestBody List<LmsSupportRequest> list) {
        try {
            list = this.lmsSupportRequestService.validateAndSetRequests(list);

            for (LmsSupportRequest sr : list) {
                this.attachmentService.deletePartialFiles(new ArrayList<>(), sr.getSupportId(), sr.getCudFlag_());
            }
        } catch (Exception e) {
            throw (e);
        }
        return this.cudMultipleData(this.entityClass(), list); // null 값 제외되므로 설비코드, 알람코드 삭제 불가
    }
}