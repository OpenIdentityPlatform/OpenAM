/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultSiteAttributeMapper.java,v 1.2 2009/01/08 04:29:00 hengming Exp $
 *
 */


package com.sun.identity.saml.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLServiceManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;

/**
 * This class reads Attribute Map in local configuration and maps user's local  * attributes to list of <code>Attribute</code> objects to be returned as
 * <code>AttributeStatements</code> elements, as part of the
 * Authentication Assertion returned to the partner during the
 * SSO scenario of Browser Artifact and POST profile.
 * <p>
 *
 */
public class DefaultSiteAttributeMapper implements ConsumerSiteAttributeMapper {

    /**
     * Returns <code>List</code> of <code>Attribute</code> objects
     *
     * @param token  User's session.
     * @param request The HttpServletRerquest object of the request which
     *                may contains query attributes to be included in the
     *                Assertion. This could be null if unavailable.
     * @param response The HttpServletResponse object. This could be null 
     *                if unavailable.
     * @param targetURL value for TARGET query parameter when the user
     *                  accessing the SAML aware servlet or post profile
     *                  servlet. This could be null if unavailabl
     * @return <code>List</code> if <code>Attribute</code> objects.
     *         <code>Attribute</code> is defined in the SAML SDK as part of
     *         <code>com.sun.identity.saml.assertion</code> package.
     * @throws SAMLException if attributes cannot be obtained.
     */
    public List getAttributes(Object token, HttpServletRequest request,
        HttpServletResponse response, String targetURL)
        throws SAMLException {

        Map attrMap = (Map)SAMLServiceManager.getAttribute(
            SAMLConstants.ATTRIBUTE_MAP);

        if ((attrMap == null) || (attrMap.isEmpty())) {
            return null;
        }

        Set localAttrNames = new HashSet();
        localAttrNames.addAll(attrMap.values());
        Map localValueMap = null;
        try {
            DataStoreProvider dsProvider = 
                DataStoreProviderManager.getInstance().
                getDataStoreProvider(SAMLConstants.SAML);
            localValueMap = dsProvider.getAttributes(
                SessionManager.getProvider().getPrincipalName(token),
                localAttrNames); 
        } catch (Exception ex) {
            if (SAMLUtils.debug.warningEnabled()) {
                SAMLUtils.debug.warning("DefaultSiteAttributeMapper." +
                    "getAttributes:", ex);
            }
        }

        List samlAttrs = null;
        for(Iterator iter = attrMap.keySet().iterator(); iter.hasNext();) {
            String samlAttrName = (String)iter.next();
            String localAttrName = (String)attrMap.get(samlAttrName);
            String attrNamespace = null;

            StringTokenizer tokenizer = new StringTokenizer(samlAttrName, "|");
            int tokenCount = tokenizer.countTokens();
            if (tokenCount == 1) {
                attrNamespace = SAMLConstants.assertionSAMLNameSpaceURI;
            } else if (tokenCount == 2) {
                attrNamespace = tokenizer.nextToken();
                samlAttrName = tokenizer.nextToken();
            } else {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("DefaultSiteAttributeMapper." +
                        "getAttribute: invalid saml attribute in attribute " +
                        " map. saml attribute = " + samlAttrName + ", the " +
                        " syntax is namespace|attrName.");
                }
                continue;
            }

            String[] localAttrValues = null;
            if ((localValueMap != null) && (!localValueMap.isEmpty())) {
                Set values = (Set)localValueMap.get(localAttrName); 
                if ((values == null) || (values.isEmpty())) {
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("DefaultSiteAttributeMapper." +
                            "getAttribute: user profile does not have " +
                            "value for " + localAttrName +
                            " but is going to check ssotoken:");
                    }
                } else {
                    localAttrValues = (String[])values.toArray(
                        new String[values.size()]);
                }
            } 

            if (localAttrValues == null) {
                try {
                    localAttrValues = SessionManager.getProvider().getProperty(
                        token, localAttrName);
                } catch (SessionException ex) {
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("DefaultSiteAttributeMapper." +
                            "getAttribute:", ex);
                    }
                }
            }

            if ((localAttrValues == null) || (localAttrValues.length == 0)) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("DefaultSiteAttributeMapper." +
                        "getAttribute: user does not have " +
                        localAttrName);
                }
            } else {
                Attribute samlAttr = getSAMLAttribute(samlAttrName,
                    attrNamespace, localAttrValues);
                if (samlAttr != null) {
                    if (samlAttrs == null) {
                        samlAttrs = new ArrayList();
                    }
                    samlAttrs.add(samlAttr);
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("DefaultSiteAttributeMapper." +
                            "getAttribute: add atttribute = " + samlAttrName +
                            ", attrNamespace = " + attrNamespace +
                            ", values = " + localAttrValues);
                    }
                }
            }
        }

        return samlAttrs;      
    }

    /**
     * Returns the SAML <code>Attribute</code> object.
     *
     * @param name attribute name.
     * @param attrNamespace Name format of the attribute
     * @param values attribute values.
     * @exception SAMLException if any failure.
     */
    protected Attribute getSAMLAttribute(String name, String attrNamespace,
        String[] values) throws SAMLException {

        if ((values == null) || (values.length == 0)) {
            return null;
        }
        Attribute attribute = new Attribute(name, attrNamespace, values[0]);
        for(int i=1; i<values.length; i++) {
            attribute.addAttributeValue(values[i]);
        }
        return attribute;
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     * @param realm realm to check the dynamical profile creation attributes.
     * @return true if dynamical profile creation or ignore profile is enabled,
     * false otherwise.
     */
    static boolean isDynamicalOrIgnoredProfile(String realm) {
        try {
            OrganizationConfigManager orgConfigMgr = AuthD.getAuth().
                getOrgConfigManager(realm);
            ServiceConfig svcConfig = orgConfigMgr.getServiceConfig(
                ISAuthConstants.AUTH_SERVICE_NAME);
            Map attrs = svcConfig.getAttributes();
            String tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.DYNAMIC_PROFILE);
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "DefaultSiteAttributeMapper.isDynamicalOrIgnoredProfile:" +
                    " attr = " + tmp);
            }
            return ((tmp != null) && (tmp.equalsIgnoreCase("createAlias") ||
                tmp.equalsIgnoreCase("true") ||
                tmp.equalsIgnoreCase("ignore")));
        } catch (Exception e) {
            SAMLUtils.debug.error("DefaultSiteAttributeMapper." +
                "isDynamicalOrIgnoredProfile: unable to get attribute", e);
            return false;
        }
    }
}
