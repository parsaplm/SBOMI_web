package com.parsa.middleware.businessobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RootStructure {
	// String
	String dateDesigned;
	String dateModified;
	String designer;
	String designNo;
	String objectDescription;
	String objectID;
	String objectName;
	String objectType;
	String offerNumber;
	String revisionID;

	// List
	List<ItemStructure> itemStructure;
	List<ClassificationAttribute> uniqueClassificationAttributes;
	List<VariantRule> uniqueVariantRule;

	@JsonProperty("dateDesigned")
	public String getDateDesigned() {
		return dateDesigned;
	}

	@JsonProperty("dateModified")
	public String getDateModified() {
		return dateModified;
	}

	@JsonProperty("designer")
	public String getDesigner() {
		return designer;
	}

	@JsonProperty("designNo")
	public String getDesignNo() {
		return designNo;
	}

	@JsonProperty("children")
	public List<ItemStructure> getItemStructure() {
		return itemStructure;
	}

	@JsonProperty("objectDescription")
	public String getObjectDescription() {
		return objectDescription;
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

	@JsonProperty("offerNumber")
	public String getOfferNumber() {
		return offerNumber;
	}

	@JsonProperty("revisionID")
	public String getRevisionID() {
		return revisionID;
	}

	public List<ClassificationAttribute> getUniqueClassificationAttributes() {
		if (uniqueClassificationAttributes != null) {
			return uniqueClassificationAttributes;
		} else {
			return finUniqueClassificationAttributes(itemStructure);
		}
	}

	public List<VariantRule> getUniqueVariantRules() {
		if (uniqueVariantRule != null) {
			return uniqueVariantRule;
		} else {
			return findUniqueVariantRules(itemStructure);
		}
	}

	public void setDateDesigned(String dateDesigned) {
		this.dateDesigned = dateDesigned;
	}

	public void setDateModified(String dateModified) {
		this.dateModified = dateModified;
	}

	public void setDesigner(String designer) {
		this.designer = designer;
	}

	public void setDesignNo(String designNo) {
		this.designNo = designNo;
	}

	public void setItemStructure(List<ItemStructure> itemStructure) {
		this.itemStructure = itemStructure;
	}

	public void setObjectDescription(String objectDescription) {
		this.objectDescription = objectDescription;
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

	public void setOfferNumber(String offerNumber) {
		this.offerNumber = offerNumber;
	}

	public void setRevisionID(String revisionID) {
		this.revisionID = revisionID;
	}

	private List<VariantRule> findUniqueVariantRules(List<ItemStructure> parentList) {

		final Set<VariantRule> uniqueChildren = new HashSet<>();

		for (int i = 0; i < parentList.size(); i++) {

			for (final ItemStructure item : parentList.get(i).getItemStructure()) {
				final List<VariantRule> children = item.getVariantRules();
				uniqueChildren.addAll(children);

			}
		}

		return new ArrayList<>(uniqueChildren);
	}

	private List<ClassificationAttribute> finUniqueClassificationAttributes(List<ItemStructure> parentList) {
		final Set<ClassificationAttribute> commonChildrenSet = new HashSet<>();

		for (int i = 0; i < parentList.size(); i++) {
			final List<ClassificationAttribute> children = parentList.get(i).getClassificationAttributes();
			commonChildrenSet.addAll(children);
		}

		return new ArrayList<>(commonChildrenSet);
	}

}
