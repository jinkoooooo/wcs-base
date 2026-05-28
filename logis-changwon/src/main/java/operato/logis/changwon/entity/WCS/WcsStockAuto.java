package operato.logis.changwon.entity.WCS;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "wcs_stock_auto", idStrategy = GenerationRule.UUID)
public class WcsStockAuto extends xyz.elidom.orm.entity.basic.ElidomStampHook {

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "stock_type", nullable = false, length = 2)
	private Integer stockType;

	@Column (name = "crane_no", nullable = false, length = 3)
	private Integer craneNo;

	@Column (name = "loc_level", nullable = false, length = 1)
	private Integer locLevel;

	@Column (name = "loc_col", nullable = false, length = 2)
	private Integer locCol;

	@Column (name = "loc_row", nullable = false, length = 2)
	private Integer locRow;

	@Column (name = "loc_deep", nullable = false, length = 1)
	private Integer locDeep;

	@Column (name = "loc_side", nullable = false, length = 10)
	private String locSide;

	@Column (name = "loc_cd", nullable = false, length = 10)
	private String locCd;

	@Column (name = "rack_locked", nullable = false, length = 1)
	private Integer rackLocked;

	@Column (name = "rack_disabled", nullable = false, length = 1)
	private Integer rackDisabled;

	@Column (name = "stock_id", length = 40)
	private String stockId;

	@Column (name = "task_id", length = 40)
	private String taskId;

	@Column (name = "store_date")
	private Date storeDate;

	@Column (name = "ship_date")
	private Date shipDate;

	@Column (name = "crane_disabled", nullable = false, length = 1)
	private Integer craneDisabled;

	@Column (name = "rack_height", length = 4)
	private Integer rackHeight;

	@Column (name = "rack_weight", length = 4)
	private Integer rackWeight;

	@Column (name = "stock_grade", length = 4)
	private Integer stockGrade;

	@Column (name = "dedicated_type", length = 20)
	private String dedicatedType;

	@Column (name = "restricted_type", length = 20)
	private String restrictedType;

	@Column (name = "owner_group_code", nullable = false, length = 60)
	private String ownerGroupCode;

	@Column (name = "attribute_a", length = 60)
	private String attributeA;

	@Column (name = "attribute_b", length = 60)
	private String attributeB;

	@Column (name = "position_x")
	private float positionX;

	@Column (name = "position_y")
	private float positionY;

	@Column (name = "position_z")
	private float positionZ;
}
