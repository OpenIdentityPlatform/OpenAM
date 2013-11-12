/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMUserPasswordValidationPlugin.java,v 1.4 2008/08/13 15:55:36 pawand Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common;

import java.security.AccessController;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMInvalidDNException;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMServiceUtils;
import com.iplanet.am.sdk.AMUserPasswordValidation;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;

/**
 * This class is an utility to detect the specified pattern in the given string
 * 
 */
public class AMUserPasswordValidationPlugin extends AMUserPasswordValidation
        implements AMConstants {
    static Debug debug = Debug.getInstance("amProfile_ldap");

    private static String SEPERATOR = "|";

    public AMUserPasswordValidationPlugin() {
    }

    /**
     * Checks for invalid characters in the source string
     * 
     * @param userID
     *            source string which should be validated
     * @param envParams
     *            parameters for which the userID validation is enforced.
     * @throws throws
     *             AMException when it detects specified pattern within source
     *             string which need to be validated OR if source string is null
     */

    public void validateUserID(String userID, Map envParams) throws AMException 
    {

        StringBuilder errorString = new StringBuilder(10);
        SSOToken token = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());
        String orgDN = (String) envParams
                .get(com.sun.identity.shared.Constants.ORGANIZATION_NAME);
        String regEx = getOrgUserInvalidChars(orgDN, token);
        if (userID == null || userID.length() == 0) {
            debug.error("AMUserPasswordValidationPlugin."
                    + "validateUserID() : Source string is null or empty"
                    + userID);
            throw new AMInvalidDNException(AMSDKBundle.getString("157"), "157");
        }
        if (regEx == null || regEx.length() == 0) {
            debug.error("AMUserPasswordValidationPlugin."
                    + "validateUserID() : List of invalid characters is null "
                    + "or empty" + regEx);
            throw new AMInvalidDNException(AMSDKBundle.getString("157"), "157");
        }
        StringTokenizer st = new StringTokenizer(regEx, SEPERATOR);
        while (st.hasMoreTokens()) {
            String obj = st.nextToken();
            if (userID.indexOf(obj) > -1) {
                debug.error("AMUserPasswordValidationPlugin."
                        + "validateUserID() : Detected invalid chars ...");
                debug.error("AMUserPasswordValidationPlugin."
                        + "validateUserID() : User Name validation Failed:"
                        + obj);
                errorString.append(obj).append(" ");
            }
        }
        Object args[] = { userID, errorString.toString() };

        if (errorString.length() != 0) {
            throw new AMException(AMSDKBundle.getString("1002", args), "1002",
                    args);

        }
    }

    private String getOrgUserInvalidChars(String orgDN, SSOToken token) {
        try {
            String cachedValue = AdministrationServiceListener.
                getOrgInvalidCharsFromCache(orgDN);
            if (cachedValue != null) {
	        return cachedValue;
            }
            // Get the org config
            ServiceConfig sc = AMServiceUtils.getOrgConfig(token, orgDN,
                    ADMINISTRATION_SERVICE);
            if (sc != null) {
                Map attributes = sc.getAttributes();
                Set value = (Set) attributes.get(INVALID_USERID_CHARACTERS);
                String invalidChars =  ((value == null || value.isEmpty()) ? 
                    null : (String) value.iterator().next());
                AdministrationServiceListener.setOrgInvalidCharsInCache(orgDN,
                    invalidChars);
                return invalidChars;
            } else {
                return getGlobalUserInvalidChars(token);
            }
        } catch (Exception ee) {
            return getGlobalUserInvalidChars(token);
        }
    }

    private String getGlobalUserInvalidChars(SSOToken token) {
        // Org Config may not exist. Get default values
        String cachedValue = AdministrationServiceListener.
            getGlobalInvalidCharsFromCache();
        if (cachedValue != null) {
	    return cachedValue;
        }
        if (debug.messageEnabled()) {
            debug.message("AMUserPasswordValidationPlugin."
                    + "getGlobalUserInvalidChars(): Organization config for "
                    + "service (" + ADMINISTRATION_SERVICE + ","
                    + INVALID_USERID_CHARACTERS + ") not found. "
                    + "Obtaining default service config values ..");
        }
        try {
            Map defaultValues = AMServiceUtils.getServiceConfig(token,
                    ADMINISTRATION_SERVICE, SchemaType.ORGANIZATION);
            if (defaultValues != null) {
                Set value = (Set) defaultValues.get(INVALID_USERID_CHARACTERS);
                String invalidChars = ((value == null || value.isEmpty()) ? 
                    null : (String) value.iterator().next());
                
                AdministrationServiceListener.setGlobalInvalidCharsInCache(
                    invalidChars);
                return invalidChars;
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("AMUserPasswordValidationPlugin."
                        + "getGlobalUserInvalidChars(): Unable to get "
                        + "UserID invalid characters", e);
            }
        }
        return null;
    }
}
