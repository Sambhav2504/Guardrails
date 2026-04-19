package com.example.guardrail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GuardrailApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuardrailApplication.class, args);
    }
}
