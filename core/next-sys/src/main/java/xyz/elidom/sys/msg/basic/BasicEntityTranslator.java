/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.msg.basic;

import java.util.List;

import org.springframework.stereotype.Component;

import xyz.elidom.dbist.util.StringJoiner;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.msg.IEntityTranslator;

/**
 * 기본 엔티티 기반 번역기 - 메시지 번역을 위한 소스는 없고 기본 언어로만 번역 
 * 
 * @author shortstop
 */
@Component
public class BasicEntityTranslator implements IEntityTranslator {

	@Override
	public String getTermByEntity(String entityName, String columnName) {
		return columnName;
	}

	@Override
	public String getTermByEntity(String entityName, List<String> columnNameList) {
		StringJoiner joiner = new StringJoiner(OrmConstants.COMMA);
		for(String colName : columnNameList) {
			joiner.add(colName);
		}
		
		return joiner.toString();
	}

}
