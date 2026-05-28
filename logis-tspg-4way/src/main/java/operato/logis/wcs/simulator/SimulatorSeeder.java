package operato.logis.wcs.simulator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.simulator.SimulatorConfig.SeedSku;

// 시뮬레이터 시드 SKU 등록 전담 (Factory). 최초 tick 1회 멱등 실행.
@Component
@RequiredArgsConstructor
public class SimulatorSeeder {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorSeeder.class);

    private final InventoryItemMasterRepository inventoryItemMasterRepository;

    private volatile boolean done = false;

    public boolean isDone() {
        return done;
    }

    // 시드 SKU 일괄 등록 — 이미 존재하는 SKU 는 skip. 1회만 수행.
    public void ensureOnce() {
        if (done) return;
        ensureSeed();
        done = true;
    }

    // 시드 SKU 중 1개 랜덤.
    public SeedSku randomSku() {
        return SimulatorConfig.SEED_SKUS.get(
                ThreadLocalRandom.current().nextInt(SimulatorConfig.SEED_SKUS.size()));
    }

    private void ensureSeed() {
        List<String> codes = SimulatorConfig.SEED_SKUS.stream().map(SeedSku::code).toList();
        Map<String, ExtTbInventoryItemMaster> existing =
                inventoryItemMasterRepository.findAsMapByOwnerAndCodes(SimulatorConfig.OWNER_CODE, codes);

        int already = existing.size();
        int inserted = 0;
        int failed = 0;
        for (SeedSku sku : SimulatorConfig.SEED_SKUS) {
            if (existing.containsKey(sku.code())) continue;
            try {
                inventoryItemMasterRepository.insert(toEntity(sku));
                inserted++;
                logger.info("[ Sim ][ Seed ] sku inserted - code={}, name={}", sku.code(), sku.name());
            } catch (Exception e) {
                failed++;
                logger.error("[ Sim ][ Seed ] sku failed - code={}", sku.code(), e);
            }
        }
        logger.info("[ Sim ][ Seed ] done - existing={}, inserted={}, failed={}, total={}",
                already, inserted, failed, codes.size());
    }

    // SeedSku → JPA 엔티티.
    private ExtTbInventoryItemMaster toEntity(SeedSku sku) {
        ExtTbInventoryItemMaster e = new ExtTbInventoryItemMaster();
        e.setItemOwner(SimulatorConfig.OWNER_CODE);
        e.setItemCode(sku.code());
        e.setItemName(sku.name());
        e.setItemType("PALLET");
        e.setItemGrade(0);
        e.setItemUnit("EA");
        e.setItemWeight(sku.weight());
        e.setBoxQty(sku.boxQty());
        e.setPalletQty(sku.palletQty());
        e.setRemarks("시뮬레이터 자동 시드");
        return e;
    }
}
