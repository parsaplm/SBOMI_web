package com.parsa.middleware.businessobjects;

import com.parsa.middleware.constants.TcConstants;
import com.parsa.middleware.processing.Utility;
import com.parsa.middleware.util.JsonUtil;
import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class ModularBuilding implements StructureObject {
	// JSON
	private JSONObject jsonObject;
	private final JSONObject oldJsonObject;

	// Teamcenter Object
	private Item mbItem;
	private ItemRevision mbItemRevision;
	private BOMLine mbBomLine;

	// Structure Objects
	private StructureObject similarStructureObject;

	// List
	private final List<StructureObject> childrenList;
	private final List<StructureObject> parentList;

	// Map
	private final Map<String, String> properties;
	private final Map<String, String> classificationMap;

	// Boolean
	boolean wasCreated = false;
	boolean hasParent = false;

	// String
	private String displayString;
	private String jsonRevisionID;
	private String jsonObjectID;
	private String revisionRule;
	private String classID;
	private String releaseStatus;
	private String workflow;

	private boolean notFound;

	// Integer
	private final int depth;

	public ModularBuilding(JSONObject json, StructureObject parent, int depth) throws JSONException {
		this.depth = depth;

		// JSON
		oldJsonObject = new JSONObject(json.toString());
		oldJsonObject.remove(TcConstants.JSON_ACAD_HANDLE);
		jsonObject = new JSONObject(json.toString());
		jsonObject.remove(TcConstants.JSON_CHILDREN);

		// Teamcenter Objects
		mbItem = null;
		mbItemRevision = null;
		mbBomLine = null;

		// Lists
		childrenList = new ArrayList<>();
		parentList = new ArrayList<>();
		parentList.add(parent);

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
	public String getReleaseStatus() {
		return releaseStatus;
	}

	@Override
	public void setReleaseStatus(String releaseStatus) {
		this.releaseStatus = releaseStatus;
	}

	@Override
	public BOMLine getBomLine() {
		return mbBomLine;
	}

	@Override
	public List<StructureObject> getChildren() {
		return childrenList;
	}

	@Override
	public String getClassID() {
		return classID;
	}

	@Override
	public Map<String, String> getClassificationMap() {
		return classificationMap;
	}

	@Override
	public int getDepth() {
		return depth;
	}

	public String getDesigner() {
		return properties.get(TcConstants.JSON_DESIGNER);
	}

	public String getDesignNo() {
		return properties.get(TcConstants.JSON_DESIGN_NO);
	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public Item getItem() {
		return mbItem;
	}

	@Override
	public ItemRevision getItemRevision() {
		return mbItemRevision;
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

	public String getObjectName() {
		return properties.get(TcConstants.JSON_OBJECT_NAME);
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	public String getRevisionRule() {
		return revisionRule;
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
		mbBomLine = bomLine;
		setDisplayString(Utility.getDisplayString(bomLine));
	}

	@Override
	public void setClassID(String classID) {
		this.classID = classID;
	}

	public void setDesigner(String designer) {
		properties.put(TcConstants.JSON_DESIGNER, designer);
	}

	public void setDesignNo(String designNo) {
		properties.put(TcConstants.JSON_DESIGN_NO, designNo);
	}

	@Override
	public void setDisplayString(String string) {
		displayString = string.isBlank() ? displayString : string;
		try {
			jsonObject.put(TcConstants.JSON_OBJECT_ID, displayString.split("/|-")[0]);
			jsonObject.put(TcConstants.JSON_REVISION_ID, displayString.split("/|-")[1].split(";")[0]);
		} catch (final IndexOutOfBoundsException | JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setItem(Item item) {
		mbItem = item;
	}

	@Override
	public void setItemRevision(ItemRevision itemRevision) {
		mbItemRevision = itemRevision;
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

	public void setObjectName(String objectName) {
		properties.put(TcConstants.JSON_OBJECT_NAME, objectName);
	}

	public void setRevisionRule(String revisionRule) {
		this.revisionRule = revisionRule;
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

	/**
	 * Fill all attributes that are given in the JSONObject
	 */
	private void fillAttributes() {
		properties.put(TcConstants.JSON_OBJECT_TYPE, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_TYPE));
		properties.put(TcConstants.JSON_OBJECT_DESCRIPTION,
				JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_DESCRIPTION));
		properties.put(TcConstants.JSON_OBJECT_NAME, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_NAME));

		properties.put(TcConstants.JSON_DESIGNER, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_DESIGNER));
		properties.put(TcConstants.JSON_DESIGN_NO, JsonUtil.getAttribute(jsonObject, TcConstants.JSON_DESIGN_NO));

		jsonRevisionID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_REVISION_ID);
		jsonObjectID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_OBJECT_ID);
		revisionRule = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_REVISION_RULE);
		releaseStatus = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_RELEASE_STATUS);

		classID = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_STORAGE_CLASS_ID);
		workflow = JsonUtil.getAttribute(jsonObject, TcConstants.JSON_WORKFLOW);

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
	public void addParent(StructureObject parent) {
		parentList.add(parent);
	}

	@Override
	public List<StructureObject> getParentList() {
		return parentList;
	}

	@Override
	public boolean similar(StructureObject structureObject, Logger logger) {
		return false;
	}

	@Override
	public JSONObject getOldJsonObject() {
		return oldJsonObject;
	}
}
