package operato.logis.samsung.entity.xyz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "tb_mw_if_xyz_pallet_exchange", idStrategy = GenerationRule.UUID)
public class TbMwIfXyzPalletExchange extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @JsonProperty("pallet_id")
    @Column(name = "pallet_id", nullable = false, length = 10)
    private String palletId;

    @Column(name = "pallet_sequence", length = 20)
    private String palletSequence;
}