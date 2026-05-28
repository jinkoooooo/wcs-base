package xyz.anyware.wcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.client.RestTemplate;

@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties
@ComponentScan(basePackages = { "xyz.anyware.*", "xyz.anythings.*", "xyz.elidom.*", "operato.*" },
		excludeFilters = @ComponentScan.Filter(
		type = FilterType.REGEX,
		pattern = "operato\\.logis\\.connector\\.sap\\..*"
))
@ImportResource({ "classpath:/WEB-INF/application-context.xml", "classpath:/WEB-INF/dataSource-context.xml" })
public class AnywareWcsApplication {
	
	public static void main(String[] args) {
		System.setProperty("spring.jdbc.getParameterType.ignore", "true");
		SpringApplication.run(AnywareWcsApplication.class, args);
	}
	
	@Bean
	DefaultCookieSerializer defaultCookieSerializer(){
        DefaultCookieSerializer defaultCookieSerializer = new DefaultCookieSerializer();
        defaultCookieSerializer.setCookieName("CTSESSION");
        return defaultCookieSerializer;
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}