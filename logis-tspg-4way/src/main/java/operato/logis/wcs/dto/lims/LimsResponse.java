package operato.logis.wcs.dto.lims;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * WES → LIMS 공통 응답. { status, request_id, message, data }
 *   - status: "success" 성공 / "fail" 실패
 */
@Getter
public class LimsResponse<T> {

    private final String status;

    @JsonProperty("request_id")
    private final String requestId;

    private final String message;

    private final T data;

    private LimsResponse(String status, String requestId, String message, T data) {
        this.status = status;
        this.requestId = requestId;
        this.message = message;
        this.data = data;
    }

    public static <T> LimsResponse<T> success(String requestId, T data) {
        return new LimsResponse<>("success", requestId, "성공", data);
    }

    public static <T> LimsResponse<T> fail(String requestId, String message) {
        return new LimsResponse<>("fail", requestId, message, null);
    }
}