package operato.logis.samsung.rest.wms;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.sys.system.service.AbstractRestService;

/**
 * WCS -> WMS 보고 컨트롤러
 */
@RestController
@RequestMapping("/rest/wms/report") // TODO: endpoint 확정
@RequiredArgsConstructor
public class WmsReportController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() { return WmsReportController.class; }

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(WmsReportController.class);

    /**
     * WCS 입고 실적보고 I/F
     */
}
