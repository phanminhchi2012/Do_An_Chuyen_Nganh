package com.example.scalys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScalysApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScalysApplication.class, args);
	}

}
