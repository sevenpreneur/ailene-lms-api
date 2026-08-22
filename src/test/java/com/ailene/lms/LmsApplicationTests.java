package com.ailene.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LmsApplicationTests {

	// LmsApplication's own static block never runs here -- @SpringBootTest only reads its bytecode metadata, it never loads the class.
	static {
		LmsApplication.loadDotenv();
	}

	@Test
	void contextLoads() {
	}

}
