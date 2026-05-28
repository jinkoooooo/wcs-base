package operato.logis.inventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InboundLocationRequestDto {

    private List<ItemIdentifierDto> itemList;

    private List<String> reservedLocCodeList; // 옵션: 여러번 호출 시 중복 로케이션 반환 예방

    private String locGroup; // 옵션: 특정 로케이션 Group에서 조회

    private String itemType;

    private int totalWeight;

    private int totalHeight;
}