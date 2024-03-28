package com.parsa.middleware.businessobjects;

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

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class SolutionVariant implements StructureObject {
	// JSON
	private JSONObject jsonObject;
	private final JSONObject oldJsonObject;

	// Teamcenter Object
	private Item solutionItem;
	private ItemRevision solutionItemRevision;
	private BOMLine solutionBomLine;
	private BOMLine genericBomLine;

	// Structure Objects
	private StructureObject similarStructureObject;

	// Map
	private Map<String, String> variantRules;
	private final Map<String, Family> variantRuleFamilyMap;
	private Map<String, String> childrenVariantCategories;
	private Map<String, String> classificationMap;
	private final Map<String, String> properties;

	// List
	private final List<StructureObject> parentList;
	private final List<StructureObject> childrenList;

	// Boolean
	private boolean hasParent = false;
	private boolean notFound = false;
	private boolean wasCreated = false;

	// String
	private String displayString;
	private String classID;
	private String jsonRevisionID;
	private String releaseStatus;
	private String workflow;

	// Integer
	private int solutionVariantCategory;
	private final int depth;

	public SolutionVariant(JSONObject json, StructureObject parent, int depth) throws JSONException {
		this.depth = depth;

		// JSON
		oldJsonObject = new JSONObject(json.toString());
//		oldJsonObject.remove(TcConstants.JSON_ACAD_HANDLE);
		jsonObject = new JSONObject(json.toString());
		jsonObject.remove(TcConstants.JSON_CHILDREN);

		// Teamcenter Objects
		solutionItem = null;
		solutionItemRevision = null;
		solutionBomLine = null;
		genericBomLine = null;

		// Lists
		childrenList = new ArrayList<>();
		parentList = new ArrayList<>();
		if (parent != null) {
			parentList.add(parent);
			parent.addChild(this);
		}
		// Maps
		variantRules = new HashMap<>();
		variantRuleFamilyMap = new HashMap<>();
		childrenVariantCategories = new HashMap<>();
		classificationMap = new HashMap<>();
		properties = new HashMap<>();

		displayString = "";
		similarStructureObject = null;
		solutionVariantCategory = Utility
				.getSolutionVariantCategoryFromString(jsonObject.optString(TcConstants.JSON_SOLUTION_VARIANT_CATEGORY));

		fillAttributes();
	}

	@Override
	public JSONObject getOldJsonObject() {
		return oldJsonObject;
	}

	@Override
	public void addChild(StructureObject child) {
		childrenList.add(child);
	}

	@Override
	public void addClassification(Map<String, String> newClassification) {
		classificationMap.putAll(newClassification);
	}

	public void addFamily(Family family) {
		variantRuleFamilyMap.put(family.getFamilyID(), family);
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
	public BOMLine getBomLine() {
		return solutionBomLine;
	}

	@Override
	public List<StructureObject> getChildren() {
		return childrenList;
	}

	public Map<String, String> getChildrenVariantCategories() {
		return childrenVariantCategories;
	}

	@Override
	public Map<String, String> getClassificationMap() {
		return classificationMap;
	}

	public Family getFamily(String familyID) {
		return variantRuleFamilyMap.get(familyID);
	}

	public BOMLine getGenericBomLine() {
		return genericBomLine;
	}

	public String getGenericObjectID() {
		return properties.get(TcConstants.JSON_GENERIC_OBJECT_ID);
	}

	@Override
	public Item getItem() {
		return solutionItem;
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
		return properties.get(TcConstants.JSON_OBJECT_ID);
	}

	@Override
	public int getDepth() {
		return depth;
	}

	@Override
	public StructureObject getSimilarStructureObject() {
		return similarStructureObject;
	}

	public int getSolutionVariantCategory() {
		return solutionVariantCategory;
	}

	public Map<String, String> getVariantRules() {
		return variantRules;
	}

	@Override
	public String getWorkflow() {
		return workflow;
	}

	@Override
	public void setBomLine(BOMLine bomLine) {
		solutionBomLine = bomLine;
		setDisplayString(Utility.getDisplayString(bomLine));
	}

	public void setChildrenVariantCategories(Map<String, String> childrenVariantCategories) {
		this.childrenVariantCategories = childrenVariantCategories;
	}

	public void setGenericBomLine(BOMLine bomLine) {
		genericBomLine = bomLine;
	}

	public void setGenericItemID(String genericItemID) {
		properties.put(TcConstants.JSON_GENERIC_OBJECT_ID, genericItemID);
	}

	@Override
	public void setItem(Item item) {
		solutionItem = item;
	}

	public void setJsonRevisionID(String jsonRevisionID) {
		this.jsonRevisionID = jsonRevisionID;
	}

	public void setSolutionVariantCategory(int solutionVariantCategory) {
		this.solutionVariantCategory = solutionVariantCategory;
	}

	public void setVariantRules(Map<String, String> variantRuleMap) {
		variantRules = variantRuleMap;
	}

	/**
	 * Fill the attributes from the JSONObject
	 */
	private void fillAttributes() throws JSONException {
		properties.put(TcConstants.JSON_GENERIC_OBJECT_ID,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_GENERIC_OBJECT_ID));
		properties.put(TcConstants.JSON_OBJECT_TYPE, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE));
		properties.put(TcConstants.JSON_OBJECT_DESCRIPTION,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_DESCRIPTION));

		properties.put(TcConstants.JSON_COORDINATES, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_COORDINATES));
		properties.put(TcConstants.JSON_ROTATION, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_ROTATION));
		properties.put(TcConstants.JSON_POSITION_DESIGNATOR,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_POSITION_DESIGNATOR));
		properties.put(TcConstants.JSON_FIND_NO, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_FIND_NO));

		classID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_STORAGE_CLASS_ID);
		releaseStatus = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS);
		jsonRevisionID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_REVISION_ID);
		workflow = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_WORKFLOW);

		variantRules = JsonUtil
				.getSolutionVariantMapFromJsonArray(JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_VARIANT_RULES));
		childrenVariantCategories = JsonUtil.mapChildrenSolutionVariantEntries(
				JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_CHILD_SOLUTION_VARIANTS));

		classificationMap = JsonUtil.getClassificationMapFromJsonArray(
				JsonUtil.getJsonArray(jsonObject, TcConstants.JSON_CLASSIFICATION_ATTRIBUTES));
	}

	@Override
	public void setJsonObject(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public void setItemType(String itemType) {
		properties.put(TcConstants.JSON_OBJECT_TYPE, itemType);
	}

	@Override
	public ItemRevision getItemRevision() {
		return solutionItemRevision;
	}

	@Override
	public void setItemRevision(ItemRevision itemRevision) {
		solutionItemRevision = itemRevision;
		setDisplayString(Utility.getDisplayString(itemRevision));

	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public void setDisplayString(String string) {
		displayString = string;
		try {
			jsonObject.put(TcConstants.JSON_OBJECT_ID, displayString.split("/|-")[0]);
			jsonObject.put(TcConstants.JSON_REVISION_ID, displayString.split("/|-")[1].split(";")[0]);
		} catch (final IndexOutOfBoundsException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public void addProperty(String propertyName, String propertyValue) {
		properties.put(propertyName, propertyValue);
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
	public void setSimilarStructureObject(StructureObject structureObject) {
		similarStructureObject = structureObject;
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
	public String getJsonRevisionID() {
		return jsonRevisionID;
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
		final List<String> compareKeyList = Arrays.asList(new String[] { TcConstants.JSON_OBJECT_TYPE,
				TcConstants.JSON_STORAGE_CLASS_ID, TcConstants.JSON_CLASSIFICATION_ATTRIBUTES,
				TcConstants.JSON_REVISION_ID, TcConstants.JSON_VARIANT_RULES, TcConstants.JSON_GENERIC_OBJECT_ID,
				TcConstants.JSON_SOLUTION_VARIANT_CATEGORY, TcConstants.JSON_CHILD_SOLUTION_VARIANT_CATEGORIES,
				TcConstants.JSON_CHILD_SOLUTION_VARIANTS, TcConstants.JSON_OBJECT_ID, TcConstants.JSON_FAMILY_ID,
				TcConstants.JSON_FEATURE_ID, TcConstants.JSON_WORKFLOW, TcConstants.JSON_RELEASE_STATUS });

		return similarObject(comparingObject, oldJsonObject, compareKeyList, logger);
//
//			final List<String> compareKeyList = Arrays.asList(new String[] { TcConstants.JSON_OBJECT_TYPE,
//					TcConstants.JSON_STORAGE_CLASS_ID, TcConstants.JSON_CLASSIFICATION_ATTRIBUTES,
//					TcConstants.JSON_REVISION_ID, TcConstants.JSON_VARIANT_RULES, TcConstants.JSON_GENERIC_OBJECT_ID,
//					TcConstants.JSON_SOLUTION_VARIANT_CATEGORY, TcConstants.JSON_CHILD_SOLUTION_VARIANT_CATEGORIES,
//					TcConstants.JSON_CHILD_SOLUTION_VARIANTS, TcConstants.JSON_OBJECT_ID });
//
//			final Set<String> set = comparingObject.keySet();
//
//			// Do both objects have the same keys?
//			if (!set.equals(jsonObject.keySet())) {
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
//				final Object valueJson = jsonObject.get(currentKey);
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

	/**
	 * Compare the two given JSONObjects. But only compare the given attributes.
	 * 
	 * @param jsonObject
	 * @param comparingObject
	 * @param logger
	 * @return
	 */
	private boolean similarObject(JSONObject jsonObject, JSONObject comparingObject, List<String> compareKeyList,
			Logger logger) {
//		logger.info(
//				String.format("Compare the JSONObjects %s and %s.", jsonObject.optString(TcConstants.JSON_OBJECT_NAME),
//						comparingObject.optString(TcConstants.JSON_OBJECT_NAME)));
		try {
//			final Set<String> set = (Set<String>) comparingObject.keys();

			Iterator<String> keysIterator = comparingObject.keys();
			Set<String> keySet = new HashSet<>();
			while (keysIterator.hasNext()) {
				keySet.add(keysIterator.next());
			}

			// Do both objects have the same keys?
//			if (!set.equals(jsonObject.keySet())) {
////				logger.warning("The attributes of the JSONObjects don't match.");
//				return false;
//			}

			// Iterate through all keys
			final Iterator<String> iterator = keySet.iterator();
			while (iterator.hasNext()) {
				final String currentKey = iterator.next();

				// Ignore all keys from the list
				if (!compareKeyList.contains(currentKey)) {
					continue;
				}

				final Object valueComparing = comparingObject.opt(currentKey);
				final Object valueJson = jsonObject.opt(currentKey);

				if (valueComparing instanceof JSONObject) {

					// Compare the JSONObjects
					if (!(similarObject((JSONObject) valueComparing, (JSONObject) valueJson, compareKeyList, logger))) {
						return false;
					}
				} else if (valueComparing instanceof JSONArray) {

					final JSONArray jsonArrayOther = new JSONArray(valueJson.toString());

					// Compare the number of variant rules
					if (currentKey.equals(TcConstants.JSON_VARIANT_RULES)
							&& jsonArrayOther.length() != ((JSONArray) valueComparing).length()) {
						return false;
					}

					// Compare every JSONObject in the JSONArray with every other JSONObject in the
					// other JSONArray
					for (int i = 0; i < ((JSONArray) valueComparing).length(); i++) {
						final JSONObject jsonObjectThis = new JSONObject(
								((JSONArray) valueComparing).getJSONObject(i).toString());

						for (int j = 0; j < jsonArrayOther.length(); j++) {
							if (similarObject(jsonArrayOther.getJSONObject(j), jsonObjectThis, compareKeyList,
									logger)) {
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
