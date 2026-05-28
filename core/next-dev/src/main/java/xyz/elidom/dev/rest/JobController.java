package xyz.elidom.dev.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dev.entity.JobDefinition;
import xyz.elidom.dev.repository.JobRepository;
import xyz.elidom.dev.service.DynamicJobScheduler;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/job")
@ServiceDesc(description = "Job Service API")
public class JobController extends AbstractRestService {

    private final DynamicJobScheduler jobScheduler;
    private final JobRepository jobRepository;

    @Override
    protected Class<?> entityClass() {
        return JobDefinition.class;
    }

    @RequestMapping(method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name="page", required=false) Integer page,
            @RequestParam(name="limit", required=false) Integer limit,
            @RequestParam(name="select", required=false) String select,
            @RequestParam(name="sort", required=false) String sort,
            @RequestParam(name="query", required=false) String query) {
        return this.search(this.entityClass(), page, limit, select, sort, query);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Find one by ID")
    public JobDefinition findOne(@PathVariable("id") String id) {
        return this.getOne(this.entityClass(), id);
    }

    @RequestMapping(value="/{id}/exist", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check exists By ID")
    public Boolean isExist(@PathVariable("id") String id) {
        return this.isExistOne(this.entityClass(), id);
    }

    @RequestMapping(method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description="Create")
    public JobDefinition create(@RequestBody JobDefinition input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public JobDefinition update(@PathVariable("id") String id, @RequestBody JobDefinition input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Map<String, Object> multipleUpdate(@RequestBody List<JobDefinition> list) {
        for (JobDefinition request : list) {
            // 1. 업데이트나 삭제인 경우에만 체크를 진행
            boolean isUpdateOrDelete = request.getCudFlag_().equals(OrmConstants.CUD_FLAG_UPDATE)
                    || request.getCudFlag_().equals(OrmConstants.CUD_FLAG_DELETE);

            if (isUpdateOrDelete) {
                JobDefinition job = jobRepository.findById(request.getId());

                // 2. DB에 데이터가 없거나(이미 삭제됨), 현재 활성화 상태라면 차단
                if (ValueUtil.isEmpty(job) || job.getIsActive()) {
                    return ValueUtil.newMap("code,message", 1, "fail");
                }
            }
        }

        this.cudMultipleData(this.entityClass(), list);

        return ValueUtil.newMap("code,message", 0, "success");
    }

    @RequestMapping(value="/refresh", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Job Refresh API")
    public Boolean refreshJobs() {
        jobScheduler.refreshJobs();
        return true;
    }

    @RequestMapping(value="/control", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Job Start or Stop API")
    public Boolean controlJobStatus(@RequestBody JobDefinition job) {
        jobRepository.updateJobActive(job.getId(), job.getIsActive());
        jobScheduler.refreshJobs();
        return true;
    }
}