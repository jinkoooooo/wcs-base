package operato.logis.inventory.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemIdentifierDto {

    private String itemOwner;

    private String itemCode;

    private Integer itemQty;
}