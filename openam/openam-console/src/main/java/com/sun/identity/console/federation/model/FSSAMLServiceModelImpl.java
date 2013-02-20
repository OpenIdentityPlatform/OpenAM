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
 * $Id: FSSAMLServiceModelImpl.java,v 1.3 2008/06/25 05:49:40 qcheng Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMResBundleCacher;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class FSSAMLServiceModelImpl
    extends AMModelBase
    implements FSSAMLServiceModel {
    private ServiceSchemaManager serviceSchemaManager;
    private Map attributeSchemas = new HashMap();
    private ResourceBundle resBundle;
    private static final String SAML_SERVICE_NAME = "iPlanetAMSAMLService";
    private static final String SAML_TRUSTED_PARTNERS =
        "iplanet-am-saml-partner-urls";
    
    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public FSSAMLServiceModelImpl(HttpServletRequest req,  Map map) {
        super(req, map);
        init();
    }
    
    /**
     * Returns localized labels for all attributes.
     *
     * @return localized labels for all attributes.
     */
    public Map getAttributeLabels() {
        Map labels = new HashMap(attributeSchemas.size() *2);
        for (Iterator i = attributeSchemas.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            AttributeSchema as = (AttributeSchema)attributeSchemas.get(name);
            String i18nKey = as.getI18NKey();
            
            if (resBundle != null) {
                labels.put(name, Locale.getString(resBundle, i18nKey, debug));
            } else {
                labels.put(name, i18nKey);
            }
        }
        return labels;
    }
    
    /**
     * Returns localized inline help for all attributes.
     * 
     * @return localized inline help for all attributes.
     */
    public Map getAttributeInlineHelps() {
        Map helps = new HashMap(attributeSchemas.size() *2);
        for (Iterator iter = attributeSchemas.keySet().iterator();
        iter.hasNext();
        ) {
            String name = (String)iter.next();
            AttributeSchema as = (AttributeSchema)attributeSchemas.get(name);
            String i18nKey = as.getI18NKey() + ".help";
            
            if (resBundle != null) {
                String help = Locale.getString(resBundle, i18nKey, debug);
                if ((help != null) && !help.equals(i18nKey)) {
                    helps.put(name, help);
                }
            }
        }
        return helps;
    }
    
    /**
     * Returns a Map SAMLv1.x attribute values.
     *
     * @return SAMLv1.x attribute values.
     */
    public Map getAttributeValues() {
        Map values = new HashMap(attributeSchemas.size() *2);
        for (Iterator i = attributeSchemas.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            AttributeSchema as = (AttributeSchema)attributeSchemas.get(name);
            values.put(name, as.getDefaultValues());
        }
        return values;
    }
    
    /**
     * Set SAMLv1.x attribute values.
     *
     * @param values Attribute values. Map of attribute name to set of values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void setAttributeValues(Map values) throws AMConsoleException {
        String[] params = new String[3];
        params[0] = SAML_SERVICE_NAME;
        params[1] = "-";
        String curAttrSchemaName = "";
        try {
            for (Iterator iter = values.entrySet().iterator(); iter.hasNext();){
                Map.Entry entry = (Map.Entry)iter.next();
                String name = (String)entry.getKey();
                curAttrSchemaName = name;
                params[2] = name;
                logEvent("ATTEMPT_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                    params);
                AttributeSchema as =
                    (AttributeSchema)attributeSchemas.get(name);
                as.setDefaultValues((Set)entry.getValue());
                logEvent("SUCCEED_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                    params);
            }
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {SAML_SERVICE_NAME, "-", curAttrSchemaName,
            strError};
            logEvent("SSO_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {SAML_SERVICE_NAME, "-", curAttrSchemaName,
            strError};
            logEvent("SMS_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }
    
    private void init() {
        String curSchemaType = "*";
        try {
            serviceSchemaManager = new ServiceSchemaManager(
                SAML_SERVICE_NAME, getUserSSOToken());
            String i18nFileName = serviceSchemaManager.getI18NFileName();
            
            if ((i18nFileName != null) && (i18nFileName.trim().length() > 0)) {
                resBundle = AMResBundleCacher.getBundle(
                    i18nFileName, getUserLocale());
            }
            
            String[] params = new String[3];
            params[0] = SAML_SERVICE_NAME;
            params[2] = "*";
            Set schemaTypes = serviceSchemaManager.getSchemaTypes();
            
            for (Iterator iter = schemaTypes.iterator(); iter.hasNext(); ) {
                SchemaType type = (SchemaType)iter.next();
                ServiceSchema schema = serviceSchemaManager.getSchema(type);
                
                if (schema!= null) {
                    curSchemaType = type.getType();
                    params[1] = curSchemaType;
                    logEvent("ATTEMPT_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                        params);
                    Set aschemas = schema.getAttributeSchemas();
                    
                    for (Iterator i = aschemas.iterator(); i.hasNext(); ) {
                        AttributeSchema as = (AttributeSchema)i.next();
                        String i18n = as.getI18NKey();
                        
                        if ((i18n != null) && (i18n.trim().length() > 0)) {
                            attributeSchemas.put(as.getName(), as);
                        }
                    }
                    logEvent("SUCCEED_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                        params);
                }
            }
        } catch (SSOException e) {
            String[] paramsEx = {SAML_SERVICE_NAME, "*", curSchemaType,
            getErrorString(e)};
            logEvent("SSO_EXCEPTION_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            debug.error("FSSAMLServiceModelImpl.init", e);
        } catch (SMSException e) {
            String[] paramsEx = {SAML_SERVICE_NAME, "*", curSchemaType,
            getErrorString(e)};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            debug.error("FSSAMLServiceModelImpl.init", e);
        }
    }
    
    /**
     * delete trusted partners.
     *
     * @param values a Set of trusted partners .
     * @throws AMConsoleException if name cannot be set.
     */
    public void deleteTrustPartners(Set values)
    throws AMConsoleException {
        String schemaName = SAML_TRUSTED_PARTNERS;
        String[] params = new String[3];
        params[0] = SAML_SERVICE_NAME;
        params[1] = "-";
        params[2] = schemaName;
        
        try {
            AttributeSchema as =
                (AttributeSchema)attributeSchemas.get(schemaName);
            Set orgValues = (Set)as.getDefaultValues();
            orgValues.removeAll(values);
            as.setDefaultValues(orgValues);
            logEvent("SUCCEED_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {SAML_SERVICE_NAME, "-", schemaName,
            strError};
            logEvent("SSO_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {SAML_SERVICE_NAME, "-", schemaName,
            strError};
            logEvent("SMS_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }
    
    /*
     * modify trusted partners.
     *
     * @param values a Set of trusted partners .
     * @throws AMConsoleException if name cannot be set.
     */
    public void modifyTrustPartners(Set values)
    throws AMConsoleException {
        String curAttrSchemaName = SAML_TRUSTED_PARTNERS;
        String[] params = new String[5];
        params[0] = SAML_SERVICE_NAME;
        params[1] = "-";
        params[2] = curAttrSchemaName;
        params[3] = "-";
        params[4] = values.toString();
        
        try {
            AttributeSchema as =
                (AttributeSchema)attributeSchemas.get(curAttrSchemaName);
            as.setDefaultValues(values);
            logEvent("SUCCEED_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {SAML_SERVICE_NAME, "-", curAttrSchemaName,
            strError};
            logEvent("SSO_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {SAML_SERVICE_NAME, "-", curAttrSchemaName,
            strError};
            logEvent("SMS_EXCEPTION_SET_ATTR_VALUE_ATR_SCHEMA_SCHEMA_TYPE",
                paramsEx);
            throw new AMConsoleException(strError);
        }
    }
}
