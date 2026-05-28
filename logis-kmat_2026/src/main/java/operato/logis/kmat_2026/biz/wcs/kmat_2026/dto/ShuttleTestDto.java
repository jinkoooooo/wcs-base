package operato.logis.kmat_2026.biz.wcs.kmat_2026.dto;

import lombok.Getter;
import lombok.Setter;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.AllocationResult;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;

@Getter
@Setter
public class ShuttleTestDto {
    private WcsOrderCommand command;

    private AllocationResult allocation;
}
