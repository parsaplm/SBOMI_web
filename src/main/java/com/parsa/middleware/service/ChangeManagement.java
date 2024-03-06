package com.parsa.middleware.service;

import com.parsa.middleware.businessobjects.ModularBuilding;
import com.parsa.middleware.businessobjects.SolutionVariant;
import com.parsa.middleware.businessobjects.StructureObject;
import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.ImportData;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.session.AppXSession;
import com.parsa.middleware.util.JsonUtil;
import com.teamcenter.services.loose.workflow.WorkflowService;
import com.teamcenter.services.loose.workflow._2008_06.Workflow.ContextData;
import com.teamcenter.services.loose.workflow._2008_06.Workflow.InstanceInfo;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.LOVService;
import com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct;
import com.teamcenter.services.strong.core._2013_05.LOV.LOVInput;
import com.teamcenter.services.strong.core._2013_05.LOV.LOVValueRow;
import com.teamcenter.services.strong.core._2013_05.LOV.ValidateLOVValueSelectionsResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Contains all methods that are used to change properties of Teamcenter
 * objects.
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
@Component
public class ChangeManagement extends Utility {
	private final Logger logger = Logger.getLogger(ChangeManagement.class.getName());
	//private final ImportData importData;
	//private final ClassificationManagement classificationManagement;
	private final AppXSession session;

	private final ConfigProperties settings;

	/**
	 * Initiates the class with a logger.
	 *
	 * @param currentSession
	 * @param settings
	 */
	public ChangeManagement(AppXSession currentSession, ConfigProperties settings) {
		//this.importData = importData;
		session = currentSession;

		//classificationManagement = new ClassificationManagement(session);
		this.settings = settings;
	}

	/**
	 * Change the properties of the given BOMLine to the values given in jsonObject.
	 * This is used for all BOMLines that are below the root object of type 'CT4MB'.
	 * 
	 * @param bomline    The BOMLine whose properties should change
	 * @param jsonObject The JSONObect that contains information over what
	 *                   properties should change
	 */
	public void changeProperties(BOMLine bomline, JSONObject jsonObject) {
		logger.info(String.format("Change the properties of %s.", getDisplayString(bomline)));

		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

		dataManagementService.getProperties(new ModelObject[] { bomline },
				new String[] { TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX });

		final Map<String, VecStruct> propertiesToSet = new HashMap<>();
		VecStruct vecStruct = new VecStruct();

		// Set description to currentBomLine
		if (jsonObject.has(TcConstants.JSON_OBJECT_DESCRIPTION)) {
			vecStruct = new VecStruct();
			vecStruct.stringVec = new String[] {
					JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_DESCRIPTION) };

			propertiesToSet.put(TcConstants.TEAMCENTER_BOMLINE_OBJECT_DESCRIPTION, vecStruct);
			logger.info(String.format("Change the property %s to %s.",
					TcConstants.TEAMCENTER_BOMLINE_OBJECT_DESCRIPTION, vecStruct.stringVec[0]));
		}

		// Change find number if a new one is given
		if (jsonObject.has(TcConstants.JSON_FIND_NO)) {
			vecStruct = new VecStruct();
			vecStruct.stringVec = new String[] { JsonUtil.getAttribute(jsonObject, TcConstants.JSON_FIND_NO) };

			propertiesToSet.put(TcConstants.TEAMCENTER_FIND_NO, vecStruct);
			logger.info(String.format("Change the property %s to %s.", TcConstants.TEAMCENTER_FIND_NO,
					vecStruct.stringVec[0]));
		}

		// Change the quantity

		// Change the position designator
		if (jsonObject.has(TcConstants.JSON_POSITION_DESIGNATOR)) {
			vecStruct = new VecStruct();
			vecStruct.stringVec = new String[] {
					JsonUtil.getAttribute(jsonObject, TcConstants.JSON_POSITION_DESIGNATOR) };

			propertiesToSet.put(TcConstants.TEAMCENTER_BOMLINE_POSITION_DESIGNATOR, vecStruct);
			logger.info(String.format("Change the property %s to %s.",
					TcConstants.TEAMCENTER_BOMLINE_POSITION_DESIGNATOR, vecStruct.stringVec[0]));
		}

