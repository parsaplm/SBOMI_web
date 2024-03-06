package com.parsa.middleware.service;

import com.parsa.middleware.businessobjects.ModularBuilding;
import com.parsa.middleware.businessobjects.SolutionVariant;
import com.parsa.middleware.businessobjects.StructureObject;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.ImportData;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.session.AppXSession;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.internal.loose.classification.ClassificationRestBindingStub;
import com.teamcenter.services.internal.loose.classification._2009_10.Classification;
import com.teamcenter.services.internal.loose.classification._2009_10.Classification.GetClassificationPropertiesResponse;
import com.teamcenter.services.loose.classification.ClassificationService;
import com.teamcenter.services.loose.classification._2007_01.Classification.*;
import com.teamcenter.services.strong.core._2013_05.LOV.LOVValueRow;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains all methods that are required to add or change a classification of a
 * Teamcenter object.
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
@Component
public class ClassificationManagement extends Utility {
	private final Logger logger = Logger.getLogger(ClassificationManagement.class.getName());
	//private ImportData importData;
	private final AppXSession session;

	/**
	 * Initiates the class with a logger.
	 * 
	 * @param currentSession
	 */
	public ClassificationManagement( AppXSession currentSession) {
		//this.importData = importData;
		session = currentSession;
	}

	/**
	 * Set the classification of the currentBomLine if either the BOMLine isn't
	 * classified yet or the classification is different than the classification in
	 * the JSONObject.
	 * 
	 * @param searchManagement
	 * @param structureObject
	 */
	public boolean classify(StructureObject structureObject, SearchManagement searchManagement) {
		logger.info(String.format(
				"Check if the given BOMLine %s is already classified and if the classification should be overriden.",
				structureObject.getDisplayString()));

		boolean isAlreadyClassified,successfullyClassified = false;
		try {
			final BOMLine currentBomLine = structureObject.getBomLine();

			isAlreadyClassified = currentBomLine.get_bl_is_classified();

			// Only classify if the classification isn't the same as in the jsonObject
			if (!isAlreadyClassified
					|| (isAlreadyClassified && !hasSameClassification(structureObject, currentBomLine))) {

				// Get the storage class ID from the configurator context
				final String storageClassID = getStorageClassID(currentBomLine, structureObject, searchManagement);

				successfullyClassified = classifyObject(currentBomLine, storageClassID, isAlreadyClassified, structureObject);

			}
			//successfullyClassified = true;
		} catch (final NullPointerException e) {
			logger.severe(String.format("Couldn't get a storage class ID from the BOMLine %s.",
					structureObject.getDisplayString()));

			e.printStackTrace();
			//importData.setSuccessfulClassification(false, structureObject.getDisplayString());
			successfullyClassified = false;
		} catch (final NotLoadedException e) {
			logger.log(Level.SEVERE, "The given ModelObject isn't loaded.\n\n" + e.getMessage());

			e.printStackTrace();
			//importData.setSuccessfulClassification(false, structureObject.getDisplayString());
			successfullyClassified = false;
		}
		return successfullyClassified;

	}

