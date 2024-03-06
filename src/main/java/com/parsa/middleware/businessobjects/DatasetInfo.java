package com.parsa.middleware.businessobjects;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class DatasetInfo {
	private final Properties properties;
	private final File datasetFile;

	private final Map<String, String[]> extensionInfo;

	/**
	 * Load the file from its default file path. The default file path is the same
	 * folder this software is in.
	 * 
	 */
	public DatasetInfo() {
		properties = new Properties();
		extensionInfo = new HashMap<>();

		// Get the file if it exists
//		final File thisFile = new File(
//				BOMImporterGUI.class.getProtectionDomain().getCodeSource().getLocation().getPath());
//		datasetFile = new File(thisFile.getParentFile() + File.separator + "datasetInfo.ini");
		// Get the path of the config file
		final File thisFile = new File(System.getProperty("user.home"), "Desktop");

		// Get the directory of the SBOMI jar file
		final String directory = ClassLoader.getSystemClassLoader().getResource(".").getPath().replaceAll("%20", " ");
		File jarDirectory = new File(directory);
		jarDirectory = jarDirectory != null && jarDirectory.isDirectory() ? jarDirectory : thisFile;

		datasetFile = new File(jarDirectory + File.separator + "datasetInfo.ini");
		if (datasetFile.exists()) {
			loadDatasetInfo();
		} else {

			// Create the file with default values that aren't used
			try {
				datasetFile.createNewFile();
				setDefaultValues();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Load the file from the given file path
	 * 
	 * @param filepath
	 */
	public DatasetInfo(String filepath) {
		datasetFile = new File(filepath + File.separator + "datasetInfo.ini");
		properties = new Properties();
		extensionInfo = new HashMap<>();

		if (datasetFile.exists()) {
			loadDatasetInfo();
		} else {

			// Create the file with default values that aren't used
			try {
				datasetFile.createNewFile();
				setDefaultValues();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the informations for dataset types with the given file extension. Right
	 * now the array consists of the entries: 0 = dataset type 1 = referenced name 2
	 * = relation type
	 * 
	 * @param fileExtension
	 * @return
	 */
	public String[] getDatasetInfoFromExtension(String fileExtension) {
		return extensionInfo.get(fileExtension);
	}

	public String getDatasetFileLocation() {
		return datasetFile.getAbsolutePath();
	}

	/**
	 * Load the dataset extension informations from the properties file.
	 * 
	 */
	private void loadDatasetInfo() {
		try {
			properties.load(new FileInputStream(datasetFile));
			final String[] extensionArray = properties.getProperty("extensions").split(",");

			// Check if we have at least one dataset extension given
			if (extensionArray.length == 0) {
				return;
			}

			// Add all dataset info to a map
			for (final String extension : extensionArray) {
				System.out.println(extension);
				final String[] extensionInfoArray = properties.getProperty(extension.trim()).split(",");

				// Each entry must have 3 values
				if (extensionInfoArray.length == 3) {
					extensionInfo.put(extension.trim(), extensionInfoArray);
				}

			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Set the default values to the properties file. These are just an example and
	 * can be deleted if necessary.
	 * 
	 */
	private void setDefaultValues() {
		// Default values
		properties.setProperty("extensions", "exampleExtension");
		properties.setProperty("exampleExtension", "datasetType, referencedName, relationType");

		// Save the default values
		OutputStream output;
		try {
			output = new FileOutputStream(datasetFile.getAbsolutePath());
			properties.store(output, null);

		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
