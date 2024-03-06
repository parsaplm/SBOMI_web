package com.parsa.middleware.businessobjects;

/**
 * 
 *   @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */

import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.util.JsonUtil;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Container implements StructureObject {
	// JSON
	private JSONObject jsonObject;
	private final JSONObject oldJsonObject;

	// Teamcenter Objects
	private BOMLine containerBomLine;
	private Item containerItem;
	private ItemRevision containerItemRevision;

	// Structure Objects
	private StructureObject similarStructureObject;

	// Lists
	private final List<StructureObject> childrenList;
	private final List<StructureObject> parentList;

	// Maps
	private Map<String, String> classificationMap;
	private final Map<String, String> properties;

	// Boolean
	private boolean hasParent = false;
	private boolean notFound = false;
	private boolean wasCreated = false;

	// String
	private String classID;
	private String displayString;
	private String jsonObjectID;
	private String jsonRevisionID;
	private String releaseStatus;
	private String workflow;

	// Integer
	private final int depth;

	public Container(JSONObject json, StructureObject parent, int depth) throws JSONException {
		this.depth = depth;

		// JSON
		oldJsonObject = new JSONObject(json.toString());
		oldJsonObject.remove(TcConstants.JSON_ACAD_HANDLE);
		jsonObject = new JSONObject(json.toString());
		jsonObject.remove(TcConstants.JSON_CHILDREN);

		// Teamcenter Objects
		containerItem = null;
		containerItemRevision = null;
		containerBomLine = null;

		// Lists
		childrenList = new ArrayList<>();
		parentList = new ArrayList<>();
		if (parent != null) {
			parentList.add(parent);

			parent.addChild(this);
		}
		// Maps
		classificationMap = new HashMap<>();
		properties = new HashMap<>();

		displayString = "";
		similarStructureObject = null;

		fillAttributes();
	}

	@Override
	public void addChild(StructureObject child) {
		childrenList.add(child);
	}

	@Override
	public void addClassification(Map<String, String> newClassification) {
		classificationMap.putAll(newClassification);
	}

	@Override
	public void addProperty(String propertyName, String propertyValue) {
		properties.put(propertyName, propertyValue);
	}

	@Override
	public BOMLine getBomLine() {
		return containerBomLine;
	}

	@Override
	public List<StructureObject> getChildren() {
		return childrenList;
	}

	@Override
	public Map<String, String> getClassificationMap() {
		return classificationMap;
	}

	public String getCoordinates() {
		return properties.get(TcConstants.JSON_COORDINATES);
	}

	@Override
	public int getDepth() {
		return depth;
	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public Item getItem() {
		return containerItem;
	}

	@Override
	public ItemRevision getItemRevision() {
		return containerItemRevision;
	}

	@Override
	public String getItemType() {
		return properties.get(TcConstants.JSON_OBJECT_TYPE);
	}

	@Override
	public JSONObject getJsonObject() {
		return jsonObject;
	}

	@Override
	public String getJsonObjectID() {
		return jsonObjectID;
	}

	@Override
	public String getJsonRevisionID() {
		return jsonRevisionID;
	}

	public String getObjectDescription() {
		return properties.get(TcConstants.JSON_OBJECT_DESCRIPTION);
	}

	public String getObjectGroupID() {
		return properties.get(TcConstants.JSON_OBJECT_GROUP_ID);
	}

	public String getObjectName() {
		return properties.get(TcConstants.JSON_OBJECT_NAME);
	}

	@Override
	public JSONObject getOldJsonObject() {
		return oldJsonObject;
	}

	public String getPositionDesignator() {
		return properties.get(TcConstants.JSON_POSITION_DESIGNATOR);
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	public String getRotation() {
		return properties.get(TcConstants.JSON_ROTATION);
	}

	@Override
	public StructureObject getSimilarStructureObject() {
		return similarStructureObject;
	}

	@Override
	public String getWorkflow() {
		return workflow;
	}

	@Override
	public void setBomLine(BOMLine bomLine) {
		containerBomLine = bomLine;
		setDisplayString(Utility.getDisplayString(bomLine));
	}

	public void setCoordinates(String coordinates) {
		properties.put(TcConstants.JSON_COORDINATES, coordinates);
	}

	@Override
	public void setDisplayString(String string) {
		displayString = string;
		try {
			jsonObject.put(TcConstants.JSON_OBJECT_ID, displayString.split("/|-")[0]);
			jsonObject.put(TcConstants.JSON_REVISION_ID, displayString.split("/|-")[1].split(";")[0]);
		} catch (final IndexOutOfBoundsException | JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setItem(Item item) {
		containerItem = item;
	}

	@Override
	public void setItemRevision(ItemRevision itemRevision) {
		containerItemRevision = itemRevision;
		setDisplayString(Utility.getDisplayString(itemRevision));
	}

	@Override
	public void setItemType(String itemType) {
		properties.put(TcConstants.JSON_OBJECT_TYPE, itemType);
	}

	@Override
	public void setJsonObject(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	public void setJsonObjectID(String jsonObjectID) {
		this.jsonObjectID = jsonObjectID;
	}

	public void setJsonRevisionID(String jsonRevisionID) {
		this.jsonRevisionID = jsonRevisionID;
	}

	public void setObjectDescription(String objectDescription) {
		properties.put(TcConstants.JSON_OBJECT_DESCRIPTION, objectDescription);
	}

	public void setObjectGroupID(String objectGroupID) {
		properties.put(TcConstants.JSON_OBJECT_GROUP_ID, objectGroupID);
	}

	public void setObjectName(String objectName) {
		properties.put(TcConstants.JSON_OBJECT_NAME, objectName);
	}

	public void setPositionDesignator(String positionDesignator) {
		properties.put(TcConstants.JSON_POSITION_DESIGNATOR, positionDesignator);
	}

	public void setRotation(String rotation) {
		properties.put(TcConstants.JSON_ROTATION, rotation);
	}

	@Override
	public void setSimilarStructureObject(StructureObject structureObject) {
		similarStructureObject = structureObject;
	}

	private void fillAttributes() throws JSONException {
		properties.put(TcConstants.JSON_OBJECT_TYPE, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE));
		properties.put(TcConstants.JSON_OBJECT_GROUP_ID,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_GROUP_ID));
		properties.put(TcConstants.JSON_OBJECT_NAME, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_NAME));
		properties.put(TcConstants.JSON_OBJECT_DESCRIPTION,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_DESCRIPTION));

		properties.put(TcConstants.JSON_COORDINATES, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_COORDINATES));
		properties.put(TcConstants.JSON_ROTATION, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_ROTATION));
		properties.put(TcConstants.JSON_POSITION_DESIGNATOR,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_POSITION_DESIGNATOR));
		properties.put(TcConstants.JSON_FIND_NO, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_FIND_NO));
		properties.put(TcConstants.JSON_PSEUDO_SERIAL_NUMBER,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_PSEUDO_SERIAL_NUMBER));

		classID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_STORAGE_CLASS_ID);
		releaseStatus = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS);
		workflow = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_WORKFLOW);

		jsonObjectID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_ID);
		jsonRevisionID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_REVISION_ID);

		classificationMap = JsonUtil.getClassificationMapFromJsonArray(
				JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_CLASSIFICATION_ATTRIBUTES));
	}

	@Override
	public String getClassID() {
		return classID;
	}

	@Override
	public void setClassID(String classID) {
		this.classID = classID;
	}

	@Override
	public String getReleaseStatus() {
		return releaseStatus;
	}

	@Override
	public void setReleaseStatus(String releaseStatus) {
		this.releaseStatus = releaseStatus;
	}

	@Override
	public boolean wasCreated() {
		return wasCreated;
	}

	@Override
	public boolean isNotFound() {
		return notFound;
	}

	@Override
	public void setNotFound(boolean notFound) {
		this.notFound = notFound;
	}

	@Override
	public void setWasCreated(boolean bool) {
		wasCreated = bool;
	}

	@Override
	public boolean hasParent() {
		return hasParent;
	}

	@Override
	public void setHasParent(boolean bool) {
		hasParent = bool;
	}

	@Override
	public List<StructureObject> getParentList() {
		return parentList;
	}

	@Override
	public void addParent(StructureObject parent) {
		parentList.add(parent);
	}

	@Override
	public boolean similar(StructureObject structureObject, Logger logger) {
//		try {

		final JSONObject comparingObject = structureObject.getOldJsonObject();
		return similarObject(oldJsonObject, comparingObject, logger);

//			final List<String> compareKeyList = Arrays
//					.asList(new String[] { TcConstants.JSON_OBJECT_TYPE, TcConstants.JSON_OBJECT_GROUP_ID,
//							TcConstants.JSON_STORAGE_CLASS_ID, TcConstants.JSON_CLASSIFICATION_ATTRIBUTES,
//							TcConstants.JSON_CHILDREN, TcConstants.JSON_REVISION_ID, TcConstants.JSON_OBJECT_ID });
//
//			final Set<String> set = comparingObject.keySet();
//
//			// Do both objects have the same keys?
//			if (!set.equals(oldJsonObject.keySet())) {
//				return false;
//			}
//
//			// Iterate through all keys
//			final Iterator<String> iterator = set.iterator();
//			while (iterator.hasNext()) {
//				final String currentKey = iterator.next();
//
//				// Compare only keys from the list
//				if (!compareKeyList.contains(currentKey)) {
//					continue;
//				}
//
//				final Object valueComparing = comparingObject.get(currentKey);
//				final Object valueJson = oldJsonObject.get(currentKey);
//
//				if (valueComparing instanceof JSONObject) {
//
//					// Compare the JSONObjects
//					if (!((JSONObject) valueComparing).similar(valueJson)) {
//						return false;
//					}
//				} else if (valueComparing instanceof JSONArray) {
//
//					// Compare the JSONArray
//					if (!((JSONArray) valueComparing).similar(valueJson)) {
//						return false;
//					}
//				} else if (!valueComparing.equals(valueJson)) {
//
//					// Compare the String values
//					logger.warning(String.format("The property %s is %s and not %s. No match.", currentKey,
//							valueComparing, valueJson));
//					return false;
//				}
//			}
//
//			// All relevant JSON properties are matching
//			return true;
//		} catch (final JSONException e) {
//			e.printStackTrace();
//		}
//		return false;
	}

	private boolean similarObject(JSONObject jsonObject, JSONObject comparingObject, Logger logger) {
//		logger.info(
//				String.format("Compare the JSONObjects %s and %s.", jsonObject.optString(TcConstants.JSON_OBJECT_NAME),
//						comparingObject.optString(TcConstants.JSON_OBJECT_NAME)));
		try {
			final List<String> compareKeyList = Arrays.asList(new String[] { TcConstants.JSON_OBJECT_TYPE,
					TcConstants.JSON_OBJECT_GROUP_ID, TcConstants.JSON_CLASSIFICATION_ATTRIBUTES,
					TcConstants.JSON_CHILDREN, TcConstants.JSON_REVISION_ID, TcConstants.JSON_OBJECT_ID,
					TcConstants.JSON_STORAGE_CLASS_ID, TcConstants.JSON_VARIANT_RULES,
					TcConstants.JSON_GENERIC_OBJECT_ID, TcConstants.JSON_SOLUTION_VARIANT_CATEGORY,
					TcConstants.JSON_CHILD_SOLUTION_VARIANT_CATEGORIES, TcConstants.JSON_CHILD_SOLUTION_VARIANTS,
					TcConstants.JSON_FAMILY_ID, TcConstants.JSON_FEATURE_ID, TcConstants.JSON_WORKFLOW,
					TcConstants.JSON_RELEASE_STATUS });

			final Set<String> set = (Set<String>) comparingObject.keys();

			// Do both objects have the same keys?
			if (!set.equals(jsonObject.keys())) {
//				logger.warning("The attributes of the JSONObjects don't match.");
				return false;
			}

			// Iterate through all keys
			final Iterator<String> iterator = set.iterator();
			while (iterator.hasNext()) {
				final String currentKey = iterator.next();

				// Ignore all keys not in the list
				if (!compareKeyList.contains(currentKey)) {
					continue;
				}

				final Object valueComparing = comparingObject.opt(currentKey);
				final Object valueJson = jsonObject.opt(currentKey);

//				if (valueJson == null || valueComparing == null) {
//					// If the key only exists in one JSONObject
//					continue;
//				}

				if (valueComparing instanceof JSONObject) {

					// Compare the JSONObjects
					if (!(similarObject((JSONObject) valueComparing, (JSONObject) valueJson, logger))) {
						return false;
					}
				} else if (valueComparing instanceof JSONArray) {

					final JSONArray jsonArrayOther = new JSONArray(valueJson.toString());

					// Compare the number of variant rules
					if (currentKey.equals(TcConstants.JSON_VARIANT_RULES)
							&& jsonArrayOther.length() != ((JSONArray) valueComparing).length()) {
						return false;
					}

					// Compare the number of children
					if (currentKey.equals(TcConstants.JSON_CHILDREN)
							&& jsonArrayOther.length() != ((JSONArray) valueComparing).length()) {
						return false;
					}
					// Compare every JSONObject in the JSONArray with every other JSONObject in the
					// other JSONArray
					for (int i = 0; i < ((JSONArray) valueComparing).length(); i++) {
						final JSONObject jsonObjectThis = new JSONObject(
								((JSONArray) valueComparing).getJSONObject(i).toString());

						for (int j = 0; j < jsonArrayOther.length(); j++) {
							if (similarObject(jsonArrayOther.getJSONObject(j), jsonObjectThis, logger)) {
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
//					logger.warning(String.format("The property %s is %s and not %s. No match.", currentKey,
//							valueComparing, valueJson));
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

}
