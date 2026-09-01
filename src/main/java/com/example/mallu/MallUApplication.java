package com.example.mallu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.mallu.**.mapper")
@EnableScheduling
public class MallUApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallUApplication.class, args);
    }

}
