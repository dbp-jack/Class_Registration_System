package com.course.classregistration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ClassRegistrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClassRegistrationApplication.class, args);
	}

}
