package com.controledegastos.backend;

import com.controledegastos.backend.config.RuntimeEnvironmentDefaults;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		RuntimeEnvironmentDefaults.applyToSystemProperties();
		SpringApplication application = new SpringApplication(BackendApplication.class);
		application.setDefaultProperties(RuntimeEnvironmentDefaults.resolve());
		application.run(args);
	}

}
