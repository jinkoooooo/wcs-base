package xyz.elidom.dbist.util;

import java.util.UUID;

import xyz.elidom.dbist.metadata.Column;

public class UuidGenerator implements ValueGenerator {
	public Object generate(Object data, Column column) throws Exception {
		return UUID.randomUUID();
	}
}
