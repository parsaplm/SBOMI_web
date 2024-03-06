package com.parsa.middleware.service;

import com.cfg0.services.internal.loose.configurator._2014_06.ConfiguratorManagement.CreateUpdateVariantRulesResponse;
import com.parsa.middleware.businessobjects.ImportStatistic;
import com.parsa.middleware.businessobjects.SolutionVariant;
import com.parsa.middleware.businessobjects.StructureObject;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.session.AppXSession;
import com.parsa.middleware.util.JsonUtil;
import com.smc0.services.loose.structuremanagement.SolutionVariantManagementService;
import com.smc0.services.loose.structuremanagement._2018_11.SolutionVariantManagement.SearchParameter;
import com.smc0.services.loose.structuremanagement._2018_11.SolutionVariantManagement.SearchSVItemInput;
import com.smc0.services.loose.structuremanagement._2018_11.SolutionVariantManagement.SearchSVItemsResponse;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core.DataManagementService;
import com.teamcenter.services.loose.core._2007_01.DataManagement.GetItemFromIdPref;
import com.teamcenter.services.loose.core._2007_01.DataManagement.RelationFilter;
import com.teamcenter.services.loose.core._2007_06.DataManagement.RelationAndTypesFilter;
import com.teamcenter.services.loose.core._2007_09.DataManagement.ExpandGRMRelationsPref2;
import com.teamcenter.services.loose.core._2007_09.DataManagement.ExpandGRMRelationsResponse2;
import com.teamcenter.services.loose.core._2009_10.DataManagement.GetItemFromAttributeInfo;
import com.teamcenter.services.loose.core._2009_10.DataManagement.GetItemFromAttributeResponse;
import com.teamcenter.services.strong.cad.StructureManagementRestBindingStub;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.GetRevisionRulesResponse;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.RevisionRuleInfo;
import com.teamcenter.services.strong.core.LOVService;
import com.teamcenter.services.strong.core._2013_05.LOV.*;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2007_09.SavedQuery.QueryResults;
import com.teamcenter.services.strong.query._2007_09.SavedQuery.SavedQueriesResponse;
import com.teamcenter.services.strong.query._2010_09.SavedQuery.BusinessObjectQueryClause;
import com.teamcenter.services.strong.query._2010_09.SavedQuery.BusinessObjectQueryInput;
import com.teamcenter.services.strong.query._2019_06.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Contains all methods that are used to search for objects in Teamcenter.
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class SearchManagement extends Utility {
	private final Logger logger;
	private final DataManagementService dataManagementService;
	private final String releaseStatus;
	private final AppXSession session;

	private final Map<String, BOMLine> bomLineCache = new HashMap<>();

	/**
	 * Initiate the class with a logger.
	 * 
	 * @param logger
	 */
	public SearchManagement(Logger logger, String status, AppXSession currentSession) {
		this.logger = logger;
		releaseStatus = status;
		session = currentSession;

		dataManagementService = DataManagementService.getService(session.getConnection());
	}

	/**
	 * Searches for an object with the given object type and the given object ID.
	 * 
	 * @param objectType The technical object type name
	 * @param objectID   The object ID of the desired object
	 * @return The desired object if it was found. Null otherwise.
	 */
	public Item searchObject(String objectType, String objectID) {
		logger.info(String.format("Search for the object of the type %s with the item ID %s.", objectType, objectID));

		final Map<String, String> itemAttributeMap = new HashMap<>();
		itemAttributeMap.put(TcConstants.TEAMCENTER_ITEM_ID, objectID);
		itemAttributeMap.put(TcConstants.TEAMCENTER_OBJECT_TYPE, objectType);

		final GetItemFromAttributeInfo attributeInfo = new GetItemFromAttributeInfo();
		attributeInfo.itemAttributes = itemAttributeMap;

		final RelationFilter pref = new RelationFilter();
		pref.objectTypeNames = new String[] { objectType };

		final GetItemFromIdPref prefIds = new GetItemFromIdPref();
		prefIds.prefs = new RelationFilter[] { pref };

		final GetItemFromAttributeResponse responseItem = dataManagementService
				.getItemFromAttribute(new GetItemFromAttributeInfo[] { attributeInfo }, 1, prefIds);

		// Return the object if it was found
		if (!Utility.serviceDataError(responseItem.serviceData, logger)
				&& responseItem.serviceData.sizeOfPlainObjects() > 0) {
			logger.info(String.format("Found %d entries for the object with the ID %s",
					responseItem.serviceData.sizeOfPlainObjects(), objectID));
			return (Item) responseItem.serviceData.getPlainObject(0);
		}

		return null;
	}

	/**
	 * Get all objects with the object type in the given JSONObject and return all
	 * of them, which have the same object group ID as the JSONObject.
	 * 
	 * @param json
	 * @return
	 */
	public List<BOMLine> searchObjectsByType(String objectType, String objectGroupID) {
		logger.info("Get all objects with the type " + objectType + ".");
		final ArrayList<BOMLine> returnedObjects = new ArrayList<>();

		final BusinessObjectQueryInput input = new BusinessObjectQueryInput();
		final BusinessObjectQueryClause clauseObjectName = new BusinessObjectQueryClause();

		final SavedQueryService queryService = SavedQueryService.getService(session.getConnection());

		final String objectGroupName = getObjectGroupName(objectType, objectGroupID);

		// We need a clause. In this case we just take all objects with a name
		clauseObjectName.mathOperator = "=";
		clauseObjectName.logicOperator = "AND";
		clauseObjectName.propName = TcConstants.TEAMCENTER_OBJECT_NAME;
		clauseObjectName.propValue = "*";

		// Set the options for the query input
		input.clientId = input.getClass().getName() + Integer.toString(input.hashCode());
		input.maxNumToReturn = 0;
		input.typeName = objectType + "Revision";
		input.clauses = new BusinessObjectQueryClause[] { clauseObjectName };

		// Get the response
		final SavedQueriesResponse resp = queryService
				.executeBusinessObjectQueries(new BusinessObjectQueryInput[] { input });

		if (!serviceDataError(resp.serviceData, logger)) {

			try {
				for (final QueryResults result : resp.arrayOfResults) {
					final ServiceData sData = dataManagementService.loadObjects(result.objectUIDS);

					final ModelObject[] itemRevisionArray = new ModelObject[sData.sizeOfPlainObjects()];

					// Check the objects for the correct object group
					for (int i = 0; i < sData.sizeOfPlainObjects(); i++) {
						itemRevisionArray[i] = sData.getPlainObject(i);
					}

//					dataManagementService.getProperties(itemRevisionArray,
//							new String[] { TcConstants.TEAMCENTER_OBJECT_GROUP });

					for (int i = 0; i < sData.sizeOfPlainObjects(); i++) {
						final ItemRevision tempRevision = (ItemRevision) sData.getPlainObject(i);

						if (tempRevision.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_GROUP)
								.equals(objectGroupName) && hasCorrectObjectGroup(tempRevision, objectGroupID)) {
							returnedObjects.add(getBomLine(tempRevision, session));

						}
					}
				}
			} catch (final NotLoadedException e) {
				logger.severe("At least one property couldn't get loaded.\n" + e.getMessage());

				e.printStackTrace();
			}
		}

		logger.info(String.format("Found %d object(s) with the type %sRevision and the object group ID %s.",
				returnedObjects.size(), objectType, objectGroupID));
		return returnedObjects;
	}

	/**
	 * Search for all objects with the given object group.
	 *
	 * @return
	 * @throws ServiceException
	 */
	public List<BOMLine> searchObjectsWithObjectGroup(String objectType, String objectGroupID, String releaseStatus) {
		logger.info(String.format("Get all objects of the type %s with the object group %s and %s.", objectType,
				objectGroupID, releaseStatus.isBlank() ? "no release status" : "the release status " + releaseStatus));
		final List<BOMLine> returnedObjects = new ArrayList<>();

		final SavedQuery.BusinessObjectQueryInput3 input = new SavedQuery.BusinessObjectQueryInput3();
		final BusinessObjectQueryClause objectGroupClause = new BusinessObjectQueryClause();
		final BusinessObjectQueryClause releaseStatusClause = new BusinessObjectQueryClause();
//		final BusinessObjectQueryClause childrenClause = new BusinessObjectQueryClause();

		// Get the service stub
		final SavedQueryService queryService = SavedQueryService.getService(session.getConnection());
		final DataManagementService dmService = DataManagementService.getService(session.getConnection());

		objectGroupClause.mathOperator = "=";
		objectGroupClause.logicOperator = "AND";
		objectGroupClause.propValue = objectGroupID;
		objectGroupClause.propName = TcConstants.TEAMCENTER_LOV + ":" + TcConstants.TEAMCENTER_OBJECT_GROUP + "."
				+ TcConstants.TEAMCENTER_ITEM_ID;
// "CT4_LOV:ct4_object_gr.item_id"

		// The object must have this release status
		releaseStatusClause.mathOperator = "=";
		releaseStatusClause.logicOperator = "AND";
		releaseStatusClause.propValue = releaseStatus;
		releaseStatusClause.propName = TcConstants.TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST + ".name";

		//
		// BOMView Revision:structure_revisions.PSOccurrence<-parent_bvr.child_item

		// Set the options for the query input
		input.clientId = input.getClass().getName() + Integer.toString(input.hashCode());
		input.maxNumToReturn = 100;
		input.boTypeName = objectType + "Revision";
		input.clauses = releaseStatus.isBlank() ? new BusinessObjectQueryClause[] { objectGroupClause }
				: new BusinessObjectQueryClause[] { objectGroupClause, releaseStatusClause };

		// Get the response
		final SavedQueriesResponse resp = queryService
				.executeBOQueriesWithSort(new SavedQuery.BusinessObjectQueryInput3[] { input });

		if (serviceDataError(resp.serviceData, logger)) {
			return returnedObjects;
		}

		for (final QueryResults arrayOfResult : resp.arrayOfResults) {
			final ServiceData serviceData = dmService.loadObjects(arrayOfResult.objectUIDS);

			// Add all objects to an array
			for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
				final ItemRevision tempRevision = (ItemRevision) serviceData.getPlainObject(i);
//				try {
//					final ModelObject[] statusArray = tempRevision
//							.getPropertyObject(TcConstants.TEAMCENTER_ALL_WORKFLOWS).getModelObjectArrayValue();
//
//					// Return the solution object if the release status is correct
//					if (releaseStatus.isBlank() && statusArray.length == 0) {
//
//						// No release status is required and the solution variant has none
//						returnedObjects.add(getBomLine(tempRevision, session));
//					} else if (statusArray.length > 0 && statusArray[0]
//							.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_NAME).equals(releaseStatus)) {
//
//						// The release status name and the variant rules are the same
//						returnedObjects.add(getBomLine(tempRevision, session));
//					}
//				} catch (final NotLoadedException e) {
//					logger.severe("A property of an object wasn't properly loaded.\n" + e.getMessage());
//
//					e.printStackTrace();
//				}
				returnedObjects.add(getBomLine(tempRevision, session));

			}
//			}
		}

		logger.info(String.format("Found %d %s objects with the object group %s and %s.", returnedObjects.size(),
				objectType, objectGroupID,
				releaseStatus.isBlank() ? "no release status" : "the release status " + releaseStatus));
		return returnedObjects;
	}

	/**
	 * Get the display name of the object group with the given type and ID.
	 * 
	 * @param objectType
	 * @param objectGroupID
	 * @return
	 */
	private String getObjectGroupName(String objectType, String objectGroupID) {
		logger.info(String.format("Get the display name of the object group with the type %s and the ID %s.",
				objectType, objectGroupID));

		final LOVValueRow[] lovValues = getObjectGroupEntry(objectType);

		// List all UIDs in an array
		final String[] uidArray = new String[lovValues.length];
		for (int i = 0; i < lovValues.length; i++) {
			uidArray[i] = lovValues[i].uid;
		}

		// Get the correct object group name
		final ServiceData serviceData = dataManagementService.loadObjects(uidArray);
		for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
			final ModelObject currentObject = serviceData.getPlainObject(i);

			try {
				if (objectGroupID.equals(currentObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_ITEM_ID))) {
					return currentObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_CURRENT_NAME);
				}
			} catch (final NotLoadedException e) {
				logger.severe("At least one property couldn't get loaded.\n" + e.getMessage());

				e.printStackTrace();
			}
		}
		return "*";
	}

	/**
	 * Get all parent objects with the given type from the given object ID.
	 * 
	 * @param objectType
	 * @param objectID
	 * @return
	 */
	public List<Item> searchParentObjects(String objectType, String objectID) {
		logger.info(String.format("Get all parent objects of the type %s from the object %s.", objectType, objectID));
		final List<Item> returnedObjects = new ArrayList<>();

		final SavedQuery.BusinessObjectQueryInput3 input = new SavedQuery.BusinessObjectQueryInput3();
		final BusinessObjectQueryClause objectIdClause = new BusinessObjectQueryClause();

		// Get the service stub
		final SavedQueryService queryService = SavedQueryService.getService(session.getConnection());
		final DataManagementService dmService = DataManagementService.getService(session.getConnection());

		objectIdClause.mathOperator = "=";
		objectIdClause.logicOperator = "AND";
		objectIdClause.propValue = objectID;
		objectIdClause.propName = "ItemRevision<-items_tag.structure_revisions.PSOccurrence<-parent_bvr.Item:child_item.item_id";

		// Set the options for the query input
		input.clientId = input.getClass().getName() + Integer.toString(input.hashCode());
		input.maxNumToReturn = 0;
		input.boTypeName = objectType;
		input.clauses = new BusinessObjectQueryClause[] { objectIdClause };

		// Get the response
		final SavedQueriesResponse resp = queryService
				.executeBOQueriesWithSort(new SavedQuery.BusinessObjectQueryInput3[] { input });

		if (serviceDataError(resp.serviceData, logger)) {
			return returnedObjects;
		}

		for (final QueryResults arrayOfResult : resp.arrayOfResults) {
			final ServiceData serviceData = dmService.loadObjects(arrayOfResult.objectUIDS);

			// Add all objects to an array
			for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
				final Item tempItem = (Item) serviceData.getPlainObject(i);
				returnedObjects.add(tempItem);
			}
//			
		}

		logger.info(String.format("Found %d %s parent objects of the object %s.", returnedObjects.size(), objectType,
				objectID));
		return returnedObjects;
	}

	/**
	 * Search in Teamcenter for an already existing solution variant with the given
	 * variant rule object.
	 *
	 * @param solutionVariantManagement
	 * @param genericBomLine            The generic object from the desired solution
	 *                                  variant
	 * @param variantRulesResponse      The variant rule object that contains all
	 *                                  variant rules from the solution variant we
	 *                                  want
	 * @param variantManagement
	 * @return The BOMLine of the solution variant, if it was found. Otherwise null.
	 * @throws NotLoadedException
	 */
	public BOMLine searchSolutionVariant(final SolutionVariantManagementService solutionVariantManagement,
										 final BOMLine genericBomLine, final CreateUpdateVariantRulesResponse variantRulesResponse,
										 SolutionVariant solutionVariant, SolutionVariantManagement variantManagement) throws NotLoadedException {
		logger.info(String.format("Search for an already existing solution variant from the generic object %s.",
				getDisplayString(genericBomLine)));

		// Set the search preferences
		final Map<String, String> searchPreferences = new HashMap<>();
		searchPreferences.put(TcConstants.TEAMCENTER_OUTPUT_PAGE_SIZE, "0"); // all possible solution variants
		searchPreferences.put(TcConstants.TEAMCENTER_USER_OR_SYSTEM_SELECTED, "1"); // use user selected variant rules
		searchPreferences.put(TcConstants.TEAMCENTER_FULL_OR_PARTIAL_MATCHING, "0"); // all variant rules must match

		// Set the search parameter
		final SearchParameter searchParameter = new SearchParameter();
		searchParameter.svCategories = 4; // only reuse
		searchParameter.searchVariantRule = variantRulesResponse.ruleOutputs[0].ruleObject;
		searchParameter.searchPreferences = searchPreferences;

//		dataManagementService.getProperties(new ModelObject[] { genericBomLine },
//				new String[] { TcConstants.TEAMCENTER_ITEM_REVISION });

		final SearchSVItemInput searchSVItemInput = new SearchSVItemInput();
		searchSVItemInput.searchItemRevision = genericBomLine.get_bl_revision();
		searchSVItemInput.selectedItemRevision = genericBomLine.get_bl_revision();

		final SearchSVItemsResponse searchSVItemsResponse = solutionVariantManagement
				.searchSolutionVariantItems3(new SearchSVItemInput[] { searchSVItemInput }, searchParameter);

		// Did an error occur when searching for solution variant items?
		if (serviceDataError(searchSVItemsResponse.serviceData, logger)) {
			return null;
		}

		final ModelObject[] foundSolutionVariants = new ModelObject[searchSVItemsResponse.serviceData
				.sizeOfPlainObjects()];

		// Put all found solution variants in one array
		for (int i = 0; i < searchSVItemsResponse.serviceData.sizeOfPlainObjects(); i++) {
			foundSolutionVariants[i] = searchSVItemsResponse.serviceData.getPlainObject(i);
		}

		return getSolutionVariantWithStatus(foundSolutionVariants, solutionVariant.getReleaseStatus(), solutionVariant, variantManagement);
	}

	/**
	 * Check if the given ItemRevision has the given release status.
	 *
	 * @param solutionVariantRevision
	 * @param releaseStatus
	 * @param variantManagement
	 * @return
	 */
	private BOMLine getSolutionVariantWithStatus(ModelObject[] solutionVariantArray, String releaseStatus,
												 SolutionVariant solutionVariant, SolutionVariantManagement variantManagement) {
		logger.info(String.format(
				"Check if the current object has the release status '%s' and the variant rules are correct.",
				releaseStatus));

		try {
			ModelObject[] statusArray;
			BOMLine bomline;

			// Iterate through all solution variants and check their release status
			for (final ModelObject element : solutionVariantArray) {
				bomline = getBomLine((ItemRevision) element, session);

				DataManagementService.getService(session.getConnection()).getProperties(new ModelObject[] { element },
						new String[] { TcConstants.TEAMCENTER_RELEASE_STATUS_LIST });
				statusArray = element.getPropertyObject(TcConstants.TEAMCENTER_RELEASE_STATUS_LIST)
						.getModelObjectArrayValue();
				if(statusArray.length > 0)
					DataManagementService.getService(session.getConnection()).getProperties(new ModelObject[] { statusArray[0] },
							new String[] { TcConstants.TEAMCENTER_OBJECT_NAME });
				// Return the solution object if the release status is correct
				if (releaseStatus.isBlank() && statusArray.length == 0) {

					// No release status is required and the solution variant has none
					logger.info("Found the solution variant " + getDisplayString(element) + ".");
					return getBomLine((ItemRevision) element, session);
				} else if (statusArray.length > 0
						&& statusArray[0].getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_NAME)
								.equals(releaseStatus)
						&& variantRuleIsTheSame(bomline, solutionVariant.getOldJsonObject())) {

					// The release status name and the variant rules are the same
					logger.info("Found the solution variant " + getDisplayString(element) + ".");
					return getBomLine((ItemRevision) element, session);
				}
				else if(!releaseStatus.isBlank() && variantRuleIsTheSame(bomline, solutionVariant.getOldJsonObject())){
					// The release status is not in TC but in JSON file
					logger.info("Found the solution variant " + getDisplayString(element) + ".");
					variantManagement.setTriggerWorkFlowAndExit(true);
					return getBomLine((ItemRevision) element, session);
				}
				else if(releaseStatus.isBlank() && variantRuleIsTheSame(bomline, solutionVariant.getOldJsonObject())){
					// The release status is in TC but not in JSON file
					logger.info("Found the solution variant " + getDisplayString(element) + ".");
					return getBomLine((ItemRevision) element, session);
				}
			}
		} catch (final NotLoadedException e) {
			logger.severe("A property of an object wasn't properly loaded.\n" + e.getMessage());

			e.printStackTrace();
		}

		logger.info("Didn't found an existing solution variant.");
		return null;
	}

	/**
	 * Check if the given ItemRevision has the given release status.
	 * 
	 * @param solutionVariantRevision
	 * @param releaseStatus
	 * @return
	 */
	private boolean hasCorrectStatus(BOMLine bomline, String releaseStatus) {
		logger.info(String.format("Check if the current object has the release status '%s'.", releaseStatus));

		try {

			// Get all status from the solution variant
//			final ModelObject statusArray = bomline
//					.getPropertyObject(TcConstants.TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST).getModelObjectValue();

			// The solution variant shouldn't have a release status if none is given
			return bomline.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST)
					.contains(releaseStatus);
		} catch (final NotLoadedException e) {
			logger.severe("A property of an object wasn't properly loaded.\n" + e.getMessage());

			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Search for a RevisionRule with the given name.
	 * 
	 * @return The RevisionRule with the given name. Null otherwise.
	 */
	public RevisionRule searchRevisionRule(String ruleName) {
		logger.info(String.format("Search for the revision rule with the name %s.", ruleName));

		final BusinessObjectQueryInput input = new BusinessObjectQueryInput();
		final BusinessObjectQueryClause clauseRuleName = new BusinessObjectQueryClause();

		final SavedQueryService queryService = SavedQueryService.getService(session.getConnection());

		clauseRuleName.mathOperator = "=";
		clauseRuleName.logicOperator = "AND";
		clauseRuleName.propName = TcConstants.TEAMCENTER_OBJECT_NAME;
		clauseRuleName.propValue = ruleName;

		// Set the options for the query input
		input.clientId = input.getClass().getName() + Integer.toString(input.hashCode());
		input.maxNumToReturn = 0;
		input.typeName = TcConstants.TEAMCENTER_REVISIONRULE;
		input.clauses = new BusinessObjectQueryClause[] { clauseRuleName };

		// Get the response
		final SavedQueriesResponse resp = queryService
				.executeBusinessObjectQueries(new BusinessObjectQueryInput[] { input });

		if (!serviceDataError(resp.serviceData, logger) && resp.arrayOfResults.length > 0) {
			final ServiceData sData = dataManagementService.loadObjects(resp.arrayOfResults[0].objectUIDS);

			if (!serviceDataError(sData, logger) && sData.sizeOfPlainObjects() > 0) {

				logger.info("Found the revision rule.");
				return (RevisionRule) sData.getPlainObject(0);
			}
		}

		logger.info(String.format("Couldn't find a revision rule with the name %s.", ruleName));
		return null;
	}

	public void searchAllRevisionRules() {
		logger.info("Get all revision rules.");

		final StructureManagement structureManagement = StructureManagementRestBindingStub
				.getService(session.getConnection());

		try {
			final GetRevisionRulesResponse revisionRuleResponse = structureManagement.getRevisionRules();
			if (!serviceDataError(revisionRuleResponse.serviceData, logger)) {
				System.out.println();

				final ModelObject[] revRules = new ModelObject[revisionRuleResponse.output.length];
				int i = 0;
				for (final RevisionRuleInfo revisionRuleInfo : revisionRuleResponse.output) {
					System.out.println();
					revRules[i] = revisionRuleInfo.revRule;
					i++;
				}
			}

		} catch (final ServiceException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		}
	}

	/**
	 * Search for all objects of the given object type with the given design number.
	 * Only one object will be returned because the design number is unique and if a
	 * different structure with the same design number is created, the root object
	 * will just be revised.
	 * 
	 * @return The object with the given design number. Null otherwise.
	 */
	public ItemRevision searchObjectByDesignNo(String objectType, String designNo) {
		logger.info(String.format("Search for the object of the type %s with the drawing number %s.", objectType,
				designNo));
		final ArrayList<ModelObject> returnedObjects = new ArrayList<>();

		final BusinessObjectQueryInput input = new BusinessObjectQueryInput();
		final BusinessObjectQueryClause clauseDesignNo = new BusinessObjectQueryClause();
		final BusinessObjectQueryClause releaseStatusClause = new BusinessObjectQueryClause();

		final SavedQueryService queryService = SavedQueryService.getService(session.getConnection());

		clauseDesignNo.mathOperator = "=";
		clauseDesignNo.logicOperator = "AND";
		clauseDesignNo.propName = TcConstants.TEAMCENTER_DRAWING_NO;
		clauseDesignNo.propValue = designNo;

		// The object must have this release status
		releaseStatusClause.mathOperator = "=";
		releaseStatusClause.logicOperator = "AND";
		releaseStatusClause.propValue = releaseStatus;
		releaseStatusClause.propName = TcConstants.TEAMCENTER_RELEASE_STATUS_LIST + ".name";

		// Set the options for the query input
		input.clientId = input.getClass().getName() + Integer.toString(input.hashCode());
		input.maxNumToReturn = 0;
		input.typeName = objectType + "Revision";
		input.clauses = releaseStatus.isBlank() ? new BusinessObjectQueryClause[] { clauseDesignNo }
				: new BusinessObjectQueryClause[] { clauseDesignNo, releaseStatusClause };

		// Get the response
		final SavedQueriesResponse resp = queryService
				.executeBusinessObjectQueries(new BusinessObjectQueryInput[] { input });

		if (!serviceDataError(resp.serviceData, logger)) {
			System.out.println("DesignNo: " + designNo + ", Found objects: " + resp.arrayOfResults.length);
			for (final QueryResults result : resp.arrayOfResults) {
				final ServiceData sData = dataManagementService.loadObjects(result.objectUIDS);

				for (int i = 0; i < sData.sizeOfPlainObjects(); i++) {
					final ItemRevision tempRevision = (ItemRevision) sData.getPlainObject(i);
					return tempRevision;
//					try {
//						final ModelObject[] statusArray = tempRevision
//								.getPropertyObject(TcConstants.TEAMCENTER_ALL_WORKFLOWS).getModelObjectArrayValue();
//
//						// Return the solution object if the release status is correct
//						if (releaseStatus.isBlank() && statusArray.length == 0) {
//
//							// No release status is required and the solution variant has none
//							returnedObjects.add(tempRevision);
//						} else if (statusArray.length > 0
//								&& statusArray[0].getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_NAME)
//										.equals(releaseStatus)) {
//
//							// The release status name and the variant rules are the same
//							returnedObjects.add(tempRevision);
//						}
//					} catch (final NotLoadedException e) {
//						logger.severe("A property of an object wasn't properly loaded.\n" + e.getMessage());
//
//						e.printStackTrace();
//					}
				}
			}
		}

		logger.info(String.format("Found %d objects with the drawing number %s and %s.", returnedObjects.size(),
				designNo, releaseStatus.isBlank() ? "no release status" : "the release status " + releaseStatus));

		if (returnedObjects.size() > 0) {
			return (ItemRevision) returnedObjects.get(0);
		}

		return null;
	}

	/**
	 * Search for already existing structures that are described in the given
	 * JSONObject.
	 * 
	 * @param jsonObject A JSONObject
	 * @return The root BOMLine of a structure that has the same attributes as the
	 *         JSONObject structure.
	 */
	public BOMLine searchExistingStructure(JSONObject jsonObject, ImportStatistic importStatistic) throws JSONException {
		logger.info(String.format("Search for already existing structures that are described in the JSONObject %s.",
				jsonObject.optString("objectName")));

		final List<BOMLine> rootObjectList = searchObjectsWithObjectGroup(
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE),
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_GROUP_ID), releaseStatus);

		BOMLine finalBomLine = null;
		final long startTime = System.currentTimeMillis();
		final List<CompletableFuture<BOMLine>> bomList = new ArrayList<>();

		final ExecutorService threadPool = Executors.newFixedThreadPool(3);
		for (final BOMLine bomline : rootObjectList) {
			final List<JSONObject> jsonChildrenList = JsonUtil
					.getListFromJsonArray(jsonObject.optJSONArray(TcConstants.JSON_CHILDREN));

			bomList.add(CompletableFuture.supplyAsync(() -> {
				final List<BOMLine> missingLines = compareChildren2(bomline, jsonChildrenList, false);
				if (missingLines != null && missingLines.isEmpty()) {

					logger.info("Found the structure " + getDisplayString(bomline) + ".");
					return bomline;
				}

				closeBOMLines(new BOMLine[] { bomline }, session);

				return null;
			}, threadPool));

		}

		try {
			// TODO: Throws BOMWindow != BOMLine Exception
			// Get the first/only returned BOMLine, that's not null
			finalBomLine = anyMatch(bomList, i -> i != null).get();
			if (finalBomLine != null) {
				threadPool.shutdownNow();
			}
		} catch (final InterruptedException e) {
			logger.log(Level.SEVERE, "The function was interrupted.\n\n" + e.getMessage(), e);

			e.printStackTrace();
		} catch (final ExecutionException e) {
			logger.log(Level.WARNING, "No matching BOMLine was found.\n\n" + e.getMessage(), e);
		}

		final long endTime = System.currentTimeMillis();
		final long time = (endTime - startTime) / 1000;
		System.out.println("Comparing children took: " + time);
		logger.log(Level.INFO, "Comparing children took " + time + " seconds.");
		importStatistic.setTimeCompareExistingStructures(importStatistic.getTimeCompareExistingStructures() + time);

		return finalBomLine;
	}

	/**
	 * Runs all given threads asynchronous and returns the thread that returned a
	 * non null object that matches the criteria.
	 * 
	 * @param <T>
	 * @param list     A list of CompletableFuture objects that have a return value
	 * @param criteria The criteria, when the return value of a CompletableFuture is
	 *                 accepted
	 * @return Returns a completed CompletableFeature that fulfills the given
	 *         criteria.
	 */
	private <T> CompletableFuture<T> anyMatch(List<? extends CompletionStage<? extends T>> list,
			Predicate<? super T> criteria) {
		logger.info("Run all given threads asynchronous and return the first thread that matches the criteria.");

		final CompletableFuture<T> result = new CompletableFuture<>();
		final Consumer<T> whenMatching = v -> {
			if (criteria.test(v)) {
				result.complete(v);
			}
		};

		CompletableFuture.allOf(list.stream().map(f -> f.thenAccept(whenMatching)).toArray(CompletableFuture<?>[]::new))
				.whenComplete(
						(ignored, t) -> result.completeExceptionally(t != null ? t : new NoSuchElementException()));
		return result;
	}

	/**
	 * Get all possible object group entries for the given object type.
	 * 
	 * @param objectType
	 * @return
	 */
	public LOVValueRow[] getObjectGroupEntry(String objectType) {
		logger.info(String.format("Get all possible object group entries for the object type %s.", objectType));

		final Map<String, String[]> propertyValues = new HashMap<>();
		propertyValues.put(TcConstants.TEAMCENTER_LOV_CONTEXT_OBJECT, new String[0]);
		propertyValues.put(TcConstants.TEAMCENTER_LOV_CONTEXT_PROP_NAME, new String[0]);

		final LOVInput lovInput = new LOVInput();
		lovInput.boName = objectType + "Revision";
		lovInput.operationName = "Edit";
		lovInput.owningObject = null;
		lovInput.propertyValues = propertyValues;

		final LovFilterData lovFilterData = new LovFilterData();
		lovFilterData.filterString = "";
		lovFilterData.order = 0;
		lovFilterData.maxResults = 2000;
		lovFilterData.numberToReturn = 200;

		final InitialLovData initialLOVData = new InitialLovData();
		initialLOVData.propertyName = TcConstants.TEAMCENTER_OBJECT_GROUP;
		initialLOVData.lovInput = lovInput;
		initialLOVData.filterData = lovFilterData;

		// Get all possible LOV objects for the object group
		final LOVSearchResults searchResults = LOVService.getService(session.getConnection())
				.getInitialLOVValues(initialLOVData);

		if (!serviceDataError(searchResults.serviceData, logger)) {
			logger.info(String.format("Found %d object group entries for the object type %s.",
					searchResults.lovValues.length, objectType));
			return searchResults.lovValues;
		}

		return new LOVValueRow[0];
	}

	/**
	 * Get all storage classes for a given object type.
	 * 
	 * @param objectType
	 * @return
	 */
	public LOVValueRow[] getStorageClassEntry(String objectType) {
		logger.info(String.format("Get all possible storage class entries for the object type %s.", objectType));

		final Map<String, String[]> propertyValues = new HashMap<>();
		propertyValues.put(TcConstants.TEAMCENTER_LOV_CONTEXT_OBJECT, new String[0]);
		propertyValues.put(TcConstants.TEAMCENTER_LOV_CONTEXT_PROP_NAME, new String[0]);

		final LOVInput lovInput = new LOVInput();
		lovInput.boName = objectType;
		lovInput.operationName = "Edit";
		lovInput.owningObject = null;
		lovInput.propertyValues = propertyValues;

		final LovFilterData lovFilterData = new LovFilterData();
		lovFilterData.filterString = "";
		lovFilterData.order = 0;
		lovFilterData.maxResults = 2000;
		lovFilterData.numberToReturn = 200;

		final InitialLovData initialLOVData = new InitialLovData();
		initialLOVData.propertyName = TcConstants.TEAMCENTER_STORAGE_CLASS;
		initialLOVData.lovInput = lovInput;
		initialLOVData.filterData = lovFilterData;

		// Get all possible LOV objects for the object group
		final LOVSearchResults searchResults = LOVService.getService(session.getConnection())
				.getInitialLOVValues(initialLOVData);

		if (!serviceDataError(searchResults.serviceData, logger)) {
			logger.info(String.format("Found %d storage class entries for the object type %s.",
					searchResults.lovValues.length, objectType));
			return searchResults.lovValues;
		}

		return new LOVValueRow[0];
	}

	/**
	 * Compare all revisions of the rootObject and compare them with the structure
	 * given in the List of JSONObjects.
	 * 
	 * @param rootObject
	 * @param jsonChildrenList
	 * @return
	 */
	public ItemRevision searchRevision(Item rootObject, List<JSONObject> jsonChildrenList) {
		logger.info(String.format(
				"Search for a revision from the object %s that is equal to the structure in the given JSON file.",
				getDisplayString(rootObject)));
		try {

//			dataManagementService.getProperties(new ModelObject[] { rootObject },
//					new String[] { TcConstants.TEAMCENTER_REVISION_LIST });
			final ModelObject[] revisionList = rootObject.get_revision_list();

			// Check all revisions if at least one is equal to the given structure
			for (final ModelObject itemRevision : revisionList) {
				final List<BOMLine> missingSubstructureList = compareChildren2(
						getBomLine((ItemRevision) itemRevision, session), jsonChildrenList, true);

				if (missingSubstructureList != null && missingSubstructureList.isEmpty()) {
					// Found the correct revision

					logger.info(String.format("Found the correct revision %s.", getDisplayString(itemRevision)));
					return (ItemRevision) itemRevision;
				} else if (missingSubstructureList != null) {
					// The revision does have different children

					final String missingChildren = missingSubstructureList.stream().map(Utility::getDisplayString)
							.collect(Collectors.joining(", "));
					logger.warning(String.format(
							"The revision %s has not the correct children. It's missing structures for the objects %s.",
							getDisplayString(itemRevision), missingChildren));
				} else {
					// The revision does not have the same amount of children

					logger.warning(String.format(
							"The revision %s has not the correct children. The amount of children is different.",
							getDisplayString(itemRevision)));
				}
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}

		logger.info("No revision could be found.");
		return null;
	}

	public ItemRevision searchRevision(StructureObject rootObject) {
		logger.info(String.format(
				"Search for a revision from the object %s that is equal to the structure in the given JSON file.",
				rootObject.getDisplayString()));
		try {

//			dataManagementService.getProperties(new ModelObject[] { rootObject.getItem() },
//					new String[] { TcConstants.TEAMCENTER_REVISION_LIST });
			final ModelObject[] revisionList = rootObject.getItem().get_revision_list();

			// Check all revisions if at least one is equal to the given structure
			for (final ModelObject itemRevision : revisionList) {
				if (compareChildren(getBomLine((ItemRevision) itemRevision, session), rootObject, true)) {

					logger.info(String.format("Found the correct revision %s.", getDisplayString(itemRevision)));
					return (ItemRevision) itemRevision;
				} else {
					logger.warning(String.format("The revision %s has not the correct children.",
							getDisplayString(itemRevision)));
				}
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}

		logger.info("No revision could be found.");
		return null;
	}

	/**
	 * Compare the children from a given BOMLine with a list of JSONObjects.
	 * 
	 * @param bomLine
	 * @param jsonChildrenList
	 * @return
	 */
	public List<BOMLine> compareChildren2(BOMLine bomLine, List<JSONObject> jsonChildrenList,
			boolean compareAllLevels) {
		logger.info(String.format(
				"Compare the children from a BOMLine with the corresponding list of children in the given JSON file."));
		final List<BOMLine> bomChildrenList = new ArrayList<>();
		boolean bomlineFound = false;

		try {
			final ModelObject[] bomChildrenArray = bomLine.get_bl_child_lines();

			// The number of children differs
			if (jsonChildrenList.size() > bomChildrenArray.length) {
				logger.info("The amount of children is different.");
				return null;
			}

			for (final ModelObject element : bomChildrenArray) {
				bomChildrenList.add((BOMLine) element);
			}

			// Iterate through all children objects from the JSON file
			for (final JSONObject json : jsonChildrenList) {
				final String transformationMatrix = MatrixManagement.calculateTransformationMatrix(
						json.optString(TcConstants.JSON_COORDINATES), json.optString(TcConstants.JSON_ROTATION),
						logger);
				bomlineFound = false;

//				logger.severe(String.format("DEBUG: Search a BOMLine for the JSON object %s.",
//						JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));

				// Iterate through all children objects from the BOMLine
				for (final BOMLine bomChild : bomChildrenList) {
					final String bomObjectType = bomChild.get_bl_item_object_type();

//					logger.severe(String.format("DEBUG: Check the BOMLine %s-%s.", bomChild.get_bl_item_current_id(),
//							bomChild.get_bl_item_object_name()));

					// checkChildren
					if (bomChild.get_bl_has_children() && !json.has(TcConstants.JSON_VARIANT_RULES)
							&& (!json.has(TcConstants.JSON_CHILDREN)
									|| JsonUtil.getJsonArray(json, TcConstants.JSON_CHILDREN).length() == 0)) {

						logger.info(String.format("The BOMLine has children, but it shouldn't have them."));
//						logger.severe(
//								String.format("DEBUG: The BOMLine %s-%s has children, but the JSON object %s does not.",
//										bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//										JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));

						continue;
					}

					// checkObjectType
					if (!bomObjectType.equals(json.opt(TcConstants.JSON_OBJECT_TYPE))) {
						logger.info(String.format("The object type isn't the same as from the current JSONObject."
								+ bomChild.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID)));
//						logger.severe(String.format(
//								"DEBUG: The object type of the BOMLine %s-%s is incorrect. It should be %s.",
//								bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//								JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_TYPE)));

						continue;
					}

					// checkReleaseStatus
					if (!hasCorrectStatus(bomChild, JsonUtil.getAttribute(json, TcConstants.JSON_RELEASE_STATUS))) {
						logger.info(String.format("The release status isn't the same as from the current JSONObject."
								+ bomChild.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID)));
						continue;
					}

					// checkQuantity
					// same -> checkPackCount
					final String jsonQuantity = json.optString(TcConstants.JSON_QUANTITY);
					final String bomQuantity = bomChild.get_bl_quantity();
					if ((bomQuantity.isBlank() && !jsonQuantity.isBlank() && Integer.parseInt(jsonQuantity) > 1)
							|| (jsonQuantity.isBlank() && !bomQuantity.isBlank() && Integer.parseInt(bomQuantity) > 1)
							|| (!bomQuantity.isBlank() && !jsonQuantity.isBlank()
									&& !bomQuantity.equals(jsonQuantity))) {

						logger.info(String.format("The quantity isn't the same as from the current JSONObject."));
						continue;
					}

					// checkTransformationMatrix
					if (!bomChild.get_bl_plmxml_occ_xform().equals(transformationMatrix)
							&& !(bomChild.get_bl_plmxml_occ_xform().isBlank()
									&& transformationMatrix.equals("1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1"))) {
//						logger.severe(String.format(
//								"DEBUG: The Matrix from the JSON: %s\nThe Matrix from the BOMLine %s-%s: %s",
//								transformationMatrix, bomChild.get_bl_item_current_id(),
//								bomChild.get_bl_item_object_name(), bomChild.get_bl_plmxml_occ_xform()));
						logger.info(String
								.format("The transformation matrix isn't the same as from the current JSONObject."));
						continue;
					}

					// TODO: Check if must be readded later on
//					// checkFindNo
//					if (!bomChild.get_bl_sequence_no().equals(json.opt(TcConstants.JSON_FIND_NO))) {
//						logger.info(String.format("The find no. isn't the same as from the current JSONObject."));
//						continue;
//					}

					// hasChildren
					// yes - checkChildrenCount
					// no - hasVariantRules
					// yes - checkVariantRules
					if (json.has(TcConstants.JSON_CHILDREN)
							&& JsonUtil.getJsonArray(json, TcConstants.JSON_CHILDREN).length() != 0) {

						// Compare the object group
						if (!hasCorrectObjectGroup((ItemRevision) bomChild.get_bl_revision(),
								JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_GROUP_ID))) {
							logger.info(
									String.format("The object group isn't the same as from the current JSONObject."));
							continue;
						}

						if (compareAllLevels) {
							final List<BOMLine> missingLines = compareChildren2(bomChild, JsonUtil.getListFromJsonArray(
									JsonUtil.getJsonArray(json, TcConstants.JSON_CHILDREN)), compareAllLevels);

							// Not all JSONObjects have a corresponding BOMLine
							if (missingLines == null || !missingLines.isEmpty()) {
								logger.info(
										String.format("Not all children JSONObjects have a corresponding BOMLine."));
								continue;
							}
						}
					} else if (json.has(TcConstants.JSON_VARIANT_RULES)) {
						// Compare the variant rules

						final BOMLine genericObject = getGenericSolutionVariantObject(bomChild);

						if (genericObject == null
								|| !genericObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID)
										.equals(JsonUtil.getAttribute(json, TcConstants.JSON_GENERIC_OBJECT_ID))) {
							logger.info(String
									.format("The generic object ID is not the same as from the current JSONObject."));
//							logger.severe(String.format(
//									"DEBUG: The generic object ID %s from the BOMLine %s-%s is not the same as in the JSON: %s.",
//									genericObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID),
//									bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//									JsonUtil.getAttribute(json, TcConstants.JSON_GENERIC_OBJECT_ID)));
							continue;
						}

						if (!variantRuleIsTheSame(bomChild, json)) {
							logger.info(
									String.format("The variant rules aren't the same as from the current JSONObject."));
							continue;
						}

						// Compare the solution variant category
//						if (!hasCorrectSolutionVarianCategory(bomChild, json)) {
//							logger.info(String.format(
//									"The solution variant category isn't the same as from the current JSONObject."));
//							continue;
//						}
					}

					// We have found a match
//					logger.severe(String.format("DEBUG: Found the BOMLine %s-%s for the JSON object %s.",
//							bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//							JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));
					bomChildrenList.remove(bomChild);
					bomlineFound = true;
					break;
				}

				if (!bomlineFound) {
					logger.info(String.format("Haven't found a BOMLine for the object %s.",
							JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));
				} else {
					logger.info(String.format("Found a BOMLine for the object %s.",
							JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));
				}
			}

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		} catch (final NullPointerException e) {
			logger.severe("At least one object was null. Can't finish the import.");

			e.printStackTrace();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return bomChildrenList;
	}

	public boolean compareChildren(BOMLine bomLine, StructureObject parentObject, boolean compareAllLevels)
			throws NotLoadedException {
		logger.info(String.format(
				"Compare the children from a BOMLine object with the corresponding list of children %s in the given JSON file. ",
				parentObject.getDisplayString()));
		final List<BOMLine> bomChildrenList = new ArrayList<>();
		boolean bomlineFound = false;

		try {

			final ModelObject[] bomChildrenArray = bomLine.get_bl_child_lines();

			// The number of children differs
			if (parentObject.getChildren().size() != bomChildrenArray.length
					&& !parentObject.getItemType().equals(TcConstants.TEAMCENTER_MODUL)) {
				logger.info("The amount of children is different.");
				return false;
			}

			// Check the object type
			if (!parentObject.getItemType().equals(bomLine.get_bl_item_object_type())) {
				logger.info("The object type is different.");
				return false;
			}

			// Check the object group id
			if (parentObject.getProperties().containsKey(TcConstants.JSON_OBJECT_GROUP_ID)
					&& !hasCorrectObjectGroup((ItemRevision) bomLine.get_bl_revision(),
							parentObject.getProperties().get(TcConstants.JSON_OBJECT_GROUP_ID))) {
				logger.info("The object group id is different.");
				return false;
			}

			for (final ModelObject element : bomChildrenArray) {
				bomChildrenList.add((BOMLine) element);
			}

			// Iterate through all children objects from the JSON file
			for (final StructureObject child : parentObject.getChildren()) {
				final JSONObject json = child.getOldJsonObject();
				final String transformationMatrix = MatrixManagement.calculateTransformationMatrix(
						json.optString(TcConstants.JSON_COORDINATES), json.optString(TcConstants.JSON_ROTATION),
						logger);
				bomlineFound = false;

//				logger.severe(String.format("DEBUG: Search a BOMLine for the JSON object %s.",
//						JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));

				// Iterate through all children objects from the BOMLine
				for (final BOMLine bomChild : bomChildrenList) {
					final String bomObjectType = bomChild.get_bl_item_object_type();

//					logger.severe(String.format("DEBUG: Check the BOMLine %s-%s.", bomChild.get_bl_item_current_id(),
//							bomChild.get_bl_item_object_name()));

					// checkChildren
					if (bomChild.get_bl_has_children() && !json.has(TcConstants.JSON_VARIANT_RULES)
							&& (!json.has(TcConstants.JSON_CHILDREN)
									|| JsonUtil.getJsonArray(json, TcConstants.JSON_CHILDREN).length() == 0)) {

						logger.info(String.format("The BOMLine has children, but it shouldn't have them."));
//						logger.severe(
//								String.format("DEBUG: The BOMLine %s-%s has children, but the JSON object %s does not.",
//										bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//										JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));

						continue;
					}

					// checkObjectType
					if (!bomObjectType.equals(json.opt(TcConstants.JSON_OBJECT_TYPE))) {
						logger.info(String.format("The object type isn't the same as from the current JSONObject."
								+ bomChild.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID)));
//						logger.severe(String.format(
//								"DEBUG: The object type of the BOMLine %s-%s is incorrect. It should be %s.",
//								bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//								JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_TYPE)));

						continue;
					}

					// checkReleaseStatus
					if (!hasCorrectStatus(bomChild, child.getReleaseStatus())) {
						logger.info(String.format("The release status isn't the same as from the current JSONObject."
								+ bomChild.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID)));
						continue;
					}

					// TODO: enable when property is updated
//					// isCabin?
//					// yes - checkProductVariantRule
//					if (bomObjectType.equals(TcConstants.TEAMCENTER_CABIN)
//							&& !productVariantRuleIsTheSame(bomChild, json)) {
//						logger.info(String.format(
//								"The product variant rule isn't the same as the variant rules from the current JSONObject."));
//						continue;
//					}

					// checkQuantity
					// same - checkPackCount
					final String jsonQuantity = json.optString(TcConstants.JSON_QUANTITY);
					final String bomQuantity = bomChild.get_bl_quantity();
					if ((bomQuantity.isBlank() && !jsonQuantity.isBlank() && Integer.parseInt(jsonQuantity) > 1)
							|| (jsonQuantity.isBlank() && !bomQuantity.isBlank() && Integer.parseInt(bomQuantity) > 1)
							|| (!bomQuantity.isBlank() && !jsonQuantity.isBlank()
									&& !bomQuantity.equals(jsonQuantity))) {

						logger.info(String.format("The quantity isn't the same as from the current JSONObject."));
						continue;
					}

					// checkTransformationMatrix
					if (!bomChild.get_bl_plmxml_occ_xform().equals(transformationMatrix)
							&& !(bomChild.get_bl_plmxml_occ_xform().isBlank()
									&& transformationMatrix.equals("1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1"))) {
//						logger.severe(String.format(
//								"DEBUG: The Matrix from the JSON: %s\nThe Matrix from the BOMLine %s-%s: %s",
//								transformationMatrix, bomChild.get_bl_item_current_id(),
//								bomChild.get_bl_item_object_name(), bomChild.get_bl_plmxml_occ_xform()));
						logger.info(String
								.format("The transformation matrix isn't the same as from the current JSONObject."));
						continue;
					}

					// TODO: Check if must be readded later on
//					// checkFindNo
//					if (!bomChild.get_bl_sequence_no().equals(json.opt(TcConstants.JSON_FIND_NO))) {
//						logger.info(String.format("The find no. isn't the same as from the current JSONObject."));
//						continue;
//					}

					// hasChildren
					// yes - checkChildrenCount
					// no - hasVariantRules
					// yes - checkVariantRules
					if (json.has(TcConstants.JSON_CHILDREN)
							&& JsonUtil.getJsonArray(json, TcConstants.JSON_CHILDREN).length() != 0) {

						// Compare the object group
						if (!hasCorrectObjectGroup((ItemRevision) bomChild.get_bl_revision(),
								JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_GROUP_ID))) {
							logger.info(
									String.format("The object group isn't the same as from the current JSONObject."));
							continue;
						}

						// Compare all levels of the structure recursively
						if (compareAllLevels) {
							if (!compareChildren(bomChild, child, compareAllLevels)) {
								logger.info(
										String.format("Not all children JSONObjects have a corresponding BOMLine."));
								continue;
							}
						}
					} else if (json.has(TcConstants.JSON_VARIANT_RULES)) {
						// Compare the variant rules

						BOMLine genericObject = bomLineCache.get(getDisplayString(bomChild));

						if (genericObject == null) {
							genericObject = getGenericSolutionVariantObject(bomChild);
							bomLineCache.put(getDisplayString(bomChild), genericObject);
						}
//						final BOMLine genericObject = getGenericSolutionVariantObject(bomChild);

//						dataManagementService.getProperties(new ModelObject[] { genericObject },
//								new String[] { TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID });
						if (genericObject == null
								|| !genericObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID)
										.equals(JsonUtil.getAttribute(json, TcConstants.JSON_GENERIC_OBJECT_ID))) {
							logger.info(String
									.format("The generic object ID is not the same as from the current JSONObject."));
//							logger.severe(String.format(
//									"DEBUG: The generic object ID %s from the BOMLine %s-%s is not the same as in the JSON: %s.",
//									genericObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID),
//									bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//									JsonUtil.getAttribute(json, TcConstants.JSON_GENERIC_OBJECT_ID)));
							continue;
						}

						if (!variantRuleIsTheSame(bomChild, json)) {
							logger.info(
									String.format("The variant rules aren't the same as from the current JSONObject."));
							continue;
						}

						// Compare the solution variant category
//						if (!hasCorrectSolutionVarianCategory(bomChild, json)) {
//							logger.info(String.format(
//									"The solution variant category isn't the same as from the current JSONObject."));
//							continue;
//						}
					}

					// We have found a match
