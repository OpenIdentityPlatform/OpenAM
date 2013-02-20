/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSDefaultRealmAttributePlugin.java,v 1.2 2008/06/25 05:46:53 qcheng Exp $
 *
 */


package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class <code>FSDefaultRealmAttributePlugin</code> is the default
 * attribute plugin implementation of <code>FSRealmAttributePlugin</code>
 * of Identity provider. The default implementation will read the
 * attribute map configuration defined in hosted IDP local configuration
 * and create SAML <code>AttributeStatement</code>s so that they can be
 * inserted into SAML SSO <code>Assertion</code>. 
 */
public class FSDefaultRealmAttributePlugin implements FSRealmAttributePlugin {

    /**
     * Returns list of <code>AttributeStatement</code>s by using attribute
     * map defined in the configuration.
     * @param realm The realm under which the entity resides.
     * @param hostEntityId Hosted identity provider entity id.
     * @param remoteEntityID Remote provider's entity id
     * @param subject Subject subject of the authenticated principal.
     * @param token user's session.
     * @return list of SAML <code>AttributeStatement<code>s.
     */
    public List getAttributeStatements(
           String realm,
           String hostEntityId,
           String remoteEntityID,
           FSSubject subject,
           Object token)
    {

        FSUtils.debug.message(
            "FSDefaultAttributePlugin.getAttributeStatements");

        Map attributeMap = null;
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            if (metaManager != null) {
                IDPDescriptorConfigElement idpConfig =
                    metaManager.getIDPDescriptorConfig(realm, hostEntityId);
                if (idpConfig != null) {
                    Map attributes = IDFFMetaUtils.getAttributes(idpConfig);
                    attributeMap = FSServiceUtils.parseAttributeConfig((List)
                        attributes.get(IFSConstants.IDP_ATTRIBUTE_MAP));
                }
            }

        } catch (IDFFMetaException me) {
            FSUtils.debug.error("FSDefaultAttributePlugin.getAttribute" +
                "Statements: meta exception.", me);
            return null;
        }

        if (attributeMap == null || attributeMap.isEmpty()) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSDefaultAttributePlugin.getAttribute" +
                    "Statements: Attribute map configuration is empty.");
            }
            return null;
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSDefaultAttributePlugin.getAttribute" +
                    "Statements: Attribute map configuration: " + attributeMap);
            }
        }

        List statements = new ArrayList();
        List attributes = new ArrayList();
        try {
            Iterator iter = attributeMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                String attributeName = (String)entry.getKey(); 
                String attributeValue = 
                    getAttributeValue(token, (String)entry.getValue()); 
                if (attributeValue != null) {
                    Attribute attr = new Attribute(
                        attributeName,
                        SAMLConstants.assertionSAMLNameSpaceURI,
                        attributeValue);
                    attributes.add(attr);
                }
            }
       
            AttributeStatement statement = 
                new AttributeStatement(subject, attributes);
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSDefaultAttributePlugin.getAttribute" +
                    "Statements: attribute statement: " + statement.toString());
            }

            statements.add(statement);
            return statements;
        } catch (SAMLException ex) {
            FSUtils.debug.error("FSDefaultAttributePlugin.getAttribute" +
                "Statements: SAML Exception", ex);
        }
        return new ArrayList();
    }

    private String getAttributeValue(Object token, String attrName) {

        if (attrName == null) {
            FSUtils.debug.error("FSDefaultAttributePlugin.getAttribute" +
                "Value: attribute Name is null. Check the attribute map");
            return null;
        }
  
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            String userID = sessionProvider.getPrincipalName(token);
            DataStoreProvider dsProvider =
                DataStoreProviderManager.getInstance().
                    getDataStoreProvider(IFSConstants.IDFF);
            Set attrValues = dsProvider.getAttribute(userID, attrName);
            if (attrValues == null || attrValues.isEmpty()) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSDefaultAttributePlugin.getAttribute"
                        + "Value: values not found for : " + attrName);
                }
                return null;
            }
            return (String)attrValues.iterator().next();
        } catch (SessionException se) {
            FSUtils.debug.error(
                "FSDefaultAttributePlugin.getAttributeValue: exception:",
                se);
        } catch (DataStoreProviderException dspe) {
            FSUtils.debug.error(
                "FSDefaultAttributePlugin.getAttributeValue: exception: ",
                dspe);
        }
        return null;
    }

}
