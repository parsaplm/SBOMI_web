package com.parsa.middleware.service;

import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.processing.ImportData;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.repository.QueueRepository;
import com.parsa.middleware.util.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import java.util.logging.Logger;

import static com.parsa.middleware.constants.TcConstants.*;

@Service
public class ImportService {

    private final Logger logger = Logger.getLogger("SBOMILogger");

    @Autowired
    private QueueRepository queueRepository;
    private final ConfigProperties configProperties;

    private static final int DEFAULT_MAX_PARALLEL_IMPORTS = 1;


    private final ApplicationContext context;


//    private ThreadPoolExecutor taskExecutor;

    private final Map<Integer, Future<?>> runningTasks = new ConcurrentHashMap<>();

    private final Map<ImportStatus, String> statusFolderMapping;


    String todoFolderPath, inProgressFolderPath, errorFolderPath, doneFolderPath, cancelFolderPath;

    private final Semaphore importSemaphore;

    private ExecutorService taskExecutor;

    public ExecutorService getTaskExecutor() {
        if (taskExecutor == null) {
            taskExecutor = Executors.newFixedThreadPool(getMaxParallelImportsFromConfig());
        }
        return taskExecutor;
    }

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


        importSemaphore = new Semaphore(getMaxParallelImportsFromConfig());

    }

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
                    // Check if the file name exists in the queue table with current status as "ERROR"
                    QueueEntity existingErrorEntity = queueRepository.findFirstByFilenameAndCurrentStatusOrderByCreationDateDesc(file.getName(), ImportStatus.ERROR);

                    if (existingErrorEntity != null) {
                        // If file with the same name exists in "ERROR" status, use that file
                        Path sourceFolderPath = Paths.get(todoFolderPath, file.getName());
                        QueueEntity queueEntity = parseJson(new File(sourceFolderPath.toString()));
                        queueEntity.setTaskId(existingErrorEntity.getTaskId());
                        queueEntity.setCreationDate(existingErrorEntity.getCreationDate());
                        queueEntity.setCurrentStatus(ImportStatus.IN_SCOPE);
                        queueRepository.save(queueEntity);
                    } else {
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
        importData();
    }

    @Transactional
    public void importData_old() {

        System.out.println("Importing JSON data...");

//        taskExecutor = new ThreadPoolExecutor(getMaxParallelImportsFromConfig(), getMaxParallelImportsFromConfig(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());


        // Fetch files from the database with status 'in scope' and order by favorite = true
        List<QueueEntity> filesToProcess = queueRepository.findByCurrentStatusOrderByIsFavoriteDesc(ImportStatus.IN_SCOPE);

        for (QueueEntity fileToProcess : filesToProcess) {
            if (runningTasks.size() < getMaxParallelImportsFromConfig()) {
                String fileName = fileToProcess.getFilename();

                moveFileToInProgressFolder(fileName);

                fileToProcess.setCurrentStatus(ImportStatus.IN_PROGRESS);
                queueRepository.save(fileToProcess);

                // Execute task with associated task ID
//                Future<?> task = taskExecutor.submit(() -> processFile(fileName, fileToProcess.getTaskId()));
//                runningTasks.put(fileToProcess.getTaskId(), task);
            } else {
                // Wait for some task to complete before proceeding
                break;
            }
        }
    }


    @Transactional
    public void importData() {

        System.out.println("Importing JSON data...");

//        taskExecutor = new ThreadPoolExecutor(getMaxParallelImportsFromConfig(), getMaxParallelImportsFromConfig(), 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());


        // Fetch files from the database with status 'in scope' and order by favorite = true
        List<QueueEntity> filesToProcess = queueRepository.findByCurrentStatusOrderByIsFavoriteDesc(ImportStatus.IN_SCOPE);

        for (QueueEntity fileToProcess : filesToProcess) {
            if (importSemaphore.tryAcquire()) {
                String fileName = fileToProcess.getFilename();

                moveFileToInProgressFolder(fileName);

                fileToProcess.setCurrentStatus(ImportStatus.IN_PROGRESS);
                queueRepository.save(fileToProcess);

                // Execute task with associated task ID
                Future<?> task = getTaskExecutor().submit(() -> processFile(fileName, fileToProcess.getTaskId()));
                runningTasks.put(fileToProcess.getTaskId(), task);


//                runningTasks.put(fileToProcess.getTaskId(), task);
            } else {
                // Wait for some task to complete before proceeding
                break;
            }
        }
    }


    private void processFile(String fileName, int taskId) {
        try {
            // Read file content
            inProgressFolderPath = configProperties.getTransactionFolder() + "/" + TcConstants.FOLDER_IN_PROGRESS;
            Path sourceFolderPath = Paths.get(inProgressFolderPath, fileName);
            String content = new String(Files.readAllBytes(sourceFolderPath));

            // Perform import logic
            final JSONObject jsonObject = new JSONObject(content);

            QueueEntity element;
            Optional<QueueEntity> optionalQueueEntity = queueRepository.findById(taskId);
            if (optionalQueueEntity.isEmpty()) {
                return;
            } else {
                element = optionalQueueEntity.get();
            }
            // Import the file
            ImportData localImportData = context.getBean(ImportData.class);
            final String teamcenterObjectName = localImportData.importStructure(jsonObject, element);
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
        } catch (IOException | JSONException e) {
            // Handle exceptions
            Thread.currentThread().interrupt(); // Restore interrupted status
            e.printStackTrace();
        } finally {
            runningTasks.remove(taskId);
            importSemaphore.release(); // Release the semaphore
            importDataForToDoFolder(); // Refresh and Start the next import
        }

    }

    private QueueEntity parseJson(File jsonFile) {
        logger.info(String.format("Read and set all values that are defined in the JSON file %s.", jsonFile.getName()));
        QueueEntity queueEntity = new QueueEntity();
        try {

            queueEntity.setImportProgress(0);
            queueEntity.setImportTime(0);
            queueEntity.setFavorite(false);

            queueEntity.setLogfileName("");
            queueEntity.setSbomiHostName("");
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

            // Check if file exists in destination folder
            if (Files.exists(destinationPath)) {
                // If file with same name exists in destination, delete it
                Files.delete(destinationPath);
            }

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
