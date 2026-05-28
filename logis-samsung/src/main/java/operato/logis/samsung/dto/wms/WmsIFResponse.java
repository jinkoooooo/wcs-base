package operato.logis.samsung.dto.wms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import operato.logis.samsung.consts.WmsIFCode;

/**
 * WCS -> WMS 지시 수신 결과 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WmsIFResponse {

    private String code; // 정상 수신 여부 응답 코드
    private String regId; // 송신한 요청 ID
    private String message; // 응답 설명 0 - successful 또는 에러메시지
    private String detailMessage; // 에러인 경우, 세부 메시지

    public static WmsIFResponse success(String regId, String detailMessage) {
        WmsIFResponse dto = new WmsIFResponse();
        dto.code = WmsIFCode.SUCCESSFUL.getValue();
        dto.regId = regId;
        dto.message = WmsIFCode.SUCCESSFUL.name();
        dto.detailMessage = detailMessage;
        return dto;
    }

    public static WmsIFResponse fail(WmsIFCode codeConst, String regId, String detailMessage) {
        WmsIFResponse dto = new WmsIFResponse();
        dto.code = codeConst.getValue();
        dto.regId = regId;
        dto.message = codeConst.name();
        dto.detailMessage = detailMessage;
        return dto;
    }
}