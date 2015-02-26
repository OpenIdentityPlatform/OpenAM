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
 * $Id: FSDefaultAttributeMapper.java,v 1.3 2008/06/25 05:46:53 qcheng Exp $
 *
 */


package com.sun.identity.federation.services;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;


/**
 * This class <code>FSDefaultAttributeMapper</code> is the default 
 * implementation of the <code>FSAttributeMapper</code> used at the service
 * provider(SP) and configurable through provider's local configuration.
 * The default implementation reads the assertion attributes and
 * map to the configured attribute map that is defined in Provider's
 * local configuration. The attributes will be populated to the 
 * session of the user for the consumption of any
 * dependent applications. If the configuration is not defined, then the 
 * attributes in the assertion themselves will be populated. 
 */ 
public class FSDefaultAttributeMapper implements FSAttributeMapper {

    /**
     * Returns the attribute map for the given list of 
     * <code>AttributeStatement</code>s. 
     * @param statements list of <code>AttributeStatements</code>s.
     * @param hostEntityId Hosted provider entity id.
     * @param remoteEntityId Remote provider entity id.
     * @param token Single sign-on session token.
     * @return map of attribute values. The  map will have the key as the
     *             attribute name and the map value is the attribute value
     *             that are passed via the single sign-on assertion.
     */
    public Map getAttributes(
        List statements, 
        String hostEntityId, 
        String remoteEntityId, 
        Object token)
    {

        Map map = new HashMap();
        if (statements == null || statements.size() == 0) {
            return map;
        }

        Map configMap = null;
        try {
            IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
            if (metaManager != null) {
                SPDescriptorConfigElement spConfig =
                    metaManager.getSPDescriptorConfig("/", hostEntityId);
                if (spConfig != null) {
                    Map attributes = IDFFMetaUtils.getAttributes(spConfig);
                    configMap = FSServiceUtils.parseAttributeConfig((List)
                        attributes.get(IFSConstants.SP_ATTRIBUTE_MAP));
                }
            }
        } catch (IDFFMetaException fme) {
            FSUtils.debug.error("FSDefaultAttributeMapper.getAttributes:" +
                " Unable to read configuration map.", fme);
            return map;
        }

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSDefaultAttributeMapper.getAttributeMap: Configured map " +
                configMap);
        }
        for (Iterator iter = statements.iterator(); iter.hasNext();) {
            AttributeStatement statement = (AttributeStatement)iter.next();
            List attributes = statement.getAttribute();
            if (attributes == null || attributes.size() == 0) {
                continue;
            } 

            Iterator iter1 = attributes.iterator();
            while (iter1.hasNext()) {
                Attribute attribute = (Attribute)iter1.next();
                List values = null;
                try {
                    values = attribute.getAttributeValue(); 
                } catch (SAMLException ex) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSDefaultAttributeMapper.get" +
                            "Attributes: Exception", ex);
                    }
                    continue;
                }
                if (values == null || values.size() == 0) {
                    continue;
                }
                String attributeName = attribute.getAttributeName();
                if (configMap != null && !configMap.isEmpty()) {
                    String realAttrName = (String)configMap.get(attributeName);
                    if (realAttrName != null && realAttrName.length() > 0) {
                        attributeName = realAttrName;
                    }
                }

                //Retrieve the first only one.
                String valueString = XMLUtils.getElementValue(
                        (Element)values.get(0));
                if (valueString != null && valueString.length() > 0) {
                    map.put(attributeName, valueString);
                }
            }
        }
        return map;
    }

}
