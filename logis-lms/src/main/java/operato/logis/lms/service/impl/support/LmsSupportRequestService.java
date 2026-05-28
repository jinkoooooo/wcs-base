package operato.logis.lms.service.impl.support;

import lombok.RequiredArgsConstructor;
import net.sf.common.util.ValueUtils;
import operato.logis.lms.consts.SupportStatus;
import operato.logis.lms.dto.support.SupportFileUploadDto;
import operato.logis.lms.entity.support.LmsSupportRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.sys.util.EntityUtil;
import xyz.elidom.util.ValueUtil;

import java.lang.reflect.Method;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LmsSupportRequestService extends AbstractQueryService {

    private final KakaoAuthService kakaoAuthService;
    private final CryptoService cryptoService;
    private final LmsSupportAttachmentService attachmentService;
    private final MailService mailService;

    private final Logger logger = LoggerFactory.getLogger(LmsSupportRequestService.class);

    // 조회 전 전처리: 다중 객체 복호화
    @Transactional(readOnly = true)
    public void decryptRequestPage(Page<?> page) {
        @SuppressWarnings("unchecked")
        Page<LmsSupportRequest> typedPage = (Page<LmsSupportRequest>) page;
        List<LmsSupportRequest> srList = typedPage.getList();

        for (LmsSupportRequest sr : srList) {
            decryptRequest(sr);
        }
    }

    // 조회 전 전처리: 단일 객체 복호화
    public void decryptRequest(LmsSupportRequest data) {
        cryptoService.decryptAll(data);
    }

    // 다중 객체 저장 전 검증 및 데이터 보강
    public List<LmsSupportRequest> validateAndSetRequests(List<LmsSupportRequest> srList) {
        if (CollectionUtils.isEmpty(srList)) return srList;
        for (LmsSupportRequest sr : srList) {
            this.validateAndSetRequest(sr);
            cryptoService.encryptAll(sr);
        }
        return srList;
    }

    /**
     * 단일 객체 저장 전 검증 및 데이터 보강
     * 1. '임시 저장' 상태로 변경 허용되는지 검증
     * 2. 관리자 권한 확인 및 데이터 가공
     * 3. 공통권한 기본값 부여
     *
     * @param data
     * @return
     */
    private LmsSupportRequest validateAndSetRequest(LmsSupportRequest data) {
        if (ObjectUtils.isEmpty(data)) return data;

        String currentStatus = data.getStatus();
        String draftStatus = SupportStatus.DRAFT.getValue();

        if (draftStatus.equals(currentStatus) && !validateRequestStatus(data.getSupportId(), draftStatus)) {
            logger.info("[validateAndSetRequest] Cannot change status into DRAFT");
            throw new ElidomClientException(HttpStatus.SC_UNPROCESSABLE_ENTITY, "유효하지 않은 상태 변경입니다.", null, null);
        }

        if (User.isCurrentUserAdmin()) {
            setAdminMetadata(data, currentStatus);
        } else if (!validateRequester(data)){
            return null;
        }

        if (ValueUtils.isEmpty(data.getDeleted())) {
            data.setDeleted(false);
        }

        return data;
    }

    // 단일 객체 저장 전 담당자 지정일시, 완료일시 부여
    private void setAdminMetadata(LmsSupportRequest data, String currentStatus) {
        boolean isAssignableStatus = currentStatus != null && List.of(SupportStatus.ASSIGNED.getValue(), SupportStatus.DRAFT.getValue()).contains(currentStatus);

        if (ValueUtils.isEmpty(data.getReceiverId()) && isAssignableStatus) {
            data.setReceiverId(User.currentUser().getId());
        }

        if (!ValueUtils.isEmpty(data.getReceiverId()) && ValueUtils.isEmpty(data.getReceivedAt())) {
            data.setReceivedAt(DateUtil.getDate());
        }

        if (SupportStatus.COMPLETED.getValue().equals(currentStatus) && ValueUtils.isEmpty(data.getCompletedAt())) {
            data.setCompletedAt(DateUtil.getDate());
        }
    }

    // 단일 객체 저장 전 일반작성자가 수정할 권한이 있는지 확인
    // - 비로그인: 수정 권한없음
    // - 신규요청: 작성자 미확인
    private boolean validateRequester(LmsSupportRequest data) {
        User currentUser = User.currentUser();
        String creator = data.getRequesterId();

        if (currentUser == null) {
            logger.info("[validateRequester] Failed. Current user is empty.");
            return false;
        }

        if (!StringUtils.hasText(creator)){
            logger.info("[validateRequester] New Request. creator = {}, requester = {}", creator, currentUser.getId());
            return true;
        }

        if (creator.equals(currentUser.getId())) {
            logger.info("[validateRequester] User is creator = {}", creator);
            return true;
        }

        logger.info("[validateRequester] Forbidden. creator = {}, requester = {}", creator, currentUser.getId());
        return false;
    }

    // 임시저장 상태로 변경 가능한지 검증
    private boolean validateRequestStatus(String id, String draftStatus) {
        // 신규저장 검증 통과
        if (ValueUtils.isEmpty(id)) {
            return true;
        }

        LmsSupportRequest oldSr = this.queryManager.select(true, LmsSupportRequest.class, id);

        if (ObjectUtils.isEmpty(oldSr)) {
            logger.info("[validateRequestStatus] failed. SupportRequest Not found");
            return false;
        }

        String oldStatus = oldSr.getStatus();

        return draftStatus.equals(oldStatus) || SupportStatus.UNKNOWN.getValue().equals(oldStatus);
    }

    // 유지보수 요청 텍스트 생성/수정/삭제
    private void updateSupportRequest(LmsSupportRequest sr) {
        Method cudMethod = EntityUtil.getCudMethod(LmsSupportRequest.class);
        String cudFlag = EntityUtil.getCudValue(sr, cudMethod);

        if (ValueUtils.isEmpty(sr)) {
            logger.info("[updateSupportRequest] 생성/수정/삭제할 데이터가 없습니다. data = {}", sr);
            return;
        }

        if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_CREATE)) {
            this.queryManager.insert(sr);
        } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_UPDATE)) {
            this.queryManager.update(sr);
        } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_DELETE)) {
            this.queryManager.delete(sr);
        }
    }

    /**
     * 유지보수 요청 텍스트+파일 데이터 CUD 진입점
     *
     * @param dto 유지보수 요청 파일+텍스트
     * @return CUD성공여부
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean cudSupportRequestWithFiles(SupportFileUploadDto dto) {
        try {
            LmsSupportRequest srData = convertDtoToEntity(dto);

            srData = validateAndSetRequest(srData);
            if (srData == null) {
                logger.info("[cudSupportRequestWithFiles] 권한 부족으로 CUD 불가");
                return false;
            }

            cryptoService.encryptAll(srData);
            logger.info("[cudSupportRequestWithFiles] encrypted data update");

            updateSupportRequest(srData);

            String refId = srData.getSupportId();
            String cudFlag = dto.getCudFlag_();

            logger.info("[cudSupportRequestWithFiles] processing files");
            attachmentService.deletePartialFiles(dto.getDeletedFileIds(), refId, cudFlag);
            attachmentService.updateSupportRequestFiles(dto.getFiles(), refId, cudFlag);

            // 신규 등록('c')일 때만 보냄
            if ("c".equals(cudFlag)) {
                sendKakaoNotification(srData);
            }
            mailService.sendRequestMail(srData);
            return true;

        } catch (ElidomClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ElidomRuntimeException("유지보수 요청 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 카카오 알림 전송
    private void sendKakaoNotification(LmsSupportRequest srData) {
        try {
            String title = srData.getTitle();
            String centerName = srData.getLcId();
            String requester = srData.getAssigneeId();
            String equipId = srData.getEquipId();
            String alarmId = srData.getAlarmId();
            String content = srData.getContent();
            String category = srData.getCategory();

            // 파라미터 3개 넘기기
            kakaoAuthService.sendToFriends(title, centerName, requester, category, equipId, alarmId, content);
        } catch (Exception k) {
            // [중요] 카톡 전송이 실패해도, 게시글 저장은 성공으로 처리해야 함!
            logger.warn("카카오톡 알림 전송 실패 (저장은 성공함): {}", k.getMessage());
        }
    }

    // 저장 전 LmsSuportRequest DTO -> ENTITY 변환
    private LmsSupportRequest convertDtoToEntity(SupportFileUploadDto dto) {
        LmsSupportRequest entity = LmsSupportRequest.builder()
                .id(dto.getId())
                .supportId(dto.getSupportId())
                .lcId(dto.getLcId())
                .equipId(dto.getEquipId())
                .alarmId(dto.getAlarmId())
                .status(dto.getStatus())
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .requesterId(dto.getRequesterId())
                .assigneeId(dto.getAssigneeId())
                .receiverId(dto.getReceiverId())
                .isDeleted(dto.getIsDeleted())
                .receivedAt(dto.getReceivedAt())
                .completedAt(dto.getCompletedAt())
                .build();
        entity.setDomainId(dto.getDomainId());
        entity.setCreatorId(dto.getCreatorId());
        entity.setUpdaterId(dto.getUpdaterId());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setCudFlag_(dto.getCudFlag_());
        return entity;
    }
}