package operato.logis.samsung.event;

import lombok.*;
import operato.logis.connector.hokusho.dto.HokushoPerformanceReportRequest;
import operato.logis.samsung.consts.BoxTrackingEventType;
import xyz.elidom.sys.event.SysEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * BCR/비전 스캔 이벤트(박스 트래킹)
 * - INIT: 최초 스캔/등록
 * - UPDATE: 기존 건 갱신(없으면 생성하지 않음 등의 정책 분기용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public class BoxTrackingEvent extends SysEvent {

    /** BCR 스테이션 번호(예: "1","2") */
    private String bcrNo;

    /** 트리거 ID(PLC/설비 트리거 식별자) */
    private String plcSeqNo; // 이 값이 1001|1002

    /** 이벤트 타입 (INIT/UPDATE) */
    private BoxTrackingEventType eventType;

    /** 스캔된 박스 바코드(키) */
    private String barcode;

    /** (선택) 이미 발급된 MW parcelId */
    private String parcelId;

    /** 품번(88....)(선택) */
    private String itemCode;

    /** 라인/설비(선택) */
    private String lineId;
    private String equipId;

    /** 규격/방향(선택, 단위 mm) */
    private Integer boxLength;
    private Integer boxWidth;
    private Integer boxHeight;
    private Integer boxAngle;

    /** 비전/검사 요약(선택) */
    private Integer cognexVisionResult;
    private Integer samsungVisionResult;
    private Integer manualVisionResult;

    /** 비전 이미지 파일명(선택, 7장 기준) */
    private String fileNameTop;
    private String fileNameFront;
    private String fileNameBack;
    private String fileNameLeft;
    private String fileNameRight;
    private String fileNameBottomLeft;
    private String fileNameBottomRight;

    /** 측정 시각(선택, null이면 서비스단에서 now 처리) */
    private Date measuredAt;

    private HokushoPerformanceReportRequest hokushoData;

    /** SpEL 등에서 정수 값 비교가 필요할 때 사용 (예: 조건식) */
    public Integer getEventTypeValue() {
        return eventType == null ? null : eventType.getHelper().getValue();
    }

    public List<String> getPlcSeqNoList() {
        if (plcSeqNo == null || plcSeqNo.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(plcSeqNo.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList(); // JDK 17 지원
    }

    /**
     * 디버깅용: 이벤트 내부 값들을 보기 좋게 포맷한 로그 문자열
     */
    public String toPrettyLog() {
        StringBuilder sb = new StringBuilder(256);

        sb.append("\n[BoxTrackingEvent]")
                .append("\n - bcrNo                 : ").append(nvl(bcrNo))
                .append("\n - plcSeqNo              : ").append(nvl(plcSeqNo))
                .append("\n - plcSeqNoList          : ").append(getPlcSeqNoList())
                .append("\n - eventType             : ").append(nvl(eventType))
                .append("\n - barcode               : ").append(nvl(barcode))
                .append("\n - parcelId              : ").append(nvl(parcelId))
                .append("\n - itemCode              : ").append(nvl(itemCode))
                .append("\n - lineId / equipId      : ")
                .append(nvl(lineId)).append(" / ").append(nvl(equipId))
                .append("\n - box(LxWxH, angle)     : ")
                .append(nvl(boxLength)).append(" x ")
                .append(nvl(boxWidth)).append(" x ")
                .append(nvl(boxHeight)).append(", angle=").append(nvl(boxAngle))
                .append("\n - vision(cognex/samsung/manual) : ")
                .append(nvl(cognexVisionResult)).append(" / ")
                .append(nvl(samsungVisionResult)).append(" / ")
                .append(nvl(manualVisionResult))
                .append("\n - fileNames(top/front/back/left/right/bottomL/bottomR)")
                .append("\n      -> ")
                .append(nvl(fileNameTop)).append(" / ")
                .append(nvl(fileNameFront)).append(" / ")
                .append(nvl(fileNameBack)).append(" / ")
                .append(nvl(fileNameLeft)).append(" / ")
                .append(nvl(fileNameRight)).append(" / ")
                .append(nvl(fileNameBottomLeft)).append(" / ")
                .append(nvl(fileNameBottomRight))
                .append("\n - measuredAt            : ").append(nvl(measuredAt))
                .append("\n - hasHokushoData        : ").append(hokushoData != null)
                .append('\n');

        return sb.toString();
    }

    private String nvl(Object value) {
        return (value == null) ? "-" : String.valueOf(value);
    }
}
