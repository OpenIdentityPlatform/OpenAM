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
 * $Id: AuthToServiceHelper.java,v 1.2 2008/06/25 05:43:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.console.authentication.model.AuthConfigurationModelImpl;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.plugins.AuthenticateToServiceCondition;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for authenticate to service (add and edit) view bean.
 */
public class AuthToServiceHelper {
    private static SSOToken adminSSOToken =
            (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());

    /**
     * Name of search filter text field.
     */
    public static final String ATTR_FILTER = "tfSearch";

    /**
     * Name of realm list box.
     */
    public static final String ATTR_REALM = "Realm";

    /**
     * Name of service list box.
     */
    public static final String ATTR_SERVICE =
            AuthenticateToServiceCondition.AUTHENTICATE_TO_SERVICE;

    private static AuthToServiceHelper instance = new AuthToServiceHelper();

    private AuthToServiceHelper() {
    }

    /**
     * Returns an instance of this singleton.
     *
     * @return an instance of this singleton.
     */
    public static AuthToServiceHelper getInstance() {
        return instance;
    }

    /**
     * Set properties to property sheet model.
     *
     * @param values Values (realm concatenate service).
     * @param propertySheetModel Property Sheet Model.
     */
    public void setPropertiesValues(
        Map values,
        AMPropertySheetModel propertySheetModel
    ) {
        if ((values != null) && !values.isEmpty()) {
            String propName = (String)values.keySet().iterator().next();
            Set val = (Set)values.get(propName);

            if ((val != null) && !val.isEmpty()) {
                if (propName.equals(ATTR_SERVICE)) {
                    String str = (String)val.iterator().next();
                    propertySheetModel.setValue(ATTR_REALM,
                            AMAuthUtils.getRealmFromRealmQualifiedData(str));
                    propertySheetModel.setValue(ATTR_SERVICE,
                            AMAuthUtils.getDataFromRealmQualifiedData(str));
                }
            }
        }
    }

    /**
     * Returns condition values (realm concatenate service).
     *
     * @param model Policy Model.
     * @param propertySheetModel Property Sheet Model.
     * @return condition values.
     * @throws AMConsoleException if values are invalid.
     */
    public Map getConditionValues(
        PolicyModel model,
        AMPropertySheetModel propertySheetModel
    ) throws AMConsoleException
    {
        String realmValue = (String)propertySheetModel.getValue(ATTR_REALM);

        String service = 
                (String)propertySheetModel.getValue(ATTR_SERVICE);
        if ((service == null) || (service.length() == 0)) {
            throw new AMConsoleException(model.getLocalizedString(
                    "policy.condition.missing.auth.service"));
        }

        HashSet set = new HashSet(2);
        Map map = new HashMap(2);
        set.add(AMAuthUtils.toRealmQualifiedAuthnData(realmValue, service));
        map.put(ATTR_SERVICE, set);
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

    /**
     * Returns service names that are registered in the realm.
     *
     * @param base Base realm name for this search. null indicates root
     *        suffix.
     * @return service names that are registered in the realm.
     * @throws AMConsoleException if search fails.
     */
    public Set getAssignedServiceNamesInRealm(String base, PolicyModel model)
        throws AMConsoleException
    {
        return AuthConfigurationModelImpl.getNamedConfigurations(
                adminSSOToken, base);
    }
}
