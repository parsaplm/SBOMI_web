package com.parsa.middleware.constants;

/**
 * A collection of all used contant values. The values are sorted alphabetically
 * by use and again alphabetically by name. The format for the values is:
 * <USENAME>_<VALUE>
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class TcConstants {

	/////////////////////////////////////
	// Database table and column names //
	/////////////////////////////////////

	// C
	public static final String DATABASE_CREATION_DATE = "creation_date";
	public static final String DATABASE_CURRENT_STATUS = "current_status";

	// D
	public static final String DATABASE_DRAWING_NUMBER = "drawing_number";

	// E
	public static final String DATABASE_ENCRYPTED_PASSWORD = "encrypted_password";
	public static final String DATABASE_END_IMPORT_DATE = "end_import_date";

	// F
	public static final String DATABASE_FAVORITE = "is_favorite";
	public static final String DATABASE_FILENAME = "filename";

	// I
	public static final String DATABASE_IMPORT_PROGRESS = "import_progress";
	public static final String DATABASE_IMPORT_TIME = "import_time";

	// L
	public static final String DATABASE_LOGFILE_NAME = "logfile_name";

	// N
	public static final String DATABASE_NEW_VALUE = "new_val";
	public static final String DATABASE_NUMBER_OF_CONTAINER = "number_of_container";
	public static final String DATABASE_NUMBER_OF_OBJECTS = "number_of_objects";

	// O
	public static final String DATABASE_OLD_VALUE = "old_val";
	public static final String DATABASE_OPERATION = "operation";

	// S
	public static final String DATABASE_SALT = "salt";
	public static final String DATABASE_SBOMI_HOSTNAME = "sbomi_host_name";
	public static final String DATABASE_START_IMPORT_DATE = "start_import_date";

	// T
	public static final String DATABASE_TABLE_QUEUE = "queue";
	public static final String DATABASE_TABLE_USERS = "users";
	public static final String DATABASE_TABLE_LOG = "t_history";
	public static final String DATABASE_TASK_ID = "task_id";
	public static final String DATABASE_TEAMCENTER_ROOT_OBJECT = "teamcenter_root_object";
	public static final String DATABASE_TIME_STAMP = "tstamp";

	// U
	public static final String DATABASE_USERNAME = "username";

	//////////////////////////
	// JSON attribute names //
	//////////////////////////

	// A
	public static final String JSON_ACAD_HANDLE = "acadHandle";

	// C
	public static final String JSON_CHILDREN = "children";
	public static final String JSON_CHILD_SOLUTION_VARIANTS = "childrenSolutionVariants";
	public static final String JSON_CHILD_SOLUTION_VARIANT_CATEGORIES = "childSolutionVariantCategories";
	public static final String JSON_CLASSIFICATION_ATTRIBUTES = "classificationAttributes";
	public static final String JSON_CLASSIFICATION_ID = "classificationID";
	public static final String JSON_CLASSIFICATION_VALUE = "classificationValue";
	public static final String JSON_COORDINATES = "coordinates";

	// D
	public static final String JSON_DATE_MODIFIED = "dateModified";
	public static final String JSON_DESIGNER = "designer";
	public static final String JSON_DESIGN_NO = "designNo";
	public static final String JSON_DRAWING_NO = "drawingNo";

	// F
	public static final String JSON_FAMILY_ID = "familyID";
	public static final String JSON_FEATURE_ID = "featureID";
	public static final String JSON_FIND_NO = "findNo";

	// G
	public static final String JSON_GENERIC_OBJECT_ID = "genericObjectID";

	// O
	public static final String JSON_OBJECT_DESCRIPTION = "objectDescription";
	public static final String JSON_OBJECT_GROUP_ID = "objectGroupID";
	public static final String JSON_OBJECT_ID = "objectID";
	public static final String JSON_OBJECT_NAME = "objectName";
	public static final String JSON_OBJECT_TYPE = "objectType";

	// P
	public static final String JSON_POSITION_DESIGNATOR = "positionDesignator";
	public static final String JSON_PSEUDO_SERIAL_NUMBER = "pseudoSerialNumber";

	// Q
	public static final String JSON_QUANTITY = "quantity";

	// R
	public static final String JSON_RELEASE_STATUS = "releaseStatus";
	public static final String JSON_REVISION_ID = "revisionID";
	public static final String JSON_REVISION_RULE = "revisionRule";
	public static final String JSON_ROTATION = "rotation";

	// S
	public static final String JSON_SOLUTION_VARIANT_CATEGORY = "solutionVariantCategory";
	public static final String JSON_STORAGE_CLASS_ID = "storageClassID";

	// V
	public static final String JSON_VARIANT_RULES = "variantRules";

	// W
	public static final String JSON_WORKFLOW = "workflow";

	//////////////////
	// Folder names //
	//////////////////

	// B
	public static final String FOLDER_BOMI_LOG = "bomi";

	// C
	public static final String FOLDER_CANCELED = "canceled";

	// D
	public static final String FOLDER_DATASET = "dataset";
	public static final String FOLDER_DELETED = "deleted";
	public static final String FOLDER_DONE = "done";

	// E
	public static final String FOLDER_ERROR = "error";

	// I
	public static final String FOLDER_IN_PROGRESS = "inProgress";
	public static final String FOLDER_IN_REVIEW = "inReview";

	// L
	public static final String FOLDER_LOG = "log";

	// S
	public static final String FOLDER_SBOMI_LOG = "sbomi";

	// T
	public static final String FOLDER_TODO = "todo";
	public static final String FOLDER_TRANSACTION = "transaction";

	// U
	public static final String FOLDER_UPDATED = "updated";

	///////////////////////
	// SBOMI label names //
	///////////////////////

	// V
	public static final String SBOMI_VERSION = "1.5.23";

	/////////////////////////////
	// Settings property names //
	/////////////////////////////

	// A
	public static final String SETTINGS_ALWAYS_CLASSIFY = "alwaysClassify";

	// D
	public static final String SETTINGS_DATABASE_HOST = "databaseHost";
	public static final String SETTINGS_DATABASE_INSTANCE = "databaseInstance";
	public static final String SETTINGS_DATABASE_NAME = "databaseName";
	public static final String SETTINGS_DATABASE_PASSWORD = "databasePassword";
	public static final String SETTINGS_DATABASE_TABLE_NAME = "databaseTableName";
	public static final String SETTINGS_DATABASE_USER = "databaseUser";
	public static final String SETTINGS_DELETE_ENTRIES_INTERVALL = "deleteEntriesIntervall";

	// L
	public static final String SETTINGS_LOG_FOLDER = "logFolder";
	public static final String SETTINGS_LOGGING_TABLE_NAME = "loggingTableName";

	// M
	public static final String SETTINGS_MAX_ERRORS = "maxErrors";

	// P
	public static final String SETTINGS_PARALLEL_IMPORTS = "parallelImports";

	// S
	public static final String SETTINGS_SEARCH_PARALLEL = "searchParallel";
	public static final String SETTINGS_SERVER_URL = "serverURL";

	// T
	public static final String SETTINGS_TRANSACTION_FOLDER = "transactionFolder";
	public static final String SETTINGS_TC_MAX_RETRIES = "tcMaxRetries";
	public static final String SETTINGS_TC_MAX_RETRIES_DEFAULT_VALUE = "3";
	public static final String SETTINGS_TC_RETRY_DELAY = "tcRetryDelay";
	public static final String SETTINGS_TC_RETRY_DELAY_DEFAULT_VALUE = "60";

	// U
	public static final String SETTINGS_UPDATE_INTERVALL = "updateIntervall";
	public static final String SETTINGS_USERGROUP = "usergroup";
	public static final String SETTINGS_USERNAME = "username";

	///////////////////////////////
	// Teamcenter property names //
	///////////////////////////////

	// A
	public static final String TEAMCENTER_ALL_WORKFLOWS = "fnd0AllWorkflows";

	// B
	public static final String TEAMCENTER_BOMLINE_ALL_WORKFLOWS = "bl_rev_fnd0AllWorkflows";
	public static final String TEAMCENTER_BOMLINE_CHILDREN = "bl_child_lines";
	public static final String TEAMCENTER_BOMLINE_CURRENT_REVISION_ID = "bl_rev_current_revision_id";
	public static final String TEAMCENTER_BOMLINE_DESIGNER = "bl_rev_ct4Designer";
	public static final String TEAMCENTER_BOMLINE_DRAWING_NO = "bl_rev_ct4DrawingNo";
	public static final String TEAMCENTER_BOMLINE_GENERIC_OBJECT = "bl_rev_Smc0SolutionVariantSource";
	public static final String TEAMCENTER_BOMLINE_HAS_CHILDREN = "bl_has_children";
	public static final String TEAMCENTER_BOMLINE_ITEM = "bl_item";
	public static final String TEAMCENTER_BOMLINE_OBJECT_DESCRIPTION = "bl_rev_object_desc";
	public static final String TEAMCENTER_BOMLINE_OBJECT_ID = "bl_item_current_id";
	public static final String TEAMCENTER_BOMLINE_OBJECT_TYPE = "bl_item_object_type";
	public static final String TEAMCENTER_BOMLINE_POSITION_DESIGNATOR = "bl_position_designator";
	public static final String TEAMCENTER_BOMLINE_QUANTITY = "bl_quantity";
	public static final String TEAMCENTER_BOMLINE_PRODUCT_VARIANT_RULE = "bl_rev_ct4ProductVariantRule";
	public static final String TEAMCENTER_BOMLINE_RELATIVE_TRANSFORMATION_MATRIX = "bl_plmxml_occ_xform";
	public static final String TEAMCENTER_BOMLINE_RELEASE_STATUS_LIST = "bl_rev_release_status_list";
	public static final String TEAMCENTER_BOMLINE_REVISION = "bl_revision";
	public static final String TEAMCENTER_BOMLINE_SOLUTION_VARIANT_SOURCE = "bl_rev_Smc0SolutionVariantSource";
	public static final String TEAMCENTER_BOMLINE_VARIANT_RULE_TEXT = "bl_rev_ct4_variant_rule_text_id";
	public static final String TEAMCENTER_BOMLINE_WINDOW = "bl_window";

	public static final String TEAMCENTER_BOMLINE_IS_CLASSIFIED = "bl_is_classified";

	// C
	public static final String TEAMCENTER_CABIN = "CT4Cabin";
	public static final String TEAMCENTER_CID = "cid";
	public static final String TEAMCENTER_CLASS_ID = "ct4_CLASS_ID";
	public static final String TEAMCENTER_CONFIG_PERSPECTIVE = "smc0ConfigPerspective";
	public static final String TEAMCENTER_CURRENT_NAME = "current_name";

	// D
	public static final String TEAMCENTER_DRAWING_NO = "ct4DrawingNo";
	public static final String TEAMCENTER_DRY_RUN = "DryRun";

	// F
	public static final String TEAMCENTER_FEATURE = "Cfg0Feature";
	public static final String TEAMCENTER_FEATURE_FAMILY = "Cfg0FeatureFamily";
	public static final String TEAMCENTER_FIND_NO = "bl_sequence_no";
	public static final String TEAMCENTER_FMS_BOOTSTRAP_URL = "Fms_BootStrap_Urls";
	public static final String TEAMCENTER_FULL_OR_PARTIAL_MATCHING = "fullOrPartialMatching";

	// H
	public static final String TEAMCENTER_HAS_FREE_FORM_VALUES = "cfg0HasFreeFormValues";

	// I
	public static final String TEAMCENTER_IMAN_RENDERING = "IMAN_Rendering";
	public static final String TEAMCENTER_IMAN_SPECIFICATION = "IMAN_specification";
	public static final String TEAMCENTER_ITEM_ID = "item_id";
	public static final String TEAMCENTER_ITEM_REVISION = "item_revision";
	public static final String TEAMCENTER_ITEM_TAG = "items_tag";

	// L
	public static final String TEAMCENTER_LOV = "CT4_LOV";
	public static final String TEAMCENTER_LOV_CONTEXT_OBJECT = "fnd0LOVContextObject";
	public static final String TEAMCENTER_LOV_CONTEXT_PROP_NAME = "fnd0LOVContextPropName";

	// M
	public static final String TEAMCENTER_MODUL = "CT4modul";
	public static final String TEAMCENTER_MODULAR_BUILDING = "CT4MB";

	// N
	public static final String TEAMCENTER_NUMBER_OF_LINES_TO_PROCESS = "NumberOfLinesToProcess";

	// O
	public static final String TEAMCENTER_OBJECT_DESCRIPTION = "object_desc";
	public static final String TEAMCENTER_OBJECT_GROUP = "ct4_object_gr";
	public static final String TEAMCENTER_OBJECT_NAME = "object_name";
	public static final String TEAMCENTER_OBJECT_STRING = "object_string";
	public static final String TEAMCENTER_OBJECT_TYPE = "object_type";
	public static final String TEAMCENTER_OPTIONAL = "cfg0IsDiscretionary";
	public static final String TEAMCENTER_OUTPUT_PAGE_SIZE = "outputPageSize";

	// P
	public static final String TEAMCENTER_PART = "Part";
	public static final String TEAMCENTER_POSITION_DESIGNATOR = "bl_position_designator";
	public static final String TEAMCENTER_PRODUCT_FAMILY = "Cfg0ProductModelFamily";
	public static final String TEAMCENTER_PSEUDO_SERIAL_NUMBER = "CT4_PseudoSN";

	// Q
	public static final String TEAMCENTER_QUANTITY = "bl_quantity";

	// R
	public static final String TEAMCENTER_RELEASE_STATUS_LIST = "release_status_list";
	public static final String TEAMCENTER_REVISION_LIST = "revision_list";
	public static final String TEAMCENTER_REVISIONRULE = "RevisionRule";
	public static final String TEAMCENTER_REVISION_RULE = "revision_rule";
	public static final String TEAMCENTER_RULE_DATE = "rule_date";
	public static final String TEAMCENTER_RULE_SET_COMPILE_DATE = "cfg0RuleSetCompileDate";

	// S
	public static final String TEAMCENTER_SECONDARY_OBJECT = "secondary_object";
	public static final String TEAMCENTER_SOLUTION_VARIANT_CATEGORY = "smc0SolutionVariantCategory";
	public static final String TEAMCENTER_SOLUTION_VARIANT_SOURCE = "Smc0SolutionVariantSource";
	public static final String TEAMCENTER_SOLUTION_VARIANT_SOURCE_CATEGORY = "smc0SVSourceCategory";
	public static final String TEAMCENTER_STOP_ON_ERROR = "StopOnError";
	public static final String TEAMCENTER_STORAGE_CLASS = "ct4_StorCLASS_ref";

	// U
	public static final String TEAMCENTER_USER_OR_SYSTEM_SELECTED = "userOrSystemSelected";

	// V
	public static final String TEAMCENTER_VALID_AND_COMPLETE = "ValidAndComplete";
	public static final String TEAMCENTER_VALID_AND_INCOMPLETE = "ValidAndInComplete";

	// W
	public static final String TEAMCENTER_WORKFLOW_SBOMI_TRIGGER = "SBOMI_Freigabe";

	//////////////////////////////
	// Teamcenter Dataset names //
	//////////////////////////////

	// B
	public static final String TEAMCENTER_DATASET_BITMAP = "Bitmap";

	// D
	public static final String TEAMCENTER_DATASET_DWG = "CT4_DWG";
	public static final String TEAMCENTER_DATASET_DXF = "DXF";

	// I
	public static final String TEAMCENTER_DATASET_IMAGE = "Image";

	// J
	public static final String TEAMCENTER_DATASET_JSON = "CT4_JSON";
	public static final String TEAMCENTER_DATASET_JPG = "JPEG";
	public static final String TEAMCENTER_DATASET_JPG_REFERENCE = "JPEG_Reference";

	// P
	public static final String TEAMCENTER_DATASET_PDF = "PDF";
	public static final String TEAMCENTER_DATASET_PDF_REFERENCE = "PDF_Reference";
	public static final String TEAMCENTER_DATASET_PNG = "PNG";

	//queue entity Query Constants - search criteria
	public static final String SC_FILE_NAME = "1";
	public static final String SC_DRAWING_NUMBER = "2";
	public static final String SC_TEAMCENTER_ROOT_OBJECT = "3";
	public static final String SC_LOG_FILE_NAME = "4";

}
