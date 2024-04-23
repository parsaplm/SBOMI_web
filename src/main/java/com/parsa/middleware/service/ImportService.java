package com.parsa.middleware.service;

import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.processing.ImportData;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.repository.QueueRepository;
import com.parsa.middleware.util.JsonUtil;
import com.parsa.middleware.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.parsa.middleware.constants.TcConstants.*;

@Service
public class ImportService {

    private static final int DEFAULT_MAX_PARALLEL_IMPORTS = 1;
    final ThreadPoolExecutor threadPool;
    private final Logger logger = Logger.getLogger("SBOMILogger");
    private final ConfigProperties configProperties;
    private final ApplicationContext context;
    private final Map<ImportStatus, String> statusFolderMapping;
    @Autowired
    private QueueRepository queueRepository;
    private List<Thread> threadList = new ArrayList<>();

    @Autowired
    public ImportService(ConfigProperties configProperties, ApplicationContext context) {
        this.configProperties = configProperties;
        this.context = context;
        // Initialize status to folder mapping
        this.statusFolderMapping = new HashMap<>();
        statusFolderMapping.put(ImportStatus.IN_SCOPE, FOLDER_TODO);
        statusFolderMapping.put(ImportStatus.CANCELED, FOLDER_CANCELED);
        statusFolderMapping.put(ImportStatus.IN_PROGRESS, FOLDER_IN_PROGRESS);
        statusFolderMapping.put(ImportStatus.ERROR, FOLDER_ERROR);
        statusFolderMapping.put(ImportStatus.DONE, FOLDER_DONE);
        statusFolderMapping.put(ImportStatus.DELETED, FOLDER_DELETED);
        statusFolderMapping.put(ImportStatus.IN_REVIEW, FOLDER_IN_REVIEW);
        threadPool = createThreadPool();

    }

    public void startImport() {
        logger.info("Start the import of all files in the 'todo' folder.");
        updateThreadList(); // Update list and start imports
    }

