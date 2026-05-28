package operato.logis.connector.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CommonApiResponse {

    private Integer code;

    private String msg;

    private String message;

    private Map<String, Object> data;

    public static CommonApiResponse success() {
        CommonApiResponse response = new CommonApiResponse();
        response.setCode(0);
        response.setMsg("ok");
        return response;
    }
}