	/**
	 * Classify the given BOMLine.
	 * 
	 * @param currentBomLine
	 * @param classID
	 * @param isClassified
	 * @param structureObject
	 */
	private boolean classifyObject(BOMLine currentBomLine, String classID, boolean isClassified,
			StructureObject structureObject) {
		logger.info("Classify the object " + structureObject.getDisplayString()
				+ " with the attributes from the JSON file.");

		final ClassificationService classificationService = ClassificationService.getService(session.getConnection());
		boolean isClassifiedSuccessfully = true;

		try {
			Map<String, String> variantRuleList = new HashMap<>();
			Map<String, String> classificationList = new HashMap<>();

			// Don't classify when the type is TcConstants.TEAMCENTER_PART
			if (structureObject.getItemType().equals(TcConstants.TEAMCENTER_PART)) {
				logger.log(Level.INFO, "Can't classify objects of the type \"Part\".");
				return isClassifiedSuccessfully;
			}

			// Solution variants use their variant rules for classification
			if (structureObject instanceof SolutionVariant) {
				variantRuleList = ((SolutionVariant) structureObject).getVariantRules();
			}

			// Modular building don't get classified
			if (!(structureObject instanceof ModularBuilding)) {
				classificationList = structureObject.getClassificationMap();
			}

			// Don't try to classify if the owning group is wrong
//			if (!currentBomLine.get_bl_rev_owning_group().equals(person.get_owning_group().getPropertyDisplayableValue("name"))) {
//				logger.log(Level.WARNING, "The object " + getDisplayString(structureObject.getBomLine())
//						+ " couldn't be classified. The owning group differs from the group of the current user.");
//				return;
//			}

			if (classID.isEmpty()) {
				logger.warning(String.format(
						"Couldn't classify de object %s. No storage class was given. Please check the JSON file or the ConfiguratorContext.",
						structureObject.getDisplayString()));

				//importData.setSuccessfulClassification(false, structureObject.getDisplayString());
				isClassifiedSuccessfully = false;
				return isClassifiedSuccessfully;
			}

			final ClassificationProperty[] classificationPropertyVariantRules = new ClassificationProperty[variantRuleList
					.size()];
			final ClassificationProperty[] classificationPropertyClassificationAttributes = new ClassificationProperty[classificationList
					.size()];

			getVariantRuleClassification(variantRuleList, classificationPropertyVariantRules, structureObject);
			getClassificationAttributes(classificationList, classificationPropertyClassificationAttributes);

			final ClassificationProperty[] classificationProperty = concatClassificationPropertyArrays(
					classificationPropertyVariantRules, classificationPropertyClassificationAttributes);

			// Don't classify, when no classification properties are given
			if (classificationProperty.length == 0) {
				logger.warning("Couldn't classify the object " + structureObject.getDisplayString()
						+ ". There were no valid classification attributes given.");

//				importData.setSuccessfulClassification(false, structureObject.getDisplayString());
				isClassifiedSuccessfully = false;
				return isClassifiedSuccessfully;
			}

			final ClassificationObject classificationObject = new ClassificationObject();
			classificationObject.classId = classID;
			classificationObject.properties = classificationProperty;
			classificationObject.unitBase = "METRIC";
			// wsoId is null for standalone ICO.
			classificationObject.wsoId = currentBomLine.get_bl_revision();

			final ModelObject ico = getICOFromBOMLine(currentBomLine);

			ServiceData serviceData = null;
			if (ico == null) {
				// Create the ICO object
				CreateClassificationObjectsResponse createICOResponse = null;
				createICOResponse = classificationService
						.createClassificationObjects(new ClassificationObject[] { classificationObject });
				serviceData = createICOResponse.data;
			} else {

				classificationObject.clsObjTag = ico;

				// Update the classification attributes
				final UpdateClassificationObjectsResponse updateICOResponse = classificationService
						.updateClassificationObjects(new ClassificationObject[] { classificationObject });

				serviceData = updateICOResponse.data;
			}

			if (serviceDataError(serviceData, logger)) {
				// The classification wasn't successful

				//importData.setSuccessfulClassification(false, structureObject.getDisplayString());
				isClassifiedSuccessfully = false;
			} else {
				// The classification was successful but could have used invalid IDs

				final Map<String, String> classificationProperties = new HashMap<>();
				getClassificationProperties(currentBomLine, classificationProperties);

				for (final String name : classificationProperties.keySet()) {
					if (classificationProperties.get(name).contains("not found")) {
						logger.severe(String.format(
								"The object %s wasn't correctly classified. The value for the classification attribute %s was invalid.",
								structureObject.getDisplayString(), name));

						//importData.setSuccessfulClassification(false, structureObject.getDisplayString());
						isClassifiedSuccessfully = false;
						break;
					}
				}

				logger.info(String.format("The object was successfully classified."));
			}

		} catch (final ServiceException e) {
			logger.log(Level.SEVERE, "Couldn't classify the object.\n\n" + e.getMessage(), e);

			e.printStackTrace();
			//importData.setSuccessfulClassification(false, structureObject.getDisplayString());
			isClassifiedSuccessfully = false;
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
			//importData.setSuccessfulClassification(false, structureObject.getDisplayString());
			isClassifiedSuccessfully = false;
		} catch (final Exception e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		//	importData.setSuccessfulClassification(false, structureObject.getDisplayString());
			isClassifiedSuccessfully = false;
		}
		return isClassifiedSuccessfully;
	}

