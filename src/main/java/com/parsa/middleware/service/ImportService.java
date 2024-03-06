package com.parsa.middleware.service;

import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.processing.ImportData;
import com.parsa.middleware.repository.QueueRepository;
import com.parsa.middleware.util.JsonUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Service
public class ImportService {

    private final Logger LOGGER= Logger.getLogger("SBOMILogger");

    @Value("${application.transaction-folder-path}")
    private String baseFolderPath;

    @Autowired
    private QueueRepository queueRepository;
    @Autowired
    private ImportData importData;



    @Transactional
    public void importData() {

        System.out.println("Importing JSON data...");
        String todoFolderPath = baseFolderPath + "/" + TcConstants.FOLDER_TODO;
        String inProgressFolderPath = baseFolderPath + "/" + TcConstants.FOLDER_IN_PROGRESS;
        File todoFolder = new File(todoFolderPath);
        File[] files = todoFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                try {
                    // Read file content
                    String content = new String(Files.readAllBytes(file.toPath()));

                    // Save content to the database (Assuming you have a repository called QueueRepository)
                    QueueEntity queue ;
                    queue = parseJson(file);
                    //queue.setContent(content);
                    queue.setFilename(file.getName());
                    queue.setCurrentStatus(TcConstants.FOLDER_IN_PROGRESS);
                    queueRepository.save(queue);

                    // Move file to the inprogress folder
                    Path sourcePath = file.toPath();
                    Path destinationPath = Paths.get(inProgressFolderPath, file.getName());
                    Files.move(sourcePath, destinationPath);
                    JSONObject jsonObject = new JSONObject(content);
                    jsonObject.put(TcConstants.DATABASE_FILENAME, queue.getFilename());

                    importData.importStructure(jsonObject);

                    System.out.println("File '" + file.getName() + "' imported successfully.");
                } catch (IOException e) {
                    System.err.println("Error reading or moving file: " + e.getMessage());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private QueueEntity parseJson(File jsonFile) throws IOException {
            LOGGER.info(String.format("Read and set all values that are defined in the JSON file %s.", jsonFile.getName()));
            QueueEntity queueEntity = new QueueEntity();
            try {

              //  queueEntity.setCreationDate(LocalDateTime.now());

                queueEntity.setImportProgress(0);
                queueEntity.setImportTime(0);
//              thrownErrors = 0;

                queueEntity.setCurrentStatus(ImportStatus.IN_SCOPE.toString());
                queueEntity.setFavorite(false);

              //  historyLog = "";
                queueEntity.setLogfileName("");
                queueEntity.setSbomiHostName("");
                queueEntity.setTeamcenterRootObject("<empty>");

                final JSONObject json = JsonUtil.readJsonFile(LOGGER, jsonFile);

                queueEntity.setFilename(jsonFile.getName());
                queueEntity.setDrawingNumber(json.optString(TcConstants.JSON_DESIGN_NO));
                queueEntity.setNumberOfContainer(JsonUtil.getContainerCount(LOGGER, json));
                queueEntity.setNumberOfObjects(JsonUtil.getObjectsCount(LOGGER, json));
            } catch (final NullPointerException e) {
                LOGGER.severe("Couldn't access the JSON file.");
                queueEntity.setFilename("No file found");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        return queueEntity;
        // Assuming Queue class is your entity and JSON matches its structure
    }

}
