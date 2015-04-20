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
 * $Id: EntitlementConditionAdaptor.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 *
 * Portions Copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;
import org.forgerock.openam.utils.CollectionUtils;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class EntitlementConditionAdaptor
    implements EntitlementCondition {
    private String displayType;

    /**
     * Sets display type.
     *
     * @param displayType Display Type.
     */
    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    /**
     * Returns display type.
     *
     * @return Display Type.
     */
    public String getDisplayType() {
        return displayType;
    }

    /**
     * Initializes the condition object.
     *
     * @param parameters Parameters for initializing the condition.
     */
    public void init(Map<String, Set<String>> parameters) {
    }

    protected void setState(JSONObject jo) {
        displayType = (jo.has("displayType")) ? jo.optString("displayType") :
            null;
    }

    protected void toJSONObject(JSONObject jo) 
        throws JSONException {
        if (displayType != null) {
            jo.put("displayType", displayType);
        }
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return  <code>true</code> if the passed in object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || !getClass().equals(obj.getClass())) {
            return false;
        }
        EntitlementConditionAdaptor other = (EntitlementConditionAdaptor)obj;
        return CollectionUtils.genericCompare(this.displayType, other.displayType);
    }

    /**
     * Returns hash code of the object.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {
        return (displayType == null) ? 0 : displayType.hashCode();
    }
}
