package operato.logis.samsung.rest.mw;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.xyz.XyzOrderInfoRequest;
import operato.logis.samsung.entity.mw.TbMwXyzOrder;
import operato.logis.samsung.service.mw.TbMwXyzOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

import java.util.List;
import java.util.Map;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tb_mw_xyz_order")
@ServiceDesc(description="TbMwXyzOrder Service API")
public class TbMwXyzOrderController extends AbstractRestService {

    private final TbMwXyzOrderService tbMwXyzOrderService;

    @Override
    protected Class<?> entityClass() {
        return TbMwXyzOrder.class;
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
    public TbMwXyzOrder findOne(@PathVariable("id") String id) {
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
    public TbMwXyzOrder create(@RequestBody TbMwXyzOrder input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public TbMwXyzOrder update(@PathVariable("id") String id, @RequestBody TbMwXyzOrder input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<TbMwXyzOrder> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }

    @RequestMapping(value="/cancel_order", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Cancel, 대기 or 진행 중인 작업 강제 종료")
    public Map<String, Object> cancelOrder(@RequestBody TbMwXyzOrder order) {
        return this.tbMwXyzOrderService.cancelOrder(order);
    }

    @RequestMapping(value="/search", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search (Pagination) By POST")
    public Page<?> searchPost(@RequestBody Map<String, Object> params) {

        // 1. 페이징 및 기본 파라미터 안전하게 파싱 (Map에서 꺼낼 때 타입 오류 방지)
        Integer page = params.get("page") != null ? Integer.valueOf(params.get("page").toString()) : null;
        Integer limit = params.get("limit") != null ? Integer.valueOf(params.get("limit").toString()) : null;
        String select = (String) params.get("select");
        String sort = (String) params.get("sort");

        // 2. Query 파라미터 처리 (프론트에서 문자열로 오든 배열로 오든 모두 대응)
        String queryStr = null;
        if (params.get("query") != null) {
            Object queryObj = params.get("query");
            if (queryObj instanceof String) {
                queryStr = (String) queryObj;
            } else {
                // 프론트엔드에서 배열 객체 자체를 보냈을 경우, Elidom 내부 파서를 위해 Gson으로 안전하게 문자열 변환
                queryStr = new Gson().toJson(queryObj);
            }
        }

        // 3. Elidom의 기존 search 로직 태우기
        return this.search(this.entityClass(), page, limit, select, sort, queryStr);
    }

    @PostMapping(value = "/reject_info", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Search (Pagination) By POST")
    public Page<?> searchWithRejectCalc(@RequestBody XyzOrderInfoRequest params) {
        String[] acceptDateTimeList = params.getAcceptDatetime() != null ? params.getAcceptDatetime().split(",") : new String[0];

        List<TbMwXyzOrder> result = tbMwXyzOrderService.getRejectDeliveryInfo(
                params.getProcessStatus(), params.getStartPointCd(), params.getEndPointCd(), params.getItemCode(), acceptDateTimeList);

        Integer limit = params.getLimit() != null ? params.getLimit() : 50;

        Page<TbMwXyzOrder> pageResult = new Page<TbMwXyzOrder>();
        pageResult.setList(result);
        pageResult.setSize(limit);
        pageResult.setTotalSize(result.size());
        return pageResult;
    }
}