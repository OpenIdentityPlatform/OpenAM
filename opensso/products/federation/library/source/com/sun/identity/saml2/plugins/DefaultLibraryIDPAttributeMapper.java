/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: DefaultLibraryIDPAttributeMapper.java,v 1.3 2009/11/30 21:11:08 exu Exp $
 */

package com.sun.identity.saml2.plugins;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.HashSet;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;

/**
 * This class <code>DefaultLibraryIDPAttributeMapper</code> implements the
 * <code>IDPAttributeMapper</code> to return the SAML <code>Attribute</code>
 * objects that may be inserted in the SAML Assertion.
 * This IDP attribute mapper reads the attribute map configuration defined
 * in the hosted IDP configuration and construct the SAML
 * <code>Attribute</code> objects. If the mapped values are not present in
 * the data store, this will try to read from the Single sign-on token.
 */
public class DefaultLibraryIDPAttributeMapper extends DefaultAttributeMapper 
    implements IDPAttributeMapper {

    /**
     * Constructor
     */
    public DefaultLibraryIDPAttributeMapper() {
    }

    /**
     * Returns list of SAML <code>Attribute</code> objects for the 
     * IDP framework to insert into the generated <code>Assertion</code>.
     * 
     * @param session Single sign-on session.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @param realm name of the realm.
     * @exception SAML2Exception if any failure.
     */
    public List getAttributes(Object session, String hostEntityID,
        String remoteEntityID, String realm) throws SAML2Exception
    {
 
        if (hostEntityID == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }

        if (realm == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }
       
        if (session == null) {
            throw new SAML2Exception(bundle.getString("nullSSOToken"));
        }

        try {
            if (!SessionManager.getProvider().isValid(session)) {
                if (debug.warningEnabled()) {
                    debug.warning("DefaultLibraryIDPAttributeMapper." +
                        "getAttributes: Invalid session");
                }
                return null;
            }

            Map configMap = getConfigAttributeMap(realm, remoteEntityID, SP);
            if (debug.messageEnabled()) {
                debug.message("DefaultLibraryIDPAttributeMapper." +
                    "getAttributes: remote SP attribute map = " + configMap);
            }
            if ((configMap == null) || (configMap.isEmpty())) {
                configMap = getConfigAttributeMap(realm, hostEntityID, IDP);
                if ((configMap == null) || (configMap.isEmpty())) {
                    if (debug.messageEnabled()) {
                        debug.message("DefaultLibraryIDPAttributeMapper." +
                            "getAttributes: Configuration map is not defined.");
                    }
                    return null;
                }
                if (debug.messageEnabled()) {
                    debug.message("DefaultLibraryIDPAttributeMapper." +
                        "getAttributes: hosted IDP attribute map=" + configMap);
                }
            }

            List attributes = new ArrayList();
            
            Set localAttributes = new HashSet();
            localAttributes.addAll(configMap.values());
            Map valueMap = null;

            if (!isDynamicalOrIgnoredProfile(realm)) {
                try {
                    valueMap = dsProvider.getAttributes(
                        SessionManager.getProvider().getPrincipalName(session),
                        localAttributes); 
                } catch (DataStoreProviderException dse) {
                    if (debug.warningEnabled()) {
                        debug.warning("DefaultLibraryIDPAttributeMapper." +
                            "getAttributes:", dse);
                    }
                    //continue to check in ssotoken.
                }
            }

            Iterator iter = configMap.keySet().iterator();
            while(iter.hasNext()) {
                String samlAttribute = (String)iter.next();
                String localAttribute = (String)configMap.get(samlAttribute);
                String nameFormat = null;
                // if samlAttribute has format nameFormat|samlAttribute
                StringTokenizer tokenizer = 
                    new StringTokenizer(samlAttribute, "|");
                if (tokenizer.countTokens() > 1) {
                    nameFormat = tokenizer.nextToken();
                    samlAttribute = tokenizer.nextToken();
                }
                String[] localAttributeValues = null;
                if ((valueMap != null) && (!valueMap.isEmpty())) {
                    Set values = (Set)valueMap.get(localAttribute); 
                    if ((values == null) || (values.isEmpty())) {
                        if (debug.messageEnabled()) {
                            debug.message("DefaultLibraryIDPAttributeMapper." +
                                "getAttribute: user profile does not have " +
                                "value for " + localAttribute +
                                " but is going to check ssotoken:");
                        }
                    } else {
                        localAttributeValues = (String[])values.toArray(
                            new String[values.size()]);
                    }
                } 
                if (localAttributeValues == null) {
                    localAttributeValues = SessionManager.
                        getProvider().getProperty(session, localAttribute);
                }

                if ((localAttributeValues == null) ||
                    (localAttributeValues.length == 0)) {

                    if (debug.messageEnabled()) {
                        debug.message("DefaultLibraryIDPAttributeMapper." +
                            "getAttribute: user does not have " +
                            localAttribute);
                    }
                    continue;
                }

                attributes.add(getSAMLAttribute(samlAttribute, nameFormat,
                    localAttributeValues, hostEntityID, remoteEntityID, realm));
            }
            return attributes;      

        } catch (SessionException se) {
            debug.error("DefaultLibraryIDPAttribute.getAttributes: ", se);
            throw new SAML2Exception(se);
        }

    }

    /**
     * Decides whether it needs to escape XML special characters for attribute
     * values or not.
     * @param hostEntityID Entity ID for hosted provider.
     * @param remoteEntityID Entity ID for remote provider.
     * @param realm the providers are in.
     * @return <code>true</code> if it should escape special characters for
     *   attribute values; <code>false</code> otherwise.
     */
    protected boolean needToEscapeXMLSpecialCharacters(String hostEntityID,
        String remoteEntityID, String realm)
    {
        return true;
    }

    /**
     * Returns the SAML <code>Attribute</code> object.
     *
     * @param name attribute name.
     * @param nameFormat Name format of the attribute
     * @param values attribute values.
     * @param hostEntityID Entity ID for hosted provider.
     * @param remoteEntityID Entity ID for remote provider.
     * @param realm the providers are in.
     * @return SAML <code>Attribute</code> element.
     * @exception SAML2Exception if any failure.
     */
    protected Attribute getSAMLAttribute(String name, String nameFormat,
         String[] values, String hostEntityID, String remoteEntityID,
         String realm) 
    throws SAML2Exception {

        if (name == null) {
            throw new SAML2Exception(bundle.getString("nullInput"));
        }

        AssertionFactory factory = AssertionFactory.getInstance();
        Attribute attribute =  factory.createAttribute();

        attribute.setName(name);
        if (nameFormat != null) {
            attribute.setNameFormat(nameFormat);
        }
        if (values != null) {
            boolean toEscape = needToEscapeXMLSpecialCharacters(
                hostEntityID, remoteEntityID, realm);
            List list = new ArrayList();
            for (int i=0; i<values.length; i++) {
                if (toEscape) {
                    list.add(XMLUtils.escapeSpecialCharacters(values[i]));
                } else {
                    list.add(values[i]);
                }
            }
            attribute.setAttributeValueString(list);
        }
        return attribute;
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     * @param realm realm to check the dynamical profile creation attributes.
     * @return true if dynamical profile creation or ignore profile is enabled,
     * false otherwise.
     */
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        return true;
    }
}
