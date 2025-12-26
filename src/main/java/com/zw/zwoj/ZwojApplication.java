package com.zw.zwoj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.zw.zwoj.mapper")
@EnableScheduling
public class ZwojApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZwojApplication.class, args);
    }

}
