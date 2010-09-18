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
 * $Id: AdminInterfaceUtils.java,v 1.5 2008/08/19 19:09:01 veiming Exp $
 *
 */

package com.sun.identity.common.admin;

import java.security.AccessController;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * <code>AdminInterfaceUtils</code> provides a set common utility methods to
 * administration console and command line tool.
 */
public class AdminInterfaceUtils implements ServiceListener {
    /**
     * Returns the <code>AMObject</code> of a given DN and a store connection.
     * 
     * @param debug
     *            instance
     * @param dn
     *            of the object as a String
     * @param storeConn
     *            store connection
     * @return <code>AMObject</code>
     * @throws AMException
     *             if <code>AMSDK</code> is unable to get
     *             <code>AMObject</code> for <code>dn</code>
     * @throws SSOException
     *             if session expires.
     */
    public static AMObject getAMObject(Debug debug, String dn,
            AMStoreConnection storeConn) throws AMException, SSOException {
        AMObject obj = null;
        int objectType = storeConn.getAMObjectType(dn);

        switch (objectType) {
        case AMObject.ORGANIZATION:
            obj = storeConn.getOrganization(dn);
            break;
        case AMObject.ORGANIZATIONAL_UNIT:
            obj = storeConn.getOrganizationalUnit(dn);
            break;
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            obj = storeConn.getStaticGroup(dn);
            break;
        case AMObject.DYNAMIC_GROUP:
            obj = storeConn.getDynamicGroup(dn);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            obj = storeConn.getAssignableDynamicGroup(dn);
            break;
        case AMObject.ROLE:
            obj = storeConn.getRole(dn);
            break;
        case AMObject.FILTERED_ROLE:
            obj = storeConn.getFilteredRole(dn);
            break;
        case AMObject.USER:
            obj = storeConn.getUser(dn);
            break;
        case AMObject.PEOPLE_CONTAINER:
            obj = storeConn.getPeopleContainer(dn);
            break;
        case AMObject.GROUP_CONTAINER:
            obj = storeConn.getGroupContainer(dn);
            break;
        default:
            if (debug.warningEnabled()) {
                debug.warning("AdminInterfaceUtils.getAMObject: "
                        + "Cannot create AMObject for:" + dn);
            }
        }

        return obj;
    }

    /**
     * Returns true if an object is a descendent of an organizational unit.
     * 
     * @param debug
     *            instance
     * @param storeConn
     *            Store Connection object.
     * @param obj
     *            <code>AMObject</code> to be inspected.
     * @return true if <code>obj</code> is a descendent of an organizational
     *         unit.
     */
    public static boolean inOrganizationalUnit(Debug debug,
            AMStoreConnection storeConn, AMObject obj) {
        boolean inOrgUnit = false;
        String parentDN = obj.getParentDN();

        try {
            while ((parentDN != null) && !inOrgUnit) {
                if (storeConn.getAMObjectType(parentDN) == 
                    AMObject.ORGANIZATIONAL_UNIT) {
                    inOrgUnit = true;
                } else {
                    AMObject parent = getAMObject(debug, parentDN, storeConn);
                    parentDN = (parent != null) ? parent.getParentDN() : null;
                }
            }
        } catch (SSOException ssoe) {
            debug.warning("AdminInterfaceUtils.inOrganizationalUnit", ssoe);
        } catch (AMException ame) {
            debug.warning("AdminInterfaceUtils.inOrganizationalUnit", ame);
        }

        return inOrgUnit;
    }

    /**
     * Returns the naming attribute used the specified object. If this naming
     * attribute cannot be determined for the object, empty string will be
     * returned.
     * 
     * @param object
     *            type of object.
     * @param debug
     *            instance for writing warning message.
     * @return naming attribute for the object.
     */
    public static String getNamingAttribute(int object, Debug debug) {
        String namingAttribute = "";

        try {
            namingAttribute = AMStoreConnection.getNamingAttribute(object);
        } catch (AMException ame) {
            debug.warning("couldn't get naming attribute");
        }

        return namingAttribute;
    }

    /**
     * Returns the default people container name.
     * 
     * @return default people container name
     */
    public static String defaultPeopleContainerName() {
        initialize();
        return (defaultPC);
    }

    /**
     * Returns the default group container name.
     * 
     * @return default group container name
     */
    public static String defaultGroupContainerName() {
        initialize();
        return (defaultGC);
    }

    /**
     * Returns the default agent container name.
     * 
     * @return default agent container name
     */
    public static String defaultAgentContainerName() {
        initialize();
        return (defaultAC);
    }

