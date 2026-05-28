package operato.logis.wcs.dto.lims;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * IF02 시험의뢰 응답 data — 시험 의뢰 요청 정보 LIST.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class If02ResponseData {

    /** 시험 의뢰 요청 정보. */
    private List<If02RequestItem> requests;
}