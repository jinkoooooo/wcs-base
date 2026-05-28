package operato.logis.lms.service.impl.support;

import lombok.RequiredArgsConstructor;
import net.sf.common.util.ValueUtils;
import operato.logis.connector.api.service.ExternalApiService;
import operato.logis.lms.LmsConstants;
import operato.logis.lms.dto.support.MailReceiverDto;
import operato.logis.lms.dto.support.MailRequest;
import operato.logis.lms.entity.center.LmsCenters;
import operato.logis.lms.entity.support.LmsSupportRequest;
import operato.logis.lms.entity.support.LmsSupportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.engine.ITemplateEngine;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.system.transport.sender.MailSender;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService extends AbstractQueryService {
    private final ExternalApiService externalApiService;
    private final MailSender mailSender;

    @Autowired
    @Qualifier("basic")
    private ITemplateEngine templateEngine;

    private static final String DEFAULT_MAIL_SENDER = "no-reply@info.logisall.com"; // PC지원센터 문의하여 ID(no-reply) 커스텀 가능

    private final Logger logger = LoggerFactory.getLogger(MailService.class);

    /**
     * 유지보수 신규 요청 시, 해당 센터 manager 모두에게 메일 발송
     *
     * @param data 유지보수 요청 데이터
     */
    public void sendRequestMail(LmsSupportRequest data) {
        logger.info("[sendMail] Initiating mail dispatch process.");
        try {
            logger.info("[sendMail] Requesting mail API. cud_flag_ = {}, support_id = {}", data.getCudFlag_(), data.getSupportId());

            if (data.getLcId() == null) {
                logger.info("Failed to send mail. lc_id is null");
                return;
            }

            MailRequest request = writeRequestMailContent(data);
            if (request == null) {
                logger.info("Failed to create mail content. support_id = {}", data.getSupportId());
                return;
            }

            for (int i = 0; i < request.getToRecipients().size(); i++) {
                // NOTE: 이메일 보내는 이 변경 -> from = new InternetAddress("no-reply@info.logisall.com", "로지스올시스템즈", "UTF-8");
                this.mailSender.send(request.getSubject(), DEFAULT_MAIL_SENDER, request.getToRecipients().get(i), request.getBody(), request.getTemplateParams(), ValueUtil.newMap(SysConstants.EMAIL_OPT_MIME_TYPE, SysConstants.EMAIL_MIME_TYPE_TEXT_HTML_UTF_8));
            }
            logger.info("Mail delivery success. support_id = {}", data.getSupportId());
        } catch (Exception e) {
            logger.info("Mail delivery failed. error = {}", e.getMessage(), e);
        }
    }

    // 메일 라이브러리로 메일 전송
    public void sendMail(LmsSupportResponse data) {
        logger.info("[sendMail] Initiating mail dispatch process.");
        try {
            logger.info("[sendMail] Requesting mail API. cud_flag_ = {}, res_id = {}, support_id = {}", data.getCudFlag_(), data.getResId(), data.getSupportId());

            MailRequest request = createMailBody(data, false);
            if (request == null) {
                logger.info("[sendMail] Aborted. Empty mail body. res_id = {}, support_id = {}", data.getResId(), data.getSupportId());
                return;
            }

            // NOTE: 이메일 보내는 이 변경 -> from = new InternetAddress("no-reply@info.logisall.com", "로지스올시스템즈", "UTF-8");
            this.mailSender.send(request.getSubject(), DEFAULT_MAIL_SENDER, request.getToRecipients().get(0), request.getBody(), request.getTemplateParams(), ValueUtil.newMap(SysConstants.EMAIL_OPT_MIME_TYPE, SysConstants.EMAIL_MIME_TYPE_TEXT_HTML_UTF_8));
            logger.info("[sendMail] Delivery success. res_id = {}", data.getResId());
        } catch (Exception e) {
            logger.info("[sendMail] Delivery failed. error = {}", e.getMessage(), e);
        }
    }

    // 외부/내부 메일 API 분리
    private MailRequest createMailBody(LmsSupportResponse data, Boolean isExternalApi) {
        MailRequest request = writeMailContent(data);
        if (request == null) {
            logger.info("[createMailBody] Failed to create mail content. res_id = {}, support_id = {}", data.getResId(), data.getSupportId());
            return null;
        }
        request.setIsExternApi(isExternalApi);
        logger.info("[createMailBody] isExternApi = {}", isExternalApi);
        return request;
    }

    // 메일 메타데이터 BODY 작성
    private MailRequest writeMailContent(LmsSupportResponse data) {
        String lcId = data.getLcId();
        if (ValueUtils.isEmpty(lcId)) {
            logger.info("[writeMailContent] Invalid Support Request data. lc_id is empty. res_id = {}, lc_id = {}", data.getResId(), lcId);
            return null;
        }

        String supportId = data.getSupportId();
        if (ValueUtils.isEmpty(supportId)) {
            logger.info("[writeMailContent] Invalid Support Response data. support_id is empty. res_id = {}", data.getResId());
            return null;
        }

        LmsSupportRequest sReq = this.queryManager.selectByCondition(LmsSupportRequest.class, ValueUtil.newMap("supportId", supportId));
        if (sReq == null) {
            logger.info("[writeMailContent] Not found Support Request data. support_id = {}", supportId);
            return null;
        }

        LmsCenters center = this.queryManager.selectByCondition(LmsCenters.class, ValueUtil.newMap("lcId", data.getLcId()));
        User author = this.queryManager.selectByCondition(User.class, ValueUtil.newMap("login", sReq.getRequesterId()));
        if (author == null) {
            logger.info("[writeMailContent] Recipient user not found.");
            return null;
        }
        if (ValueUtils.isEmpty(author.getEmail())) {
            logger.info("[writeMailContent] Recipient's email is empty. login = {}, email = {}", author.getLogin(), author.getEmail());
            return null;
        }

        User user = User.currentUser();
        List<String> bccRecipients = new ArrayList<>();
        if (user != null && ValueUtil.isNotEmpty(user.getEmail())) {
            bccRecipients.add(user.getEmail());
        }

        String title = "[시스템 알림] 유지보수 답변이 작성되었습니다.";
        String to = author.getEmail();
        logger.info("[writeMailContent] to email = {}", to);
        String sReqTitle = StringUtils.hasText(sReq.getTitle()) ? sReq.getTitle() : "-";
        String lcNm = center.getLcNm();
        String responseContent = StringUtils.hasText(data.getContent()) ? data.getContent() : "-";
        String link = SettingUtil.getValue(SysConfigConstants.CLIENT_CONTEXT_PATH);
        // 줄바꿈 변환 (\r\n, \n, \r → <br/>)
        String formattedContent = responseContent.trim().replaceAll("(\r\n|\n|\r)", "<br/>");

        Map<String, Object> templateParams = ValueUtil.newMap("supportId,supportTitle,lcId,lcNm,responseContent,link", supportId, sReqTitle, lcId, lcNm, formattedContent, link);
        String templatePath = LmsConstants.SUPPORT_RESPONSE_MAIL_TEMPLATE_PATH;
        String template = FileUtil.readClassPathResource(templatePath);
        StringWriter writer = new StringWriter();
        this.templateEngine.processTemplate(template, writer, templateParams, null);
        String filledContent = writer.toString();

        return MailRequest.builder()
                .apiKey(null)
                .senderEmailAddress(DEFAULT_MAIL_SENDER)
                .toRecipients(Collections.singletonList(to)) // NOTE: logis-connect의 Java 8 호환 (기존: List.of())
                .ccRecipients(new ArrayList<>())
                .bccRecipients(bccRecipients)
                .subject(title)
                .body(filledContent)
                .isImportant(false)
                .attachments(new ArrayList<>())
                .isExternApi(true)
                .templateParams(templateParams)
                .build();
    }

    /**
     * 유지보수 등록 시, 알림 메일 본문 작성
     * 1. 유지보수 요청 데이터 유효성 검증
     * 2. 해당 센터의 Manager(센터관리자) 조회
     * 3. 센터관리자로 메일 전송
     */
    private MailRequest writeRequestMailContent(LmsSupportRequest data) {
        String lcId = data.getLcId();
        String supportId = data.getSupportId();

        if (ValueUtils.isEmpty(supportId)) {
            logger.info("[writeMailContent] Invalid Support Response data. support_id is empty");
            return null;
        }

        if (ValueUtils.isEmpty(lcId)) {
            logger.info("[writeMailContent] Invalid Support Request data. lc_id is empty. support_id = {}, lc_id = {}", supportId, lcId);
            return null;
        }

        String sql = """
                SELECT u.email, c.lc_nm FROM users_roles r
                    LEFT JOIN lms_center_users cu ON cu.user_id = r.user_id 
                    JOIN users u ON cu.user_id = u.login
                    JOIN lms_centers c ON c.lc_id = cu.lc_id  
                WHERE cu.lc_id = :lcId 
                    AND r.role_id = '7156af53-47d8-40d5-a603-302568efb8b4';
                """;
        List<MailReceiverDto> receivers = this.queryManager.selectListBySql(sql, ValueUtil.newMap("lcId", lcId), MailReceiverDto.class, 0, 0);

        if (ValueUtils.isEmpty(receivers) || receivers.size() < 1) {
            logger.info("[wrtieMailContent] Recipient user not found.");
            return null;
        }

        String title = "[시스템 알림] 유지보수 요청이 작성되었습니다.";
        String sReqTitle = StringUtils.hasText(data.getTitle()) ? data.getTitle() : "-";
        String lcNm = receivers.get(0).getLcNm();
        String requestContent = StringUtils.hasText(data.getContent()) ? data.getContent() : "-";
        String link = SettingUtil.getValue(SysConfigConstants.CLIENT_CONTEXT_PATH);
        // 줄바꿈 변환 (\r\n, \n, \r → <br/>)
        String formattedContent = requestContent.trim().replaceAll("(\r\n|\n|\r)", "<br/>");

        Map<String, Object> templateParams = ValueUtil.newMap("supportId,supportTitle,lcId,lcNm,requestContent,link", supportId, sReqTitle, lcId, lcNm, formattedContent, link);
        String templatePath = LmsConstants.SUPPORT_REQUEST_MAIL_TEMPLATE_PATH;
        String template = FileUtil.readClassPathResource(templatePath);
        StringWriter writer = new StringWriter();
        this.templateEngine.processTemplate(template, writer, templateParams, null);
        String filledContent = writer.toString();

        return MailRequest.builder()
                .apiKey(null)
                .senderEmailAddress(DEFAULT_MAIL_SENDER)
                .toRecipients(receivers.stream().map(MailReceiverDto::getEmail).toList())
                .ccRecipients(new ArrayList<>())
                .bccRecipients(new ArrayList<>())
                .subject(title)
                .body(filledContent)
                .isImportant(false)
                .attachments(new ArrayList<>())
                .isExternApi(true)
                .templateParams(templateParams)
                .build();
    }
}