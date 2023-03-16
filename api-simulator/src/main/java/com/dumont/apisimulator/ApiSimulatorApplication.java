package com.dumont.apisimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.dumont" })
public class ApiSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiSimulatorApplication.class, args);
	}

}