    /**
     * Returns the default people container name created when an organization is
     * created in OpenSSO. This may not be the one which the user
     * sees as his/her default container through OpenSSO console.
     * 
     * @return Returns the default people container name created when an
     *         organization is created in OpenSSO. This may not be
     *         the one which the user sees as his/her default container through
     *         OpenSSO console.
     */
    public static String defaultPCCreateDuringOrgConfig() {
        initialize();
        return (defaultPCCreateDuringOrgConfig);
    }

    /**
     * Returns the default group container name created when an organization is
     * created in OpenSSO. This may not be the one which the user
     * sees as his/her default container through OpenSSO console.
     * 
     * @return Returns the default group container name created when an
     *         organization is created in OpenSSO. This may not be
     *         the one which the user sees as his/her default container through
     *         OpenSSO console.
     */
    public static String defaultGCCreateDuringOrgConfig() {
        initialize();
        return (defaultGCCreateDuringOrgConfig);
    }

    /**
     * Returns the default org admin role name created when an organization is
     * created in OpenSSO.
     * 
     * @return Returns the default org admin role name created when an
     *         organization is created in OpenSSO.
     */
    public static String defaultOrgAdminRoleCreateDuringOrgConfig() {
        initialize();
        return (defaultORGADMIN);
    }

    /**
     * Returns the default help desk admin role name created when an
     * organization is created in OpenSSO.
     * 
     * @return Returns the default help desk admin role name created when an
     *         organization is created in OpenSSO.
     */
    public static String defaultHelpDeskAdminRoleCreateDuringOrgConfig() {
        initialize();
        return (defaultHELP_DESK_ADMIN);
    }

    /**
     * Returns the default policy admin role name created when an organization
     * is created in OpenSSO.
     * 
     * @return Returns the default policy admin role name created when an
     *         organization is created in OpenSSO.
     */
    public static String defaultPolicyAdminRoleCreateDuringOrgConfig() {
        initialize();
        return (defaultPOLICY_ADMIN);
    }

    /**
     * Initializes the default containers using SMS
     */
    private static void initialize() {
        if (!initialized) {
            try {
                // Generate a SSOToken to initialize ServiceSchemaManager
                String adminDN = (String) AccessController
                        .doPrivileged(new AdminDNAction());
                String adminPassword = (String) AccessController
                        .doPrivileged(new AdminPasswordAction());
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                ServiceConfigManager scm = new ServiceConfigManager(
                        SERVICE_NAME, mgr.createSSOToken(new AuthPrincipal(
                                adminDN), adminPassword));
                if (!addedListener) {
                    addedListener = true;
                    scm.addListener(new AdminInterfaceUtils());
                }
                ServiceConfig globalConfig = scm.getGlobalConfig(null);

                ServiceConfig templatesConfig = globalConfig
                        .getSubConfig(TEMPLATES);

                ServiceConfig structTemplateSubConfig = templatesConfig
                        .getSubConfig(STRUCTURE_TEMPLATES);

                ServiceConfig groupContConfig = structTemplateSubConfig
                        .getSubConfig(GCCREATE_ATTR_NAME);

                ServiceConfig peopleContConfig = structTemplateSubConfig
                        .getSubConfig(PCCREATE_ATTR_NAME);

                ServiceConfig orgAdminConfig = structTemplateSubConfig
                        .getSubConfig(ORG_ADMIN_ATTR_NAME);

                ServiceConfig helpDeskAdminConfig = structTemplateSubConfig
                        .getSubConfig(HELP_DESK_ADMIN_ATTR_NAME);

                ServiceConfig policyAdminConfig = structTemplateSubConfig
                        .getSubConfig(POLICY_ADMIN_ATTR_NAME);

                defaultGCCreateDuringOrgConfig = getConfigAttributeValue(
                        groupContConfig, defaultGCCreateDuringOrgConfig);

                defaultPCCreateDuringOrgConfig = getConfigAttributeValue(
                        peopleContConfig, defaultPCCreateDuringOrgConfig);

                defaultORGADMIN = getConfigAttributeValue(orgAdminConfig,
                        defaultORGADMIN);

                defaultHELP_DESK_ADMIN = getConfigAttributeValue(
                        helpDeskAdminConfig, defaultHELP_DESK_ADMIN);

                defaultPOLICY_ADMIN = getConfigAttributeValue(
                        policyAdminConfig, defaultPOLICY_ADMIN);

                ServiceSchemaManager sm = new ServiceSchemaManager(
                        CONSOLE_SERVICE_NAME, mgr.createSSOToken(
                                new AuthPrincipal(adminDN), adminPassword));
                if (!addedListener) {
                    addedListener = true;
                    sm.addListener(new AdminInterfaceUtils());
                }
                ServiceSchema schema = sm.getGlobalSchema();
                defaultAC = getAttributeValue(schema, AC_ATTR_NAME, defaultAC);
                defaultGC = getAttributeValue(schema, GC_ATTR_NAME, defaultGC);
                defaultPC = getAttributeValue(schema, PC_ATTR_NAME, defaultPC);
            } catch (Exception e) {
                // Use the default values, and write out debug warning msg
                debug.warning("AdminInterfaceUtils: Unable to get "
                        + "default People, Groups, Org Admin Role, "
                        + "Help Desk Admin Role, Policy Admin Role and "
                        + "Agents containers from SM", e);
            }

            if (debug.messageEnabled()) {
                debug.message("AdminInterfaceUtils: Defaults container: "
                        + defaultPC + ", " + defaultGC + ", " + defaultAC
                        + ", " + defaultPCCreateDuringOrgConfig + ", "
                        + defaultGCCreateDuringOrgConfig + ", "
                        + defaultORGADMIN + ", " + defaultHELP_DESK_ADMIN
                        + ", " + defaultPOLICY_ADMIN);
            }
            initialized = true;
        }
    }

