package com.dumont.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.dumont" })
public class JavaServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaServicesApplication.class, args);
	}

}
