package operato.logis.wcs.rest.inbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.generator.PalletBarcodeGenerator;
import operato.logis.wcs.service.impl.query.inbound.InboundRegistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * 입고등록 REST Controller
 *
 * - GET  /rest/wcs/inbound/regist          : 입고등록 내역 조회 (query/sort/page/limit)
 * - POST /rest/wcs/inbound/regist/register : 입고 지시 등록 (WCS Facade 호출)
 * - POST /rest/wcs/inbound/regist/delete   : 입고등록 내역 삭제
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/inbound/regist")
@ServiceDesc(description = "InboundRegist Service API")
public class InboundRegistController {

    private static final Logger logger = LoggerFactory.getLogger(InboundRegistController.class);

    private final InboundRegistService inboundRegistService;

    private final PalletBarcodeGenerator palletBarcodeGenerator;

    /**
     * 입고등록 내역 조회 (페이징)
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고등록 내역 조회")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Inbound ][ Regist ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return inboundRegistService.search(query, sort, page, limit);
    }

    /**
     * 입고 지시 등록
     * - body: { eqGroupId, ownerCode, items: [{itemCode, lotNo, qty, uom}] }
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고 지시 등록")
    public Map<String, Object> register(@RequestBody Map<String, Object> requestData) {
        logger.info("[ Inbound ][ Regist ] register - {}", requestData);
        return inboundRegistService.register(requestData);
    }

    /**
     * 입고등록 내역 삭제
     * - body: { hostOrderKeys: ["UI-INB-xxx", ...] }
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고등록 내역 삭제")
    public Map<String, Object> delete(@RequestBody DeleteRequest requestData) {
        List<String> hostOrderKeys = requestData.getHostOrderKeys();
        logger.info("[ Inbound ][ Regist ] delete - {}", hostOrderKeys);
        return inboundRegistService.deleteByHostOrderKeys(hostOrderKeys);
    }

    /**
     * 신규 파렛트 바코드 발번
     * - 포맷: {YYYYMMDD}_{SEQ6}  (예: 20260513_000001)
     * - UI 입고등록 화면에서 [추가] 버튼 클릭 시 호출
     */
    @RequestMapping(value = "/pallet-barcode", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "파렛트 바코드 발번")
    public Map<String, Object> generatePalletBarcode() {
        String palletBarcode = palletBarcodeGenerator.generate();
        logger.info("[ Inbound ][ Regist ] pallet barcode issued - {}", palletBarcode);
        return Map.of("palletBarcode", palletBarcode);
    }

    // 삭제 요청 바디 (hostOrderKeys 배열)
    public static class DeleteRequest {
        private List<String> hostOrderKeys;

        public List<String> getHostOrderKeys() { return hostOrderKeys; }
        public void setHostOrderKeys(List<String> hostOrderKeys) {
            this.hostOrderKeys = hostOrderKeys;
        }
    }
}