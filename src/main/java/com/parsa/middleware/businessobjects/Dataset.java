package com.parsa.middleware.businessobjects;

import java.io.File;

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class Dataset {
	private final File datasetFile;
	private String datasetTool;
	private String relationType;
	private String datasetType;
	private String extension;
	private String name;
	private String referencedName;

	public Dataset(File file) {
		datasetFile = file;
		extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
		name = file.getName();// .substring(0, file.getName().lastIndexOf('.'));
	}

	public String getReferencedName() {
		return referencedName;
	}

	public void setReferencedName(String referencedName) {
		this.referencedName = referencedName;
	}

	public File getDatasetFile() {
		return datasetFile;
	}

	public String getDatasetTool() {
		return datasetTool;
	}

	public String getDatasetType() {
		return datasetType;
	}

	public String getExtension() {
		return extension;
	}

	public String getName() {
		return name;
	}

	public String getRelationType() {
		return relationType;
	}

	public void setDatasetTool(String datasetTool) {
		this.datasetTool = datasetTool;
	}

	public void setDatasetType(String datasetType) {
		this.datasetType = datasetType;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}
}
