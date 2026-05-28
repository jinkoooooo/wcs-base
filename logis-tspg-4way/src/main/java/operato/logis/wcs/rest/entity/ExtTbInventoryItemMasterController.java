package operato.logis.wcs.rest.entity;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.rest.common.AbstractExtRestService;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tbinventoryitemmaster")
@ServiceDesc(description = "ExtTbInventoryItemMaster Service API")
public class ExtTbInventoryItemMasterController extends AbstractExtRestService {

    private static final Logger logger = LoggerFactory.getLogger(ExtTbInventoryItemMasterController.class);

    private final InventoryItemMasterRepository repository;

    @Override
    protected Class<?> entityClass() {
        return ExtTbInventoryItemMaster.class;
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Search (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "select", required = false) String select,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "query", required = false) String query) {
        return this.search(this.entityClass(), page, limit, select, sort, query);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Find one by ID")
    public ExtTbInventoryItemMaster findOne(@PathVariable("id") String id) {
        return this.getOne(this.entityClass(), id);
    }

    @RequestMapping(value = "/{id}/exist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Check exists By ID")
    public Boolean isExist(@PathVariable("id") String id) {
        return this.isExistOne(this.entityClass(), id);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Create")
    public ExtTbInventoryItemMaster create(@RequestBody ExtTbInventoryItemMaster input) {
        return this.createOne(input);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Update")
    public ExtTbInventoryItemMaster update(@PathVariable("id") String id, @RequestBody ExtTbInventoryItemMaster input) {
        return this.updateOne(input);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Delete (soft) - deleted_at set + fetched=false (상위 pull 재동기화)")
    public void delete(@PathVariable("id") String id,
                       @RequestParam(name = "reason", required = false) String reason) {
        // 물리삭제 대신 soft delete (상위 pull 로 삭제 동기화)
        int affected = repository.softDelete(id, reason);
        logger.warn("[ Inventory ][ ItemMaster ] soft delete - id={}, affected={}", id, affected);
    }

    @RequestMapping(value = "/update_multiple", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Create, Update or Delete multiple at one time")
    public Boolean multipleUpdate(@RequestBody List<ExtTbInventoryItemMaster> list) {
        logger.info("[ Inventory ][ ItemMaster ] multipleUpdate - count={}", list == null ? 0 : list.size());
        return this.cudMultipleData(this.entityClass(), list);
    }

    /**
     * /update_multiple 과 동일한 일괄 CUD 이되, 이 엔드포인트만 특별히 두 규칙을 강제한다.
     *
     * 1) create/update 시 fetched 를 항상 false 로 강제 → 차회 상위(LIMS/MES) pull 재전송.
     * 2) delete 는 물리삭제 금지 → soft delete(deleted_at set + fetched=false)로 보관·재동기화.
     *
     * update 는 grid 가 보낸 부분 payload 를 그대로 전체 update 하면 미전송 컬럼이 null 로 덮여
     * not-null(item_owner 등) 제약을 위반하므로, 기존 row 를 읽어 non-null 필드만 머지한 뒤 update 한다.
     */
    @RequestMapping(value = "/update_multiple_soft", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Create/Update or Soft-Delete multiple at one time - create/update 는 fetched=false 강제, 삭제는 soft delete")
    public Boolean multipleUpdateSoft(@RequestBody List<ExtTbInventoryItemMaster> list) {
        logger.info("[ Inventory ][ ItemMaster ] multipleUpdateSoft - count={}", list == null ? 0 : list.size());
        // 빈 요청은 무영향 성공 처리
        if (ValueUtil.isEmpty(list)) return Boolean.TRUE;

        // cud_flag 별 분기 - delete=soft delete, update=머지 후 fetched=false, create=fetched=false
        for (ExtTbInventoryItemMaster item : list) {
            String cudFlag = item.getCudFlag_();

            if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_DELETE)) {
                // 물리삭제 대신 tombstone + 상위 pull 재동기화 트리거 (deleted_at set + fetched=false)
                int affected = repository.softDelete(item.getId(), null);
                logger.warn("[ Inventory ][ ItemMaster ] soft delete - id={}, affected={}", item.getId(), affected);

            } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_UPDATE)) {
                // 기존 row 에 들어온 non-null 필드만 머지 → 부분 payload 로 not-null 컬럼 null 화 방지
                ExtTbInventoryItemMaster current = repository.findById(item.getId());
                if (ValueUtil.isEmpty(current)) continue;                 // 이미 사라진 row 는 무시
                BeanUtils.copyProperties(item, current, nullPropertyNames(item));
                current.setFetched(false);                                // 어떤 상황에서든 미인수로 강제
                repository.update(current);

            } else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_CREATE)) {
                // 신규도 항상 미인수 상태로 진입
                item.setFetched(false);
                repository.insert(item);
            }
        }
        return Boolean.TRUE;
    }

    /** 값이 null 인 property 명 배열 - BeanUtils.copyProperties 의 ignore 대상(non-null 필드만 머지). */
    private static String[] nullPropertyNames(Object source) {
        BeanWrapper wrapper = new BeanWrapperImpl(source);
        return Arrays.stream(wrapper.getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .filter(name -> wrapper.getPropertyValue(name) == null)
                .toArray(String[]::new);
    }

    /**
     * 상위(MES/ERP) Pull endpoint - auto-ack.
     *
     * 호출 시점에 미인수 마스터 목록을 반환하면서 해당 row 들을 fetched=true 로 일괄 마크.
     * 즉 호출 자체가 인수 확정 신호이며 별도 acknowledge API 가 필요 없다.
     * insert/update 발생 시 fetched=false 로 복귀하여 차회 pull 에서 재전송된다.
     */
    @RequestMapping(value = "/unfetched", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "상위 미인수 마스터 목록 - 호출 즉시 fetched=true 자동 인수")
    public List<ExtTbInventoryItemMaster> listUnfetched() {
        List<ExtTbInventoryItemMaster> rows = repository.findUnfetched();
        if (ValueUtil.isEmpty(rows)) return rows;
        List<String> ids = rows.stream().map(ExtTbInventoryItemMaster::getId).toList();
        int affected = repository.markFetchedAll(ids);
        logger.info("[ Inventory ][ ItemMaster ] pull returned={}, auto-acked={}", rows.size(), affected);
        // 응답 row 의 fetched 도 메모리상 true 로 맞춰 클라이언트 혼선 방지
        rows.forEach(r -> r.setFetched(Boolean.TRUE));
        return rows;
    }

    @RequestMapping(value = "/{id}/fetched", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "수동 인수 표시 (보조) - 일반 흐름은 GET /unfetched 의 auto-ack")
    public Map<String, Object> markFetched(@PathVariable("id") String id) {
        int affected = repository.markFetched(id);
        return Map.of("success", affected > 0, "affected", affected, "id", id);
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Search (Pagination) By POST")
    public Page<?> searchPost(@RequestBody Map<String, Object> params) {
        Integer page = ValueUtil.isNotEmpty(params.get("page")) ? Integer.valueOf(params.get("page").toString()) : null;
        Integer limit = ValueUtil.isNotEmpty(params.get("limit")) ? Integer.valueOf(params.get("limit").toString()) : null;
        String select = (String) params.get("select");
        String sort = (String) params.get("sort");

        String queryStr = null;
        if (ValueUtil.isNotEmpty(params.get("query"))) {
            Object queryObj = params.get("query");
            queryStr = (queryObj instanceof String s) ? s : new Gson().toJson(queryObj);
        }

        return this.search(this.entityClass(), page, limit, select, sort, queryStr);
    }
}