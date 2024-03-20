package com.parsa.middleware.service;

import com.parsa.middleware.config.ConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.logging.Logger;

import static com.parsa.middleware.constants.TcConstants.FOLDER_DELETED;

@Component
public class FileCleanupScheduler {

    private final Logger logger = Logger.getLogger("SBOMILogger");


    @Autowired
    ConfigProperties configProperties;

    @Scheduled(cron = "#{@configProperties.getDeleteSchedule()}")
    public void deleteFilesScheduled() {
        String deleteFolderPath = configProperties.getTransactionFolder() + File.separator + FOLDER_DELETED;
        File deleteFolder = new File(deleteFolderPath);
        if (!deleteFolder.exists()) {
            logger.severe("Delete folder does not exist.");
            return;
        }

        File[] filesToDelete = deleteFolder.listFiles();
        if (filesToDelete != null) {
            for (File file : filesToDelete) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        logger.info("Deleted file: " + file.getName());
                    } else {
                        logger.severe("Failed to delete file: " + file.getName());
                    }
                }
            }
        }
    }
}