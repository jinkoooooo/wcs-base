package operato.logis.samsung.cognex.service;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class DownloadImages {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // 경로 및 계정 설정
    private static final String REJECT_IMAGE_BASE_PATH = "\\\\192.168.100.151\\Upload";
    private static final String LOCAL_ROOT_PATH = "D:/cognex";

    // 계정 정보 (Windows 로그인 정보)
    private static final String SMB_USER = "admin";
    private static final String SMB_PASS = "";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Scheduled(cron = "0 59 23 * * *")
    public void executeTask() {
        logger.info("=== 이미지 동기화 및 정리 작업 시작 ===");
        downloadTodayImages();
        deleteOldFolders();
        logger.info("=== 작업 종료 ===");
    }

    private void downloadTodayImages() {
        String todayDateStr = LocalDate.now().format(DATE_FORMATTER);
        String targetUrl = convertToSmbUrl(REJECT_IMAGE_BASE_PATH, todayDateStr);

        try {
            // 1. 기본 Context 가져오기
            CIFSContext baseContext = SingletonContext.getInstance();

            NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(null, SMB_USER, SMB_PASS);

            // 2. 인증이 적용된 Context 생성
            CIFSContext context = baseContext.withCredentials(auth);

            // 3. 연결 시도
            try (SmbFile remoteDir = new SmbFile(targetUrl, context)) {
                if (!remoteDir.exists()) {
                    logger.warn("서버에 오늘자 폴더가 없습니다: {}", targetUrl);
                    return;
                }

                File localDateDir = new File(LOCAL_ROOT_PATH, todayDateStr);
                if (!localDateDir.exists()) {
                    localDateDir.mkdirs();
                }

                SmbFile[] files = remoteDir.listFiles();
                int count = 0;
                if (files != null) {
                    for (SmbFile file : files) {
                        if (file.isFile()) {
                            saveFileToLocal(file, localDateDir);
                            count++;
                        }
                    }
                }
                logger.info("총 {}개 파일 다운로드 완료", count);
            }
        } catch (Exception e) {
            logger.error("이미지 다운로드 실패: {}", e.getMessage());
        }
    }

    private void deleteOldFolders() {
        File rootDir = new File(LOCAL_ROOT_PATH);
        File[] folders = rootDir.listFiles();

        if (folders == null) return;

        LocalDate thresholdDate = LocalDate.now().minusDays(30);

        for (File folder : folders) {
            if (folder.isDirectory()) {
                try {
                    LocalDate folderDate = LocalDate.parse(folder.getName(), DATE_FORMATTER);
                    if (folderDate.isBefore(thresholdDate)) {
                        logger.info("30일 경과 데이터 삭제: {}", folder.getName());
                        FileSystemUtils.deleteRecursively(folder);
                    }
                } catch (DateTimeParseException e) {
                    // 날짜 형식이 아니면 무시
                }
            }
        }
    }

    private void saveFileToLocal(SmbFile smbFile, File localDir) {
        String fileName = smbFile.getName();
        File destFile = new File(localDir, fileName);

        if (destFile.exists()) return;

        try (InputStream in = new SmbFileInputStream(smbFile);
             OutputStream out = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            logger.error("파일 저장 에러: {}", fileName, e);
        }
    }

    private String convertToSmbUrl(String windowsPath, String subFolder) {
        String cleanPath = windowsPath.replaceAll("^\\\\+", "").replace("\\", "/");
        StringBuilder sb = new StringBuilder("smb://");
        sb.append(cleanPath);
        if (!cleanPath.endsWith("/")) sb.append("/");
        sb.append(subFolder).append("/");
        return sb.toString();
    }
}