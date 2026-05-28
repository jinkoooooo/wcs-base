package operato.logis.samsung.service.hokusho;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.samsung.consts.TrackingType;
import operato.logis.samsung.entity.mw.TbMwBox;
import operato.logis.samsung.entity.mw.TbMwBoxTrack;
import operato.logis.samsung.service.hokusho.proc.TrackProcessor;
import operato.logis.samsung.service.mw.BoxTrackingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HokushoOrderDispatcherService extends AbstractQueryService {

    private final BoxTrackingService boxTrackingService;
    private final List<TrackProcessor> processors;

    private Map<TrackingType, TrackProcessor> procMap;

    @Transactional
    public void handle(TbMwBoxTrack tr) {
        if (tr == null || ValueUtil.isEmpty(tr.getPlcSeqNo())) {
            logger.warn("[HOKUSHO] invalid track: {}", tr);
            return;
        }
        initMapIfNeeded();

        TbMwBox box = boxTrackingService.findBoxByKey(tr.getBoxId(), tr.getParcelId());

        TrackingType type = resolveType(tr.getTrackingType());
        TrackProcessor proc = procMap.getOrDefault(type, null);

        if (proc == null) {
            logger.info("[DISPATCH] no processor for type={}, boxId={}", type, tr.getBoxId());
            return;
        }

        try {
            proc.process(tr, box);
        } catch (Exception e) {
            logger.error("[DISPATCH] error. type={}, boxId={}, err={}", type, tr.getBoxId(), e.getMessage(), e);
            throw e;
        }
    }

    private void initMapIfNeeded() {
        if (procMap != null) return;
        procMap = new EnumMap<>(TrackingType.class);
        for (TrackProcessor p : processors) procMap.put(p.supports(), p);
    }

    private TrackingType resolveType(String raw) {
        if (ValueUtil.isEmpty(raw)) return TrackingType.UNKNOWN;
        try { return TrackingType.fromValue(Integer.parseInt(raw)); }
        catch (Exception ignore) {
            try { return TrackingType.valueOf(raw); }
            catch (Exception e) { return TrackingType.UNKNOWN; }
        }
    }
}
