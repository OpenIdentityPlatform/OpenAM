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
 * $Id: SubConfigMeta.java,v 1.2 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

import com.sun.identity.shared.locale.Locale;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/* - NEED NOT LOG - */

public class SubConfigMeta {
    public static final String SUBCONFIG_ID_DELIMITER = "/";

    private static SSOToken adminSSOToken =
        AMAdminUtils.getSuperAdminSSOToken();

    private String serviceName;
    private AMModel amModel;
    ServiceConfig globalConfig;
    ServiceSchema globalSchema;
    private ResourceBundle serviceResourceBundle;
    private Set globalSubSchemaNames;
    private Map mapServiceSchemaNameToL10NName;
    private Set singleInstanceGlobalSubSchemas;
    private Set creatableGlobalSubSchemas;
    private ServiceSchema corrSchema;
    private ServiceConfig parentConfig;
    private List globalSubConfigurations;

    public SubConfigMeta(String serviceName, AMModel model) {
        this.serviceName = serviceName;
        amModel = model;
        initialize();
    }

    private void resetMeta() {
        globalSubSchemaNames = null;
        mapServiceSchemaNameToL10NName = null;
        singleInstanceGlobalSubSchemas = null;
        creatableGlobalSubSchemas = null;
        corrSchema = null;
        parentConfig = null;
        globalSubConfigurations = null;
    }

    /**
     * Returns true if there are global sub schema.
     *
     * @return true if there are global sub schema.
     */
    public boolean hasGlobalSubSchema() {
        return (globalSubSchemaNames != null) &&
            !globalSubSchemaNames.isEmpty();
    }

    /**
     * Returns list of sub configuration objects.
     *
     * @return list of sub configuration objects.
     * @see SMSubConfig
     */
    public List getSubConfigurations() {
        return (globalSubConfigurations != null) ?
            globalSubConfigurations : new ArrayList();
    }

    /**
     * Returns a map of sub schema name to its localized name. We should
     * be able to create sub configuration with these names.
     *
     * @return Map of sub schema name to its localized name.
     */
    public Map getCreateableSubSchemaNames() {
        Map map = new HashMap(creatableGlobalSubSchemas.size() *2);
        for (Iterator i = creatableGlobalSubSchemas.iterator(); i.hasNext(); ) {
            String s = (String)i.next();
            map.put(s, mapServiceSchemaNameToL10NName.get(s));
        }
        return map;
    }

    /**
     * Returns sub service schema.
     *
     * @throws SMSException if sub schema cannot be determined.
     */
    public ServiceSchema getServiceSchema() {
        return corrSchema;
    }

    /**
     * Returns sub service schema of current sub schema.
     *
     * @param name Name of sub schema.
     * @throws SMSException if sub schema cannot be determined.
     */
    public ServiceSchema getServiceSchema(String name)
        throws SMSException {
        return corrSchema.getSubSchema(name);
    }

