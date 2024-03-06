package com.parsa.middleware.businessobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VariantRule {
	String familyID;
	String featureID;

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
		final VariantRule other = (VariantRule) obj;
		if (familyID == null) {
			if (other.familyID != null) {
				return false;
			}
		} else if (!familyID.equals(other.familyID)) {
			return false;
		}
		if (featureID == null) {
			if (other.featureID != null) {
				return false;
			}
		} else if (!featureID.equals(other.featureID)) {
			return false;
		}
		return true;
	}

	@JsonProperty("familyID")
	public String getFamilyID() {
		return familyID;
	}

	@JsonProperty("featureID")
	public String getFeatureID() {
		return featureID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((familyID == null) ? 0 : familyID.hashCode());
		result = prime * result + ((featureID == null) ? 0 : featureID.hashCode());
		return result;
	}

	public void setFamilyID(String familyID) {
		this.familyID = familyID;
	}

	public void setFeatureID(String featureID) {
		this.featureID = featureID;
	}

}
