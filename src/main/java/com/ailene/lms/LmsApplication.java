package com.ailene.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class LmsApplication {

	// Runs on class load, before SpringApplication.run(), so DB_URL/etc. work without exporting .env manually (see LmsApplicationTests for the @SpringBootTest equivalent).
	static {
		loadDotenv();
	}

	public static void main(String[] args) {
		SpringApplication.run(LmsApplication.class, args);
	}

	// Package-private so LmsApplicationTests can call it too -- @SpringBootTest never actually loads this class (Spring only reads its bytecode metadata), so the static block above never runs for tests.
	static void loadDotenv() {
		Path envFile = Path.of(".env");
		if (!Files.exists(envFile)) {
			return;
		}

		try {
			for (String line : Files.readAllLines(envFile)) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
					continue;
				}
				int separatorIndex = trimmed.indexOf('=');
				String key = trimmed.substring(0, separatorIndex).trim();
				String value = trimmed.substring(separatorIndex + 1).trim();
				if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}
				if (System.getProperty(key) == null && System.getenv(key) == null) {
					System.setProperty(key, value);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
