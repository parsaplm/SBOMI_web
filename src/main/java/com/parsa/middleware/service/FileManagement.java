package com.parsa.middleware.service;

import com.parsa.middleware.businessobjects.Dataset;
import com.parsa.middleware.businessobjects.StructureObject;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.session.AppXSession;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class FileManagement {

	/**
	 * Create the transaction directories if they don't exist yet.
	 * 
	 * @param directoryPath
	 */
	public static boolean createTransactionDirectory(String directoryPath, Logger logger) {
		logger.info("Check, if the transaction folders must be created.");

		final File transactionFolder = new File(directoryPath);

		try {
			// Check if we have permission to create the transaction folder if it doesn't
			// exist
			if (!transactionFolder.exists() && !Files.isWritable(transactionFolder.getParentFile().toPath())) {
				logger.severe(String.format("Cant' write to the folder %s. Please check the permissions.",
						transactionFolder.getParent()));
				return false;
			}

			if (!transactionFolder.exists()) {
				logger.info("Creating the transaction folder.");
				transactionFolder.mkdirs();
			}

			// Check if you can write to the transaction folder
			if (!Files.isWritable(transactionFolder.toPath())) {
				logger.info(String.format("Can't write to the folder %s. Please check the permissions.",
						transactionFolder.getPath()));
				return false;
			}

			final File todoFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_TODO);
			if (!todoFolder.exists()) {
				logger.info("Creating the transaction folder.");
				todoFolder.mkdirs();
			}

			final File progressFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_IN_PROGRESS);
			if (!progressFolder.exists()) {
				logger.info("Creating the inProgress folder.");
				progressFolder.mkdirs();
			}

			final File reviewFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_IN_REVIEW);
			if (!reviewFolder.exists()) {
				logger.info("Creating the inReview folder.");
				reviewFolder.mkdirs();
			}

			final File doneFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_DONE);
			if (!doneFolder.exists()) {
				logger.info("Creating the done folder.");
				doneFolder.mkdirs();
			}

			final File cancelFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_CANCELED);
			if (!cancelFolder.exists()) {
				logger.info("Creating the canceled folder.");
				cancelFolder.mkdirs();
			}

			final File errorFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_ERROR);
			if (!errorFolder.exists()) {
				logger.info("Creating the error folder.");
				errorFolder.mkdirs();
			}

			final File deletedFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_DELETED);
			if (!deletedFolder.exists()) {
				logger.info("Creating the deleted folder.");
				deletedFolder.mkdirs();
			}

			final File updatedFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_UPDATED);
			if (!updatedFolder.exists()) {
				logger.info("Creating the updated folder.");
				updatedFolder.mkdirs();
			}

			final File datasetFolder = new File(directoryPath + File.separator + TcConstants.FOLDER_DATASET);
			if (!datasetFolder.exists()) {
				logger.info("Creating the dataset folder.");
				datasetFolder.mkdirs();
			}

			return true;
		} catch (final SecurityException e) {
			logger.severe(String.format("Can't create the transaction folder %s. Please check the write permissions.",
					directoryPath));

			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Create a new JSON file with the name from the given QueueElement and the
	 * JSONObject as content. The filename gets the ending 'updated', since the
	 * given JSONObject has some attributes values that are changed from the
	 * original file.
	 * 
	 * @param queueElement
	 * @param structureObject
	 * @param transactionFolder
	 * @param logger
	 */
	public static void createUpdatedFile(QueueEntity queueElement, StructureObject structureObject,
										 String transactionFolder, Logger logger, AppXSession session) {
		logger.info(String.format("Updating the data of the JSON file %s.", queueElement.getFilename()));

		try {

			DataManagementService.getService(session.getConnection()).getProperties(
					new ModelObject[] { structureObject.getBomLine() },
					new String[] { TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID,
							TcConstants.TEAMCENTER_BOMLINE_CURRENT_REVISION_ID });

			final String newFilename = queueElement.getDrawingNumber() + "~"
					+ structureObject.getBomLine().get_bl_item_current_id() + "~"
					+ structureObject.getBomLine().get_bl_rev_current_revision_id() + ".json";

			final BufferedWriter writer = new BufferedWriter(new FileWriter(
					transactionFolder + File.separator + TcConstants.FOLDER_UPDATED + File.separator + newFilename));
			writer.write(structureObject.getJsonObject().toString());
			writer.close();
		} catch (final IOException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Delete all files from the datasets from the given List.
	 * 
	 * @param datasetFiles
	 * @param logger
	 */
	public static void deleteDatasets(List<Dataset> datasetFiles, Logger logger) {
		logger.info("Delete all datasets that are uploaded to Teamcenter.");

		for (final Dataset dataset : datasetFiles) {
			final File datasetFile = dataset.getDatasetFile();

			logger.info(String.format("Delete the file %s.", datasetFile.getName()));
			datasetFile.delete();
		}
	}

	/**
	 * Delete the file that's referenced by the given QueueElement.
	 * 
	 * @param queueElement
	 * @param transactionFolder
	 * @param logger
	 * @return
	 * @throws IOException
	 */
	public static boolean deleteFile(QueueEntity queueElement, String transactionFolder, Logger logger)
			throws IOException {
		logger.info("Deleting the file " + queueElement.getFilename());

		final File file = new File(getPathToStatus(queueElement.getCurrentStatus(), transactionFolder, logger)
				+ File.separator + queueElement.getFilename());
		return Files.deleteIfExists(file.toPath());
	}

	/**
	 * Get a list of all files that are in the folder
	 * 'dataset/queueElement.getFileName()'.
	 * 
	 * @param queueElement
	 * @param transactionFolder
	 * @param logger
	 * @return
	 */
	@Deprecated
	public static List<File> getDatasetFilesFromFolder(QueueEntity queueElement, String transactionFolder,
			Logger logger) {
		logger.info(String.format("Get all files from the folder %s and append them to the current structure.",
				queueElement.getFilename()));
		final List<File> fileList = new ArrayList<>();

		final String filename = queueElement.getFilename().substring(0, queueElement.getFilename().lastIndexOf("."));

		// The folder with the same name as the given QueueElement
		final File folder = new File(
				transactionFolder + File.separator + TcConstants.FOLDER_DATASET + File.separator + filename);

		// Add all containing files if the folder exists
		if (folder.exists()) {
			for (final File file : folder.listFiles()) {
				fileList.add(file);
			}
		}
		return fileList;
	}

	public static List<File> getFilesFromFolder(String filepath, Logger logger) {
		logger.info(String.format("Get all files from the folder %s.", filepath));

		final List<File> fileList = new ArrayList<>();
		final File folder = new File(filepath);

		// Add all containing files if the folder exists
		if (folder.exists()) {
			for (final File file : folder.listFiles()) {
				fileList.add(file);
			}
		}
		return fileList;
	}

	public static List<FileTime> getLatestInvalidLog(String transactionFolder, Logger logger) {
		logger.info("Check if the last SBOMI log file was properly closed.");
		final List<FileTime> creationModifiedTimeArray = new ArrayList<>();

		final File sbomiLogDir = new File(transactionFolder + File.separator + "sbomi");
		final File[] sbomiLogArray = sbomiLogDir.listFiles();

		// Sort by creation date
		final Comparator<File> comparator = Comparator.comparing(file -> {
			try {
				return Files.readAttributes(Paths.get(file.toURI()), BasicFileAttributes.class).creationTime();
			} catch (final IOException e) {
				return null;
			}
		});

		Arrays.sort(sbomiLogArray, comparator);
		Collections.reverse(Arrays.asList(sbomiLogArray));

		// It should have at least four elements: the current and the old log and the
		// temp file of the current and the old log
		if (sbomiLogArray.length > 3) {

			// It's a temp file
			if (sbomiLogArray[2].getName().endsWith(".lck")) {
				try {
					creationModifiedTimeArray
							.add(Files.readAttributes(Paths.get(sbomiLogArray[3].toURI()), BasicFileAttributes.class)
									.creationTime());
					creationModifiedTimeArray
							.add(Files.readAttributes(Paths.get(sbomiLogArray[3].toURI()), BasicFileAttributes.class)
									.lastModifiedTime());

				} catch (final IOException e) {
					logger.severe(e.getMessage());

					e.printStackTrace();
				}

			}
		}

		return creationModifiedTimeArray;

	}

	/**
	 * Return the folder path depending on the current ImportStatus of the object.
	 * 
	 * @param currentStatusString
	 * @return
	 */
	public static String getPathToStatus(String currentStatusString, String transactionFolder, Logger logger) {
		logger.info(String.format("Get the path to the files with the status %s.", currentStatusString));

		ImportStatus currentStatus = ImportStatus.valueOf(currentStatusString.toUpperCase().replace("_", ""));

		switch (currentStatus) {
		case IN_SCOPE:
			return transactionFolder + File.separator + TcConstants.FOLDER_TODO;
		case IN_PROGRESS:
			return transactionFolder + File.separator + TcConstants.FOLDER_IN_PROGRESS;
		case IN_REVIEW:
			return transactionFolder + File.separator + TcConstants.FOLDER_IN_REVIEW;
		case DONE:
			return transactionFolder + File.separator + TcConstants.FOLDER_DONE;
		case ERROR:
			return transactionFolder + File.separator + TcConstants.FOLDER_ERROR;
		case CANCELED:
			return transactionFolder + File.separator + TcConstants.FOLDER_CANCELED;
		case DELETED:
			return transactionFolder + File.separator + TcConstants.FOLDER_DELETED;
		}
		return transactionFolder;
	}

	/**
	 * 
	 * 
	 * @param filepath The folder from were the files should get listed.
	 * @return A set of
	 * @throws IOException
	 */
	public static Set<String> listFilesFromPath(String filepath, Logger logger) throws IOException {
		logger.info(String.format("Read all files from the folder %s.", filepath));

		try (Stream<Path> stream = Files.list(Paths.get(filepath))) {
			return stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName).map(Path::toString)
					.collect(Collectors.toSet());
		}
	}

	/**
	 * Move the file depending on the ImportStatus.
	 * 
	 * @param element           The object corresponding to the file
	 * @param newStatus         The new status of the object
	 * @param transactionFolder Where the transaction folders are located
	 * @param logger            The current logger.
	 */
	public static void moveFile(QueueEntity element, ImportStatus newStatus, String transactionFolder, Logger logger)
			throws IOException {
		final String currentPath = getPathToStatus(element.getCurrentStatus(), transactionFolder, logger);
		final String newPath = getPathToStatus(newStatus.toString(), transactionFolder, logger);

		logger.info(String.format("Move the file %s from %s to %s.", element.getFilename(), currentPath, newPath));

		if (!Files.isWritable(Paths.get(currentPath)) || !Files.isWritable(Paths.get(newPath))) {
			logger.severe(String.format("Can't move the file. Check your write permissions for %s and %s.", currentPath,
					newPath));
			throw new IOException(String.format("Can't move the file. Check your write permissions for %s and %s.",
					currentPath, newPath));
		}

		final File newFile = new File(newPath + File.separator + element.getFilename());
		if (newFile.exists()) {
			newFile.delete();
		}

		final Path temp = Files.move(Paths.get(currentPath + File.separator + element.getFilename()),
				Paths.get(newPath + File.separator + element.getFilename()));

		if (temp != null) {
			logger.info("Moved the file successfully.");
		} else {
			logger.warning("Failed to move the file.");
		}

	}
}
