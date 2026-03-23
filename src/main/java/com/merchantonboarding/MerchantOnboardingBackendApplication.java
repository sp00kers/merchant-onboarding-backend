package com.merchantonboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MerchantOnboardingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MerchantOnboardingBackendApplication.class, args);
	}

}
