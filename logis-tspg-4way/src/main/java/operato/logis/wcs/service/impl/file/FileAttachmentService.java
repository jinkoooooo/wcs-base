package operato.logis.wcs.service.impl.file;

import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.TbWcsFileAttachment;
import operato.logis.wcs.service.repository.FileAttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 모든 화면이 공통으로 쓰는 파일 첨부 서비스.
 *
 * 업로드/다운로드/교체/삭제 단일 진입점. 화면별 분기 없음.
 * 호출 측이 category 로 도메인을 구분한다.
 */
@Service
public class FileAttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(FileAttachmentService.class);
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FileAttachmentRepository repository;
    private final String storageRoot;

    public FileAttachmentService(FileAttachmentRepository repository,
                                 @Value("${wcs.file.storage.root:#{systemProperties['user.home']}/wcs-uploads}") String storageRoot) {
        this.repository = repository;
        this.storageRoot = storageRoot;
    }

    /**
     * 첨부 ID 로 조회.
     */
    @Transactional(readOnly = true)
    public TbWcsFileAttachment findById(String id) {
        return repository.findById(id);
    }

    /**
     * 새 첨부 업로드. UUID 를 선생성해 file_path 를 INSERT 시점에 확정한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsFileAttachment upload(String category, String originalFilename,
                                      String contentType, InputStream content, long size) {
        // 입력 검증
        if (ValueUtil.isEmpty(content) || size <= 0) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "업로드 파일이 비어있습니다.");
        }

        // UUID 선생성 — file_path 가 NOT NULL 이고 경로 구성에 id 가 들어가서 INSERT 전에 확정 필요
        TbWcsFileAttachment e = new TbWcsFileAttachment();
        e.setId(UUID.randomUUID().toString());
        e.setCategory(ValueUtil.isEmpty(category) ? "default" : category);
        String safeName = sanitize(originalFilename);
        e.setFileName(safeName);
        e.setFileSize(size);
        e.setContentType(contentType);

        // 디스크 저장
        Path target = resolvePath(e);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            logger.error("[ File ][ Attachment ] save failed - category={}, name={}", e.getCategory(), safeName, ioe);
            throw new ElidomRuntimeException(WcsError.INTERNAL_ERROR.codeAsString(),
                    "파일 저장 실패: " + ioe.getMessage());
        }

        // DB INSERT
        e.setFilePath(target.toAbsolutePath().toString());
        repository.insert(e);

        logger.info("[ File ][ Attachment ] upload completed - id={}, category={}, name={}, size={}, path={}",
                e.getId(), e.getCategory(), safeName, size, target);
        return e;
    }

    /**
     * 기존 첨부 교체. id 는 유지하고 디스크 파일과 메타데이터만 갈아치운다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsFileAttachment replace(String id, String originalFilename,
                                       String contentType, InputStream content, long size) {
        TbWcsFileAttachment e = requireById(id);
        String safeName = sanitize(originalFilename);
        e.setFileName(safeName);
        e.setFileSize(size);
        e.setContentType(contentType);

        // 새 파일 덮어쓰기
        Path target = resolvePath(e);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            logger.error("[ File ][ Attachment ] replace failed - id={}, name={}", id, safeName, ioe);
            throw new ElidomRuntimeException(WcsError.INTERNAL_ERROR.codeAsString(),
                    "파일 저장 실패: " + ioe.getMessage());
        }

        // DB UPDATE
        e.setFilePath(target.toAbsolutePath().toString());
        repository.update(e, "fileName", "fileSize", "contentType", "filePath");
        return e;
    }

    /**
     * id 로 조회 후 없으면 예외.
     */
    private TbWcsFileAttachment requireById(String id) {
        TbWcsFileAttachment e = repository.findById(id);
        if (ValueUtil.isEmpty(e)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "file_attachment 없음. id=" + id);
        }
        return e;
    }

    /**
     * 저장 경로 계산 — {storageRoot}/{category}/{yyyyMMdd}/{id}_{filename}.
     */
    private Path resolvePath(TbWcsFileAttachment e) {
        String datePart = LocalDate.now().format(DATE_DIR);
        return Paths.get(storageRoot, e.getCategory(), datePart, e.getId() + "_" + e.getFileName());
    }

    /**
     * 파일명에서 OS·경로 메타문자를 underscore 로 치환. 200자 초과면 뒤쪽만 보존.
     */
    private String sanitize(String name) {
        if (ValueUtil.isEmpty(name)) return "file";
        String s = name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return s.length() > 200 ? s.substring(s.length() - 200) : s;
    }
}
