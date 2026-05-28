package operato.logis.changwon.rest.MFC;

import operato.logis.changwon.entity.MFC.PrsJobSts;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;

@RestController
@RequestMapping("/rest/prs_job_sts")
@ServiceDesc(description="PrsJobSts Service API")
public class PrsJobStsController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return PrsJobSts.class;
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search (Pagination) By Search Conditions")
    public Page<PrsJobSts> index(
            @RequestParam(name="page", required=false) Integer page,
            @RequestParam(name="limit", required=false) Integer limit,
            @RequestParam(name="select", required=false) String select,
            @RequestParam(name="sort", required=false) String sort,
            @RequestParam(name="query", required=false) String query) {

        String sql = """
				SELECT MACHINE_ID, ODR.WMS_ORD_NO, STS.ORDER_ID, STS.PALLET_ID, LOAD_CHK, ERROR_CODE
				FROM PRS_JOB_STS STS
				LEFT JOIN C_JOB_ODR ODR ON (STS.ORDER_ID = ODR.ORDER_ID AND STS.JOB_NO = ODR.JOB_NO)
				ORDER BY MACHINE_ID ASC
				""";
        List<PrsJobSts> items = this.queryManager.selectListBySql(sql, null, PrsJobSts.class, 0, 0);

        Page<PrsJobSts> result = (Page<PrsJobSts>) this.search(PrsJobSts.class, page, limit, select, sort, query);
        result.setList(items);

        return result;
    }

    @RequestMapping(value="/{id}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Find one by ID")
    public PrsJobSts findOne(@PathVariable("id") String id) {
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
    public PrsJobSts create(@RequestBody PrsJobSts input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public PrsJobSts update(@PathVariable("id") String id, @RequestBody PrsJobSts input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<PrsJobSts> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }
}
