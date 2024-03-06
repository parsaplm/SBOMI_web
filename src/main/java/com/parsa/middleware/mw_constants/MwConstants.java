package com.parsa.middleware.mw_constants;

public class MwConstants {

    public static final String URL = "url";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String ITEM_TYPE = "itemType";
    public static final String ITEM_ID = "itemId";
    public static final String ITEM_NAME = "itemName";
    public static final String ITEM_REV_ID = "itemRevId";

    public static final String EXPORT_TYPE = "exportType";

    public static final String REVISION_RULE = "revisionRule";

    public static final String PARENT_ID = "parentId";

    public static final String ITEM_LIST = "itemlist";
    public static final String STRUCTURE_LIST = "structurelist";

    public static final String MW_ITEM_LIST = "mwitemlist";

    public static final String FAMILY_FEATURE_LIST = "familyfeaturelist";


    public static final String IS_FAMILY = "isFamily";

    public static final String STATUS_SUCCESS = "SUCCESS";

    public static final String TC_SESSION_OBJ = "TC_SESSION_OBJ";

    public static final String SRC_SYSTEM_TC = "Teamcenter";
    public static final String SRC_SYSTEM_INFOR = "INFORLN";
    public static final String TRANS_STATE_TRANSFERRING = "Transferring";
    public static final String TRANS_STATE_TRANSFERRED = "Transferred";
    public static final String TRANS_STATE_NEW = "New";

    public static final String PROCESS_TYPE = "processType";

    public static final int PROCESS_ITEM_EXPORT = 0;
    public static final int PROCESS_STRUCTURE_EXPORT = 1;

    public static final int PROCESS_FAMILY_FEATURE_EXPORT = 2;

    public static final int PROCESS_FAMILY_FEATURE_BULK_EXPORT = 3;

    //status codes for mw_transaction_sys_log table
    public static final int NEW = 0;
    public static final int SYNCHRONISED = 1;
    public static final int FAILED = 2;

    //response variable
    public static final String ERR_CODE = "errCode";
    public static final String ERR_MSG = "errMsg";
    public static final String TC_ITEM_NOT_FOUND_CODE = "666";
    public static final String TC_STRUCTURE_NOT_FOUND_CODE = "777";
    public static final String TC_FAMILY_FEATURE_NOT_FOUND_CODE = "888";

    public static final String TC_BULK_FAMILY_FEATURE_NOT_FOUND_CODE = "999";
    public static final String TC_FAMILY_FEATURE_CREATE_SUCCESS_CODE = "880";
    public static final String TC_BULK_FAMILY_FEATURE_CREATE_SUCCESS_CODE = "990";
    public static final String TC_FAMILY_FEATURE_UPDATE_SUCCESS_CODE = "881";
    public static final String TC_BULK_FAMILY_FEATURE_UPDATE_SUCCESS_CODE = "991";
    public static final String TC_ITEM_NOT_FOUND_MSG = "Item Not Found in Teamcenter";
    public static final String TC_STRUCTURE_NOT_FOUND_MSG = "Structure Not Found in Teamcenter";
    public static final String TC_FAMILY_FEATURE_NOT_FOUND_MSG = "Family/Feature Not Found in Teamcenter";
    public static final String TC_FAMILY_FEATURE_CREATE_MSG = "Family/Feature Created Successfully";
    public static final String TC_FAMILY_FEATURE_UPDATE_MSG = "Family/Feature Updated Successfully";
    public static final String TC_BULK_FAMILY_FEATURE_CREATE_MSG = "Family/Feature Created Successfully";
    public static final String TC_BULK_FAMILY_FEATURE_UPDATE_MSG = "Family/Feature Updated Successfully";


}