//					logger.severe(String.format("DEBUG: Found the BOMLine %s-%s for the JSON object %s.",
//							bomChild.get_bl_item_current_id(), bomChild.get_bl_item_object_name(),
//							JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));
					bomChildrenList.remove(bomChild);
					bomlineFound = true;
					break;
				}

				if (!bomlineFound) {
					logger.info(String.format("Haven't found a BOMLine for the object %s.",
							JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));
					return false;
				} else {
					logger.info(String.format("Found a BOMLine for the object %s.",
							JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME)));
				}
			}

		} catch (final NotLoadedException | JSONException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}

		return bomlineFound;

	}

	public static List<JSONObject> findOccurrences(JSONArray jsonArray) throws JSONException {
		final List<JSONObject> occurrences = new ArrayList<>();
		final Map<String, Integer> objectCountMap = new HashMap<>();

		findOccurrencesRecursive(jsonArray, occurrences, objectCountMap);

		return occurrences;
	}

	private static void findOccurrencesRecursive(JSONArray jsonArray, List<JSONObject> occurrences,
			Map<String, Integer> objectCountMap) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject jsonObject = jsonArray.getJSONObject(i);
			removeProperties(jsonObject, "coordinates", "rotation"); // Remove properties from current object

			final JSONArray childrenArray = jsonObject.optJSONArray("children");
			if (childrenArray != null) {
				removeProperties(childrenArray, "coordinates", "rotation"); // Remove properties from children array
				findOccurrencesRecursive(childrenArray, occurrences, objectCountMap);
			}

			final String jsonString = jsonObject.toString();
			objectCountMap.put(jsonString, objectCountMap.getOrDefault(jsonString, 0) + 1);
			occurrences.add(jsonObject);
		}
	}

	private static void removeProperties(JSONObject jsonObject, String... propertyNames) {
		for (final String propertyName : propertyNames) {
			if (jsonObject.has(propertyName)) {
				jsonObject.remove(propertyName);
			}
		}
	}

	private static void removeProperties(JSONArray jsonArray, String... propertyNames) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject jsonObject = jsonArray.getJSONObject(i);
			removeProperties(jsonObject, propertyNames);
		}
	}

	/**
	 * Check if the given ItemRevision has an object group with the given ID.
	 * 
	 * @param bomChild
	 * @param json
	 * @return
	 */
	public boolean hasCorrectObjectGroup(ItemRevision itemRevision, String objectGroupID) {
		logger.info(
				String.format("Check if the object group from the current BOMLine is the same as in the JSON file."));

		try {

			// Get the current object group object from the given BOMLine
			final com.teamcenter.soa.client.model.Property objectGroupProperty = itemRevision
					.getPropertyObject(TcConstants.TEAMCENTER_OBJECT_GROUP);
			final ModelObject objectGroup = objectGroupProperty.getModelObjectValue();

			// Compare the given objectGroupID with the itemID from the object group object
			if (objectGroup == null) {
				logger.warning(
						String.format("The object %s does not have a object group.", getDisplayString(itemRevision)));
			} else if (!objectGroup.getPropertyDisplayableValue(TcConstants.TEAMCENTER_ITEM_ID).equals(objectGroupID)) {
				logger.warning(String.format("The object does not have the correct object group %s. It is %s.",
						objectGroupID, objectGroup.getPropertyDisplayableValue(TcConstants.TEAMCENTER_ITEM_ID)));
			} else {
				return true;
			}

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		} catch (final NullPointerException e) {
			logger.warning("The object " + getDisplayString(itemRevision) + " does not have an object group.");

			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Compare the BOMLine with the JSONObject and check the solution variant
	 * category is correct.
	 * 
	 * @param bomLine
	 * @param json
	 * @return
	 */
	private boolean hasCorrectSolutionVarianCategory(BOMLine bomLine, JSONObject json) {
		logger.info(String.format(
				"Compare the solution variant category between the current solution variant and the corresponding JSONObject."));

		try {
//			final ItemRevision itemRevision = (ItemRevision) ((Item) bomLine.get_bl_item()).get_item_revision();
			final int jsonCategory = getSolutionVariantCategoryFromString(
					json.optString(TcConstants.JSON_SOLUTION_VARIANT_CATEGORY));
			final int bomCategory = getSolutionVariantCategory(bomLine);

			// If the category from the BOMLine is Reuse
			if (bomCategory == 2) {

				if (jsonCategory == 2) {

					// We can reuse the BOMLine since both categories are Reuse
					return true;
				} else if (jsonCategory == -1) {
					final Item genericItem = searchObject(json.optString(TcConstants.JSON_OBJECT_TYPE),
							json.optString(TcConstants.JSON_GENERIC_OBJECT_ID));
					final BOMLine genericBomLine = getBomLine(genericItem, session);
//					dataManagementService.getProperties(new ModelObject[] { genericBomLine },
//							new String[] { TcConstants.TEAMCENTER_SOLUTION_VARIANT_CATEGORY });

					// Is the generic object category Reuse?
					return genericBomLine.getPropertyDisplayableValue(TcConstants.TEAMCENTER_SOLUTION_VARIANT_CATEGORY)
							.equalsIgnoreCase("Reuse");
				}
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Get the solution variant category of the given BOMLine.
	 * 
	 * @param solutionVariantBomLine
	 * @return
	 */
	private int getSolutionVariantCategory(BOMLine solutionVariantBomLine) {
		logger.info(
				"Get the solution variant category of the object " + getDisplayString(solutionVariantBomLine) + ".");
		try {
			final Item item = (Item) solutionVariantBomLine.get_bl_item();

//			dataManagementService.getProperties(new ModelObject[] { item },
//					new String[] { TcConstants.TEAMCENTER_REVISION_LIST });
			final ItemRevision itemRevision = (ItemRevision) item.get_revision_list()[0];

			// Create a list of all relations we need
			final RelationAndTypesFilter[] typeFilter = new RelationAndTypesFilter[1];
			typeFilter[0] = new RelationAndTypesFilter();
			typeFilter[0].relationTypeName = TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE;

			// Configure the relation preferences
			final ExpandGRMRelationsPref2 relationPref = new ExpandGRMRelationsPref2();
			relationPref.expItemRev = false;
			relationPref.info = typeFilter;
			relationPref.returnRelations = true;

			// Get the relations we wanted
			final ExpandGRMRelationsResponse2 response = dataManagementService
					.expandGRMRelationsForPrimary(new ModelObject[] { itemRevision }, relationPref);

			final ModelObject[] relationArray = new ModelObject[response.serviceData.sizeOfPlainObjects()];
			for (int i = 0; i < response.serviceData.sizeOfPlainObjects(); i++) {
				relationArray[i] = response.serviceData.getPlainObject(i);
			}

			for (final ModelObject object : relationArray) {

				// Select the solution variant source object
				if (object.getTypeObject().isInstanceOf(TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE)) {
					final ImanRelation relation = (ImanRelation) object;
					return Integer.parseInt(
							relation.getPropertyDisplayableValue(TcConstants.TEAMCENTER_SOLUTION_VARIANT_CATEGORY));
				}
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Get the solution variant category of the given BOMLine.
	 * 
	 * @param solutionVariantBomLine
	 * @return
	 */
	private BOMLine getGenericSolutionVariantObject(BOMLine solutionVariantBomLine) {
		logger.info("Get the generic object of the object " + getDisplayString(solutionVariantBomLine) + ".");
		try {
			final Item item = (Item) solutionVariantBomLine.get_bl_item();
			final ItemRevision itemRevision = (ItemRevision) item.get_revision_list()[0];

			// Create a list of all relations we need
			final RelationAndTypesFilter[] typeFilter = new RelationAndTypesFilter[1];
			typeFilter[0] = new RelationAndTypesFilter();
			typeFilter[0].relationTypeName = TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE;

			// Configure the relation preferences
			final ExpandGRMRelationsPref2 relationPref = new ExpandGRMRelationsPref2();
			relationPref.expItemRev = false;
			relationPref.info = typeFilter;
			relationPref.returnRelations = true;

			// Get the relations we wanted
			final ExpandGRMRelationsResponse2 response = dataManagementService
					.expandGRMRelationsForPrimary(new ModelObject[] { itemRevision }, relationPref);

			final ModelObject[] relationArray = new ModelObject[response.serviceData.sizeOfPlainObjects()];
			for (int i = 0; i < response.serviceData.sizeOfPlainObjects(); i++) {
				relationArray[i] = response.serviceData.getPlainObject(i);
			}

			for (final ModelObject object : relationArray) {

				// Select the solution variant source object
				if (object.getTypeObject().isInstanceOf(TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE)) {
					final ImanRelation relation = (ImanRelation) object;

					final ModelObject genericModelObject = relation.get_secondary_object();
					if (genericModelObject instanceof Item) {

						logger.info("Return the generic object Item.");
						return getBomLine((Item) genericModelObject, session);
					} else if (genericModelObject instanceof ItemRevision) {

						logger.info("Return the generic object Item Revision.");
						return getBomLine((ItemRevision) genericModelObject, session);
					} else if (genericModelObject instanceof BOMLine) {

						logger.info("Return the generic object BOMLine.");
						return (BOMLine) genericModelObject;
					}

				}
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
		return null;
	}

	private boolean variantRuleIsTheSame(BOMLine bomLine, JSONObject json) {
		logger.info(String.format(
				"Check if the variant rule of the current BOMLine is equal to the variant rules of the structure in the given JSONObject."));

		final Map<String, String> productVariantMap = new HashMap<>();
		Map<String, String> solutionVariantMap = new HashMap<>();

		try {
			final String bomProductVariantRule = bomLine
					.getPropertyDisplayableValue(TcConstants.TEAMCENTER_BOMLINE_VARIANT_RULE_TEXT);

			solutionVariantMap = JsonUtil
					.getSolutionVariantMapFromJsonArray(JsonUtil.getJsonArray(json, TcConstants.JSON_VARIANT_RULES));

			// The property should be filled if we have solution variants
			if (bomProductVariantRule.isBlank() && !solutionVariantMap.isEmpty()) {
				logger.info("The BOMLine has no variant rules.");
				return false;
			} else if (bomProductVariantRule.isBlank() && solutionVariantMap.isEmpty()) {
				logger.info("The BOMLine and the JSON have no variant rules.");

				return true;
			}

			final List<String> matches = new ArrayList<>();

			// Get all entries that are either a number, a number with leading chars or
			// 'None'
			final Pattern regex = Pattern.compile("[0-9]+|[a-zA-Z][a-zA-Z][0-9]+|(\\b(None)\\b)|\"(.*?)\"|'(.*?)'");
			final Matcher m = regex.matcher(bomProductVariantRule);
			while (m.find()) {
				matches.add(m.group());
			}

			// Remove all entries with chars that aren't 'None'
			matches.removeIf(s -> s.matches("[a-zA-Z][a-zA-Z][0-9]+"));

			for (int i = 0; i < matches.size(); i += 2) {
				if (matches.get(i + 1).equals("None") || matches.get(i + 1).contains("'")
						|| matches.get(i + 1).contains("\"")) {
					continue;
				}
				productVariantMap.put(matches.get(i), matches.get(i + 1));
			}

//			logger.severe(String.format(
//					"DEBUG: The variant rules from the BOMLine: %s\n\nThe variant rules from the JSON: %s",
//					productVariantMap, solutionVariantMap));

			if (solutionVariantMap.size() > productVariantMap.size()) {
				logger.info("The JSON object has more variant rules than the BOMLine.");
				return false;
			} else if (solutionVariantMap.size() < productVariantMap.size()) {
				logger.info("The BOMLine has more variant rules than the JSON object.");
				return false;
			}

			return solutionVariantMap.equals(productVariantMap);
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		} catch (final Exception e) {
			logger.severe(e.getMessage());
		}


		return false;
	}
}
