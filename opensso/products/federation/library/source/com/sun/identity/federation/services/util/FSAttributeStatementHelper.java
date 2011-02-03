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
 * $Id: FSAttributeStatementHelper.java,v 1.3 2008/06/25 05:47:04 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.util;

import org.w3c.dom.Element;
import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * This class helps in creating  <code>AttributeStatement</code>s to add 
 * auto federation attributes as part of an assertion that is generated 
 * through Single Sign-On.
 */ 
public class FSAttributeStatementHelper {

    /**
     * Gets a SAML <code>AttributeStatement</code> by using an
     * <code>AutoFederate</code> attribute that is configured in Local Provider.
     * @param realm The realm under which the entity resides.
     * @param entityID Host Provider's entity ID.
     * @param sub Liberty Subject.
     * @param ssoToken session of the user
     * @return Generated Auto Federate Attribute Statement.
     * @exception FSException if an error occurred
     */
    public static AttributeStatement getAutoFedAttributeStatement(
        String realm, String entityID, FSSubject sub, Object ssoToken)
        throws FSException
    {
        IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
        BaseConfigType hostConfig = null;
        try {
            if (metaManager != null) {
                hostConfig = metaManager.getIDPDescriptorConfig(realm,entityID);
            }
        } catch (IDFFMetaException fae) {
            FSUtils.debug.error("FSAttributeStatementHelper.getAutoFed" +
                "AttributeStatement: IDFFMetaException ", fae);
            throw new FSException(fae);
        }

        String autoFedAttr = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostConfig, IFSConstants.AUTO_FEDERATION_ATTRIBUTE);
        if (autoFedAttr == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAttributeStatementHelper.getAutoFed:" +
                    "AttributeStatement: AutoFederate Attribute is null");
            }
            return null; 
        }

        List values = new ArrayList();
        try {
            String userID = 
                SessionManager.getProvider().getPrincipalName(ssoToken);
            DataStoreProvider provider = DataStoreProviderManager.getInstance().
                getDataStoreProvider(IFSConstants.IDFF);
            Set vals = provider.getAttribute(userID, autoFedAttr);
            Iterator iter = vals.iterator();
            while(iter.hasNext()) {
                values.add(getAttributeValue((String)iter.next()));
            }

        } catch (SessionException se) {
            FSUtils.debug.error("FSAttributeStatementHelper.getAutoFed" +
                "AttributeStatement: SessionException ", se);
            throw new FSException(se);
        } catch (DataStoreProviderException ie) {
            FSUtils.debug.error("FSAttributeStatementHelper.getAutoFed" +
                "AttributeStatement: DataStoreProviderException ", ie);
            throw new FSException(ie);
        }
        
        if (values == null || values.size() == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAtributeStatementHelper.getAuto:" +
                    "FedAttributeStatement. No values for autofed attribute");
            }
            return null;
        }

        try {
            Attribute attribute = new Attribute(IFSConstants.AUTO_FED_ATTR, 
                IFSConstants.assertionSAMLNameSpaceURI, values);

            List attributeList = new ArrayList();
            attributeList.add(attribute);
            return new AttributeStatement(sub, attributeList);

        } catch (SAMLException ex) {
            FSUtils.debug.error("FSAttributeStatementHelper.getAutoFed" +
                "AttributeStatement: SAMLException ", ex);
            throw new FSException(ex);
        } 
         
    }

    /**
     * Gets the SAML Attribute value as a DOM Element.
     */ 
    private static Element getAttributeValue(String value) throws FSException {
        if (value == null) {
            throw new FSException("nullInputParameter", null);
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(SAMLConstants.ASSERTION_PREFIX)
            .append("AttributeValue")
            .append(SAMLConstants.assertionDeclareStr)
            .append(">").append(value).append("</")
            .append(SAMLConstants.ASSERTION_PREFIX)
            .append("AttributeValue>");

        try {
            return XMLUtils.toDOMDocument(
                sb.toString(), FSUtils.debug).getDocumentElement();
        } catch (Exception ex) {
            throw new FSException(ex);
        }
    }

}
