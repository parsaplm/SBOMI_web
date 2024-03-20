package com.parsa.middleware.service;

import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.processing.ImportData;
import com.parsa.middleware.repository.QueueRepository;
import com.parsa.middleware.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static com.parsa.middleware.constants.TcConstants.*;

@Service
public class ImportService {

    private final Logger logger = Logger.getLogger("SBOMILogger");

    @Autowired
    private QueueRepository queueRepository;
    @Autowired
    private ConfigProperties configProperties;

    private static final int DEFAULT_MAX_PARALLEL_IMPORTS = 1;


    private final ApplicationContext context;


    private BlockingQueue<File> importQueue = new LinkedBlockingQueue<>();

    private ThreadPoolExecutor taskExecutor;

    private volatile boolean cancellationRequested = false;

    private final Map<Integer, Future<?>> runningTasks = new ConcurrentHashMap<>();

    private final Map<ImportStatus, String> statusFolderMapping;


    String todoFolderPath, inProgressFolderPath, errorFolderPath, doneFolderPath, cancelFolderPath;

    ExecutorService executor;

    @Autowired
    public ImportService(ApplicationContext context) {
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


    }

    @Transactional
    public void refreshToDoFolderAndImport() {
        // Retrieve todo folder path
        String todoFolderPath = configProperties.getTransactionFolder() + "/" + FOLDER_TODO;
        try {
            // Get files from todo folder
            File todoFolder = new File(todoFolderPath);
            File[] files = todoFolder.listFiles();

            // Check if files exist
            if (files != null && files.length > 0) {
                for (File file : files) {
                    // Insert file information into the database

                    QueueEntity existingEntity = queueRepository.findFirstByFilenameOrderByTaskIdDesc(file.getName());
                    if (existingEntity != null) {
                        existingEntity.setCurrentStatus(ImportStatus.IN_SCOPE);
                        queueRepository.save(existingEntity);
                    } else {

                        Path sourceFolderPath = Paths.get(todoFolderPath, file.getName());

                        QueueEntity queueEntity = parseJson(new File(sourceFolderPath.toString()));

                        queueRepository.save(queueEntity);
                    }
                    importData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public void importData() {

        System.out.println("Importing JSON data...");

        // Fetch files from the database with status 'in scope'

        taskExecutor = new ThreadPoolExecutor(getMaxParallelImportsFromConfig(), getMaxParallelImportsFromConfig(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        System.out.println("Importing JSON data...");

        // Fetch files from the database with status 'in scope'
        List<QueueEntity> filesToProcess = queueRepository.findByCurrentStatus(ImportStatus.IN_SCOPE);

        for (QueueEntity fileToProcess : filesToProcess) {
            if (taskExecutor.getQueue().size() < getMaxParallelImportsFromConfig()) {
                String fileName = fileToProcess.getFilename();

                moveFileToInProgressFolder(fileName);

                fileToProcess.setCurrentStatus(ImportStatus.IN_PROGRESS);
                queueRepository.save(fileToProcess);

                // Execute task with associated task ID
                Future<?> task = taskExecutor.submit(() -> processFile(fileName, fileToProcess.getTaskId()));
                runningTasks.put(fileToProcess.getTaskId(), task);
            } else {
                break; // Break the loop if no space is available in the thread pool
            }
        }
    }

    private void startNextImport() {
        importData();
    }


    private void processFile(String fileName, int taskId) {
        try {
//            while (!Thread.currentThread().isInterrupted()) {
                // Set up cancellation flag
                AtomicBoolean isCancelled = new AtomicBoolean(false);

                // Read file content
                inProgressFolderPath = configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_IN_PROGRESS;
                Path sourceFolderPath = Paths.get(inProgressFolderPath, fileName);
                String content = new String(Files.readAllBytes(sourceFolderPath));

                // Perform import logic

                final JSONObject jsonObject = new JSONObject(content);


                QueueEntity element = parseJson(new File(sourceFolderPath.toString()));
                if (taskId > 0) {
                    element.setTaskId(taskId);
                }
                // Execute importStructure in a separate thread
//            Thread importThread = new Thread(() -> {
                // Import the file
                ImportData localImportData = context.getBean(ImportData.class);
                final String teamcenterObjectName = localImportData.importStructure(jsonObject, logger, element, isCancelled);
                // Update status based on import result
                if (teamcenterObjectName != null && !teamcenterObjectName.isEmpty()) {
                    moveFileToDoneFolder(fileName);
                    element.setCurrentStatus(ImportStatus.DONE);
                    element.setTeamcenterRootObject(teamcenterObjectName);
                } else {
                    moveFileToErrorFolder(fileName);
                    element.setCurrentStatus(ImportStatus.ERROR);
                }
                element.setEndImportDate(OffsetDateTime.now());
                queueRepository.save(element);
//            });

//            importThread.start();

                // Wait for the thread to complete or cancel it if needed
//            while (!importThread.isInterrupted()) {
//                // Check if cancellation is requested
//                if (isCancelled.get()) {
//                    importThread.interrupt(); // Cancel the import thread
//                    break;
//                }
//                Thread.sleep(1000); // Adjust sleep time as needed
//            }

//            }
        }catch (IOException | JSONException e) {
            // Handle exceptions
             Thread.currentThread().interrupt(); // Restore interrupted status
            e.printStackTrace();
        }
//        catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        finally {
            startNextImport();
        }


}

    private QueueEntity parseJson(File jsonFile) throws IOException, JSONException {
            logger.info(String.format("Read and set all values that are defined in the JSON file %s.", jsonFile.getName()));
            QueueEntity queueEntity = new QueueEntity();
            try {

              //  queueEntity.setCreationDate(LocalDateTime.now());

                queueEntity.setImportProgress(0);
                queueEntity.setImportTime(0);
//              thrownErrors = 0;

                queueEntity.setCurrentStatus(ImportStatus.IN_SCOPE);
                queueEntity.setFavorite(false);

              //  historyLog = "";
                queueEntity.setLogfileName("");
                queueEntity.setSbomiHostName("");
                queueEntity.setTeamcenterRootObject("<empty>");

                final JSONObject json = JsonUtil.readJsonFile(logger, jsonFile);

                queueEntity.setFilename(jsonFile.getName());
                queueEntity.setDrawingNumber(json.optString(TcConstants.JSON_DESIGN_NO));
                queueEntity.setNumberOfContainer(JsonUtil.getContainerCount(logger, json));
                queueEntity.setNumberOfObjects(JsonUtil.getObjectsCount(logger, json));
            } catch (final NullPointerException e) {
                logger.severe("Couldn't access the JSON file.");
                queueEntity.setFilename("No file found");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        return queueEntity;
        // Assuming Queue class is your entity and JSON matches its structure
    }


    public void cancelImport(int taskId) {
        Future<?> taskThread = runningTasks.get(taskId);
        if (taskThread != null) {
            taskThread.cancel(true); // Interrupt the thread associated with the task ID
        }
        // Optionally, you can remove the thread from the map if needed
        runningTasks.remove(taskId);

        Optional<QueueEntity> queueEntity = queueRepository.findById(taskId);
        QueueEntity entity = queueEntity.get();
        entity.setCurrentStatus(ImportStatus.CANCELED);
        queueRepository.save(entity);
    }

    public void setFavorite(int taskId) {
        Optional<QueueEntity> queueEntity = queueRepository.findById(taskId);
        QueueEntity entity = queueEntity.get();
        entity.setFavorite(true);
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
            Path sourcePath = Paths.get(sourceFolder, fileName);
            Path destinationPath = Paths.get(destinationFolder, fileName);
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
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
        String errorFolderPath = configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_IN_PROGRESS;
        moveFile(configProperties.getTransactionFolder() + "/" + FOLDER_TODO, errorFolderPath, fileName);
    }

    public boolean updateQueueStatus(int taskId, ImportStatus oldStatus, ImportStatus newStatus) {
        Optional<QueueEntity> queueEntityOptional = queueRepository.findById(taskId);
        if (queueEntityOptional.isEmpty()) {
            return false; // Queue entity not found
        }

        QueueEntity queueEntity = queueEntityOptional.get();

        // Validate the old status
//        if (!oldStatus.equalsIgnoreCase(queueEntity.getCurrentStatus())) {
//            return false; // Old status does not match current status
//        }

        // Validate the new status and update the queue entity
        if (!statusFolderMapping.containsKey(newStatus)) {
            return false; // Invalid new status
        }


        if (newStatus.toString().equalsIgnoreCase(ImportStatus.IN_REVIEW.toString()) || newStatus.toString().equalsIgnoreCase(ImportStatus.DELETED.toString()) ||
                newStatus.toString().equalsIgnoreCase(ImportStatus.IN_SCOPE.toString()) ) {
            queueEntity.setCurrentStatus(newStatus);
            queueRepository.save(queueEntity);
        }
         else
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
