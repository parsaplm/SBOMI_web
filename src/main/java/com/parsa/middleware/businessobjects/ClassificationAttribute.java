package com.parsa.middleware.businessobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClassificationAttribute {
	String classificationID;
	String classificationValue;

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ClassificationAttribute other = (ClassificationAttribute) obj;
		if (classificationID == null) {
			if (other.classificationID != null) {
				return false;
			}
		} else if (!classificationID.equals(other.classificationID)) {
			return false;
		}
		if (classificationValue == null) {
			if (other.classificationValue != null) {
				return false;
			}
		} else if (!classificationValue.equals(other.classificationValue)) {
			return false;
		}
		return true;
	}

	@JsonProperty("classificationID")
	public String getClassificationID() {
		return classificationID;
	}

	@JsonProperty("classificationValue")
	public String getClassificationValue() {
		return classificationValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classificationID == null) ? 0 : classificationID.hashCode());
		result = prime * result + ((classificationValue == null) ? 0 : classificationValue.hashCode());
		return result;
	}

	public void setClassificationID(String classificationID) {
		this.classificationID = classificationID;
	}

	public void setClassificationValue(String classificationValue) {
		this.classificationValue = classificationValue;
	}

}
