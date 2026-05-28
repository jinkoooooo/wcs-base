package operato.logis.samsung.dto.buffer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.buffer.TbMwBufferStorageArea;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BufferSkuAllocationDto {

    private String itemCode;
    private Integer qty;
    private Integer grade;
    private List<TbMwBufferStorageArea> allocationAreas;
}