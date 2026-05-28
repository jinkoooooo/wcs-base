package operato.logis.changwon.entity.MFC;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "c_err_def", idStrategy = GenerationRule.UUID)
public class ErrDef extends xyz.elidom.orm.entity.basic.ElidomStampHook {

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "error_machine", length = 6)
	private String errorMachine;

	@Column (name = "error_code", nullable = false, length = 6)
	private Integer errorCode;

	@Column (name = "error_name", length = 256)
	private String errorName;

	@Column (name = "error_solve", length = 256)
	private String errorSolve;
}
