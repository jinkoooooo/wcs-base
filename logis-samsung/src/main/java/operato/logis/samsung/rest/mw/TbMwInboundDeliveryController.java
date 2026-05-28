package operato.logis.samsung.rest.mw;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.service.buffer.BufferInboundService;
import operato.logis.samsung.service.mw.InboundImportService;
import operato.logis.samsung.service.mw.TbMwInboundDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tb_mw_inbound_delivery")
@ServiceDesc(description="TbMwInboundDelivery Service API")
public class TbMwInboundDeliveryController extends AbstractRestService {

    private final TbMwInboundDeliveryService tbMwInboundDeliveryService;

    private final InboundImportService inboundImportService;

    private final BufferInboundService bufferInboundService;

    /**
     * Logger
     */
    protected Logger logger = LoggerFactory.getLogger(TbMwInboundDeliveryController.class);

    @Override
    protected Class<?> entityClass() {
        return TbMwInboundDelivery.class;
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
    public TbMwInboundDelivery findOne(@PathVariable("id") String id) {
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
    public TbMwInboundDelivery create(@RequestBody TbMwInboundDelivery input) {
        return this.createOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update")
    public TbMwInboundDelivery update(@PathVariable("id") String id, @RequestBody TbMwInboundDelivery input) {
        return this.updateOne(input);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.entityClass(), id);
    }

    @RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<TbMwInboundDelivery> list) {
        return this.cudMultipleData(this.entityClass(), list);
    }

    @RequestMapping(value="/excel_upload", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Excel upload at one time")
    public Boolean excelUpload(@RequestBody List<TbMwInboundDelivery> list) {
        this.queryManager.insertBatch(list);
        return true;
    }

    @RequestMapping(
            value = "/inbound_delivey_info",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "특정 주문 정보 조회(날짜/주문번호/컨테이너번호)")
    public List<TbMwInboundDelivery> getInboundDeliveryInfo(@RequestBody TbMwInboundDelivery req) {

        return tbMwInboundDeliveryService.getInboundDeliveryByDate(req.getInboundDate());
    }

    @RequestMapping(
            value = "/inbound_delivey_info_date",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "특정 주문 정보 조회(날짜/주문번호/컨테이너번호)")
    public List<TbMwInboundDelivery> getInboundDeliveryInfoDate(@RequestBody Map<String, Object> reqMap) {
        String startDate = (String) reqMap.get("startDate");
        String endDate = (String) reqMap.get("endDate");
        return tbMwInboundDeliveryService.getInboundDeliveryByDateSdl(startDate,endDate);
    }

    @RequestMapping(
            value = "/save_inbound_seq",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "입고 상세 입고순서(inbound_seq) 저장")
    public Map<String, Object> saveInboundSeq(@RequestBody List<TbMwInboundDelivery> items) {

        if (ValueUtil.isEmpty(items)) {
            throw new ElidomRuntimeException("저장할 데이터가 없습니다.");
        }

        int updated = tbMwInboundDeliveryService.updateInboundSeq(items);

        Map<String, Object> res = new HashMap<String, Object>();
        res.put("success", true);
        res.put("updatedCount", updated);
        return res;
    }

    @RequestMapping(
            value = "/import_inbound_delivery",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "입고 리스트 Excel Import")
    public Map<String, Object> importInboundDelivery(@RequestBody List<TbMwInboundDelivery> items) {

        if (ValueUtil.isEmpty(items)) {
            throw new ElidomRuntimeException("저장할 데이터가 없습니다.");
        }


        Map<String, Object> result = inboundImportService.insertImportData(items);
        // todo: 로직 수정 완료 후 주석해제
        // 시퀀스 버퍼 상품 등급 계산
        //bufferInboundService.allocateLane();
        return result;
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
    @RequestMapping(
            value = "/defect_report",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "불량보고서 조회 데이터")
    public List<Map<String, Object>> getInboundDefectReport(@RequestBody Map<String, Object> param) {

        // 필수 파라미터 검증
        if (ValueUtil.isEmpty(param.get("cntrNo")) || ValueUtil.isEmpty(param.get("inboundDate"))) {
            throw new ElidomRuntimeException("컨테이너 번호(cntrNo) 또는 입고일자(inboundDate)가 없습니다.");
        }

        return tbMwInboundDeliveryService.getInboundDefectReport(param);
    }
}