    /**
     * Returns default values of sub service schema of current sub schema. Map
     * of attribute name (String) to attribute values (Set).
     *
     * @param name Name of sub schema.
     * @throws SMSException if default values be determined.
     */
    public Map getServiceSchemaDefaultValues(String name)
        throws SMSException {
        Map defaultValues = new HashMap();
        ServiceSchema ss = corrSchema.getSubSchema(name);
        Set attributeSchemas = ss.getAttributeSchemas();

        for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)i.next();
            Set values = as.getDefaultValues();
            if (values != null) {
                defaultValues.put(as.getName(), values);
            }
        }
        return defaultValues;
    }

    private void initialize() {
        try {
            ServiceConfigManager mgr = new ServiceConfigManager(
                serviceName, adminSSOToken);
            globalConfig = mgr.getGlobalConfig(null);

            if (globalConfig != null) {
                ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                    serviceName, adminSSOToken);
                globalSchema = schemaMgr.getGlobalSchema();
                String rbName = schemaMgr.getI18NFileName();

                if ((rbName != null) && (rbName.trim().length() > 0)) {
                    serviceResourceBundle = AMResBundleCacher.getBundle(rbName,
                        amModel.getUserLocale());
                }
            }
        } catch (SSOException e) {
            AMModelBase.debug.error("SubConfigMeta.getGlobalSchema" ,e);
        } catch (SMSException e) {
            AMModelBase.debug.error("SubConfigMeta.getGlobalSchema" ,e);
        }
    }

    private boolean validInstance() {
        return (globalSchema != null) && 
            (globalConfig != null) &&
            (serviceResourceBundle != null);
    }

    public void setParentId(String parentId) {
        if (validInstance()) {
            resetMeta();

            try {
                getCorrespondingSchema(parentId);
                getSupportedGlobalSubSchema();

                if ((globalSubSchemaNames != null) && 
                    !globalSubSchemaNames.isEmpty()
                ) {
                    creatableGlobalSubSchemas = new HashSet();
                    creatableGlobalSubSchemas.addAll(globalSubSchemaNames);
                    getSubConfigurationsFromConfig();
                }
            } catch (SSOException e) {
                AMModelBase.debug.error("SubConfigMeta.getParentId", e);
            } catch (SMSException e) {
                AMModelBase.debug.error("SubConfigMeta.getParentId", e);
            }
        }
    }

    private void getSupportedGlobalSubSchema() {
        try {
            globalSubSchemaNames = corrSchema.getSubSchemaNames();

            if ((globalSubSchemaNames != null) &&
                !globalSubSchemaNames.isEmpty()
            ) {
                mapServiceSchemaNameToL10NName = new HashMap();
                singleInstanceGlobalSubSchemas = new HashSet();

                for (Iterator i =globalSubSchemaNames.iterator(); i.hasNext();){
                    String name = (String)i.next();
                    ServiceSchema ss = corrSchema.getSubSchema(name);
                    String i18nKey = ss.getI18NKey();

                    if ((i18nKey == null) || (i18nKey.trim().length() == 0)) {
                        i.remove();
                    } else {
                        mapServiceSchemaNameToL10NName.put(name,
                            Locale.getString(serviceResourceBundle,
                                i18nKey, AMModelBase.debug));
                        if (!ss.supportsMultipleConfigurations()) {
                            singleInstanceGlobalSubSchemas.add(name);
                        }
                    }
                }
            }
        } catch (SMSException e) {
            AMModelBase.debug.error("SubConfigMeta.getSupportedGlobalSubSchema" ,e);
        }
    }

    private void getCorrespondingSchema(String parentId)
        throws SSOException, SMSException {
        corrSchema = globalSchema;
        parentConfig = globalConfig;

        StringTokenizer st = new StringTokenizer(parentId,
            SUBCONFIG_ID_DELIMITER);

        /*
         * discard the first one because it is always /
         */
        if (st.hasMoreTokens()) {
            st.nextToken();
        }

        while (st.hasMoreTokens()) {
            String configId = st.nextToken();
            configId = AMAdminUtils.replaceString(configId, "%2F", "/");
            configId = AMAdminUtils.replaceString(configId, "%25", "%");
            parentConfig = parentConfig.getSubConfig(configId);
            corrSchema = corrSchema.getSubSchema(parentConfig.getSchemaID());
        }
    }

    private void getSubConfigurationsFromConfig() {
        try {
            String[] params = {serviceName, parentConfig.getComponentName()};
            amModel.logEvent("ATTEMPT_READ_GLOBAL_SUB_CONFIGURATION_NAMES",
                params);
            Set names = parentConfig.getSubConfigNames();

            if ((names != null) && !names.isEmpty()) {
                Collator collator = Collator.getInstance(
                    amModel.getUserLocale());
                SortedSet set = new TreeSet(
                    new SMSubConfigComparator(collator));

                for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();
                    ServiceConfig conf = parentConfig.getSubConfig(name);
                    String schemaID = conf.getSchemaID();

                    if (globalSubSchemaNames.contains(schemaID)) {
                        String displayType = (String)
                            mapServiceSchemaNameToL10NName.get(schemaID);
                        set.add(new SMSubConfig(
                            conf.getComponentName(), name, displayType));
                        if (singleInstanceGlobalSubSchemas.contains(schemaID)) {
                            creatableGlobalSubSchemas.remove(schemaID);
                        }
                    }
                }

                if (!set.isEmpty()) {
                    globalSubConfigurations = new ArrayList(set.size());
                    globalSubConfigurations.addAll(set);
                }
            }
            amModel.logEvent("SUCCEED_READ_GLOBAL_SUB_CONFIGURATION_NAMES",
                params);
        } catch (SSOException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                amModel.getErrorString(e)};
            amModel.logEvent(
                "SSO_EXCEPTION_READ_GLOBAL_SUB_CONFIGURATION_NAMES",
                paramsEx);
            AMModelBase.debug.error("SubConfigMeta.getSubConfigurations", e);
        } catch (SMSException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                amModel.getErrorString(e)};
            amModel.logEvent(
                "SMS_EXCEPTION_READ_GLOBAL_SUB_CONFIGURATION_NAMES",
                paramsEx);
            AMModelBase.debug.error("SubConfigMeta.getSubConfigurations", e);
        }
    }

    /**
     * Deletes sub configurations.
     *
     * @param names Set of sub configuration names that are to be deleted.
     * @throws AMConsoleException if sub configurations cannot be deleted.
     */
    public void deleteSubConfigurations(Set names)
        throws AMConsoleException {
        String curName = null;

        try {
            if (parentConfig != null) {
                String[] params = new String[3];
                params[0] = serviceName;
                params[1] = parentConfig.getComponentName();

                for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                    curName = (String)iter.next();
                    params[2] = curName;
                    amModel.logEvent(
                        "ATTEMPT_DELETE_GLOBAL_SUB_CONFIGURATION", params);
                    parentConfig.removeSubConfig(curName);
                    removeFromSubConfigList(curName);
                    amModel.logEvent(
                        "SUCCEED_DELETE_GLOBAL_SUB_CONFIGURATION", params);
                }
            }
        } catch (SSOException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                curName, amModel.getErrorString(e)};
            amModel.logEvent("SSO_EXCEPTION_DELETE_GLOBAL_SUB_CONFIGURATION",
                paramsEx);
            throw new AMConsoleException(amModel.getErrorString(e));
        } catch (SMSException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                curName, amModel.getErrorString(e)};
            amModel.logEvent("SMS_EXCEPTION_DELETE_GLOBAL_SUB_CONFIGURATION",
                paramsEx);
            throw new AMConsoleException(amModel.getErrorString(e));
        }
    }

    private void removeFromSubConfigList(String name) {
        boolean removed = false;
        for (Iterator i = globalSubConfigurations.iterator();
            i.hasNext() && !removed;
        ) {
            SMSubConfig sc = (SMSubConfig)i.next();
            if (sc.getName().equals(name)) {
                i.remove();
                removed  = true;
            }
        }
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
        String[] params = {serviceName, parentConfig.getComponentName(),
            name, schemaName};
        try {
            amModel.logEvent(
                "ATTEMPT_CREATE_GLOBAL_SUB_CONFIGURATION", params);
            parentConfig.addSubConfig(name, schemaName, 0, values);
            amModel.logEvent(
                "SUCCEED_CREATE_GLOBAL_SUB_CONFIGURATION", params);
        } catch (SSOException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                name, schemaName, amModel.getErrorString(e)};
            amModel.logEvent("SSO_EXCEPTION_CREATE_GLOBAL_SUB_CONFIGURATION",
                paramsEx);
            throw new AMConsoleException(amModel.getErrorString(e));
        } catch (SMSException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                name, schemaName, amModel.getErrorString(e)};
            amModel.logEvent("SMS_EXCEPTION_CREATE_GLOBAL_SUB_CONFIGURATION",
                paramsEx);
            throw new AMConsoleException(amModel.getErrorString(e));
        }
    }

    /**
     * Returns attribute values.
     *
     * @return attribute values.
     * @throws AMConsoleException if attribute values cannot be determined.
     */
    public Map getSubConfigAttributeValues()
        throws AMConsoleException {
        String[] params = {serviceName, parentConfig.getComponentName()};
        amModel.logEvent(
            "SUCCEED_READ_GLOBAL_SUB_CONFIGURATION_ATTRIBUTE_VALUES",
            params);
        return parentConfig.getAttributes();
    }

    /**
     * Set attribute values.
     *
     * @param values Attribute values.
     * @throws AMConsoleException if attribute values cannot be set.
     */
    public void setSubConfigAttributeValues(Map values)
        throws AMConsoleException {
        try {
            String[] params = {serviceName, parentConfig.getComponentName()};
            amModel.logEvent(
                "ATTEMPT_WRITE_GLOBAL_SUB_CONFIGURATION_ATTRIBUTE_VALUES",
                params);
            parentConfig.setAttributes(values);
            amModel.logEvent(
                "SUCCEED_WRITE_GLOBAL_SUB_CONFIGURATION_ATTRIBUTE_VALUES",
                params);
        } catch (SSOException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                amModel.getErrorString(e)};
            amModel.logEvent(
                "SSO_EXCEPTION_WRITE_GLOBAL_SUB_CONFIGURATION_ATTRIBUTE_VALU",
                paramsEx);
            throw new AMConsoleException(amModel.getErrorString(e));
        } catch (SMSException e) {
            String[] paramsEx = {serviceName, parentConfig.getComponentName(),
                amModel.getErrorString(e)};
            amModel.logEvent(
                "SMS_EXCEPTION_WRITE_GLOBAL_SUB_CONFIGURATION_ATTRIBUTE_VALU",
                paramsEx);
            throw new AMConsoleException(amModel.getErrorString(e));
        }
    }
}
