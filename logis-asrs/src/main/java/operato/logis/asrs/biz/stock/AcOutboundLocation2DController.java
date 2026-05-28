package operato.logis.asrs.biz.stock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import operato.logis.asrs.core.stock.OutboundLocation2DCommandCore;
import operato.logis.asrs.dto.request.OutboundLocation2DExecuteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.dto.response.OutboundLocation2DCellResponse;
import operato.logis.asrs.dto.response.OutboundLocation2DDepthResponse;
import operato.logis.asrs.dto.response.OutboundLocation2DMapResponse;
import operato.logis.asrs.query.stock.OutboundLocation2DQueryService;
import operato.logis.asrs.query.stock.model.OutboundLocation2DAisleOptionView;
import operato.logis.asrs.query.stock.model.OutboundLocation2DMapRowView;
import operato.logis.asrs.query.stock.model.OutboundLocation2DSideOptionView;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

/**
 * 2D 로케이션 지정출고 조회 API.
 */
@RestController
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/aislecore/stocks/outbound-location-2d")
@ServiceDesc(description = "Outbound Location 2D API")
public class AcOutboundLocation2DController {

    private final OutboundLocation2DQueryService outboundLocation2DQueryService;
    private final OutboundLocation2DCommandCore outboundLocation2DCommandCore;

    @PostMapping("/execute")
    @ApiDesc(description = "2D 로케이션 지정출고 실행")
    public void executeOutbound(@RequestBody OutboundLocation2DExecuteRequest request) {
        outboundLocation2DCommandCore.executeOutbound(request);
    }

    @GetMapping("/aisles")
    @ApiDesc(description = "2D 출고 화면 aisle 옵션 조회")
    public List<OutboundLocation2DAisleOptionView> getAisles(
            @RequestParam("areaCode") String areaCode
    ) {
        return outboundLocation2DQueryService.getAisleOptions(areaCode);
    }

    @GetMapping("/sides")
    @ApiDesc(description = "2D 출고 화면 side 옵션 조회")
    public List<OutboundLocation2DSideOptionView> getSides(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("aisleNo") Integer aisleNo
    ) {
        return outboundLocation2DQueryService.getSideOptions(areaCode, aisleNo);
    }

    @GetMapping("/map")
    @ApiDesc(description = "2D 출고 화면 맵 조회")
    public OutboundLocation2DMapResponse getMap(
            @RequestParam("areaCode") String areaCode,
            @RequestParam("aisleNo") Integer aisleNo,
            @RequestParam("sideCode") String sideCode
    ) {
        List<OutboundLocation2DMapRowView> rows =
                outboundLocation2DQueryService.getMapRows(areaCode, aisleNo, sideCode);

        OutboundLocation2DMapResponse response = new OutboundLocation2DMapResponse();
        response.setAreaCode(areaCode);
        response.setAisleNo(aisleNo);
        response.setSideCode(sideCode);

        int maxBayNo = rows.stream()
                .map(OutboundLocation2DMapRowView::getBayNo)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0);

        int maxLevelNo = rows.stream()
                .map(OutboundLocation2DMapRowView::getLevelNo)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0);

        response.setMaxBayNo(maxBayNo);
        response.setMaxLevelNo(maxLevelNo);

        Map<String, OutboundLocation2DCellResponse> grouped = new LinkedHashMap<String, OutboundLocation2DCellResponse>();

        for (OutboundLocation2DMapRowView row : rows) {
            String key = row.getBayNo() + "-" + row.getLevelNo();

            OutboundLocation2DCellResponse cell = grouped.get(key);
            if (cell == null) {
                cell = new OutboundLocation2DCellResponse();
                cell.setBayNo(row.getBayNo());
                cell.setLevelNo(row.getLevelNo());
                cell.setDepths(new ArrayList<OutboundLocation2DDepthResponse>());
                grouped.put(key, cell);
            }

            OutboundLocation2DDepthResponse depth = new OutboundLocation2DDepthResponse();
            depth.setDepthNo(row.getDepthNo());
            depth.setLocationId(row.getLocationId());
            depth.setLocationCode(row.getLocationCode());
            depth.setOccupied(row.getStockUnitNo() != null && !row.getStockUnitNo().isEmpty());
            depth.setStockUnitId(row.getStockUnitId());
            depth.setStockUnitNo(row.getStockUnitNo());
            depth.setItemId(row.getItemId());
            depth.setItemCode(row.getItemCode());
            depth.setItemName(row.getItemName());
            depth.setQty(row.getQty());
            depth.setReservedQty(row.getReservedQty());
            depth.setLotNo(row.getLotNo());
            depth.setStockStatusCode(row.getStockStatusCode());
            depth.setActiveYn(row.getActiveYn());

            cell.getDepths().add(depth);
        }

        response.setCells(new ArrayList<OutboundLocation2DCellResponse>(grouped.values()));
        return response;
    }
}