package com.controledegastos.backend;

import com.controledegastos.backend.config.RuntimeEnvironmentDefaults;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(BackendApplication.class);
		application.setDefaultProperties(RuntimeEnvironmentDefaults.resolve());
		application.run(args);
	}

}
