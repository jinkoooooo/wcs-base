package xyz.elidom.core.entity.relation;

import java.io.Serializable;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "procedures", idStrategy = GenerationRule.UUID, isRef=true)
public class ProcedureRef implements Serializable {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 253109158881005725L;
	
	@PrimaryKey
	private String id;

	@Column (name = "name", nullable = false, length = 36)
	private String name;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	
}

