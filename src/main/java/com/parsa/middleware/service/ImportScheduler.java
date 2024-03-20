package com.parsa.middleware.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ImportScheduler {
    @Autowired
    private ImportService importService;


    @Scheduled(cron = "#{@configProperties.getImportCronExpression()}")
    public void importData() {


//        importService.importData(); // Trigger import process according to the configured schedule
        importService.refreshToDoFolderAndImport();

    }

    @PostConstruct
    public void importDataOnStartup() {
        //importService.importData(); // Trigger import process when the application starts
        importService.refreshToDoFolderAndImport();
    }

}
