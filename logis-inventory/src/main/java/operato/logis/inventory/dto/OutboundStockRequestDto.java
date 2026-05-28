package operato.logis.inventory.dto;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.consts.OutboundCalculateStrategy;

import java.util.List;

@Getter
@Setter
public class OutboundStockRequestDto {

    OutboundCalculateStrategy outboundCalculateStrategy;

    List<ItemIdentifierDto> itemList;

    String lotNo; // 옵션: 특정 Lot에서 조회

    String locGroup; // 옵션: 특정 로케이션 Group에서 조회
}