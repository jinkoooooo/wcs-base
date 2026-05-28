package operato.logis.asrs.biz.item;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.query.item.ItemCategoryQueryService;
import operato.logis.asrs.query.item.model.ItemCategoryOptionView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 상품 카테고리 옵션 조회 API.
 */
@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/item-categories")
@ServiceDesc(description = "AisleCore Item Category Query API")
public class AcItemCategoryQueryController {

    private final ItemCategoryQueryService itemCategoryQueryService;

    /**
     * 활성 카테고리 목록 조회.
     */
    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find active item categories")
    public List<ItemCategoryOptionView> findActiveCategories() {
        return itemCategoryQueryService.findActiveCategories();
    }
}