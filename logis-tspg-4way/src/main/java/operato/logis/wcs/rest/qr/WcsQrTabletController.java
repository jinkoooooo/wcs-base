package operato.logis.wcs.rest.qr;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.inventory.QrTabletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;

/**
 * QR 태블릿 조회 API.
 *
 * 박스/파렛트 바코드 단일 진입점. 박스는 "B-" prefix.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/wcs/qr", produces = MediaType.APPLICATION_JSON_VALUE)
@ServiceDesc(description = "QR 태블릿 조회 API")
public class WcsQrTabletController {

    private final QrTabletService qrTabletService;

    @GetMapping("/scan")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> scan(@RequestParam("code") String code) {
        return qrTabletService.scan(code);
    }
}
