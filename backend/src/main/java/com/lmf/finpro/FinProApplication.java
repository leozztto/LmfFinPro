package com.lmf.finpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinProApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinProApplication.class, args);
    }
}
