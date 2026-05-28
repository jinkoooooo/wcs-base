package operato.logis.connector.core.rest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import operato.logis.connector.core.event.EventDirection;
import operato.logis.connector.core.event.IntegrationEvent;
import operato.logis.connector.core.event.SystemType;
import operato.logis.connector.core.mapper.IMappingRegistry;
import operato.logis.connector.http.mapper.HttpMappingMetaData;
import operato.logis.connector.sap.mapper.SapMappingMetaData;
import operato.logis.connector.sap.model.SapPayload;
import operato.logis.connector.sap.model.SapRequestDto;
import xyz.anythings.sys.rest.DynamicControllerSupport;
import xyz.elidom.sys.model.BaseResponse;

@RestController
@Transactional
@RequestMapping("/rest/integration")
public class IntegrationController extends DynamicControllerSupport {
	@Autowired
    private ApplicationEventPublisher publisher;
	
	@Autowired
	private IMappingRegistry<HttpMappingMetaData> httpMappingRegistry;

    // SAP registry는 있을 수도/없을 수도
    @Autowired(required = false)
    private IMappingRegistry<SapMappingMetaData> sapMappingRegistry;

    @Value("${connector.sap.enabled:false}")
    private boolean sapEnabled;

    /** 건강 체크 */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String index() {
        return "true";
    }

    /** 1) SAP RFC 연동 (결과 대기) */
    @PostMapping(value = "/sap/call", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> callSapRfc(@RequestBody SapRequestDto dto) {

        // SAP 꺼져있으면 아예 막기
        if (!sapEnabled || sapMappingRegistry == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new BaseResponse(false, "SAP 커넥터 비활성 상태입니다. (connector.sap.enabled=false)", null));
        }

        SapPayload payload = new SapPayload(dto.getIsSend(), dto.getImportParams(), dto.getTableDataMap());

        IntegrationEvent<SapPayload> event = new IntegrationEvent<>(
                7L,  // domainId: 상황에 따라 dto에서 받게 변경 가능
                SystemType.SAP,
                dto.getIsSend() ? EventDirection.SEND : EventDirection.RECEIVE,
                dto.getFunctionName(),
                payload
        );

        publisher.publishEvent(event);

        try {
            BaseResponse result = event.getFutureResponse().get(10, TimeUnit.SECONDS);
            return ResponseEntity.status(result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SAP 처리 실패 : " + e.getMessage());
        }
    }

    /** 2) HTTP Inbound 연동 (결과 대기, Map payload) */
    @PostMapping(value = "/{system}/{domainId}/{*url}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> httpInbound(@PathVariable String system, @PathVariable Long domainId, @PathVariable String url,
                                         @RequestBody Map<String, Object> payload) {

        IntegrationEvent<Map<String, Object>> event = new IntegrationEvent<>(
                domainId,
                SystemType.valueOf(system.toUpperCase()),
                EventDirection.RECEIVE,
                "/" + url,
                payload
        );

        publisher.publishEvent(event);

        try {
            BaseResponse result = event.getFutureResponse().get(5, TimeUnit.SECONDS);
            return ResponseEntity.status(result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("HTTP 처리 실패 : " + e.getMessage());
        }
    }

    /** 3) 매핑 핫-리로드 */
    @PostMapping("/reload-mappings")
    public ResponseEntity<String> reloadMappings() {
        httpMappingRegistry.reload();

        // SAP는 있을 때만
        if (sapMappingRegistry != null) {
            sapMappingRegistry.reload();
        }

        return ResponseEntity.ok("Mappings reloaded successfully.");
    }
}