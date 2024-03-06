package com.parsa.middleware.controller;

import com.parsa.middleware.Application;
import com.parsa.middleware.config.Loggable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Controller class for handling application-related operations.
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    /**
     * Restarts the Spring Boot application.
     *
     * @return A message indicating the restart process.
     */
    @Loggable
    @PostMapping("/restart")
    public String restartApplication() {
        ApplicationArguments args = applicationContext.getBean(ApplicationArguments.class);

        Thread thread = new Thread(() -> {
            applicationContext.close();
            applicationContext = SpringApplication.run(Application.class, args.getSourceArgs());
        });

        thread.setDaemon(false);
        thread.start();

        return "Application restarted";
    }
}

