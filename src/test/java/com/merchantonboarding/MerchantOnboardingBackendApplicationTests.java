package com.merchantonboarding;

// import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Integration test — uses @SpringBootTest to boot the entire Spring application context
// This verifies that all beans, configurations, database connections, and Kafka settings load without errors
@SpringBootTest
// @Disabled("Requires running MySQL and Kafka containers")
class MerchantOnboardingBackendApplicationTests {

	// Tests that the full Spring application context starts up successfully
	// If any bean fails to initialize (e.g., missing config, broken dependency), this test will fail
	@Test
	void contextLoads() {
	}

}
