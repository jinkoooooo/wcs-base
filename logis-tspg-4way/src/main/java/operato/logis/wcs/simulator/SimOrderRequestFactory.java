package operato.logis.wcs.simulator;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.simulator.HostSimulator.Phase;
import operato.logis.wcs.simulator.SimulatorConfig.SeedSku;

// 시뮬 HOST 주문 요청 빌더 (Factory). 입고는 랜덤 SKU, 출고는 IDLE 시뮬 재고 픽업.
@Component
@RequiredArgsConstructor
public class SimOrderRequestFactory {

    private final SimulatorSeeder seeder;
    private final SimStockQuery simStockQuery;

    // 페이즈별 요청 빌드. SWITCHING_* 페이즈는 null.
    public HostOrderApi.Request build(String groupId, Phase phase, String hostOrderKey) {
        return switch (phase) {
            case INBOUND  -> inbound(groupId, hostOrderKey);
            case OUTBOUND -> outbound(groupId, hostOrderKey);
            default       -> null;
        };
    }

    // 입고 요청 — 랜덤 SKU, qty=50 고정.
    public HostOrderApi.Request inbound(String groupId, String hostOrderKey) {
        SeedSku sku = seeder.randomSku();
        HostOrderApi.Request req = new HostOrderApi.Request();
        req.setHostSystemCode(SimulatorConfig.HOST_SYSTEM_CODE);
        req.setHostOrderKey(hostOrderKey);
        req.setOrderType("INBOUND");
        req.setOwnerCode(SimulatorConfig.OWNER_CODE);
        req.setEqGroupId(groupId);
        req.setBarcode(SimulatorConfig.SIM_BARCODE_PREFIX + hostOrderKey);
        req.setPriority(5);

        HostOrderApi.Item item = new HostOrderApi.Item();
        item.setLineNo(1);
        item.setItemCode(sku.code());
        item.setLotNo(SimulatorConfig.SIM_LOT_PREFIX + hostOrderKey);
        item.setQty(50);
        item.setUom("EA");
        req.setItems(List.of(item));
        return req;
    }

    // 출고 요청 — IDLE 시뮬 재고 1건 랜덤 픽업. 재고 없으면 null.
    public HostOrderApi.Request outbound(String groupId, String hostOrderKey) {
        SimStock pick = simStockQuery.pickRandom(groupId);
        if (pick == null || pick.itemQty() <= 0) return null;
        return outboundFromStock(groupId, hostOrderKey, pick);
    }

    // 지정 재고 1건에 대한 출고 요청 빌드.
    public HostOrderApi.Request outboundFromStock(String groupId, String hostOrderKey, SimStock pick) {
        HostOrderApi.Request req = new HostOrderApi.Request();
        req.setHostSystemCode(SimulatorConfig.HOST_SYSTEM_CODE);
        req.setHostOrderKey(hostOrderKey);
        req.setOrderType("OUTBOUND");
        req.setOwnerCode(SimulatorConfig.OWNER_CODE);
        req.setEqGroupId(groupId);
        req.setPriority(5);

        HostOrderApi.Item item = new HostOrderApi.Item();
        item.setLineNo(1);
        item.setItemCode(pick.itemCode());
        item.setQty(pick.itemQty());
        item.setUom("EA");
        req.setItems(List.of(item));
        return req;
    }
}
