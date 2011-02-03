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
 * $Id: SubConfigModelImpl.java,v 1.3 2008/06/25 05:43:19 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.configuration.ISubConfigNames;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.SubConfigMeta;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.SubConfigPropertyXMLBuilder;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */
/* Logging is done in base.model.SubConfigMeta class */

public class SubConfigModelImpl
    extends AMModelBase
    implements SubConfigModel
{
    private SubConfigMeta subConfigMeta;
    private String serviceName;
    private String parentId;
    private SubConfigPropertyXMLBuilder xmlBuilder;

    private static Set SCHEMA_TYPE = new HashSet();
    static {
        SCHEMA_TYPE.add(SchemaType.GLOBAL);
    }

    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param parentId Parent Id.
     * @param map of user information
     */
    public SubConfigModelImpl(
        HttpServletRequest req,
        String serviceName,
        String parentId,
        Map map
    ) throws AMConsoleException {
        super(req, map);
        this.serviceName = serviceName;
        this.parentId = parentId;
        subConfigMeta = new SubConfigMeta(serviceName, this);
        subConfigMeta.setParentId(parentId);
    }

    /** 
     * Returns property sheet XML for adding new sub configuration.
     *
     * @param name Name of Schema.
     * @return property sheet XML for adding new sub configuration.
     */
    public String getAddConfigPropertyXML(String name)
        throws AMConsoleException {
        try {
            xmlBuilder = new SubConfigPropertyXMLBuilder(serviceName, 
                subConfigMeta.getServiceSchema(name), this);
            String xml = xmlBuilder.getXML();
            String attributeNameXML = 
                (getSelectableSubConfigNamesPlugin(name) != null) ?
                AMAdminUtils.getStringFromInputStream(
                    getClass().getClassLoader().getResourceAsStream(
              "com/sun/identity/console/propertySubConfigSelectableName.xml")) :
                AMAdminUtils.getStringFromInputStream(
                    getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertySubConfigName.xml"));
            xml = SubConfigPropertyXMLBuilder.prependXMLProperty(
                xml, attributeNameXML);
            return xml;
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /** 
     * Returns property sheet XML for editing sub configuration.
     *
     * @param viewbeanClassName Class name of view bean.
     * @return property sheet XML for editing sub configuration.
     */
    public String getEditConfigPropertyXML(String viewbeanClassName)
        throws AMConsoleException {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        boolean canModify = dConfig.hasPermission("/", null,
            AMAdminConstants.PERMISSION_MODIFY, getUserSSOToken(),
                viewbeanClassName);

        try {
            xmlBuilder = new SubConfigPropertyXMLBuilder(serviceName, 
                subConfigMeta.getServiceSchema(), this);
            xmlBuilder.setSupportSubConfig(subConfigMeta.hasGlobalSubSchema());
            xmlBuilder.setViewBeanName("SubConfigEdit");
            if (!canModify) {
                xmlBuilder.setAllAttributeReadOnly(true);
            }
            return xmlBuilder.getXML();
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**

    /**
     * Returns a map of sub schema name to its localized name. We should
     * be able to create sub configuration with these names.
     *
     * @return Map of sub schema name to its localized name.
     */
    public Map getCreateableSubSchemaNames() {
        return subConfigMeta.getCreateableSubSchemaNames();
    }

    /**
     * Returns a set of attribute names for a sub schema.
     *
     * @param schemaName Name of Schema.
     * @return Set of attribute names for a sub schema.
     */
    public Set getAttributeNames(String schemaName) {
        Set names = null;
        Set attributeSchemas = xmlBuilder.getAttributeSchemas();
        if ((attributeSchemas != null) && !attributeSchemas.isEmpty()) {
            names = new HashSet(attributeSchemas.size() *2);
            for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)i.next();
                names.add(as.getName());
            }
        }
        return (names != null) ? names : Collections.EMPTY_SET;
    }

    /** 
     * Creates a new sub configuration.
     *
     * @param name Name of sub configuration.
     * @param schemaName Name of schema name.
     * @param values Map of attribute name to its values.
     * @throws AMConsoleException if sub configuration cannot be created.
     */
    public void createSubConfig(String name, String schemaName, Map values)
        throws AMConsoleException {
        if ((name == null) || (name.trim().length() == 0)) {
            throw new AMConsoleException(
                "subconfig.missing.subconfiguration.name.message");
        }

        subConfigMeta.createSubConfig(name, schemaName, values);
    }

    /**
     * Returns attribute values.
     *
     * @return attribute values.
     * @throws AMConsoleException if attribute values cannot be determined.
     */
    public Map getSubConfigAttributeValues()
        throws AMConsoleException {
        return subConfigMeta.getSubConfigAttributeValues();
    }

    /**
     * Set attribute values.
     *
     * @param values Attribute values.
     * @throws AMConsoleException if attribute values cannot be set.
     */
    public void setSubConfigAttributeValues(Map values)
        throws AMConsoleException {
        subConfigMeta.setSubConfigAttributeValues(values);
    }

    /**
     * Returns default values of a schema.
     *
     * @param name Name of Schema.
     * @return default values of a schema.
     * @throws AMConsoleException if default values cannot be determined.
     */
    public Map getServiceSchemaDefaultValues(String name)
        throws AMConsoleException {
        try {
            return subConfigMeta.getServiceSchemaDefaultValues(name);
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns true if this service has global sub schema.
     *
     * @return true if this service has global sub schema.
     */
    public boolean hasGlobalSubSchema() {
        return subConfigMeta.hasGlobalSubSchema();
    }

    /**
     * Returns list of sub configuration objects.
     *
     * @return list of sub configuration objects.
     * @see com.sun.identity.console.base.model.SMSubConfig
     */
    public List getSubConfigurations() {
        return subConfigMeta.getSubConfigurations();
    }

    /**
     * Deletes sub configurations.
     *
     * @param names Names of sub configuration which are to be deleted.
     * @throws AMConsoleException if sub configuration cannot be deleted.
     */
    public void deleteSubConfigurations(Set names)
        throws AMConsoleException {
        subConfigMeta.deleteSubConfigurations(names);
    }

    /**
     * Returns plugin name for returning possible sub configuration names.
     * 
     * @param subSchemaName Name of sub schema.
     * @return plugin name for returning possible sub configuration names.
     */
    public String getSelectableSubConfigNamesPlugin(String subSchemaName) {
        String plugin = null;
        ResourceBundle rb = ResourceBundle.getBundle("subConfigNamesPlugin");
        try {
            plugin = rb.getString(serviceName + "." + subSchemaName);
        } catch (MissingResourceException e) {
            //ignore, ok if no plugin is configured
        }
        return plugin;
    }
    
    /**
     * Returns a set of possible names of sub configuration.
     * 
     * @param subSchemaName Name of sub schema
     * @return a set of possible names of sub configuration.
     */
    public Set getSelectableConfigNames(String subSchemaName) {
        Set names = null;
        String plugin = getSelectableSubConfigNamesPlugin(subSchemaName);
        if (plugin != null) {
            try {
                Class clazz = Class.forName(plugin);
                ISubConfigNames instance = (ISubConfigNames)clazz.newInstance();
                names = new TreeSet();
                names.addAll(instance.getNames());
            } catch (InstantiationException ex) {
                debug.error("SubConfigModelImpl.getSelectableConfigNames", ex);
            } catch (IllegalAccessException ex) {
                debug.error("SubConfigModelImpl.getSelectableConfigNames", ex);
            } catch (ClassNotFoundException ex) {
                debug.error("SubConfigModelImpl.getSelectableConfigNames", ex);
            }
        }
        return names;
    }
}
