package operato.logis.samsung.entity.buffer.relation;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.io.Serializable;

@Getter
@Setter
@Table(name = "tb_mw_buffer_task_detail", idStrategy = GenerationRule.UUID, isRef = true)
public class TbMwBufferTaskDetailRef implements Serializable {

    private static final long serialVersionUID = -4541061945169617064L;

    @PrimaryKey
    private String id;
}