package com.civicdesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CivicDeskApplicationTests {

	@Test
	void contextLoads() {
	}

}

// .\mvnw.cmd test
//.\mvnw.cmd clean test