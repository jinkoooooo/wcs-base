package operato.logis.lms.dto.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MailReceiverDto {

    private String email; // 수신인 메일
    private String lcNm; // 수신인 소속 센터
}