package operato.logis.asrs.biz.item;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.item.ItemInboundPolicyFacadeCore;
import operato.logis.asrs.dto.response.ItemInboundPolicyResult;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/items")
@ServiceDesc(description = "AisleCore Item Policy API")
public class AcItemPolicyController {

    private final ItemInboundPolicyFacadeCore itemInboundPolicyFacadeCore;

    @RequestMapping(
            value = "/inbound-policy",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiDesc(description = "Find inbound item policy by areaCode and itemCode")
    public ItemInboundPolicyResult getInboundPolicy(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("itemCode") String itemCode
    ) {
        return itemInboundPolicyFacadeCore.getInboundPolicy(areaCode, itemCode);
    }
}