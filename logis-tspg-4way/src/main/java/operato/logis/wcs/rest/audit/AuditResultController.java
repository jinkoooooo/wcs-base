package operato.logis.wcs.rest.audit;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.service.audit.AuditReportPdfRenderer;
import operato.logis.wcs.service.impl.query.audit.AuditLogResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * 감사 이력 조회 REST Controller
 * - Elidom 표준 CRUD와 동일한 query/sort/page/limit 파라미터 구조 사용
 * - query: JSON 필터 배열 [{"name":"actor_type","operator":"eq","value":"USER"}, ...]
 * - sort:  JSON 정렬 배열 [{"field":"created_at","ascending":false}, ...]
 * - PDF: 동일 필터로 보고서 출력
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/audit/result")
@ServiceDesc(description = "AuditResult Service API")
public class AuditResultController {

    private static final Logger logger = LoggerFactory.getLogger(AuditResultController.class);

    private final AuditLogResultService auditLogResultService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "감사 이력 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Audit ][ Result ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return auditLogResultService.search(query, sort, page, limit);
    }

    @GetMapping(value = "/report/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @ApiDesc(description = "감사 이력 PDF 보고서 출력")
    public ResponseEntity<byte[]> reportPdf(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "inline", defaultValue = "true") boolean inline) {

        logger.debug("[ Audit ][ Result ] report start - query={}, sort={}", query, sort);
        List<Map<String, Object>> rows = auditLogResultService.listForReport(query, sort);
        byte[] pdf = AuditReportPdfRenderer.render(rows);
        logger.info("[ Audit ][ Result ] report completed - rows={}", rows.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                (inline ? "inline" : "attachment") + "; filename=\"audit-report.pdf\"");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
