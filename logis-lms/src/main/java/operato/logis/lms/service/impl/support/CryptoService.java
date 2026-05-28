package operato.logis.lms.service.impl.support;

import lombok.RequiredArgsConstructor;
import operato.logis.lms.consts.EncryptionDomain;
import operato.logis.lms.entity.support.LmsSupportRequest;
import operato.logis.lms.entity.support.LmsSupportResponse;
import operato.logis.lms.util.AesGcmEncryptor;
import operato.logis.lms.util.EncryptedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import xyz.elidom.exception.server.ElidomRuntimeException;

import java.nio.charset.StandardCharsets;

// 필드 암/복호화 서비스
@Service
@RequiredArgsConstructor
public class CryptoService {

    // todo: 공통 로직 리팩토링

    private final AesGcmEncryptor aesGcmEncryptor;

    private final Logger logger = LoggerFactory.getLogger(CryptoService.class);

    public void encryptAll(LmsSupportRequest entity) {
        encryptAssigneeId(entity);
    }

    public void encryptAll(LmsSupportResponse entity) {
        encryptCreatorNm(entity);
    }

    public void decryptAll(LmsSupportRequest entity) {
        decryptAssigneeId(entity);
    }

    public void decryptAll(LmsSupportResponse entity) {
        decryptCreatorNm(entity);
    }

    /**
     * 암호화
     */

    // 유지보수 요청 - 담당자 암호화
    private void encryptAssigneeId(LmsSupportRequest entity) {
        if (ObjectUtils.isEmpty(entity) || ObjectUtils.isEmpty(entity.getLcId())) {
            throw new IllegalStateException("AAD로 사용할 센터코드가 없어 암호화를 진행할 수 없습니다. 센터코드를 생성해주세요");
        }

        try {
            String assigneePlain = entity.getAssigneeId();
            if (assigneePlain == null || assigneePlain.trim().isEmpty()) {
                entity.setAssigneeId(null);
                entity.setAssigneeCt(null);
                entity.setAssigneeIv(null);
                entity.setAssigneeKeyId(null);
                logger.info("assigneePlain is empty. entity = {}", entity);
                return;
            }
            logger.info("assigneePlain is not empty");

            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            EncryptedData eData = aesGcmEncryptor.encrypt(assigneePlain, aad, EncryptionDomain.PERSONAL_INFO);

            if (eData != null) {
                entity.setAssigneeCt(eData.getCipherText());
                entity.setAssigneeIv(eData.getIv());
                entity.setAssigneeKeyId(eData.getKeyId());
                logger.info("[Crypto-Success] Domain: PERSONAL_INFO, id = {}, lcID = {}, keyId = {}, aad-length = {}bytes", entity.getId(), entity.getLcId(), eData.getKeyId(), aad.length);
            }
        } catch (Exception e) {
            logger.error("[Crypto-Fail] Domain: PERSONAL_INFO, id = {}, lcId = {}, e = {}", entity.getId(), entity.getLcId(), e.getMessage());
            throw e;
        } finally {
            entity.setAssigneeId(null);
        }
    }

    // 유지보수 답변 - 작성자 암호화
    private void encryptCreatorNm(LmsSupportResponse entity) {
        if (ObjectUtils.isEmpty(entity) || ObjectUtils.isEmpty(entity.getLcId())) {
            throw new IllegalStateException("AAD로 사용할 센터코드가 없어 암호화를 진행할 수 없습니다. 센터코드를 생성해주세요");
        }

        try {
            String creatorNmPlain = entity.getCreatorNm();
            if (creatorNmPlain == null || creatorNmPlain.trim().isEmpty()) {
                entity.setCreatorNm(null);
                entity.setCreator(null);
                entity.setCreatorNmIv(null);
                entity.setCreatorNmKeyId(null);
                logger.info("creatorNmPlain is empty. entity = {}", entity);
                return;
            }
            logger.info("creatorNmPlain is not empty");

            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            EncryptedData eData = aesGcmEncryptor.encrypt(creatorNmPlain, aad, EncryptionDomain.PERSONAL_INFO);

            if (eData != null) {
                entity.setCreatorNmCt(eData.getCipherText());
                entity.setCreatorNmIv(eData.getIv());
                entity.setCreatorNmKeyId(eData.getKeyId());
                logger.info("[Crypto-Success] Domain: PERSONAL_INFO, id = {}, lcID = {}, keyId = {}, aad-length = {}bytes", entity.getId(), entity.getLcId(), eData.getKeyId(), aad.length);
            }
        } catch (Exception e) {
            logger.error("[Crypto-Fail] Domain: PERSONAL_INFO, id = {}, lcId = {}, e = {}", entity.getId(), entity.getLcId(), e.getMessage());
            throw e;
        } finally {
            entity.setCreatorNm(null);
        }
    }

    /**
     * 복호화
     */

    // 유지보수 요청 - 담당자 복호화
    private LmsSupportRequest decryptAssigneeId(LmsSupportRequest entity) {
        if (ObjectUtils.isEmpty(entity)) return entity;

        try {
            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            byte[] assigneeCt = entity.getAssigneeCt();
            byte[] assigneeIv = entity.getAssigneeIv();

            if (assigneeCt == null) {
                return entity;
            }

            if (assigneeIv == null) {
                logger.info("Decrypt assigneeId failed. assigneeIv is null.");
                return entity;
            }

            String assigneePlain = aesGcmEncryptor.decrypt(assigneeCt, assigneeIv, aad, EncryptionDomain.PERSONAL_INFO);
            entity.setAssigneeId(assigneePlain);
        } catch (Exception e) {
            logger.error("[decryptAssignee] failed. lcId = {}, e = {}", entity.getLcId(), e.getMessage());
            entity.setAssigneeId(null);
        } finally {
            entity.setAssigneeCt(null);
            entity.setAssigneeIv(null);
            entity.setAssigneeKeyId(null);
        }
        return entity;
    }

    // 유지보수 답변 - 작성자 복호화
    private LmsSupportResponse decryptCreatorNm(LmsSupportResponse entity) {
        if (ObjectUtils.isEmpty(entity)) return entity;

        try {
            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            byte[] creatorNmCt = entity.getCreatorNmCt();
            byte[] creatorNmIv = entity.getCreatorNmIv();

            if (creatorNmCt == null) {
                return entity;
            }

            if (creatorNmIv == null) {
                logger.info("Decrypt creatorNmId failed. creatorNmIv is null.");
                return entity;
            }

            String creatorNmPlain = aesGcmEncryptor.decrypt(creatorNmCt, creatorNmIv, aad, EncryptionDomain.PERSONAL_INFO);
            entity.setCreatorNm(creatorNmPlain);
        } catch (Exception e) {
            logger.error("[decryptCreatorNm] failed. lcId = {}, e = {}", entity.getLcId(), e.getMessage());
            entity.setCreatorNm(null);
        } finally {
            entity.setCreatorNmCt(null);
            entity.setCreatorNmIv(null);
            entity.setCreatorNmKeyId(null);
        }
        return entity;
    }
}
