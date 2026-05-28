package operato.logis.changwon.event;

import lombok.Getter;
import lombok.Setter;
import operato.logis.changwon.entity.WMS.WmsOdr;
import xyz.elidom.sys.event.SysEvent;

@Getter
@Setter
public class WmsOrderEvent extends SysEvent {

    private WmsOdr wmsOdr;
}