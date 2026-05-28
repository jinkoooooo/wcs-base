package operato.logis.asrs.biz.item;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.query.item.ItemMasterQueryService;
import operato.logis.asrs.query.item.model.ItemMasterDetailView;
import operato.logis.asrs.query.item.model.ItemMasterListView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 상품마스터 조회 API.
 *
 * 역할:
 * - 상품 목록 조회
 * - 상품 상세 조회
 */
@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/items")
@ServiceDesc(description = "AisleCore Item Master Query API")
public class AcItemMasterQueryController {

    private final ItemMasterQueryService itemMasterQueryService;

    /**
     * 상품 목록 조회.
     *
     * 예:
     * GET /rest/aislecore/items?itemCode=AC-ITEM&activeYn=Y
     */
    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find item master list")
    public List<ItemMasterListView> findItems(
            @RequestParam(name = "itemCode", required = false) String itemCode,
            @RequestParam(name = "itemName", required = false) String itemName,
            @RequestParam(name = "categoryCode", required = false) String categoryCode,
            @RequestParam(name = "storageTempType", required = false) String storageTempType,
            @RequestParam(name = "activeYn", required = false) String activeYn
    ) {
        return itemMasterQueryService.findItemMasters(
                itemCode,
                itemName,
                categoryCode,
                storageTempType,
                activeYn
        );
    }

    /**
     * 상품 단건 상세 조회.
     *
     * 외부 조회 key는 itemCode 기준 사용
     */
    @RequestMapping(
            value = "/{itemCode}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find item master detail by itemCode")
    public ItemMasterDetailView findItemDetail(@PathVariable("itemCode") String itemCode) {
        return itemMasterQueryService.findItemMasterDetail(itemCode);
    }
}