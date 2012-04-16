/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SMSDataEntry.java,v 1.1 2009/04/02 19:40:59 veiming Exp $
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.sm;

import com.sun.identity.shared.JSONUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class encapsulates a distinguished name and its attribute values.
 */
public class SMSDataEntry {
    private String dn;
    private Map attributeValues;

    /**
     * Constructs an instance.
     *
     * @param dn Distinguished name.
     * @param attributeValues attribute values.
     */
    public SMSDataEntry(String dn, Map attributeValues) {
        this.dn = dn;
        this.attributeValues = new HashMap();
        parseAttributeValues(attributeValues);
    }

    public SMSDataEntry(String jsonString) throws JSONException {
        
        JSONObject o = new JSONObject(jsonString);
        this.dn = o.getString("dn");
        this.attributeValues = JSONUtils.getMapStringSetString(o, "attributeValues");   
    }

    public String getDN() {
        return dn;
    }

    private void parseAttributeValues(Map raw) {
        parseAttributeValues((Set)raw.get(SMSEntry.ATTR_XML_KEYVAL));
        parseAttributeValues((Set)raw.get(SMSEntry.ATTR_KEYVAL));
    }

    private void parseAttributeValues(Set raw) {
        for (Iterator i = raw.iterator(); i.hasNext(); ) {
            String s = (String)i.next();
            int idx = s.indexOf('=');
            if (idx != -1) {
                String name = s.substring(0, idx);
                String value = s.substring(idx+1);

                Set set = (Set)attributeValues.get(name);
                if (set == null) {
                    set = new HashSet();
                    attributeValues.put(name, set);
                }
                set.add(value);
            }
        }
    }

    public Set getAttributeValues(String attributeName) {
        return (Set)attributeValues.get(attributeName);
    }

    public String getAttributeValue(String attributeName) {
        Set val = (Set)attributeValues.get(attributeName);
        return ((val != null) && !val.isEmpty()) ?
            (String)val.iterator().next() : null;
    }
    
    public String toJSONString() throws JSONException {
        
        JSONObject result = new JSONObject();
        
        result.put("dn", dn);
        result.put("attributeValues", attributeValues);
                
        return result.toString();
    }
}
