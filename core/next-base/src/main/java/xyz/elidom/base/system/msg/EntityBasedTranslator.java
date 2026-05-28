/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.system.msg;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.base.rest.ResourceController;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.msg.IEntityTranslator;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * 엔티티 기반의 용어 번역기 
 * 
 * @author shortstop
 */
@Component
public class EntityBasedTranslator implements IEntityTranslator {

	@Override
	public String getTermByEntity(String entityName, String colName) {
		return this.getTermByEntity(entityName, SysValueUtil.newStringList(colName));
	}

	@Override
	public String getTermByEntity(String entityName, List<String> colNameList) {
		ResourceController controller = BeanUtil.get(ResourceController.class);
		Resource resource = controller.resourceColumns(controller.findOne(SysConstants.SHOW_BY_NAME_METHOD, entityName).getId());
		List<ResourceColumn> items = resource.getItems();

		List<String> terms = new ArrayList<String>();
		for (String colName : colNameList) {
			for (ResourceColumn column : items) {
				if (SysValueUtil.isEqual(column.getName(), colName)) {
					terms.add(column.getTerm());
					break;
				}
			}
		}

		return SysValueUtil.listToString(terms);
	}

}
