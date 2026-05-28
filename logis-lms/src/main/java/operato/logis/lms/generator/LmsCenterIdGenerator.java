package operato.logis.lms.generator;

import operato.logis.lms.consts.RegionCd;
import operato.logis.lms.entity.center.LmsCenterRegionSeq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Component
public class LmsCenterIdGenerator extends AbstractQueryService {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(LmsCenterIdGenerator.class);

    // 센터 ID 생성
    public String generateLcId(String address) {
        // 1. 주소 평문에서 지역 추출
        String regionCd = RegionCd.fromAddress(address).name();

        // 2. 다음 시퀀스 번호 조회
        int regionSeq = getNextSeqForRegion(regionCd);

        // 3. lc_id 생성
        return String.format("%s%03d", regionCd, regionSeq); // "SEL-001" 형식
    }

    // 지역별 센터 수 조회
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation= Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int getNextSeqForRegion(String regionCd) {
        // 조회
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("region_id", regionCd);
        LmsCenterRegionSeq seq =  this.queryManager.selectByCondition(LmsCenterRegionSeq.class, condition);

        if (ObjectUtils.isEmpty(seq)) throw new ElidomRuntimeException("Invalid address", "Invalid address");

        // 갱신
        seq.setCurrentSeq(seq.getCurrentSeq() + 1);
        this.queryManager.update(seq);

        return seq.getCurrentSeq();
    }
}