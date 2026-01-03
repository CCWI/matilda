package edu.hm.ccwi.matilda.libmngm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SecurityConfigurationBuilder;

@SpringBootApplication
@EnableEurekaClient
public class MatildaLibMngmApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatildaLibMngmApplication.class, args);
    }

	@Bean
	public SecurityConfiguration securityConfiguration() {
		return SecurityConfigurationBuilder.builder()
				.enableCsrfSupport(true)
				.build();
	}
}