package xyz.elidom.sys.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * @author Minu.Kim
 */
public class HttpRequestWrapper extends HttpServletRequestWrapper {
	private byte[] rawData;

	public HttpRequestWrapper(HttpServletRequest request) throws IOException {
		super(request);
		this.setRawData(this.toByteArray(super.getInputStream()));
	}

	private byte[] toByteArray(ServletInputStream inputStream) {
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			int nRead;
			byte[] data = new byte[1024];

			while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			return buffer.toByteArray();
		} catch (Exception e) {
			return new byte[0];
		}
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ServletImpl(new ByteArrayInputStream(this.getRawData()));
	}

	public byte[] getRawData() {
		return rawData;
	}

	public void setRawData(byte[] rawData) {
		this.rawData = rawData;
	}
}