		// Change the transformation matrix
		if (jsonObject.has(TcConstants.JSON_COORDINATES) || jsonObject.has(TcConstants.JSON_ROTATION)) {
			vecStruct = new VecStruct();
			vecStruct.stringVec = new String[] { MatrixManagement.calculateTransformationMatrix(
					JsonUtil.getAttribute(jsonObject, TcConstants.JSON_COORDINATES),
					JsonUtil.getAttribute(jsonObject, TcConstants.JSON_ROTATION), getDisplayString(bomline), logger) };

			propertiesToSet.put(TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX, vecStruct);
			try {
				logger.info(String.format("Change the property %s from %s to %s.",
						TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX,
						bomline.get_bl_plmxml_occ_xform(), vecStruct.stringVec[0]));
			} catch (final NotLoadedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE).equals(TcConstants.TEAMCENTER_CABIN)) {

			// Set the pseudo serial number
			if (jsonObject.has(TcConstants.JSON_PSEUDO_SERIAL_NUMBER)) {
				vecStruct = new VecStruct();
				vecStruct.stringVec = new String[] {
						JsonUtil.getAttribute(jsonObject, TcConstants.JSON_PSEUDO_SERIAL_NUMBER) };

				propertiesToSet.put(TcConstants.TEAMCENTER_PSEUDO_SERIAL_NUMBER, vecStruct);

				logger.info(String.format("Change the property %s to %s.", TcConstants.TEAMCENTER_PSEUDO_SERIAL_NUMBER,
						vecStruct.stringVec[0]));
			}
		}

		// Set the properties
		final ServiceData setPropertiesResponse = dataManagementService.setProperties(new ModelObject[] { bomline },
				propertiesToSet);

		serviceDataError(setPropertiesResponse, logger);

		try {
			saveBOMLine(bomline, session);
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
	}

