package operato.logis.lms.rest.monitoring;

import operato.logis.lms.dto.monitoring.LmsMonitoringStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.ArrayList;
import java.util.List;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/lms_monitoring_status")
@ServiceDesc(description = "LmsMonitoringStatus Service API")
public class LmsMonitoringStatusController {

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Search (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "select", required = false) String select,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "query", required = false) String query) {
        // TODO: 쿼리변경
        List<LmsMonitoringStatusDto> list = new ArrayList<>();
        Page<LmsMonitoringStatusDto> newPage = new Page<>();
        newPage.setSize(list.size());
        newPage.setList(list);
        return newPage;
        //return this.search(this.entityClass(), page, limit, select, sort, query);
    }
    //
    //@RequestMapping(value="/{id}", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    //@ApiDesc(description="Find one by ID")
    //public LmsMonitoringStatus findOne(@PathVariable("id") String id) {
    //	return this.getOne(this.entityClass(), id);
    //}
    //
    //@RequestMapping(value="/{id}/exist", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    //@ApiDesc(description="Check exists By ID")
    //public Boolean isExist(@PathVariable("id") String id) {
    //	return this.isExistOne(this.entityClass(), id);
    //}
    //
    //@RequestMapping(method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    //@ResponseStatus(HttpStatus.CREATED)
    //@ApiDesc(description="Create")
    //public LmsMonitoringStatus create(@RequestBody LmsMonitoringStatus input) {
    //	return this.createOne(input);
    //}
    //
    //@RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    //@ApiDesc(description="Update")
    //public LmsMonitoringStatus update(@PathVariable("id") String id, @RequestBody LmsMonitoringStatus input) {
    //	return this.updateOne(input);
    //}
    //
    //@RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    //@ApiDesc(description="Delete")
    //public void delete(@PathVariable("id") String id) {
    //	this.deleteOne(this.entityClass(), id);
    //}
    //
    //@RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    //@ApiDesc(description="Create, Update or Delete multiple at one time")
    //public Boolean multipleUpdate(@RequestBody List<LmsMonitoringStatus> list) {
    //	return this.cudMultipleData(this.entityClass(), list);
    //}
}