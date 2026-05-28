package operato.logis.connector.http.mapper;

import java.util.List;

public class HttpMappingMetaData {

	private String system;
	private String domainId;
	private String baseUrl;
	private List<MappingDetail> mappings;

	// Getter/Setter
	public String getSystem() {
		return system;
	}

	public void setSystem(String system) {
		this.system = system;
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String tenancy) {
		this.domainId = tenancy;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public List<MappingDetail> getMappings() {
		return mappings;
	}

	public void setMappings(List<MappingDetail> mappings) {
		this.mappings = mappings;
	}

	// 내부 클래스
	public static class MappingDetail {
		private String mappingId;
		private String endpoint;
		private String direction;
		private String eventPath;
		private String jobType;
		private List<FieldMapping> fieldMappings;

		// Getter/Setter
		public String getMappingId() {
			return mappingId;
		}

		public void setMappingId(String mappingId) {
			this.mappingId = mappingId;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getDirection() {
			return direction;
		}

		public void setDirection(String direction) {
			this.direction = direction;
		}

		public String getEventPath() {
			return eventPath;
		}

		public void setEventPath(String eventPath) {
			this.eventPath = eventPath;
		}

		public String getJobType() {
			return jobType;
		}

		public void setJobType(String jobType) {
			this.jobType = jobType;
		}

		public List<FieldMapping> getFieldMappings() {
			return fieldMappings;
		}

		public void setFieldMappings(List<FieldMapping> fieldMappings) {
			this.fieldMappings = fieldMappings;
		}
	}

	public static class FieldMapping {
		private String source;
		private String target;
		private String format; // optional

		// Getter/Setter
		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getTarget() {
			return target;
		}

		public void setTarget(String target) {
			this.target = target;
		}

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}
	}
}