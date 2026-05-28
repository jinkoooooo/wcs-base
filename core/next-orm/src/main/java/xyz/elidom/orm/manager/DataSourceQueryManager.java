/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.manager;

import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.dml.Dml;

/**
 * 기본 데이터소스 외에 프레임워크에서 등록한 외부 데이터소스를 이용한 QueryManager 구현
 * 
 * @author shortstop
 */
public class DataSourceQueryManager extends AbstractQueryManager {

	private Dml dml;

	private Ddl ddl;

	@Override
	public Dml getDml() {
		return this.dml;
	}

	public void setDml(Dml dml) {
		this.dml = dml;
	}

	public Ddl getDdl() {
		return ddl;
	}

	public void setDdl(Ddl ddl) {
		this.ddl = ddl;
	}
}
