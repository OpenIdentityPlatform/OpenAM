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
 * $Id: IDRepoModelImpl.java,v 1.4 2009/12/09 20:51:22 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS 
 */

package com.sun.identity.console.realm.model;

import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.locale.Locale;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMResBundleCacher;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.common.IdRepoUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class IDRepoModelImpl
    extends AMModelBase
    implements IDRepoModel
{
    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public IDRepoModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns a set of ID Repository names.
     *
     * @param realmName Name of Realm.
     * @return a set of ID Repository names.
     * @throws AMConsoleException if there are errors getting these names.
     */
    public Set getIDRepoNames(String realmName)
        throws AMConsoleException {
        String[] param = {realmName};
        logEvent("ATTEMPT_GET_ID_REPO_NAMES", param);

        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(
                realmName, null);
            Set names = (cfg != null) ? cfg.getSubConfigNames() :
                Collections.EMPTY_SET;
            logEvent("SUCCEED_GET_ID_REPO_NAMES", param);
            return names;
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("SMS_EXCEPTION_GET_ID_REPO_NAMES", paramsEx);
            // using 'no.properties' error for consistency between modules
            throw new AMConsoleException("no.properties");
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("SSO_EXCEPTION_GET_ID_REPO_NAMES", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns map of ID Repo type name to its localized string.
     *
     * @return map of ID Repo type name to its localized string.
     */
    public Map getIDRepoTypesMap()
        throws AMConsoleException {
        try {
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ResourceBundle rb = AMResBundleCacher.getBundle(
                schemaMgr.getI18NFileName(), getUserLocale());
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            Set names = orgSchema.getSubSchemaNames();
            Map map = new HashMap(names.size() *2);

            for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                ServiceSchema ss = orgSchema.getSubSchema(name);
                String i18nKey = ss.getI18NKey();

                if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                    map.put(name, Locale.getString(rb, i18nKey, debug));
                }
            }

            return map;
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns an option list of ID Repo type name to its localized string.
     *
     * @return an option list of ID Repo type name to its localized string.
     */
    public OptionList getIDRepoTypes()
        throws AMConsoleException {
        Map map = AMFormatUtils.reverseStringMap(getIDRepoTypesMap());
        OptionList optList = new OptionList();
        List sorted = AMFormatUtils.sortKeyInMap(map, getUserLocale());

        for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
            String label = (String)iter.next();
            optList.add(label, (String)map.get(label));
        }

        return optList;
    }

    /**
     * Returns property sheet XML for ID Repo Profile.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class Name of View Bean.
     * @param type Type of ID Repo.
     * @param bCreate <code>true</code> for creation operation.
     * @return property sheet XML for ID Repo Profile.
     */
    public String getPropertyXMLString(
        String realmName,
        String viewbeanClassName,
        String type,
        boolean bCreate
    ) throws AMConsoleException {
        try {
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            ServiceSchema ss = orgSchema.getSubSchema(type);
            DelegationConfig dConfig = DelegationConfig.getInstance();
            boolean canModify = dConfig.hasPermission(realmName, null,
                AMAdminConstants.PERMISSION_MODIFY, this, viewbeanClassName);
            Set attributeSchemas = ss.getAttributeSchemas();
            PropertyXMLBuilder.removeAttributeSchemaWithoutI18nKey(
                attributeSchemas);
            PropertyXMLBuilder builder = new PropertyXMLBuilder(
                IdConstants.REPO_SERVICE, this, attributeSchemas, SchemaType.ORGANIZATION);
            if (!bCreate && !canModify) {
                builder.setAllAttributeReadOnly(true);
            }

            String xml = builder.getXML();
            String xmlFile = (bCreate) ?
                "com/sun/identity/console/propertyRMIDRepoAdd.xml" :
                "com/sun/identity/console/propertyRMIDRepoEdit.xml";
            String header = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFile));
            xml = PropertyXMLBuilder.prependXMLProperty(xml, header);
            return xml;
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns default attribute values of ID Repo.
     *
     * @param type Type of ID Repo.
     * @return default attribute values of ID Repo.
     */
    public Map getDefaultAttributeValues(String type) {
        Map values = null;

        try {
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            ServiceSchema ss = orgSchema.getSubSchema(type);
            Set attrs = ss.getAttributeSchemas();
            values = new HashMap(attrs.size() *2);

            for (Iterator iter = attrs.iterator(); iter.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)iter.next();
                String i18nKey = as.getI18NKey();
                if ((i18nKey != null) && (i18nKey.length() > 0)) {
                    values.put(as.getName(), as.getDefaultValues());
                }
            }
        } catch (SMSException e) {
            debug.warning("IDRepoModelImpl.getDefaultAttributeValues", e);
        } catch (SSOException e) {
            debug.warning("IDRepoModelImpl.getDefaultAttributeValues", e);
        }
        
        if((values!=null) && IdRepoUtils.hasIdRepoSchema(type)){
            values.put("idRepoLoadSchema", Collections.EMPTY_SET);
        }
        
        return (values != null) ? values : Collections.EMPTY_MAP;
    }

    /**
     * Returns attribute values of ID Repo.
     *
     * @param realmName Name of realm.
     * @param name Name of ID Repo.
     * @return attribute values of ID Repo.
     * @throws AMConsoleException if attribute values cannot be obtained.
     */
    public Map getAttributeValues(String realmName, String name)
        throws AMConsoleException {
        String[] params = {realmName, name};
        logEvent("ATTEMPT_GET_ATTR_VALUES_ID_REPO", params);

        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(
                realmName, null);
            ServiceConfig ss = cfg.getSubConfig(name);
            Map attrValues = ss.getAttributes();
            logEvent("SUCCEED_GET_ATTR_VALUES_ID_REPO", params);
            return attrValues;
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, name, strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUES_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, name, strError};
            logEvent("SSO_EXCEPTION_GET_ATTR_VALUES_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Creates ID Repo object.
     *
     * @param realmName Name of Realm.
     * @param idRepoName Name of ID Repo.
     * @param idRepoType Type of ID Repo.
     * @param values Map of attribute name to set of values.
     * @throws AMConsoleException if object cannot be created.
     */
    public void createIDRepo(
        String realmName,
        String idRepoName,
        String idRepoType,
        Map values
    ) throws AMConsoleException {
        String[] params = {realmName, idRepoName, idRepoType};
        logEvent("ATTEMPT_CREATE_ID_REPO", params);
        
        values.remove("idRepoLoadSchema");
        
        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(
                realmName, null);

            if (cfg == null) {
                cfg = createOrganizationConfig(realmName, idRepoType);
            }

            cfg.addSubConfig(idRepoName, idRepoType, 0, values);
            logEvent("SUCCEED_CREATE_ID_REPO", params);

        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, idRepoName, idRepoType, strError};
            logEvent("SMS_EXCEPTION_CREATE_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, idRepoName, idRepoType, strError};
            logEvent("SSO_EXCEPTION_CREATE_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private ServiceConfig createOrganizationConfig(
        String realmName,
        String idRepoType
    ) throws AMConsoleException {
        try {
            OrganizationConfigManager orgCfgMgr = new OrganizationConfigManager(
                getUserSSOToken(), realmName);
            Map attrValues = getDefaultAttributeValues();
            return orgCfgMgr.addServiceConfig(
                IdConstants.REPO_SERVICE, attrValues);
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    public Map getDefaultAttributeValues() {
        Map values = null;

        try {
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            Set attrs = orgSchema.getAttributeSchemas();
            values = new HashMap(attrs.size() *2);

            for (Iterator iter = attrs.iterator(); iter.hasNext(); ) {
                AttributeSchema as = (AttributeSchema) iter.next();
                values.put(as.getName(), as.getDefaultValues());
            }
        } catch (SMSException e) {
            debug.warning("IDRepoModelImpl.getDefaultAttributeValues", e);
        } catch (SSOException e) {
            debug.warning("IDRepoModelImpl.getDefaultAttributeValues", e);
        }

        return (values != null) ? values : Collections.EMPTY_MAP;
    }

    /**
     * Deletes ID Repo objects.
     *
     * @param realmName Name of Realm.
     * @param names Set of ID Repo names to be deleted.
     * @throws AMConsoleException if object cannot be deleted.
     */
    public void deleteIDRepos(String realmName, Set names)
        throws AMConsoleException {
        String[] params = new String[2];
        params[0] = realmName;
        String curName = "";

        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(
                realmName, null);

            for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                curName = name;
                params[1] = name;
                logEvent("ATTEMPT_DELETE_ID_REPO", params);

                cfg.removeSubConfig(name);

                logEvent("SUCCEED_DELETE_ID_REPO", params);
            }
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, curName, strError};
            logEvent("SMS_EXCEPTION_DELETE_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, curName, strError};
            logEvent("SSO_EXCEPTION_DELETE_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Edit ID Repo object.
     *
     * @param realmName Name of Realm.
     * @param idRepoName Name of ID Repo.
     * @param values Map of attribute name to set of values.
     * @throws AMConsoleException if object cannot be created.
     */
    public void editIDRepo(String realmName, String idRepoName, Map values)
        throws AMConsoleException {
        String[] params = {realmName, idRepoName};
        logEvent("ATTEMPT_MODIFY_ID_REPO", params);

        values.remove("idRepoLoadSchema");
        
        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(
                realmName, null);
            ServiceConfig ss = cfg.getSubConfig(idRepoName);
            ss.setAttributes(values);
            logEvent("SUCCEED_MODIFY_ID_REPO", params);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, idRepoName, strError};
            logEvent("SMS_EXCEPTION_MODIFY_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, idRepoName, strError};
            logEvent("SSO_EXCEPTION_MODIFY_ID_REPO", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns ID Repo Type of an object.
     *
     * @param realmName Name of Realm.
     * @param idRepoName Name of ID Repo.
     * @return ID Repo Type of an object.
     * @throws AMConsoleException if type cannot be determined.
     */
    public String getIDRepoType(String realmName, String idRepoName)
        throws AMConsoleException {
        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
                IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(
                realmName, null);
            ServiceConfig ss = cfg.getSubConfig(idRepoName);
            return ss.getSchemaID();
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    public void loadIdRepoSchema(String idRepoName,
        String realm, ServletContext servletCtx)
        throws AMConsoleException {
        try {
            IdRepoUtils.loadIdRepoSchema(getUserSSOToken(), idRepoName, realm, servletCtx);
        } catch (IdRepoException ex) {
            throw new AMConsoleException(getErrorString(ex));
        }

    }

}
