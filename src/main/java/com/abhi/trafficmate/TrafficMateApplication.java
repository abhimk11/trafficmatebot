package com.abhi.trafficmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrafficMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficMateApplication.class, args);
    }

}
