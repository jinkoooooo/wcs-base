package operato.logis.samsung.rest.mw;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import operato.logis.samsung.service.mw.BcrItemDimensionAvgAggregateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import operato.logis.samsung.entity.mw.TbMwBcrItemDimensionAvg;

import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.util.SettingUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequiredArgsConstructor
@RequestMapping("/rest/tb_mw_bcr_item_dimension_avg")
@ServiceDesc(description="TbMwBcrItemDimensionAvg Service API")
public class TbMwBcrItemDimensionAvgController extends AbstractRestService {

	private final BcrItemDimensionAvgAggregateService bcrItemDimensionAvgAggregateService;

	@Override
	protected Class<?> entityClass() {
		return TbMwBcrItemDimensionAvg.class;
	}

	@RequestMapping(method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
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
	public TbMwBcrItemDimensionAvg findOne(@PathVariable("id") String id) {
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
	public TbMwBcrItemDimensionAvg create(@RequestBody TbMwBcrItemDimensionAvg input) {
		return this.createOne(input);
	}

	@RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update")
	public TbMwBcrItemDimensionAvg update(@PathVariable("id") String id, @RequestBody TbMwBcrItemDimensionAvg input) {
		return this.updateOne(input);
	}

	@RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple at one time")
	public Boolean multipleUpdate(@RequestBody List<TbMwBcrItemDimensionAvg> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}

	/**
	 * 최근 N일 집계 (기본 7일)
	 * POST /api/mw/bcr/dimension-avg/aggregate?days=7
	 */
	@PostMapping("/dimension-avg/aggregate")
	public BcrItemDimensionAvgAggregateService.AggregateResult aggregate(@RequestParam(value="days", required=false) Integer days) {
		int periodDays = (days != null) ? days : Integer.parseInt(SettingUtil.getValue("mw.bcr.avg.aggregate.days", "7"));
		return bcrItemDimensionAvgAggregateService.aggregateLastDays(periodDays);
	}

    @RequestMapping(value="/search", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search (Pagination) By POST")
    public Page<?> searchPost(@RequestBody Map<String, Object> params) {

        Integer page = params.get("page") != null ? Integer.valueOf(params.get("page").toString()) : null;
        Integer limit = params.get("limit") != null ? Integer.valueOf(params.get("limit").toString()) : null;
        String select = (String) params.get("select");
        String sort = (String) params.get("sort");

        String queryStr = null;
        if (params.get("query") != null) {
            Object queryObj = params.get("query");
            if (queryObj instanceof String) {
                queryStr = (String) queryObj;
            } else {
                queryStr = new Gson().toJson(queryObj);
            }
        }

        return this.search(this.entityClass(), page, limit, select, sort, queryStr);
    }
}