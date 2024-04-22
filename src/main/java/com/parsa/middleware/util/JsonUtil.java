package com.parsa.middleware.util;

import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.service.SearchManagement;
import com.parsa.middleware.session.AppXSession;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core.DataManagementService;
import com.teamcenter.services.loose.core._2008_06.DataManagement.BOWithExclusionIn;
import com.teamcenter.services.loose.core._2010_04.DataManagement.BusinessObjectHierarchy;
import com.teamcenter.services.loose.core._2010_04.DataManagement.DisplayableSubBusinessObjectsResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class that has all methods that are needed when using JSONObjects.
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public abstract class JsonUtil {

	/**
	 * Get the given attribute from the given JSONObject.
	 * 
	 * @param solutionVariantJson
	 * @param attribute
	 * @return
	 */
	public static String getAttribute(JSONObject solutionVariantJson, String attribute) {
		if (solutionVariantJson.has(attribute)) {
			return solutionVariantJson.optString(attribute);
		}

		return "";
	}

	/**
	 * Iterate through the two given JSONObjects and save the differences in a
	 * string.
	 * 
	 * @param logger
	 * @param oldJson
	 * @param newJson
	 * @return
	 */
	public static String getChanges(Logger logger, JSONObject oldJson, JSONObject newJson, LocalDateTime time) throws JSONException {
		logger.info(String.format("Compare two JSONObjects and return their differences."));

		final Map<String, String> oldJsonMap = jsonToMap(oldJson, logger);
		final Map<String, String> newJsonMap = jsonToMap(newJson, logger);

		String changes = "";

		// Add all changed values with a timestamp to the String
		for (final String attribute : oldJsonMap.keySet()) {
			if (!oldJsonMap.get(attribute).equals(newJsonMap.get(attribute))) {
				changes += String.format("[%s] The attribute %s changed from %s to %s.\n",
						time.format(DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss")), attribute,
						oldJsonMap.get(attribute), newJsonMap.get(attribute));
			}
		}

		return changes;
	}

	/**
	 * Count the occurrence of container objects in the JSON file.
	 * 
	 * @param json
	 * @return
	 */
	public static int getContainerCount(Logger logger, JSONObject json) throws JSONException {
		logger.info("Get the amount of container objects in the JSON file.");
		return countContainer(json);
	}

	/**
	 * Read the drawing number of the given JSON file.
	 * 
	 * @param json
	 * @return
	 */
	public static String getDrawingNumber(Logger logger, JSONObject json) {
		logger.info(String.format("Get the drawing number."));

		final String drawingNo = json.optString(TcConstants.JSON_DESIGN_NO);
		return drawingNo;
	}

	/**
	 * Get the task iD and the current status of the database entry from the
	 * JSONObject.
	 * 
	 * @param logger
	 * @param newJson
	 * @param time
	 * @return
	 */
	public static String getInsertLogEntry(Logger logger, JSONObject newJson, LocalDateTime time) {
		logger.info(String.format("Get the task ID and current status of the database entry."));

		final String changes = String.format("[%s] Created the task %s with the status %s.\n",
				time.format(DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss")),
				newJson.optString(TcConstants.DATABASE_TASK_ID),
				newJson.optString(TcConstants.DATABASE_CURRENT_STATUS));

		return changes;
	}

	/**
	 * Check if the given JSONObject has an attribute with the given name and return
	 * it as JSONArray.
	 * 
	 * @param solutionVariantJson A JSONObject
	 * @param attribute           The name of the desired JSONArray
	 * @return A JSONArray or null, if no JSONArray with the given name exists.
	 */
	public static JSONArray getJsonArray(JSONObject solutionVariantJson, String attribute) throws JSONException {
		if (solutionVariantJson.has(attribute)) {
			return solutionVariantJson.getJSONArray(attribute);
		}

		return new JSONArray();
	}

	/**
	 * Put all entries of JSONObjects from the given JSONArray into a List and
	 * return it.
	 * 
	 * @param jsonArray A JSONArray consisting of JSONObjects
	 * @return A List of all JSONObjects from jsonArray
	 */
	public static List<JSONObject> getListFromJsonArray(JSONArray jsonArray) throws JSONException {
		final List<JSONObject> jsonList = new ArrayList<>();

		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonList.add(jsonArray.getJSONObject(i));
			}

			return jsonList;
		}

		return new ArrayList<>();
	}

	/**
	 * Create a Map of family and feature IDs from the given JSONArray.
	 * 
	 * @param jsonArray
	 * @return
	 */
	public static Map<String, String> getSolutionVariantMapFromJsonArray(JSONArray jsonArray) throws JSONException {
		final Map<String, String> variantRuleMap = new HashMap<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject variantRule = jsonArray.getJSONObject(i);

			variantRuleMap.put(variantRule.getString(TcConstants.JSON_FAMILY_ID),
					variantRule.getString(TcConstants.JSON_FEATURE_ID));
		}

		return variantRuleMap;
	}

	/**
	 * Build recursively a HashMap that counts the amount of each variant rule in
	 * jsonObject and its children. The variant rules are saved as "familyID =
	 * featureID".
	 * 
	 * @param jsonObject       A JSONObject with either variant rules or with
	 *                         children (or children of children) that have variant
	 *                         rules
	 * @param jsonVariantRules The already saved variant rules.
	 * @return The HashMap jsonVariantRules with the summed up occurrences of each
	 *         variant rule.
	 */
	public static Map<String, Integer> getVariantRulesAsMap(JSONObject jsonObject,
			Map<String, Integer> jsonVariantRules) throws JSONException {

		// Call this method for all children
		if (jsonObject.has(TcConstants.JSON_CHILDREN)) {
			final JSONArray children = jsonObject.getJSONArray(TcConstants.JSON_CHILDREN);
			for (int i = 0; i < children.length(); i++) {
				jsonVariantRules = getVariantRulesAsMap(children.getJSONObject(i), jsonVariantRules);
			}
		}

		// Fill jsonVariantRules with all existing variant rules
		if (jsonObject.has(TcConstants.JSON_VARIANT_RULES)) {
			final JSONArray variantRules = jsonObject.getJSONArray(TcConstants.JSON_VARIANT_RULES);

			for (int i = 0; i < variantRules.length(); i++) {
				final JSONObject currentObject = variantRules.getJSONObject(i);

				String variantRule = "";
				if (currentObject.has(TcConstants.JSON_FAMILY_ID) && currentObject.has(TcConstants.JSON_FEATURE_ID)) {
					variantRule = currentObject.optString(TcConstants.JSON_FAMILY_ID) + " = "
							+ currentObject.optString(TcConstants.JSON_FEATURE_ID);
				}

				if (jsonVariantRules.containsKey(variantRule)) {
					// if the entry already exists, increment it
					jsonVariantRules.put(variantRule, jsonVariantRules.get(variantRule) + 1);
				} else {
					jsonVariantRules.put(variantRule, 1);
				}
			}
		}

		return jsonVariantRules;
	}

	/**
	 * Create a Map of classification names and their values from the given
	 * JSONArray.
	 * 
	 * @param jsonArray
	 * @return
	 */
	public static Map<String, String> getClassificationMapFromJsonArray(JSONArray jsonArray) throws JSONException {
		final Map<String, String> variantRuleMap = new HashMap<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject variantRule = jsonArray.getJSONObject(i);

			variantRuleMap.put(variantRule.getString(TcConstants.JSON_CLASSIFICATION_ID),
					variantRule.getString(TcConstants.JSON_CLASSIFICATION_VALUE));
		}

		return variantRuleMap;
	}

	/**
	 * Count the occurrence of objects in the JSON file.
	 * 
	 * @param logger
	 * @param json
	 * @return
	 */
	public static int getObjectsCount(Logger logger, JSONObject json) throws JSONException {
		logger.info("Get the amount of objects in the JSON file.");
		return countObjects(json);
	}

	/**
	 * Compare all JSONObjects on the level of jsonObject and count the similar
	 * JSONObjects.
	 * 
	 * @param jsonObject
	 * @return
	 */
	public static Map<JSONObject, List<JSONObject>> getSimilarObjects(JSONObject jsonObject, Logger logger) throws JSONException {
		logger.info("Compare all JSONObjects on the level of jsonObject and count the similar JSONObjects.");

		final Map<JSONObject, List<JSONObject>> similarJSONObjects = new HashMap<>();
		final Map<JSONObject, Integer> similarObjects = new HashMap<>();

		if (jsonObject.has(TcConstants.JSON_CHILDREN)) {
			final JSONArray children = jsonObject.getJSONArray(TcConstants.JSON_CHILDREN);

			for (int i = 0; i < children.length(); i++) {
				boolean hasSimilarObject = false;
				final JSONObject currentChild = children.getJSONObject(i);

				// put the first JSONObject into the HashMap and skip to the next JSONObject
				if (similarObjects.isEmpty()) {
					similarObjects.put(currentChild, 1);
					final List<JSONObject> jsonList = new ArrayList<>();
					jsonList.add(currentChild);
					similarJSONObjects.put(currentChild, jsonList);

					continue;
				}

				// Does a similar object already exist in the HashMap?
				for (final JSONObject currentObject : similarObjects.keySet()) {
					JSONObject tempCurrentObject = new JSONObject();
					tempCurrentObject = currentObject;
					JSONObject tempCurrentChild = new JSONObject();
					tempCurrentChild = currentChild;
					if (similar(tempCurrentObject, tempCurrentChild, logger)) {
						similarObjects.put(currentObject, similarObjects.get(currentObject) + 1);

						final List<JSONObject> jsonObjectList = similarJSONObjects.get(currentObject);
						jsonObjectList.add(currentChild);
						similarJSONObjects.put(currentObject, jsonObjectList);

						hasSimilarObject = true;
						break;
					}
				}

				// There was no similar object and the object isn't already in the HashMap
				if (!hasSimilarObject) {
					similarObjects.put(currentChild, 1);
					final List<JSONObject> jsonList = new ArrayList<>();
					jsonList.add(currentChild);
					similarJSONObjects.put(currentChild, jsonList);
				}
			}
		}

		// Print the hashmap
		String output = "";
		for (final JSONObject json : similarObjects.keySet()) {
			final String key = json.optString(TcConstants.JSON_OBJECT_NAME);
			final String value = Integer.toString(similarObjects.get(json));
			output += key + " = " + value + ", ";
		}

		logger.info("The different objects are: " + output);

		String output2 = "";
		for (final JSONObject json : similarJSONObjects.keySet()) {
			final String key = json.optString(TcConstants.JSON_OBJECT_NAME);
			final String value = Integer.toString(similarJSONObjects.get(json).size());
			output2 += key + " = " + value + ", ";
		}

		logger.info(output2);

		return similarJSONObjects;
	}

	/**
	 * Get all solution variant JSONObjects below the given JSONObject and return
	 * them in a list.
	 * 
	 * @param jsonObject
	 * @return
	 */
	public static List<JSONObject> getSolutionVariants(JSONObject jsonObject) throws JSONException {
		final List<JSONObject> solutionVariantList = new ArrayList<>();

		if (jsonObject.has(TcConstants.JSON_CHILDREN)) {
			findJsonSolutionVariants(jsonObject.getJSONArray(TcConstants.JSON_CHILDREN), solutionVariantList);
		}

		return solutionVariantList;
	}

	/**
	 * Create a HashMap out of the attributes of the given JSONObject.
	 * 
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	public static Map<String, String> jsonToMap(JSONObject json, Logger logger) throws JSONException {
		logger.info("Convert the given JSONObject to a HashMap<String, String>.");
		final Map<String, String> map = new HashMap<>();
		final Iterator<?> keys = json.keys();

		while (keys.hasNext()) {
			final String key = (String) keys.next();
			final String value = json.get(key).toString();
			map.put(key, value);
		}

		return map;
	}

	/**
	 * Map all entries of the given JSONArray 'childrenSolutionVariants' as objectID
	 * <-> solutionVariantCategory.
	 * 
	 * @param jsonArray The JSONArray 'childrenSolutionVariants'
	 * @return A Map with the solution variant category corresponding to each object
	 *         ID
	 */
	public static Map<String, String> mapChildrenSolutionVariantEntries(JSONArray jsonArray) throws JSONException {
		final Map<String, String> childSolutionVariantList = new HashMap<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject jsonObject = jsonArray.getJSONObject(i);

			childSolutionVariantList.put(jsonObject.optString(TcConstants.JSON_OBJECT_ID),
					jsonObject.optString(TcConstants.JSON_SOLUTION_VARIANT_CATEGORY));
		}
		return childSolutionVariantList;
	}

	/**
	 * Read the JSON file and return it as JSONObject.
	 * 
	 * @param jsonFile
	 * @return
	 */
	public static JSONObject readJsonFile(Logger logger, File jsonFile) {
		logger.info(String.format("Read the JSON file %s.", jsonFile.getName()));

		String jsonString = "";
		try {
			jsonString = new String(Files.readAllBytes(jsonFile.toPath()));
			final JSONObject rootObject = new JSONObject(jsonString);

			if (isValid(logger, jsonString)) {
				return rootObject;
			}
		} catch (final FileNotFoundException | NoSuchFileException e) {
			logger.log(Level.SEVERE, "JSON file couldn't be found.\n\n" + e.getMessage(), e);
//			e.printStackTrace();
		} catch (final IOException e) {
			logger.log(Level.SEVERE, "Couldn't read JSON file.\n\n" + e.getMessage(), e);
			e.printStackTrace();
		} catch (final JSONException e) {
			logger.log(Level.SEVERE, "The JSON file wasn't correct. Please check it for errors. \n\n" + e.getMessage(),
					e);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Determine if two JSONObjects are similar. They must contain the same set of
	 * names which must be associated with similar values.
	 *
	 * @param jsonObject The other JSONObject
	 * @return true if they are equal
	 */
	public static boolean similar(JSONObject jsonObject, JSONObject comparingObject, Logger logger) {
//		LOGGER.info("Compare two JSONObjects recursively, if they are at least similar.");
		try {
			final List<String> ignoredKeys = Arrays
					.asList(new String[] { TcConstants.JSON_REVISION_ID, TcConstants.JSON_OBJECT_DESCRIPTION,
							TcConstants.JSON_PSEUDO_SERIAL_NUMBER, TcConstants.JSON_POSITION_DESIGNATOR,
							TcConstants.JSON_OBJECT_NAME, TcConstants.JSON_ACAD_HANDLE, TcConstants.JSON_FIND_NO });

			final Set<String> set = (Set<String>) comparingObject.keys();

			// Do both objects have the same keys?
			if (!set.equals(jsonObject.keys())) {
				return false;
			}

			// Iterate through all keys
			final Iterator<String> iterator = set.iterator();
			while (iterator.hasNext()) {
				final String currentKey = iterator.next();

				// Ignore the Transformation matrix from Container
				if ((currentKey.equals(TcConstants.JSON_ROTATION) || currentKey.equals(TcConstants.JSON_COORDINATES))
						&& jsonObject.optString(TcConstants.JSON_OBJECT_TYPE).equals(TcConstants.TEAMCENTER_CABIN)) {
					continue;
				}

				// ignore the solution variant category if the object is no solution variant
				if (currentKey.equals(TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)
						&& !jsonObject.has(TcConstants.JSON_VARIANT_RULES)) {
					continue;
				}

				// Ignore all keys from the list
				if (ignoredKeys.contains(currentKey)) {
					continue;
				}

				final Object valueComparing = comparingObject.get(currentKey);
				final Object valueJson = jsonObject.get(currentKey);

				if (valueComparing instanceof JSONObject) {

					// Compare the JSONObjects
					if (!(similar((JSONObject) valueComparing, (JSONObject) valueJson, logger))) {
						return false;
					}
				} else if (valueComparing instanceof JSONArray) {

					final JSONArray jsonArrayOther = new JSONArray(valueJson.toString());

					// Compare every JSONObject in the JSONArray with every other JSONObject in the
					// other JSONArray
					for (int i = 0; i < ((JSONArray) valueComparing).length(); i++) {
						final JSONObject jsonObjectThis = new JSONObject(
								((JSONArray) valueComparing).getJSONObject(i).toString());

						for (int j = 0; j < jsonArrayOther.length(); j++) {
							if (similar(jsonArrayOther.getJSONObject(j), jsonObjectThis, logger)) {
								jsonArrayOther.remove(j);
								break;
							}
						}
					}

					// Return false, if the JSONArrays aren't similar
					if (jsonArrayOther.length() != 0) {
						return false;
					}
				} else if (!valueComparing.equals(valueJson)) {
					return false;
				}
			}

			return true;
		} catch (final JSONException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "There was an error when comparing JSONObjects.\n\n" + e.getMessage());
		}

		return false;
	}

	/**
	 * Determine if two solution variant JSONObjects are similar. They must contain
	 * the same set of names which must be associated with similar values.
	 *
	 * @param jsonObject The other JSONObject
	 * @return true if they are equal
	 */
	public static boolean similarSolutionVariants(JSONObject jsonObject, JSONObject comparingObject, Logger logger) {
//		LOGGER.info("Compare two JSONObjects recursively, if they are at least similar.");
		try {
			final List<String> notIgnoredKeys = Arrays
					.asList(new String[] { TcConstants.JSON_OBJECT_TYPE, TcConstants.JSON_GENERIC_OBJECT_ID,
							TcConstants.JSON_OBJECT_ID, TcConstants.JSON_SOLUTION_VARIANT_CATEGORY,
							TcConstants.JSON_VARIANT_RULES, TcConstants.JSON_FAMILY_ID, TcConstants.JSON_FEATURE_ID });

			final Set<String> set = (Set<String>) comparingObject.keys();

			// Do both objects have the same keys?
			if (!set.equals(jsonObject.keys())) {
				return false;
			}

			// Iterate through all keys
			final Iterator<String> iterator = set.iterator();
			while (iterator.hasNext()) {
				final String currentKey = iterator.next();

				// Ignore all keys that aren't in the list
				if (!notIgnoredKeys.contains(currentKey)) {
					continue;
				}

				final Object valueComparing = comparingObject.get(currentKey);
				final Object valueJson = jsonObject.get(currentKey);

				if (valueComparing instanceof JSONObject) {

					// Compare the JSONObjects
					if (!(similarSolutionVariants((JSONObject) valueComparing, (JSONObject) valueJson, logger))) {
						return false;
					}
				} else if (valueComparing instanceof JSONArray) {

					final JSONArray jsonArrayOther = new JSONArray(valueJson.toString());

					// The amount of solution variants should be the same
					if (((JSONArray) valueComparing).length() != jsonArrayOther.length()) {
						return false;
					}

					// Compare every JSONObject in the JSONArray with every other JSONObject in the
					// other JSONArray
					for (int i = 0; i < ((JSONArray) valueComparing).length(); i++) {
						final JSONObject jsonObjectThis = new JSONObject(
								((JSONArray) valueComparing).getJSONObject(i).toString());

						for (int j = 0; j < jsonArrayOther.length(); j++) {
							if (similarSolutionVariants(jsonArrayOther.getJSONObject(j), jsonObjectThis, logger)) {
								jsonArrayOther.remove(j);
								break;
							}
						}
					}

					// Return false, if the JSONArrays aren't similar
					if (jsonArrayOther.length() != 0) {
						return false;
					}
				} else if (!valueComparing.equals(valueJson)) {
					return false;
				}
			}

			return true;
		} catch (final JSONException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "There was an error when comparing JSONObjects.\n\n" + e.getMessage());
			return false;
		}
	}

	/**
	 * Validate if all JSONObjects in the given JSON have an object type given and
	 * the object types are all valid.
	 * 
	 * @param json
	 * @param logger
	 * @return
	 */
	public static boolean validObjectType(JSONObject json, Logger logger, AppXSession session) {
		logger.info(
				" Validate if all JSONObjects in the given JSON have an object type given and the object types are all valid.");

		try {
			// Get all object types
			final Set<String> objectTypeSet = new HashSet<>();
			getAllObjectTypes(json, objectTypeSet, logger);

			// TODO: rename variable
			// Get all available item types
			final BOWithExclusionIn[] bci = new BOWithExclusionIn[1];
			bci[0] = new BOWithExclusionIn();
			bci[0].boTypeName = "Item";

			final DisplayableSubBusinessObjectsResponse resp = DataManagementService.getService(session.getConnection())
					.findDisplayableSubBusinessObjectsWithDisplayNames(bci);

			if (Utility.serviceDataError(resp.serviceData, logger)) {
				return false;
			}

			final Map<String, String> objectTypeMap = new HashMap<>();

			// Put all values into a map to only iterate once through it
			for (final BusinessObjectHierarchy a : resp.output[0].displayableBOTypeNames) {
				objectTypeMap.put(a.boName, a.boDisplayName);
			}

			for (final String objectType : objectTypeSet) {
				if (!objectTypeMap.containsKey(objectType)) {
					// The object type does not exist
					return false;
				}
			}
		} catch (final ServiceException e) {
			logger.severe(String.format("There was a problem while getting the object types. \n%s", e.getMessage()));

			e.printStackTrace();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	/**
	 * Iterate through the JSON structure and add all object types to the given Set.
	 * Throw an error if no object type is given.
	 * 
	 * @param json
	 * @param objectTypeSet
	 * @param logger
	 */
	private static void getAllObjectTypes(JSONObject json, Set<String> objectTypeSet, Logger logger) throws JSONException {
		// Return false if either no object type is given or the given
		// object type is empty
		if (json.optString(TcConstants.JSON_OBJECT_TYPE).isBlank()) {
			logger.severe(String.format("The JSONObject %s is either missing the attribute objectType or it is empty.",
					json.optString(TcConstants.JSON_OBJECT_NAME)));
			return;
		}

		// Add the current object type to the set
		objectTypeSet.add(json.optString(TcConstants.JSON_OBJECT_TYPE));

		// Iterate through all children to get their object type
		if (json.has(TcConstants.JSON_CHILDREN)) {
			final JSONArray children = json.getJSONArray(TcConstants.JSON_CHILDREN);
			for (int i = 0; i < children.length(); i++) {
				final JSONObject child = children.getJSONObject(i);
				getAllObjectTypes(child, objectTypeSet, logger);
			}
		}
	}

	/**
	 * Validate if all given JSONObjects are valid solution variants.
	 * 
	 * @param jsonObject
	 * @param logger
	 * @return
	 */
	public static boolean validSolutionVariant(List<JSONObject> jsonObjectList, Logger logger) throws JSONException {
		logger.info("Validate if all given JSONObjects are valid solution variants.");
		boolean allValid = true;

		for (final JSONObject jsonObject : jsonObjectList) {

			// Check if children are given
			if (jsonObject.has(TcConstants.JSON_CHILDREN)
					&& jsonObject.getJSONArray(TcConstants.JSON_CHILDREN).length() != 0) {
				logger.warning(String.format(
						"The solution variant %s has children given. This is not intendet. Please check the JSON file.",
						jsonObject.optString(TcConstants.JSON_OBJECT_NAME)));
			}

			// Check if the generic object ID is given
			if (jsonObject.optString(TcConstants.JSON_GENERIC_OBJECT_ID).isBlank()) {
				logger.severe(String.format(
						"The solution variant %s does not have a generic object ID given. Please check the JSON file.",
						jsonObject.optString(TcConstants.JSON_OBJECT_NAME)));
				allValid = false;
			}

			// Check if all variant rules contain familyID and featureID
			final JSONArray variantRuleArray = jsonObject.getJSONArray(TcConstants.JSON_VARIANT_RULES);
			for (int i = 0; i < variantRuleArray.length(); i++) {
				final JSONObject variantRule = variantRuleArray.getJSONObject(i);

				if (!variantRule.has(TcConstants.JSON_FAMILY_ID) && !variantRule.has(TcConstants.JSON_FEATURE_ID)) {
					logger.severe(String.format(
							"At least one variant rule of the solution variant %s is not valid. It is missing either the familyID or the featureID. Please check the JSON file.",
							jsonObject.optString(TcConstants.JSON_OBJECT_NAME)));
					allValid = false;
				}
			}
		}

		return allValid;
	}

	/**
	 * Check if the given JSONArray has only valid object groups given.
	 * 
	 * @param jsonArray        A JSONArray of JSONObjects
	 * @param logger           The logger of the current Thread
	 * @param searchManagement
	 * @return
	 */
	public static boolean validObjectGroups(JSONArray jsonArray, Logger logger, SearchManagement searchManagement,
			AppXSession session) throws JSONException {
		logger.info("Check if the given JSONArray as only valid object groups given.");

		final Map<String, Set<String>> objectGroupMap = new HashMap<>();
		final DataManagementService dataManagement = DataManagementService.getService(session.getConnection());

		return getAllObjectGroupIDs(jsonArray, logger, objectGroupMap)
				? hasAllObjectGroupsGiven(searchManagement, objectGroupMap, dataManagement, logger)
				: false;
	}

	/**
	 * Validate that all children below the given JSONObject have the non-empty
	 * attribute 'pseudoSerialNumber'.
	 * 
	 * @param json
	 * @param logger
	 * @return
	 */
	public static boolean validPseudoSerialNumber(JSONObject json, Logger logger) throws JSONException {
		logger.info(String.format("Check all children from the JSONObject %s if they have a pseudoSerialNumber given.",
				json.optString(TcConstants.JSON_OBJECT_NAME)));

		if (json.has(TcConstants.JSON_CHILDREN)) {
			final JSONArray children = json.getJSONArray(TcConstants.JSON_CHILDREN);
			for (int i = 0; i < children.length(); i++) {
				final JSONObject child = children.getJSONObject(i);

				// Return false if either no pseudoSerialNumber is given or the given
				// pseudoSerialNumber is empty
				if (getAttribute(child, TcConstants.JSON_OBJECT_TYPE).equals(TcConstants.TEAMCENTER_CABIN)
						&& (!child.has(TcConstants.JSON_PSEUDO_SERIAL_NUMBER)
								|| child.optString(TcConstants.JSON_PSEUDO_SERIAL_NUMBER).isEmpty())) {
					logger.warning(String.format(
							"The JSONObject %s is either missing the attribute pseudoSerialNumber or it is empty.",
							child.optString(TcConstants.JSON_OBJECT_NAME)));
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Check the JSONObject and its children for a solution variant category that's
	 * not "Reuse".
	 * 
	 * @param jsonObject
	 * @return
	 */
	public static boolean mustCreateStructure(JSONObject jsonObject) throws JSONException {

		final List<JSONObject> solutionVariantList = getSolutionVariants(jsonObject);
		Map<String, String> childSolutionVariantMap = new HashMap<>();

		// Check the solution variants for non-Reuse categories
		for (final JSONObject solutionVariant : solutionVariantList) {

			if (Utility.getSolutionVariantCategoryFromString(
					solutionVariant.optString(TcConstants.JSON_SOLUTION_VARIANT_CATEGORY)) < 2) {
				return true;
			}

			if (solutionVariant.has(TcConstants.JSON_CHILD_SOLUTION_VARIANT_CATEGORIES)) {
				childSolutionVariantMap = mapChildrenSolutionVariantEntries(
						jsonObject.optJSONArray(TcConstants.JSON_CHILD_SOLUTION_VARIANT_CATEGORIES));

				// Check the children solution variants for non-Reuse categories
				for (final String objectID : childSolutionVariantMap.keySet()) {
					if (Utility.getSolutionVariantCategoryFromString(childSolutionVariantMap.get(objectID)) < 2) {
						return true;
					}
				}

			}
		}

		return false;
	}

	/**
	 * Iterate via depth-first search through the JSON file and count the number of
	 * Container objects in it.
	 * 
	 * @param jsonObject The JSONObject, of which the children will be count
	 * @return The amount of Container objects in the JSON file
	 */
	private static int countContainer(JSONObject jsonObject) throws JSONException {
		int amountContainer = jsonObject.optString(TcConstants.JSON_OBJECT_TYPE).contains(TcConstants.TEAMCENTER_CABIN)
				? 1
				: 0;

		if (jsonObject.has(TcConstants.JSON_CHILDREN)) {
			final JSONArray childrenOfObject = jsonObject.getJSONArray(TcConstants.JSON_CHILDREN);
			for (int i = 0; i < childrenOfObject.length(); i++) {
				amountContainer += countContainer(childrenOfObject.getJSONObject(i));
			}
		}

		return amountContainer;
	}

	/**
	 * Iterate via depth-first search through the JSON file and count the number of
	 * objects in it.
	 * 
	 * @param jsonObject The JSONObject, of which the children will be count
	 * @return The amount of objects in the JSON file
	 */
	private static int countObjects(JSONObject jsonObject) throws JSONException {
		int amountChildren = 1;
		if (jsonObject.has(TcConstants.JSON_CHILDREN)) {
			final JSONArray childrenOfObject = jsonObject.getJSONArray(TcConstants.JSON_CHILDREN);
			for (int i = 0; i < childrenOfObject.length(); i++) {
				amountChildren += countObjects(childrenOfObject.getJSONObject(i));
			}
		}

		return amountChildren;
	}

	/**
	 * Iterate through all JSONObjects in the given JSONArray and either add them to
	 * the solutionVariantList if they have variant rules or call this method
	 * recursively again with the children array of each JSONObject.
	 * 
	 * @param jsonArray           A JSONArray of JSONObjects
	 * @param solutionVariantList A list which contains all found solution variant
	 *                            JSONObjects
	 */
	private static void findJsonSolutionVariants(JSONArray jsonArray, List<JSONObject> solutionVariantList) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject jsonObject = jsonArray.getJSONObject(i);

			if (jsonObject.has(TcConstants.JSON_CHILDREN)) {
				findJsonSolutionVariants(jsonObject.getJSONArray(TcConstants.JSON_CHILDREN), solutionVariantList);
			} else if (jsonObject.has(TcConstants.JSON_VARIANT_RULES)) {
				solutionVariantList.add(jsonObject);
			}
		}
	}

	/**
	 * Recursively iterate through all JSONObjects and collect their objectGroupIDs.
	 * 
	 * @param jsonArray
	 * @param logger
	 * @param objectGroupMap
	 */
	private static boolean getAllObjectGroupIDs(JSONArray jsonArray, Logger logger,
			Map<String, Set<String>> objectGroupMap) throws JSONException {
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject jsonObject = jsonArray.getJSONObject(i);
			final String objectType = getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE);

			// We don't care about solution variants or objects without children
			if (jsonObject.has(TcConstants.JSON_VARIANT_RULES) || !jsonObject.has(TcConstants.JSON_CHILDREN)
					|| jsonObject.getJSONArray(TcConstants.JSON_CHILDREN).length() == 0) {
				continue;
			}

			// We're done if the objectGroupID isn't given
			if (!jsonObject.has(TcConstants.JSON_OBJECT_GROUP_ID)
					|| jsonObject.optString(TcConstants.JSON_OBJECT_GROUP_ID).isEmpty()) {
				logger.severe(String.format(
						"The JSONObject %s is either missing the attribute 'objectGroupID' or it is empty. Please check the JSON file.",
						getAttribute(jsonObject, TcConstants.JSON_OBJECT_NAME)));
				return false;
			}

			// Create the entry of this object type if it doesn't exist yet
			if (!objectGroupMap.containsKey(objectType)) {
				final Set<String> set = new HashSet<>();
				set.add(getAttribute(jsonObject, TcConstants.JSON_OBJECT_GROUP_ID));

				objectGroupMap.put(objectType, set);
			} else {
				objectGroupMap.get(objectType).add(getAttribute(jsonObject, TcConstants.JSON_OBJECT_GROUP_ID));
			}

			// Get all objectGroupIDs from the children objects.
			if (!getAllObjectGroupIDs(getJsonArray(jsonObject, TcConstants.JSON_CHILDREN), logger, objectGroupMap)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Check all object type entries in the given map if they are valid.
	 * 
	 * @param searchManagement
	 * @param objectGroupMap
	 * @param dataManagement
	 * @return
	 */
	private static boolean hasAllObjectGroupsGiven(SearchManagement searchManagement,
			final Map<String, Set<String>> objectGroupMap, final DataManagementService dataManagement, Logger logger) {
		logger.info("Check all object type entries in the given map if they are valid.");

		final Set<String> missingObjectGroupIDs = new HashSet<>();

		for (final String objectType : objectGroupMap.keySet()) {

			final Set<String> objectGroupIDs = objectGroupMap.get(objectType);
			logger.info(String.format("The object groups in the JSON for the type %s are %s", objectType,
					Arrays.asList(objectGroupIDs.toArray())));

			final com.teamcenter.services.strong.core._2013_05.LOV.LOVValueRow[] lovValues = searchManagement
					.getObjectGroupEntry(objectType);

			// List all UIDs in an array
			final String[] uidArray = new String[lovValues.length];
			for (int i = 0; i < lovValues.length; i++) {
				uidArray[i] = lovValues[i].uid;
			}

			// Remove all itemIDs that we found
			final ServiceData serviceData = dataManagement.loadObjects(uidArray);
			for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
				final ModelObject currentObject = serviceData.getPlainObject(i);
				String itemID;
				try {

					dataManagement.getProperties(new ModelObject[] { currentObject },
							new String[] { TcConstants.TEAMCENTER_ITEM_ID });
					itemID = currentObject.getPropertyDisplayableValue(TcConstants.TEAMCENTER_ITEM_ID);
					if (objectGroupIDs.contains(itemID)) {
						objectGroupIDs.remove(itemID);
					}
				} catch (final NotLoadedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			missingObjectGroupIDs.addAll(objectGroupIDs);

			// If we couldn't find an object group to each entry
			if (!objectGroupIDs.isEmpty()) {
				logger.severe(String.format(
						"Not all object group IDs were valid. The IDs %s don't exist for the object type %s.",
						Arrays.asList(objectGroupIDs.toArray()), objectType));
			}
		}

		if (!missingObjectGroupIDs.isEmpty()) {
			logger.severe(String.format("Not all object group IDs were valid. The IDs %s are invalid.",
					Arrays.asList(missingObjectGroupIDs.toArray())));

			return false;
		}

		return true;
	}

	/**
	 * Check, if the given jsonString is a valid JSONObject or JSONArray.
	 * 
	 * @param logger
	 * @param jsonString
	 * @return
	 */
	private static boolean isValid(Logger logger, String jsonString) {
		logger.info("Check if the given String is a valid JSON object.");

		try {
			new JSONObject(jsonString);
		} catch (final JSONException ex) {
			try {
				new JSONArray(jsonString);
			} catch (final JSONException ex1) {
				logger.severe(
						"The JSON file is not valid. Please check for syntax errors like missing commas or missing brackets.");
				return false;
			}
		}

		logger.info("The given String is a valid JSON object.");
		return true;
	}
}
