package com.parsa.middleware.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ImportScheduler {
    @Autowired
    private ImportService importService;


    @Scheduled(cron = "#{@configProperties.getUpdateSchedule()}")
    public void importData() {

        System.out.println("ImportScheduler.importData() is called");
        importService.importDataForToDoFolder();

    }

    @PostConstruct
    public void importDataOnStartup() {
        //importService.importData(); // Trigger import process when the application starts
        importService.importDataForToDoFolder();
    }

}
