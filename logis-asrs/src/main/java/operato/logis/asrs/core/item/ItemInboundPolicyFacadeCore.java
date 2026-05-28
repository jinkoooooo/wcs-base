package operato.logis.asrs.core.item;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.dto.response.ItemInboundPolicyResult;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;

@Service
@RequiredArgsConstructor
public class ItemInboundPolicyFacadeCore {

    private final ItemQueryService itemQueryService;
    private final LocationQueryService locationQueryService;
    private final ItemPolicyResolveCore itemPolicyResolveCore;

    public ItemInboundPolicyResult getInboundPolicy(String areaCode, String itemCode) {
        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        TbAcItemMaster item = itemQueryService.findItemByCode(itemCode);

        boolean lotControlRequired = itemPolicyResolveCore.isLotControlRequired(area.getId(), item.getId());
        boolean expiryControlRequired = itemPolicyResolveCore.isExpiryControlRequired(area.getId(), item.getId());
        boolean serialControlRequired = itemPolicyResolveCore.resolve(area.getId(), item.getId()).isSerialControlRequired();

        boolean lotEnabled = lotControlRequired || expiryControlRequired;
        boolean lotRequired = lotControlRequired || expiryControlRequired;

        return ItemInboundPolicyResult.builder()
                .areaId(area.getId())
                .areaCode(area.getAreaCode())
                .itemId(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .lotEnabled(lotEnabled)
                .lotRequired(lotRequired)
                .lotControlRequired(lotControlRequired)
                .expiryControlRequired(expiryControlRequired)
                .serialControlRequired(serialControlRequired)
                .build();
    }
}