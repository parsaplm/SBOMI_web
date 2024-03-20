package com.parsa.middleware.processing;

import com.parsa.middleware.businessobjects.*;
import com.parsa.middleware.businessobjects.Dataset;
import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.enums.ImportStatus;
import com.parsa.middleware.logger.ImportLogger;
import com.parsa.middleware.model.QueueEntity;
import com.parsa.middleware.repository.QueueRepository;
import com.parsa.middleware.service.*;
import com.parsa.middleware.session.AppXSession;
import com.parsa.middleware.util.JsonUtil;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidCredentialsException;
import com.teamcenter.services.loose.structuremanagement.StructureRestBindingStub;
import com.teamcenter.services.loose.structuremanagement.StructureService;
import com.teamcenter.services.loose.structuremanagement._2012_09.Structure.AddInformation;
import com.teamcenter.services.loose.structuremanagement._2012_09.Structure.AddParam;
import com.teamcenter.services.loose.structuremanagement._2012_09.Structure.AddResponse;
import com.teamcenter.services.strong.administration._2012_09.PreferenceManagement.GetPreferencesResponse;
import com.teamcenter.services.strong.bom.StructureManagementService;
import com.teamcenter.services.strong.bom._2008_06.StructureManagement;
import com.teamcenter.services.strong.bom._2008_06.StructureManagement.RemoveChildrenFromParentLineResponse;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.FileManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.*;
import com.teamcenter.services.strong.core._2006_03.FileManagement.CommitDatasetFileInfo;
import com.teamcenter.services.strong.core._2006_03.FileManagement.DatasetFileInfo;
import com.teamcenter.services.strong.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData;
import com.teamcenter.services.strong.core._2006_03.FileManagement.GetDatasetWriteTicketsResponse;
import com.teamcenter.services.strong.core._2007_01.DataManagement;
import com.teamcenter.services.strong.core._2008_06.DataManagement.DatasetProperties2;
import com.teamcenter.services.strong.core._2008_06.DataManagement.ReviseInfo;
import com.teamcenter.services.strong.core._2008_06.DataManagement.ReviseOutput;
import com.teamcenter.services.strong.core._2008_06.DataManagement.ReviseResponse2;
import com.teamcenter.services.strong.core._2010_04.DataManagement.GetDatasetCreationRelatedInfoResponse2;
import com.teamcenter.services.strong.core._2012_02.DataManagement.WhereUsedInputData;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ErrorStack;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.common.ObjectPropertyPolicy;
import com.teamcenter.soa.common.PolicyProperty;
import com.teamcenter.soa.common.PolicyType;
import com.teamcenter.soa.exceptions.CanceledOperationException;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Read the given JSON file and create the structure defined in the file.
 *
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 */
@Service
@Scope("prototype")
public class ImportData extends Utility {
	private Logger logger;
	//private final ImportHandler importHandler;
	private QueueEntity currentQueueElement;
	private final ImportStatistic importStatistic;

	// List
	private final List<StructureObject> solutionVariantList;
	private final List<StructureObject> itemList;
	private final List<StructureObject> containerList;

	// Map
	final private Map<StructureObject, List<StructureObject>> itemMap = new HashMap<>();
	final private Map<StructureObject, List<StructureObject>> containerMap = new HashMap<>();
	final private Map<StructureObject, List<StructureObject>> solutionVariantMap = new HashMap<>();

	private ZonedDateTime ruleDate;

	// Boolean
	private boolean structureWasCreated;
	private boolean structureMustBeRevised;
	private  AppXSession session;
	private   ConfigProperties settings;
	private   ChangeManagement changeManagement;
	private   ClassificationManagement classificationManagement;

	private QueueRepository queueRepository;


