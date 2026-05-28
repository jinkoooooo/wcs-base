package operato.logis.asrs.biz.item;

import operato.logis.asrs.core.item.ItemMasterCommandCore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.dto.request.ItemActiveToggleRequest;
import operato.logis.asrs.dto.request.ItemMasterBulkUpsertRequest;
import operato.logis.asrs.dto.request.ItemMasterUpsertRequest;
import operato.logis.asrs.dto.response.ItemMasterBulkSaveResult;
import operato.logis.asrs.dto.response.ItemMasterSaveResult;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 상품마스터 명령 API.
 *
 * 역할:
 * - 등록
 * - 수정
 * - 삭제
 * - 사용여부 변경
 * - bulk upsert
 */
@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/items")
@ServiceDesc(description = "AisleCore Item Master Command API")
public class AcItemMasterCommandController {

    private final ItemMasterCommandCore itemMasterCommandCore;

    /**
     * 상품 신규 등록.
     */
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiDesc(description = "Create item master")
    public ItemMasterSaveResult createItem(@RequestBody ItemMasterUpsertRequest request) {
        return itemMasterCommandCore.createItemMaster(request);
    }

    /**
     * 상품 수정.
     *
     * 외부 식별자는 itemCode 기준
     */
    @RequestMapping(
            value = "/{itemCode}",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Update item master by itemCode")
    public ItemMasterSaveResult updateItem(@PathVariable("itemCode") String itemCode,
                                           @RequestBody ItemMasterUpsertRequest request) {
        return itemMasterCommandCore.updateItemMaster(itemCode, request);
    }

    /**
     * 상품 삭제.
     */
    @RequestMapping(
            value = "/{itemCode}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Delete item master by itemCode")
    public void deleteItem(@PathVariable("itemCode") String itemCode) {
        itemMasterCommandCore.deleteItemMaster(itemCode);
    }

    /**
     * 사용 여부 변경.
     *
     * 주의:
     * - 프론트 defHttp 래퍼가 patch() 를 지원하지 않아 PUT 으로 통일
     */
    @RequestMapping(
            value = "/{itemCode}/active",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Change item activeYn by itemCode")
    public ItemMasterSaveResult changeActiveYn(@PathVariable("itemCode") String itemCode,
                                               @RequestBody ItemActiveToggleRequest request) {
        return itemMasterCommandCore.changeItemActiveYn(itemCode, request);
    }

    /**
     * 상품 일괄 저장.
     *
     * 정책:
     * - itemCode 기준 존재하면 update
     * - 없으면 create
     */
    @RequestMapping(
            value = "/bulk-upsert",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Bulk upsert item masters")
    public ItemMasterBulkSaveResult bulkUpsert(@RequestBody ItemMasterBulkUpsertRequest request) {
        return itemMasterCommandCore.bulkUpsertItemMasters(request);
    }
}