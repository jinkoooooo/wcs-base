package operato.logis.changwon.event;

import lombok.Getter;
import lombok.Setter;
import operato.logis.changwon.entity.WCS.WcsTask;
import xyz.elidom.sys.event.SysEvent;

@Getter
@Setter
public class WcsTaskEvent extends SysEvent {

    private WcsTask wcsTask;

    private String orderKind;

    private String method;

    private String attribute;
}