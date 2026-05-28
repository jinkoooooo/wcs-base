package operato.logis.samsung.service.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.connector.api.service.ExternalApiService;
import operato.logis.connector.gtr.entity.GtrToken;
import operato.logis.samsung.WcsConstants;
import operato.logis.samsung.WcsUtils;
import operato.logis.samsung.dto.xyz.XyzDevanningRequest;
import operato.logis.samsung.dto.xyz.XyzDevanningResult;
import operato.logis.samsung.entity.mw.TbMwXyzOrder;
import operato.logis.samsung.entity.wcs.TbMwUnitHeartbeat;
import operato.logis.samsung.entity.xyz.TbMwIfXyzPalletExchange;
import operato.logis.samsung.entity.xyz.TbMwXyzDvnOrder;
import operato.logis.samsung.service.mw.TbMwChuteManagementService;
import operato.logis.samsung.service.mw.TbMwInboundDeliveryService;
import operato.logis.samsung.service.wcs.UnitHeartbeatService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class XyzDevanningService extends AbstractQueryService {

    private final ExternalApiService externalApiService;
    private final TbMwChuteManagementService tbMwChuteManagementService;
    private final TbMwInboundDeliveryService tbMwInboundDeliveryService;
    private final UnitHeartbeatService unitHeartbeatService;

    private final String UNIT_CODE_DEVANNING = "XYZ_DEVANNING";

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

    /**
     *      * XYZ code -> msg 변환 메소드
     *      *
     *      * @param code XYZ에서 반환하는 상태 코드
     * @return 기존에 협의된 상태 코드별 메시지
     */
    private String convertStatusToMsg(int code) {
        return switch (code) {
            case 0 -> "오더 없음";
            case 1 -> "오더 진행 중";
            case 99 -> "프로그램 정지 상태";
            default -> "알 수 없음";
        };
    }

    public CommonApiResponse sendDevanningRequest(String cntr_no, String bl_no) {
        // 1. API URL 설정
        String xyzServerIP = WcsConstants.XYZ_DEVANNING_URL;
        String endPoint = "/api/preset_order";
        String url = xyzServerIP + endPoint;

        CommonApiResponse result = new CommonApiResponse();

        try {
            Map<String, Object> param = new HashMap<>();
            param.put("cntrNo", cntr_no);
            // param.put("blNo", bl_no); // 현재 SQL 쿼리에는 없지만, 나중에 필요하면 주석 해제

            List<Map<String, Object>> deliveryList = tbMwInboundDeliveryService.getInboundDevanningRequest(param);

            XyzDevanningRequest request = new XyzDevanningRequest();
            request.setName(cntr_no);

            XyzDevanningRequest.RawData rawData = new XyzDevanningRequest.RawData();
            rawData.setOrderType(5);
            rawData.setSkuInputType("detailed");

            // 4-1. DB 조회 결과를 Items 리스트로 변환 (Map 데이터 추출)
            List<XyzDevanningRequest.Item> items = new ArrayList<>();
            if (ValueUtil.isNotEmpty(deliveryList)) {
                for (Map<String, Object> delivery : deliveryList) {
                    XyzDevanningRequest.Item item = new XyzDevanningRequest.Item();

                    String itemCode = String.valueOf(delivery.get("item_code"));

                    item.setBarcode(itemCode);
                    item.setBarcodeDirection(0);
                    item.setId(-1);
                    item.setName(itemCode);

                    item.setHeight(delivery.get("item_height") != null ? (int) Double.parseDouble(String.valueOf(delivery.get("item_height"))) : 0);
                    item.setLength(delivery.get("item_length") != null ? (int) Double.parseDouble(String.valueOf(delivery.get("item_length"))) : 0);
                    item.setWidth(delivery.get("item_width") != null ? (int) Double.parseDouble(String.valueOf(delivery.get("item_width"))) : 0);
                    item.setWeight(delivery.get("item_weight") != null ? (int) Double.parseDouble(String.valueOf(delivery.get("item_weight"))) : 0);

                    items.add(item);
                }
            }
            rawData.setItems(items);

            // 4-2. 트럭 및 크로스빔 제원 세팅
            XyzDevanningRequest.TruckInnerDimension truckDim = new XyzDevanningRequest.TruckInnerDimension();
            truckDim.setHeight(2690);
            truckDim.setLength(1);
            truckDim.setWidth(1);
            rawData.setTruckInnerDimension(truckDim);

            XyzDevanningRequest.CrossBeamDimension crossBeamDim = new XyzDevanningRequest.CrossBeamDimension();
            crossBeamDim.setWidth(300);
            crossBeamDim.setHeight(180);
            rawData.setCrossBeamDimension(crossBeamDim);

            request.setRawData(rawData);

            // 5. API 전송
            logger.info("[XYZ] Devanning Request API Body : {}", WcsUtils.logRequestBody(request));

            Mono<CommonApiResponse> responseMono = externalApiService.post(url, request, CommonApiResponse.class);
            result = responseMono.block();

            if (ValueUtil.isNotEmpty(result) && result.getCode() != null && result.getCode() == 0) {

                TbMwXyzDvnOrder dvnOrder = new TbMwXyzDvnOrder();

                dvnOrder.setOrderKey(cntr_no);
                dvnOrder.setResultCode("1");
                dvnOrder.setResultMsg("START");
                dvnOrder.setCreatorId("WCS");

                if (!items.isEmpty()) {
                    String joinedBarcodes = items.stream()
                            .map(XyzDevanningRequest.Item::getBarcode)
                            .collect(Collectors.joining(","));

                    dvnOrder.setBarcode(joinedBarcodes);
                }

                // 6-3. DB Insert 실행
                try {
                    this.queryManager.insert(dvnOrder);
                    logger.info("[XYZ] DB 저장 완료 - ORDER_KEY(컨테이너): {}", cntr_no);
                } catch (Exception e) {
                    logger.error("[XYZ] DB 저장 중 에러 발생 : {}", e.getMessage());
                }

            } else {
                logger.error("[XYZ] API 응답이 비정상적이거나 실패했습니다.");
            }

        } catch (ElidomRuntimeException e) {
            logger.error("[XYZ] Devanning 컨테이너 [{}] 작업 요청 중 에러 발생 : {}", cntr_no, e.getMessage());
        } catch (Exception e) {
            logger.error("[XYZ] Devanning 컨테이너 [{}] 데이터 조립 중 에러 발생 : {}", cntr_no, e.getMessage(), e);
        }

        return result;
    }

    public void getXyzDevanningCancel() {
        // API URL 설정
        String xyzServerIP = WcsConstants.XYZ_DEVANNING_URL;
        String endPoint = "/api/manager/abort";
        String url = xyzServerIP + endPoint;

        // XYZ 응답 수신
        try {
            Mono<CommonApiResponse> responseMono = externalApiService.get(url, CommonApiResponse.class);
            CommonApiResponse response = responseMono.block();

            // XYZ Status를 Unit Status에 반영
            if (ValueUtil.isNotEmpty(response)) {
                Map<String, Object> data = response.getData();
                Integer code = (Integer) data.get("status");

                logger.error("[XYZ] Status 조회 중 에러 발생");
            }
        }
        catch (ElidomRuntimeException e) {
            logger.error("[XYZ] Status 조회 중 에러 발생 : {}", e.getMessage());
        }
    }

    public void getXyzDevanningDelete() {
        // API URL 설정
        String xyzServerIP = WcsConstants.XYZ_DEVANNING_URL;
        String endPoint = "/api/preset_order";
        String url = xyzServerIP + endPoint;

        try {
            Mono<CommonApiResponse> responseMono = externalApiService.delete(url, CommonApiResponse.class);
            CommonApiResponse response = responseMono.block();

            if (ValueUtil.isNotEmpty(response)) {
                // 3. 정상 수신 시 에러 로그를 찍던 버그 수정
                logger.info("[XYZ] Devanning Delete 요청 성공 : {}", WcsUtils.logRequestBody(response));
            } else {
                logger.error("[XYZ] Devanning Delete 요청 실패 - 응답이 비어있습니다.");
            }
        }
        catch (ElidomRuntimeException e) {
            logger.error("[XYZ] Status 조회 중 에러 발생 : {}", e.getMessage());
        }
    }

    /**
     * XYZ -> MW 작업 통보 (START / COMPLETE) 수신 처리 로직
     */
    public CommonApiResponse XyzDevanningOrderResult(XyzDevanningResult request) {
        logger.info("[XYZ -> MW] Order 통보 수신 - OrderKey: {}, Msg: {}, Code: {}",
                request.getOrderKey(), request.getResultMsg(), request.getResultCode());

        try {
            // 1. XYZ에서 보낸 상태(result_msg)를 MW의 기준 상태값으로 매핑
            String mwStatus = "";
            if ("START".equalsIgnoreCase(request.getResultMsg())) {
                mwStatus = "1"; // 작업 시작 상태코드
            } else if ("COMPLETE".equalsIgnoreCase(request.getResultMsg())) {
                mwStatus = "0"; // 작업 완료 상태코드
            }

            // 2. ORDER_KEY를 기준으로 기존 데이터 조회 (return 키워드 제거, SQL = 중복 수정)
            String sql = "select * from tb_mw_xyz_dvn_order where order_key = :order_key";
            Map<String, Object> param = ValueUtil.newMap("order_key", request.getOrderKey());

            List<TbMwXyzDvnOrder> orderList = this.queryManager.selectListBySql(sql, param, TbMwXyzDvnOrder.class,0,0);

            int orderCount = (orderList != null) ? orderList.size() : 0;
            logger.info("[XYZ -> MW] SQL 조회 완료 - OrderKey: {}, 조회된 데이터 건수: {}건", request.getOrderKey(), orderCount);

            // 3. 데이터가 없을 경우의 방어 로직
            if (orderList == null || orderList.isEmpty()) {
                logger.error("[XYZ -> MW] 매칭되는 OrderKey({})의 데이터가 존재하지 않습니다.", request.getOrderKey());

                CommonApiResponse errorResponse = new CommonApiResponse();
                errorResponse.setCode(-1);
                errorResponse.setMsg("매칭되는 Order 데이터가 존재하지 않습니다.");
                return errorResponse;
            }

            TbMwXyzDvnOrder existingOrder = orderList.get(0);

            existingOrder.setResultCode(String.valueOf(request.getResultCode()));
            existingOrder.setResultMsg(request.getResultMsg());
            existingOrder.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));


            // 6. 업데이트 실행
            this.queryManager.update(existingOrder, "resultCode", "resultMsg", "updatedAt");

            // 7. 로깅
            logger.info("[XYZ -> MW] Order [{}] 상태가 [{}]로 정상 업데이트 되었습니다.", request.getOrderKey(), mwStatus);

        } catch (Exception e) {
            logger.error("[XYZ -> MW] 통보 수신 처리 중 에러 발생 : {}", e.getMessage(), e);

            CommonApiResponse errorResponse = new CommonApiResponse();
            errorResponse.setCode(-1);
            errorResponse.setMsg("통보 수신 처리 중 에러 발생: " + e.getMessage());
            return errorResponse;
        }

        // 8. 최종 성공 응답 반환 (원래 의도했던 타입)
        CommonApiResponse response = CommonApiResponse.success();
        response.setData(new java.util.HashMap<>());

        return response;
    }
}