	/**
	 * Get the storage class ID from either the configurator context that's
	 * referenced by the given BOMLine or as attribute on the StructureObject.
	 * 
	 * @param currentBomLine
	 * @param structureObject
	 * @param searchManagement
	 * @return
	 * @throws NotLoadedException
	 */
	private String getStorageClassID(BOMLine currentBomLine, StructureObject structureObject,
			SearchManagement searchManagement) throws NotLoadedException {
		logger.info(String.format("Get the storage class ID from the object %s.", structureObject.getDisplayString()));

		String storageClassID = "";

		try {
			ModelObject configuratorContext;
			if (structureObject instanceof SolutionVariant) {

				// The generic object should have the config perspective
				configuratorContext = getConfigContext(((SolutionVariant) structureObject).getGenericBomLine());
			} else {

				// The current object could have a config perspective
				configuratorContext = getConfigContext(currentBomLine);
			}

			if (configuratorContext != null) {

				// We could get the storage class ID from the configurator context
//				dataManagementService.getProperties(new ModelObject[] { configuratorContext },
//						new String[] { TcConstants.TEAMCENTER_STORAGE_CLASS });

				final ModelObject storageClass = ((Item) configuratorContext)
						.getPropertyObject(TcConstants.TEAMCENTER_STORAGE_CLASS).getModelObjectValue();

				// Get all possible storage class entries for configurator context objects
				final LOVValueRow[] lovValueRow = searchManagement
						.getStorageClassEntry(configuratorContext.getTypeObject().getName());

				// Compare the UIDs of all possible storage class entries with the UID of the
				// selected storage class
				for (final LOVValueRow row : lovValueRow) {
					if (row.uid.equals(storageClass.getUid())) {

						// We get the storage class ID if we found the correct object
						storageClassID = row.propDisplayValues.get(TcConstants.TEAMCENTER_CID)[0];
					}
				}
			} else {
				storageClassID = structureObject.getClassID();
			}
		} catch (final NullPointerException e) {
			logger.severe(String.format(
					"Couldn't classify the object %s. It has neither a storageClassID given in the JSON file, nor a corresponding configurator context.",
					structureObject.getDisplayString()));

			e.printStackTrace();
		}

		return storageClassID;
	}

	/**
	 * Compare the current classification of currentBomLine with the classification
	 * in jsonObject.
	 * 
	 * @param structureObject
	 * @param currentBomLine
	 * @return True, if the classification is the same. False otherwise
	 */
	private boolean hasSameClassification(StructureObject structureObject, BOMLine currentBomLine) {
		logger.info(String.format(
				"Compare the classification attributes from the JSON file with the classification of the BOMLine %s.",
				structureObject.getDisplayString()));

		final Map<String, String> classificationProperties = new HashMap<>();
		getClassificationProperties(currentBomLine, classificationProperties);

		// Compare variant rules and classification attributes
		if (structureObject instanceof SolutionVariant) {

			if (classificationProperties.equals(((SolutionVariant) structureObject).getVariantRules())
					&& structureObject.getClassificationMap().isEmpty()) {

				// If the solution variant is already classified only with the variant rules
				logger.info(String.format(
						"The BOMLine is correctly classified via variant rules just as given in the JSON file.",
						structureObject.getDisplayString()));
				return true;
			} else if (!structureObject.getClassificationMap().isEmpty()) {

				// If the solution variant should be classified with variant rules and
				// additional classification attributes
				final Map<String, String> map = new HashMap<>(structureObject.getClassificationMap());
				map.putAll(((SolutionVariant) structureObject).getVariantRules());

				return map.equals(classificationProperties);
			}
		}

		// Compare only the classification attributes
		return structureObject.getClassificationMap().equals(classificationProperties);
	}

