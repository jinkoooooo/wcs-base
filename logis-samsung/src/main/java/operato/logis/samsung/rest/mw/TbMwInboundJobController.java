package operato.logis.samsung.rest.mw;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.ProcessStatus;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.entity.mw.TbMwInboundJob;
import operato.logis.samsung.entity.mw.TbMwXyzOrder;
import operato.logis.samsung.service.mw.TbMwInboundJobService;
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
@RequestMapping("/rest/tb_mw_inbound_job")
@ServiceDesc(description="TbMwInboundJob Service API")
public class TbMwInboundJobController extends AbstractRestService {

	private final TbMwInboundJobService tbMwInboundJobService;
	private final TbMwXyzOrderService tbMwXyzOrderService;

	@Override
	protected Class<?> entityClass() {
		return TbMwInboundJob.class;
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
	public TbMwInboundJob findOne(@PathVariable("id") String id) {
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
	public TbMwInboundJob create(@RequestBody TbMwInboundJob input) {
		return this.createOne(input);
	}

	@RequestMapping(value="/{id}", method=RequestMethod.PUT, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update")
	public TbMwInboundJob update(@PathVariable("id") String id, @RequestBody TbMwInboundJob input) {
		return this.updateOne(input);
	}

	@RequestMapping(value="/{id}", method=RequestMethod.DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@RequestMapping(value="/update_multiple", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple at one time")
	public Boolean multipleUpdate(@RequestBody List<TbMwInboundJob> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}

	@RequestMapping(
			value = "/inbound_job_info",
			method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiDesc(description = "특정 주문 정보 조회(날짜/주문번호/컨테이너번호)")
	public List<TbMwInboundJob> getInboundDeliveryInfo(@RequestBody TbMwInboundDelivery req) {

		return tbMwInboundJobService.getInboundJobByDate(req.getInboundDate());
	}

    @RequestMapping(
            value = "/inbound_job_info_date",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "특정 주문 정보 조회(날짜/주문번호/컨테이너번호)")
    public List<TbMwInboundJob> getInboundDeliveryInfoDate(@RequestBody Map<String, Object> reqMap) {
        String startDate = (String) reqMap.get("startDate");
        String endDate = (String) reqMap.get("endDate");

        return tbMwInboundJobService.getInboundJobByDateSdl(startDate, endDate);
    }

	@RequestMapping(value="/start_delivery_job", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="입고 작업 시작")
	public Map<String, Object> startDelivery(@RequestBody TbMwInboundJob data) {
		return tbMwInboundJobService.getStartDelivery(data);
	}

	@RequestMapping(value="/pause_delivery_job", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="입고 작업 일시정지")
	public Map<String, Object> pauseDelivery(@RequestBody TbMwInboundDelivery data) {
		Map<String, Object> result = tbMwInboundJobService.getPauseDelivery(data);
		if (!result.get("code").equals(0)) {
			return result;
		}

		// Pallet Conveyor 초기화
		List<TbMwXyzOrder> xyzOrderList = tbMwXyzOrderService.getOrderListByDeliveryInfo(data.getBlNo(), data.getCntrNo());
		for (TbMwXyzOrder xyzOrder : xyzOrderList) {
			if (ProcessStatus.ORDER_START.value().equals(xyzOrder.getProcessStatus())) {
				tbMwXyzOrderService.abortOrder(xyzOrder);
			}
		}

		return result;
	}

	@RequestMapping(value="/abort_delivery_job", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="입고 작업 강제종료")
	public Map<String, Object> abortDelivery(@RequestBody TbMwInboundDelivery data) {
		Map<String, Object> result = tbMwInboundJobService.getAbortDelivery(data);
		if (!result.get("code").equals(0)) {
			return result;
		}

		// Pallet Conveyor 초기화
		List<TbMwXyzOrder> xyzOrderList = tbMwXyzOrderService.getOrderListByDeliveryInfo(data.getBlNo(), data.getCntrNo());
		for (TbMwXyzOrder xyzOrder : xyzOrderList) {
			if (ProcessStatus.ORDER_START.value().equals(xyzOrder.getProcessStatus())) {
				tbMwXyzOrderService.abortOrder(xyzOrder);
			}
		}

		return result;
	}

    @RequestMapping(
            value="/delete_delivery_job",
            method=RequestMethod.POST,
            consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description="컨테이너 작업 및 입고 상세 내역 일괄 삭제")
    public Map<String, Object> deleteDeliveryJob(@RequestBody TbMwInboundJob req) {

        return tbMwInboundJobService.deleteDeliveryJob(req);
    }
}