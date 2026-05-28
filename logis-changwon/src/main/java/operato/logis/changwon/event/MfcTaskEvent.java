package operato.logis.changwon.event;

import lombok.Getter;
import lombok.Setter;
import operato.logis.changwon.entity.MFC.JobRet;
import xyz.elidom.sys.event.SysEvent;

@Getter
@Setter
public class MfcTaskEvent extends SysEvent {

    private JobRet result;

    private String endPointCd;
}