package com.parsa.middleware.util;


import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.RevisionRule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Utils {

//    public static HashMap<String, String> getVariantRule(String variantRuleText) {
//        return (HashMap<String, String>) Arrays.asList(variantRuleText.split("AND")).stream().map(s -> s.split("=")).collect(Collectors.toMap(e -> e[0].replaceAll("\\[(.*?)\\]", ""), e -> e[1]));
//    }

    public static HashMap<String, String> getVariantRule(String variantRuleText) throws Exception {
        if (variantRuleText == null || variantRuleText.isEmpty()) {
            throw new IllegalArgumentException("Variant rule text is empty or null");
        }

        String[] rulePairs = variantRuleText.split("AND");
        HashMap<String, String> variantRuleMap = new HashMap<>();

        for (String rulePair : rulePairs) {
            String[] keyValue = rulePair.split("=");

            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Invalid variant rule format: " + variantRuleText);
            }

            String key = keyValue[0].replaceAll("\\[(.*?)\\]", "");
            String value = keyValue[1];

            if (variantRuleMap.containsKey(key)) {
                throw new Exception("Duplicate key '" + key + "' found in variant rule: " + variantRuleText);
            }

            variantRuleMap.put(key, value);
        }

        return variantRuleMap;
    }

    /**
     * Retrieves the revision rule as per input value. It looks for revision
     * rule from all available revision rules in system.
     * @param revRuleName Revision Rule name. If null or empty string passed,
     *  first value from all revision rules is returned.
     * @return Revision Rule object
     * @throws Exception
     */
    public static RevisionRule getRevisionRuleByName(String revRuleName, Connection m_connection) throws Exception
    {
        RevisionRule revRule = null;
        StructureManagementService m_smService = StructureManagementService.getService(m_connection);
        StructureManagement.GetRevisionRulesResponse ruleResp = m_smService.getRevisionRules();

        RevisionRule retRevRule = null;

        // If an empty or null string passed, return first value
        if(revRuleName == null || revRuleName.length() == 0)
        {
            retRevRule = ruleResp.output.length > 0 ? ruleResp.output[0].revRule : null;
            return retRevRule;
        }

        for(StructureManagement.RevisionRuleInfo revRuleInfo : ruleResp.output)
        {
            revRule = revRuleInfo.revRule;

            com.teamcenter.soa.client.model.Property[] objNameProp = getObjectProperties(revRule, new String[] {"object_name"}, m_connection);
            if(objNameProp[0].getDisplayableValue().equals(revRuleName))
            {
                retRevRule = revRule;
                break;
            }
        }

        return retRevRule;
    }

    /**
     * This function loads the property explicitly which are not part of property policy
     * @param object
     * @param properties
     * @return
     * @throws Exception
     */
    public static Property[] getObjectProperties (ModelObject object, String[] properties, Connection m_connection) throws Exception
    {
        Property[] retVal = new Property[properties.length];

        com.teamcenter.services.strong.core.DataManagementService m_dmService = com.teamcenter.services.strong.core.DataManagementService.getService(m_connection);
        ServiceData sd = m_dmService.getProperties(new ModelObject[]{object}, properties);
        ModelObject modelObject = sd.getPlainObject(0);

        int i = 0;
        for(String prop : properties)
        {
            retVal[i++] = modelObject.getPropertyObject(prop);
        }
        return retVal;
    }

    public static <T extends CharSequence> T handleNull(T value, T defaultValue) {
        return value != null && value.length() > 0 ? value : defaultValue;
    }






}
