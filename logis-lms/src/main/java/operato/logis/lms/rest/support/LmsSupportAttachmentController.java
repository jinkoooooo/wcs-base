package operato.logis.lms.rest.support;

import operato.logis.lms.LmsConstants;
import operato.logis.lms.entity.support.LmsSupportAttachment;
import operato.logis.lms.service.impl.support.LmsSupportAttachmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.task.ServiceLogRemoveTask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/support-attachment")
@ServiceDesc(description = "Support Attachment Controller API")
public class LmsSupportAttachmentController extends AbstractRestService {

    private final ServiceLogRemoveTask serviceLogRemoveTask;
    private LmsSupportAttachmentService lmsSupportAttachmentService;

    private final Logger logger = LoggerFactory.getLogger(LmsSupportAttachmentController.class);

    @Override
    protected Class<?> entityClass() {
        return LmsSupportAttachment.class;
    }

    public LmsSupportAttachmentController(LmsSupportAttachmentService lmsSupportAttachmentService, ServiceLogRemoveTask serviceLogRemoveTask) {
        this.lmsSupportAttachmentService = lmsSupportAttachmentService;
        this.serviceLogRemoveTask = serviceLogRemoveTask;
    }

    /**
     * 유지보수 요청/답변 건의 첨부파일 목록 조회
     *
     * @param id 유지보수 요청/답변 id (support_id 또는 res_id)
     * @return 첨부파일 목록
     */
    @GetMapping(value = "/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Find SupportAttachments By refId")
    public List<LmsSupportAttachment> findOne(@PathVariable("id") String id) {
        return this.lmsSupportAttachmentService.getFileByRefId(id);
    }

    /**
     * 파일 다운로드
     *
     * @param id 파일 고유식별자 (lms_support_attachment의 id)
     * @return Blob 데이터
     * @throws IOException
     */
    @RequestMapping(value = "/download/{id}", method = RequestMethod.GET)
    @ApiDesc(description = "Download Support attchment")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") String id) throws IOException {
        logger.info("[downloadFile] Find file. id = {}", id);
        LmsSupportAttachment entity = this.getOne(true, this.entityClass(), id);

        String storedName = entity == null ? "test.png" : entity.getStoredFileName();
        String originName = entity == null ? null : entity.getOriginFileName();
        String encodedName = UriUtils.encode(originName, StandardCharsets.UTF_8);
        String internalPath = LmsConstants.INTERNAL_FILE_PATH + "/" + storedName; // match path with nginx setting
        logger.info("[downloadFile] originName = {}, storedName = {}, path = {}", originName, storedName, internalPath);

        return ResponseEntity.ok()
                .header("X-Accel-Redirect", internalPath)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .build();
    }
}