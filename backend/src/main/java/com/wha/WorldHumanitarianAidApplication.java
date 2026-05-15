package com.wha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
public class WorldHumanitarianAidApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorldHumanitarianAidApplication.class, args);
    }
}
