package com.parsa.middleware.businessobjects;

import com.parsa.middleware.constants.TcConstants;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An objects that contains all information about a Teamcenter family object.
 * It's used to minimize the calls to the Teamcenter server.
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class Family {
	private final Logger logger;
	private final WorkspaceObject family;

	private final Map<String, ModelObject> features;

	private String familyID;
	private String familyName;
	private String familyGroupName;
	private String classID;

	private boolean isOptional = false;
	private boolean hasFreeFormValues = false;

	/**
	 * 
	 * @param logger
	 */
	public Family(ModelObject familyObject, Logger logger) {
		this.logger = logger;
		family = (WorkspaceObject) familyObject;
		features = new HashMap<>();
		setProperties();
	}

	/**
	 * 
	 * @return The Id of this family object
	 */
	public String getFamilyID() {
		return familyID;
	}

	/**
	 * 
	 * @return The name of this family object
	 */
	public String getFamilyName() {
		return familyName;
	}

	/**
	 * 
	 * @return The name of the group of this family object
	 */
	public String getFamilyGroupName() {
		return familyGroupName;
	}

	/**
	 * 
	 * @return The Teamcenter family object
	 */
	public ModelObject getFamily() {
		return family;
	}

	/**
	 * 
	 * @return True if this family is an optional value. False otherwise
	 */
	public boolean isOptional() {
		return isOptional;
	}

	/**
	 * Get the feature map of this object.
	 * 
	 * @return A HashMap of Teamcenter feature objects and their ID.
	 */
	public Map<String, ModelObject> getFeatureMap() {
		return features;
	}

	/**
	 * Add the given feature to the feature map. Every entry of the feature map
	 * consist of the feature ID as String and its corresponding feature object.
	 * 
	 * @param feature the feature that should be added to the feature map
	 */
	public void addFeature(ModelObject feature) {
		try {
//			dataManagementService.getProperties(new ModelObject[] { feature },
//					new String[] { TcConstants.TEAMCENTER_OBJECT_STRING });
			features.put(feature.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_STRING).split("/")[0],
					feature);
		} catch (final NotLoadedException e) {
			logger.severe("Couldn't add a feature to the feature list.");

			e.printStackTrace();
		}
	}

	/**
	 * Set important properties into the local fields
	 */
	private void setProperties() {
		try {
//			dataManagementService.getProperties(new ModelObject[] { family },
//					new String[] { TcConstants.TEAMCENTER_OBJECT_STRING, TcConstants.TEAMCENTER_OPTIONAL,
//							TcConstants.TEAMCENTER_CLASS_ID, TcConstants.TEAMCENTER_HAS_FREE_FORM_VALUES });

			familyID = family.get_object_string().split("/")[0];
			familyName = ""; // family.get_object_string().split("/")[1];
			isOptional = family.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OPTIONAL).equals("True");
			classID = family.getPropertyDisplayableValue(TcConstants.TEAMCENTER_CLASS_ID);
			hasFreeFormValues = family.getPropertyDisplayableValue(TcConstants.TEAMCENTER_HAS_FREE_FORM_VALUES)
					.equals("True");
		} catch (final NotLoadedException e) {
			logger.severe(String.format("Couldn't get all properties for the current family object."));

			e.printStackTrace();
		}
	}

	public boolean hasFreeFormValues() {
		return hasFreeFormValues;
	}

	public void setGroupName(String groupName) {
		familyGroupName = groupName;
	}
}
