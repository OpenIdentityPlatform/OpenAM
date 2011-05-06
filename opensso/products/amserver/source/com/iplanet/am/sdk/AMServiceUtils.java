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
 * $Id: AMServiceUtils.java,v 1.7 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

/**
 * This class has some of the most commonly used Service Management
 * functionality.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public class AMServiceUtils {

    private static Debug debug = Debug.getInstance("amProfile");

    /**
     * Get attribute names for the specified Service and Schema Type
     * 
     * @param token
     *            SSOToken a valid SSOToken
     * @param serviceName
     *            the service name
     * @param type
     *            the SchemaType
     * @return the Set of attribute names for that specified Service and Schema
     *         Type
     */
    protected static Set getServiceAttributeNames(SSOToken token,
            String serviceName, SchemaType type) throws SMSException,
            SSOException {
        ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName, token);
        ServiceSchema ss = null;
        try {
            ss = ssm.getSchema(type);
        } catch (SMSException sme) {
            if (debug.warningEnabled()) {
                debug.warning("AMServiceUtils.getServiceAttributeNames():"
                        + " No schema defined for " + type);
            }
        }

        if ((ss == null) || (type == SchemaType.POLICY)) {
            return Collections.EMPTY_SET;
        }

        return ss.getAttributeSchemaNames();
    }

    /**
     * Method to get the attribute names of a service with CosQualifier. For
     * example: Return set could be: <br>
     * ["iplanet-am-web-agent-allow-list merge-schemes",
     * "iplanet-am-web-agent-deny-list merge-schemes"]
     * <p>
     * This method only populates these attributes only for Dynamic attributes.
     */
    protected static Set getServiceAttributesWithQualifier(SSOToken token,
            String serviceName) throws SMSException, SSOException {

        ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName, token);
        ServiceSchema ss = null;
        try {
            ss = ssm.getSchema(SchemaType.DYNAMIC);
        } catch (SMSException sme) {
            if (debug.warningEnabled()) {
                debug.warning(
                        "AMServiceUtils.getServiceAttributesWithQualifier(): " +
                        "No schema defined for SchemaType.DYNAMIC type");
            }
        }

        if (ss == null) {
            return Collections.EMPTY_SET;
        }

        Set attrNames = new HashSet();
        Set attrSchemaNames = ss.getAttributeSchemaNames();
        Iterator itr = attrSchemaNames.iterator();
        while (itr.hasNext()) {
            String attrSchemaName = (String) itr.next();
            AttributeSchema attrSchema = ss.getAttributeSchema(attrSchemaName);
            String name = attrSchemaName + " " + attrSchema.getCosQualifier();
            attrNames.add(name);
        }
        return attrNames;
    }

    /**
     * Returns true if the service has the subSchema. False otherwise.
     * 
     * @param token
     *            SSOToken a valid SSOToken
     * @param serviceName
     *            the service name
     * @param schemaType
     *            service schema type (Dynamic, Policy etc)
     * @return true if the service has the subSchema.
     */
    public static boolean serviceHasSubSchema(SSOToken token,
            String serviceName, SchemaType schemaType) throws SMSException,
            SSOException {
        boolean schemaTypeFlg = false;
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName,
                    token);
            Set types = ssm.getSchemaTypes();
            if (debug.messageEnabled()) {
                debug.message("AMServiceUtils.serviceHasSubSchema() "
                        + "SchemaTypes types for " + serviceName + " are: "
                        + types);
            }
            schemaTypeFlg = types.contains(schemaType);
        } catch (ServiceNotFoundException ex) {
            if (debug.warningEnabled()) {
                debug.warning("AMServiceUtils.serviceHasSubSchema() "
                        + "Service does not exist : " + serviceName);
            }
        }
        return (schemaTypeFlg);
    }

    /**
     * Get service default config from SMS
     * 
     * @param token
     *            SSOToken a valid SSOToken
     * @param serviceName
     *            the service name
     * @param type
     *            service schema type (Dynamic, Policy etc)
     * @return returns a Map of Default Configuration values for the specified
     *         service.
     */
    public static Map getServiceConfig(SSOToken token, String serviceName,
            SchemaType type) throws SMSException, SSOException {
        Map attrMap = null; // Map of attribute/value pairs
        if (type != SchemaType.POLICY) {
            ServiceSchemaManager scm = new ServiceSchemaManager(serviceName,
                    token);
            ServiceSchema gsc = scm.getSchema(type);
            attrMap = gsc.getAttributeDefaults();
        }
        return attrMap;
    }

    /**
     * Create Service Template for a AMro profile, could be used to set policy
     * to a profile
     * 
     * @param token
     *            SSOToken
     * @param orgDN
     *            DN of the org or org unit
     * @param serviceName
     *            Service Name
     * @param avPair
     *            attributes to be set
     * @return String DN of the newly created template
     */
    protected static ServiceConfig createOrgConfig(SSOToken token,
            String orgDN, String serviceName, Map avPair) throws SSOException,
            AMException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                    token);
            ServiceConfig sc = scm.createOrganizationConfig(orgDN, avPair);
            return sc;
        } catch (ServiceNotFoundException ex) {
            Object args[] = { serviceName };
            String locale = AMCommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("481", args, locale),
                    "481", args);
        } catch (ServiceAlreadyExistsException ex) {
            Object args[] = { serviceName };
            String locale = AMCommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("479", args, locale),
                    "479", args);
        } catch (SMSException ex) {
            Object args[] = { serviceName };
            String locale = AMCommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("482", args, locale),
                    "482", args);
        }
    }

    /**
     * Get organization config for the service
     * 
     * @param token
     *            SSOToken
     * @param orgDN
     *            DN of the org or org unit
     * @param serviceName
     *            Service Name
     * @return ServiceConfig of the organization for the service
     */
    public static ServiceConfig getOrgConfig(SSOToken token, String orgDN,
            String serviceName) throws SSOException, AMException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                    token);
            ServiceConfig sc = scm.getOrganizationConfig(orgDN, null);
            DN theOrgDN = new DN(orgDN);
            if (theOrgDN.equals(new DN(SMSEntry.getAMSdkBaseDN()))
                    && sc != null) {
                Map avPair = sc.getAttributes();
                Set subConfigs = sc.getSubConfigNames();
                if (avPair.isEmpty()
                        && (subConfigs == null || subConfigs.isEmpty())) {
                    return null;
                }
            }
            return sc;
        } catch (ServiceNotFoundException ex) {
            Object args[] = { serviceName };
            String locale = AMCommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("481", args, locale),
                    "481", args);
        } catch (ServiceAlreadyExistsException ex) {
            Object args[] = { serviceName };
            String locale = AMCommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("479", args, locale),
                    "479", args);
        } catch (SMSException ex) {
            Object args[] = { serviceName };
            String locale = AMCommonUtils.getUserLocale(token);
            throw new AMException(AMSDKBundle.getString("482", args, locale),
                    "482", args);
        }
    }

    /**
     * Gets object classes for the services.
     * 
     * @param token
     *            SSOToken
     * @param serviceNames
     *            Set of service names
     * @return Set of object classes
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    static Set getServiceObjectClasses(SSOToken token, Set serviceNames)
            throws SSOException, AMException {
        Set result = new HashSet();
        try {
            Iterator it = serviceNames.iterator();
            while (it.hasNext()) {
                String serviceName = (String) it.next();
                if (serviceHasSubSchema(token, serviceName, SchemaType.GLOBAL)) 
                {
                    Map attrs = getServiceConfig(token, serviceName,
                            SchemaType.GLOBAL);
                    Set vals = (Set) attrs.get("serviceObjectClasses");
                    if (vals != null) {
                        result.addAll(vals);
                    }
                }
            }
        } catch (SMSException smsex) {
            debug.error("AMServiceUtils.getServiceObjectClasses() Unable to "
                    + "get them: ", smsex);
            throw new AMException(token, "161");
        }
        return result;
    }
}
