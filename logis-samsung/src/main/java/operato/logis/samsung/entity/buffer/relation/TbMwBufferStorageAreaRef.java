package operato.logis.samsung.entity.buffer.relation;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.io.Serializable;

@Getter
@Setter
@Table(name = "tb_mw_buffer_storage_area", idStrategy = GenerationRule.UUID, isRef = true)
public class TbMwBufferStorageAreaRef implements Serializable {

    private static final long serialVersionUID = 9031747085786799596L;

    @PrimaryKey
    private String id;

    @Column(name = "aisle_no")
    private Integer aisleNo;

    @Column(name = "level_no")
    private Integer levelNo;
}