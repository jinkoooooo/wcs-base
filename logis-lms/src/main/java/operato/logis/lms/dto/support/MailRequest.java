package operato.logis.lms.dto.support;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailRequest {
    // 공통
    private List<String> toRecipients;  // 수신인
    private String subject;             // 메일 제목
    private String body;                // 메일 본문 html
    private Boolean isExternApi;        // 외부메일API 사용 여부 / false: 내부 메일라이브러리 사용

    // 외부 메일 API only
    private String apiKey;              // 메일 API 키
    private String senderEmailAddress;  // 발신인
    private List<String> ccRecipients;  // 참조
    private List<String> bccRecipients; // 숨은참조
    private List<String> attachments;   // 첨부파일 UNC 경로 / 첨부파일은 attachments 필드에 경로 입력 혹은 POST data에 파일 직접 추가하는 방식 중 선택
    private Boolean isImportant;        // 중요메일 여부

    // 내부 라이브러리 only
    private Map<String, Object> templateParams; // 메일 템플릿에 작성할 파라미터
}
