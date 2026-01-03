package edu.hm.ccwi.matilda.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableEurekaClient
public class MatildaGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatildaGatewayApplication.class, args);
	}

}
