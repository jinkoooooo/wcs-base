package operato.logis.samsung.rest.wms;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.wms.InboundDeliveryRequest;
import operato.logis.samsung.dto.wms.WmsIFResponse;
import operato.logis.samsung.service.wms.WmsInboundContainerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.sys.system.service.AbstractRestService;

/**
 * WMS -> WCS 지시 컨트롤러
 */
@RestController
@RequestMapping("/rest/wms/order") // TODO: endpoint 확정
@RequiredArgsConstructor
public class WmsOrderController extends AbstractRestService {

    @Override
    protected Class<?> entityClass() { return WmsOrderController.class; }

    private final WmsInboundContainerService inboundContainerService;

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(WmsOrderController.class);

    /**
     * WCS 입고 컨테이너 수신
     * - todo: endpoint 확정
     */
    @PostMapping(value = "/inbound_delivery", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Legacy 서버 입고 컨테이너 수신")
    public WmsIFResponse receiveContainerRequest(@RequestBody InboundDeliveryRequest request) {
        request.setLcNm("samsung");
        return inboundContainerService.receive(request);
    }
}