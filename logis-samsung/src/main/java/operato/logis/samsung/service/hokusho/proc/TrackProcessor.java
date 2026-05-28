package operato.logis.samsung.service.hokusho.proc;

import operato.logis.samsung.consts.TrackingType;
import operato.logis.samsung.entity.mw.TbMwBox;
import operato.logis.samsung.entity.mw.TbMwBoxTrack;

public interface TrackProcessor {
    TrackingType supports();
    void process(TbMwBoxTrack tr, TbMwBox box);
}
