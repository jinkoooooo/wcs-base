package operato.logis.wcs.rest.file;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.TbWcsFileAttachment;
import operato.logis.wcs.service.impl.file.FileAttachmentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 공통 파일 첨부 REST.
 *   POST /rest/wcs/file-attachment            업로드 (신규)
 *   POST /rest/wcs/file-attachment/{id}       업로드 (교체)
 *   GET  /rest/wcs/file-attachment/{id}       다운로드
 *   GET  /rest/wcs/file-attachment/{id}/meta  메타 조회
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/file-attachment")
@ServiceDesc(description = "공통 파일 첨부 API")
public class FileAttachmentController {

    private final FileAttachmentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "파일 업로드 (신규)")
    public Map<String, Object> upload(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam("file") MultipartFile file) {
        if (ValueUtil.isEmpty(file) || file.isEmpty()) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "file 이 비어있습니다.");
        }
        try (InputStream in = file.getInputStream()) {
            TbWcsFileAttachment saved = service.upload(category,
                    file.getOriginalFilename(), file.getContentType(), in, file.getSize());
            return toResponse(saved);
        } catch (IOException e) {
            throw new ElidomRuntimeException(WcsError.INTERNAL_ERROR.codeAsString(), "업로드 실패", e);
        }
    }

    @PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "파일 교체")
    public Map<String, Object> replace(@PathVariable("id") String id,
                                       @RequestParam("file") MultipartFile file) {
        if (ValueUtil.isEmpty(file) || file.isEmpty()) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "file 이 비어있습니다.");
        }
        try (InputStream in = file.getInputStream()) {
            TbWcsFileAttachment saved = service.replace(id,
                    file.getOriginalFilename(), file.getContentType(), in, file.getSize());
            return toResponse(saved);
        } catch (IOException e) {
            throw new ElidomRuntimeException(WcsError.INTERNAL_ERROR.codeAsString(), "교체 실패", e);
        }
    }

    @GetMapping(value = "/{id}/meta", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "메타 조회")
    public Map<String, Object> meta(@PathVariable("id") String id) {
        TbWcsFileAttachment e = service.findById(id);
        if (ValueUtil.isEmpty(e)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "file_attachment 없음. id=" + id);
        }
        return toResponse(e);
    }

    @GetMapping("/{id}")
    @ApiDesc(description = "파일 다운로드")
    public ResponseEntity<InputStreamResource> download(@PathVariable("id") String id) {
        TbWcsFileAttachment e = service.findById(id);
        if (ValueUtil.isEmpty(e) || ValueUtil.isEmpty(e.getFilePath())) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "파일 없음. id=" + id);
        }
        // 메타의 file_path 가 실제 디스크에 없으면 부재 처리
        Path path = Paths.get(e.getFilePath());
        if (!Files.exists(path)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "파일 부재. path=" + path);
        }
        try {
            // 한글 파일명은 RFC 5987 filename* 로 인코딩
            String filename = ValueUtil.isNotEmpty(e.getFileName()) ? e.getFileName() : "file";
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    ValueUtil.isNotEmpty(e.getContentType()) ? e.getContentType() : "application/octet-stream"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
            headers.setContentLength(Files.size(path));
            // 파일 스트림을 그대로 흘려보냄 (메모리 적재 회피)
            return new ResponseEntity<>(new InputStreamResource(new FileInputStream(path.toFile())),
                    headers, HttpStatus.OK);
        } catch (IOException ioe) {
            throw new ElidomRuntimeException(WcsError.INTERNAL_ERROR.codeAsString(), "다운로드 실패", ioe);
        }
    }

    private Map<String, Object> toResponse(TbWcsFileAttachment e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("category", e.getCategory());
        m.put("file_name", e.getFileName());
        m.put("file_size", e.getFileSize());
        m.put("content_type", e.getContentType());
        m.put("download_url", "/rest/wcs/file-attachment/" + e.getId());
        return m;
    }
}