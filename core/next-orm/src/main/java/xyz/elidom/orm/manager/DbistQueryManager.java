/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.elidom.dbist.dml.Dml;

/**
 * IQueryManager의 Dbist 구현체  
 * 
 * @author shortstop
 */
@Component
public class DbistQueryManager extends AbstractQueryManager {
	
	@Autowired
	private Dml dml;

	@Override
	public Dml getDml() {
		return this.dml;
	}
}