	/**
	 * Recursively change the properties of all objects of a structure. Which
	 * properties will change depends on the attributes given in the JSONObject.
	 * 
	 * @param bomLine
	 * @param jsonObject
	 *//*
	public void changePropertiesOfStructure2(BOMLine bomline, StructureObject structureObject,
			SearchManagement searchManagement) {
		logger.info(String.format("Change the properties of %s.", structureObject.getDisplayString()));

		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

		dataManagementService.getProperties(new ModelObject[] { bomline },
				new String[] { TcConstants.TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST });

		try {
			final Map<String, String> propertyMap = structureObject.getProperties();
			final Map<String, VecStruct> propertiesToSet = new HashMap<>();

			// Change properties that are relative to the parent
			if (structureObject.hasParent()) {
				// TODO: enable when property is updated
//				if (structureObject.wasCreated() && structureObject.getItemType().equals("CT4Cabin")) {
//					changeProductVariantRule(bomline, structureObject, dataManagementService, propertiesToSet);
//				}

				// Set PseudoSerialNumber if it's a Cabin
//				if (structureObject.getItemType().equals("CT4Cabin")) {
//					addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_PSEUDO_SERIAL_NUMBER);
//				}

				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_POSITION_DESIGNATOR);
				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_FIND_NO);
				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_QUANTITY);

				// If we have a rotation and coordinates given
				if (!isEmptyOrNull(propertyMap.get(TcConstants.JSON_ROTATION))
						&& !isEmptyOrNull(propertyMap.get(TcConstants.JSON_COORDINATES))) {
					logger.info(String.format("The object %s has coordinates and a rotation given.",
							structureObject.getDisplayString()));
					final VecStruct vecStruct = new VecStruct();

					vecStruct.stringVec = new String[] { MatrixManagement.calculateTransformationMatrix(
							propertyMap.get(TcConstants.JSON_COORDINATES), propertyMap.get(TcConstants.JSON_ROTATION),
							structureObject.getDisplayString(), logger) };
					// TODO: refresh

					propertiesToSet.put(TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX, vecStruct);
					logger.info(String.format("Change the property %s to %s.",
							TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX, vecStruct.stringVec[0]));
				}
			}

			if (!bomline.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST).isBlank()) {
				logger.info(String.format("The current object has a release status given."));

				// Set the properties
				if (!propertiesToSet.isEmpty()) {
					final ServiceData setPropertiesResponse = dataManagementService
							.setProperties(new ModelObject[] { bomline }, propertiesToSet);

					if (serviceDataError(setPropertiesResponse, logger)) {
						return;
					}
				}

				// If the StructureObject has children
				if (structureObject.getChildren().size() > 0) {
					iterateChildren(bomline, structureObject, searchManagement, dataManagementService);
				}
			} else if (!(structureObject instanceof ModularBuilding)) {
				logger.info(String.format("The current object has no release status given."));

				// If we have an objectGroupID given
				if (!isEmptyOrNull(propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID))) {

					// If the object doesn't already have the correct object group
					if (!searchManagement.hasCorrectObjectGroup((ItemRevision) bomline.get_bl_revision(),
							propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID))) {
						setObjectGroup(searchManagement, bomline, propertyMap);
					}
				}

				// Change the description if any was given
				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_OBJECT_DESCRIPTION);

				// Set the properties
				final ServiceData setPropertiesResponse = dataManagementService
						.setProperties(new ModelObject[] { bomline }, propertiesToSet);

				if (serviceDataError(setPropertiesResponse, logger)) {
					return;
				}

				// Classify the object, if attributes are given and it's not a solution variant
				if (settings.isAlwaysClassify() && canClassify(structureObject)
						&& !(structureObject instanceof SolutionVariant)) {
					classificationManagement.classify(structureObject, searchManagement);
				}

				// If the StructureObject has children
				if (structureObject.getChildren().size() > 0) {
					iterateChildren(bomline, structureObject, searchManagement, dataManagementService);
				}
			} else {
				logger.info(String.format("The current object is a Modular Building."));

				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_DESIGN_NO);
				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_DESIGNER);
				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_OBJECT_DESCRIPTION);

				// Set the properties
				final ServiceData setPropertiesResponse = dataManagementService
						.setProperties(new ModelObject[] { bomline }, propertiesToSet);

				if (serviceDataError(setPropertiesResponse, logger)) {
					return;
				}

				// If the StructureObject has children
				if (structureObject.getChildren().size() > 0) {
					iterateChildren(bomline, structureObject, searchManagement, dataManagementService);
				}
			}

			// Call the workflow if any is given and the object was just created
			if (!structureObject.getWorkflow().isBlank() && structureObject.wasCreated()) {
				addWorkflowAsync(bomline, structureObject.getWorkflow());
			}
			logger.info("workflow triggered successfully");

			// Call the workflow if the object has no release status yet
//			if (!structureObject.getReleaseStatus().isBlank() && structureObject.wasCreated()
//					&& !(structureObject instanceof SolutionVariant)) {
//				addWorkflow(bomline, TcConstants.TEAMCENTER_WORKFLOW_SBOMI_TRIGGER);
//			}

			saveBOMLine(bomline, session);

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		} catch (final NullPointerException e) {
			logger.severe(String.format("Couldn't access an object correctly.\n\n%s.", e.getMessage()));

			e.printStackTrace();
		}
	}*/