	public ImportData(ApplicationContext context) {

//		this.settings = settings;
//		this.changeManagement = changeManagement;
//		this.classificationManagement = classificationManagement;
		this.session = context.getBean(AppXSession.class);
		this.settings = context.getBean(ConfigProperties.class);
		this.changeManagement = context.getBean(ChangeManagement.class);
		this.classificationManagement = context.getBean(ClassificationManagement.class);
		this.queueRepository = context.getBean(QueueRepository.class);

		//logger = ImportLogger.createBOMILogger(loggerFilePath, queueElement.getTaskID(), queueElement.getDrawingNumber());
		//logger = Logger.getLogger(ImportData.class.getName());
		//logger.info(String.format("Use the logger %s.", logger.getName()));

		//setLogFileName(queueElement);

		//currentQueueElement = queueElement;
		solutionVariantList = new ArrayList<>();
		itemList = new ArrayList<>();
		containerList = new ArrayList<>();

//		session = currentSession;

		importStatistic = new ImportStatistic();

		//initializeStatistic();
//		try {
//			logger.severe(session.getConnection().getDiscriminator());
//		} catch (final CanceledOperationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	private void setLogFileName(QueueEntity queueElement) {
		logger.info("Set the log file name to the QueueElement.");
		for (final Handler handler : logger.getHandlers()) {
			final Field[] handlerFiles = handler.getClass().getDeclaredFields();
			AccessibleObject.setAccessible(handlerFiles, true);

			for (final Field field : handlerFiles) {
				if (field.getName().equals("files")) {
					File[] files;
					try {
						files = (File[]) field.get(handler);
						queueElement.setLogfileName(files[0].getName());
						break;
					} catch (final IllegalArgumentException e) {
						logger.severe(e.getMessage());

						e.printStackTrace();
					} catch (final IllegalAccessException e) {
						logger.severe(e.getMessage());

						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Initialize the import statistic for this import.
	 */
	private void initializeStatistic() {
		importStatistic.setAmountOfContainers(currentQueueElement.getNumberOfContainer());
		importStatistic.setAmountOfObjects(currentQueueElement.getNumberOfContainer());
		importStatistic.setSyslogFile(session.getConnection().getTcSessionInfo().extraInfo.get("syslogFile"));
		importStatistic.setTcServerUrl(getSettings().getUrl());
	}

	private void setPolicy() {
		logger.info("Set the property policy.");

		final ObjectPropertyPolicy objectPropertyPolicy = new ObjectPropertyPolicy();
		objectPropertyPolicy.addType(getPolicyType("Item",
				new String[] { TcConstants.TEAMCENTER_ITEM_REVISION, TcConstants.TEAMCENTER_OBJECT_STRING,
						TcConstants.TEAMCENTER_OBJECT_NAME, TcConstants.TEAMCENTER_REVISION_LIST,
						TcConstants.TEAMCENTER_ITEM_ID, "Smc0HasVariantConfigContext" }));
		objectPropertyPolicy.addType(getPolicyType("ItemRevision",
				new String[] { TcConstants.TEAMCENTER_OBJECT_GROUP, TcConstants.TEAMCENTER_ITEM_TAG,
						TcConstants.TEAMCENTER_ALL_WORKFLOWS, TcConstants.TEAMCENTER_OBJECT_STRING,TcConstants.TEAMCENTER_RELEASE_STATUS_LIST}));
		objectPropertyPolicy.addType(getPolicyType("BOMLine",
				new String[] { TcConstants.TEAMCENTER_BOMLINE_REVISION, TcConstants.TEAMCENTER_BOMLINE_WINDOW,
						TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX,
						TcConstants.TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST,
						TcConstants.TEAMCENTER_BOMLINE_ALL_WORKFLOWS, TcConstants.TEAMCENTER_BOMLINE_CHILDREN,
						TcConstants.TEAMCENTER_BOMLINE_OBJECT_ID, TcConstants.TEAMCENTER_BOMLINE_CURRENT_REVISION_ID,
						TcConstants.TEAMCENTER_BOMLINE_VARIANT_RULE_TEXT, TcConstants.TEAMCENTER_BOMLINE_QUANTITY,
						TcConstants.TEAMCENTER_FIND_NO, TcConstants.TEAMCENTER_BOMLINE_SOLUTION_VARIANT_SOURCE,
						TcConstants.TEAMCENTER_SOLUTION_VARIANT_CATEGORY, TcConstants.TEAMCENTER_BOMLINE_OBJECT_TYPE,
						TcConstants.TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY, TcConstants.TEAMCENTER_BOMLINE_ITEM,
						TcConstants.TEAMCENTER_BOMLINE_HAS_CHILDREN, TcConstants.TEAMCENTER_OBJECT_STRING , TcConstants.TEAMCENTER_BOMLINE_IS_CLASSIFIED}));
		objectPropertyPolicy.addType(getPolicyType("BOMWindow", new String[] { "Smc0HasVariantConfigContext",
				TcConstants.TEAMCENTER_REVISION_RULE, TcConstants.TEAMCENTER_CONFIG_PERSPECTIVE }));
		objectPropertyPolicy.addType(getPolicyType("RevisionRule", new String[] { TcConstants.TEAMCENTER_RULE_DATE }));
		objectPropertyPolicy.addType(getPolicyType("Cfg0ConfiguratorPerspective",
				new String[] { TcConstants.TEAMCENTER_OBJECT_STRING, TcConstants.TEAMCENTER_STORAGE_CLASS }));
		objectPropertyPolicy.addType(getPolicyType("WorkspaceObject",
				new String[] { TcConstants.TEAMCENTER_OBJECT_STRING, TcConstants.TEAMCENTER_ITEM_ID,
						TcConstants.TEAMCENTER_CURRENT_NAME, TcConstants.TEAMCENTER_ALL_WORKFLOWS,
						TcConstants.TEAMCENTER_OBJECT_NAME, TcConstants.TEAMCENTER_SOLUTION_VARIANT_CATEGORY,
						TcConstants.TEAMCENTER_SECONDARY_OBJECT, TcConstants.TEAMCENTER_OBJECT_DESCRIPTION }));

//		objectPropertyPolicy.addType(getPolicyType("Cfg0LiteralOptionValue",
//				new String[] { TcConstants.TEAMCENTER_OBJECT_TYPE, TcConstants.TEAMCENTER_OBJECT_STRING }));

//		objectPropertyPolicy.addType(getPolicyType("Cfg0LiteralValueFamily",
//				new String[] { TcConstants.TEAMCENTER_OBJECT_TYPE, TcConstants.TEAMCENTER_OBJECT_STRING,  TcConstants.TEAMCENTER_OPTIONAL,
//						TcConstants.TEAMCENTER_CLASS_ID, TcConstants.TEAMCENTER_HAS_FREE_FORM_VALUES }));
//		
//		objectPropertyPolicy.addType(getPolicyType("Cfg0FamilyGroup",
//				new String[] { TcConstants.TEAMCENTER_OBJECT_TYPE, TcConstants.TEAMCENTER_OBJECT_STRING }));

		objectPropertyPolicy.addType(
				getPolicyType("Smc0SolutionVariantSource", new String[] { TcConstants.TEAMCENTER_SECONDARY_OBJECT,
						TcConstants.TEAMCENTER_SOLUTION_VARIANT_CATEGORY, TcConstants.TEAMCENTER_OBJECT_STRING }));

		objectPropertyPolicy.addType(getPolicyType("Cfg0AbsConfiguratorWSO",
				new String[] { TcConstants.TEAMCENTER_OBJECT_TYPE, TcConstants.TEAMCENTER_OBJECT_STRING,
						TcConstants.TEAMCENTER_OPTIONAL, TcConstants.TEAMCENTER_CLASS_ID,
						TcConstants.TEAMCENTER_HAS_FREE_FORM_VALUES }));

		final SessionService sessionService = SessionService.getService(session.getConnection());
		final String response = sessionService.setObjectPropertyPolicy(objectPropertyPolicy);

		logger.severe(response);
	}

	private PolicyType getPolicyType(String objectType, String[] propertyArray) {
		final PolicyType policyType = new PolicyType();
		policyType.setName(objectType);
		policyType.setModifier(PolicyProperty.WITH_PROPERTIES, true);

		for (final String property : propertyArray) {
			policyType.addProperty(property);
		}

		return policyType;
	}

	/**
	 * Login to Teamcenter with the given credentials.
	 *
	 * @param username
	 * @param password
	 * @return
	 */
	public boolean teamcenterLogin() {
		logger.info("Try to login to Teamcenter.");

		try {

			final StringBuilder stringBuilder = new StringBuilder();
		//	stringBuilder.append(password);

			final User user = session.login(settings.getuName(), settings.getPassword());
			if (user != null) {
				logger.info("Successfully logged in");
				setPolicy();
				return true;
			}
		} catch (final NullPointerException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		} catch (final RuntimeException e) {
			logger.severe(String.format("There was an error while logging in.\n%s", e.getMessage()));

			e.printStackTrace();
		} catch (final InvalidCredentialsException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		logger.info("Couldn't log in.");
		return false;
	}


	/**
	 * Start to create the structure that's described in the given JSONObject.
	 *
	 * @param jsonObject
	 */
	public String importStructure(JSONObject jsonObject, Logger logger, QueueEntity queue, AtomicBoolean isCancelled ) {
		currentQueueElement = queue;
//		this.logger = logger;
		this.logger = ImportLogger.createBOMILogger(settings.getLogFolder(), queue.getTaskId(), queue.getDrawingNumber());

		logger.info("Start the import of the given JSON structure.");
		structureMustBeRevised = false;
		structureWasCreated = false;

		importStatistic.setStartImportTime();
		importStatistic.setCanClassify(getSettings().isAlwaysClassify());
		importStatistic.setSuccessfulClassification(getSettings().isAlwaysClassify());

		try {
			Thread.sleep(5000); // Delay for 5 seconds
			System.out.println("5 seconds have passed.");
			// More code to execute after the delay
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		if (isTaskCancelled(currentQueueElement.getTaskId())) {
			logger.info("The import is canceled.");
			endImport(true);
			return "";
		}

		// Your import logic here...

		teamcenterLogin();
		try {
//			setPolicy();

			final SearchManagement searchManagement = new SearchManagement(logger,
					JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS), session);
			try {
				logger.severe(session.getConnection().getDiscriminator());
			} catch (final CanceledOperationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Validate the given JSON
			if (!isValidJsonStructure(jsonObject, searchManagement)) {
				logger.severe("The given JSON file does not contain a valid structure. Please check the JSON file.");
				endImport(true);
				return "";
			}

//			removeAcadHandle(jsonObject);
			// Get the rule date from the JSON
//			setPolicy();
			setRuleDate(jsonObject);

			final StructureObject rootStructureObject = createInternalStructure(jsonObject, null, 10);
			rootStructureObject.setHasParent(false);

			logger.info(
					String.format("The structure has %d container objects, %d solution variants and %d other objects.",
							containerList.size(), solutionVariantList.size(), itemList.size()));

			// Remove all duplicate objects and add similar objects to the map
			removeDuplicateStructureObjects(solutionVariantList, solutionVariantMap);
			removeDuplicateStructureObjects(itemList, itemMap);
			removeDuplicateStructureObjects(containerList, containerMap);

			logger.info(String.format(
					"After removing duplicates the structure has %d container objects, %d solution variants and %d other objects.",
					containerMap.keySet().size(), solutionVariantMap.keySet().size(), itemMap.keySet().size()));

			// Call different workflow methods depending on the object type of the root
			// object in the JSON file
			switch (rootStructureObject.getItemType()) {
			case TcConstants.TEAMCENTER_MODULAR_BUILDING:

				// Normal SBOMI import
				return createMBStructure((ModularBuilding) rootStructureObject);
			case TcConstants.TEAMCENTER_MODUL:

				// Create only one singular solution variant
				if (jsonObject.has(TcConstants.JSON_VARIANT_RULES)) {
					return createSolutionVariant(jsonObject);
				}
			default:
				if (jsonObject.has("children")
						&& JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_CHILDREN).length() == 0) {
					return createObjectStructure2(rootStructureObject);
				} else {
					return createSingleObject(jsonObject);
				}
			}
		} catch (final NullPointerException e) {
			logger.severe("At least one object was null. Can't finish the import successfully.");

			e.printStackTrace();

			importStatistic.setSuccessfulImport(false);
//			endImport(false);
		} catch (final Exception e) {
			e.printStackTrace();
			logger.severe(e.getMessage());

			importStatistic.setSuccessfulImport(false);
//			endImport(false);
		}
		finally {
			importStatistic.setEndImportTime();
			endImport(false);
		}



		return "";
	}

	private void removeAcadHandle(JSONObject jsonObject) throws JSONException {
		jsonObject.remove(TcConstants.JSON_ACAD_HANDLE);
		if (jsonObject.has(TcConstants.JSON_CHILDREN)) {
			final JSONArray children = jsonObject.getJSONArray(TcConstants.JSON_CHILDREN);

			for (int i = 0; i < children.length(); i++) {
				removeAcadHandle(children.getJSONObject(i));
			}
		}

	}

	/**
	 * The start method of any import. Calls the workflow method depending on the
	 * object type of the root object.
	 *
	 * @param jsonObject The JSONObject of the structure
	 * @return The Teamcenter name of the root object of the structure
	 */
	public String importStructure2(JSONObject jsonObject) {
		logger.info("Start the import of the given JSON structure.");
		structureMustBeRevised = false;
		structureWasCreated = false;

		importStatistic.setStartImportTime();
		importStatistic.setCanClassify(getSettings().isAlwaysClassify());
		importStatistic.setSuccessfulClassification(getSettings().isAlwaysClassify());

//		final ObjectMapper om = new ObjectMapper();

		try {
			setRuleDate(jsonObject);

			// Call different workflow methods depending on the object type of the root
			// object in the JSON file
			switch (JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE)) {
			case TcConstants.TEAMCENTER_MODULAR_BUILDING:

				// Normal SBOMI import
//				return createMBStructure(jsonObject);
			case TcConstants.TEAMCENTER_MODUL:

				// Create only one singular solution variant
				if (jsonObject.has(TcConstants.JSON_VARIANT_RULES)) {
					return createSolutionVariant(jsonObject);
				}
			default:
				if (jsonObject.has("children")
						&& JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_CHILDREN).length() != 0) {
					return createObjectStructure(jsonObject);
				} else {
					return createSingleObject(jsonObject);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			logger.severe(e.getMessage());

			importStatistic.setSuccessfulImport(false);
			endImport(false);
		}
		return "";
	}

	private StructureObject createInternalStructure(JSONObject jsonObject, StructureObject parentObject, int depth) throws JSONException {

		StructureObject structureObject;

		// Get the current object type
		switch (JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE)) {
		case TcConstants.TEAMCENTER_MODULAR_BUILDING:
			structureObject = new ModularBuilding(jsonObject, parentObject, depth);
			break;
		case TcConstants.TEAMCENTER_CABIN:
			structureObject = new Container(jsonObject, parentObject, depth);
			containerList.add(structureObject);
			break;
		case TcConstants.TEAMCENTER_MODUL:
			structureObject = new SolutionVariant(jsonObject, parentObject, depth);
			solutionVariantList.add(structureObject);
			break;
		default:
			structureObject = new com.parsa.middleware.businessobjects.Item(jsonObject, parentObject, depth);
			itemList.add(structureObject);
		}

		structureObject.setHasParent(parentObject != null);

		// Call the children recursively
		final JSONArray childrenArray = JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_CHILDREN);

		depth--;
		for (int i = 0; i < childrenArray.length(); i++) {
			createInternalStructure(childrenArray.getJSONObject(i), structureObject, depth);
		}

		return structureObject;
	}

	private void removeDuplicateStructureObjects(List<StructureObject> structureObjectList,
			Map<StructureObject, List<StructureObject>> structureObjectMap) {
		logger.info(String.format("Remove all duplicate objects."));

		Map<StructureObject, List<StructureObject>> modifications = new HashMap<>();

		// Get the iterator for the structureObjectList
		Iterator<StructureObject> structureObjectIterator = structureObjectList.iterator();

		// Iterate over the structureObjectList using the iterator
		while (structureObjectIterator.hasNext()) {
			StructureObject structureObject = structureObjectIterator.next();
			boolean foundEntry = false;

			Iterator<StructureObject> iterator = structureObjectMap.keySet().iterator();
			while (iterator.hasNext()) {
				StructureObject mapStructureObject = iterator.next();
				if (structureObject.getOldJsonObject().equals(mapStructureObject.getOldJsonObject())) {
					// The JSONObjects are exactly the same
					List<StructureObject> parentListCopy = new ArrayList<>(structureObject.getParentList()); // Create a copy of the parent list
					for (final StructureObject parent : parentListCopy) { // Iterate over the copy
						parent.getChildren().remove(structureObject);
						parent.addChild(mapStructureObject);
						mapStructureObject.addParent(parent);
					}
					foundEntry = true;
					iterator.remove(); // Safely remove the current element from the collection
				} else if (structureObject.similar(mapStructureObject, logger)) {
					// The relative properties are different
					modifications.computeIfAbsent(mapStructureObject, k -> new ArrayList<>()).add(structureObject);
					structureObject.setSimilarStructureObject(mapStructureObject);
					foundEntry = true;
					break;
				}
			}

			if (!foundEntry) {
				modifications.put(structureObject, new ArrayList<>());
			}
		}

// Apply modifications to the original map
		structureObjectMap.putAll(modifications);

	}

		/*
	 * Get the desired rule date
	 */
	public ZonedDateTime getRuleDate() {
		return ruleDate;
	}

	/**
	 * Create a structure from a modular building object. This is the intended SBOMI
	 * functionality.
	 *
	 * @param modularBuilding
	 * @return
	 */
	private String createMBStructure(ModularBuilding modularBuilding) throws JSONException {
		logger.info("Create a whole container structure.");
		final SearchManagement searchManagement = new SearchManagement(logger, modularBuilding.getReleaseStatus(),
				session);
		//final ChangeManagement changeManagement = new ChangeManagement(this, logger, session);


		// We start to collect the solution variants
		//currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// Check if there was an error
		if (!createSolutionVariants(searchManagement,
				JsonUtil.getAttribute(modularBuilding.getJsonObject(), TcConstants.JSON_REVISION_RULE))) {
			endImport(true);
			return "";
		}

		logger.info("All solution variants were created and found.");

//		if (importHandler.importIsCanceled(currentQueueElement)) {
//			logger.info("The import is canceled.");
//			endImport(true);
//			return "";
//		}




		// The next import may start now
//		importHandler.setFlag(true);

		// return "";
		// We start to build the structure
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// create method to createStructure()
//		final ModularBuilding modularBuilding = searchAndCreateStructure(jsonObject, searchManagement,
//				similarStructuresMap);

		searchAndCreateStructure(modularBuilding, searchManagement);
//			createStructure();

		modularBuilding.setHasParent(false);

		if (isTaskCancelled(currentQueueElement.getTaskId())) {
			logger.info("The import is canceled.");
			endImport(true);
			return "";
		}

//		if (importHandler.importIsCanceled(currentQueueElement)) {
//			logger.info("The import is canceled.");
//			endImport(false);
//			return modularBuilding.getDisplayString();
//		}

		// We change the properties according to the given values
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

//		changeProperties(searchManagement, modularBuilding);

		long startDatasetTime = System.currentTimeMillis();
//		JsonUtil.getAttribute(modularBuilding.getJsonObject(), TcConstants.DATABASE_FILENAME);
		addDatasetsToStructure(modularBuilding,JsonUtil.getAttribute(modularBuilding.getJsonObject(), TcConstants.DATABASE_FILENAME));
		long endDatasetTime = System.currentTimeMillis();
		importStatistic.setTimeDatasetUpload(startDatasetTime, endDatasetTime);

		changeSingleObjectProperties(modularBuilding.getBomLine(), modularBuilding, searchManagement);

		updateJsonObjects(modularBuilding);
		updateJsonFile(modularBuilding);

		// We are done

//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		importStatistic.setSuccessfulImport(true);
		endImport(false);

		return modularBuilding.getDisplayString();
	}

	private void searchAndCreateStructure(ModularBuilding modularBuilding, SearchManagement searchManagement) {
		logger.info(String.format("Search for an existing MB structure. Create it, if none is found."));

		try {
//			importStatistic.setStartCreateStructure();
			importStatistic.setStartSearchStructure();
			importStatistic.setDrawingNumber(modularBuilding.getDesignNo());

			// If the object ID was given
			if (!modularBuilding.getJsonObjectID().isEmpty()) {

				final Item item = searchManagement.searchObject(modularBuilding.getItemType(),
						modularBuilding.getJsonObjectID());

				if (item != null) {
					modularBuilding.setItem(item);
					modularBuilding.setItemRevision((ItemRevision) modularBuilding.getItem().get_item_revision());
					modularBuilding.setBomLine(getBomLine(modularBuilding.getItemRevision(), session));
				}
			}

			// If the objectID wasn't given or no object with that ID exists
			if (modularBuilding.getItem() == null) {

				// Search for an object with the given designNo
				final ItemRevision itemRevision = searchManagement.searchObjectByDesignNo(modularBuilding.getItemType(),
						modularBuilding.getDesignNo());

				// We found an object with the design No
				if (itemRevision != null) {
					modularBuilding.setItemRevision(itemRevision);
					modularBuilding.setItem(modularBuilding.getItemRevision().get_items_tag());
				}
			}

			// If the root object was found
			if (modularBuilding.getItem() != null) {

				ItemRevision existingItemRevision = null;
				modularBuilding.setWasCreated(true);

//				if (hasOnlyReuseCategory(modularBuilding)) {

				// Search for a matching revision
//					existingItemRevision = searchManagement.searchRevision(modularBuilding);
				final List<JSONObject> jsonChildren = new ArrayList<>();
				final JSONArray childrenArray = JsonUtil.getJsonArray(modularBuilding.getOldJsonObject(),
						TcConstants.JSON_CHILDREN);
				for (int i = 0; i < childrenArray.length(); i++) {
					jsonChildren.add(childrenArray.getJSONObject(i));
				}

				existingItemRevision = searchManagement.searchRevision(modularBuilding.getItem(), jsonChildren);
//				}

				// Create a new revision if none was found
				if (existingItemRevision == null) {
					logger.info("Create a new revision of the modular building.");

					structureWasCreated = true;
					importStatistic.setWasRevised(true);

					modularBuilding.setItemRevision(reviseObject(modularBuilding.getItem()));
					modularBuilding.setBomLine(getBomLine(modularBuilding.getItemRevision(), session));
				} else {
					logger.info("Use an already existing revision of the modular building.");

					importStatistic.setStructureExisted(true);

					modularBuilding.setItemRevision(existingItemRevision);
					modularBuilding.setBomLine(getBomLine(modularBuilding.getItemRevision(), session));
					importStatistic.setEndSearchStructure();
//					importStatistic.setEndCreateStructure();
					return;
				}
			} else {
				// We didn't found an object with the design no
				structureWasCreated = true;
				modularBuilding.setWasCreated(true);

				logger.info("Create the structure entirely new.");

				final Map<String, String> extendedProperties = new HashMap<>();
				extendedProperties.put(TcConstants.TEAMCENTER_DRAWING_NO, modularBuilding.getDesignNo());
				modularBuilding.setItem(createObject(modularBuilding.getObjectName(),
						modularBuilding.getObjectDescription(), modularBuilding.getItemType(),
						modularBuilding.getJsonRevisionID(), extendedProperties));

				DataManagementService.getService(session.getConnection()).getProperties(
						new ModelObject[] { modularBuilding.getItem() },
						new String[] { TcConstants.TEAMCENTER_ITEM_REVISION, TcConstants.TEAMCENTER_OBJECT_STRING });
				modularBuilding.setBomLine(getBomLine(modularBuilding.getItem(), session));
				if (modularBuilding.getItem().get_item_revision() != null) {
					modularBuilding.setItemRevision((ItemRevision) modularBuilding.getItem().get_item_revision());
				} else if (modularBuilding.getBomLine().get_bl_revision() != null) {
					modularBuilding.setItemRevision((ItemRevision) modularBuilding.getBomLine().get_bl_revision());
				}
			}
			importStatistic.setEndSearchStructure();
			importStatistic.setStartCreateStructure();
			startStructureCreation(searchManagement, modularBuilding);
			importStatistic.setEndCreateStructure();
		} catch (final NotLoadedException | JSONException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
	}

	/**
	 * Iterate through the whole internal structure and return false if at least one
	 * solution variant category is not Reuse.
	 * 
	 * @param structureObject
	 * @return
	 */
	private boolean hasOnlyReuseCategory(StructureObject structureObject) {
		if (structureObject instanceof SolutionVariant) {
			return ((SolutionVariant) structureObject).getSolutionVariantCategory() == 2;
		} else {
			for (final StructureObject child : structureObject.getChildren()) {
				if (!hasOnlyReuseCategory(child)) {
					return false;
				}
			}
		}

		return true;
	}

	// create new structure here
	private void startStructureCreation(SearchManagement searchManagement, StructureObject rootStructureObject) {
		logger.info("Create the structure from the bottom to top.");

	//	final ChangeManagement changeManagement = new ChangeManagement(this, session, settings);
		final Queue<StructureObject> queue = new PriorityQueue<>(
				((StructureObject a, StructureObject b) -> Integer.compare(a.getDepth(), b.getDepth())));
		final Queue<StructureObject> tempQueue = new PriorityQueue<>(
				((StructureObject a, StructureObject b) -> Integer.compare(a.getDepth(), b.getDepth())));

		// Add all solution variants to the queue
//		for (final StructureObject solutionVariant : solutionVariantMap.keySet()) {
//			if (!queue.contains(solutionVariant)) {
//				queue.add(solutionVariant);
//			}
//		}

		createQueue(tempQueue, rootStructureObject);
		while (!tempQueue.isEmpty()) {
			final StructureObject currentObject = tempQueue.poll();
			if (!queue.contains(currentObject)) {
				queue.add(currentObject);
			}
		}

//		createNextLevel(queue, searchManagement, changeManagement);
		createStructure(queue, searchManagement, changeManagement);
	}

	/**
	 * Fill the queue with all objects of the structure
	 * 
	 * @param queue           The current queue
	 * @param structureObject the object to be added
	 */
	private void createQueue(Queue<StructureObject> queue, StructureObject structureObject) {
		queue.add(structureObject);

		// Recursively add the children
		for (final StructureObject childObject : structureObject.getChildren()) {
			createQueue(queue, childObject);
		}
	}

	private void createStructure(Queue<StructureObject> queue, SearchManagement searchManagement,
			ChangeManagement changeManagement) {
		logger.info("Iterate through all objects level by level and search or create them in Teamcenter.");

		while (!queue.isEmpty()) {
			final StructureObject currentObject = queue.poll();

			if (!currentObject.wasCreated()) {
				Item item = null;

				final boolean isAnyNotFound = currentObject.getChildren().stream().anyMatch(obj -> obj.isNotFound());

				if (!currentObject.getJsonObjectID().isBlank()) {
					// We have an item ID given
					item = searchManagement.searchObject(currentObject.getItemType(), currentObject.getJsonObjectID());

					// Create the object if none was found
					if (item == null) {
						item = createObject(currentObject.getProperties().get(TcConstants.JSON_OBJECT_NAME),
								currentObject.getProperties().get(TcConstants.JSON_OBJECT_DESCRIPTION),
								currentObject.getItemType(), currentObject.getJsonRevisionID());

						currentObject.setBomLine(getBomLine(item, session));
						currentObject.setNotFound(true);

						// Add the children to the new BOMLine
						if (!currentObject.getChildren().isEmpty()) {
							for (final StructureObject childObject : currentObject.getChildren()) {
								addChildrenToBOMLineWithTiming(currentObject, childObject);
							}
						}
					} else {
						currentObject.setBomLine(getBomLine(item, session));
					}

					addBomlineToSimilar(currentObject);
					changeSingleObjectProperties(currentObject.getBomLine(), currentObject,
							searchManagement);
				} else if (!isAnyNotFound && !currentObject.getChildren().isEmpty()
						&& hasExistingObject(currentObject, searchManagement)) {
					// We have found an existing object with the correct children
					currentObject.setWasCreated(true);
				} else {
					// We have to create the object new

					item = createObject(currentObject.getProperties().get(TcConstants.JSON_OBJECT_NAME),
							currentObject.getProperties().get(TcConstants.JSON_OBJECT_DESCRIPTION),
							currentObject.getItemType(), currentObject.getJsonRevisionID());

					currentObject.setBomLine(getBomLine(item, session));
					currentObject.setNotFound(true);

					addBomlineToSimilar(currentObject);

					// Add the children to the new BOMLine
					if (!currentObject.getChildren().isEmpty()) {
						for (final StructureObject childObject : currentObject.getChildren()) {
							addChildrenToBOMLineWithTiming(currentObject, childObject);
						}
					}

					changeSingleObjectProperties(currentObject.getBomLine(), currentObject,
							searchManagement);
				}
			} else {
				try { // TODO: possible NPE on line 823
					if (!currentObject.getChildren().isEmpty()
							&& currentObject.getBomLine().get_bl_child_lines().length != currentObject.getChildren()
									.size()) {
						for (final StructureObject childObject : currentObject.getChildren()) {
							addChildrenToBOMLineWithTiming(currentObject, childObject);
						}
					}
				} catch (final NotLoadedException e) {
					logger.severe(e.getMessage());

					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Creates the next level of StructureObjects, including their objects and
	 * BOMLines, based on the provided object.
	 *
	 * @param searchManagement The root StructureObject from which to create the next
	 *                        level.
	 */
	private void createNextLevel(Queue<StructureObject> queue, SearchManagement searchManagement,
			ChangeManagement changeManagement) {
		logger.info(
				"Iterate through all objects and search for existing parents and create them, if none where found.");
		final Set<StructureObject> usedStructureObjects = new HashSet<>();

		// TODO: refactor
		while (!queue.isEmpty()) {
			// pop current object from the queue
			final StructureObject currentObject = queue.poll();
			usedStructureObjects.add(currentObject);

			try {
				if (!currentObject.wasCreated()) {
					// Create the object and BOMLine for the currentObject
					createObjectAndBOMLine(currentObject, searchManagement);
					addBomlineToSimilar(currentObject);

					changeSingleObjectProperties(currentObject.getBomLine(), currentObject,
							searchManagement);

					if (currentObject.hasParent()) {
						// Create the object and BOMLine for each parent and add their parents to the
						// queue
						for (final StructureObject parentObject : currentObject.getParentList()) {

							// The modular building object already exists as root
							if (parentObject.getItemType().equals(TcConstants.TEAMCENTER_MODULAR_BUILDING)) {
								addChildrenToBOMLineWithTiming(parentObject, currentObject);

								changeSingleObjectProperties(currentObject.getBomLine(), currentObject,
										searchManagement);

//								if (!queue.contains(parentObject) && !usedStructureObjects.contains(parentObject)) {
//									queue.add(parentObject);
//								}

								continue;
							}

							createObjectAndBOMLine(parentObject, searchManagement);
							addBomlineToSimilar(parentObject);

							// add object group here
							changeSingleObjectProperties(parentObject.getBomLine(), parentObject,
									searchManagement);

//							if (!queue.contains(parentObject) && !usedStructureObjects.contains(parentObject)) {
//								queue.add(parentObject);
//							}
						}
					}
				} else {
					if (currentObject.hasParent()) {
						for (final StructureObject parentObject : currentObject.getParentList()) {

							// The modular building object already exists as root
							if (parentObject.getItemType().equals(TcConstants.TEAMCENTER_MODULAR_BUILDING)) {
								addChildrenToBOMLineWithTiming(parentObject, currentObject);
								changeSingleObjectProperties(currentObject.getBomLine(), currentObject,
										searchManagement);

//								if (!queue.contains(parentObject) && !usedStructureObjects.contains(parentObject)) {
//									queue.add(parentObject);
//								}

								break;
							}
							// check first if we need to create an object before comparing
							final boolean isAnyNotFound = parentObject.getChildren().stream()
									.anyMatch(obj -> obj.isNotFound());
							if (isAnyNotFound) {
								createObjectAndBOMLine(parentObject, searchManagement);
								addBomlineToSimilar(parentObject);
								changeSingleObjectProperties(parentObject.getBomLine(), parentObject,
										searchManagement);
							}
//							// Update the BOMLine with where-used references for each parent
//							else if (updateBOMLineWithWhereUsed(currentObject, parentObject, searchManagement)) {
							else if (hasExistingObject(parentObject, searchManagement)) {
								parentObject.setWasCreated(true);
							} else {
//                                // Create the object and BOMLine for the parent if where-used references are not
//                                // found
								createObjectAndBOMLine(parentObject, searchManagement);
								addBomlineToSimilar(parentObject);
								changeSingleObjectProperties(parentObject.getBomLine(), parentObject,
										searchManagement);
							}
//							if (!queue.contains(parentObject) && !usedStructureObjects.contains(parentObject)) {
//								queue.add(parentObject);
//							}
						}
					} else {
//						if (currentObject.getBomLine().get_bl_child_lines().length != currentObject.getChildren()
//								.size()) {
//							for (final StructureObject childObject : currentObject.getChildren()) {
//								if (!usedStructureObjects.contains(childObject) && !queue.contains(childObject)) {
//									final Item item = searchManagement.searchObject(childObject.getItemType(),
//											childObject.getJsonObjectID());
//									childObject.setBomLine(getBomLine(item));
//									queue.add(childObject);
//								}
//							}
//						}
					}
				}

			} catch (final NotLoadedException e) {
				logger.severe(e.getMessage());

				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * 
	 * @param parentStructureObject
	 * @param searchManagement
	 * @return
	 */
	private boolean hasExistingObject(StructureObject parentStructureObject, SearchManagement searchManagement) {
		logger.info("Search for an existing parent object.");
		final Set<Item> possibleParentsSet = new HashSet<>();
		final Map<StructureObject, List<Item>> possibleParentMap = new HashMap<>();

		// Get all parent objects from all child objects
		for (final StructureObject childObject : parentStructureObject.getChildren()) {
			try {
				possibleParentMap.put(childObject, searchManagement.searchParentObjects(
						parentStructureObject.getItemType(), childObject.getBomLine().get_bl_item_current_id()));
				possibleParentsSet.addAll(possibleParentMap.get(childObject));
			} catch (final NotLoadedException e) {
				logger.severe(e.getMessage());

				e.printStackTrace();
			} catch (final NullPointerException e) {
				e.printStackTrace();
			}
		}

		final Set<Item> tempSet = new HashSet<>(possibleParentsSet);

		// Remove all objects that are not used by all children
		for (final Item parentBomline : tempSet) {
			for (final List<Item> list : possibleParentMap.values()) {
				if (!list.contains(parentBomline)) {
					possibleParentsSet.remove(parentBomline);
					break;
				}
			}
		}

		// Get the first parent that matches the properties
		for (final Item parentItem : possibleParentsSet) {
			final BOMLine bomline = getBomLine(parentItem, session);

			try {

				final ModelObject[] statusArray = bomline.get_bl_revision()
						.getPropertyObject(TcConstants.TEAMCENTER_RELEASE_STATUS_LIST).getModelObjectArrayValue();

				if (!parentStructureObject.getReleaseStatus().isBlank() && statusArray.length > 0) {
					for (final ModelObject element : statusArray) {
						if (element.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_NAME)
								.equals(parentStructureObject.getReleaseStatus())) {

							// Check if also the other properties and children are correct
							if (searchManagement.compareChildren(bomline, parentStructureObject, false)) {
								parentStructureObject.setBomLine(bomline);

								logger.info("Found a matching parent object.");
								return true;
							}
						}
					}
				} else if (!parentStructureObject.getReleaseStatus().isBlank() && statusArray.length == 0) {
					continue;
				}

				if (searchManagement.compareChildren(bomline, parentStructureObject, false)) {
					parentStructureObject.setBomLine(bomline);

					logger.info("Found a matching parent object.");
					return true;
				}
			} catch (final NotLoadedException e) {
				logger.severe(e.getMessage());

				e.printStackTrace();
			}

			// Close the BOMLines we don't need anymore
			closeBOMLines(new BOMLine[] { bomline }, session);
		}

		logger.info("Didn't found a matching parent object.");
		return false;
	}

	private void addBomlineToSimilar(StructureObject currentStructureObject) {
		final StructureObject mapStructureObject = currentStructureObject.getSimilarStructureObject() == null
				? currentStructureObject
				: currentStructureObject.getSimilarStructureObject();

		if (currentStructureObject instanceof Container) {

			// Iterate all similar container objects and add the current BOMLine
			for (final StructureObject similarObject : containerMap.get(mapStructureObject)) {
				if (similarObject.getBomLine() == null) {
					similarObject.setBomLine(currentStructureObject.getBomLine());
					similarObject.setItem(currentStructureObject.getItem());
					similarObject.setWasCreated(true);
					similarObject.setNotFound(currentStructureObject.isNotFound());
				}
			}
		} else if (currentStructureObject instanceof Item) {

			// Iterate all similar Item objects and add the current BOMLine
			for (final StructureObject similarObject : itemMap.get(mapStructureObject)) {
				if (similarObject.getBomLine() == null) {
					similarObject.setBomLine(currentStructureObject.getBomLine());
					similarObject.setItem(currentStructureObject.getItem());
					similarObject.setWasCreated(true);
					similarObject.setNotFound(currentStructureObject.isNotFound());
				}
			}
		}
	}

	/**
	 * Creates an object, sets its BOMLine, and updates related properties for the
	 * given StructureObject.
	 *
	 * @param structureObject The StructureObject for which the object and BOMLine
	 *                        are being created.
	 * @throws NotLoadedException if the object or BOMLine are not loaded.
	 */
	private void createObjectAndBOMLine(StructureObject structureObject, SearchManagement searchManagement)
			throws NotLoadedException {
		// Create the object with the specified properties
		structureObject.setItem(createObject(structureObject.getProperties().get(TcConstants.JSON_OBJECT_NAME),
				structureObject.getProperties().get(TcConstants.JSON_OBJECT_DESCRIPTION), structureObject.getItemType(),
				structureObject.getJsonRevisionID()));

		// Set the BOMLine for the object
		structureObject.setBomLine(getBomLine(structureObject.getItem(), session));

		// Set the 'wasCreated' flag to indicate that the object was successfully
		// created
		structureObject.setWasCreated(true);
		// Set the ItemRevision for the object based on its BOMLine
		structureObject.setItemRevision((ItemRevision) structureObject.getBomLine().get_bl_revision());
		// Iterate over the children of the object and add them to the BOMLine
		for (final StructureObject child : structureObject.getChildren()) {

			if (child.getBomLine() == null) {
				// Search for existing object with the given object ID
				if (!child.getJsonObjectID().isBlank()) {

					// The object ID is given in the JSON file
					final Item item = searchManagement.searchObject(child.getItemType(), child.getJsonObjectID());

					if (item != null) {
						// The object did already exist
						child.setItem(item);
						child.setBomLine(getBomLine(item, session));
					}
				} else {

					// Search an existing object without an object ID given
					searchAndCreateSingleObject(searchManagement, child);
				}
			}

			addChildrenToBOMLineWithTiming(structureObject, child);
		}
	}

	/**
	 * Updates the BOMLine of the parentObject with a referenced BOMLine obtained
	 * from the where-used objects of the specified object.
	 *
	 * @param structureObject  The StructureObject for which the BOMLine is being
	 *                         updated.
	 * @param parentObject     The parent StructureObject for which the BOMLine is
	 *                         being compared.
	 * @param searchManagement The SearchManagement instance used for comparing
	 *                         BOMLines.
	 * @return {@code true} if the BOMLine was successfully updated, {@code false}
	 *         otherwise.
	 * @throws NotLoadedException if the referenced objects are not loaded.
	 */
	private boolean updateBOMLineWithWhereUsed(StructureObject structureObject, StructureObject parentObject,
			SearchManagement searchManagement) throws NotLoadedException {
		logger.info(String.format("updateBOMLineWithWhereUsed of %s object and parentObject: ",
				structureObject.getDisplayString(), parentObject.getDisplayString()));
		// Retrieve the referenced view objects from the given object's BOMLine
		final ModelObject[] referencedViewObjects = getWhereUsedObjects(
				getWorkspaceObject(structureObject.getBomLine()));

		if (referencedViewObjects.length > 0) {

			// Get the first referenced PSBOMViewRevision object
			for (final ModelObject referenceObject : referencedViewObjects) {
				if (referenceObject instanceof PSBOMViewRevision) {
					final PSBOMViewRevision psbomViewRevision = (PSBOMViewRevision) referenceObject;

					// Retrieve the referenced objects from the PSBOMViewRevision
					final ModelObject[] referencedObjects = getWhereUsedObjects(getWorkspaceObject(psbomViewRevision));
					if (referencedObjects.length > 0) {

						for (final ModelObject modelObject : referencedObjects) {
							if (modelObject instanceof ItemRevision) {
								final ItemRevision itemRevision = (ItemRevision) modelObject;
								final BOMLine parent = getBomLine(itemRevision, session);

								if (searchManagement.compareChildren(parent, parentObject, false)) {
									parentObject.setBomLine(parent);
									return true;
								}

								closeBOMLines(new BOMLine[] { parent }, session);
							}
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Retrieves the objects where the given WorkspaceObject is referenced.
	 *
	 * @param workspaceObject The WorkspaceObject to retrieve the referenced objects
	 *                        for.
	 * @return An array of ModelObjects representing the referenced objects.
	 * @throws NotLoadedException if the WorkspaceObject is not loaded.
	 */
	private ModelObject[] getWhereUsedObjects(WorkspaceObject workspaceObject) throws NotLoadedException {
		logger.info(String.format("getWhereUsedObjects of %s object.", workspaceObject.get_object_name()));

		// Get the DataManagementService instance
		final DataManagementService dmService = DataManagementService.getService(session.getConnection());
		final List<ModelObject> bomLineObjects = new ArrayList<>();
		if (workspaceObject != null) {
			// Call the whereReferenced operation to get the where referenced objects
			final DataManagement.WhereReferencedResponse whereUsedResponse = dmService
					.whereReferenced(new WorkspaceObject[] { workspaceObject }, 1);

			// Retrieve the plain objects from the service response

			for (int i = 0; i < whereUsedResponse.serviceData.sizeOfPlainObjects(); i++) {
				bomLineObjects.add(whereUsedResponse.serviceData.getPlainObject(i));

			}
		}
		logger.info(String.format("getWhereUsedObjects found of %s object.", bomLineObjects.size()));

		return bomLineObjects.toArray(new ModelObject[0]);
	}

	/**
	 * Retrieves the WorkspaceObject from the given BOMLine object.
	 *
	 * @param bomLine The BOMLine object to retrieve the WorkspaceObject from.
	 * @return The WorkspaceObject corresponding to the BOMLine.
	 * @throws NotLoadedException if the BOMLine object is not loaded.
	 */
	private WorkspaceObject getWorkspaceObject(BOMLine bomLine) {

		// Cast the item of the BOMLine to WorkspaceObject
		WorkspaceObject workspaceObject = null;
		try {
			workspaceObject = (WorkspaceObject) bomLine.get_bl_item();

			// Create a new WhereUsedInputData and assign the workspaceObject as the
			// inputObject
			final WhereUsedInputData WhereUsedInputData = new WhereUsedInputData();
			WhereUsedInputData.inputObject = workspaceObject;
		} catch (final NotLoadedException e) {
			// throw new RuntimeException(e);
		}

		return workspaceObject;
	}

	/**
	 * Retrieves the WorkspaceObject from the given PSBOMViewRevision object.
	 *
	 * @param bomLine The PSBOMViewRevision object to retrieve the WorkspaceObject
	 *                from.
	 * @return The WorkspaceObject corresponding to the PSBOMViewRevision.
	 * @throws NotLoadedException if the PSBOMViewRevision object is not loaded.
	 */

	private WorkspaceObject getWorkspaceObject(PSBOMViewRevision bomLine) throws NotLoadedException {

		// Cast the PSBOMViewRevision to WorkspaceObject
		final WorkspaceObject workspaceObject = bomLine;

		// Create a new WhereUsedInputData and assign the workspaceObject as the
		// inputObject
		final WhereUsedInputData WhereUsedInputData = new WhereUsedInputData();
		WhereUsedInputData.inputObject = workspaceObject;

		return workspaceObject;
	}

	/**
	 * Create a structure or a single object from an object that's not a modular
	 * building or a solution variant.
	 *
	 * @return
	 */
	private String createObjectStructure(JSONObject jsonObject) throws JSONException {
		logger.info("Create a small structure.");
		final SearchManagement searchManagement = new SearchManagement(logger,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS), session);

		if (!isValidJsonStructure(jsonObject, searchManagement)) {
			logger.severe("The given JSON file does not contain a valid structure. Please check the JSON file.");
			endImport(true);
			return "";
		}

		// Create solution variants
	//	currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// Create the StructureObject depending on the root object type
		StructureObject structureObject;
		switch (JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE)) {
		case TcConstants.TEAMCENTER_CABIN:
			structureObject = new Container(jsonObject, null, 0);
			structureObject.setHasParent(false);
			break;
		default:
			structureObject = new com.parsa.middleware.businessobjects.Item(jsonObject, null, 0);
			structureObject.setHasParent(false);
			break;
		}

		final List<JSONObject> solutionVariantList = JsonUtil.getSolutionVariants(jsonObject);
		if (!solutionVariantList.isEmpty()) {

			// Search and create solution variants
			if (!createSolutionVariantObjects(jsonObject)) {
				endImport(true);
				return "";
			}
		}

		// The next import may start now
//		importHandler.setFlag(true);

		if (isTaskCancelled(currentQueueElement.getTaskId())) {
			logger.info("The import is canceled.");
			endImport(true);
			return "";
		}

//		if (importHandler.importIsCanceled(currentQueueElement)) {
//			logger.info("The import is canceled.");
//			endImport(false);
//			return structureObject.getDisplayString();
//		}

		// Create structure
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		structureObject = buildStructure(jsonObject, structureObject, searchManagement);

		if (isTaskCancelled(currentQueueElement.getTaskId())) {
			logger.info("The import is canceled.");
			endImport(true);
			return "";
		}

//		if (importHandler.importIsCanceled(currentQueueElement)) {
//			logger.info("The import is canceled.");
//			endImport(false);
//			return structureObject.getDisplayString();
//		}

		// Change properties
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		//final ChangeManagement changeManagement = new ChangeManagement(this,  session, settings);
		changePropertiesOfStructure2(structureObject.getBomLine(), structureObject, searchManagement);

		// Increment progress (Import done)
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		importStatistic.setSuccessfulImport(true);
		endImport(false);

		return structureObject.getDisplayString();

	}

	/**
	 * Create a structure or a single object from an object that's not a modular
	 * building or a solution variant.
	 *
	 * @return
	 */
	private String createObjectStructure2(StructureObject rootStructureObject) throws JSONException {
		logger.info("Create a small structure.");
		final SearchManagement searchManagement = new SearchManagement(logger,
				JsonUtil.getAttribute(rootStructureObject.getJsonObject(), TcConstants.JSON_RELEASE_STATUS), session);
		//final ChangeManagement changeManagement = new ChangeManagement(this,  session, settings);

		// Create solution variants
	//	currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// Create the solution variants
		if (!createSolutionVariants(searchManagement,
				JsonUtil.getAttribute(rootStructureObject.getJsonObject(), TcConstants.JSON_REVISION_RULE))) {
			endImport(true);
			return "";
		}

		// The next import may start now
//		importHandler.setFlag(true);

		if (isTaskCancelled(currentQueueElement.getTaskId())) {
			logger.info("The import is canceled.");
			endImport(true);
			return "";
		}

//		if (importHandler.importIsCanceled(currentQueueElement)) {
//			logger.info("The import is canceled.");
//			endImport(false);
//			return rootStructureObject.getDisplayString();
//		}

		// Create structure
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		startStructureCreation(searchManagement, rootStructureObject);

		if (isTaskCancelled(currentQueueElement.getTaskId())) {
			logger.info("The import is canceled.");
			endImport(true);
			return "";
		}

//		if (importHandler.importIsCanceled(currentQueueElement)) {
//			logger.info("The import is canceled.");
//			endImport(false);
//			return rootStructureObject.getDisplayString();
//		}

		// Change properties
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		changeSingleObjectProperties(rootStructureObject.getBomLine(), rootStructureObject,
				searchManagement);

		updateJsonObjects(rootStructureObject);
		updateJsonFile(rootStructureObject);

		// Increment progress (Import done)
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		importStatistic.setSuccessfulImport(true);
		endImport(false);

		return rootStructureObject.getDisplayString();

	}

	/**
	 * Create a single object that is given in the JSON file.
	 *
	 * @param jsonObject
	 * @return
	 */
	private String createSingleObject(JSONObject jsonObject) throws JSONException {
		logger.info("Create a single object.");
		final SearchManagement searchManagement = new SearchManagement(logger,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS), session);

		if (!isValidJsonStructure(jsonObject, searchManagement)) {
			logger.severe("The given JSON file does not contain a valid structure. Please check the JSON file.");
			endImport(true);
			return "";
		}

		// Create the StructureObject depending on the root object type
		StructureObject structureObject;
		switch (JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE)) {
		case TcConstants.TEAMCENTER_CABIN:
			structureObject = new Container(jsonObject, null, 0);
			structureObject.setHasParent(false);
			break;
		default:
			structureObject = new com.parsa.middleware.businessobjects.Item(jsonObject, null, 0);
			structureObject.setHasParent(false);
			break;
		}

		// Create solution variants
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// Create structure
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// Search for existing object with the given object ID
		if (!structureObject.getJsonObjectID().isBlank()) {

			// The object ID is given in the JSON file
			final Item item = searchManagement.searchObject(structureObject.getItemType(),
					structureObject.getJsonObjectID());

			if (item != null) {
				// The object did already exist
				structureObject.setItem(item);
				structureObject.setBomLine(getBomLine(item, session));
			}
		} else {

			// Search an existing object without an object ID given
			searchAndCreateSingleObject(searchManagement, structureObject);
		}

		// Stop if no BOMLine was found or created
		if (structureObject.getBomLine() == null) {
			endImport(true);
			return "";
		}
		if (isTaskCancelled(currentQueueElement.getTaskId())) {
			logger.info("The import is canceled.");
			endImport(true);
			return "";
		}
//		if (importHandler.importIsCanceled(currentQueueElement)) {
//			logger.info("The import is canceled.");
//			endImport(true);
//			return structureObject.getDisplayString();
//		}

		// The next import may start now
//		importHandler.setFlag(true);

		// Change properties
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		//final ChangeManagement changeManagement = new ChangeManagement(this, session, settings);
		changePropertiesOfStructure2(structureObject.getBomLine(), structureObject, searchManagement);

		// Increment progress (Import done)
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		importStatistic.setSuccessfulImport(true);
		endImport(true);

		return structureObject.getDisplayString();
	}

	/**
	 * Search for a single object in Teamcenter. Create it, if it doesn't exist.
	 *
	 * @param searchManagement
	 * @param structureObject
	 */
	private void searchAndCreateSingleObject(final SearchManagement searchManagement, StructureObject structureObject) {
		logger.info(String.format("Search or create the given object of type %s.", structureObject.getItemType()));

		try {

			// We have no object ID given
			final List<BOMLine> possibleObjects = searchManagement.searchObjectsWithObjectGroup(
					structureObject.getItemType(),
					structureObject.getProperties().get(TcConstants.JSON_OBJECT_GROUP_ID),
					structureObject.getReleaseStatus());

			if (!possibleObjects.isEmpty()) {
				for (final BOMLine bomline : possibleObjects) {
					if (bomline.get_bl_has_children()) {

						// We only want a single object, not a structure
						continue;
					}

					// We have one object without children
					structureObject.setBomLine(bomline);
					logger.info(String.format("Found the %s object %s.", structureObject.getItemType(),
							structureObject.getDisplayString()));
					break;
				}
			}

			if (structureObject.getBomLine() == null) {
				// Create new object, if no existing object was found

				final Item item = createObject(structureObject.getProperties().get(TcConstants.JSON_OBJECT_NAME),
						structureObject.getProperties().get(TcConstants.JSON_OBJECT_DESCRIPTION),
						structureObject.getItemType(), structureObject.getJsonRevisionID());

				structureObject.setItem(item);
				structureObject.setBomLine(getBomLine(item, session));
				logger.info(String.format("Created the %s object %s.", structureObject.getItemType(),
						structureObject.getDisplayString()));
			}

		} catch (final NotLoadedException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		}

	}

	/**
	 * Create a single solution variant object from the given JSON.
	 *
	 * @param jsonObject
	 * @return
	 */
	private String createSolutionVariant(JSONObject jsonObject) throws JSONException {
		logger.info("Create a single solution variant object.");
		final SearchManagement searchManagement = new SearchManagement(logger,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS), session);
//		final ClassificationManagement classificationManagement = new ClassificationManagement(logger, this, session);
		final SolutionVariantManagement solutionVariantManagement = new SolutionVariantManagement(logger,
				classificationManagement, searchManagement, this, session);

		// Validate the JSON file
		final List<JSONObject> solutionVariantList = new ArrayList<>();
		solutionVariantList.add(jsonObject);
		if (!JsonUtil.validObjectType(jsonObject, logger, session)) {
			logger.severe("The object type in the given JSON file is invalid or missing. Please check the JSON file.");
			return "";
		} else if (!JsonUtil.validSolutionVariant(solutionVariantList, logger)) {
			logger.severe("The given JSON file does not contain a valid solution variant. Please check the JSON file.");
			return "";
		}

		// Start the import of the solution variant
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// Create the solution variant object with all necessary properties
		final SolutionVariant solutionVariant = new SolutionVariant(jsonObject, null, 0);
		solutionVariant.setHasParent(false);
		solutionVariant.setSolutionVariantCategory(getSolutionVariantCategoryFromString(
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)));

		// Try to search or create the solution variant
		solutionVariantManagement.searchAndCreateSolutionVariant(solutionVariant,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS));

		// Cancel the import if at least one solution variant could not be created.
		if (solutionVariant.getBomLine() == null) {
			logger.severe(String.format(
					"Couldn't create a solution variant for the object %s. Please check the JSON file or the generic object %s for errors.",
					JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_NAME),
					solutionVariant.getGenericObjectID()));
			return "";
		}

		// Change the properties
//		currentQueueElement.incrementImportProgess();
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);
		// Change the properties and classify the object

		// Import done
//		currentQueueElement.incrementImportProgess();
//		importHandler.changeValue(TcConstants.DATABASE_IMPORT_PROGRESS, currentQueueElement.getImportProgress(),
//				currentQueueElement);

		// The next import may start now
//		importHandler.setFlag(true);
		importStatistic.setSuccessfulImport(true);
		endImport(true);

		return solutionVariant.getDisplayString();
	}

	private void setRuleDate(JSONObject jsonObject) {
		logger.info("Get the rule date for the creation of solution variants.");

		try {
			final String[] dateAndTime = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_DATE_MODIFIED).split(" ");
			final String[] date = dateAndTime[0].split("\\.");
			final String[] time = dateAndTime[1].split(":");

			final TimeZone timeZone = TimeZone.getDefault();
			ruleDate = ZonedDateTime.of(Integer.parseInt(date[2]), Integer.parseInt(date[1]), Integer.parseInt(date[0]),
					Integer.parseInt(time[0]), Integer.parseInt(time[1]),
					time.length > 2 ? Integer.parseInt(time[2]) : 0, 0, timeZone.toZoneId());
		} catch (final ArrayIndexOutOfBoundsException e) {
			logger.severe(String.format("The JSON attribute %s isn't a valid value. Could not get the rule date.",
					TcConstants.JSON_DATE_MODIFIED));

			e.printStackTrace();
		} catch (final NullPointerException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		}
	}

	/**
	 * Check the JSONObject, if all necessary attributes are given and if some given
	 * values are correct.
	 *
	 * @param jsonObject
	 * @return
	 */
	private boolean isValidJsonStructure(JSONObject jsonObject, SearchManagement searchManagement) throws JSONException {
		logger.info("Validate the JSON structure.");

		// validate object type
		if (!JsonUtil.validObjectType(jsonObject, logger, session)) {
			logger.severe("At least one object type is invalid. Please check the JSON file.");
			return false;
		}

		// validate object group
		if (!JsonUtil.validObjectGroups(JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_CHILDREN), logger,
				searchManagement, session)) {
			logger.severe("At least one object group is invalid or missing. Please check the JSON file.");
			return false;
		}

		// validate pseudo serial number
		if (!JsonUtil.validPseudoSerialNumber(jsonObject, logger)) {
			logger.severe("At least one pseudoSerialNumber is invalid or missing. Please check the JSON file.");
			return false;
		}

		return true;
	}

	/**
	 * Get the Settings.
	 *
	 * @return
	 */
	public ConfigProperties getSettings() {
		return settings;
	}

	/**
	 * Add all datasets in the corresponding directory to the given StructureObject
	 * and delete the original files.
	 *
	 * @param structureObject
	 * @param attribute
	 */
	private void addDatasetsToStructure(StructureObject structureObject, String jsonFileName) {
		logger.info(String.format("Add all datasets to the %s object %s that are given in the dataset folder.",
				structureObject.getItemType(), structureObject.getDisplayString()));

		final com.teamcenter.services.strong.core._2006_03.FileManagement fileManagement = FileManagementService
				.getService(session.getConnection());
		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

		// Get the dataset files
		final String filename = currentQueueElement.getFilename().substring(0,
				currentQueueElement.getFilename().lastIndexOf("."));
		final String datasetFolder = getSettings().getTransactionFolder() + File.separator
				+ TcConstants.FOLDER_DATASET + File.separator + filename;

		final List<File> datasetFileList = FileManagement
				.getFilesFromFolder(datasetFolder, logger);
		final List<Dataset> datasetList = new ArrayList<>();

		// The dataset folder is empty or does not exist
		if (datasetFileList.isEmpty()) {
			logger.info("There are no dataset files to upload.");
			return;
		}

		if (structureObject.getItemRevision() == null) {
			try {
				structureObject.setItemRevision((ItemRevision) structureObject.getBomLine().get_bl_revision());
			} catch (final NotLoadedException e) {
				logger.severe(e.getMessage());

				e.printStackTrace();
			}
		}

		final List<DatasetProperties2> datasetPropertiesList = createDatasetPropertyList(structureObject,
				datasetFileList, datasetList, dataManagementService);

		// Change from a list to an array
		final DatasetProperties2[] datasetPropertiesArray = new DatasetProperties2[datasetPropertiesList.size()];
		for (int i = 0; i < datasetPropertiesList.size(); i++) {
			datasetPropertiesArray[i] = datasetPropertiesList.get(i);
		}

		logger.info("Create the empty dataset objects in Teamcenter.");
		final CreateDatasetsResponse createDatasetsResponse = dataManagementService
				.createDatasets2(datasetPropertiesArray);
		if (serviceDataError(createDatasetsResponse.serviceData, logger)) {
			return;
		}

		final GetDatasetWriteTicketsInputData[] inputDataArray = createInputDataArray(datasetList,
				datasetPropertiesArray, createDatasetsResponse);

		logger.info("Create the dataset tickets.");
		final GetDatasetWriteTicketsResponse datasetWriteTickesResponse = fileManagement
				.getDatasetWriteTickets(inputDataArray);

		if (serviceDataError(datasetWriteTickesResponse.serviceData, logger)) {
			return;
		}

		// Upload the dataset data
		if (!datasetList.isEmpty() && !uploadDatasetData(datasetList, datasetWriteTickesResponse)) {
			return;
		}

		final CommitDatasetFileInfo[] commitInfo = datasetWriteTickesResponse.commitInfo;
		for (int i = 0; i < commitInfo.length; i++) {
			commitInfo[i].createNewVersion = false;
		}

		logger.info("Add the uploaded dataset data to the dataset objects in Teamcenter.");
		final ServiceData commitDatasetFilesResponse = fileManagement
				.commitDatasetFiles(datasetWriteTickesResponse.commitInfo);

		if (serviceDataError(commitDatasetFilesResponse, logger)) {
			return;
		}

		logger.info("The dataset upload was successful.");

		// Delete all imported files
		FileManagement.deleteDatasets(datasetList, logger);
		logger.info("All files were deleted.");
	}

	/**
	 * Upload the dataset data to Teamcenter.
	 *
	 * @param datasetList
	 * @param datasetWriteTickesResponse
	 * @return
	 */
	private boolean uploadDatasetData(final List<Dataset> datasetList,
			final GetDatasetWriteTicketsResponse datasetWriteTickesResponse) {
		logger.info("Upload the data of the dataset files to Teamcenter.");

		try {
			// Get the Bootstrap URL
			final com.teamcenter.services.strong.administration.PreferenceManagementService prefMgmtService = com.teamcenter.services.strong.administration.PreferenceManagementService
					.getService(session.getConnection());
			final GetPreferencesResponse tcprefResponse = prefMgmtService
					.getPreferences(new String[] { TcConstants.TEAMCENTER_FMS_BOOTSTRAP_URL }, true);
			final String bootStrapURL = tcprefResponse.response[0].values.values[0];

			final FileManagementUtility fileManagementUtility = new FileManagementUtility(session.getConnection(), null,
					null, new String[] { bootStrapURL }, datasetList.get(0).getDatasetFile().getParent());

			// Upload all datasets
			for (int i = 0; i < datasetWriteTickesResponse.commitInfo.length; i++) {
				final String ticket = datasetWriteTickesResponse.commitInfo[i].datasetFileTicketInfos[0].ticket;
				final ErrorStack err = fileManagementUtility.putFileViaTicket(ticket,
						datasetList.get(i).getDatasetFile());
				if (err != null) {
					logger.severe(Arrays.toString(err.getErrorValues()));
					fileManagementUtility.term();
					return false;
				}
			}

			fileManagementUtility.term();
			return true;
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			logger.severe(e.getMessage());
		}

		return false;
	}

	/**
	 * Create the GetDatasetWriteTicketsInputData array with all necessary
	 * informations of the given datasets.
	 *
	 * @param datasetList
	 * @param datasetPropertiesArray
	 * @param createDatasetsResponse
	 * @return
	 */
	private GetDatasetWriteTicketsInputData[] createInputDataArray(final List<Dataset> datasetList,
			final DatasetProperties2[] datasetPropertiesArray, final CreateDatasetsResponse createDatasetsResponse) {
		logger.info("Create the InputDataArray with informations about the datasets.");

		final GetDatasetWriteTicketsInputData[] inputDataArray = new GetDatasetWriteTicketsInputData[datasetPropertiesArray.length];
		GetDatasetWriteTicketsInputData inputData;
		for (int i = 0; i < datasetPropertiesArray.length; i++) {
			logger.info(
					String.format("Add the info of the file %s to an empty dataset.", datasetList.get(i).getName()));

			// Information from the dataset
			final DatasetFileInfo[] datasetFileInfo = new DatasetFileInfo[1];
			datasetFileInfo[0] = new DatasetFileInfo();
			datasetFileInfo[0].fileName = datasetList.get(i).getDatasetFile().getName();
			datasetFileInfo[0].namedReferencedName = datasetList.get(i).getReferencedName();
			datasetFileInfo[0].allowReplace = false;
			datasetFileInfo[0].clientId = String.valueOf(i);
			datasetFileInfo[0].isText = datasetPropertiesArray[i].name.contains(".json"); // Only json files are
			// text

			// The dataset object that should get the new file
			inputData = new GetDatasetWriteTicketsInputData();
			inputData.datasetFileInfos = datasetFileInfo;
			inputData.dataset = createDatasetsResponse.output[i].dataset;
			inputData.createNewVersion = true;

			inputDataArray[i] = inputData;
		}
		return inputDataArray;
	}

	/**
	 * Create a List of DatasetProperties for the given datasets to define their
	 * properties.
	 *
	 * @param structureObject
	 * @param datasetFileList
	 * @param datasetList
	 * @param dataManagementService
	 * @return
	 */
	private List<DatasetProperties2> createDatasetPropertyList(StructureObject structureObject,
			final List<File> datasetFileList, final List<Dataset> datasetList,
			final DataManagementService dataManagementService) {
		logger.info(
				"Iterate through all given dataset files and create the dataset property list for all valid data types.");

		Dataset dataset;
		DatasetProperties2 datasetProperties;
		final List<DatasetProperties2> datasetPropertiesList = new ArrayList<>();

		final DatasetInfo datasetInfo = new DatasetInfo();
		logger.severe(datasetInfo.getDatasetFileLocation());
		int id = 0;
		for (int i = 0; i < datasetFileList.size(); i++) {
			final File file = datasetFileList.get(i);
			logger.info(String.format("Select the file %s.", file.getName()));

			dataset = new Dataset(file);
			final String[] datasetInfoArray = datasetInfo
					.getDatasetInfoFromExtension(dataset.getExtension().toLowerCase());

			// Ignore extensions that aren't defined
			if (datasetInfoArray == null || datasetInfoArray.length < 3) {
				logger.info(String.format("The file %s can't be uploaded, because the extension isn't defined.",
						file.getName()));
				importStatistic.addNotImportedDatasetFiles(file.getName());
				continue;
			}

			// Set the dataset values
			dataset.setDatasetType(datasetInfoArray[0].trim());
			dataset.setReferencedName(datasetInfoArray[1].trim());
			dataset.setRelationType(datasetInfoArray[2].trim());

			datasetList.add(dataset);

			// Get the tool and dataset name
			final GetDatasetCreationRelatedInfoResponse2 resp2 = dataManagementService
					.getDatasetCreationRelatedInfo2(dataset.getDatasetType(), structureObject.getItemRevision());

			if (serviceDataError(resp2.serviceData, logger)) {
				importStatistic.addNotImportedDatasetFiles(file.getName());
				continue;
//				return new ArrayList<>();
			}

			dataset.setDatasetTool(resp2.toolNames[0]);

			datasetProperties = new DatasetProperties2();
			datasetProperties.toolUsed = dataset.getDatasetTool();
			datasetProperties.name = dataset.getName();
			datasetProperties.relationType = dataset.getRelationType();
			datasetProperties.type = dataset.getDatasetType();

			datasetProperties.container = structureObject.getItemRevision();
			datasetProperties.clientId = String.valueOf(id++);
			datasetPropertiesList.add(datasetProperties);
		}

		return datasetPropertiesList;
	}

	/**
	 * Recursively iterate through all StructureObjects and add the JSONObjects of
	 * their child objects.
	 *
	 * @param structureObject
	 */
	private void updateJsonObjects(StructureObject structureObject) throws JSONException {
		logger.info(String.format("Add the JSON children objects to %s.", structureObject.getDisplayString()));

		if (!structureObject.getJsonObject().has(TcConstants.JSON_VARIANT_RULES)) {
			final JSONArray childrenArray = new JSONArray();
			for (final StructureObject childObject : structureObject.getChildren()) {
				updateJsonObjects(childObject);
				childrenArray.put(childObject.getJsonObject());
//				logger.severe(childObject.getJsonObject().optString(TcConstants.JSON_OBJECT_NAME) + ", "
//						+ childObject.getJsonObject().optString(TcConstants.JSON_ACAD_HANDLE));
			}

			structureObject.getJsonObject().put(TcConstants.JSON_CHILDREN, childrenArray);
		}
	}

	private void updateJsonFile(StructureObject structureObject) {
//		importHandler.updateJsonFile(currentQueueElement, structureObject, session);
	}

	/**
	 * Print the import statistic, close all handlers and maybe set the flag to
	 * allow the next import.
	 *
	 * @param setFlag True, if the next import is allowed to start
	 */
	private void endImport(boolean setFlag) {
		logger.info(importStatistic.getLogStatistics());

		if (setFlag) {
//			importHandler.setFlag(true);
		}

		// Close all handlers and the logger
		for (int i = logger.getHandlers().length - 1; i >= 0; i--) {
			logger.getHandlers()[i].close();
		}
	}

	/**
	 * Change the properties of all objects of the structure below the given
	 * ModularBuilding to the values given in the JSON file.
	 *
	 * @param searchManagement
	 * @param modularBuilding
	 */
	private void changeProperties(final SearchManagement searchManagement, final ModularBuilding modularBuilding) {
		logger.info("Change the properties from all objects of the current structure.");
		importStatistic.setStartEditProperties();

		// Change the properties
		//final ChangeManagement changeManagement = new ChangeManagement(this,  session, settings);
		changePropertiesOfStructure2(modularBuilding.getBomLine(), modularBuilding, searchManagement);

		// If either the structure was revised or created entirely new
		if (structureMustBeRevised || structureWasCreated) {

			// Add all duplicated structures
			addDuplicatedStructures(modularBuilding, changeManagement);
		}

		importStatistic.setEndEditProperties();
		importStatistic.setEndImportTime();
	}

	/**
	 * Search and create the structure from the given JSONObject.
	 *
	 * @param json
	 * @param searchManagement
	 * @param similarStructuresMap
	 * @return
	 */
	private ModularBuilding searchAndCreateStructure(JSONObject json, final SearchManagement searchManagement,
			final Map<JSONObject, List<JSONObject>> similarStructuresMap) throws JSONException {
		logger.info("Search and create the structure from the JSON file.");
		importStatistic.setStartCreateStructure();

		final ModularBuilding modularBuilding = new ModularBuilding(json, null, 0);
		importStatistic.setDrawingNumber(modularBuilding.getDesignNo());

		// Search for the root object by its object ID if it is given.
		if (!modularBuilding.getJsonObjectID().isEmpty()) {
			modularBuilding.setItem(
					searchManagement.searchObject(modularBuilding.getItemType(), modularBuilding.getJsonObjectID()));
		}

		// If the objectID wasn't given or no object with that ID exists
		if (modularBuilding.getItem() == null) {
			try {
				// Search for an object with the given designNo
				modularBuilding.setItemRevision(searchManagement.searchObjectByDesignNo(modularBuilding.getItemType(),
						modularBuilding.getDesignNo()));

				modularBuilding.setItem(
						modularBuilding.getItemRevision() != null ? modularBuilding.getItemRevision().get_items_tag()
								: null);

			} catch (final NotLoadedException e) {
				logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

				e.printStackTrace();
			}
		}

		// If the root object was found
		if (modularBuilding.getItem() != null) {
			// The existing structure may already exist
			ItemRevision existingItemRevision = null;

			modularBuilding.setWasCreated(false);

			if (!structureMustBeRevised) {
				final List<JSONObject> jsonChildrenList = JsonUtil
						.getListFromJsonArray(JsonUtil.getJsonArray(json, TcConstants.JSON_CHILDREN));

				existingItemRevision = searchManagement.searchRevision(modularBuilding.getItem(), jsonChildrenList);
				// TODO: Create the StructureObjects for the found structure
			}

			// Create a new revision if none was found
			if (existingItemRevision == null) {
				logger.info("Create a new revision of the modular building.");

				structureWasCreated = true;
				importStatistic.setWasRevised(true);

				existingItemRevision = reviseObject(modularBuilding.getItem());
				modularBuilding.setWasCreated(true);

				modularBuilding.setItemRevision(existingItemRevision);
				modularBuilding.setBomLine(getBomLine(existingItemRevision, session));

				modularBuilding.setDisplayString(getDisplayString(existingItemRevision));
				createChildrenFromJson(similarStructuresMap, modularBuilding, searchManagement);
			} else {
				logger.info("Use an already existing revision of the modular building.");
				importStatistic.setStructureExisted(true);

				modularBuilding.setItemRevision(existingItemRevision);
				modularBuilding.setBomLine(getBomLine(existingItemRevision, session));

				modularBuilding.setDisplayString(getDisplayString(existingItemRevision));
			}
		} else {

			structureWasCreated = true;
			modularBuilding.setWasCreated(true);
			try {
				logger.info("Create the structure entirely new.");

				// Create a new structure
				modularBuilding
						.setItem(createObject(modularBuilding.getObjectName(), modularBuilding.getObjectDescription(),
								modularBuilding.getItemType(), modularBuilding.getJsonRevisionID()));

//				DataManagementService.getService(session.getConnection()).getProperties(
//						new ModelObject[] { modularBuilding.getItem() },
//						new String[] { TcConstants.TEAMCENTER_ITEM_REVISION, TcConstants.TEAMCENTER_OBJECT_STRING });

				modularBuilding.setBomLine(getBomLine(modularBuilding.getItem(), session));

				modularBuilding.setItemRevision((ItemRevision) modularBuilding.getItem().get_item_revision());
			} catch (final NotLoadedException e) {

				logger.severe(e.getMessage());
				e.printStackTrace();
			} catch (final NullPointerException e) {

				logger.severe(e.getMessage());
				e.printStackTrace();
			}

			// Create all children and add them to the root object
			createChildrenFromJson(similarStructuresMap, modularBuilding, searchManagement);
		}

		importStatistic.setEndCreateStructure();
		return modularBuilding;
	}

	/**
	 * Create all solution variants that are described in the given JSONObject.
	 *
	 * @param searchManagement
	 * @param revisionRuleName
	 * @return
	 */
	private boolean createSolutionVariants(SearchManagement searchManagement, String revisionRuleName) {
		logger.info(String.format("Create all solution variants that are described in the given JSON."));
		importStatistic.setStartSolutionVariants();

		try {
		// The number of parallel threads you want
		int parallelism = Optional.ofNullable(getSettings().getSearchParallel())
				.map(Integer::parseInt)
				.orElse(5);



		AtomicBoolean hasError = new AtomicBoolean(false);

		// Create an ExecutorService with a fixed thread pool
		ExecutorService executorService = Executors.newFixedThreadPool(parallelism);

		// Your Semaphore to control parallelism
		Semaphore concurrentySemaphore = new Semaphore(parallelism);

		// Your solutionVariantMap
		// Map<SolutionVariant, List<StructureObject>> solutionVariantMap = ...

		// List to hold CompletableFuture tasks
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (StructureObject solutionVariant : solutionVariantMap.keySet()) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					// Acquire a permit from the semaphore
					concurrentySemaphore.acquire();

					// Your solutionVariantManagement.searchAndCreateSolutionVariant logic here
					logger.info("call SVs");

//					final ClassificationManagement classificationManagement = new ClassificationManagement(logger, this, session);
					final SolutionVariantManagement solutionVariantManagement = new SolutionVariantManagement(logger,
							classificationManagement, searchManagement, this, session);

					solutionVariant.setHasParent(!solutionVariant.getParentList().isEmpty());

					// Check if a generic object ID is given
					if (solutionVariant.getProperties().get(TcConstants.JSON_GENERIC_OBJECT_ID).isBlank()) {
						logger.severe(String.format(
								"The object %s either has no genericObjectID given or the attribute is empty.",
								JsonUtil.getAttribute(solutionVariant.getJsonObject(), TcConstants.JSON_OBJECT_NAME)));
						importStatistic.setEndSolutionVariants();
						hasError.set(true);
						return;
					}

					((SolutionVariant) solutionVariant).setSolutionVariantCategory(
							getSolutionVariantCategoryFromString(JsonUtil.getAttribute(solutionVariant.getJsonObject(),
									TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)));


					solutionVariantManagement.searchAndCreateSolutionVariant((SolutionVariant) solutionVariant, revisionRuleName);
					if (solutionVariant.getBomLine() == null) {
						logger.severe(String.format("solutionVariant.getBomLine() is null"));
						hasError.set(true);
					}

					if (solutionVariant.getBomLine() == null) {
						logger.severe(String.format(
								"Couldn't create a solution variant for the object %s. Please check the JSON file or the generic object %s for errors.",
								JsonUtil.getAttribute(solutionVariant.getJsonObject(), TcConstants.JSON_OBJECT_NAME),
								((SolutionVariant) solutionVariant).getGenericObjectID()));
						importStatistic.setEndSolutionVariants();
						hasError.set(true);
					}

					if (getSettings().isAlwaysClassify()) {
						//					classificationManagement.classify(solutionVariant, searchManagement);
					}

					// Iterate through all similar solution variants
					for (final StructureObject similarSolutionVariant : solutionVariantMap.get(solutionVariant)) {
						similarSolutionVariant.setBomLine(solutionVariant.getBomLine());
						similarSolutionVariant.setWasCreated(solutionVariant.wasCreated());

						((SolutionVariant) similarSolutionVariant).setSolutionVariantCategory(
								((SolutionVariant) solutionVariant).getSolutionVariantCategory());
						((SolutionVariant) similarSolutionVariant)
								.setGenericBomLine(((SolutionVariant) solutionVariant).getGenericBomLine());

					}


				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println(e.getMessage());
				} finally {
					// Release the permit
					concurrentySemaphore.release();
				}
			}, executorService);

			futures.add(future);
		}

		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		try {
			allOf.get(); // Wait for all asynchronous tasks to complete
		} catch (InterruptedException | ExecutionException e) {
			logger.severe(e.getMessage());
		}
		finally {
			// Shutdown the ExecutorService when done
			executorService.shutdown();
		}

		if (hasError.get()) {
			// Handle errors if any solution variant failed
			logger.severe(String.format("Couldn't create solution variants."));
			return false;
		}









			/*for (final StructureObject solutionVariant : solutionVariantMap.keySet()) {
				solutionVariant.setHasParent(!solutionVariant.getParentList().isEmpty());

				// Check if a generic object ID is given
				if (solutionVariant.getProperties().get(TcConstants.JSON_GENERIC_OBJECT_ID).isBlank()) {
					logger.severe(String.format(
							"The object %s either has no genericObjectID given or the attribute is empty.",
							JsonUtil.getAttribute(solutionVariant.getJsonObject(), TcConstants.JSON_OBJECT_NAME)));
					importStatistic.setEndSolutionVariants();
					return false;
				}

				((SolutionVariant) solutionVariant).setSolutionVariantCategory(
						getSolutionVariantCategoryFromString(JsonUtil.getAttribute(solutionVariant.getJsonObject(),
								TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)));

				// Try to search or create the solution variant
				solutionVariantManagement.searchAndCreateSolutionVariant((SolutionVariant) solutionVariant,
						revisionRuleName);

				// Cancel the import if at least one solution variant could not be created.
				if (solutionVariant.getBomLine() == null) {
					logger.severe(String.format(
							"Couldn't create a solution variant for the object %s. Please check the JSON file or the generic object %s for errors.",
							JsonUtil.getAttribute(solutionVariant.getJsonObject(), TcConstants.JSON_OBJECT_NAME),
							((SolutionVariant) solutionVariant).getGenericObjectID()));
					importStatistic.setEndSolutionVariants();
					return false;
				}

				if (getSettings().isAlwaysClassify()) {
//					classificationManagement.classify(solutionVariant, searchManagement);
				}

				// Iterate through all similar solution variants
				for (final StructureObject similarSolutionVariant : solutionVariantMap.get(solutionVariant)) {
					similarSolutionVariant.setBomLine(solutionVariant.getBomLine());
					similarSolutionVariant.setWasCreated(solutionVariant.wasCreated());

					((SolutionVariant) similarSolutionVariant).setSolutionVariantCategory(
							((SolutionVariant) solutionVariant).getSolutionVariantCategory());
					((SolutionVariant) similarSolutionVariant)
							.setGenericBomLine(((SolutionVariant) solutionVariant).getGenericBomLine());

				}
			}*/


//			AtomicBoolean hasError = new AtomicBoolean(false);
//
//			Semaphore concurrentySemaphore = new Semaphore(Integer.parseInt(getSettings().getSearchParallel()));
//
//			List<CompletableFuture<Void>> futures = solutionVariantMap.keySet().stream()
//					.map(solutionVariant -> CompletableFuture.runAsync(() -> {
//
//						try {
//							concurrentySemaphore.acquire();
//							logger.info("call SVs");
//
//							final ClassificationManagement classificationManagement = new ClassificationManagement(logger, this, session);
//							final SolutionVariantManagement solutionVariantManagement = new SolutionVariantManagement(logger,
//									classificationManagement, searchManagement, this, session);
//
//							solutionVariant.setHasParent(!solutionVariant.getParentList().isEmpty());
//
//							// Check if a generic object ID is given
//							if (solutionVariant.getProperties().get(TcConstants.JSON_GENERIC_OBJECT_ID).isBlank()) {
//								logger.severe(String.format(
//										"The object %s either has no genericObjectID given or the attribute is empty.",
//										JsonUtil.getAttribute(solutionVariant.getJsonObject(), TcConstants.JSON_OBJECT_NAME)));
//								importStatistic.setEndSolutionVariants();
//								hasError.set(true);
//								return;
//							}
//
//							((SolutionVariant) solutionVariant).setSolutionVariantCategory(
//									getSolutionVariantCategoryFromString(JsonUtil.getAttribute(solutionVariant.getJsonObject(),
//											TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)));
//
//
//							solutionVariantManagement.searchAndCreateSolutionVariant((SolutionVariant) solutionVariant, revisionRuleName);
//							if (solutionVariant.getBomLine() == null) {
//								logger.severe(String.format("solutionVariant.getBomLine() is null"));
//								hasError.set(true);
//							}
//
//							if (solutionVariant.getBomLine() == null) {
//								logger.severe(String.format(
//										"Couldn't create a solution variant for the object %s. Please check the JSON file or the generic object %s for errors.",
//										JsonUtil.getAttribute(solutionVariant.getJsonObject(), TcConstants.JSON_OBJECT_NAME),
//										((SolutionVariant) solutionVariant).getGenericObjectID()));
//								importStatistic.setEndSolutionVariants();
//								hasError.set(true);
//							}
//
//							if (getSettings().isAlwaysClassify()) {
//			//					classificationManagement.classify(solutionVariant, searchManagement);
//							}
//
//							// Iterate through all similar solution variants
//							for (final StructureObject similarSolutionVariant : solutionVariantMap.get(solutionVariant)) {
//								similarSolutionVariant.setBomLine(solutionVariant.getBomLine());
//								similarSolutionVariant.setWasCreated(solutionVariant.wasCreated());
//
//								((SolutionVariant) similarSolutionVariant).setSolutionVariantCategory(
//										((SolutionVariant) solutionVariant).getSolutionVariantCategory());
//								((SolutionVariant) similarSolutionVariant)
//										.setGenericBomLine(((SolutionVariant) solutionVariant).getGenericBomLine());
//
//							}
//
//						} catch (InterruptedException e) {
//							Thread.currentThread().interrupt();
//							logger.severe(e.getMessage());
//						}
//						finally {
//							concurrentySemaphore.release();
//						}
//
//					}))
//					.collect(Collectors.toList());
//
//			CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//
//			try {
//				allOf.get(); // Wait for all asynchronous tasks to complete
//			} catch (InterruptedException | ExecutionException e) {
//				logger.severe(e.getMessage());
//			}
//
//			if (hasError.get()) {
//				// Handle errors if any solution variant failed
//				logger.severe(String.format("Couldn't create solution variants."));
//				return false;
//			}

			// Continue with the rest of your logic
			// ...




			logger.info(String.format("All solution variants were successfully found or created."));
			importStatistic.setEndSolutionVariants();

			return true;
		} catch (final NullPointerException e) {
			logger.severe(String.format("Couldn't create all solution variants."));

			e.printStackTrace();
		}

		logger.severe(String.format("Not all solution variants were successsfully found or created."));
		importStatistic.setEndSolutionVariants();
		return true;
	}

	private boolean createSolutionVariantObjects(JSONObject json) throws JSONException {
		importStatistic.setStartSolutionVariants();

		final Set<JSONObject> solutionVariantJsonSet = new HashSet<>();
		solutionVariantJsonSet.addAll(JsonUtil.getSolutionVariants(json));

		// Get all solution variants
//		if (!getAllSolutionVariants(solutionVariantJsonSet, solutionVariantList,
//				JsonUtil.getAttribute(json, TcConstants.JSON_REVISION_RULE),
//				JsonUtil.getAttribute(json, TcConstants.JSON_RELEASE_STATUS))) {
//
//			// If at least one solution variant configuration wasn't valid
//			importStatistic.setEndSolutionVariants();
//			importStatistic.setEndImportTime();
//
//			return false;
//		}

		importStatistic.setEndSolutionVariants();
		return true;
	}

	/**
	 * Create a Map in which the keys are different substructures and the
	 * corresponding Lists consist of equal substructures with minor (position)
	 * changes.
	 *
	 * @param solutionVariantJsonSet
	 * @return
	 */
	private Map<JSONObject, List<JSONObject>> removeDuplicates(Set<JSONObject> solutionVariantJsonSet) {
		logger.info(String.format("Remove all duplicate JSON solution variants from the given list."));

		final Map<JSONObject, List<JSONObject>> reducedJsonMap = new HashMap<>();
		final List<JSONObject> removedObjects = new ArrayList<>();
		List<JSONObject> tempList;

		for (final JSONObject json : solutionVariantJsonSet) {
			tempList = new ArrayList<>();

			// ignore JSONObjects that are known duplicates
			if (removedObjects.contains(json)) {
				continue;
			}

			// Search for duplicates of this JSONObject and remove them
			for (final JSONObject comparingJson : solutionVariantJsonSet) {

				// ignore the same JSON and all that already were used
				if (json.equals(comparingJson) || removedObjects.contains(comparingJson)) {
					continue;
				}

				// Is this JSONObject a duplicate?
				if (JsonUtil.similarSolutionVariants(json, comparingJson, logger)) {
					tempList.add(comparingJson);
					removedObjects.add(comparingJson);
				}
			}

			reducedJsonMap.put(json, tempList);
			removedObjects.add(json);
		}
//		logger.severe(String.format("Amount of solution variant objects: %d.", reducedJsonMap.size()));
		return reducedJsonMap;
	}

	/**
	 * Create container objects and their substructures.
	 *
	 * @param searchManagement
	 * @param similarStructuresMap
	 * @param parentObject
	 */
	private void createChildrenFromJson(final Map<JSONObject, List<JSONObject>> similarStructuresMap,
			StructureObject parentObject, SearchManagement searchManagement) {
		logger.info("Create the Container objects and their substructures");

		// Create the Container objects and their substructures
		for (final JSONObject jsonObject : similarStructuresMap.keySet()) {
//			Container container = new Container(jsonObject, parentObject);
//			container.setHasParent(true);
//			container.setReleaseStatus(parentObject.getReleaseStatus());

			// Add the similar structures to the container object
			for (final JSONObject j : similarStructuresMap.get(jsonObject)) {
//				container.addSimilarStructure(j);
			}

			// Create the structure
//			container = (Container) buildStructure(jsonObject, container, searchManagement);
//			parentObject.addChild(container);
//			addChildrenToBOMLine(parentObject, container);
		}
	}

	/**
	 * Add the given child StructureObject to the parent StructureObject.
	 *
	 * @param parentObject
	 * @param childObject
	 */
	private BOMLine addChildrenToBOMLine(StructureObject parentObject, final StructureObject childObject) {
		logger.info(String.format("Add the child %s to the parent object %s.", childObject.getDisplayString(),
				parentObject.getDisplayString()));

		final BOMLine bomline = parentObject.getBomLine() == null ? getBomLine(parentObject.getItemRevision(), session)
				: parentObject.getBomLine();

		if (childObject.getDisplayString().isBlank() || parentObject.getDisplayString().isBlank()) {
			logger.severe("At least one object does not exist.");
			return null;
		}

		final Map<String, String> map = new HashMap<>();
		if (childObject.getProperties().get(TcConstants.JSON_FIND_NO) != null) {
			map.put(TcConstants.TEAMCENTER_FIND_NO, childObject.getProperties().get(TcConstants.JSON_FIND_NO));
		}

		if (childObject.getProperties().containsKey(TcConstants.JSON_QUANTITY)) {
			map.put(TcConstants.TEAMCENTER_QUANTITY, childObject.getProperties().get(TcConstants.JSON_QUANTITY));
		}

		if (childObject.getProperties().get(TcConstants.JSON_POSITION_DESIGNATOR) != null) {
			map.put(TcConstants.TEAMCENTER_POSITION_DESIGNATOR,
					childObject.getProperties().get(TcConstants.JSON_POSITION_DESIGNATOR));
		}

		if (!childObject.getProperties().get(TcConstants.JSON_COORDINATES).isBlank()
				&& !childObject.getProperties().get(TcConstants.JSON_ROTATION).isBlank()) {
			final String matrix = MatrixManagement.calculateTransformationMatrix(
					childObject.getProperties().get(TcConstants.JSON_COORDINATES),
					childObject.getProperties().get(TcConstants.JSON_ROTATION), getDisplayString(bomline), logger);

			map.put(TcConstants.TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX, matrix);
		}

		if (childObject.getItemType().equals(TcConstants.TEAMCENTER_CABIN)) {
			map.put(TcConstants.TEAMCENTER_PSEUDO_SERIAL_NUMBER,
					childObject.getProperties().get(TcConstants.JSON_PSEUDO_SERIAL_NUMBER));
		}
//		
		// The BOMLine to be added
		final AddInformation addInformation = new AddInformation();
		addInformation.line = childObject.getBomLine();
		addInformation.initialValues = map;

		// Set the parameter for add
		final AddParam addParam = new AddParam();
		addParam.flags = 6;
		addParam.parent = bomline;
		addParam.toBeAdded = new AddInformation[] { addInformation };

		// Add the BOMLine to the parent BOMLine
		final StructureService structureService = StructureRestBindingStub.getService(session.getConnection());
		final AddResponse addResponse = structureService.add(new AddParam[] { addParam });

		// Did an error occur?
		if (!serviceDataError(addResponse.serviceData, logger)) {
			try {
				saveBOMLine(bomline, session);

				return (BOMLine) addResponse.addedLines[0];
			} catch (final NotLoadedException e) {
				logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

				e.printStackTrace();
			}
		}

		return null;
	}


	public void addChildrenToBOMLineWithTiming(StructureObject parentObject, final StructureObject childObject) {
		// Record the start time
		long startTime = System.currentTimeMillis();


		// Call the original function
		addChildrenToBOMLine(parentObject, childObject);

		// Record the end time
		long endTime = System.currentTimeMillis();

		// Calculate and log the execution time
		long executionTime = (endTime - startTime) / 1000;

		importStatistic.setTimeAddChildrenToBomline(executionTime);
	}


	/**
	 * Create and search all solution variants from the given JSONObjects and add
	 * them to the solutionVariantList.
	 *
	 * @param revisionRuleName
	 * @param solutionVariantJsonSet
	 * @param solutionVariantList
	 * @return
	 */
	private boolean getAllSolutionVariants(final Set<JSONObject> solutionVariantJsonSet,
			List<SolutionVariant> solutionVariantList, String revisionRuleName, String releaseStatus) {
		logger.info("Get all solution variants that are used in the given structure.");
		final SearchManagement searchManagement = new SearchManagement(logger, releaseStatus, session);
//		final ClassificationManagement classificationManagement = new ClassificationManagement(logger, this, session);

		final SolutionVariantManagement solutionVariantManagement = new SolutionVariantManagement(logger,
				classificationManagement, searchManagement, this, session);

//		String jsonSolutionVariantNames = "";
//		for (final JSONObject json : solutionVariantJsonSet) {
//			jsonSolutionVariantNames += JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME) + ", ";
//		}

		final Map<JSONObject, List<JSONObject>> reducedJsonMap = removeDuplicates(solutionVariantJsonSet);

//		String reducedJsonSolutionVariantNames = "";
//		for (final JSONObject json : reducedJsonMap.keySet()) {
//			reducedJsonSolutionVariantNames += JsonUtil.getAttribute(json, TcConstants.JSON_OBJECT_NAME) + ", ";
//		}
//		 Get or create all solution variants.
//		final Map<JSONObject, BOMLine> solutionVariantMap = new HashMap<>();

		try {
			for (final JSONObject solutionVariantJson : reducedJsonMap.keySet()) {

				final SolutionVariant solutionVariant = new SolutionVariant(solutionVariantJson, null, 0);
				solutionVariant.setReleaseStatus(releaseStatus);
				solutionVariant.setHasParent(true);
//				solutionVariant.addFamily(family);

				if (JsonUtil.getAttribute(solutionVariantJson, TcConstants.JSON_GENERIC_OBJECT_ID).isEmpty()) {
					logger.severe(String.format(
							"The object %s either has no genericObjectID given or the attribute is empty.",
							JsonUtil.getAttribute(solutionVariantJson, TcConstants.JSON_OBJECT_NAME)));
					return false;
				}

				solutionVariant.setSolutionVariantCategory(getSolutionVariantCategoryFromString(
						JsonUtil.getAttribute(solutionVariantJson, TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)));

				// Try to search or create the solution variant
				solutionVariantManagement.searchAndCreateSolutionVariant(solutionVariant, revisionRuleName);

				// Cancel the import if at least one solution variant could not be created.
				if (solutionVariant.getBomLine() == null) {
					logger.severe(String.format(
							"Couldn't create a solution variant for the object %s. Please check the JSON file or the generic object %s for errors.",
							JsonUtil.getAttribute(solutionVariantJson, TcConstants.JSON_OBJECT_NAME),
							solutionVariant.getGenericObjectID()));
					return false;
				}

				if (getSettings().isAlwaysClassify()) {
					// classificationManagement.classify(solutionVariant, searchManagement);
				}

				// Create a SolutionVariant object from the solution variant
				solutionVariantList.add(solutionVariant);

				// Add the solution variant and its corresponding JSONObject to the map
//			solutionVariantMap.put(solutionVariantJson, solutionVariant.getBomLine());

				// Create the duplicated solution variants
				for (final JSONObject duplicateJson : reducedJsonMap.get(solutionVariantJson)) {

					final SolutionVariant duplicateSolutionVariant = new SolutionVariant(duplicateJson, null, 0);

					duplicateSolutionVariant.setSolutionVariantCategory(getSolutionVariantCategoryFromString(
							JsonUtil.getAttribute(solutionVariantJson, TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)));

					duplicateSolutionVariant.setBomLine(solutionVariant.getBomLine());
					duplicateSolutionVariant.setGenericBomLine(solutionVariant.getGenericBomLine());
					duplicateSolutionVariant.setHasParent(true);

					duplicateSolutionVariant.setWasCreated(solutionVariant.wasCreated());
					// duplicateSolutionVariant.setWasFound(solutionVariant.wasFound());
					duplicateSolutionVariant.setReleaseStatus(releaseStatus);

					solutionVariantList.add(duplicateSolutionVariant);
				}
			}

			logger.info(String.format("All solution variants were successfully found or created."));
			return true;
		} catch (final NullPointerException e) {
			logger.severe(String.format("Couldn't create all solution variants."));

			e.printStackTrace();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		logger.severe(String.format("Not all solution variants were successsfully found or created."));
		return false;
	}

	/**
	 * Add all similar substructures to the given StructureObject and apply the
	 * minor changes they need.
	 *
	 * @param rootStructureObject
	 * @param changeManagement
	 */
	private void addDuplicatedStructures(StructureObject rootStructureObject, ChangeManagement changeManagement) {
		logger.info(String.format(
				"Add all duplicate structures to the root object %s and change their properties accordingly.",
				rootStructureObject.getDisplayString()));

		// For every Container object
		/*
		 * for (final StructureObject childStructureObject :
		 * rootStructureObject.getChildren()) { if (childStructureObject instanceof
		 * Container && ((Container) childStructureObject).getSimilarStructures().size()
		 * > 1) { final Container containerObject = (Container) childStructureObject;
		 *
		 * // For every similar object of containerObject for (final JSONObject
		 * similarJSONObject : containerObject.getSimilarStructures()) { if
		 * (similarJSONObject.similar(containerObject.getOldJsonObject())) { continue; }
		 *
		 * final BOMLine bomline = addChildrenToBOMLine(rootStructureObject,
		 * childStructureObject);
		 *
		 * // Only change the properties if the children could be added if (bomline !=
		 * null) { changeManagement.changeProperties(bomline, similarJSONObject); } } }
		 * }
		 */
	}

	/**
	 * Create a new Item with the given attributes.
	 *
	 * @param objectName
	 * @param objectDescription
	 * @param objectType
	 * @param revisionID
	 * @return
	 */
	@SafeVarargs
	private Item createObject(String objectName, String objectDescription, String objectType, String revisionID,
			Map<String, String>... optionalParams) {
		logger.info(String.format("Create a new %s object with the name %s.", objectType, objectName));

		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

		// We need one item ID with for the given object type
		final GenerateItemIdsAndInitialRevisionIdsProperties generateItemIdsAndInitials = new GenerateItemIdsAndInitialRevisionIdsProperties();
		generateItemIdsAndInitials.count = 1;
		generateItemIdsAndInitials.itemType = objectType;

		final GenerateItemIdsAndInitialRevisionIdsResponse responseId = dataManagementService
				.generateItemIdsAndInitialRevisionIds(
						new GenerateItemIdsAndInitialRevisionIdsProperties[] { generateItemIdsAndInitials });

		// Check if a new ID was created
		if (serviceDataError(responseId.serviceData, logger)) {
			return null;
		}

		// The return is a map of ItemIdsAndInitialRevisionIds keyed on the
		// 0-based index of requested IDs. Since we only asked for IDs for one
		// data type, the map key is '0'
		final BigInteger bIkey = new BigInteger("0");

		final Map<BigInteger, ItemIdsAndInitialRevisionIds[]> createdItemIDsMap = responseId.outputItemIdsAndInitialRevisionIds;
		final ItemIdsAndInitialRevisionIds[] myNewIds = createdItemIDsMap.get(bIkey);
		final String newId = myNewIds[0].newItemId;

		final List<ItemProperties> itemPropertiesList = new ArrayList<>();

		// Get all properties for the item creation
		final ItemProperties itemProperties = new ItemProperties();
		itemProperties.clientId = "";
		itemProperties.description = objectDescription;
		itemProperties.name = objectName;
		itemProperties.revId = revisionID.isBlank() ? myNewIds[0].newRevId : revisionID;
		itemProperties.type = objectType;
		itemProperties.itemId = newId;

		itemPropertiesList.add(itemProperties);

		final CreateItemsResponse response = dataManagementService
				.createItems(itemPropertiesList.toArray(new ItemProperties[0]), null, itemProperties.type);

		// Check if an error occurred while creating the item
		if (serviceDataError(response.serviceData, logger)) {
			return null;
		}
		final Item item = response.output[0].item;
		logger.info(String.format("Created the %s object %s.", objectType, getDisplayString(item)));

		if (optionalParams.length > 0) {

			final ItemRevision itemRevision = Utility.getItemRevision(item);
			for (final Map<String, String> paramMap : optionalParams) {
				for (final Map.Entry<String, String> entry : paramMap.entrySet()) {
					final String key = entry.getKey();
					final String value = entry.getValue();
					logger.info(String.format("Set Parameter %s key with the value %s.", key, value));
					Utility.setProperty(key, value, itemRevision, session);

				}
			}

		}

		return item;
	}

	/**
	 * Recursively search for an already existing substructure and create it if none
	 * was found.
	 *
	 * @param jsonChild
	 * @return
	 */
	private StructureObject buildStructure(JSONObject jsonChild, StructureObject currentStructureObject,
			SearchManagement searchManagement) throws JSONException {
		logger.info(String.format("Search or create the object %s for building the structure.",
				currentStructureObject.getDisplayString()));

		// The object we try to get
		BOMLine bomline = null;

		// Check if the the object type and the name are given
		if (!jsonChild.has(TcConstants.JSON_OBJECT_TYPE) || !jsonChild.has(TcConstants.JSON_OBJECT_NAME)) {
			logger.severe(String.format("The current JSONObject is missing the object name and/or the type."));
			return currentStructureObject;
		}

		// Return the correct SolutionVariant object
		if (jsonChild.has(TcConstants.JSON_VARIANT_RULES)) {
			for (final StructureObject solutionVariant : solutionVariantList) {
				if (solutionVariant.getOldJsonObject().equals(jsonChild)) {
//					currentStructureObject = solutionVariant;
					return solutionVariant;
				}
			}
		}

		// Search for the object if an ID is given.
		if (!JsonUtil.getAttribute(jsonChild, TcConstants.JSON_OBJECT_ID).isEmpty()) {
			final Item item = searchManagement.searchObject(
					JsonUtil.getAttribute(jsonChild, TcConstants.JSON_OBJECT_TYPE),
					JsonUtil.getAttribute(jsonChild, TcConstants.JSON_OBJECT_ID));

			bomline = getBomLine(item, session);
//			currentStructureObject.setWasFound(true);
		}

		// Can we reuse an existing structure?
		if (jsonChild.has(TcConstants.JSON_CHILDREN) && !JsonUtil.mustCreateStructure(jsonChild)) {
			bomline = searchManagement.searchExistingStructure(jsonChild, importStatistic);

			// Return the StructureObject if a structure was found
			if (bomline != null) {
				currentStructureObject.setBomLine(bomline);
//				currentStructureObject.setWasFound(true);

				return currentStructureObject;
			}
		}

		// Create the BOMLine if it wasn't found yet
		if (bomline == null) {
			bomline = getBomLine(createObject(JsonUtil.getAttribute(jsonChild, TcConstants.JSON_OBJECT_NAME),
					JsonUtil.getAttribute(jsonChild, TcConstants.JSON_OBJECT_DESCRIPTION),
					JsonUtil.getAttribute(jsonChild, TcConstants.JSON_OBJECT_TYPE),
					JsonUtil.getAttribute(jsonChild, TcConstants.JSON_REVISION_ID)), session);
			currentStructureObject.setWasCreated(true);
		}

		// Add the BOMLine to the StructureObject
		currentStructureObject.setBomLine(bomline);
//		currentStructureObject.setDisplayString(getDisplayString(bomline));

		// Iterate through the children
		if (jsonChild.has(TcConstants.JSON_CHILDREN)) {
			for (final JSONObject childJson : JsonUtil
					.getListFromJsonArray(JsonUtil.getJsonArray(jsonChild, TcConstants.JSON_CHILDREN))) {
				StructureObject child = null;

				// Decide on the type
				if (childJson.has(TcConstants.JSON_VARIANT_RULES)) {
//					child = new SolutionVariant(childJson);
				} else {
//					child = new com.parsa.sbomimporter.data.Item(childJson);
				}

				child.setReleaseStatus(currentStructureObject.getReleaseStatus());
				child.setHasParent(true);
				child = buildStructure(childJson, child, searchManagement);
				currentStructureObject.addChild(child);
				addChildrenToBOMLineWithTiming(currentStructureObject, child);
			}
		}

		return currentStructureObject;
	}

	/**
	 * Reserve Revision IDs
	 *
	 * @param items Array of Items to reserve IDs for
	 * @return Map of RevisionIds
	 */
	private Map<BigInteger, RevisionIds> generateRevisionIds(ModelObject[] items) {
		// Get the service stub
		final DataManagementService dmService = DataManagementService.getService(session.getConnection());

		GenerateRevisionIdsResponse response = null;
		GenerateRevisionIdsProperties[] input = null;
		input = new GenerateRevisionIdsProperties[items.length];

		for (int i = 0; i < items.length; i++) {
			final GenerateRevisionIdsProperties property = new GenerateRevisionIdsProperties();
			property.item = items[i];
			property.itemType = "";
			input[i] = property;
		}

		// *****************************
		// Execute the service operation
		// *****************************
		response = dmService.generateRevisionIds(input);

		// The AppXPartialErrorListener is logging the partial errors returned
		// In this simple example if any partial errors occur we will throw a
		// ServiceException
		if (!serviceDataError(response.serviceData, logger)) {
			return response.outputRevisionIds;
		}

		return new HashMap<>();
	}

	/**
	 * Create a new revision of the given Item.
	 *
	 * @param item
	 * @return
	 */
	private ItemRevision reviseObject(Item item) {
		logger.info(String.format("Create a new revision on the object %s.", getDisplayString(item)));
		final DataManagementService dmService = DataManagementService.getService(session.getConnection());

		try {
			final ModelObject[] revisionList = item.get_revision_list();
			final ItemRevision latestRevision = (ItemRevision) revisionList[revisionList.length - 1];
			final ReviseInfo[] reviseInfo = new ReviseInfo[1];
			final RevisionIds rev = generateRevisionIds(new ModelObject[] { item }).get(new BigInteger("0"));

//			dmService.getProperties(new ModelObject[] { item }, new String[] { TcConstants.TEAMCENTER_OBJECT_NAME });

			reviseInfo[0] = new ReviseInfo();
			reviseInfo[0].baseItemRevision = latestRevision;
			reviseInfo[0].clientId = latestRevision.getUid();
			reviseInfo[0].name = item.get_object_name();
			reviseInfo[0].newRevId = rev.newRevId;

			final ReviseResponse2 revised = dmService.revise2(reviseInfo);

			if (!serviceDataError(revised.serviceData, logger)) {
				for (final ReviseOutput itemRev : revised.reviseOutputMap.values()) {
					final BOMLine bomline = getBomLine(itemRev.newItemRev, session);

					cutChildren(bomline);
					saveBOMLine(bomline, session);
					return itemRev.newItemRev;
				}
			}

		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Cut the children from the BOMLine.
	 *
	 * @param bom
	 */
	private void cutChildren(BOMLine bom) {
		logger.info(String.format("Cut the children from the new revision of %s.", getDisplayString(bom)));

		final StructureManagement structureManagement = StructureManagementService.getService(session.getConnection());

		try {
//			dmService.getProperties(new ModelObject[] { bom },
//					new String[] { TcConstants.TEAMCENTER_BOMLINE_CHILDREN });
			final ModelObject[] children = bom.get_bl_child_lines();
			final BOMLine[] bomChildren = new BOMLine[children.length];

			if (bomChildren.length > 0) {
				for (int i = 0; i < children.length; i++) {
					bomChildren[i] = (BOMLine) children[i];
				}

				final RemoveChildrenFromParentLineResponse response = structureManagement
						.removeChildrenFromParentLine(bomChildren);
				serviceDataError(response.serviceData, logger);
			}
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}
	}

	/**
	 * Set the successful classification value to the import statistic.
	 *
	 * @param bool
	 * @param objectName
	 */
	public void setSuccessfulClassification(boolean bool, String objectName) {
		importStatistic.setSuccessfulClassification(bool);
		importStatistic.addInvalidClassification(objectName);
	}

	public ImportStatistic getImportStatistic() {
		return importStatistic;
	}


	public void changeSingleObjectProperties(BOMLine bomline, StructureObject structureObject,
											 SearchManagement searchManagement) {
		logger.info(String.format("Change the properties of the object %s.", structureObject.getDisplayString()));

		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

//		dataManagementService.getProperties(new ModelObject[] { bomline },
//				new String[] { TcConstants.TEAMCENTER_BOMLINE_ALL_WORKFLOWS });

		try {
			final Map<String, String> propertyMap = structureObject.getProperties();
			final Map<String, DataManagement.VecStruct> propertiesToSet = new HashMap<>();

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
				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_OBJECT_DESCRIPTION);

				// If we have an objectGroupID given
				if (!isEmptyOrNull(propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID))) {

					// If the object doesn't already have the correct object group
					if (!searchManagement.hasCorrectObjectGroup((ItemRevision) bomline.get_bl_revision(),
							propertyMap.get(TcConstants.JSON_OBJECT_GROUP_ID))) {
						changeManagement.setObjectGroup(searchManagement, bomline, propertyMap);
					}
				}

				// Set the properties
				final ServiceData setPropertiesResponse = dataManagementService
						.setProperties(new ModelObject[] { bomline }, propertiesToSet);

				if (serviceDataError(setPropertiesResponse, logger)) {
					return;
				}

				// Classify the object, if attributes are given and it's not a solution variant
				if (settings.isAlwaysClassify() && changeManagement.canClassify(structureObject)) {
					boolean isSuccessClassified = classificationManagement.classify(structureObject, searchManagement);
					if(!isSuccessClassified)
						setSuccessfulClassification(false, structureObject.getDisplayString());
				}
			}

			// Call the workflow if the object has no release status yet and was just
			// created
			if (!structureObject.getWorkflow().isBlank()) {
				changeManagement.addWorkflowAsync(bomline, structureObject.getWorkflow());
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
	}

	/**
	 * Recursively change the properties of all objects of a structure. Which
	 * properties will change depends on the attributes given in the JSONObject.
	 *
	 * @param searchManagement
	 * @param bomline
	 */
	public void changePropertiesOfStructure2(BOMLine bomline, StructureObject structureObject,
											 SearchManagement searchManagement) {
		logger.info(String.format("Change the properties of %s.", structureObject.getDisplayString()));

		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());

		dataManagementService.getProperties(new ModelObject[] { bomline },
				new String[] { TcConstants.TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST });

		try {
			final Map<String, String> propertyMap = structureObject.getProperties();
			final Map<String, DataManagement.VecStruct> propertiesToSet = new HashMap<>();

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

				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_POSITION_DESIGNATOR);
				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_FIND_NO);
				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_QUANTITY);

				// If we have a rotation and coordinates given
				if (!isEmptyOrNull(propertyMap.get(TcConstants.JSON_ROTATION))
						&& !isEmptyOrNull(propertyMap.get(TcConstants.JSON_COORDINATES))) {
					logger.info(String.format("The object %s has coordinates and a rotation given.",
							structureObject.getDisplayString()));
					final DataManagement.VecStruct vecStruct = new DataManagement.VecStruct();

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
						changeManagement.setObjectGroup(searchManagement, bomline, propertyMap);
					}
				}

				// Change the description if any was given
				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_OBJECT_DESCRIPTION);