	/**
	 * Get all classification properties from the given BOMLine and add them to the
	 * given Map.
	 * 
	 * @param currentBomLine
	 * @param classificationProperties
	 */
	private void getClassificationProperties(BOMLine currentBomLine,
			final Map<String, String> classificationProperties) {
		logger.info(String.format("Get the current classification properties of the BOMLine %s.",
				getDisplayString(currentBomLine)));

		final Classification classificationService = ClassificationRestBindingStub.getService(session.getConnection());
		try {
			final GetClassificationPropertiesResponse classificationPropertiesResponse = classificationService
					.getClassificationProperties(new ModelObject[] { currentBomLine.get_bl_item() });

			// check for size of both sets
			final Map<String, String[]> propNameMap = classificationPropertiesResponse.propnames;
			final Map<String, String[]> propValueMap = classificationPropertiesResponse.propvalues;

			if (!propNameMap.isEmpty() && !propValueMap.isEmpty()) {

				final String[] propArr = propNameMap.get(propNameMap.keySet().iterator().next());
				final String[] valArr = propValueMap.get(propValueMap.keySet().iterator().next());

				// Both arrays should have the same size
				if (propArr.length == valArr.length) {
					for (int i = 0; i < propArr.length; i++) {
						classificationProperties.put(propArr[i], valArr[i]);
					}
				}
			}
		} catch (final ServiceException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
	}

	/**
	 * Fill the given ClassificationProperty array with the values from the given
	 * Map.
	 * 
	 * @param classificationMap
	 * @param classificationPropertyClassificationAttributes
	 */
	private void getClassificationAttributes(Map<String, String> classificationMap,
			final ClassificationProperty[] classificationPropertyClassificationAttributes) {
		logger.info("Get all classification attributes from the given JSON attribute classificationAttributes.");

		int index;
		// Get all classification attributes from classificationAttributes
		index = 0;
		for (final String classificationID : classificationMap.keySet()) {
			final ClassificationPropertyValue classificationPropertyValue = new ClassificationPropertyValue();
			classificationPropertyValue.dbValue = classificationMap.get(classificationID);

			// Add the classification attribute properties to the property list
			classificationPropertyClassificationAttributes[index] = new ClassificationProperty();
			classificationPropertyClassificationAttributes[index].attributeId = Integer.parseInt(classificationID);
			classificationPropertyClassificationAttributes[index].values = new ClassificationPropertyValue[] {
					classificationPropertyValue };
			index++;
		}
	}

	/**
	 * Get all classification attributes from the variant rules of the given
	 * StructureObject.
	 * 
	 * @param variantRuleList
	 * @param classificationPropertyVariantRules
	 * @param structureObject
	 */
	private void getVariantRuleClassification(Map<String, String> variantRuleList,
			final ClassificationProperty[] classificationPropertyVariantRules, StructureObject structureObject) {
		logger.info("Get all classification attributes from the variant rules.");

		// Get all classification attributes from the variant rules
		int index = 0;
		for (final String classificationID : variantRuleList.keySet()) {
			if (((SolutionVariant) structureObject).getFamily(classificationID).hasFreeFormValues()) {
				continue;
			}

			final ClassificationPropertyValue classificationPropertyValue = new ClassificationPropertyValue();
			classificationPropertyValue.dbValue = variantRuleList.get(classificationID);

			// Add the classification attribute properties to the property list
			classificationPropertyVariantRules[index] = new ClassificationProperty();
			classificationPropertyVariantRules[index].attributeId = Integer.parseInt(classificationID);
			classificationPropertyVariantRules[index].values = new ClassificationPropertyValue[] {
					classificationPropertyValue };
			index++;
		}
	}

	/**
	 * Get the ICO object from the given BOMLine.
	 * 
	 * @param bomline
	 * @return
	 */
	private ModelObject getICOFromBOMLine(BOMLine bomline) {
		logger.info(String.format("Get the ICO from the BOMLine %s.", getDisplayString(bomline)));

		final ClassificationService classificationService = ClassificationService.getService(session.getConnection());

		// Get the ICO object from the BOMLine
		ModelObject ico = null;
		try {
			final FindClassificationObjectsResponse findICOResponse = classificationService
					.findClassificationObjects(new ModelObject[] { bomline.get_bl_revision() });

			// Check if the response and the map containing ICOs are not null
			if (findICOResponse != null && findICOResponse.icos != null) {
				// Get the ICO array for the given BOM revision
				ModelObject[] icoArray = findICOResponse.icos.get(bomline.get_bl_revision());

				// Check if the ICO array is not null and not empty
				if (icoArray != null && icoArray.length > 0) {
					// Get the first ICO from the array
					ico = icoArray[0];
				} else {
					logger.warning("ICO array is null or empty for the given BOMLine revision.");
				}
			} else {
				logger.warning("Response or ICO map is null.");
			}
		} catch (final ServiceException e) {
			logger.severe(String.format("Couldn't get the ICO from the BOMLine. \n%s", e.getMessage()));
			e.printStackTrace();
		} catch (final NullPointerException e) {
			logger.severe("The BOMLine or its revision doesn't have an ICO object.");
			e.printStackTrace();
		} catch (final NotLoadedException e) {
			e.printStackTrace();
		}

		return ico;
	}


	/**
	 * Concatenate the classification properties from the variant rules and the JSON
	 * attribute classificationAttributes.
	 * 
	 * @param classificationPropertyVariantRules
	 * @param classificationPropertyClassificationAttributes
	 * @return
	 */
	private ClassificationProperty[] concatClassificationPropertyArrays(
			ClassificationProperty[] classificationPropertyVariantRules,
			ClassificationProperty[] classificationPropertyClassificationAttributes) {
		logger.info(
				"Concatenate the classification properties from the variant rules and the JSON attribute classificationAttributes.");

		final ArrayList<ClassificationProperty> tempList = new ArrayList<>();
		for (final ClassificationProperty property : classificationPropertyVariantRules) {
			if (property != null) {
				tempList.add(property);
			}
		}

		for (final ClassificationProperty property : classificationPropertyClassificationAttributes) {
			if (property != null) {
				tempList.add(property);
			}
		}

		return tempList.toArray(new ClassificationProperty[tempList.size()]);

	}
}
