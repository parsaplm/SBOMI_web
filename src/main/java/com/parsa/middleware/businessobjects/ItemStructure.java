package com.parsa.middleware.businessobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class ItemStructure {
	// List
	ArrayList<ItemStructure> children;
	ArrayList<VariantRule> variantRules;
	ArrayList<ClassificationAttribute> classificationAttributes;

	// String
	String coordinates;
	String findNo;
	String genericObjectID;
	String objectDescription;
	String objectGroupID;
	String objectID;
	String objectName;
	String objectType;
	String positionDesignator;
	String pseudoSerialNumber;
	String quantity;
	String revisionID;
	String rotation;
	String solutionVariantCategory;
	String storageClassID;
	String workflow;

	@JsonProperty("classificationAttributes")
	public ArrayList<ClassificationAttribute> getClassificationAttributes() {
		return classificationAttributes;
	}

	@JsonProperty("coordinates")
	public String getCoordinates() {
		return coordinates;
	}

	@JsonProperty("findNo")
	public String getFindNo() {
		return findNo;
	}

	@JsonProperty("genericObjectID")
	public String getGenericObjectID() {
		return genericObjectID;
	}

	@JsonProperty("children")
	public ArrayList<ItemStructure> getItemStructure() {
		return children;
	}

	@JsonProperty("objectDescription")
	public String getObjectDescription() {
		return objectDescription;
	}

	@JsonProperty("objectGroupID")
	public String getObjectGroupID() {
		return objectGroupID;
	}

	@JsonProperty("objectID")
	public String getObjectID() {
		return objectID;
	}

	@JsonProperty("objectName")
	public String getObjectName() {
		return objectName;
	}

	@JsonProperty("objectType")
	public String getObjectType() {
		return objectType;
	}

	@JsonProperty("positionDesignator")
	public String getPositionDesignator() {
		return positionDesignator;
	}

	@JsonProperty("pseudoSerialNumber")
	public String getPseudoSerialNumber() {
		return pseudoSerialNumber;
	}

	@JsonProperty("quantity")
	public String getQuantity() {
		return quantity;
	}

	@JsonProperty("revisionID")
	public String getRevisionID() {
		return revisionID;
	}

	@JsonProperty("rotation")
	public String getRotation() {
		return rotation;
	}

	@JsonProperty("solutionVariantCategory")
	public String getSolutionVariantCategory() {
		return solutionVariantCategory;
	}

	@JsonProperty("storageClassID")
	public String getStorageClassID() {
		return storageClassID;
	}

	@JsonProperty("variantRules")
	public ArrayList<VariantRule> getVariantRules() {
		return variantRules;
	}

	@JsonProperty("workflow")
	public String getWorkflow() {
		return workflow;
	}

	public void setClassificationAttributes(ArrayList<ClassificationAttribute> classificationAttributes) {
		this.classificationAttributes = classificationAttributes;
	}

	public void setCoordinates(String coordinates) {
		this.coordinates = coordinates;
	}

	public void setFindNo(String findNo) {
		this.findNo = findNo;
	}

	public void setGenericObjectID(String genericObjectID) {
		this.genericObjectID = genericObjectID;
	}

	public void setItemStructure(ArrayList<ItemStructure> children) {
		this.children = children;
	}

	public void setObjectDescription(String objectDescription) {
		this.objectDescription = objectDescription;
	}

	public void setObjectGroupID(String objectGroupID) {
		this.objectGroupID = objectGroupID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public void setPositionDesignator(String positionDesignator) {
		this.positionDesignator = positionDesignator;
	}

	public void setPseudoSerialNumber(String pseudoSerialNumber) {
		this.pseudoSerialNumber = pseudoSerialNumber;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	public void setRevisionID(String revisionID) {
		this.revisionID = revisionID;
	}

	public void setRotation(String rotation) {
		this.rotation = rotation;
	}

	public void setSolutionVariantCategory(String solutionVariantCategory) {
		this.solutionVariantCategory = solutionVariantCategory;
	}

	public void setStorageClassID(String storageClassID) {
		this.storageClassID = storageClassID;
	}

	public void setVariantRules(ArrayList<VariantRule> variantRules) {
		this.variantRules = variantRules;
	}

	public void setWorkflow(String workflow) {
		this.workflow = workflow;
	}

//    List<VariantRule> uniqueVariantRules;
//    
//    private static List<ItemStructure> removeDuplicates(List<ItemStructure> itemList) {
//        Set<ItemStructure> uniqueItems = new HashSet<>(itemList);
//        return new ArrayList<>(uniqueItems);
//    }

}
