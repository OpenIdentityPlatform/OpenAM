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
 * $Id: SCModelBase.java,v 1.3 2008/07/10 23:27:24 veiming Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SchemaType;
import com.iplanet.sso.SSOException;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/* - LOG COMPLETE - */

/**
 * This class contains the common service management related calls used
 * for global service configuration. Views that need service processing can
 * implement this class and get many needed routines for free. The extending
 * class needs to implement <code>getServiceName()</code> and return the 
 * appropriate service name.
 */ 
public  abstract class SCModelBase
    extends AMModelBase
    implements SCModel {

    ServiceSchemaManager manager = null;

    /**
     * Creates a service data model implementation object
     *
     * @param req The <code>HttpServletRequest</code> object.
     * @param map of user information.
     */
    public SCModelBase(HttpServletRequest req, Map map) {
        super(req,  map);
        initializeSchemaManager();
    }

    /*
    * Establishes the ServiceSchemaManager connection for future operations.
    */
    private void initializeSchemaManager() {
        String error = null;
        try {
            manager =  new ServiceSchemaManager(
                getServiceName(), getUserSSOToken());
        } catch (SSOException e) {
            error = e.getMessage();
        } catch (SMSException e) {
            error = e.getMessage();
        }
        if (error != null && debug.warningEnabled()) {
            debug.warning("couldn't initialize schema manager for " + 
                getServiceName());
            debug.warning("reason: " + error);
        }
    }

    /**
     * Gets the values for all the attributes defined for the 
     * specified type. 
     */
    protected Map getAttributeValues(SchemaType type)
        throws AMConsoleException 
    {
        String serviceName = getServiceName();
        String[] params = {serviceName, type.getType(), "*"};
        logEvent("ATTEMPT_GET_ATTR_VALUE_SCHEMA_TYPE", params);

        try {
            Map values = null;
            ServiceSchema schema = getServiceSchemaManager().getSchema(type);

            if (schema != null) {
                Set attributes = schema.getAttributeSchemaNames();
                values = getAttributeValues(schema, attributes);
                logEvent("SUCCEED_GET_ATTR_VALUE_SCHEMA_TYPE", params);
            } else {
                logEvent("NO_SCHEMA_GET_ATTR_VALUE_SCHEMA_TYPE", params);
            }

            return (values == null) ? Collections.EMPTY_MAP : values;
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, type.getType(), "*", strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_SCHEMA_TYPE", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, type.getType(), "*", strError};
            logEvent("SSO_EXCEPTION_GET_ATTR_VALUE_SCHEMA_TYPE", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Gets the values for the specified set of attributes defined for the 
     * specified type. 
     */
    protected Map getAttributeValues(SchemaType type, Set attributes)
        throws AMConsoleException 
    {
        String serviceName = getServiceName();
        String attributeNames = AMAdminUtils.getString(attributes, ",", false);
        String[] params = {serviceName, type.getType(), attributeNames};
        logEvent("ATTEMPT_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE", params);

        try {
            ServiceSchema schema = getServiceSchemaManager().getSchema(type);
            Map values = getAttributeValues(schema, attributes);
            logEvent("SUCCEED_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE", params);
            return (values == null) ? Collections.EMPTY_MAP : values;
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, type.getType(), attributeNames,
                strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, type.getType(), attributeNames,
                strError};
            logEvent("SSO_EXCEPTION_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private Map getAttributeValues(ServiceSchema schema, Set attributes) {
        Map values = new HashMap(attributes.size() * 2);
        for (Iterator x = attributes.iterator(); x.hasNext();) {
            String attributeName = (String)x.next();
            AttributeSchema as = 
                (AttributeSchema)schema.getAttributeSchema(attributeName);

            if (as != null) {
                AttributeSchema.Type type = as.getType();
                if ((type == AttributeSchema.Type.MULTIPLE_CHOICE) ||
                    (type == AttributeSchema.Type.SINGLE_CHOICE)) 
                {
                    Map m = new HashMap(4);
                    m.put(AMAdminConstants.CHOICES, 
                        AMAdminUtils.toSet(as.getChoiceValues()));
                    m.put(AMAdminConstants.VALUES, as.getDefaultValues());
                    values.put(as.getName(), m);
                } else {
                    values.put(as.getName(), as.getDefaultValues());
                }
            }
        }                                             
        return (values == null) ? Collections.EMPTY_MAP : values;
    }

    /** 
     * Sets the attribute values for the specified schema type.
     */
    protected void setAttributeValues(SchemaType type, Map values) 
        throws AMConsoleException
    {
        String serviceName = getServiceName();
        String attributeNames = AMAdminUtils.getString(
            values.keySet(), ",", false);
        String[] params = {serviceName, type.getType(), attributeNames};
        logEvent("ATTEMPT_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE", params);

        try {
            ServiceSchema schema = getServiceSchemaManager().getSchema(type);
            schema.setAttributeDefaults(values);
            logEvent("SUCCEED_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE", params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, type.getType(), attributeNames,
                strError};
            logEvent("SSO_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, type.getType(), attributeNames,
                strError};
            logEvent("SMS_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        }     
    }

    /**
     * Returns a set of attribute names that are defined for the given
     * <i>SchemaType</i>. All attribute names are returned. There is no
     * schema, display, or i18nkey checks performed here.
     */
    protected Set getAttributeNames(SchemaType type)
        throws AMConsoleException 
    {
        try {
            ServiceSchema schema =  getServiceSchemaManager().getSchema(type);
            Set attributes = schema.getAttributeSchemaNames(); 
            return (attributes == null) ? Collections.EMPTY_SET : attributes;
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    private ServiceSchemaManager getServiceSchemaManager()
        throws SSOException, SMSException {
        if (manager == null) {
            manager =  new ServiceSchemaManager(
                getServiceName(), getUserSSOToken());
        }
        return manager;
    }

    /**
    * Returns the values for the GLOBAL, ORGANIZATION, and DYNAMIC
    * attributes in the service.
    *
    * @return Map property values.
    */
    public Map getValues() throws AMConsoleException {
        Map tmp = new HashMap();
        tmp.putAll(getAttributeValues(SchemaType.GLOBAL));
        tmp.putAll(getAttributeValues(SchemaType.ORGANIZATION));
        tmp.putAll(getAttributeValues(SchemaType.DYNAMIC));
        return tmp;
    }

    /**
    * Sets the values for the attributes that are configurable for the 
    * service.  The policy service contains only global and organization
    * attributes.  To store the attributes, we first remove the global 
    * attributes and put them into another Map. This map is then  stored
    * in the service config. The values remaining in the original map are
    * the organization attributes, which can then be stored in the service
    * config.
    */
    public void setValues(Map modifiedValues) throws AMConsoleException {
        if (modifiedValues == null || modifiedValues.isEmpty()) {
            debug.message("the modifiedValues map is empty; no changes to set");
            return;
        }

        Map newValues  = removeAttributes(SchemaType.GLOBAL, modifiedValues);
        if (newValues != null && !newValues.isEmpty()) {
            setAttributeValues(SchemaType.GLOBAL, newValues);
        }

        /*
         * now remove the organization attributes from the modifiedValues and
         * put in a new Map, so it can be stored in the service config
         */
        newValues = removeAttributes(SchemaType.ORGANIZATION, modifiedValues);
        if (newValues != null && !newValues.isEmpty()) {
            setAttributeValues(SchemaType.ORGANIZATION, newValues);
        }

        /*
        * modifiedValues will now contain only the dynamic attributes.
        * we can store that in the service config now.
        */
        if (!modifiedValues.isEmpty()) {
            setAttributeValues(SchemaType.DYNAMIC, modifiedValues);
        }
    }
    
    /*
    * Removes a set of attribute values from a Map and creates a new map
    * with those values. Attributes to remove are defined by the schema type
    * passed as a parameter.
    */
    private Map removeAttributes(SchemaType type, Map original) { 
        Set names = null;
        try {
            names = getAttributeNames(type);
        } catch (AMConsoleException a) {
            if (debug.warningEnabled()) {
                debug.warning("SCModelBase.removeAttributes()" +
                    "\ncould not get the "+type+" attribute names");
            }
        }
        Map values  = new HashMap(original.size() * 2);
        
        // remove the attributes of type "type" from the original map of values
        for (Iterator i=names.iterator(); i.hasNext();) {
            String str = (String)i.next();
            Set  value = (Set)original.remove(str);
            if (value != null) {
                values.put(str, value);
            }
        }

        return values;
    }

    
    /*
    * ABSTRACT METHODS
    */
    public abstract String getServiceName();
}
