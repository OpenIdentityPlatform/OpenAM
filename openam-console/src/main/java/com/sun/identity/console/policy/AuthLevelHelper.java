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
 * $Id: AuthLevelHelper.java,v 1.2 2008/06/25 05:43:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.plugins.AuthLevelCondition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for authentication level (add and edit) view bean.
 */
public class AuthLevelHelper {
    /**
     * Name of search filter text field.
     */
    public static final String ATTR_FILTER = "tfSearch";

    /**
     * Name of realm list box.
     */
    public static final String ATTR_REALM = "Realm";

    /**
     * Name of authentication level textbox.
     */
    public static final String ATTR_AUTH_LEVEL = AuthLevelCondition.AUTH_LEVEL;

    private static AuthLevelHelper instance = new AuthLevelHelper();

    private AuthLevelHelper() {
    }

    /**
     * Returns an instance of authentication level helper object.
     *
     * @return an instance of authentication level helper object.
     */
    public static AuthLevelHelper getInstance() {
        return instance;
    }

    /**
     * Set the condition values to property sheet model.
     *
     * @param values Values which contains realm name and authentication level.
     * @param propertySheetModel Property sheet model.
     */
    public void setPropertiesValues(
        Map values,
        AMPropertySheetModel propertySheetModel
    ) {
        if ((values != null) && !values.isEmpty()) {
            String propName = (String)values.keySet().iterator().next();
            Set val = (Set)values.get(propName);

            if ((val != null) && !val.isEmpty()) {
                if (propName.equals(ATTR_AUTH_LEVEL)) {
                    String str = (String)val.iterator().next();
                    propertySheetModel.setValue(ATTR_REALM,
                            AMAuthUtils.getRealmFromRealmQualifiedData(str));
                    propertySheetModel.setValue(ATTR_AUTH_LEVEL,
                            AMAuthUtils.getDataFromRealmQualifiedData(str));
                }
            }
        }
    }

    /**
     * Returns condition values by concatentating realm and authentication
     * level.
     *
     * @param model Policy Model.
     * @param propertySheetModel Property Sheet Model for extract realm and
     *        authentication level values.
     * @return condition values.
     * @throws AMConsoleException if realm or authentication level is blank.
     */
    public Map getConditionValues(
        PolicyModel model,
        AMPropertySheetModel propertySheetModel
    ) throws AMConsoleException
    {
        String realmValue = (String)propertySheetModel.getValue(ATTR_REALM);
        realmValue = realmValue.trim();

        String levelValue = 
                (String)propertySheetModel.getValue(ATTR_AUTH_LEVEL);
        levelValue = levelValue.trim();
        if (levelValue.length() == 0) {
            throw new AMConsoleException(model.getLocalizedString(
                    "policy.condition.missing.auth.level"));
        }

        HashSet set = new HashSet(2);
        Map map = new HashMap(2);
        set.add(AMAuthUtils.toRealmQualifiedAuthnData(realmValue, levelValue));
        map.put(ATTR_AUTH_LEVEL, set);
        return map;
    }

    /**
     * Returns a set of realm names
     *
     * @param filter Search filter.
     * @param model Policy Model.
     * @return a set of realm names
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