    private ThreadPoolExecutor createThreadPool() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(getMaxParallelImportsFromConfig());
    }

    private void updateThreadList() {
        synchronized (threadList) {
            // Get queue elements and filter based on status
            List<QueueEntity> todoList = queueRepository.findByCurrentStatus(ImportStatus.IN_SCOPE);

            // Sort as in the original code (optional)
            todoList.sort(Comparator.comparing(QueueEntity::isFavorite, Comparator.reverseOrder())
                    .thenComparing(QueueEntity::getTaskId));

            // Start new imports if there's space available
            int maxParallelImports = getMaxParallelImportsFromConfig();
            for (QueueEntity queueEntity : todoList) {
                if (threadList.size() >= maxParallelImports) {
                    break; // Maximum parallel imports reached
                }

                boolean alreadyImporting = false;
                for (Thread thread : threadList) {
                    // Check if the file is already being imported
                    if (thread.getName().equals(queueEntity.getFilename())) {
                        alreadyImporting = true;
                        break;
                    }
                }
                if (!alreadyImporting) {
                    Thread importThread = createImportThread(queueEntity);
                    threadList.add(importThread);
                    importThread.start();
                }
            }
        }
    }
    @Transactional
    public Thread createImportThread(QueueEntity element) {
        Thread importThread  = new Thread(() -> {
            try {

                // Fetch the QueueEntity from the database to get the latest status
                Optional<QueueEntity> optionalEntity = queueRepository.findById(element.getTaskId());

                // Check if the entity is present and if the status is CANCELED
                if (optionalEntity.isPresent()) {
                    QueueEntity updatedElement = optionalEntity.get();
                    if (updatedElement.getCurrentStatus().equals(ImportStatus.CANCELED)) {
                        return; // Return if the status is CANCELED
                    }
                } else {
                    // Handle case where entity is not found
                    logger.severe("Entity not found in the database. %s"+ element.getFilename());
                    return; // Return early if the entity is not found
                }


                // File access and status checks
                logger.info(String.format("Start the import of the file %s.", element.getFilename()));


                String filePath = Paths.get(configProperties.getTransactionFolder() + File.separator + FOLDER_TODO
                        + File.separator + element.getFilename()).toString();
                if (!new File(filePath).exists()) {
                    logger.severe(String.format("The JSON file %s is not accessible. Can't start the import.",
                            element.getFilename()));
                    return;
                }

                if (!element.getCurrentStatus().equals(ImportStatus.IN_SCOPE)) {
                    logger.warning(String.format("The status of the import of %s isn't IN_SCOPE anymore.",
                            element.getFilename()));
                    return;
                }

                // Attempt to acquire a lock (non-blocking) on the file
                File file = new File(filePath);
               /* FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
                FileLock lock = channel.tryLock(0L, Long.MAX_VALUE, true);*/

               /* try {
                    if (lock != null) {*/

                            // Read file content while holding the lock
                            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
                            JSONObject jsonObject = new JSONObject(jsonString);

                            // Reset import status and progress (assuming relevant methods in QueueElement)
                            element.setImportProgress(0);


                            // Change import status to IN_PROGRESS (assuming relevant method in QueueElement)
                            //element.setCurrentStatus(ImportStatus.IN_PROGRESS);

                            //queueRepository.save(element); // Update progress in repository
                            //TODO we don't need this method
                            //moveFileToInProgressFolder(element.getFilename());
                            //move file to progress folder
                            FileManagement.moveFile(element, ImportStatus.IN_PROGRESS, configProperties.getTransactionFolder(), logger);
                            element.setCurrentStatus(ImportStatus.IN_PROGRESS);
                            // Set import start time
                            element.setStartImportDate(OffsetDateTime.now());
                            element.setSbomiHostName(Utility.getHostName());
//                        updateQueueElementStartTime(element); // Update start time in repository
                            queueRepository.save(element);
                            // Import logic (replace with your actual implementation)
                            ImportData localImportData = context.getBean(ImportData.class);
                            final String teamcenterObjectName = localImportData.importStructure(jsonObject, element);
                            // Update status based on import result
                            if (teamcenterObjectName != null && !teamcenterObjectName.isEmpty()) {
                                //TODO we don't need this method
                                //moveFileToDoneFolder(element.getFilename());
                                //element.setCurrentStatus(ImportStatus.DONE);
                                element.setTeamcenterRootObject(teamcenterObjectName);
                                FileManagement.moveFile(element, ImportStatus.DONE, configProperties.getTransactionFolder(), logger);
                                element.setCurrentStatus(ImportStatus.DONE);
                            } else {

                                Optional<QueueEntity> updateEntityOptional = queueRepository.findById(element.getTaskId());

                                // Check if the entity is present and if the status is CANCELED
                                if (updateEntityOptional.isPresent()) {
                                    QueueEntity updatedElement = updateEntityOptional.get();
                                    if (updatedElement.getCurrentStatus().equals(ImportStatus.CANCELED)) {
                                        //element.setCurrentStatus(ImportStatus.CANCELED);
                                        //TODO we don't need this method
                                        //moveFileToInCancelFolder(element.getFilename());
                                        FileManagement.moveFile(element, ImportStatus.CANCELED, configProperties.getTransactionFolder(), logger);
                                        element.setCurrentStatus(ImportStatus.CANCELED);
                                    } else {
                                        //TODO we don't need this method
                                        //moveFileToErrorFolder(element.getFilename());
                                        //move file to error folder
                                        //element.setCurrentStatus(ImportStatus.ERROR);
                                        FileManagement.moveFile(element, ImportStatus.ERROR, configProperties.getTransactionFolder(), logger);
                                        element.setCurrentStatus(ImportStatus.ERROR);
                                    }

//                                if (element.getCurrentStatus().equals(ImportStatus.CANCELED)) {
////                                // Import canceled
////                                // (No need to update queue or finished table as assumed in original code)
//                                    moveFileToInCancelFolder(element.getFilename());
////                                return;
//                                }
                                }

                            }
                            element.setEndImportDate(OffsetDateTime.now());
                            queueRepository.save(element);
                        /*} else{
                            logger.severe("File is already locked by another thread/process.");
                        }

                } finally {
                    if (lock != null) {
                        lock.release();
                    }
                }*/

            } catch (IOException e) {
                logger.severe(e.getMessage());
                element.setCurrentStatus(ImportStatus.ERROR);
                queueRepository.save(element);
                e.printStackTrace();
            } catch (Exception e) {
                // Unexpected exception
                element.setCurrentStatus(ImportStatus.ERROR);
                queueRepository.save(element);
                logger.severe(e.getMessage());
                e.printStackTrace();
            } finally {
                // (Assuming session handling is not relevant in this context)
                synchronized (threadList) {
                    threadList.remove(Thread.currentThread());
                }
                importDataForToDoFolder();
            }
        });

        importThread.setName(element.getFilename());
        return importThread;
    }


    //new code ends

    @Transactional
    public void refreshToDoFolder() {
        // Retrieve todo folder path
        String todoFolderPath = configProperties.getTransactionFolder() + "/" + FOLDER_TODO;
        try {
            // Get files from todo folder
            File todoFolder = new File(todoFolderPath);
            File[] files = todoFolder.listFiles();
            // Check if files exist
            if (files != null && files.length > 0) {
                for (File file : files) {
                    //validate file extension as JSON

                    if(!(TcConstants.FILE_EXT_JSON.equalsIgnoreCase(Utils.getFileExtension(file)))){
                        continue;
                    }
                    // Check if the file name exists in the queue table with current status as "ERROR"
                    QueueEntity existingErrorEntity = queueRepository.findFirstByFilenameAndCurrentStatusOrderByCreationDateDesc(file.getName(), ImportStatus.ERROR);

                    QueueEntity existingInScopeEntity = queueRepository.findFirstByFilenameAndCurrentStatusOrderByCreationDateDesc(file.getName(), ImportStatus.IN_SCOPE);

                    if (existingErrorEntity != null) {
                        // If file with the same name exists in "ERROR" status, use that file
                        Path sourceFolderPath = Paths.get(todoFolderPath, file.getName());
                        QueueEntity queueEntity = parseJson(new File(sourceFolderPath.toString()));
                        queueEntity.setTaskId(existingErrorEntity.getTaskId());
                        queueEntity.setCreationDate(existingErrorEntity.getCreationDate());
                        queueEntity.setCurrentStatus(ImportStatus.IN_SCOPE);
                        queueRepository.save(queueEntity);
                    } else if (existingInScopeEntity != null) {
                        //File is already in scope, don't do anything..
                    } else {
                        System.out.println("Saving new file..." + file.getName());
                        Path sourceFolderPath = Paths.get(todoFolderPath, file.getName());
                        QueueEntity queueEntity = parseJson(new File(sourceFolderPath.toString()));
                        queueEntity.setCurrentStatus(ImportStatus.IN_SCOPE);
                        queueRepository.save(queueEntity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Transactional
    public void importDataForToDoFolder() {
        refreshToDoFolder();
        startImport();
    }

    private QueueEntity parseJson(File jsonFile) {
        logger.info(String.format("Read and set all values that are defined in the JSON file %s.", jsonFile.getName()));
        QueueEntity queueEntity = new QueueEntity();
        try {

            queueEntity.setImportProgress(0);
            queueEntity.setImportTime(0);
            queueEntity.setFavorite(false);

            queueEntity.setLogfileName("");
            queueEntity.setTeamcenterRootObject("<empty>");

            final JSONObject json = JsonUtil.readJsonFile(logger, jsonFile);

            queueEntity.setFilename(jsonFile.getName());
            queueEntity.setDrawingNumber(json.optString(TcConstants.JSON_DESIGN_NO));
            queueEntity.setNumberOfContainer(JsonUtil.getContainerCount(logger, json));
            queueEntity.setNumberOfObjects(JsonUtil.getObjectsCount(logger, json));
            queueEntity.setSbomiHostName(Utility.getHostName());
        } catch (final NullPointerException e) {
            logger.severe("Couldn't access the JSON file.");
            queueEntity.setFilename("No file found");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return queueEntity;
        // Assuming Queue class is your entity and JSON matches its structure
    }

    @Transactional
    public void cancelImport(int taskId) {

        Optional<QueueEntity> queueEntity = queueRepository.findById(taskId);
        QueueEntity entity = queueEntity.get();
        entity.setCurrentStatus(ImportStatus.CANCELED);
        queueRepository.save(entity);
    }

    public void setFavorite(int taskId) {
        Optional<QueueEntity> queueEntity = queueRepository.findById(taskId);
        QueueEntity entity = queueEntity.get();
        entity.setFavorite(!entity.isFavorite());
        queueRepository.save(entity);
    }


    private int getMaxParallelImportsFromConfig() {
        String parallelImports = configProperties.getParallelImport();
        if (!StringUtils.isEmpty(parallelImports)) {
            try {
                return Integer.parseInt(parallelImports);
            } catch (NumberFormatException e) {
                System.err.println("Invalid value for maximum parallel imports: " + parallelImports);
            }
        }
        return DEFAULT_MAX_PARALLEL_IMPORTS;
    }


    private boolean moveFile(String sourceFolder, String destinationFolder, String fileName) {
        try {
            logger.info(String.format("Moving file %s from %s to %s.", fileName, sourceFolder, destinationFolder));
            Path sourcePath = Paths.get(sourceFolder, fileName);
            Path destinationPath = Paths.get(destinationFolder, fileName);
            File sourceFile = new File(sourceFolder);
            File destinationFile = new File(destinationFolder);


            // Check if file exists in destination folder
            /*if (Files.exists(destinationPath)) {
                // If file with same name exists in destination, delete it
                Files.delete(destinationPath);
            }*/

            //Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(sourceFile.toPath().resolve(fileName), destinationFile.toPath().resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            // Handle file moving failure
            System.err.println("Error moving file to destination folder: " + e.getMessage());
            return false;
        }
    }

    private void moveFileToDoneFolder(String fileName) {
        String doneFolderPath = configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_DONE;
        moveFile(configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_IN_PROGRESS, doneFolderPath, fileName);
    }

    private void moveFileToErrorFolder(String fileName) {
        String errorFolderPath = configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_ERROR;
        moveFile(configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_IN_PROGRESS, errorFolderPath, fileName);
    }

    private void moveFileToInProgressFolder(String fileName) {
        /*String errorFolderPath = configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_IN_PROGRESS;
        moveFile(configProperties.getTransactionFolder() + "/" + FOLDER_TODO, errorFolderPath, fileName);*/
        QueueEntity queueEntity = new QueueEntity();
        try {
            queueEntity.setCurrentStatus(ImportStatus.IN_SCOPE);
            queueEntity.setFilename(fileName);
            FileManagement.moveFile(queueEntity, ImportStatus.IN_PROGRESS, configProperties.getTransactionFolder(), logger);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void moveFileToInCancelFolder(String fileName) {
        String cancelFolderPath = configProperties.getTransactionFolder() + "/" + FOLDER_CANCELED;
        moveFile(configProperties.getTransactionFolder() + "/" + FOLDER_IN_PROGRESS, cancelFolderPath, fileName);
    }

    public boolean updateQueueStatus(int taskId, ImportStatus oldStatus, ImportStatus newStatus) {
        Optional<QueueEntity> queueEntityOptional = queueRepository.findById(taskId);
        if (queueEntityOptional.isEmpty()) {
            return false; // Queue entity not found
        }

        QueueEntity queueEntity = queueEntityOptional.get();

        // Validate the new status and update the queue entity
        if (!statusFolderMapping.containsKey(newStatus)) {
            return false; // Invalid new status
        }


        if (newStatus.toString().equalsIgnoreCase(ImportStatus.IN_REVIEW.toString()) || newStatus.toString().equalsIgnoreCase(ImportStatus.DELETED.toString()) ||
                newStatus.toString().equalsIgnoreCase(ImportStatus.IN_SCOPE.toString())) {
            queueEntity.setCurrentStatus(newStatus);
            queueRepository.save(queueEntity);
        } else
            return false; // Invalid new status

        String sourceFolder = configProperties.getTransactionFolder() + File.separator + statusFolderMapping.get(oldStatus);
        String destinationFolder = configProperties.getTransactionFolder() + File.separator + statusFolderMapping.get(newStatus);
        String fileName = queueEntity.getFilename();
        boolean moveSuccessful = moveFile(sourceFolder, destinationFolder, fileName);
        if (!moveSuccessful) {
            return false;
        }

        return true; // Or any other indication of success
    }
}
