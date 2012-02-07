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
 * $Id: DefaultSPAttributeMapper.java,v 1.3 2008/06/25 05:48:07 qcheng Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.wsfederation.common.WSFederationException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * This class <code>DefaultSPAttribute</code> implements
 * <code>SPAttributeMapper</code> for mapping the assertion attributes
 * to local attributes configured in the provider configuration.
 */
public class DefaultSPAttributeMapper extends DefaultAttributeMapper 
     implements SPAttributeMapper {

    /**
     * Constructor.
     */
    public DefaultSPAttributeMapper() { 
        debug.message("DefaultSPAttributeMapper.constructor");
        role = SP;
    }

    /**
     * Returns attribute map for the given list of <code>Attribute</code>
     * objects. 
     * @param attributes list <code>Attribute</code>objects.
     * @param userID universal identifier or distinguished name(DN) of the user.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param remoteEntityID <code>EntityID</code> of the remote provider. 
     * @param realm realm name.
     * @return a map of mapped attribute value pair. This map has the
     *         key as the attribute name and the value as the attribute value
     * @exception WSFederationException if any failure.
     */ 
    public Map getAttributes(
        List attributes,
        String userID,
        String hostEntityID,
        String remoteEntityID, 
        String realm
    ) throws WSFederationException {

        if(attributes == null || attributes.size() == 0) {
           throw new WSFederationException(bundle.getString(
                 "nullAttributes")); 
        }

        if(hostEntityID == null) {
           throw new WSFederationException(bundle.getString(
                 "nullHostEntityID"));
        }

        if(realm == null) {
           throw new WSFederationException(bundle.getString(
                 "nullRealm"));
        }
 
        Map<String,Set<String>> map = new HashMap<String,Set<String>>();
        Map configMap = getConfigAttributeMap(realm, hostEntityID);

        for(Iterator iter = attributes.iterator(); iter.hasNext();) {

            Attribute attribute = (Attribute)iter.next();
            Set<String> values = new HashSet(); 
            try {
                List attrValues = attribute.getAttributeValue();
                for(Iterator iter2 = attrValues.iterator(); iter2.hasNext();) {
                    Element attrValue = (Element)iter2.next();
                    values.add(XMLUtils.getElementValue(attrValue));
                }
            } catch (SAMLException se) {
                throw new WSFederationException(se);
            }
            String attributeName = attribute.getAttributeName();

            String localAttribute = (String)configMap.get(attributeName);
            if(localAttribute == null || localAttribute.length()== 0) {
                localAttribute = attributeName;
            }

            Set<String> existingValues = map.get(localAttribute);
            if ( existingValues != null )
            {
                existingValues.addAll(values);
            }
            else
            {
                map.put(localAttribute, values);  
            }
        }
        return map;
    }

}
