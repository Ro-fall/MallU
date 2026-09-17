package com.rofall.mallu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MallUApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallUApplication.class, args);
    }
}
