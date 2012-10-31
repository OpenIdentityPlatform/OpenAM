/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthToRealmHelper.java,v 1.2 2008/06/25 05:43:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.plugins.AuthenticateToRealmCondition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AuthToRealmHelper {
    public static final String ATTR_FILTER = "tfSearch";
    public static final String ATTR_VALUE =
        AuthenticateToRealmCondition.AUTHENTICATE_TO_REALM;
    private static AuthToRealmHelper instance = new AuthToRealmHelper();

    private AuthToRealmHelper() {
    }

    public static AuthToRealmHelper getInstance() {
        return instance;
    }

    public String getConditionXML(boolean bCreate, boolean readonly) {
        String xml = null;

        if (bCreate) {
            xml = "com/sun/identity/console/propertyAuthToRealm.xml";
        } else {
            xml = (readonly) ?
                "com/sun/identity/console/propertyAuthToRealm_Readonly.xml" :
                "com/sun/identity/console/propertyAuthToRealm.xml";
        }
        return AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(xml));
    }

    public String getMissingValuesMessage() {
        return "policy.condition.missing.authtorealm.message";
    }

    public void setPropertiesValues(
        Map values,
        AMPropertySheetModel propertySheetModel,
        AMModel model
    ) {
        if ((values != null) && !values.isEmpty()) {
            String propName = (String)values.keySet().iterator().next();
            Set val = (Set)values.get(propName);

            if ((val != null) && !val.isEmpty()) {
                if (propName.equals(ATTR_VALUE)) {
                    propertySheetModel.setValue(propName,
                        (String)val.iterator().next());
                }
            }
        }
    }

    public Map getConditionValues(
        PolicyModel model,
        AMPropertySheetModel propertySheetModel
    ) {
        Map map = new HashMap(2);
        String value = (String)propertySheetModel.getValue(ATTR_VALUE);
        if (value.trim().length() > 0) {
            HashSet set = new HashSet(2);
            set.add(value);
            map.put(ATTR_VALUE, set);
        }
        return map;
    }

    /**
     * Returns a set of realm names.
     *
     * @param filter Search Filter.
     * @param model Policy Model.
     * @return a set of realm names.
     * @throws AMConsoleException if search operation failed.
     */
    public Set getRealmNames(String filter, PolicyModel model)
        throws AMConsoleException
    {
        filter = ((filter == null) || (filter.length() == 0)) ?
            filter = "*" : filter.trim();
        return model.getRealmNames("/", filter);
    }
}
