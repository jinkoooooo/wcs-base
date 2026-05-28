package operato.logis.changwon.service.impl.MFC;

import operato.logis.changwon.entity.MFC.PrsJobSts;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
public class PrsJobStsService extends AbstractQueryService {

    public PrsJobSts getShuttleRunnerStatus(int machineId) {
        String sql = "select * from prs_job_sts where machine_id = :machineId limit 1";
        Map<String, Object> param = ValueUtil.newMap("machineId", machineId);
        return this.queryManager.selectBySql(sql, param, PrsJobSts.class);
    }
}