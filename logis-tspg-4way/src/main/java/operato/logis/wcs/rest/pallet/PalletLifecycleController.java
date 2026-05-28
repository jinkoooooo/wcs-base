package operato.logis.wcs.rest.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.pallet.PalletProgressService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

// 파렛트 라이프사이클(셔틀 오더 이력) 조회 전용 컨트롤러.
@RestController
@RequestMapping("/rest/wcs/pallets")
@ServiceDesc(description = "파렛트 라이프사이클 조회")
@RequiredArgsConstructor
public class PalletLifecycleController {

    private final PalletProgressService palletProgressService;

    @GetMapping(value = "/{p}/lifecycle", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> lifecycle(@PathVariable("p") String p) {
        return palletProgressService.listLifecycle(p);
    }
}
