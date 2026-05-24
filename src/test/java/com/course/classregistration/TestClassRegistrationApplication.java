package com.course.classregistration;

import org.springframework.boot.SpringApplication;

public class TestClassRegistrationApplication {

	public static void main(String[] args) {
		SpringApplication.from(ClassRegistrationApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
