package com.parsa.middleware.service;

import com.cfg0.services.internal.loose.configurator.ConfiguratorManagementService;
import com.cfg0.services.internal.loose.configurator._2014_06.ConfiguratorManagement.CreateUpdateVariantRulesResponse;
import com.cfg0.services.internal.loose.configurator._2014_06.ConfiguratorManagement.VariantRuleInput;
import com.cfg0.services.internal.loose.configurator._2015_10.ConfiguratorManagement.*;
import com.cfg0.services.internal.loose.configurator._2017_11.ConfiguratorManagement.ConfigurationProfile;
import com.cfg0.services.internal.loose.configurator._2017_11.ConfiguratorManagement.ConfigurationSessionInfoInput;
import com.cfg0.services.internal.loose.configurator._2018_06.ConfiguratorManagement.FamilyInfo;
import com.cfg0.services.internal.loose.configurator._2018_06.ConfiguratorManagement.GetVariabilityResponse;
import com.cfg0.services.internal.loose.configurator._2018_06.ConfiguratorManagement.GroupInfo;
import com.cfg0.services.internal.loose.configurator._2018_06.ConfiguratorManagement.Variability;
import com.parsa.middleware.businessobjects.Family;
import com.parsa.middleware.businessobjects.SolutionVariant;
import com.parsa.middleware.businessobjects.StructureObject;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.ImportData;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.session.AppXSession;
import com.parsa.middleware.util.JsonUtil;
import com.smc0.services.loose.structuremanagement.SolutionVariantManagementService;
import com.smc0.services.loose.structuremanagement._2018_11.SolutionVariantManagement.*;
import com.teamcenter.services.internal.loose.structuremanagement.VariantManagementService;
import com.teamcenter.services.internal.loose.structuremanagement._2015_10.VariantManagement.VariantConfigurationCriteria;
import com.teamcenter.services.loose.core.DataManagementService;
import com.teamcenter.services.loose.core.ReservationService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.RevisionRule;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains all method that are used to create solution variants in Teamcenter.
 *
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class SolutionVariantManagement extends Utility {
	private final int VALIDATION_EXPAND = 1;
	private final int VALIDATION_VALIDATE = 16385;
	private final Logger logger;

	private final SearchManagement searchManagement;

	private final ImportData importData;
	private final AppXSession session;

	private RevisionRule revisionRule;

	private final List<Family> familyList = new ArrayList<>();

	private final Map<String, List<Family>> familyMap = new HashMap<>();
	private boolean triggerWorkFlowAndExit;

	/**
	 * Initiate the class with a logger.
	 *
	 * @param logger
	 */
	public SolutionVariantManagement(Logger logger, ClassificationManagement classificationManagement,
                                     SearchManagement searchManagement, ImportData importData, AppXSession currentSession) {
		this.logger = logger;
		this.searchManagement = searchManagement;
		this.importData = importData;
		session = currentSession;
	}

	/**
	 * Get the solution variant with the given attributes. At first search for an
	 * already existing solution variant in Teamcenter. If that didn't return
	 * anything, create a new solution variant.
	 *
	 * @param objectID                   The object ID from the generic object of
	 *                                   the solution variant
	 * @param objectType                 The object type of the solution variant
	 * @param familyFeatureMap           A map which contains all family : feature
	 *                                   entries
	 * @param solutionVariantCategoryMap A map whose entries are 'objectID' :
	 *                                   'solutionVariantCategory'
	 * @return Either a found or a a newly created solution variant. Or null if an
	 *         error occurred.
	 */
	public BOMLine getSolutionVariant(String objectID, String objectType, Map<String, String> familyFeatureMap,
			int solutionVariantCategory, Map<String, String> solutionVariantCategoryMap) {

		return null;
	}

	/**
	 * Create a new solution variant with the given attributes.
	 *
	 * @param genericObject              The generic object of the solution variant
	 * @param familyFeatureMap           A map which contains all family : feature
	 *                                   entries
	 * @param solutionVariantCategoryMap A map whose entries are 'objectID' :
	 *                                   'solutionVariantCategory'
	 * @return A new solution variant
	 */
	public BOMLine createSolutionVariant(BOMLine genericObject, Map<String, String> familyFeatureMap,
			Map<String, String> solutionVariantCategoryMap) {

		return null;
	}

	/**
	 * Search for already existing solution variants with the given parameters.
	 *
	 * @param genericItemID
	 * @param objectType
	 * @param variantRulesMap
	 * @return
	 */
	public void searchAndCreateSolutionVariant(SolutionVariant solutionVariant, String revisionRuleName) {
		logger.info(String.format(
				"Search for an existing solution variant from the generic object %s-%s. If none exists, create it.",
				solutionVariant.getGenericObjectID(),
				JsonUtil.getAttribute(solutionVariant.getJsonObject(), TcConstants.JSON_OBJECT_NAME)));

		final long startTime = System.currentTimeMillis();
//		final SearchManagement searchManagement = new SearchManagement(logger);
		try {

			final SolutionVariantManagementService solutionVariantManagement = SolutionVariantManagementService
					.getService(session.getConnection());
			final ConfiguratorManagementService configuratorManagement = ConfiguratorManagementService
					.getService(session.getConnection());

			final Item genericItem = searchManagement.searchObject(solutionVariant.getItemType(),
					solutionVariant.getGenericObjectID());

			if (genericItem == null) {
				logger.severe(String.format(
						"The generic object %s for the solution variant %s doesn't exist. Can't create the solution variant.",
						solutionVariant.getGenericObjectID(),
						solutionVariant.getJsonObject().optString(TcConstants.JSON_OBJECT_NAME)));
				return;
			}

			solutionVariant.setGenericBomLine(getBomLine(genericItem, session));
			BOMLine solutionVariantBomLine = null;

			ModelObject configuratorContext = null;

			logger.info(String.format("Get the configurator context from the BOMLine %s.",
					getDisplayString(solutionVariant.getGenericBomLine())));
			configuratorContext = getConfigPerspective(solutionVariant.getGenericBomLine());

			// Get the revision rule with the given name
			if (!revisionRuleName.isEmpty()) {
				revisionRule = searchManagement.searchRevisionRule(revisionRuleName);
			}

			// Get the default revision rule
			if (revisionRule == null) {
				logger.info("Get the default revision rule.");
				revisionRule = getRevisionRule(solutionVariant.getGenericBomLine(), logger);
			}

			// If the childItem doesn't has an own configurator context
			if (configuratorContext == null) {
				logger.severe(
						String.format("The object %s has no ConfiguratorContext. Can't create a Solution Variant.",
								solutionVariant.getGenericObjectID()));
				return;
			}

			// Set rule date
			if (importData.getRuleDate() != null) {
				final String ruleDate = importData.getRuleDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
				logger.info(String.format("Set the rule date to %s.", ruleDate));
				setProperty(TcConstants.TEAMCENTER_RULE_SET_COMPILE_DATE, ruleDate, configuratorContext, session);
			}

			// Check, if all necessary variant rules are given
			if (getFamilyList(configuratorContext, solutionVariant, genericItem) == null) {
				return;
			}

			// Get or create the variant rules.
			@SuppressWarnings("deprecation")
			final CreateUpdateVariantRulesResponse variantRulesResponse = configuratorManagement
					.createUpdateVariantRules(new VariantRuleInput[] { createSetVariantRuleInput() }, null);
			serviceDataError(variantRulesResponse.serviceData, logger);

			// Configuration for 'setConfigurationSessionInfo'
			final ServiceData configurationSessionInfoResponse = setConfigurationSessionInfo(configuratorManagement,
					configuratorContext, variantRulesResponse);
			if (serviceDataError(configurationSessionInfoResponse, logger)) {
				return;
			}

			// Configuration for 'setVariantExpression'
			final ServiceData variantExpressionResponse = setVariantExpression(solutionVariant.getVariantRules(),
					configuratorManagement, variantRulesResponse, configuratorContext);
			if (variantExpressionResponse == null || serviceDataError(variantExpressionResponse, logger)) {
				return;
			}

			// Decide if we at first search for an existing solution variant
//			if (solutionVariant.getSolutionVariantCategory() == 2 || (solutionVariant.getSolutionVariantCategory() == -1
//					&& !mustCreateSolutionVariant(solutionVariant.getChildrenVariantCategories()))) {
			final String sourceCategory = solutionVariant.getGenericBomLine()
					.getPropertyDisplayableValue(TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY);

			if (sourceCategory.equalsIgnoreCase("Reuse")) {
				solutionVariantBomLine = searchManagement.searchSolutionVariant(solutionVariantManagement,
						solutionVariant.getGenericBomLine(), variantRulesResponse, solutionVariant, this);
			} else {
				logger.info(String.format(
						"The solution variant category of the selected generic BOMLine is %s. Can't search for existing solution variants.",
						sourceCategory));
			}
			//trigger workflow and exit from here
			if(triggerWorkFlowAndExit && solutionVariantBomLine != null) {
				addWorkflow(solutionVariantBomLine, solutionVariant.getWorkflow(), logger, session);
				return;
			}
			// We found an existing solution variant
			if (solutionVariantBomLine != null) {
				solutionVariant.setBomLine(solutionVariantBomLine);
				solutionVariant.setWasCreated(true);

				final long endTime = System.currentTimeMillis();
				final long timeDiff = (endTime - startTime) / 1000;
				logger.log(Level.INFO, "Found the solution variant after " + timeDiff + " seconds.");

				return;
			}

			// Configuration for applyVariantConfiguration
			final ServiceData variantConfigurationResponse = applyVariantConfiguration(
					solutionVariant.getGenericBomLine(), variantRulesResponse);
			if (serviceDataError(variantConfigurationResponse, logger)) {
//				solutionVariantErrorMessage += childBomLine.toDisplayString() + ", ";

				return;
			}

			// Configuration for createMultilevelSolutionVariants
			solutionVariantBomLine = createMultilevelSolutionVariants(variantRulesResponse, solutionVariant);

			if (solutionVariantBomLine == null) {
				logger.log(Level.SEVERE, "Couldn't create the solution variant.");
//				solutionVariantErrorMessage += childBomLine.toDisplayString() + ", ";

				return;
			}

			addWorkflow(solutionVariantBomLine, solutionVariant.getWorkflow(), logger, session);

			solutionVariant.setBomLine(solutionVariantBomLine);
			solutionVariant.setWasCreated(true);

			solutionVariant.setNotFound(true);
			for (final StructureObject parent : solutionVariant.getParentList()) {
				parent.setNotFound(true);

			}

			final long endTime = System.currentTimeMillis();
			final long timeDiff = (endTime - startTime) / 1000;
			logger.info(String.format("Created the solution variant %s from the generic object %s in %d seconds.",
					getDisplayString(solutionVariantBomLine), solutionVariant.getGenericObjectID(), timeDiff));

			return;

		} catch (final NullPointerException e) {
			logger.log(Level.SEVERE, "A null object was given.\n\n" + e.getMessage(), e);

			e.printStackTrace();
		}
//		catch (final NotLoadedException e) {
//			logger.severe("A property of an object wasn't properly loaded.\n" + e.getMessage());
//
//			e.printStackTrace();
//		}
		catch (final Exception e) {
			logger.log(Level.SEVERE, "An unknown exception occurred.\n\n" + e.getMessage(), e);

			e.printStackTrace();
		}

		final long endTime = System.currentTimeMillis();
		final long timeDiff = (endTime - startTime) / 1000;
		logger.log(Level.INFO, "Couldn't find the solution variant after " + timeDiff + " seconds.");
		return;
	}

	/**
	 * Create the multilevel solution variant of the given generic BOMLine.
	 *
	 * @param genericParentBomLine
	 * @param parentBomLine
	 * @param bomLineLevel
	 * @param variantRulesResponse
	 */
	private BOMLine createMultilevelSolutionVariants(CreateUpdateVariantRulesResponse variantRulesResponse,
			SolutionVariant solutionVariant) {
		logger.info("Creating solution variants for the object " + solutionVariant.getGenericObjectID()
				+ " and its children objects.");

		final SolutionVariantManagementService solutionVariantManagementService = SolutionVariantManagementService
				.getService(session.getConnection());
		final ReservationService reservationService = ReservationService.getService(session.getConnection());

		BOMLine newSolutionVariant = null;
//		int solutionVariantCategory = solutionVariant.getSolutionVariantCategory();

		final Map<BOMLine, String> genericObjectsWithSolutionVariantCategory = new HashMap<>();

		try {
			// Create a list to save the generic objects of the current bomLineLevel
			List<BOMLine> genericObjectsList = new ArrayList<>();
			genericObjectsList.add(solutionVariant.getGenericBomLine());

			ModelObject[] genericObjectsArray = new ModelObject[0];

			// Create a second list to save the generic objects of the last bomLineLevel
			List<BOMLine> secondGenericObjectsList = new ArrayList<>();

			// Create a list to save the CreateMultilevelSVInput objects
			List<CreateMultilevelSVInput> multiLevelSVInputArray = new ArrayList<>();

			Map<ModelObject, String> mappedBomLines = new HashMap<>();

			// Set the configuration preferences
			final Map<String, String> configPreferences = new HashMap<>();
			configPreferences.put(TcConstants.TEAMCENTER_STOP_ON_ERROR, "0"); // 1|0 = True|False
			configPreferences.put(TcConstants.TEAMCENTER_DRY_RUN, "0"); // 1|0 = True|False
			configPreferences.put(TcConstants.TEAMCENTER_NUMBER_OF_LINES_TO_PROCESS, "0"); // 0|>0 = All lines|specific
			// number of
			// lines

			// For all bomLinelevels
			for (int bomLineLevel = 0;; bomLineLevel++) {
				secondGenericObjectsList = new ArrayList<>();
				multiLevelSVInputArray = new ArrayList<>();

				genericObjectsArray = new ModelObject[genericObjectsList.size()];
				int index = 0;
				for (final ModelObject object : genericObjectsList) {
					genericObjectsArray[index++] = object;
				}

//				dataManagementService.getProperties(genericObjectsArray,
//						new String[] { TcConstants.TEAMCENTER_BOMLINE_CHILDREN,
//								TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY,
//								TcConstants.TEAMCENTER_BOMLINE_OBJECT_TYPE });

				// for all generic objects on the current bomLineLevel
				for (final BOMLine genericBomLine : genericObjectsList) {
					logger.info("Create a solution variant from the generic object " + getDisplayString(genericBomLine)
							+ ".");

					// The solution variant category given from the JSON file
//					solutionVariantCategory = bomLineLevel == 0 ? solutionVariantCategory
//							: getSolutionVariantCategory(solutionVariant.getChildrenVariantCategories(),
//									genericBomLine);

					// The current solution variant category
					final int solutionVariantCategoryProperty = getSolutionVariantCategoryFromString(genericBomLine
							.getPropertyDisplayableValue(TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY));

					// An invalid category was given
//					if (solutionVariantCategory < -1) {
//						logger.severe("The given solution variant category " + solutionVariantCategory
//								+ " is invalid. Please give a correct solution variant category.");
//						return null;
//					}

					// No solution variant category on the generic object
					if (solutionVariantCategoryProperty == -1) {
						logger.info(
								"Can't create a solution variant of this object. The solution variant category is missing.");
						continue;
					}

					final CreateSVItemInfo svInfo = new CreateSVItemInfo();
					svInfo.svCategoryType = solutionVariantCategoryProperty;
					svInfo.createSVItemDesc.boName = genericBomLine.get_bl_item_object_type();

					final CreateSVItemInput svInput = new CreateSVItemInput();
					svInput.createSVItemInfo = svInfo;
					svInput.genericBOMLine = genericBomLine;

					final CreateMultilevelSVInput multilevelSVInput = new CreateMultilevelSVInput();
					multilevelSVInput.createSVItemInput = svInput;
					multilevelSVInput.bomLineLevel = bomLineLevel;

					// Change the solution variant category of the generic object and add the mapped
					// BOMLine UIDs
//					if (mappedBomLines.containsKey(genericBomLine) && solutionVariantCategory > -1) {

					if (mappedBomLines.containsKey(genericBomLine)) {

						// Save the old solution variant category
//						genericObjectsWithSolutionVariantCategory.put(genericBomLine,
//								Integer.toString(solutionVariantCategoryProperty));

						// Checkout the BOMLine to prevent possible changes
//						reservationService.checkout(new ModelObject[] { genericBomLine },
//								"Prevent the object from unwanted changes.", "");

						// Has a solution variant category given or is no Part
//						if (solutionVariantCategoryProperty > 0
//								|| !genericBomLine.get_bl_item_object_type().equals(TcConstants.TEAMCENTER_PART)) {
//
//							// Set the new solution variant category
//							// TODO: check if checkouts are called
//							setProperty(TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY,
//									String.valueOf(solutionVariantCategory), genericBomLine);
//						}

						multilevelSVInput.mappedSVBOMLineUID = mappedBomLines.get(genericBomLine);
					}

					// Save the SVInput and the generic objects
					multiLevelSVInputArray.add(multilevelSVInput);

					// For all children of genericBomLine
					for (final ModelObject object : genericBomLine.get_bl_child_lines()) {
						final BOMLine bomLine = (BOMLine) object;
						// Save the current bomLine
						secondGenericObjectsList.add(bomLine);
					}
				}

				// Create the correct list type for the CreateMultilevelSVInput
				final CreateMultilevelSVInput[] multiLevelSVInputList = new CreateMultilevelSVInput[multiLevelSVInputArray
						.size()];

				for (int i = 0; i < multiLevelSVInputList.length; i++) {
					multiLevelSVInputList[i] = multiLevelSVInputArray.get(i);
				}

				final CreateMultilevelSVConfigParam svConfigParam = new CreateMultilevelSVConfigParam();
				svConfigParam.pcaVariantRule = variantRulesResponse.ruleOutputs[0].ruleObject;
				svConfigParam.configPreferences = configPreferences;

				final CreateMultilevelSVResponse multilevelSVResponse = solutionVariantManagementService
						.createMultilevelSolutionVariants3(multiLevelSVInputList, svConfigParam);

				// Empty the map to use it in the next bomLineLevel
				mappedBomLines = new HashMap<>();

				// Save the first created BOMLine if no error occurred.
				if (!serviceDataError(multilevelSVResponse.serviceData, logger)) {
					mappedBomLines = multilevelSVResponse.expandedBOMLineMap;

					if (multilevelSVResponse.creatMultilevelSVOutputList.length > 0) {

						// Have we already saved the root element?
						if (newSolutionVariant == null) {
							newSolutionVariant = (BOMLine) multilevelSVResponse.creatMultilevelSVOutputList[0].newSVBOMLine;

							DataManagementService.getService(session.getConnection())
									.loadObjects(new String[] { newSolutionVariant.getUid() });

							logger.info(multilevelSVResponse.creatMultilevelSVOutputList[0].statusInfo);

						}
					}
				}
				// We need the current generic objects to create solution variants from their
				// children
				genericObjectsList = secondGenericObjectsList;

				// Stop, when no new generic object was found
				if (genericObjectsList.isEmpty()) {
					break;
				}
			}

		} catch (final NotLoadedException e) {
			logger.severe("At least one property couldn't get loaded.\n" + e.getMessage());

			e.printStackTrace();
		}

//		resetSolutionVariantCategory(genericObjectsWithSolutionVariantCategory);
		return newSolutionVariant;
	}

	/**
	 * Create a List of Family objects from the given variability array.
	 *
	 * @param variabilityArray An array consisting of all family and feature objects
	 *                         from a configurator context.
	 * @return
	 */
	private List<Family> createFamilyList(Variability[] variabilityArray) {
		logger.info("Create a list of all possible family objects.");

		final List<Family> familyList = new ArrayList<>();
		Family family;

		// Save all non-optional families in an array
		for (final Variability variability : variabilityArray) {

			// for all grouped family objects
			for (final GroupInfo groupInfo : variability.groupedVariability) {

				final WorkspaceObject group = (WorkspaceObject) groupInfo.familyGroup;

				for (final FamilyInfo familyInfo : groupInfo.families) {
					family = createFamilyObject(familyInfo, getDisplayString(group).split("/")[0]);

					if (family != null) {
						familyList.add(family);
					}
				}
			}

			// for all model family objects
			for (final FamilyInfo familyInfo : variability.models) {
				family = createFamilyObject(familyInfo, "");

				if (family != null) {
					familyList.add(family);
				}
			}

			// for all ungrouped family objects
			for (final FamilyInfo familyInfo : variability.ungroupedVariability) {
				family = createFamilyObject(familyInfo, "");

				if (family != null) {
					familyList.add(family);
				}
			}
		}

		return familyList;
	}

	/**
	 * Create a Family object from the given FamilyInfo and the groupName.
	 *
	 * @param familyInfo
	 * @param groupName
	 * @return
	 */
	private Family createFamilyObject(FamilyInfo familyInfo, String groupName) {
		logger.info(String.format("Create a new Family object for %s.", getDisplayString(familyInfo.family)));

		final ModelObject family = familyInfo.family;
		final Family newFamily = new Family(family, logger);
		newFamily.setGroupName(groupName);

		// Add all features
		for (final ModelObject featureObject : familyInfo.values) {
			newFamily.addFeature(featureObject);
		}

		return newFamily;
	}

	/**
	 * Gets all allowed family and feature objects for the object item and checks,
	 * if all non-optional families are given. Also checks, if all family-feature
	 * combinations in variantRules are applicable as a solution variant.
	 *
	 * @param configuratorContext
	 * @param variantRules
	 * @return True, if all given variant rules are usable and if all non-optional
	 *         families are given. False otherwise
	 */
	private boolean hasAllNecessarryVariantRulesGiven(List<Family> familyList, Map<String, String> variantRulesMap,
			Item item) {
		try {

			logger.info("Check for all given variant rules, if they are allowed to use on " + getDisplayString(item)
					+ " and if every non-optional variant rule is given.");

			Set<String> nonOptionalFamilyObjects = new HashSet<>();

			// Remove all given non-optional family-objects
			nonOptionalFamilyObjects = compareGivenNonOptionalFamilies(familyList, variantRulesMap);

			// If still some family-objects are left in the array
			if (!nonOptionalFamilyObjects.isEmpty()) {

				// Add all missing non-optional families to a string
				String missingRules = "";
				final Iterator<String> iter = nonOptionalFamilyObjects.iterator();
				while (iter.hasNext()) {
					missingRules += iter.next() + ", ";
				}

				logger.info("The missing non-optional variant rules for the object " + getDisplayString(item)
						+ " are:\n" + missingRules.substring(0, missingRules.lastIndexOf(",")));
				return false;
			}

			// Check, if all family-feature combinations are allowed for this object
			if (hasValidVariantRules(variantRulesMap, familyList) && nonOptionalFamilyObjects.isEmpty()) {
				logger.info("The object " + getDisplayString(item)
						+ " has all non-optional variant rules and only allowed optional variant rules given.");
				return true;
			}

			logger.info("Not all given family-feature combinations are allowed.");
		} catch (final NullPointerException e) {
			logger.log(Level.SEVERE, "The Configurator Context is null.\n\n" + e.getMessage(), e);

			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Removes all non-optional family-objects given in variantRules from the list
	 * nonOptionalFamilyObjects.
	 *
	 * @param nonOptionalFamilyObjects
	 * @param variantRules
	 * @return The - in the best case - empty list non-optional Family Objects
	 */
	private Set<String> compareGivenNonOptionalFamilies(List<Family> familyList, Map<String, String> variantRules) {
		logger.info("Check if all non-optional variant rules are given.");

		final Set<String> variantRuleFamilies = new HashSet<>(variantRules.keySet());
		String familyID = "";

		// for every family object
		for (final Family family : familyList) {

			familyID = family.getFamilyID();
			if (variantRules.containsKey(familyID)) {

				// The family is given as variant rule
				variantRuleFamilies.remove(familyID);
			} else if (family.isOptional()) {

				// The family is not given as variant rule and optional
				variantRuleFamilies.remove(familyID);
			}
		}

		return variantRuleFamilies;
	}

	/**
	 * Set the parameters for the VariantRuleInput object
	 *
	 * @return
	 */
	private VariantRuleInput createSetVariantRuleInput() {
		logger.info("Set the parameters for the VariantruleInput object.");

		// Create the CreateInput object for the variant rule object
		final com.cfg0.services.internal.loose.configurator._2014_06.ConfiguratorManagement.CreateInput createInput = new com.cfg0.services.internal.loose.configurator._2014_06.ConfiguratorManagement.CreateInput();
		createInput.boName = "VariantRule";
		createInput.stringProps.put(TcConstants.TEAMCENTER_OBJECT_NAME, "Custom Configuration");
		createInput.stringProps.put(TcConstants.TEAMCENTER_OBJECT_DESCRIPTION, "");

		// Create the VariantRuleInput
		final VariantRuleInput variantRuleInput = new VariantRuleInput();
		variantRuleInput.creInputs = createInput;
		variantRuleInput.relationName = "";
		variantRuleInput.saveRule = false;

		return variantRuleInput;
	}

	/**
	 * Set the configuration session info for the solution variant.
	 *
	 * @param configuratorManagement
	 * @param parentBomLine
	 * @param variantRulesResponse
	 */
	private ServiceData setConfigurationSessionInfo(ConfiguratorManagementService configuratorManagement,
			ModelObject configuratorContext, CreateUpdateVariantRulesResponse variantRulesResponse) {
		logger.info("Set the configuration session info.");

		// Create the ConfigurationProfile with hardcoded parameters
		final ConfigurationProfile configurationProfile = new ConfigurationProfile();
		configurationProfile.stringMap.put("CFG_PROFILE_validateWithoutExpand", new String[] { "true" });
		configurationProfile.stringMap.put("CFG_PROFILE_minErrorSeverity", new String[] { "4" });
		configurationProfile.stringMap.put("CFG_PROFILE_applyConfigConstraints", new String[] { "true" });
		configurationProfile.stringMap.put("CFG_PROFILE_violationComputationTimeout", new String[] { "4" });
		configurationProfile.stringMap.put("CFG_PROFILE_applyDefaults", new String[] { "true" });
		configurationProfile.stringMap.put("CFG_PROFILE_preferEmptyValueForDiscretionary", new String[] { "true" });
		configurationProfile.stringMap.put("CFG_PROFILE_reportViolationForMutuallyExclusiveConstraint",
				new String[] { "true" });
		configurationProfile.stringMap.put("CFG_PROFILE_considerLowSeverityRulesForExpansion", new String[] { "true" });
		configurationProfile.stringMap.put("CFG_PROFILE_ruleDateTranslation", new String[] { "1" });
		configurationProfile.stringMap.put("CFG_PROFILE_minReportSeverity", new String[] { "4" });

		// Create the ConfigurationSessionInfoInput
		final ConfigurationSessionInfoInput sessionInfo = new ConfigurationSessionInfoInput();
		sessionInfo.configProfile = configurationProfile;
		sessionInfo.perspective = configuratorContext;
		sessionInfo.targetObjects = new ModelObject[] { variantRulesResponse.ruleOutputs[0].ruleObject,
				configuratorContext };

		// Set the ConfigurationSessionInfo and return its ServiceData object
		final ServiceData configurationSessionInfoResponse = configuratorManagement
				.setConfigurationSessionInfo(new ConfigurationSessionInfoInput[] { sessionInfo });
		return configurationSessionInfoResponse;
	}

	/**
	 * Set the variant expressions for the solution variants. These contain
	 * information of the variant rules.
	 *
	 * @param variantRules
	 * @param configuratorManagement
	 * @param defRule
	 * @param variantRulesResponse
	 */
	private ServiceData setVariantExpression(Map<String, String> variantRules,
			ConfiguratorManagementService configuratorManagement, CreateUpdateVariantRulesResponse variantRulesResponse,
			ModelObject configuratorContext) {
		logger.info("Set the variant expression with the given variant rules.");

		// Create an array of ApplicationConfigExpression
		final ApplicationConfigExpression[] applicationConfigExpression = new ApplicationConfigExpression[2];
		applicationConfigExpression[0] = createSetVariantExpressionInput(variantRules, 18, configuratorContext);
		applicationConfigExpression[1] = createSetVariantExpressionInput(variantRules, 48, configuratorContext);

		// Create the BusinessObjectConfigExpression with 2 ApplicationConfigExpressions
		final BusinessObjectConfigExpression businessObjectConfigExpression = new BusinessObjectConfigExpression();
		businessObjectConfigExpression.clientId = "";
		businessObjectConfigExpression.targetObject = variantRulesResponse.ruleOutputs[0].ruleObject;
		businessObjectConfigExpression.expressions = applicationConfigExpression;

		// Create the SetVariantExpressionInput with the previously created
		// BusinessObjectConfigExpression
		final SetVariantExpressionInput variantExpressionInput = new SetVariantExpressionInput();
		variantExpressionInput.saveExpressions = false; // Expressions will only be used for this
		// object and deleted afterwards
		variantExpressionInput.businessObjectExpressions = new BusinessObjectConfigExpression[] {
				businessObjectConfigExpression };

		// save the upper expression type in case it shouldn't be changed
		final int saveExpressionType = applicationConfigExpression[0].expressionType;

		// Return null if the configuration isn't valid
		if (!validateConfiguration(configuratorManagement, applicationConfigExpression[0], configuratorContext,
				businessObjectConfigExpression)) {
			logger.log(Level.SEVERE, "Couldn't validate the variant rule configuration.");
			return null;
		}

		// Reset the upper expression type to its original value
		applicationConfigExpression[0].expressionType = saveExpressionType;
		final ServiceData variantExpressionResponse = configuratorManagement
				.setVariantExpressions(new SetVariantExpressionInput[] { variantExpressionInput }, revisionRule);
		if (!serviceDataError(variantExpressionResponse, logger)) {
			return variantExpressionResponse;
		}

		return null;
	}

	/**
	 * Validate the given configuration for the solution variant.
	 *
	 * @param configuratorManagement
	 * @param applicationConfigExpression
	 * @param configuratorContext
	 * @return
	 */
	private boolean validateConfiguration(ConfiguratorManagementService configuratorManagement,
			ApplicationConfigExpression applicationConfigExpression, ModelObject configuratorContext,
			BusinessObjectConfigExpression businessObjectConfigExpression) {
		logger.info("Validate the given variant rule configuration");

		// The upper expression type must be 0
		applicationConfigExpression.expressionType = 0;

		final BusinessObjectConfigExpression configExpression = new BusinessObjectConfigExpression();
		configExpression.expressions = new ApplicationConfigExpression[] { applicationConfigExpression };
//		configExpression.targetObject = businessObjectConfigExpression.targetObject;
		configExpression.clientId = businessObjectConfigExpression.targetObject.getUid();

		final ValidateProductConfigInput[] configInput = new ValidateProductConfigInput[1];
		configInput[0] = new ValidateProductConfigInput();
		configInput[0].applyConstraints = 466945; // What's that magic number?
		configInput[0].context = configuratorContext;
		configInput[0].expressionsToValidate = new BusinessObjectConfigExpression[] { configExpression };

		final ValidateProductConfigurationResponse response = configuratorManagement
				.validateProductConfiguration(configInput);

		try {
			if (serviceDataError(response.serviceData, logger)) {
				return false;
			}

			if (response.outputs[0].criteriaStatus.equals(TcConstants.TEAMCENTER_VALID_AND_COMPLETE)) {
				logger.info("The solution variant configuration is valid.");
				return true;
			} else {

				// The configuration is not valid
				if (response.outputs[0].violations.length > 0) {

					// The family/feature selection is not valid
					switch (response.outputs[0].criteriaStatus) {
					case TcConstants.TEAMCENTER_VALID_AND_INCOMPLETE:
						logger.info(
								"The solution variant configuration is valid but incomplete. Some rules are missing.\n"
										+ response.outputs[0].violations[0].message);
						break;
					default:
						logger.info("The solution variant configuration is neither valid nor complete.\n"
								+ response.outputs[0].violations[0].message);
					}
				} else {

					// The rules are not followed
					final AvailableProductVariabilityInput input = new AvailableProductVariabilityInput();
					input.applyConstraints = 8705;
					input.criteriaExpression = configExpression;
					input.context = configuratorContext;
					input.familiesToTest = getFamilyUIDs(applicationConfigExpression);

					final AvailableProductVariabilityOutput response2 = configuratorManagement
							.getAvailableProductVariability(input);

					if (serviceDataError(response2.serviceData, logger)) {
						return false;
					}

					// Set the logger output depending on the validation
					switch (response2.criteriaStatus) {
					case TcConstants.TEAMCENTER_VALID_AND_COMPLETE:
						logger.info("The solution variant configuration is valid.");
						applicationConfigExpression = response2.suggestedSatisfyingExpr;

						return true;
					case TcConstants.TEAMCENTER_VALID_AND_INCOMPLETE:
						logger.info(
								"The solution variant configuration is valid but incomplete. Some rules are missing.\n");
						break;
					default:
						logger.info("The solution variant configuration is neither valid nor complete.\n");
					}
				}
			}
		} catch (final NullPointerException e) {
			logger.log(Level.SEVERE, "An object isn't accessible.\n\n" + e.getMessage());

			e.printStackTrace();
		} catch (final IndexOutOfBoundsException e) {
			logger.log(Level.SEVERE,
					"There was a problem while validating the solution variant configuration. Couldn't get a violation message.\n\n"
							+ e.getMessage());
			e.printStackTrace();

		}
		return false;
	}

	private ModelObject[] getFamilyUIDs(ApplicationConfigExpression applicationConfigExpression) {
		final List<ModelObject> familyList = new ArrayList<>();

		for (final ConfigExpressionSet configSet : applicationConfigExpression.configExprSets) {
			for (final ConfigExpression configExpression : configSet.configExpressions) {
				for (final ConfigSubExpression subExpression : configExpression.subExpressions) {
					for (final ConfigExpressionGroup expressionGroup : subExpression.expressionGroups) {
						for (final ConfigExpressionTerm term : expressionGroup.terms) {

							if (term.family != null) {
								familyList.add(term.family);
							}
						}
					}
				}
			}
		}

//		logger.severe(String.format("DEBUG: Family list size: %d", familyList.size()));

		return familyList.toArray(new ModelObject[familyList.size()]);
	}

	/**
	 * Configured the configurator expressions for the variant expression.
	 *
	 * @param variantRules   A JSONArray of Variant Rule configurations
	 * @param expressionType The value for the used expression type
	 * @return Returns the configurator expression for the variant expression input
	 */
	private ApplicationConfigExpression createSetVariantExpressionInput(Map<String, String> variantRules,
			int expressionType, ModelObject configuratorContext) {
		logger.info("Configure the configurator expressions for the variant expression.");

		final List<ConfigExpressionGroup> configExpressionGroupArray = createConfigExpressionGroupArray(variantRules,
				configuratorContext);

		// Transform the array to a list
		final ConfigExpressionGroup[] configExpressionGroupList = new ConfigExpressionGroup[configExpressionGroupArray
				.size()];
		for (int j = 0; j < configExpressionGroupList.length; j++) {
			configExpressionGroupList[j] = configExpressionGroupArray.get(j);
		}

		// Create the ConfigSubExpression with the previously created
		// ConfigExpressionGroup
		final ConfigSubExpression configSubExpression = new ConfigSubExpression();
		configSubExpression.expressionGroups = configExpressionGroupList;

		// Create the ConfigExpression
		final ConfigExpression configExpression = new ConfigExpression();
		configExpression.subExpressions = new ConfigSubExpression[] { configSubExpression };
		configExpression.clientID = "";
		configExpression.expressionType = expressionType;
		configExpression.formula = "";

		// Create the ConfigExpressionSet
		final ConfigExpressionSet expressionSet = new ConfigExpressionSet();
		expressionSet.configExpressions = new ConfigExpression[] { configExpression };

		// Create the ApplicationConfigExpression
		final ApplicationConfigExpression applicationConfigExpression = new ApplicationConfigExpression();
		applicationConfigExpression.configExprSets = new ConfigExpressionSet[] { expressionSet };
		applicationConfigExpression.expressionType = expressionType;
		applicationConfigExpression.exprID = "";
		applicationConfigExpression.formula = "";

		return applicationConfigExpression;
	}

	/**
	 * Iterate through the variantRules and create the ConfigExpressionTerms with
	 * them.
	 *
	 * @param variantRules A JSONArray of Variant Rule configurations
	 * @return A List of ConfigExpressionGroup elements
	 */
	private List<ConfigExpressionGroup> createConfigExpressionGroupArray(Map<String, String> variantRules,
			ModelObject configuratorContext) {
		logger.info("Iterate through the variant rules and create the ConfigExpressionTerms.");

		final List<ConfigExpressionGroup> configExpressionGroupArray = new ArrayList<>();
		final List<Family> addedFamilies = new ArrayList<>();
		try {
			final String objectString = configuratorContext
					.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_STRING).split(" ")[0];

			getExpressionGroupFromVariantRules(variantRules, objectString, configExpressionGroupArray, addedFamilies);
			getExpressionGroupFromFamilies(objectString, configExpressionGroupArray, addedFamilies);

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
		return configExpressionGroupArray;

	}

	/**
	 * Get the ConfigExpressionGroup List which contains all variant rules for the
	 * solution variant that aren't used.
	 *
	 * @param familyList
	 * @param configuratorContext
	 * @param configExpressionGroupArray
	 * @param addedFamilies
	 */
	private void getExpressionGroupFromFamilies(String configuratorContextID,
			final List<ConfigExpressionGroup> configExpressionGroupArray, final List<Family> addedFamilies) {
		logger.info("Get the ExpressionGroups with the ExpressionTerms which don't have a variant rule.");

		String familyID;
		String valueID;

		try {
			// Add all families without features
			for (final Family family : familyMap.get(configuratorContextID)) {
				familyID = "";
				valueID = "";

				final ConfigExpressionTerm configExpressionTerm = new ConfigExpressionTerm();
				final ConfigExpressionGroup configExpressionGroup = new ConfigExpressionGroup();

				// if the family objects wasn't used yet
				if (!addedFamilies.contains(family)) {

					final ModelObject currentFamily = family.getFamily();
//					dataManagementService.getProperties(new ModelObject[] { currentFamily },
//							new String[] { TcConstants.TEAMCENTER_OBJECT_TYPE });

					// we only need the family ID
					familyID = family.getFamilyID();
					valueID = "";

					// Is it a dynamic family?
					if (currentFamily.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_TYPE)
							.equals(TcConstants.TEAMCENTER_FEATURE_FAMILY)) {
						configExpressionGroup.context = currentFamily;
					}

					if (currentFamily.getPropertyObject(TcConstants.TEAMCENTER_OBJECT_TYPE).getStringValue()
							.equals(TcConstants.TEAMCENTER_PRODUCT_FAMILY)) {

						configExpressionTerm.familyNamespace = configuratorContextID;
					} else {
						configExpressionTerm.familyNamespace = "Teamcenter";
					}

					// Create a ConfigExpressionTerm
					configExpressionTerm.family = currentFamily;
					configExpressionTerm.familyID = familyID;
					configExpressionTerm.operatorCode = 5; // PS_variant_operator_is_equal
					configExpressionTerm.selectionClass = "User";
					configExpressionTerm.valueText = valueID;

					// Create a ConfigExpressionGroup and add the ConfigExpressionTerm to it
					configExpressionGroup.terms = new ConfigExpressionTerm[] { configExpressionTerm };
					configExpressionGroup.groupName = family.getFamilyGroupName();

					configExpressionGroupArray.add(configExpressionGroup);
				}
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
	}

	/**
	 * Get the ConfigExpressionGroup List which contains all variant rules for the
	 * solution variant.
	 *
	 * @param familyList
	 * @param variantRules
	 * @param configuratorContext
	 * @param configExpressionGroupArray
	 * @param addedFamilies
	 */
	private void getExpressionGroupFromVariantRules(Map<String, String> variantRules, String configuratorContextID,
			final List<ConfigExpressionGroup> configExpressionGroupArray, final List<Family> addedFamilies) {
		logger.info("Get the ExpressionGroups with the ExpressionTerms from the given variant rules.");

		WorkspaceObject valueObject;
		String familyID;
		String valueID;

		try {
			// for every family object
			for (final Family family : familyMap.get(configuratorContextID)) {
				valueObject = null;
				familyID = "";
				valueID = "";

				final ConfigExpressionTerm configExpressionTerm = new ConfigExpressionTerm();
				final ConfigExpressionGroup configExpressionGroup = new ConfigExpressionGroup();

				// if the correct family object is found
				if (variantRules.containsKey(family.getFamilyID())) {
					final String currentFamilyID = family.getFamilyID();
					final String currentFeatureID = variantRules.get(family.getFamilyID());
					if (!family.hasFreeFormValues() && !family.getFeatureMap().containsKey(currentFeatureID)) {
						logger.severe(String.format("The feature %s is not valid for the family %s.", currentFeatureID,
								currentFamilyID));
						return;
					}

					final ModelObject currentFamily = family.getFamily();
					final WorkspaceObject currentFeature = (WorkspaceObject) family.getFeatureMap()
							.get(currentFeatureID);

					addedFamilies.add(family);
					valueID = currentFeatureID;

//					dataManagementService.getProperties(new ModelObject[] { currentFamily, currentFeature },
//							new String[] { TcConstants.TEAMCENTER_OBJECT_TYPE });

					if (family.hasFreeFormValues()) {
						familyID = currentFamilyID;
						configExpressionTerm.family = currentFamily;

						configExpressionGroup.context = null;
					} else if (currentFeature.get_object_type().equals(TcConstants.TEAMCENTER_FEATURE)) {
						// If it's a dynamic family we only need the feature ID
						familyID = currentFeatureID;

						configExpressionGroup.context = currentFamily;
						configExpressionTerm.family = currentFeature;
					} else {
						familyID = currentFamilyID;

						configExpressionTerm.family = currentFamily;
						valueObject = currentFeature;

						configExpressionGroup.context = null;
						configExpressionTerm.value = valueObject;
					}

					if (currentFamily.getPropertyObject(TcConstants.TEAMCENTER_OBJECT_TYPE).getStringValue()
							.equals(TcConstants.TEAMCENTER_PRODUCT_FAMILY)) {

						configExpressionTerm.familyNamespace = configuratorContextID;
					} else {
						configExpressionTerm.familyNamespace = "Teamcenter";
					}

					// Create a ConfigExpressionTerm
//					configExpressionTerm.family = familyObject;
					configExpressionTerm.familyID = familyID;
					configExpressionTerm.operatorCode = 5; // PS_variant_operator_is_equal
					configExpressionTerm.selectionClass = "User";
					configExpressionTerm.valueText = valueID;

					// Create a ConfigExpressionGroup and add the ConfigExpressionTerm to it
					configExpressionGroup.terms = new ConfigExpressionTerm[] { configExpressionTerm };
					configExpressionGroup.groupName = family.getFamilyGroupName();

					configExpressionGroupArray.add(configExpressionGroup);
				}
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
	}

	/**
	 * Apply the variant configuration on the generic BOMLine.
	 *
	 * @param genericBomLine
	 * @param variantRulesResponse
	 */
	private ServiceData applyVariantConfiguration(BOMLine genericBomLine,
			CreateUpdateVariantRulesResponse variantRulesResponse) {
		logger.info(String.format("Apply the variant configuration on the generic BOMLine %s.",
				getDisplayString(genericBomLine)));

		final VariantConfigurationCriteria variantConfigurationCriteria = new VariantConfigurationCriteria();
		variantConfigurationCriteria.savedVariantRules = new ModelObject[] {
				variantRulesResponse.ruleOutputs[0].ruleObject };
		variantConfigurationCriteria.variantConfiguratorFormula = "";

		final VariantManagementService variantManagement = VariantManagementService.getService(session.getConnection());

		final ServiceData applyVariantConfigResponse = variantManagement
				.applyVariantConfiguration(getBOMWindow(genericBomLine), variantConfigurationCriteria);
		return applyVariantConfigResponse;
	}

	/**
	 * Checks, if all variant rule combinations from variantRules are allowed.
	 *
	 * @param variantRules
	 * @param allowedVariantRules
	 * @return True, if allowedVariantRules contains all variant rules from
	 *         variantRules
	 */
	private boolean hasValidVariantRules(Map<String, String> variantRules, List<Family> familyList) {
		logger.info("Check for all family objects if the given variant rules are valid.");

		for (final Family family : familyList) {

			// if the map contains the family
			if (variantRules.containsKey(family.getFamilyID())) {

				if (!family.getFeatureMap().containsKey(variantRules.get(family.getFamilyID()))
						&& !family.hasFreeFormValues()) {
					// Either the featureID doesn't exist on the family or it is empty but not a
					// free form value

					logger.severe(String.format(
							"Either the feature %s does not exist on the family %s or the feature value in the JSON file is empty,"
									+ " but the family does not support free form values.",
							variantRules.get(family.getFamilyID()), family.getFamilyID()));
					return false;
				}
			}
		}

		for (final String familyID : variantRules.keySet()) {

			boolean bool = false;
			for (final Family family : familyList) {
				if (family.getFamilyID().equals(familyID)) {
					bool = true;
					break;
				}
			}

			if (!bool) {
				// There exists no family with the current family ID
				return false;
			}
		}

		return true;
	}

	/**
	 * Check the given map if it contains a category for the generic BOMLine. If
	 * not, get the category from the BOMLine itself.
	 *
	 * @param childrenSolutionVariants
	 * @param genericBomLine
	 * @return
	 */
	private int getSolutionVariantCategory(Map<String, String> childrenSolutionVariants, BOMLine genericBomLine) {
		logger.info(
				String.format("Check the given map if it contains a category for the generic BOMLine %s and return it.",
						getDisplayString(genericBomLine)));
		String objectID = "";
		try {

//			dataManagementService.getProperties(new ModelObject[] { genericBomLine },
//					new String[] { TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID,
//							TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY });
			objectID = genericBomLine.get_bl_item_current_id();

			// Do we have the category given in the map?
			if (childrenSolutionVariants.containsKey(objectID)) {
				return Integer.parseInt(childrenSolutionVariants.get(objectID));
			}

			// Get the category from the BOMLine
			return getSolutionVariantCategoryFromString(genericBomLine
					.getPropertyDisplayableValue(TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY));
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		} catch (final NumberFormatException e) {
			logger.severe(String.format("The children solution variant from the object %s is not a valid integer.",
					objectID));

			e.printStackTrace();
		}

		return -1;
	}

	/**
	 * Reset the solution variant category of all objects
	 *
	 * @param genericObjectsWithSolutionVariantCategory
	 */
	private void resetSolutionVariantCategory(Map<BOMLine, String> genericObjectsWithSolutionVariantCategory) {
		logger.info("Reset all solution variant categories to its original state, if they were changed.");

		final ReservationService reservationService = ReservationService.getService(session.getConnection());
		final ModelObject[] bomLines = new ModelObject[genericObjectsWithSolutionVariantCategory.size()];
		int index = 0;

		for (final BOMLine bomLine : genericObjectsWithSolutionVariantCategory.keySet()) {
			final int solutionVariantCategory = Integer
					.parseInt(genericObjectsWithSolutionVariantCategory.get(bomLine));
			setProperty(TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY,
					solutionVariantCategory == -1 ? "" : Integer.toString(solutionVariantCategory), bomLine, session);
			bomLines[index++] = bomLine;
		}

		reservationService.checkin(bomLines);
	}

	/**
	 * Check all child solution variants if one of them has the solution variant
	 * category "Managed" or "Unmanaged" and therefore must be created.
	 *
	 * @param childSolutionVariants A Map with the solution variant category
	 *                              corresponding to each object ID
	 * @return True, if at least one solution variant category is "Managed" or
	 *         "Unmanaged". False otherwise.
	 */
	private boolean mustCreateSolutionVariant(Map<String, String> childSolutionVariants) {
		logger.info(
				"Check all child solution variants if one of them has the solution variant category \"Managed\" or \"Unmanaged\" and therefore must be created.");

		for (final String objectID : childSolutionVariants.keySet()) {
			if (getSolutionVariantCategoryFromString(childSolutionVariants.get(objectID)) < 2) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check all family and features from the solution variants if already exist in
	 * the list return them or else query to the TC and store them in the list
	 *
	 */
	private List<Family> getFamilyList(ModelObject configuratorContext, SolutionVariant solutionVariant,
			Item genericItem) {
		logger.info(
				"Check if all families and features from the current Configurator Context are given and return them or get them from Teamcenter.");

		final List<Family> resultFamilyList = new ArrayList<>();
		final Map<String, String> searchMap = new HashMap<>();

		String objectString;
		try {
			objectString = configuratorContext.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_STRING)
					.split(" ")[0];

			if (familyMap.containsKey(objectString)) {

				// Search if family id exist in the custom familyList
				for (final String key : solutionVariant.getVariantRules().keySet()) {
					boolean keyExists = false;

					for (final Family family : familyMap.get(objectString)) {
						if (key.equals(family.getFamilyID())) {
							keyExists = true;
							resultFamilyList.add(family);
							break;
						}
					}

					if (!keyExists) {
						searchMap.put(key, solutionVariant.getVariantRules().get(key));
					}
				}
			} else {
				familyMap.put(objectString, new ArrayList<>());
			}

			if (searchMap.size() > 0 || familyMap.get(objectString).isEmpty()) {
				logger.info(
						String.format("Get the families and features from the Configurator Context %s from Teamcenter.",
								objectString));
				// Check, if all necessary variant rules are given
				final ConfiguratorManagementService configuratorManagement = ConfiguratorManagementService
						.getService(session.getConnection());
				final GetVariabilityResponse variabilityResponse = configuratorManagement
						.getVariability(new ModelObject[] { configuratorContext });

				// Was there an error?
				if (serviceDataError(variabilityResponse.serviceData, logger)) {
					return null;
				}

				final Variability[] variabilityList = variabilityResponse.variabilityList;
				resultFamilyList.addAll(createFamilyList(variabilityList));

				if (!hasAllNecessarryVariantRulesGiven(resultFamilyList, solutionVariant.getVariantRules(),
						genericItem)) {
					logger.log(Level.SEVERE,
							"Not all non-optional variant rules are given. Can't create a solution variant from "
									+ solutionVariant.getGenericObjectID() + ".");
					return null;
				}

				for (final Family family : resultFamilyList) {
					solutionVariant.addFamily(family);
					familyMap.get(objectString).add(family);
//				familyList.add(family);
				}
			}

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
		return resultFamilyList;
	}

	public void setTriggerWorkFlowAndExit(boolean triggerWorkFlowAndExit) {
		this.triggerWorkFlowAndExit = triggerWorkFlowAndExit;
	}
}
