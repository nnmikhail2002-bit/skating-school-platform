package com.skating.platform.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SkatingPlatformBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkatingPlatformBackendApplication.class, args);
    }

}
