package xyz.elidom.dev.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Order;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.util.QueryUtil;
import xyz.elidom.dev.DevConstants;
import xyz.elidom.dev.entity.DiyService;
import xyz.elidom.dev.entity.ServiceInParam;
import xyz.elidom.dev.entity.ServiceOutParam;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.engine.IScriptEngine;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

import java.sql.Timestamp;
import java.util.*;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/diy_services")
@ServiceDesc(description = "Dynamic Logic Service API")
public class DiyServiceController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() {
        return DiyService.class;
    }

    @GetMapping( produces= MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search Dynamic Service (Pagination) By Search Conditions")
    public Page<?> index(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "select", required = false) String select,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "query", required = false) String query) {
        return this.search(this.entityClass(), page, limit, select, sort, query);
    }

    @SuppressWarnings("unchecked")
    @GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Find one Dynamic Service by ID")
    public DiyService findOne(@PathVariable("id") String id, @RequestParam(required = false) String name) {
        DiyService diyService = null;

        if(SysConstants.SHOW_BY_NAME_METHOD.equalsIgnoreCase(id)) {
            AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_NAME, name);
            diyService = this.selectByCondition(true, DiyService.class, new DiyService(Domain.currentDomainId(), name));
        } else {
            diyService = this.getOne(true, this.entityClass(), id);
        }

        diyService.setServiceInParams((List<ServiceInParam>)this.getServiceParameters(ServiceInParam.class, diyService.getId()));
        diyService.setServiceOutParams((List<ServiceOutParam>)this.getServiceParameters(ServiceOutParam.class, diyService.getId()));
        return diyService;
    }

    @GetMapping(value="/{id}/parameters/{mode}", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Search Dynamic Service In/Out Parameters by ID")
    public List<?> serviceParameters(@PathVariable("id") String id, @PathVariable("mode") String mode) {
        if(SysValueUtil.isEmpty(mode) || SysValueUtil.isEqual(mode, "in")) {
            return this.getServiceParameters(ServiceInParam.class, id);
        } else {
            return this.getServiceParameters(ServiceOutParam.class, id);
        }
    }

    /**
     * Resource Id로 Service In/Out Parameters를 조회
     *
     * @param clazz
     * @param resourceId
     * @return
     */
    @ApiDesc(description="Find Service In/Out Parameters by ID")
    private List<?> getServiceParameters(Class<?> clazz, String resourceId) {
        Query query = new Query();
        query.addFilter(new Filter("resourceId", SysValueUtil.toString(resourceId)));
        query.addFilter(new Filter("resourceType", this.entityClass().getSimpleName()));
        query.addOrder(new Order("rank", true));
        return this.queryManager.selectList(clazz, query);
    }

    @GetMapping(value="/{id}/exist", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Check if Dynamic Service exists By ID")
    public Boolean isExist(@PathVariable("id") String id) {
        return this.isExistOne(this.getClass(), id);
    }

    @PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Check Before Import")
    public List<DiyService> checkImport(@RequestBody List<DiyService> list) {
        for (DiyService item : list) {
            this.checkForImport(DiyService.class, item);
        }

        return list;
    }

    @PostMapping( consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description="Create Dynamic Service")
    public DiyService create(@RequestBody DiyService diyService) {
        return this.createOne(diyService);
    }

    @PutMapping(value="/{id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update Dynamic Service")
    public DiyService update(@PathVariable("id") String id, @RequestBody DiyService diyService) {
        return this.updateOne(diyService);
    }

    @DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Delete Dynamic Service By ID")
    public void delete(@PathVariable("id") String id) {
        this.deleteOne(this.getClass(), id);
    }

    @PostMapping(value="/update_multiple", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Create, Update or Delete multiple Dynamic Service at one time")
    public Boolean multipleUpdate(@RequestBody List<DiyService> diyServiceList) {
        return this.cudMultipleData(this.entityClass(), diyServiceList);
    }

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/{id}/update_multiple_parameters/in", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update Multiple In Parameters")
    public List<ServiceInParam> updateMultipleInParameters(HttpServletRequest req, @PathVariable("id") String id, @RequestBody List<ServiceInParam> inParams) {
        this.cudMultipleData(ServiceInParam.class, inParams);
        return (List<ServiceInParam>)this.getServiceParameters(ServiceInParam.class, id);
    }

    @SuppressWarnings("unchecked")
    @PostMapping(value = "/{id}/update_multiple_parameters/out", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Update multiple out Parameters")
    public List<ServiceOutParam> updateMultipleOutParameters(HttpServletRequest req, @PathVariable("id") String id, @RequestBody List<ServiceOutParam> outParams) {
        this.cudMultipleData(ServiceOutParam.class, outParams);
        return (List<ServiceOutParam>)this.getServiceParameters(ServiceOutParam.class, id);
    }

    @GetMapping(value="/script_type/list", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="Supported Dynmaic Service Script Types")
    public List<String> scriptTypes() {
        List<String> output = new ArrayList<String>();
        output.add("JavaScript");
        output.add("ruby");
        output.add("groovy");
        return output;
    }

    @SuppressWarnings("unchecked")
    @ApiDesc(description = "Query Dynamic Service by Name")
    @GetMapping(value = "/{name}/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object query(@PathVariable("name") String name,
                        @RequestParam(name = "input", required = false) String input,
                        @RequestParam(name = "query", required = false) String query) {
        Map<String, Object> inputMap = new HashMap<String, Object>();

        inputMap.put("domain_id", Domain.currentDomainId());

        if (SysValueUtil.isNotEmpty(input)) {
            inputMap = (Map<String, Object>) super.getJsonParser().parse(input, Map.class);
        }

        if (SysValueUtil.isNotEmpty(query)) {
            inputMap.putAll(SysValueUtil.queryToParamMap(query));
        }

        inputMap = SysValueUtil.removeEmptyValues(inputMap);
        return this.evalScript(name, inputMap);
    }

    @ApiDesc(description = "Read Dynamic Service by Name")
    @GetMapping(value = "/{name}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object read(@PathVariable("name") String name, @RequestParam Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<String, Object>();
        }

        params = SysValueUtil.removeEmptyValues(params);
        params.put("domain_id", Domain.currentDomain().getId());

        return this.evalScript(name, params);
    }

    /**
     * 추가 - 페이지네이션 처리
     *
     * @param name
     * @param params
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @ApiDesc(description = "Read Dynamic Service by Name")
    @GetMapping(value = "/{name}/read_by_pagination", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object readByPagination(@PathVariable("name") String name, @RequestParam Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<String, Object>();
        }

        String queryStr = SysValueUtil.toString(params.get("query"));
        List<Map> paramList = super.getJsonParser().parse(queryStr, List.class);
        Map<String, Object> input = new HashMap<String, Object>();

        if (SysValueUtil.isNotEmpty(paramList)) {
            for (Map data : paramList) {
                if (data.get("operator").equals("between")) {
                    String valueTemp = data.get("value").toString();

                    String dataValue[] = valueTemp.split(",");
                    String datakey1 = "from_date";
                    String datakey2 = "to_date";

                    input.put(datakey1, dataValue[0]);
                    input.put(datakey2, dataValue[1]);
                }

                input.put(data.get("name").toString(), data.get("value"));
            }
        }

        String sortStr = SysValueUtil.toString(params.get("sort"));
        List<Map> sortList = super.getJsonParser().parse(sortStr, List.class);

        if (SysValueUtil.isNotEmpty(sortList)) {
            String sortValue = "order by ";
            for (Map sort : sortList) {
                Boolean ascending = (Boolean) sort.get("ascending");
                if (ascending) {
                    sortValue = sortValue + " " + sort.get("field") + " asc, ";
                } else {
                    sortValue = sortValue + " " + sort.get("field") + " desc, ";
                }
            }
            sortValue = sortValue.substring(0, sortValue.length() - 2);

            input.put("sortString", sortValue);
        }

        input.put("domain_id", Domain.currentDomain().getId());
        input.put("page", params.get("page"));
        input.put("limit", params.get("limit"));
        input.put("_pagination_", true);
        input = SysValueUtil.removeEmptyValues(input);

        return this.evalScript(name, input);
    }

    @SuppressWarnings("unchecked")
    @ApiDesc(description = "Execute Dynamic Service by Name")
    @PostMapping(value = "/{name}/shoot", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object shoot(@PathVariable("name") String name, @RequestBody Map<String, Object> map) {
        Map<String, Object> inputMap = (Map<String, Object>) map.get("input");
        if (inputMap == null) {
            inputMap = new HashMap<String, Object>();
        }

        inputMap.put("domain_id", Domain.currentDomainId());
        Object result = this.evalScript(name, inputMap);

        if (result == null) {
            return null;
        } else if (result instanceof String || result instanceof Boolean || result instanceof Number
                || result instanceof Date || result instanceof Timestamp) {
            return SysValueUtil.newMap("result", result.toString());
        } else {
            return result;
        }
    }

    /**
     * Invoke script engine
     *
     * @param name
     * @param inputMap
     * @return
     */
    @ApiDesc(description="Invoke script engine")
    private Object evalScript(String name, Map<String, Object> inputMap) {
        DiyService conds = new DiyService(Domain.currentDomainId(), name);
        DiyService diyService = (DiyService) this.selectByCondition(true, this.entityClass(), conds);
        String scriptType = diyService.getScriptType();

        if (SysValueUtil.isEqual(scriptType, DevConstants.DIY_TYPE_DSL)) {
            return this.processDsl(diyService.getLangType(), diyService.getServiceLogic(), inputMap);

        } else if (SysValueUtil.isEqual(scriptType, DevConstants.DIY_TYPE_DSL_SQL)) {
            return this.processDslSql(diyService.getLangType(), diyService.getServiceLogic(), inputMap);

        } else if (SysValueUtil.isEqual(scriptType, DevConstants.DIY_TYPE_SQL)) {
            return this.processSql(diyService.getServiceLogic(), inputMap);

        } else {
            String defaultMessage = "Not Supported {0} Type. [{1}]";
            throw new ElidomServiceException(SysMessageConstants.NOT_SUPPORTED_TYPE, defaultMessage, MessageUtil.params("Script", scriptType));
        }
    }

    /**
     * DSL 타입 실행
     *
     * @param langType
     * @param logic
     * @param inputMap
     * @return
     */
    @ApiDesc(description="Invoke DSL Type")
    private Object processDsl(String langType, String logic, Map<String, Object> inputMap) {
        IScriptEngine scriptEngine = BeanUtil.get(IScriptEngine.class);
        return scriptEngine.runScript(langType, logic, inputMap);
    }

    /**
     * DSL-SQL 타입 실행
     *
     * @param langType
     * @param logic
     * @param inputMap
     * @return
     */
    @ApiDesc(description="Invoke DSL-SQL Type")
    private Object processDslSql(String langType, String logic, Map<String, Object> inputMap) {
        IScriptEngine scriptEngine = BeanUtil.get(IScriptEngine.class);
        Object sql = scriptEngine.runScript(langType, logic, inputMap);
        return this.querySql(sql.toString(), inputMap);
    }

    /**
     * SQL 타입 실행
     *
     * @param sql
     * @param inputMap
     * @return
     */
    @ApiDesc(description="Inoke SQL Type")
    private Object processSql(String sql, Map<String, Object> inputMap) {
        return this.querySql(sql.toString(), inputMap);
    }

    /**
     * Query SQL & Return
     *
     * @param sql
     * @param inputMap
     * @return
     */
    @ApiDesc(description="Query SQL and Return")
    private Object querySql(String sql, Map<String, Object> inputMap) {
        int page = 0;
        int limit = 0;

        if (!inputMap.containsKey("domain_id")) {
            inputMap.put("domain_id", Domain.currentDomain().getId());
        }

        if (inputMap.containsKey("page")) {
            String pageStr = inputMap.get("page").toString();
            pageStr = pageStr.replace(".0", "");
            page = Integer.parseInt(pageStr);
        }

        if (inputMap.containsKey("limit")) {
            String limitStr = inputMap.get("limit").toString();
            limitStr = limitStr.replace(".0", "");
            limit = Integer.parseInt(limitStr);
        }

        boolean paginateFlag =
                inputMap.containsKey("_pagination_") ? SysValueUtil.toBoolean(inputMap.get("_pagination_")) : false;

        if(SysValueUtil.isNotEmpty(inputMap)) {
            sql = QueryUtil.printQuery(sql, inputMap, true);
        }

        if(paginateFlag) {
            return this.queryManager.selectPageBySql(sql, inputMap, Map.class, page, limit);
        } else {
            return this.queryManager.selectListBySql(sql, inputMap, Map.class, page, limit);
        }
    }
}