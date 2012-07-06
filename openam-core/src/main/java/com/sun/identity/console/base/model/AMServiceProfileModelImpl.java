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
 * $Id: AMServiceProfileModelImpl.java,v 1.3 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class AMServiceProfileModelImpl
    extends AMModelBase
    implements AMServiceProfileModel
{
    protected PropertyXMLBuilder xmlBuilder;
    private static Set DISPLAY_SCHEMA_TYPES = new HashSet();

    static {
        DISPLAY_SCHEMA_TYPES.add(SchemaType.GLOBAL);
        DISPLAY_SCHEMA_TYPES.add(SchemaType.ORGANIZATION);
        DISPLAY_SCHEMA_TYPES.add(SchemaType.DYNAMIC);
    }

    protected String serviceName;

    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param map of user information
     */
    public AMServiceProfileModelImpl(
        HttpServletRequest req,
        String serviceName,
        Map map
    ) throws AMConsoleException {
        super(req, map);
        this.serviceName = serviceName;

        try {
            xmlBuilder = new PropertyXMLBuilder(
                serviceName, getDisplaySchemaTypes(), this);
            if (serviceName.equals(ADMIN_CONSOLE_SERVICE)  &&
                ServiceManager.isRealmEnabled()) 
            {
                AMViewConfig config = AMViewConfig.getInstance();
                xmlBuilder.discardAttribute(
                    config.getRealmEnableHiddenConsoleAttrNames());
            }
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    public Set getDisplaySchemaTypes() {
        return DISPLAY_SCHEMA_TYPES;
    }

    /**
     * Returns page title.
     *
     * @return page title.
     */
    public String getPageTitle() {
        return getLocalizedServiceName(serviceName);
    }

    protected ServiceSchemaManager getServiceSchemaManager() {
        return xmlBuilder.getServiceSchemaManager();
    }

    /**
     * Returns the XML for property sheet view component.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class Name of View Bean.
     * @param serviceName Name of Service.
     * @return the XML for property sheet view component.
     * @throws AMConsoleException if XML cannot be created.
     */
    public String getPropertySheetXML(
        String realmName,
        String viewbeanClassName,
        String serviceName
    ) throws AMConsoleException {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission(realmName, serviceName,
            AMAdminConstants.PERMISSION_MODIFY, this, viewbeanClassName);
        if (!canModify) {
            xmlBuilder.setAllAttributeReadOnly(true);
        }

        try {
            /*
             * the location needs to be set in order for the page to be 
             * constructed correctly. Some choice value components can have 
             * their values built based on the realm.
             */
            return xmlBuilder.getXML(realmName);
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns attributes values.
     *
     * @return attributes values.
     */
    public Map getAttributeValues() {
        String[] param = {serviceName};
        logEvent("ATTEMPT_READ_ALL_GLOBAL_DEFAULT_ATTRIBUTE_VALUES", param);

        Set attributeSchemas = xmlBuilder.getAttributeSchemas();
        Map values = new HashMap(attributeSchemas.size() *2);
        for (Iterator iter = attributeSchemas.iterator(); iter.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            AttributeSchema.UIType uiType = as.getUIType();

            if ((uiType == null) ||
                (!uiType.equals(AttributeSchema.UIType.NAME_VALUE_LIST) &&
                !uiType.equals(AttributeSchema.UIType.BUTTON) &&
                !uiType.equals(AttributeSchema.UIType.LINK))
            ) {
                AttributeSchema.Type type = as.getType();
                if ((type == AttributeSchema.Type.MULTIPLE_CHOICE) ||
                    (type == AttributeSchema.Type.SINGLE_CHOICE)) 
                {
                    Map tmp = new HashMap(4);
                    tmp.put("choices",AMAdminUtils.toSet(as.getChoiceValues()));
                    tmp.put("values", as.getDefaultValues());
                    values.put(as.getName(),tmp);
                }
                values.put(as.getName(), as.getDefaultValues());
            }
        }

        logEvent("SUCCEED_READ_ALL_GLOBAL_DEFAULT_ATTRIBUTE_VALUES", param);
        return values;
    }

    /**
     * Returns attribute values.
     *
     * @param name Name of attribute.
     * @return attribute values.
     */
    public Set getAttributeValues(String name) {
        boolean found = false;
        Set values = null;
        Set attributeSchemas = xmlBuilder.getAttributeSchemas();

        String[] params = {serviceName, name};
        logEvent("ATTEMPT_READ_GLOBAL_DEFAULT_ATTRIBUTE_VALUES", params);

        for (Iterator i = attributeSchemas.iterator(); i.hasNext() && !found; ){
            AttributeSchema as = (AttributeSchema)i.next();
            if (as.getName().equals(name)) {
                values = as.getDefaultValues();
                found = true;
            }
        }

        if (found) {
            logEvent("SUCCEED_READ_GLOBAL_DEFAULT_ATTRIBUTE_VALUES", params);
        } else {
            logEvent("FAILED_READ_GLOBAL_DEFAULT_ATTRIBUTE_VALUES", params);
        }

        return (values == null) ? Collections.EMPTY_SET : values;
    }

    /**
     * Set attribute values.
     *
     * @param map Map of attribute name to Set of attribute values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void setAttributeValues(Map map)
        throws AMConsoleException
    {
        Set attributeSchemas = new HashSet();
        attributeSchemas.addAll(xmlBuilder.getAttributeSchemas());
        addMoreAttributeSchemasForModification(attributeSchemas);

        // Need to find the service schema for each attributeSchema
        Map mapSvcSchemaToMapNameToValues = new HashMap();
        for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)i.next();
            String name = as.getName();
            Set values = (Set)map.get(name);

            if (values != null) {
                ServiceSchema ss = as.getServiceSchema();
                Map m = (Map)mapSvcSchemaToMapNameToValues.get(ss);

                if (m == null) {
                    m = new HashMap();
                    mapSvcSchemaToMapNameToValues.put(ss, m);
                }

                m.put(name, values);
            }
        }

        if (!mapSvcSchemaToMapNameToValues.isEmpty()) {
            for (Iterator i = mapSvcSchemaToMapNameToValues.keySet().iterator();
                i.hasNext();
            ) {
                ServiceSchema ss = (ServiceSchema)i.next();
                setDefaultValues(
                    ss, (Map)mapSvcSchemaToMapNameToValues.get(ss));
            }
        }
    }

    private void setDefaultValues(ServiceSchema ss, Map map)
        throws AMConsoleException
    {
        String csvNames = AMFormatUtils.toCommaSeparatedFormat(map.keySet());
        String[] params = {serviceName, csvNames};
        logEvent("ATTEMPT_WRITE_GLOBAL_DEFAULT_ATTRIBUTE_VALUES", params);

        try {
            ss.setAttributeDefaults(map);        
            logEvent("SUCCEED_WRITE_GLOBAL_DEFAULT_ATTRIBUTE_VALUES", params);
        } catch (SMSException e) {
            String[] paramsEx = {serviceName, csvNames, getErrorString(e)};
            logEvent("SMS_EXCEPTION_WRITE_GLOBAL_DEFAULT_ATTRIBUTE_VALUES",
                paramsEx);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            String[] paramsEx = {serviceName, csvNames, getErrorString(e)};
            logEvent("SSO_EXCEPTION_WRITE_GLOBAL_DEFAULT_ATTRIBUTE_VALUES",
                paramsEx);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Dervive classes can overwrite this method to add more attribute schema
     * for modifying the discovery service.
     *
     * @param attributeSchemas Set of attribute schemas for modification.
     */
    protected void addMoreAttributeSchemasForModification(Set attributeSchemas){
    }

    /**
     * Returns properties view bean URL for an attribute schema.
     *
     * @param name Name of attribute schema.
     * @return properties view bean URL for an attribute schema.
     */
    public String getPropertiesViewBean(String name) {
        Set attributeSchemas = xmlBuilder.getAttributeSchemas();
        String url = null;
        for (Iterator iter = attributeSchemas.iterator();
            iter.hasNext() && (url == null);
        ) {
            AttributeSchema as = (AttributeSchema)iter.next();
            if (as.getName().equals(name)) {
                url = as.getPropertiesViewBeanURL();
            }
        }
        return url;
    }
}
