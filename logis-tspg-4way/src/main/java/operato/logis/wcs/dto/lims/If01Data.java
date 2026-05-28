package operato.logis.wcs.dto.lims;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * IF01 data — 자재마스터 LIST 컨테이너.
 */
@Getter
@Setter
public class If01Data {

    /** 자재마스터 LIST. */
    private List<If01Item> items;
}