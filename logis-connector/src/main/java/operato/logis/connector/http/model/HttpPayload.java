package operato.logis.connector.http.model;

import java.util.Map;

public class HttpPayload {
	private String url;
	private Map<String, Object> payload;

	public HttpPayload() {}
	public HttpPayload(String url, Map<String, Object> payload) {
		this.url = url;
		this.payload = payload;
	}

	public String getUrl() { return url; }
	public void setUrl(String url) { this.url = url; }

	public Map<String, Object> getPayload() { return payload; }
	public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}