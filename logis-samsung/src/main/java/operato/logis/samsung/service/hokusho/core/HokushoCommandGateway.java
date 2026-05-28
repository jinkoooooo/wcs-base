package operato.logis.samsung.service.hokusho.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.hokusho.dto.HokushoCommandTaskRequest;
import operato.logis.connector.hokusho.dto.HokushoResponse;
import operato.logis.connector.hokusho.service.HokushoEcsCommandTaskService;
import operato.logis.samsung.entity.mw.TbMwBox;
import operato.logis.samsung.entity.mw.TbMwBoxTrack;
import operato.logis.samsung.utils.MwUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class HokushoCommandGateway {

    private final HokushoEcsCommandTaskService ecs;
    private final MwUtils logging;

    public void sendDivert(TbMwBox box, TbMwBoxTrack tr, String divertTo, String equipId) {
        HokushoCommandTaskRequest req = newReq(box, tr, "DVRT", equipId);
        req.getParams().put("divertTo", divertTo);
        send(req, safeBoxId(box, tr));
    }

    public void sendTurn(TbMwBox box, TbMwBoxTrack tr, int useRotator, int pickType, String endPointCd) {
        HokushoCommandTaskRequest req = newReq(box, tr, "TURN", "P1TRN1ARV1");
        req.getParams().put("useRotator", useRotator);
        req.getParams().put("pickType", pickType);
        req.getParams().put("endPointCd", endPointCd);
        send(req, safeBoxId(box, tr));
    }

    public void sendPmovSupply(TbMwBox box, TbMwBoxTrack tr, String endPointCd, String source, String target) {
        HokushoCommandTaskRequest req = newReq(box, tr, "PMOV", "P1TRN1ARV1");
        req.getParams().put("mode", "SUPPLY");
        req.getParams().put("endPointCd", endPointCd);
        req.getParams().put("source", source);
        req.getParams().put("target", target);
        send(req, safeBoxId(box, tr));
    }

    /* ---------- 내부 공통 ---------- */

    private HokushoCommandTaskRequest newReq(TbMwBox box, TbMwBoxTrack tr, String commandType, String equipId) {
        String requestedAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String lineId  = notBlank(tr.getLineId())  ? tr.getLineId()  : (box != null ? box.getFirstLineId()  : null);

        return HokushoCommandTaskRequest.builder()
                .requestedAt(requestedAt)
                .parcelId(tr.getParcelId())
                .plcSeqNo(tr.getPlcSeqNo())
                .lineId(lineId)
                .equipId(equipId)
                .commandType(commandType)
                .commandId(newCommandId(commandType,tr.getPlcSeqNo()))
                .params(new HashMap<>())
                .build();
    }

    private String newCommandId(String commandType, String plcSeqNo) {
        String ts = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

        String seq = (plcSeqNo == null) ? "0000" : plcSeqNo;

        return commandType + "-" + ts + seq;
    }

    private void send(HokushoCommandTaskRequest req, String boxIdForLog) {
        logging.logRequestBody(req);
        logging.saveRequestToFile(req);

        Mono<HokushoResponse> mono = ecs.sendCommandTask(req)
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)).filter(this::retryable));

        HokushoResponse resp = mono.block();
        if (resp == null || !"0".equals(resp.getCode())) {
            throw new IllegalStateException("[HOKUSHO] commandTask failed. code="
                    + (resp == null ? "null" : resp.getCode())
                    + ", msg=" + (resp == null ? "null" : resp.getMessage()));
        }
        log.info("[HOKUSHO] ok. boxId={}, cmdId={}, msg={}",
                boxIdForLog, resp.getCommandId(), resp.getMessage());
    }

    private boolean retryable(Throwable t) {
        String m = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
        return m.contains("timeout") || m.contains("5") || m.contains("refused");
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static String safeBoxId(TbMwBox b, TbMwBoxTrack t) { return t.getBoxId() != null ? t.getBoxId() : (b != null ? b.getBoxId() : null); }
}
