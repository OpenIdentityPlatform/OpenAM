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
 * $Id: MAPModelBase.java,v 1.3 2009/01/28 05:34:57 ww203982 Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.am.util.AMClientDetector;
import com.sun.identity.shared.locale.Locale;
import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.clientschema.AMClientCapData;
import com.iplanet.services.cdm.clientschema.AMClientCapException;
import com.iplanet.services.cdm.DefaultClientTypesManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMResBundleCacher;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.shared.ldap.util.DN;

/* - NEED NOT LOG - */

public abstract class MAPModelBase
    extends AMModelBase
    implements MAPModel
{
    private static char[] invalidCharacters =
        new char[DN.ESCAPED_CHAR.length + 5];

    static {
        int i = 0;
        for (; i < DN.ESCAPED_CHAR.length; i++) {
            invalidCharacters[i] = DN.ESCAPED_CHAR[i];
        }
        invalidCharacters[i++] = ' ';
        invalidCharacters[i++] = '/';
        invalidCharacters[i++] = ':';
        invalidCharacters[i++] = '=';
        invalidCharacters[i] = '\\';
    }

    DefaultClientTypesManager clientTypesManager;
    String mapServiceName;
    private AMClientCapData clientCapDataIntInstance = null;
    private AMClientCapData clientCapDataExtInstance = null;
    private ResourceBundle serviceResourceBundle = null;

    MAPModelBase(HttpServletRequest req, Map map) {
        super(req, map);
        clientTypesManager = (DefaultClientTypesManager)
            AMClientDetector.getClientTypesManagerInstance();
        initClientCapDataInstance();
        getServiceResourceBundle();
    }

    private void initClientCapDataInstance() {
        try {
            clientCapDataExtInstance = AMClientCapData.getExternalInstance();
            clientCapDataIntInstance = AMClientCapData.getInternalInstance();
            mapServiceName = clientCapDataIntInstance.getServiceName();
        } catch (AMClientCapException e) {
            debug.error("MAPModelBase.initClientCapDataInstance", e);
        }
    }

    private void getServiceResourceBundle() {
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(
                mapServiceName, getUserSSOToken());
            String name = scm.getI18NFileName();

            if ((name != null) && (name.length() > 0)) {
                serviceResourceBundle = AMResBundleCacher.getBundle(name,
                        getUserLocale());
            }
        } catch (SMSException e) {
            debug.warning("MAPModelBase.getServiceResourceBundle", e);
        } catch (SSOException e) {
            debug.warning("MAPModelBase.getServiceResourceBundle", e);
        }
    }

    /**
     * Canonicalizes un-support characters in LDAP DN.
     *
     * @param str  string to be canonicalized.
     * @return canonicalized string.
     */
    protected static String canonicalize(String str) {
        for (int i = 0; i < invalidCharacters.length; i++) {
            str = str.replace(invalidCharacters[i], '_');
        }
        return str;
    }
                                                                                
    /**
     * Returns true if an user agent name is valid.
     *
     * @param userAgent  user agent name.
     * @return true if <code>userAgent</code> is valid.
     */
    protected static boolean isClientTypeValid(String userAgent) {
        boolean valid = true;
        for (int i = 0; (i < invalidCharacters.length) && valid; i++) {
            valid = (userAgent.indexOf(invalidCharacters[i]) == -1);
        }
        return valid;
    }

    protected AMClientCapData getClientCapDataIntInstance() {
        return clientCapDataIntInstance;
    }

    protected AMClientCapData getClientCapDataExtInstance() {
        return clientCapDataExtInstance;
    }

    protected AMClientCapData getClientCapDataInstance(String clientType) {
        Map temp = clientCapDataIntInstance.getProperties(clientType);
        return (temp != null) ? clientCapDataIntInstance : 
            clientCapDataExtInstance;
    }

    /**
     * Returns styles of a profile.
     *
     * @param name Name of profile.
     * @return styles of a profile.
     */
    public Set getStyleNames(String name) {
        return (clientTypesManager != null)
            ? clientTypesManager.getStyles(name) : Collections.EMPTY_SET;
    }

    protected String getL10NStrFromSvcResourceBundle(String key) {
        return (serviceResourceBundle != null) ?
            Locale.getString(serviceResourceBundle, key, debug) : key;
    }

    protected AttributeSchema getDeviceAttributeSchema(String name) {
        AttributeSchema as = null;

        if ((clientCapDataIntInstance != null) &&
            (clientCapDataExtInstance != null))
        {
            as = clientCapDataIntInstance.getAttributeSchema(name);

            if (as == null) {
                as = clientCapDataExtInstance.getAttributeSchema(name);
            }

            if (as != null) {
                String i18nKey = as.getI18NKey();
                if ((i18nKey == null) || (i18nKey.length() == 0)) {
                    as = null;
                }
            }
        }

        return as;
    }

    protected Set getAdditionalPropertyValues(String clientType) {
        Set values = Collections.EMPTY_SET;
        Client client = clientTypesManager.getClientInstance(clientType);
        if (client != null) {
            values = client.getAdditionalProperties();
        }
        return values;
    }

    protected boolean isUserAgentSet(String clientType) {
        boolean isSet = true;
        AMClientCapData ccd = getClientCapDataInstance(clientType);
        Map map = ccd.getProperties(clientType);
        if (map != null) {
            isSet = (map.get(ATTRIBUTE_NAME_USER_AGENT) != null);
        }
        return isSet;
    }

    /**
     * Returns true if the given client is customizable.
     *
     * @param clientType Client Type.
     * @return true if the given client is customizable.
     */
    public boolean isCustomizable(String clientType) {
        return clientCapDataExtInstance.isClientPresent(clientType);
    }

    /**
     * Returns true if the given client is customizable.
     *
     * @param clientType Client Type.
     * @return true if the given client is customizable.
     */
    public boolean hasDefaultSetting(String clientType) {
        return clientCapDataExtInstance.isClientPresent(clientType) &&
            clientCapDataIntInstance.isClientPresent(clientType);
    }

    /**
     * Returns true if the client can be deleted.
     *
     * @param clientType Client Type.
     * @return true if the client can be deleted.
     */
    public boolean canBeDeleted(String clientType) {
        return !clientCapDataIntInstance.isClientPresent(clientType);
    }

    /**
     * Returns device user agent of a client.
     *
     * @param clientType Client Type.
     * @return device user agent of a client.
     */
    public String getDeviceUserAgent(String clientType) {
        String value = null;
        AMClientCapData ccd = getClientCapDataInstance(clientType);
        Map map = ccd.getProperties(clientType);
        if (map != null) {
            value = (String)AMAdminUtils.getValue(
                (Set)map.get(ATTRIBUTE_NAME_USER_AGENT));
        }
        return value;
    }

    /**
     * Returns prefix for client type.
     *
     * @return prefix for client type.
     */
    public String getClientTypePrefix() {
        return getLocalizedString("mapCloneClient.prefix");
    }

    /**
     * Returns prefix for device user agent.
     *
     * @return prefix for device user agent.
     */
    public String getDeviceNamePrefix() {
        return getLocalizedString("mapCloneClient.name");
    }
}
