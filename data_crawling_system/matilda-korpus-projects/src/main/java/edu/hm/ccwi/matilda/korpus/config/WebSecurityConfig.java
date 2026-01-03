package edu.hm.ccwi.matilda.korpus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan("edu.hm.ccwi.matilda.korpus")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	public WebSecurityConfig() {
		super();
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}

	@Override
	protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
		// WARNING: INSECURE - Hardcoded credentials for development only!
		// TODO: Replace with proper authentication (environment variables, database, OAuth2, etc.)
		// DO NOT USE IN PRODUCTION!
		String adminUser = System.getenv("ADMIN_USER") != null ? System.getenv("ADMIN_USER") : "temporary";
		String adminPass = System.getenv("ADMIN_PASSWORD") != null ? System.getenv("ADMIN_PASSWORD") : "temporary";
		String regularUser = System.getenv("REGULAR_USER") != null ? System.getenv("REGULAR_USER") : "user";
		String regularPass = System.getenv("REGULAR_PASSWORD") != null ? System.getenv("REGULAR_PASSWORD") : "userPass";
		
		auth.inMemoryAuthentication().withUser(adminUser).password(adminPass).roles("ADMIN")
				.and().withUser(regularUser).password(regularPass).roles("USER");
	}

	@Override
	protected void configure(final HttpSecurity http) throws Exception {// @formatter:off
		http.csrf().disable().authorizeRequests()
				.antMatchers("/api/csrfAttacker*").permitAll()
				.antMatchers("/api/customer/**").permitAll()
				.antMatchers("/api/foos/**").authenticated()
				.antMatchers("/api/async/**").permitAll()
				.antMatchers("/api/admin/**").hasRole("ADMIN")
				.and()
				.httpBasic()
				.and()
				.logout();
	} // @formatter:on

	@Bean
	public SimpleUrlAuthenticationFailureHandler myFailureHandler() {
		return new SimpleUrlAuthenticationFailureHandler();
	}
}