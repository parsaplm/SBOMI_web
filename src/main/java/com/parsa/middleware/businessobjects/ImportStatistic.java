package com.parsa.middleware.businessobjects;


import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class ImportStatistic {
	// Boolean values
	private boolean successfulClassification = false;
	private boolean successfulImport = false;
	private boolean structureExisted = false;
	private boolean wasRevised = false;
	private boolean canClassify = false;

	// Lists
	private final List<String> invalidClassifications = new ArrayList<>();
	private final List<String> notImportedDatasetFiles = new ArrayList<>();

	// Integer values
	private int amountOfObjects = 0;
	private int amountOfContainers = 0;

	// Long values
	private long timeAddChildrenToBomline = 0;
	private long timeCompareExistingStructures = 0;
	private long timeCompleteImport = 0;
	private long timeCreateStructure = 0;
	private long timeDatasetUpload = 0;
	private long timeEditProperties = 0;
	private long timeSearchStructure = 0;
	private long timeSolutionVariants = 0;

	// String values
	private String drawingNumber = "";
	private String syslogFile = "";
	private String tcServerUrl = "";

	// Start times
	private long startCreateStructure = 0;
	private long startEditProperties = 0;
	private long startImportTime = 0;
	private long startSearchStructure = 0;
	private long startSolutionVariants = 0;

	// End times
	private long endCreateStructure = 0;
	private long endEditProperties = 0;
	private long endImportTime = 0;
	private long endSearchStructure = 0;
	private long endSolutionVariants = 0;

	public void addInvalidClassification(String objectName) {
		invalidClassifications.add(objectName);
	}

	public void addNotImportedDatasetFiles(String filename) {
		notImportedDatasetFiles.add(filename);
	}

	public boolean canClassify() {
		return canClassify;
	}

	public int getAmountOfContainers() {
		return amountOfContainers;
	}

	public String getSyslogFile() {
		return syslogFile;
	}

	public void setSyslogFile(String syslogFile) {
		this.syslogFile = syslogFile;
	}

	public int getAmountOfObjects() {
		return amountOfObjects;
	}

	public String getLogStatistics() {
		String log = "Summary of the import:\n"
				+ "\t__________________________________________________________________________\n" //
				+ "\tThe import was successful: " + successfulImport + ".\n" //
				+ "\tSBOMI IP Address: " + Utility.getClientIpAddress() + "\n" //
				+ "\tTC Server IP Address : " + getTcServerUrl() + ".\n" //
				+ "\tSBOMI Version : " + TcConstants.SBOMI_VERSION + ".\n" //
				+ "\tThere are " + amountOfObjects + " objects in the structure.\n" //
				+ "\tThere are " + amountOfContainers + " container objects in the structure.\n" //
				+ "\tThe import took " + timeCompleteImport + " seconds.\n" //
				+ "\tThe search and creation of solution variants took " + timeSolutionVariants + " seconds.\n"
//				+ "\tThe comparisions of possible substructures summed up took " + timeCompareExistingStructures
//				+ " seconds.\n" //
				+ "\tThe add children to the bomline took " + timeAddChildrenToBomline + " seconds.\n"
				+ "\tThe search of the structure took " + timeSearchStructure + " seconds.\n"
				+ "\tThe creation of the structure took " + timeCreateStructure + " seconds.\n"
//				+ "\tThe editing of all properties took: " + timeEditProperties + " seconds.\n" //
				+ "\tThe structure already existed: " + structureExisted + ".\n" //
				+ "\tThe structure was revised: " + wasRevised + ".\n" //
				+ "\tThe structure should be classified: " + canClassify + ".\n" //
				+ "\tThe structure was successfully classified: " + successfulClassification + ".\n" //
				+ "\tThe Dataset upload time : " + timeDatasetUpload + " seconds.\n" //
				+ "\tThe syslog file used for this import is: " + syslogFile + ".\n" //
				+ "\tThe drawing number for this import is: \"" + drawingNumber + "\".\n";

		// Do we have invalid classifications?
		if (!invalidClassifications.isEmpty()) {
			String classificationProblems = "\tNot correctly classified are ";

			for (int i = 0; i < invalidClassifications.size() - 1; i++) {
				classificationProblems += invalidClassifications.get(i) + ", ";
			}

			log += classificationProblems + invalidClassifications.get(invalidClassifications.size() - 1) + "\n";
		}

		// Do we have not imported datasets?
		if (!notImportedDatasetFiles.isEmpty()) {
			String datasetFiles = "\tNot imported dataset files are ";

			for (int i = 0; i < notImportedDatasetFiles.size() - 1; i++) {
				datasetFiles += notImportedDatasetFiles.get(i) + ", ";
			}

			log += datasetFiles + notImportedDatasetFiles.get(notImportedDatasetFiles.size() - 1) + "\n";

		}

		log += "\t__________________________________________________________________________\n";

		return log;
	}

	public boolean isSuccessfulClassification() {
		return successfulClassification;
	}

	public boolean isSuccessfulImport() {
		return successfulImport;
	}

	public void setStartImportTime() {
		startImportTime = System.currentTimeMillis();
	}

	public void setStartSolutionVariants() {
		startSolutionVariants = System.currentTimeMillis();
	}

	public void setStartCreateStructure() {
		startCreateStructure = System.currentTimeMillis();
	}

	public void setStartEditProperties() {
		startEditProperties = System.currentTimeMillis();
	}

	public void setEndImportTime() {
		endImportTime = System.currentTimeMillis();
		timeCompleteImport = (endImportTime - startImportTime) / 1000;
	}

	public void setEndSolutionVariants() {
		endSolutionVariants = System.currentTimeMillis();
		timeSolutionVariants = (endSolutionVariants - startSolutionVariants) / 1000;
	}

	public void setEndCreateStructure() {
		endCreateStructure = System.currentTimeMillis();
		timeCreateStructure = (endCreateStructure - startCreateStructure) / 1000;
	}

	public void setEndEditProperties() {
		endEditProperties = System.currentTimeMillis();
		timeEditProperties = (endEditProperties - startEditProperties) / 1000;

	}

	public void setAmountOfContainers(int amountOfContainers) {
		this.amountOfContainers = amountOfContainers;
	}

	public void setAmountOfObjects(int amountOfObjects) {
		this.amountOfObjects = amountOfObjects;
	}

	public void setDrawingNumber(String drawingNo) {
		drawingNumber = drawingNo;
	}

	public void setCanClassify(boolean canClassify) {
		this.canClassify = canClassify;
	}

	public void setStructureExisted(boolean structureExisted) {
		this.structureExisted = structureExisted;
	}

	public void setSuccessfulClassification(boolean successfulClassification) {
		this.successfulClassification = successfulClassification;
	}

	public void setSuccessfulImport(boolean bool) {
		successfulImport = bool;
	}

	public long getTimeCompareExistingStructures() {
		return timeCompareExistingStructures;
	}

	public void setTimeCompareExistingStructures(long timeCompareExistingStructures) {
		this.timeCompareExistingStructures = timeCompareExistingStructures;
	}

	public void setWasRevised(boolean wasRevised) {
		this.wasRevised = wasRevised;
	}

	public boolean structureExisted() {
		return structureExisted;
	}

	public boolean wasRevised() {
		return wasRevised;
	}

	public String getTcServerUrl() {
		return Utility.extractIpAddress(tcServerUrl);
	}

	public void setTcServerUrl(String tcServerUrl) {
		this.tcServerUrl = tcServerUrl;
	}

	public void setStartSearchStructure() {
		startSearchStructure = System.currentTimeMillis();
	}

	public void setEndSearchStructure() {
		endSearchStructure = System.currentTimeMillis();
		timeSearchStructure = (endSearchStructure - startSearchStructure) / 1000;
	}

	public long getTimeSearchStructure() {
		return timeSearchStructure;
	}

	public long getTimeAddChildrenToBomline() {
		return timeAddChildrenToBomline;
	}

	public void setTimeAddChildrenToBomline(long timeAddChildrenToBomline) {
		this.timeAddChildrenToBomline += timeAddChildrenToBomline;
	}

	public long getTimeDatasetUpload() {
		return timeDatasetUpload;
	}

	public void setTimeDatasetUpload(long startTime, long endTime) {
		timeDatasetUpload = (endTime - startTime) / 1000;
	}
}
