package xyz.elidom.sec.entity.relation;

import java.io.Serializable;

import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "user_histories", idStrategy = GenerationRule.UUID, isRef=true)
public class UserHistoryRef implements Serializable {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 716436783163676425L;
	
	@PrimaryKey
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}