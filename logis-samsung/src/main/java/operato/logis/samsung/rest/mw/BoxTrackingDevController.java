package operato.logis.samsung.rest.mw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.samsung.consts.BoxTrackingEventType;
import operato.logis.samsung.event.BoxTrackingEvent;
import operato.logis.samsung.utils.MwUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/rest/box-tracking")
@RequiredArgsConstructor
public class BoxTrackingDevController {

    private final ApplicationEventPublisher publisher;

    private final MwUtils mwUtils;

    /* =========================
     * INIT (최초 스캔) 발행
     * ========================= */
    @PostMapping(value = "/init", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String publishInit(@RequestBody Map<String, Object> body) {
        String barcode   = str(body, "barcode", true);
        String bcrNo     = str(body, "bcrNo", false, "BCR01"); // 리스너 조건: "1"
        String parcelId  = str(body, "parcelId", false, null);
        String lineId    = str(body, "lineId", false, null);
        String equipId   = str(body, "equipId", false, null);
        String plcSeqNo = str(body, "plcSeqNo", true, "0001");
        String itemCode  = str(body, "itemCode", false, null);

        Integer length   = intOrNull(body, "boxLength");
        Integer width    = intOrNull(body, "boxWidth");
        Integer height   = intOrNull(body, "boxHeight");
        Integer angle    = intOrNull(body, "boxAngle");

        BoxTrackingEvent event = BoxTrackingEvent.builder()
                .bcrNo(bcrNo)
                .eventType(BoxTrackingEventType.BCR_EVENT) // 여기서 에러나
                .plcSeqNo(plcSeqNo)
                .barcode(barcode)
                .parcelId(parcelId)
                .itemCode(itemCode)
                .lineId(lineId)
                .equipId(equipId)
                .boxLength(length)
                .boxWidth(width)
                .boxHeight(height)
                .boxAngle(angle)
                .fileNameTop(str(body, "fileNameTop", false, null))
                .fileNameFront(str(body, "fileNameFront", false, null))
                .fileNameBack(str(body, "fileNameBack", false, null))
                .fileNameLeft(str(body, "fileNameLeft", false, null))
                .fileNameRight(str(body, "fileNameRight", false, null))
                .fileNameBottomLeft(str(body, "fileNameBottomLeft", false, null))
                .fileNameBottomRight(str(body, "fileNameBottomRight", false, null))
                .measuredAt(new Date())
                .build();

        publisher.publishEvent(event);
        log.info("[DEV][BCR_INIT] published: {}", event);
        return "OK INIT";
    }

    /* =========================
     * UPDATE (갱신) 발행
     * ========================= */
    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String publishUpdate(@RequestBody Map<String, Object> body) {
        String barcode   = str(body, "barcode", true);
        String bcrNo     = str(body, "bcrNo", false, "1"); // 리스너 조건: "1"
        String parcelId  = str(body, "parcelId", false, null);
        String lineId    = str(body, "lineId", false, null);
        String equipId   = str(body, "equipId", false, null);
        String plcSeqNo = str(body, "plcSeqNo", false, null);
        String itemCode  = str(body, "itemCode", false, null);

        Integer length   = intOrNull(body, "boxLength");
        Integer width    = intOrNull(body, "boxWidth");
        Integer height   = intOrNull(body, "boxHeight");
        Integer angle    = intOrNull(body, "boxAngle");

        BoxTrackingEvent event = BoxTrackingEvent.builder()
                .bcrNo(bcrNo)
                .eventType(BoxTrackingEventType.VISION_EVENT)
                .plcSeqNo(plcSeqNo)
                .barcode(barcode)
                .parcelId(parcelId) // 있으면 기존 조회 우선, 없으면 boxId로 조회
                .itemCode(itemCode)
                .lineId(lineId)
                .equipId(equipId)
                .boxLength(length)
                .boxWidth(width)
                .boxHeight(height)
                .boxAngle(angle)
                .fileNameTop(str(body, "fileNameTop", false, null))
                .fileNameFront(str(body, "fileNameFront", false, null))
                .fileNameBack(str(body, "fileNameBack", false, null))
                .fileNameLeft(str(body, "fileNameLeft", false, null))
                .fileNameRight(str(body, "fileNameRight", false, null))
                .fileNameBottomLeft(str(body, "fileNameBottomLeft", false, null))
                .fileNameBottomRight(str(body, "fileNameBottomRight", false, null))
                .measuredAt(new Date())
                .build();

        publisher.publishEvent(event);
        log.info("[DEV][UPDATE] published: {}", event);
        return "OK UPDATE";
    }




    /* ===== 작은 파서 ===== */
    private static String str(Map<String, Object> m, String k, boolean required) {
        return str(m, k, required, null);
    }
    private static String str(Map<String, Object> m, String k, boolean required, String def) {
        Object v = m.get(k);
        if (v == null) {
            if (required) throw new IllegalArgumentException("missing required field: " + k);
            return def;
        }
        return String.valueOf(v);
    }
    private static Integer intOrNull(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignore) { return null; }
    }
}
