package xyz.elidom.dev.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.dev.entity.JobDefinition;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class JobRepository extends AbstractQueryService {

    public List<JobDefinition> findAll() {
        String sql = "select * from jobs";
        return this.queryManager.selectListBySql(sql, null, JobDefinition.class, 0, 0);
    }

    public JobDefinition findById(String id) {
        String sql = "select * from jobs where id = :id";
        Map<String, Object> param = ValueUtil.newMap("id", id);
        return this.queryManager.selectBySql(sql, param, JobDefinition.class);
    }

    public void updateJobActive(String jobId, boolean isActive) {
        // 엔티티를 다시 조회해서 업데이트 (영속성 컨텍스트 관리)
        JobDefinition job = findById(jobId);
        if (ValueUtil.isEmpty(job)) {
            return;
        }

        job.setOkCount(0);
        job.setNgCount(0);
        job.setIsActive(isActive);
        this.queryManager.update(job, "okCount", "ngCount", "isActive");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobResult(String jobId, boolean isSuccess, String message) {
        // 엔티티를 다시 조회해서 업데이트 (영속성 컨텍스트 관리)
        JobDefinition job = findById(jobId);
        if (ValueUtil.isEmpty(job)) {
            return;
        }

        if (isSuccess) {
            job.setOkCount(job.getOkCount() + 1);
        } else {
            job.setNgCount(job.getNgCount() + 1);
        }
        job.setLastResult(message);
        job.setUpdatedAt(new Date());
        this.queryManager.update(job, "okCount", "ngCount", "lastResult", "updatedAt");
    }
}