	/**
	 * Change the properties from the given object as JSONObject and
	 * StructureObject. This object is neither a child nor does it have children.
	 * 
	 * @param jsonObject
	 * @param structureObject
	 * @param searchManagement
	 */
	/*public void changeSingleObjectProperties(BOMLine bomline, StructureObject structureObject,
			SearchManagement searchManagement) {
		logger.info(String.format("Change the properties of the object %s.", structureObject.getDisplayString()));

		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

//		dataManagementService.getProperties(new ModelObject[] { bomline },
//				new String[] { TcConstants.TEAMCENTER_BOMLINE_ALL_WORKFLOWS });

		try {
			final Map<String, String> propertyMap = structureObject.getProperties();
			final Map<String, VecStruct> propertiesToSet = new HashMap<>();

			if (!bomline.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_ALL_WORKFLOWS).isBlank()) {
				logger.info(String.format("The current object has a release status given."));

				// Set the properties
				final ServiceData setPropertiesResponse = dataManagementService
						.setProperties(new ModelObject[] { bomline }, propertiesToSet);

				if (serviceDataError(setPropertiesResponse, logger)) {
					return;
				}
			} else {
				logger.info(String.format("The current object has no release status given."));

				// Set PseudoSerialNumber if it's a Cabin
//				if (structureObject.getItemType().equals("CT4Cabin")) {
//					addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_PSEUDO_SERIAL_NUMBER);
//				}

				// Change the description if any was given
				addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_OBJECT_DESCRIPTION);

				// If we have an objectGroupID given
				if (!isEmptyOrNull(propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID))) {

					// If the object doesn't already have the correct object group
					if (!searchManagement.hasCorrectObjectGroup((ItemRevision) bomline.get_bl_revision(),
							propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID))) {
						setObjectGroup(searchManagement, bomline, propertyMap);
					}
				}

				// Set the properties
				final ServiceData setPropertiesResponse = dataManagementService
						.setProperties(new ModelObject[] { bomline }, propertiesToSet);

				if (serviceDataError(setPropertiesResponse, logger)) {
					return;
				}

				// Classify the object, if attributes are given and it's not a solution variant
				if (settings.isAlwaysClassify() && canClassify(structureObject)) {
					classificationManagement.classify(structureObject, searchManagement);
				}
			}

			// Call the workflow if the object has no release status yet and was just
			// created
			if (!structureObject.getWorkflow().isBlank()) {
				addWorkflowAsync(bomline, structureObject.getWorkflow());
			}
			logger.info("workflow triggered successfully");

			saveBOMLine(bomline, session);

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		} catch (final NullPointerException e) {
			logger.severe(String.format("Couldn't access an object correctly.\n\n%s.", e.getMessage()));

			e.printStackTrace();
		}
	}*/
/**
 *
 *
 * @param bomline
 * @param structureObject
 * @param searchManagement
 * @param dataManagementService
 * @throws NotLoadedException
 *//*
	public void iterateChildren(BOMLine bomline, StructureObject structureObject, SearchManagement searchManagement,
			final DataManagementService dataManagementService) throws NotLoadedException {
		logger.info(String.format("Iterate through all children of %s and change their properties accordingly.",
				structureObject.getDisplayString()));

//		dataManagementService.getProperties(new ModelObject[] { bomline },
//				new String[] { TcConstants.TEAMCENTER_BOMLINE_CHILDREN });

		final ModelObject[] children = bomline.get_bl_child_lines();
		final BOMLine[] bomChildren = new BOMLine[children.length];

		for (int i = 0; i < children.length; i++) {
			bomChildren[i] = (BOMLine) children[i];
		}
		final List<StructureObject> structureChildren = new ArrayList<>(structureObject.getChildren());

		// Change the properties of every child object
		for (final BOMLine bomChild : bomChildren) {

			final StructureObject childObject = getCorrectChild(structureChildren, bomChild);

			if (childObject == null) {
				logger.severe(String.format("Couldn't find a StructureObject for the BOMLine child %s.",
						getDisplayString(bomChild)));
				saveBOMLine(bomline, session);
				return;
			}

			changePropertiesOfStructure2(bomChild, childObject, searchManagement);
		}
	}*/


	/**
	 * Check if the StructureObject has properties so that it can be classified,
	 * 
	 * @param structureObject
	 * @return
	 */
	public boolean canClassify(StructureObject structureObject) {
		return !structureObject.getClassificationMap().isEmpty() || (structureObject instanceof SolutionVariant
				&& !((SolutionVariant) structureObject).getVariantRules().isEmpty());
	}

	/**
	 * Add the property with the given name to the map.
	 * 
	 * @param propertyMap
	 * @param propertiesToSet
	 * @param propertyName
	 */
	public void addPropertyToSet(Map<String, String> propertyMap, Map<String, VecStruct> propertiesToSet,
			String propertyName) {

//		logger.severe(String.format("DEBUG: %s", propertyName));

		if (!isEmptyOrNull(propertyMap.get(propertyName))) {
			logger.info(String.format("Change the property %s to %s.", propertyName, propertyMap.get(propertyName)));

			final VecStruct vecStruct = new VecStruct();
			vecStruct.stringVec = new String[] { propertyMap.get(propertyName) };
			propertiesToSet.put(getPropertyName(propertyName), vecStruct);
		}
	}

