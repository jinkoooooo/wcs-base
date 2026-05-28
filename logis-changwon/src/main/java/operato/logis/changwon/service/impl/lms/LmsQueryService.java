package operato.logis.changwon.service.impl.lms;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.changwon.entity.MFC.ErrLog;
import operato.logis.changwon.entity.MFC.PrsJobSts;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LmsQueryService extends AbstractQueryService {

    /**
     * [DB 조회] 컨베이어 설비 상태 전체 조회
     * @return List<PrsJobSts>
     */
    public List<PrsJobSts> getConveyorStatusList() {
        // 조건 없이 전체 조회 (필요시 WHERE 절 추가)
        String sql = "SELECT * FROM prs_job_sts";

        // selectBySql을 사용하여 Entity 리스트로 반환
        return this.queryManager.selectListBySql(sql, null, PrsJobSts.class, 0, 0);
    }

    /**
     * [DB 조회] 컨베이어 설비 상태 전체 조회
     * @return List<LmsEquipmentStatus>
     */
    public List<ErrLog> getAgvStatusList() {
        String sql = "select * from tb_ecs_status where is_latest  = 1";

        // selectBySql을 사용하여 Entity 리스트로 반환
        return this.queryManager.selectListBySql(sql, null, ErrLog.class, 0, 0);
    }
}
