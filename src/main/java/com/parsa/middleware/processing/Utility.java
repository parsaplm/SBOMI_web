package com.parsa.middleware.processing;

import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.session.AppXSession;
import com.teamcenter.services.loose.core.DataManagementService;
import com.teamcenter.services.loose.core._2007_01.DataManagement.VecStruct;
import com.teamcenter.services.loose.workflow.WorkflowService;
import com.teamcenter.services.loose.workflow._2008_06.Workflow.ContextData;
import com.teamcenter.services.loose.workflow._2008_06.Workflow.InstanceInfo;
import com.teamcenter.services.strong.cad.StructureManagementRestBindingStub;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse;
import com.teamcenter.services.strong.cad._2019_06.StructureManagement.CreateWindowsInfo3;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.teamcenter.soa.internal.client.model.ModelObjectImpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * Has all methods that are used in multiple classes.
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 * 
 */
public abstract class Utility {

	/**
	 * Change the char set from the given text to the given char set. Works for
	 * 'UTF-8' and 'ISO-8859-1'.
	 * 
	 * @param text
	 * @param charSet
	 * @return
	 */
	public static String changeCharSet(String text, String charSet) {
// TODO:
		return "";
	}

	/**
	 * Get the BOMLine of the latest ItemRevision from the given Item.
	 * 
	 * @param item
	 * @return The latest ItemRevision from item.
	 */
	public static BOMLine getBomLine(Item item, AppXSession session) {
		try {
//			final DataManagementService dataManagementService = DataManagementService
//					.getService(AppXSession.getConnection());
//			dataManagementService.getProperties(new ModelObject[] { item }, new String[] { "revision_list" });
			final ModelObject[] revisionList = item.get_revision_list();

			final ItemRevision itemRevision = (ItemRevision) revisionList[revisionList.length - 1];
			return getBomLine(itemRevision, session);
		} catch (final NotLoadedException e) {
			e.printStackTrace();
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get the ItemRevision of the from the given Item.
	 *
	 * @param item
	 * @return The latest ItemRevision from item.
	 */
	public static ItemRevision getItemRevision(Item item) {
		try {
//			dataManagementService.getProperties(new ModelObject[] { item }, new String[] { "revision_list" });
			final ModelObject[] revisionList = item.get_revision_list();

			return ((ItemRevision) revisionList[revisionList.length - 1]);
		} catch (final NotLoadedException e) {
			e.printStackTrace();
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get the BOMLine of the given ItemRevision.
	 * 
	 * @param itemRevision
	 * @return The BOMLine from itemRevision.
	 */
	public static BOMLine getBomLine(ItemRevision itemRevision, AppXSession session) {
		final StructureManagementService structureManagementService = StructureManagementService
				.getService(session.getConnection());
		try {

			final CreateWindowsInfo3 createWindowsInfo = new CreateWindowsInfo3();
			createWindowsInfo.itemRev = itemRevision;
			createWindowsInfo.item = itemRevision.get_items_tag();

			final CreateBOMWindowsResponse bomWindowsResponse = structureManagementService
					.createOrReConfigureBOMWindows(new CreateWindowsInfo3[] { createWindowsInfo });

			final BOMLine bomlineObject = bomWindowsResponse.output[0].bomLine;

			return bomlineObject;
		} catch (final NotLoadedException e) {
			e.printStackTrace();
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get the BOMWindow from the given BOMLine.
	 * 
	 * @param bomLine
	 * @return
	 */
	public static BOMWindow getBOMWindow(BOMLine bomLine) {
//		final DataManagementService dataManagementService = DataManagementService
//				.getService(AppXSession.getConnection());

		try {
//			dataManagementService.getProperties(new com.teamcenter.soa.client.model.ModelObject[] { bomLine },
//					new String[] { "bl_window" });
			return (BOMWindow) bomLine.get_bl_window();
		} catch (final NotLoadedException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Get the displayable name from the given BOMLine.
	 * 
	 * @param bomLine
	 * @return
	 */
	public static String getDisplayString(ModelObject modelObject) {
		try {
//			dataManagementService.getProperties(new ModelObject[] { modelObject }, new String[] { "object_string" });
			return modelObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_OBJECT_STRING);
		} catch (final NotLoadedException e) {
			e.printStackTrace();
		} catch (final NullPointerException e) {
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * Get the revision rule from the given BOMLine.
	 * 
	 * @param bomLine
	 * @return
	 */
	public static RevisionRule getRevisionRule(BOMLine bomline, Logger logger) {
		try {
//			dataManagementService.getProperties(new com.teamcenter.soa.client.model.ModelObject[] { bomline },
//					new String[] { "bl_window" });
			final BOMWindow bomWindow = (BOMWindow) bomline.get_bl_window();
			return (RevisionRule) bomWindow.get_revision_rule();
		} catch (final NotLoadedException e) {
			logger.severe(String.format("At least one property wasn't properly loaded.\n%s", e.getMessage()));

			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Return the Teamcenter value for the given solution variant category. The
	 * values are: Reuse = 2, Managed = 1, Unmanaged = 0, None given = -1.
	 * 
	 * @param category A String value of a solution variant category.
	 * @return The Teamcenter value for the given solution variant category
	 */
	public static int getSolutionVariantCategoryFromString(String category) {
//		logger.info("Get the solution variant category value from the given String.");

		// None given = -1, Unmanaged = 0, Managed = 1, Reuse = 2
		switch (category) {
		case "Reuse":
			return 2;
		case "Managed":
			return 1;
		case "Unmanaged":
			return 0;
		case "":
			return -1;
		default:
//			logger.severe(String.format("The solution variant category \"%s\" is invalid.", category));
			return -2;
		}
	}

	/**
	 * Create a time stamp of the current time. The given values are: u = year, M =
	 * month, d = day, h = hour, m = minute, s = second.
	 * 
	 * @return the time stamp in the format uuuu-MM-dd hh-mm-ss
	 */
	public static String getTimeStamp() {
		final Instant instant = Instant.now();

		final ZoneId zoneID = ZoneId.of("Europe/Berlin");
		final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneID);

		final String timeStamp = zonedDateTime.format(DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss"));
		return timeStamp;
	}

	/**
	 * Check if the given String has content.
	 * 
	 * @param string
	 * @return true if string is empty or null
	 */
	public static boolean isEmptyOrNull(String string) {
		return string == null || string.isEmpty();
	}

	/**
	 * Save the given BOMLine.
	 * 
	 * @param bomline
	 * @throws NotLoadedException
	 */
	public static void saveBOMLine(final BOMLine bomline, AppXSession session) throws NotLoadedException {
//		final DataManagementService dmService = DataManagementService.getService(AppXSession.getConnection());
		final StructureManagementService structureManagement = StructureManagementRestBindingStub
				.getService(session.getConnection());

//		dmService.getProperties(new ModelObject[] { bomline }, new String[] { "bl_window" });
		final BOMWindow window = (BOMWindow) bomline.get_bl_window();

		structureManagement.saveBOMWindows(new BOMWindow[] { window });
	}

	public static void closeBOMLines(BOMLine[] bomlineArray, AppXSession session) {
		final StructureManagementService structureManagement = StructureManagementRestBindingStub
				.getService(session.getConnection());

		final BOMWindow[] bomWindows = new BOMWindow[bomlineArray.length];

		try {
			for (int i = 0; i < bomlineArray.length; i++) {
				bomWindows[i] = (BOMWindow) bomlineArray[i].get_bl_window();
			}
		} catch (final NotLoadedException e) {
			e.printStackTrace();
		}

		structureManagement.closeBOMWindows(bomWindows);
	}

	/**
	 * Print the error message, if a service request failed.
	 * 
	 * @param serviceData The service data of a service request
	 * @return true, if the service request succeeded
	 */
	public static boolean serviceDataError(ServiceData serviceData, Logger logger) {
		if (serviceData.sizeOfPartialErrors() > 0) {
			String output = "";

			for (int i = 0; i < serviceData.sizeOfPartialErrors(); i++) {
				for (final String msg : serviceData.getPartialError(i).getMessages()) {
					output += msg + "\n";
				}
			}

			logger.severe(output);

			return true;
		}
		return false;
	}

	/**
	 * Set the given property values to the given ModelObject.
	 * 
	 * @param propertyNames
	 * @param propertyValues
	 * @param modelObject
	 */
	public static void setProperties(String[] propertyNames, String[] propertyValues, ModelObject modelObject,
			AppXSession session) {
		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());
		final Map<String, VecStruct> propertiesMap = new HashMap<>();

		for (int i = 0; i < propertyNames.length; i++) {
			final VecStruct vecStruct = new VecStruct();
			vecStruct.stringVec = new String[] { propertyValues[i] };

			propertiesMap.put(propertyNames[i], vecStruct);
		}

		dataManagementService.setProperties(new ModelObject[] { modelObject }, propertiesMap);

	}

	/**
	 * Set the given property value to the given ModelObject.
	 * 
	 * @param propertyName
	 * @param propertyValue
	 * @param modelObject
	 */
	public static void setProperty(String propertyName, String propertyValue, ModelObject modelObject,
			AppXSession session) {
		final DataManagementService dataManagementService = DataManagementService.getService(session.getConnection());
		final Map<String, VecStruct> propertiesMap = new HashMap<>();
		final VecStruct vecStruct = new VecStruct();
		vecStruct.stringVec = new String[] { propertyValue };

		propertiesMap.put(propertyName, vecStruct);
		dataManagementService.setProperties(new ModelObject[] { modelObject }, propertiesMap);
	}

	/**
	 * Get the configurator context from the given BOMLine. It is mainly used to get
	 * the storage class of a solution variant.
	 * 
	 * @param bomLine
	 * @return The configurator context object. Null if an error occurred.
	 */
	public ModelObject getConfigContext(BOMLine bomLine) throws NullPointerException {
		try {
			final ModelObject item = bomLine.get_bl_item();
//			dataManagementService.getProperties(new ModelObject[] { item },
//					new String[] { "Smc0HasVariantConfigContext" });

			//return item.getPropertyObject("Smc0HasVariantConfigContext").getModelObjectArrayValue()[0];
				// Get the property value
			ModelObject[] configContextArray = item.getPropertyObject("Smc0HasVariantConfigContext").getModelObjectArrayValue();

			// Check if the array is not empty
			if (configContextArray != null && configContextArray.length > 0) {
				return configContextArray[0]; // Return the first element of the array
			}

		} catch (final NotLoadedException e) {
			e.printStackTrace();
		} catch (final ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get the configurator perspective from the given BOMLine. It is mainly used to
	 * create solution variants.
	 * 
	 * @param bomLine
	 * @return The configurator context object. Null if an error occurred.
	 */
	public ModelObject getConfigPerspective(BOMLine bomLine) throws NullPointerException {
		final BOMWindow window = getBOMWindow(bomLine);
//		dataManagementService.getProperties(new ModelObject[] { window }, new String[] { "smc0ConfigPerspective" });

		final Hashtable<String, Property> properties = ((ModelObjectImpl) window).copyProperties();
		final Property property = properties.get("smc0ConfigPerspective");

		return property.getModelObjectValue();
	}

	/**
	 * Add a workflow with the given name to the given ModelObject.
	 * 
	 * @param modelObject
	 * @param workflowName
	 */
	public void addWorkflow(ModelObject modelObject, String workflowName, Logger logger, AppXSession session) {
		logger.info(String.format("Add the workflow %s to the given Item Revision.", workflowName));

		final WorkflowService workflowService = WorkflowService.getService(session.getConnection());
		try {
			if (workflowName.isBlank()) {
				logger.info("Couldn't add a workflow. No workflow name was given.");
				return;
			}

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

			final InstanceInfo instanceInfoResponse = workflowService.createInstance(true, null, workflowName, null,
					"generate a workflow", contextData);

			if (serviceDataError(instanceInfoResponse.serviceData, logger)) {
				return;
			}
		} catch (final NotLoadedException e) {
			logger.severe(e.getMessage());

			e.printStackTrace();
		}
	}

	/**
	 * Converts a string to an integer. If the conversion fails, the default value
	 * is returned.
	 *
	 * @param str          the string to convert
	 * @param defaultValue the default value to return if the conversion fails
	 * @return the integer value of the string, or the default value if the
	 *         conversion fails
	 */
	public static int convertToInt(String str, int defaultValue) {
		try {
			return Integer.parseInt(str);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	public static String extractIpAddress(String url) {
		// Regex pattern to match IP address
		final String regex = "(?<=://)([\\w.-]+)(?=:)";
		String result = "";
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(url);

		if (matcher.find()) {
			final String ipAddress = matcher.group();
			result = ipAddress;
		} else {
			result = "No IP address found.";
		}

		return result;
	}

	public static String getClientIpAddress() {
		try {
			final InetAddress ipAddress = InetAddress.getLocalHost();
			final String ipAddressString = ipAddress.getHostAddress();
			return ipAddressString;
		} catch (final UnknownHostException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static String getHostName() {
		try {
			// Get the local host (current machine)
			final InetAddress localHost = InetAddress.getLocalHost();

			// Get the hostname
			final String hostname = localHost.getHostName();

			return hostname;
		} catch (final UnknownHostException e) {
			// Handle any exceptions that may occur
			e.printStackTrace();
			return "Unknown";
		}
	}

}
