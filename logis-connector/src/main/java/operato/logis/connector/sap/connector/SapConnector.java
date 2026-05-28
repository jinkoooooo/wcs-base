package operato.logis.connector.sap.connector;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.connector.core.connector.IConnector;
import operato.logis.connector.core.event.EventDirection;
import operato.logis.connector.core.event.IntegrationEvent;
import operato.logis.connector.sap.mapper.SapMappingMetaData;
import operato.logis.connector.sap.model.SapPayload;
import operato.logis.connector.sap.service.RfcDataProcessorService;
import operato.logis.connector.sap.util.RfcFieldLoader;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.elidom.sys.model.BaseResponse;

@Component
public class SapConnector implements IConnector<SapPayload> {
	@Autowired
	private RfcDataProcessorService rfcService;
	@Autowired
	private RfcFieldLoader loader;
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public boolean supports(String system, EventDirection dir) {
		return "SAP".equalsIgnoreCase(system);
	}

	@Override
	public Mono<BaseResponse> handle(IntegrationEvent<SapPayload> event) {
		return Mono.fromCallable(() -> {
			SapPayload payload = event.getPayload();
			SapMappingMetaData meta = loader.loadMetaData(event.getEndpoint());

			meta.setImportParams(payload.getImportParams());
			meta.setIsSend(payload.isSend());
			if (Boolean.TRUE.equals(meta.getIsSend()) && payload.getTableDataMap() != null) {
				meta.setTableDataMap(payload.getTableDataMap());
			}
			try {
				rfcService.handleRfcCall(meta);
//				rfcService.saveResultToJson(meta);

				// [수신] GT_TAB만 리턴
				Object gtTabResult = null;
				if (meta.getTableDataMap() != null) {
					gtTabResult = meta.getTableDataMap().get("GT_TAB");
				}
				return new BaseResponse(true, "SAP 수신 성공", gtTabResult);

			} catch (Exception e) {
				log.error("[{}] SAP RFC 실패", meta.getFunctionName(), e);

				// 송신 실패, 수신 실패 모두 동일하게 처리
				Map<String, String> fail = Map.of("O_MSGTY", "E", "O_MSGLIN", e.getMessage());
				meta.setExportParams(fail);
				return new BaseResponse(false, e.getMessage(), null);
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}
}