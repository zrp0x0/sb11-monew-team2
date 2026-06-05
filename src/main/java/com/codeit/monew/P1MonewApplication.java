package com.codeit.monew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class P1MonewApplication {

    public static void main(String[] args) {
        SpringApplication.run(P1MonewApplication.class, args);
    }
}