				// Set the properties
				final ServiceData setPropertiesResponse = dataManagementService
						.setProperties(new ModelObject[] { bomline }, propertiesToSet);

				if (serviceDataError(setPropertiesResponse, logger)) {
					return;
				}

				// Classify the object, if attributes are given and it's not a solution variant
				if (settings.isAlwaysClassify() && changeManagement.canClassify(structureObject)
						&& !(structureObject instanceof SolutionVariant)) {
					boolean isSuccessClassified = classificationManagement.classify(structureObject, searchManagement);
					if(!isSuccessClassified)
						setSuccessfulClassification(false, structureObject.getDisplayString());
				}

				// If the StructureObject has children
				if (structureObject.getChildren().size() > 0) {
					iterateChildren(bomline, structureObject, searchManagement, dataManagementService);
				}
			} else {
				logger.info(String.format("The current object is a Modular Building."));

				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_DESIGN_NO);
				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_DESIGNER);
				changeManagement.addPropertyToSet(propertyMap, propertiesToSet, TcConstants.JSON_OBJECT_DESCRIPTION);

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
				changeManagement.addWorkflowAsync(bomline, structureObject.getWorkflow());
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
	}

	/**
	 *
	 *
	 * @param bomline
	 * @param structureObject
	 * @param searchManagement
	 * @param dataManagementService
	 * @throws NotLoadedException
	 */
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

			final StructureObject childObject = changeManagement.getCorrectChild(structureChildren, bomChild);

			if (childObject == null) {
				logger.severe(String.format("Couldn't find a StructureObject for the BOMLine child %s.",
						getDisplayString(bomChild)));
				saveBOMLine(bomline, session);
				return;
			}

			changePropertiesOfStructure2(bomChild, childObject, searchManagement);
		}
	}

	public boolean isTaskCancelled(int taskId) {
		Optional<QueueEntity> queueEntityOptional = queueRepository.findById(taskId);
		return queueEntityOptional.map(queueEntity -> queueEntity.getCurrentStatus().equals(ImportStatus.CANCELED.toString())).orElse(false);
	}
}
