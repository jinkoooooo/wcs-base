package operato.logis.lms.dto.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SupportFileUploadDto {
    // 유지보수 요청 정보
    private String id;
    private String supportId;
    private String lcId;
    private String equipId;
    private String alarmId;
    private String category;
    private String title;
    private String content;
    private String status;
    private String requesterId;
    private String assigneeId;
    private String receiverId;
    private Boolean isDeleted;
    private Date receivedAt;
    private Date completedAt;

    // 상속 필드
    private Long domainId;
    private String creatorId;
    private String updaterId;
    private Date createdAt;
    private Date updatedAt;

    // Ignore 필드
    private String cudFlag_;

    // 유지보수 요청 첨부파일
    private List<FileInfo> files;

    // 유지보수 요청 첨부파일 삭제대상 id
    private List<String> deletedFileIds;

    public static class FileInfo {
        private String fileName;
        private String fileType;
        private String base64Data;
        private Long size;

        public String getFileName() { return fileName; }

        public String getFileType() { return fileType; }

        public String getBase64Data() { return base64Data; }

        public Long getSize() { return size; }

        public void setFileName(String fileName) { this.fileName = fileName; }

        public void setFileType(String fileType) { this.fileType = fileType; }

        public void setBase64Data(String base64Data) { this.base64Data = base64Data; }

        public void setSize(Long size) { this.size = size; }
    }
}