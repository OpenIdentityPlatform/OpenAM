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
 * $Id: RMRealmModelImpl.java,v 1.4 2008/10/02 16:31:29 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.realm.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.MultiServicesPropertyXMLBuilder;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class RMRealmModelImpl
    extends AMModelBase
    implements RMRealmModel
{
    private static SSOToken adminSSOToken =
        (SSOToken)AccessController.doPrivileged(AdminTokenAction.getInstance());

    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public RMRealmModelImpl(HttpServletRequest req,  Map map) {
        super(req, map);
    }

    /**
     * Returns sub realm creation property XML.
     *
     * @throws AMConsoleException if there are no attributes to display.
     * @return sub realm creation property XML.
     */
    public String getCreateRealmPropertyXML()
        throws AMConsoleException {
        StringBuffer buff = new StringBuffer(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(PROPERTY_SECTION_CREATION_GENERAL);
        getPropertyXML(buff, false);
        buff.append(PropertyTemplate.END_TAG);
        return buff.toString();
    }

    /**
     * Returns realm profile property XML.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class name of View Bean
     * @throws AMConsoleException if there are no attributes to display.
     * @return realm profile property XML.
     */
    public String getRealmProfilePropertyXML(
        String realmName,
        String viewbeanClassName
    ) throws AMConsoleException {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission(realmName, null,
            AMAdminConstants.PERMISSION_MODIFY, this, viewbeanClassName);
        StringBuffer buff = new StringBuffer(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG);
        getPropertyXML(buff, !canModify);
        buff.append(PropertyTemplate.END_TAG);
        return buff.toString();
    }

    private void getPropertyXML(StringBuffer buff, boolean readonly)
        throws AMConsoleException
    {
        try {
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                adminSSOToken, "/");
            Set serviceSchemas =  orgMgr.getServiceSchemas();

            for (Iterator iter = serviceSchemas.iterator(); iter.hasNext(); ) {
                MultiServicesPropertyXMLBuilder xmlBuilder =
                    new MultiServicesPropertyXMLBuilder(
                        (ServiceSchema)iter.next(), this);
                xmlBuilder.setAllAttributeReadOnly(readonly);
                buff.append(xmlBuilder.getXML(false));
            }
        } catch (SSOException e) {
            debug.error("RMRealmModelImpl.getPropertyXML", e);
        } catch (SMSException e) {
            debug.error("RMRealmModelImpl.getPropertyXML", e);
        }
    }

    /**
     * Creates sub realm.
     *
     * @param parentRealm Parent realm name.
     * @param name Name of new sub realm.
     * @param attrValues Map of attribute name to a set of attribute values.
     * @throws AMConsoleException if sub realm cannot be created.
     */
    public void createSubRealm(
        String parentRealm,
        String name,
        Map attrValues
    ) throws AMConsoleException {
        if ((parentRealm == null) || (parentRealm.length() == 0)) {
            parentRealm = "/";
        }

        String[] params = {parentRealm, name};
        logEvent("ATTEMPT_CREATE_REALM", params);

        try {
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                getUserSSOToken(), parentRealm);
            Map map = mapAttributeValuesToServiceName(attrValues);
            orgMgr.createSubOrganization(name, map);
            logEvent("SUCCEED_CREATE_REALM", params);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {parentRealm, name, strError};
            logEvent("SMS_EXCEPTION_CREATE_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Deletes sub realms
     *
     * @param parentRealm Parent realm name.
     * @param names List of realm names to be deleted.
     * @throws AMConsoleException if sub realms cannot be deleted.
     */
    public void deleteSubRealms(String parentRealm, Collection names)
        throws AMConsoleException {
        String[] params = new String[2];
        params[0] = parentRealm;
        String currentName = "";

        try {
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                getUserSSOToken(), parentRealm);
            List orderedByLength = AMAdminUtils.orderByStringLength(names);

            for (Iterator iter = orderedByLength.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                currentName = name;
                params[1] = name;
                logEvent("ATTEMPT_DELETE_REALM", params);

                orgMgr.deleteSubOrganization(name, true);

                logEvent("SUCCEED_DELETE_REALM", params);
            }
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {parentRealm, currentName, strError};
            logEvent("SMS_EXCEPTION_DELETE_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
    * Returns Map of default attribute values used when creating
    * new realms. This only returns default values for single choice
    * type attributes. Returning other default values runs the risk
    * of violating the attribute uniqueness plugin while creating a 
    * new realm.
    *
    * @throws AMConsoleException if map cannot be obtained.
    */
    public Map getDefaultValues() {
        Map map = new HashMap();
    
        try {
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                getUserSSOToken(), "/");
            Set serviceSchemas = orgMgr.getServiceSchemas();

            for (Iterator iter = serviceSchemas.iterator(); iter.hasNext(); ) {
                ServiceSchema ss = (ServiceSchema)iter.next();
                String serviceName = ss.getServiceName();
                Set attrSchemas = ss.getAttributeSchemas();

                for (Iterator i = attrSchemas.iterator(); i.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)i.next();
                    if (as.getType() == AttributeSchema.Type.SINGLE_CHOICE) {
                        map.put(serviceName + "_" + as.getName(),
                            as.getDefaultValues());
                    }
                }
            }
        } catch (SMSException e) {
            debug.error("RMRealmModelImpl.getDefaultValues", e);
        }
    
        return map;
    }

    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @throws AMConsoleException if map cannot be obtained.
     */
    public Map getDataMap() {
        Map map = new HashMap();

        try {
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                getUserSSOToken(), "/");
            Set serviceSchemas = orgMgr.getServiceSchemas();

            for (Iterator iter = serviceSchemas.iterator(); iter.hasNext(); ) {
                ServiceSchema ss = (ServiceSchema)iter.next();
                String serviceName = ss.getServiceName();
                Set attrSchemas = ss.getAttributeSchemas();

                for (Iterator i = attrSchemas.iterator(); i.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)i.next();
                    map.put(serviceName + "_" + as.getName(),
                        Collections.EMPTY_SET);
                }
            }
        } catch (SMSException e) {
            debug.error("RMRealmModelImpl.getDataMap", e);
        }

        return map;
    }

    /**
     * Returns attribute values. Map of attribute name to set of values.
     *
     * @param name Name of Realm.
     * @return attribute values.
     * @throws AMConsoleException if attribute values cannot be obtained.
     */
    public Map getAttributeValues(String name)
        throws AMConsoleException 
    {
        String[] param = {name};
        logEvent("ATTEMPT_GET_ATTR_VALUES_OF_REALM", param);

        try {
            Map map = new HashMap();
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                getUserSSOToken(), name);
            Set serviceSchemas = orgMgr.getServiceSchemas();

            for (Iterator iter = serviceSchemas.iterator(); iter.hasNext();) {
                ServiceSchema ss = (ServiceSchema)iter.next();
                String serviceName = ss.getServiceName();
                Map values = orgMgr.getAttributes(serviceName);
                Set attributeSchemas = ss.getAttributeSchemas();

                for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
                    AttributeSchema as = (AttributeSchema)i.next();
                    String i18nKey = as.getI18NKey();

                    if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                        String attrName = as.getName();
                        Set val = (Set)values.get(attrName);
                        if (val == null) {
                            val = Collections.EMPTY_SET;
                        }
                        map.put(serviceName + "_" + attrName, val);
                    }
                }
            }
            logEvent("SUCCEED_GET_ATTR_VALUES_OF_REALM", param);

            return map;
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {name, strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUES_OF_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }
    
    /**
     * Set attribute values.
     *
     * @param name Name of Realm.
     * @param attributeValues Map of attribute name to set of values.
     * @throws AMConsoleException if attribute values cannot be updated.
     */
    public void setAttributeValues(String name, Map attributeValues)
        throws AMConsoleException {
        try {
            String[] param = {name};
            logEvent("ATTEMPT_SET_ATTR_VALUES_OF_REALM", param);
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                getUserSSOToken(), name);
            Map map = mapAttributeValuesToServiceName(attributeValues);

            for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
                String serviceName = (String)iter.next();
                orgMgr.setAttributes(serviceName, (Map)map.get(serviceName));
            }
            logEvent("SUCCEED_SET_ATTR_VALUES_OF_REALM", param);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {name, strError};
            logEvent("SMS_EXCEPTION_SET_ATTR_VALUES_OF_REALM", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private Map mapAttributeValuesToServiceName(Map attributeValues) {
        Map mapServices = null;

        if ((attributeValues != null) && !attributeValues.isEmpty()) {
            mapServices = new HashMap();

            for (Iterator i = attributeValues.keySet().iterator();
                i.hasNext();
            ) {
                String attrName = (String)i.next();
                int idx = attrName.indexOf('_');
                if (idx != -1) {
                    String serviceName = attrName.substring(0, idx);
                    String attributeName = attrName.substring(idx+1);

                    Map map = (Map)mapServices.get(serviceName);
                    if (map == null) {
                        map = new HashMap();
                        mapServices.put(serviceName, map);
                    }
                    map.put(attributeName, attributeValues.get(attrName));
                } else {
                    debug.error(
                        "RMRealmModelImpl.mapAttributeValuesToServiceName: " +
                        "unknown attribute, " + attrName);
                }
            }
        }

        return (mapServices == null) ? Collections.EMPTY_MAP : mapServices;
    }

    private static final String PROPERTY_SECTION_CREATION_GENERAL =
        "<section name=\"general\" defaultValue=\"realm.sectionHeader.general\"><property required=\"true\"><label name=\"lblName\" defaultValue=\"authDomain.attribute.label.name\" labelFor=\"tfName\" /><cc name=\"tfName\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" /></property><property required=\"true\"><label name=\"lblParent\" defaultValue=\"realm.parent.label\" labelFor=\"tfParent\" /><cc name=\"tfParent\" tagclass=\"com.sun.web.ui.taglib.html.CCSelectableListTag\" ><attribute name=\"size\" value=\"10\" /></cc></property></section>";

}