	/**
	 * 
	 * @param structureChildren
	 * @param bomline
	 * @return
	 */
	public StructureObject getCorrectChild(List<StructureObject> structureChildren, BOMLine bomline) {
		logger.info(String.format("Get the correct BOMLine child from %s.", getDisplayString(bomline)));

		String parentObjectString;
		String childObjectString;

		BOMLine childBomline;
		try {
//			dataManagementService.getProperties(new ModelObject[] { bomline },
//					new String[] { TcConstants.TEAMCENTER_BOMLINE_CHILDREN });
			parentObjectString = bomline.get_object_string();
			final String unityMatrix = "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1";

			// Compare all possible StructureObjects
			for (final StructureObject childObject : structureChildren) {
				childBomline = childObject.getBomLine();
				childObjectString = childBomline.get_object_string();

//				dataManagementService.getProperties(new ModelObject[] { childBomline },
//						new String[] { TcConstants.TEAMCENTER_BOMLINE_CHILDREN,
//								TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX });

//				if (childBomline.getUid().compareTo(bomline.getUid()) == 0) {
//					structureChildren.remove(childObject);
//					return childObject;
//				}

				// Compare the name with the ID and the amount of children
				if (parentObjectString.equals(childObjectString)
						&& childBomline.get_bl_child_lines().length == bomline.get_bl_child_lines().length) {

					// If we have a rotation and coordinates given
					if (!isEmptyOrNull(childObject.getProperties().get(TcConstants.JSON_ROTATION))
							&& !isEmptyOrNull(childObject.getProperties().get(TcConstants.JSON_COORDINATES))) {

						final String transformationMatrix = MatrixManagement.calculateTransformationMatrix(
								childObject.getProperties().get(TcConstants.JSON_COORDINATES),
								childObject.getProperties().get(TcConstants.JSON_ROTATION),
								childObject.getDisplayString(), logger);

//						if ((!childObject.wasFound() && !childObject.wasCreated())
//								&& (childBomline.get_bl_plmxml_occ_xform().equals(transformationMatrix)
//										|| childBomline.get_bl_plmxml_occ_xform().isBlank())) {
//
//							// If the matrix is different
//							structureChildren.remove(childObject);
//							return childObject;
//						} else if ((childObject.wasCreated() || childObject.wasFound())
//								&& (childBomline.get_bl_plmxml_occ_xform().isBlank()
//										|| unityMatrix.equals(childBomline.get_bl_plmxml_occ_xform()))) {
//
//							// Matrix is not yet set
//							structureChildren.remove(childObject);
//							return childObject;
//						}
					}
				}
			}

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get the Teamcenter property name corresponding to the given JSON attribute
	 * name.
	 * 
	 * @param string
	 * @return
	 */
	private String getPropertyName(String string) {
		switch (string) {
		case TcConstants.JSON_DESIGN_NO:
			return TcConstants.TEAMCENTER_BOMLINE_DRAWING_NO;
		case TcConstants.JSON_DESIGNER:
			return TcConstants.TEAMCENTER_BOMLINE_DESIGNER;
		case TcConstants.JSON_OBJECT_DESCRIPTION:
			return TcConstants.TEAMCENTER_BOMLINE_OBJECT_DESCRIPTION;
		case TcConstants.JSON_POSITION_DESIGNATOR:
			return TcConstants.TEAMCENTER_BOMLINE_POSITION_DESIGNATOR;
		case TcConstants.JSON_FIND_NO:
			return TcConstants.TEAMCENTER_FIND_NO;
		case TcConstants.JSON_PSEUDO_SERIAL_NUMBER:
			return TcConstants.TEAMCENTER_PSEUDO_SERIAL_NUMBER;
		case TcConstants.JSON_QUANTITY:
			return TcConstants.TEAMCENTER_QUANTITY;
		default:
			return "";
		}
	}

	/**
	 * Set the object group reference on the given BOMLine.
	 * 
	 * @param searchManagement
	 * @param bomline
	 * @param propertyMap
	 */
	public void setObjectGroup(SearchManagement searchManagement, final BOMLine bomline,
			final Map<String, String> propertyMap) {
		logger.info(String.format("Set the object group of %s to %s.", getDisplayString(bomline),
				propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID)));
		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

		final LOVValueRow[] objectGroupEntries = searchManagement
				.getObjectGroupEntry(propertyMap.get(TcConstants.JSON_OBJECT_TYPE));

		// List all UIDs in an array
		final String[] uidArray = new String[objectGroupEntries.length];
		for (int i = 0; i < objectGroupEntries.length; i++) {
			uidArray[i] = objectGroupEntries[i].uid;
		}

		// Remove all itemIDs that we found
		final ServiceData serviceData = dataManagementService.loadObjects(uidArray);
		for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
			final ModelObject currentObject = serviceData.getPlainObject(i);
			try {
				final ItemRevision itemRevision = (ItemRevision) bomline.get_bl_revision();

//				dataManagementService.getProperties(new ModelObject[] { currentObject },
//						new String[] { TcConstants.TEAMCENTER_ITEM_ID });

				// If the we found the correct object group
				if (currentObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_ITEM_ID)
						.equals(propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID))) {

					final Map<String, String[]> propertyValues = new HashMap<>();
					propertyValues.put(TcConstants.TEAMCENTER_OBJECT_GROUP, new String[] { objectGroupEntries[i].uid });

					// Configure the lov properties
					final LOVInput lovInput = new LOVInput();
					lovInput.operationName = "Edit";
					lovInput.boName = itemRevision.getTypeObject().getName();
					lovInput.owningObject = itemRevision;
					lovInput.propertyValues = propertyValues;

					final ValidateLOVValueSelectionsResponse response = LOVService.getService(session.getConnection())
							.validateLOVValueSelections(lovInput, TcConstants.TEAMCENTER_OBJECT_GROUP,
									new String[] { objectGroupEntries[i].uid });

					serviceDataError(response.serviceData, logger);

					final VecStruct vecStruct = new VecStruct();
					vecStruct.stringVec = new String[] { objectGroupEntries[i].uid };

					final Map<String, VecStruct> propertiesToSet = new HashMap<>();
					propertiesToSet.put(TcConstants.TEAMCENTER_OBJECT_GROUP, vecStruct);

					// Set the properties
					final ServiceData setPropertiesResponse = dataManagementService
							.setProperties(new ModelObject[] { itemRevision }, propertiesToSet);

					serviceDataError(setPropertiesResponse, logger);
					break;
				}

			} catch (final NotLoadedException e) {
				logger.severe(String.format("Couldn't load the product variant rule of the %s.",
						getDisplayString(currentObject)));

				e.printStackTrace();
			}
		}
	}

	/**
	 * Add a workflow with the given name to the given ModelObject.
	 * 
	 * @param modelObject
	 * @param workflowName
	 */
	public void addWorkflow(ModelObject modelObject, String workflowName) {
		logger.info(String.format("Add the workflow %s on the current object.", workflowName));

		final WorkflowService workflow = WorkflowService.getService(session.getConnection());
		try {
			// We need the ItemRevision
//			dataManagementService.getProperties(new ModelObject[] { modelObject }, new String[] { "bl_revision" });

			// 1 = target | 3 = reference
			final int[] relationTypeArray = new int[] { 1 };

			// Create the context
			final ContextData contextData = new ContextData();
			contextData.attachmentCount = 1; // Workflow and Target object
			contextData.attachments = new String[] { ((BOMLine) modelObject).get_bl_revision().getUid() };
			contextData.attachmentTypes = relationTypeArray;
			contextData.processTemplate = workflowName;

			final InstanceInfo instanceInfoResponse = workflow.createInstance(true, null, workflowName, null,
					"generate a workflow", contextData);

			logger.info(String.format("Workflow response partial errors:%s",instanceInfoResponse.serviceData.sizeOfPartialErrors()));
			if (serviceDataError(instanceInfoResponse.serviceData, logger)) {
				return;
			}
		} catch (final NotLoadedException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		}
	}

	public CompletableFuture<Void> addWorkflowAsync(ModelObject modelObject, String workflowName) {
		return CompletableFuture.runAsync(() -> {
			addWorkflow(modelObject, workflowName);
		});
	}

}
