package operato.logis.lms.service.impl.support;

import lombok.RequiredArgsConstructor;
import net.sf.common.util.ValueUtils;
import operato.logis.lms.entity.support.LmsSupportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.exception.client.ElidomClientException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.sys.util.EntityUtil;
import xyz.elidom.util.ValueUtil;

import java.lang.reflect.Method;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LmsSupportResponseService extends AbstractQueryService {

    private final CryptoService cryptoService;

    private final Logger logger = LoggerFactory.getLogger(LmsSupportResponseService.class);

    /**
     * 유지보수 답변 CUD
     *
     * @param entity 유지보수 답변
     * @return CUD성공여부
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean cudOne(LmsSupportResponse entity) {
        try {
            entity = validateAndSetValue(entity);
            cryptoService.encryptAll(entity);

            updateSupportResponse(entity);
            return true;
        } catch (ElidomClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ElidomRuntimeException("유지보수 요청 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 단일 객체 저장 전 검증 및 데이터 보강
    private LmsSupportResponse validateAndSetValue(LmsSupportResponse data) {
        if (ObjectUtils.isEmpty(data)) return data;

        if (!User.isCurrentUserAdmin()) {
            throw new ElidomRuntimeException("Not allowed user");
        }

        logger.info("[validateAndSetValue] Validation check finished. resId = {}", data.getResId());
        return data;
    }

    // 유지보수 답변 생성/수정/삭제
    private void updateSupportResponse(LmsSupportResponse data) {
        if (data == null) return;

        Method cudMethod = EntityUtil.getCudMethod(LmsSupportResponse.class);
        String cudFlag = EntityUtil.getCudValue(data, cudMethod);

        logger.info("[updateSupportResponse] id = {}, cudFlag = {}", data.getId(), cudFlag);

        if (ValueUtils.isEmpty(data)) {
            logger.info("[updateSupportResponse] 생성/수정/삭제할 데이터가 없습니다. data = {}", data);
            return;
        }

        if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_CREATE)) {
            this.queryManager.insert(data);
        } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_UPDATE)) {
            this.queryManager.update(data);
        } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_DELETE)) {
            this.queryManager.delete(data);
        }
    }

    // 조회 전 전처리: 다중 객체 복호화
    @Transactional(readOnly = true)
    public void decryptPage(Page<?> page) {
        @SuppressWarnings("unchecked")
        Page<LmsSupportResponse> typedPage = (Page<LmsSupportResponse>) page;
        List<LmsSupportResponse> srList = typedPage.getList();

        for (LmsSupportResponse sr : srList) {
            decrypt(sr);
        }
    }

    // 조회 전 전처리: 단일 객체 복호화
    public void decrypt(LmsSupportResponse data) {
        cryptoService.decryptAll(data);
    }
}