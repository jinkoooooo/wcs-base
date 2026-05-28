package operato.logis.samsung.service.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.connector.api.service.ExternalApiService;
import operato.logis.samsung.WcsConstants;
import operato.logis.samsung.WcsUtils;
import operato.logis.samsung.entity.xyz.TbMwIfXyzPalletExchange;
import operato.logis.samsung.service.mw.TbMwChuteManagementService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;

@Service
@RequiredArgsConstructor
public class XyzPalletService extends AbstractQueryService {

    private final ExternalApiService externalApiService;
    private final TbMwChuteManagementService tbMwChuteManagementService;

    /**
     * XYZ -> MW Pallet 교체 완료 보고
     */
    public CommonApiResponse receivePalletExchange(TbMwIfXyzPalletExchange pallet) {
        logger.info("[XYZ] Pallet Exchange Receive : {}", WcsUtils.logRequestBody(pallet));
        // 이력 생성
        pallet.setMethod("exchange");
        this.queryManager.insert(pallet);

        // Chute Pallet 교체 정보 반영
        tbMwChuteManagementService.updatePalletExchange(pallet.getPalletId(), pallet.getPalletSequence());

        return CommonApiResponse.success();
    }

    /**
     * XYZ -> MW Pallet 배출 완료 보고
     */
    public CommonApiResponse receivePalletEmission(TbMwIfXyzPalletExchange pallet) {
        logger.info("[XYZ] Pallet Emission Receive : {}", WcsUtils.logRequestBody(pallet));
        // 이력 생성
        pallet.setMethod("emission");
        this.queryManager.insert(pallet);

        // Chute Pallet 교체 정보 반영
        tbMwChuteManagementService.updatePalletExchange(pallet.getPalletId(), "");

        return CommonApiResponse.success();
    }

    /**
     * MW -> XYZ Pallet 강제 배출 요청
     *
     * @param palletId 교체할 Pallet Conveyor 번호
     * @return XYZ에서 반환하는 CommonApiResponse
     */
    public CommonApiResponse sendPalletExchange(String palletId) {
        // API Body 생성
        TbMwIfXyzPalletExchange exchange = new TbMwIfXyzPalletExchange();
        exchange.setPalletId(palletId);

        // API URL 설정
        String xyzServerIP = WcsConstants.XYZ_SERVER_URL;
        String endPoint = "/adaptor/api/MW/force_end";
        String url = xyzServerIP + endPoint;

        // XYZ 응답 수신
        CommonApiResponse result = new CommonApiResponse();
        try {
            logger.info("[XYZ] Pallet Request API Body : {}", WcsUtils.logRequestBody(exchange));
            Mono<CommonApiResponse> responseMono = externalApiService.post(url, exchange, CommonApiResponse.class);
            result = responseMono.block();
            logger.info("[XYZ] Pallet Request API Result : {}", WcsUtils.logRequestBody(result));
        }
        catch (ElidomRuntimeException e) {
            logger.error("[XYZ] Pallet {} 강제 배출 중 에러 발생 : {}", palletId, e.getMessage());
        }

        // 이력 생성
        exchange.setMethod("request");
        this.queryManager.insert(exchange);

        return result;
    }
}