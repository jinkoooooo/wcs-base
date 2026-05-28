package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommandItem;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 외부 요청 DTO를 내부 공통 처리 모델(WcsOrderCommand)로 변환한다.
 *
 * HOST / DIRECT / BATCH / SIMULATION 등의 입력 경로가 늘어나도
 * 내부 처리 파이프라인은 동일하게 유지하기 위한 변환 계층이다.
 */
@Service
public class WcsOrderCommandMapper {

    public WcsOrderCommand fromHostRequest(HostOrderReceiveRequest request) {
        List<WcsOrderCommandItem> commandItems = new ArrayList<>();

        if (!ValueUtil.isEmpty(request.getItems())) {
            for (HostOrderReceiveRequest.HostOrderItemRequest item : request.getItems()) {
                if (item == null) {
                    continue;
                }

                commandItems.add(
                        WcsOrderCommandItem.builder()
                                .lineNo(item.getLineNo())
                                .skuCode(item.getSkuCode())
                                .lotNo(item.getLotNo())
                                .qty(item.getQty())
                                .uom(item.getUom())
                                .rawAttr(item.getRawAttr())
                                .build()
                );
            }
        }

        return WcsOrderCommand.builder()
                .sourceType("HOST")
                .sourceSystemCode(request.getHostSystemCode())
                .sourceOrderKey(request.getHostOrderKey())
                .orderType(request.getOrderType())
                .ownerCode(request.getOwnerCode())
                .eqGroupId(request.getEqGroupId())
                .priority(request.getPriority() != null ? request.getPriority() : 5)
                .fromLocCode(null)
                .toLocCode(null)
                .rawPayload(request.getRawPayload())
                .persistHostOrder(true)
                .items(commandItems)
                .build();
    }
}