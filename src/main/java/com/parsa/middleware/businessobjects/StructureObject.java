package com.parsa.middleware.businessobjects;

import com.teamcenter.soa.client.model.strong.BOMLine;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public interface StructureObject {

	/**
	 * Add the given StuctureObject as child.
	 * 
	 * @param child
	 */
	void addChild(StructureObject child);

	/**
	 * Add classification properties.
	 * 
	 * @param newClassification
	 */
	void addClassification(Map<String, String> newClassification);

	/**
	 * Add the given StructureObject as parent.
	 * 
	 * @param parent
	 */
	void addParent(StructureObject parent);

	/**
	 * Add the given property with its value to the property map of this object.
	 * 
	 * @param propertyName
	 * @param propertyValue
	 */
	void addProperty(String propertyName, String propertyValue);

	/**
	 * Get the BOMLine object
	 * 
	 * @return
	 */
	BOMLine getBomLine();

	/**
	 * Get a list of all children objects
	 * 
	 * @return
	 */
	List<StructureObject> getChildren();

	/**
	 * Get the storage class ID.
	 * 
	 * @return
	 */
	String getClassID();

	Map<String, String> getClassificationMap();

	/**
	 * Get the itemID with the object name and revision ID as String.
	 * 
	 * @return
	 */
	String getDisplayString();

	int getDepth();

	/**
	 * Get the Item object.
	 * 
	 * @return
	 */
	Item getItem();

	/**
	 * Get the Item Revision
	 * 
	 * @return
	 */
	ItemRevision getItemRevision();

	/**
	 * Get the item type.
	 * 
	 * @return
	 */
	String getItemType();

	/**
	 * Get the corresponding JSONObject.
	 * 
	 * @return
	 */
	JSONObject getJsonObject();

	/**
	 * Get the object ID that is given in the JSON file.
	 * 
	 * @return
	 */
	String getJsonObjectID();

	String getJsonRevisionID();

	/**
	 * Get the original JSONObject with all properties.
	 * 
	 * @return
	 */
	JSONObject getOldJsonObject();

	/**
	 * Get the parent object.
	 * 
	 * @return
	 */
	List<StructureObject> getParentList();

	/**
	 * Get a map of all properties.
	 * 
	 * @return
	 */
	Map<String, String> getProperties();

	/**
	 * Get the release status.
	 * 
	 * @return
	 */
	String getReleaseStatus();

	/**
	 * Get the StructureObject key object from its map that is similar to the
	 * current StructureObject.
	 * 
	 * @return
	 */
	StructureObject getSimilarStructureObject();

	/**
	 * Get the name of the workflow that must be started on the Teamcenter object.
	 * 
	 * @return
	 */
	String getWorkflow();

	/**
	 * Get a boolean that confirms if this object has a parent.
	 * 
	 * @return
	 */
	boolean hasParent();

	void setBomLine(BOMLine bomLine);

	void setClassID(String newClassID);

	void setDisplayString(String string);

	/**
	 * Set a boolean to define if this object has a parent.
	 * 
	 * @param bool
	 */
	void setHasParent(boolean bool);

	void setItem(Item item);

	void setItemRevision(ItemRevision itemRevision);

	void setItemType(String itemType);

	void setJsonObject(JSONObject jsonObject);

	void setSimilarStructureObject(StructureObject structureObject);

	void setReleaseStatus(String status);

	void setWasCreated(boolean bool);

	/**
	 * Compare the JSONObject of this object and the given structureObject. The
	 * relative attributes of this object level can differ, since they only need to
	 * be added when added to a parent BOMLine. But the attributes of all other
	 * levels need to be exactly the same.
	 * 
	 * @param structureObject
	 * @return True if this and structureObject are similar
	 */
	boolean similar(StructureObject structureObject, Logger logger);

	/**
	 * True if the Teamcenter object was created.
	 * 
	 * @return
	 */
	boolean wasCreated();

	/**
	 * True if the Teamcenter object was not found.
	 *
	 * @return
	 */
	boolean isNotFound();

	void setNotFound(boolean notFound);

}
