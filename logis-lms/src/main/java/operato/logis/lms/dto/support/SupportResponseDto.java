package operato.logis.lms.dto.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import operato.logis.lms.entity.support.LmsSupportResponse;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportResponseDto {

    private LmsSupportResponse sr;
    private Boolean needSentMail;
}