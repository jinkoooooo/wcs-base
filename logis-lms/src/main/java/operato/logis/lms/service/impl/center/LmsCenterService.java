package operato.logis.lms.service.impl.center;

import operato.logis.lms.consts.EncryptionDomain;
import operato.logis.lms.entity.center.LmsCenterUsers;
import operato.logis.lms.entity.center.LmsCenters;
import operato.logis.lms.generator.LmsCenterIdGenerator;
import operato.logis.lms.util.AesGcmEncryptor;
import operato.logis.lms.util.EncryptedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LmsCenterService extends AbstractQueryService {

    private final AesGcmEncryptor aesGcmEncryptor;
    private final LmsCenterIdGenerator lmsCenterIdGenerator;

    private final LmsCenterUserService lmsCenterUserService;

    public LmsCenterService(AesGcmEncryptor aesGcmEncryptor, LmsCenterIdGenerator lmsCenterIdGenerator, LmsCenterUserService lmsCenterUserService) {
        this.aesGcmEncryptor = aesGcmEncryptor;
        this.lmsCenterIdGenerator = lmsCenterIdGenerator;
        this.lmsCenterUserService = lmsCenterUserService;
    }

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(LmsCenterService.class);

    /**
     * 사용자 소속 센터목록 조회
     * - admin: 전체 센터 목록(기본값) 또는 소속 센터 조회(relationFilter = true)
     * - 작업자: 소속 센터 목록
     *
     * @return 센터목록
     */
    @Transactional(readOnly = true)
    public List<LmsCenters> lmsCenterListByUser(Boolean relationFilter) {
        boolean useFilter = Boolean.TRUE.equals(relationFilter);
        return lmsCenterListByUserImpl(useFilter);
    }

    // 사용자 소속 센터목록 조회 구현
    public List<LmsCenters> lmsCenterListByUserImpl(boolean relationFilter) {
        try {
            Query condition = OrmUtil.newConditionForExecution();
            condition.addOrder("lcId", true);

            if (relationFilter || !User.isCurrentUserAdmin()) {
                List<LmsCenterUsers> cuList = lmsCenterUserService.lmsCenterUserListByUser(relationFilter);
                if (CollectionUtils.isEmpty(cuList)) {
                    return new ArrayList<>();
                }
                List<String> allowedLcIds = cuList.stream().map(LmsCenterUsers::getLcId).toList();
                condition.addFilter("lcId", OrmConstants.IN, allowedLcIds);
            }

            List<LmsCenters> result = queryManager.selectList(LmsCenters.class, condition);
            logger.info("사용자 소속 센터목록 조회 size = {}", result.size());

            if (CollectionUtils.isEmpty(result)) {
                return new ArrayList<>();
            }

            for (LmsCenters center : result) {
                decryptAll(center);
            }
            return result;
        } catch (Exception e) {
            logger.error("사용자 소속 센터목록 조회 실패 e =", e.getMessage());
            throw e;
        }
    }

    /**
     * 사용자 소속 센터목록 필터링
     * 1. 소속 센터 조회
     * - admin: 전체 센터 조회
     * - 일반 사용자: 소속 센터 조회
     * 2. 주소, 엣지서버 정보 복호화
     *
     * @param page
     */
    @Transactional(readOnly = true)
    public void pageByUser(Page<?> page) {
        @SuppressWarnings("unchecked")
        Page<LmsCenters> typedPage = (Page<LmsCenters>) page;
        List<LmsCenters> centers = typedPage.getList();

        if (CollectionUtils.isEmpty(centers)) return;

        if (User.isCurrentUserAdmin()) {
            // 관리자: 전체 센터 조회
            List<LmsCenters> processedCenters = centers.stream()
                    .map(this::decryptAll)
                    .toList();
            typedPage.setList(processedCenters);
            typedPage.setTotalSize(processedCenters.size());
        } else {
            // 일반 사용자
            List<LmsCenterUsers> cuList = lmsCenterUserService.lmsCenterUserListByUser(false);
            if (CollectionUtils.isEmpty(cuList)) {
                typedPage.setList(new ArrayList<>());
                typedPage.setTotalSize(0);
                return;
            }

            Set<String> allowedLcIdSet = cuList.stream().map(LmsCenterUsers::getLcId).collect(Collectors.toSet());
            List<LmsCenters> processedCenters = centers.stream()
                    .filter(center -> allowedLcIdSet.contains(center.getLcId()))
                    .map(this::decryptAll)
                    .toList();
            typedPage.setList(processedCenters);
            typedPage.setTotalSize(processedCenters.size());
        }
    }

    private LmsCenters decryptAll(LmsCenters entity) {
        decryptAddress(entity);
        decryptEdge(entity);
        return entity;
    }

    private LmsCenters encryptAll(LmsCenters entity) {
        logger.info("Start to encrypt center data. id = {}, lcId = {}", entity.getId(), entity.getLcId());
        encryptAddress(entity);
        encryptEdge(entity);
        return entity;
    }

    /**
     * 리스트 데이터의 주소, 엣지서버연결정보 암호화
     *
     * @param centers
     */
    public void encryptList(List<LmsCenters> centers) {
        try {
            for (LmsCenters entity : centers) {
                createOrReadLcId(entity);
                encryptAll(entity);
            }
        } catch (Exception e) {
            logger.error("[encryptList] encryption failed", e);
            throw e;
        }
    }

    /**
     * 주소 복호화
     *
     * @param entity
     * @return
     */
    private LmsCenters decryptAddress(LmsCenters entity) {
        if (ObjectUtils.isEmpty(entity)) {
            return entity;
        }

        try {
            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            byte[] addressCt = entity.getAddressCt();
            byte[] addressIv = entity.getAddressIv();

            if (addressCt == null) {
                return entity;
            }

            if (addressIv == null) {
                logger.info("Decrypt address failed. addressIv is null.");
                return entity;
            }

            String addressPlain = aesGcmEncryptor.decrypt(addressCt, addressIv, aad, EncryptionDomain.ADDRESS);
            entity.setAddressPlain(addressPlain);

        } catch (Exception e) {
            logger.error("[decryptAddress] failed. lcId = {}, e = {}", entity.getLcId(), e.getMessage());
            entity.setAddressPlain(null);
        } finally {
            entity.setAddressCt(null);
            entity.setAddressIv(null);
            entity.setAddressKeyId(null);
        }
        return entity;
    }

    /**
     * 엣지서버 관련정보 복호화
     *
     * @param entity
     * @return
     */
    private LmsCenters decryptEdge(LmsCenters entity) {
        if (ObjectUtils.isEmpty(entity)) return entity;

        try {
            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            byte[] edgeCt = entity.getEdgeCt();
            byte[] edgeIv = entity.getEdgeIv();

            if (edgeCt == null) {
                return entity;
            }

            if (edgeIv == null) {
                logger.info("Decrypt edge-server failed. edgeIv is null.");
                return entity;
            }

            String edgePlain = aesGcmEncryptor.decrypt(edgeCt, edgeIv, aad, EncryptionDomain.EDGE_SERVER);
            entity.setDbConnectInfo(edgePlain);
        } catch (Exception e) {
            logger.error("[decryptEdge] failed. lcId = {}, e = {}", entity.getLcId(), e.getMessage());
            entity.setDbConnectInfo(null);
        } finally {
            entity.setEdgeCt(null);
            entity.setEdgeIv(null);
            entity.setEdgeKeyId(null);
        }
        return entity;
    }

    /**
     * 주소 암호화
     *
     * @param entity
     */
    private void encryptAddress(LmsCenters entity) {
        if (ObjectUtils.isEmpty(entity) || ObjectUtils.isEmpty(entity.getLcId())) {
            throw new IllegalStateException("AAD로 사용할 센터코드가 없어 암호화를 진행할 수 없습니다. 센터코드를 생성해주세요");
        }

        try {
            String addressPlain = entity.getAddressPlain();
            if (addressPlain == null || addressPlain.trim().isEmpty()) {
                entity.setAddressPlain(null);
                entity.setAddressCt(null);
                entity.setAddressIv(null);
                entity.setAddressKeyId(null);
                logger.info("addressPlain is empty. lcId = {}", entity.getLcId());
                return;
            }

            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            EncryptedData eData = aesGcmEncryptor.encrypt(addressPlain, aad, EncryptionDomain.ADDRESS);

            if (eData != null) {
                entity.setAddressCt(eData.getCipherText());
                entity.setAddressIv(eData.getIv());
                entity.setAddressKeyId(eData.getKeyId());
                logger.info("[Crypto-Success] Domain: ADDRESS, id = {}, lcID = {}, keyId = {}, aad-length = {}bytes", entity.getId(), entity.getLcId(), eData.getKeyId(), aad.length);
            }
        } catch (Exception e) {
            logger.error("[Crypto-Fail] Domain: ADDRESS, id = {}, lcId = {}, e = {}", entity.getId(), entity.getLcId(), e.getMessage());
            throw e;
        } finally {
            entity.setAddressPlain(null);
        }
    }

    /**
     * 엣지서버 관련 정보 암호화
     *
     * @param entity
     */
    private void encryptEdge(LmsCenters entity) {
        if (ObjectUtils.isEmpty(entity) || ObjectUtils.isEmpty(entity.getLcId())) {
            throw new IllegalStateException("AAD로 사용할 센터코드가 없어 암호화를 진행할 수 없습니다. 센터코드를 생성해주세요");
        }

        try {
            String dbInfoPlain = entity.getDbConnectInfo();
            if (dbInfoPlain == null || dbInfoPlain.trim().isEmpty()) {
                entity.setEdgeCt(null);
                entity.setEdgeIv(null);
                entity.setEdgeKeyId(null);
                return;
            }

            byte[] aad = entity.getLcId().getBytes(StandardCharsets.UTF_8);
            EncryptedData eData = aesGcmEncryptor.encrypt(dbInfoPlain, aad, EncryptionDomain.EDGE_SERVER);

            if (eData != null) {
                entity.setEdgeCt(eData.getCipherText());
                entity.setEdgeIv(eData.getIv());
                entity.setEdgeKeyId(eData.getKeyId());
                logger.info("[Crypto-Success] Domain: EDGE_SERVER, id = {}, lcID = {}, keyId = {}, aad-length = {}bytes", entity.getId(), entity.getLcId(), eData.getKeyId(), aad.length);
            }
        } catch (Exception e) {
            logger.error("[Crypto-Fail] Domain: EDGE_SERVER, id = {}, lcId = {}, e = {}", entity.getId(), entity.getLcId(), e.getMessage());
            throw e;
        } finally {
            entity.setDbConnectInfo(null);
        }
    }

    /**
     * 센터코드 부여
     *
     * @param entity
     */
    private void createOrReadLcId(LmsCenters entity) {
        if (ObjectUtils.isEmpty(entity.getId())) {
            String lcId = lmsCenterIdGenerator.generateLcId(entity.getAddressPlain());
            entity.setLcId(lcId);
            return;
        }

        LmsCenters center = this.queryManager.select(entity);
        if (center != null) {
            entity.setLcId(center.getLcId());
        }
    }
}
