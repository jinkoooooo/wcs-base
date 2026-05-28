package xyz.elidom.sec.entity.relation;

import java.io.Serializable;

import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "user_role_histories", idStrategy = GenerationRule.UUID, isRef=true)
public class UserRoleHistoryRef implements Serializable {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 174214159689093829L;
	
	@PrimaryKey
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}