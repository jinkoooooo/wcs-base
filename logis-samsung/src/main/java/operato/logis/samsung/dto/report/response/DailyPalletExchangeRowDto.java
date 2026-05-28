package operato.logis.samsung.dto.report.response;

import lombok.Data;

@Data
public class DailyPalletExchangeRowDto {

    private String palletId;
    private String exchangePalletSequence;
    private String exchangeAt;
    private String emissionPalletSequence;
    private String emissionAt;
    private Long exchangeSeconds;
    private String exchangeTime;
}