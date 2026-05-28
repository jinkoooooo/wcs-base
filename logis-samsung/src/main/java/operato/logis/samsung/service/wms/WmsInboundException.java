package operato.logis.samsung.service.wms;

import operato.logis.samsung.consts.WmsIFCode;

/**
 * WMS-WCS 입고 처리 중 발생하는 비즈니스 예외
 */
public class WmsInboundException extends RuntimeException {

    private final WmsIFCode errorCode;

    public WmsInboundException(WmsIFCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WmsInboundException(WmsIFCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public WmsIFCode getErrorCode() {
        return errorCode;
    }
}
