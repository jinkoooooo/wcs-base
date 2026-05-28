package operato.logis.samsung.event;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.mw.TbMwBoxConveyorInfo;
import xyz.elidom.sys.event.SysEvent;

import java.util.List;

@Getter
@Setter
@Builder
public class BoxArrivedOnConveyorEvent extends SysEvent {

    private String boxConveyorCd;

    private List<TbMwBoxConveyorInfo> boxList;
}