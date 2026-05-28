package operato.logis.lms.service.impl.support;

import net.sf.common.util.ValueUtils;
import operato.logis.lms.LmsConstants;
import operato.logis.lms.dto.support.SupportFileUploadDto;
import operato.logis.lms.entity.support.LmsSupportAttachment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class LmsSupportAttachmentService extends AbstractQueryService {

    private static final List<String> ALLOWED_EXTENSIONS = Collections.unmodifiableList(Arrays.asList("jpg", "jpeg", "png", "gif", "pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx"));

    private static final String FILE_FULL_PATH = System.getProperty("user.dir") + LmsConstants.SUPPORT_REQUEST_FILE_BASE_PATH + LmsConstants.INTERNAL_FILE_PATH;

    private final Logger logger = LoggerFactory.getLogger(LmsSupportAttachmentService.class);

    /**
     * 유지보수 요청/답변 별 파일목록 조회
     *
     * @param refId 유지보수 요청/답변 ID (support_id, res_id)
     * @return 해당 요청/답변의 파일목록
     */
    public List<LmsSupportAttachment> getFileByRefId(String refId) {
        List<LmsSupportAttachment> files = this.queryManager.selectList(LmsSupportAttachment.class, ValueUtil.newMap("refId", refId));
        if (CollectionUtils.isEmpty(files)) {
            return new ArrayList<>();
        }
        return files;
    }

    /**
     * 유지보수 요청 파일을 특정 폴더에 저장
     * - 기존 첨부 파일 삭제
     * - 파일 저장위치가 없으면 하위 디렉토리 포함하여 생성
     *
     * @param fileInfo 원본 파일
     * @param refId    유지보수 요청/답변 id (support_id 또는 res_id)
     * @return 유지보수 파일 엔티티
     * @throws IOException
     */
    public LmsSupportAttachment processFile(SupportFileUploadDto.FileInfo fileInfo, String refId) throws IOException {
        if (ValueUtils.isEmpty(fileInfo)) {
            logger.info("[processFile] fileInfo is empty");
            return null;
        }

        String originFileName = fileInfo.getFileName();
        if (ValueUtil.isEmpty(originFileName) || !originFileName.contains(".")) {
            logger.info("[processFile] Invalid origin File Name. Check format. {}", originFileName);
            return null;
        }

        String extension = originFileName.substring(originFileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            logger.error("Invalid extension ", extension);
            throw new ElidomClientException(HttpStatus.SC_BAD_REQUEST, "Invalid extension " + extension, null, null);
        }

        String storedFileName = UUID.randomUUID() + "." + extension;
        Path dirPath = Paths.get(FILE_FULL_PATH);
        Path filePath = dirPath.resolve(storedFileName);

        Files.createDirectories(dirPath);

        try {
            byte[] fileBytes = Base64.getDecoder().decode(fileInfo.getBase64Data());
            Files.write(filePath, fileBytes);
        } catch (IllegalArgumentException e) {
            logger.error("LmsSupportRequestDebug Base64 디코딩 실패. 데이터 형식을 확인하세요.", e);
            throw new ElidomRuntimeException("Base64 디코딩 실패. 데이터 형식을 확인하세요.", e);
        }

        return new LmsSupportAttachment(originFileName, storedFileName, extension, filePath.toString(), fileInfo.getSize(), refId);
    }

    /**
     * 유지보수 요청 파일 지정 삭제
     * - 물리 삭제 후, DB 삭제
     *
     * @param deletedFileIds
     * @param refId
     * @param cudFlag
     */
    public void deletePartialFiles(List<String> deletedFileIds, String refId, String cudFlag) {
        if (ValueUtils.isEmpty(refId)) {
            logger.info("삭제할 파일 데이터가 없습니다. refId = {}", refId);
            return;
        }

        if (ValueUtils.isEmpty(cudFlag)) {
            logger.info("cudFlag가 없습니다. cudFlag = {}", cudFlag);
            throw new ElidomClientException(HttpStatus.SC_BAD_REQUEST, "cudFlag가 없습니다.", null, null);
        }

        if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_UPDATE) || ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_DELETE)) {

            List<LmsSupportAttachment> targetFiles = new ArrayList<>();
            Query condition = OrmUtil.newConditionForExecution();

            if (ValueUtil.isNotEmpty(deletedFileIds)) {
                condition.addFilter("refId", refId);
                condition.addFilter("id", OrmConstants.IN, deletedFileIds);
                targetFiles = this.queryManager.selectList(LmsSupportAttachment.class, condition);
            }

            if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_DELETE)) {
                condition = OrmUtil.newConditionForExecution();
                condition.addFilter("refId", refId);
                targetFiles = this.queryManager.selectList(LmsSupportAttachment.class, condition);
            }

            if (CollectionUtils.isEmpty(targetFiles)) {
                logger.info("삭제할 파일 데이터가 없습니다. deletedFileIds = {} / refId = {}", deletedFileIds, refId);
                return;
            }

            for (LmsSupportAttachment file : targetFiles) {
                try {
                    Path filePath = Paths.get(file.getFilePath());
                    boolean isDeleted = Files.deleteIfExists(filePath);

                    if (isDeleted) {
                        logger.info("물리파일 삭제 성공. filePath = {}", file.getFilePath());
                    } else {
                        logger.info("물리파일 삭제 불가. 파일이 존재하지 않습니다. filePath = {}", file.getFilePath());
                    }
                } catch (IOException e) {
                    // 파일 사용중이거나, 권한 문제
                    logger.error("물리파일 삭제 불가. filePath = {}", file.getFilePath(), e);
                }
            }

            this.queryManager.deleteBatch(targetFiles);
            logger.info("첨부파일 삭제 완료. 삭제 건수 = {}, refId = {}, id List = {}", targetFiles.size(), refId, deletedFileIds);
        }
    }

    // 참조id 기반 첨부파일 데이터 삭제
    public void deleteExistFileByRefId(String refId) {
        String selectSql = "SELECT * FROM lms_support_attatchment WHERE ref_id = :refId;";
        List<LmsSupportAttachment> existFiles = this.queryManager.selectListBySql(selectSql, ValueUtil.newMap("refId", refId), LmsSupportAttachment.class, 0, 0);
        this.queryManager.deleteBatch(existFiles);

        String query = "DELETE FROM lms_support_attatchment WHERE ref_id = :refId;";
        this.queryManager.executeBySql(query, ValueUtil.newMap("refId", refId));
    }

    /**
     * 유지보수 요청 파일 생성/수정/삭제
     * - 수정 : 기존 파일 삭제 후 생성
     *
     * @param srfiles 원본 파일 목록
     * @param refId   유지보수 요청/답변 id (support_id 또는 res_id)
     * @param cudFlag c: 생성, u: 수정, d: 삭제
     * @throws IOException
     */
    public void updateSupportRequestFiles(List<SupportFileUploadDto.FileInfo> srfiles, String refId, String cudFlag) throws IOException {
        if (CollectionUtils.isEmpty(srfiles) || ValueUtils.isEmpty(refId)) {
            logger.info("생성/수정/삭제할 데이터가 없습니다. files = {}, refId = {}", srfiles, refId);
            return;
        }

        if (ValueUtils.isEmpty(cudFlag)) {
            logger.info("cudFlag가 없습니다. cudFlag = {}", cudFlag);
            throw new ElidomClientException(HttpStatus.SC_BAD_REQUEST, "cudFlag가 없습니다.", null, null);
        }

        logger.info("생성/수정/삭제 대상 파일 {}건", srfiles.size());

        List<LmsSupportAttachment> saList = new ArrayList<>();

        for (SupportFileUploadDto.FileInfo fileInfo : srfiles) {

            if (ValueUtil.isNotEmpty(fileInfo.getBase64Data())) {
                LmsSupportAttachment newFileInfo = processFile(fileInfo, refId);

                if (newFileInfo == null) {
                    logger.info("File processing failed for ref_id = {}", refId);
                    continue;
                }
                newFileInfo.setCudFlag_(cudFlag);
                saList.add(newFileInfo);
            }
        }
        logger.info("생성/수정/삭제 대상 파일 {}건", saList.size());

        if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_CREATE)) {
            this.queryManager.insertBatch(saList);
        } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_UPDATE)) {
            this.queryManager.insertBatch(saList);
        } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_DELETE)) {
            deleteExistFileByRefId(refId);
        }
        logger.info("첨부파일 생성/수정/삭제 완료, refId = {}, {}건", refId, saList.size());
    }
}