    /**
     * Returns attribute value for the given container
     */
    private static String getAttributeValue(ServiceSchema ss, String an,
            String dv) throws SSOException, SMSException {
        String answer = dv;
        AttributeSchema as = ss.getAttributeSchema(an);

        if (as != null) {
            Set values = as.getDefaultValues();

            if (values != null && !values.isEmpty()) {
                answer = (String) values.iterator().next();
            }
        }
        return (answer);
    }

    /**
     * Returns attribute value from service config for the given container
     */
    private static String getConfigAttributeValue(ServiceConfig sc, String dv)
            throws SSOException, SMSException {
        String answer = dv;
        Map configMap = sc.getAttributes();

        if (configMap != null) {
            Set values = (Set) configMap.get(ATTR_NAME);
            if (values == null || values.isEmpty()) {
                return null;
            } else {
                answer = (String) values.iterator().next();
                int indx = answer.indexOf("=");
                if (indx >= 0) {
                    answer = answer.substring(indx + 1);
                    if (debug.messageEnabled()) {
                        debug.message("AdminInterfaceUtils: "
                                + "getConfigAttributeValue: " + answer);
                    }
                }
            }
        }
        return (answer);
    }

    // SMS Service Listener interfaces
    public void schemaChanged(String serviceName, String version) {
        if (serviceName.toLowerCase().equals(SERVICE_NAME)) {
            initialized = false;
        }
    }

    public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
        if (serviceName.toLowerCase().equals(SERVICE_NAME)) {
            initialized = false;
        }
    }

    public void organizationConfigChanged(String serviceName, String version,
            String orgName, String groupName, String serviceComponent, int type)
    {
        if (serviceName.toLowerCase().equals(SERVICE_NAME)) {
            initialized = false;
        }
    }

    // Private static variables for determining default containers
    private static boolean initialized, addedListener;

    private static Debug debug = Debug.getInstance("amConsole");

    private static String defaultPC = "People";

    private static String defaultGC = "Groups";

    private static String defaultAC = "Agents";

    private static String defaultPCCreateDuringOrgConfig = "People";

    private static String defaultGCCreateDuringOrgConfig = "Groups";

    private static String defaultORGADMIN = "Organization Admin Role";

    private static String defaultHELP_DESK_ADMIN = 
        "Organization Help Desk Admin Role";

    private static String defaultPOLICY_ADMIN = 
        "Organization Policy Admin Role";

    private static final String CONSOLE_SERVICE_NAME = 
        "iplanetamadminconsoleservice";

    private static final String SERVICE_NAME = "dai";

    private static final String PC_ATTR_NAME = 
        "iplanet-am-admin-console-default-pc";

    private static final String GC_ATTR_NAME = 
        "iplanet-am-admin-console-default-gc";

    private static final String AC_ATTR_NAME = 
        "iplanet-am-admin-console-default-ac";

    private static final String TEMPLATES = "templates";

    private static final String STRUCTURE_TEMPLATES = "StructureTemplates";

    private static final String ATTR_NAME = "name";

    private static final String PCCREATE_ATTR_NAME = "PeopleContainer";

    private static final String GCCREATE_ATTR_NAME = "GroupContainer";

    private static final String ORG_ADMIN_ATTR_NAME = "DPOrgAdminRole";

    private static final String HELP_DESK_ADMIN_ATTR_NAME = 
        "DPOrgHelpDeskAdminRole";

    private static final String POLICY_ADMIN_ATTR_NAME = "DPOrgPolicyAdminRole";
}
