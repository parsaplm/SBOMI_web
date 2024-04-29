package com.parsa.middleware;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
@EnableEncryptableProperties
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);


    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

}

