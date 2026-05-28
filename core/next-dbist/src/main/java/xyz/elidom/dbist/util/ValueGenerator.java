package xyz.elidom.dbist.util;

import xyz.elidom.dbist.metadata.Column;

public interface ValueGenerator {
	public Object generate(Object data, Column column) throws Exception;
}
