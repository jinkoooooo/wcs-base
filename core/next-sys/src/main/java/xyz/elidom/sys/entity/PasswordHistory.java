package xyz.elidom.sys.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "password_histories", idStrategy = GenerationRule.UUID)
public class PasswordHistory extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 893463377549583549L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = 40)
	private String id;

	@Column(name = "name", length = 80)
	private String name;

	@Column(name = "user_id", length = 32)
	private String userId;

	public PasswordHistory() {
	}

	public PasswordHistory(String name, String userId) {
		this.name = name;
		this.userId = userId;
	}

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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
