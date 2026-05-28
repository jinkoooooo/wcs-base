package operato.logis.samsung.rest.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.samsung.entity.xyz.TbMwIfXyzPalletExchange;
import operato.logis.samsung.service.xyz.XyzPalletService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/xyz/pallet")
@ServiceDesc(description="XYZ Pallet 수신 API")
public class XyzPalletController {

    private final XyzPalletService xyzPalletService;

    @RequestMapping(value="/exchange", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="XYZ Pallet 교체 완료 수신")
    public CommonApiResponse exchange(@RequestBody TbMwIfXyzPalletExchange pallet) {
        return xyzPalletService.receivePalletExchange(pallet);
    }

    @RequestMapping(value="/emission", method= RequestMethod.POST, consumes= MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description="XYZ Pallet 만재 배출 수신")
    public CommonApiResponse emission(@RequestBody TbMwIfXyzPalletExchange pallet) {
        return xyzPalletService.receivePalletEmission(pallet);
    }
}