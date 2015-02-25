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
 * $Id: DefaultSPAttributeMapper.java,v 1.7 2009/11/30 21:11:08 exu Exp $
 *
 */

/**
 * Portions Copyrighted 2014 ForgeRock AS
 */
package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.xml.XMLUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;


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
     * @exception SAML2Exception if any failure.
     */ 
    @Override
    public Map<String, Set<String>> getAttributes(
        List<Attribute> attributes,
        String userID,
        String hostEntityID,
        String remoteEntityID, 
        String realm
    ) throws SAML2Exception {

        if (attributes == null || attributes.isEmpty()) {
           throw new SAML2Exception(bundle.getString("nullAttributes"));
        }

        if (hostEntityID == null) {
           throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }

        if (realm == null) {
           throw new SAML2Exception(bundle.getString("nullRealm"));
        }

        try {
            Map<String, String> configMap = getConfigAttributeMap(realm, hostEntityID, SP);
            if (configMap == null || configMap.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("DefaultSPAttributeMapper.getAttr: Configuration map is not defined.");
                }
                return null;
            }
            if (debug.messageEnabled()) {
                debug.message("DefaultSPAttributeMapper.getAttr: hosted SP attribute map = " + configMap);
            }

            Map<String, Set<String>> map = new HashMap<String, Set<String>>();
            boolean toUnescape = needToUnescapeXMLSpecialCharacters(hostEntityID, remoteEntityID, realm);
            for (Attribute attribute : attributes) {
                Set<String> values = new HashSet<String>();
                List<String> attrValues = attribute.getAttributeValueString();
                if (attrValues != null) {
                    if (toUnescape) {
                        for (String attrValue : attrValues) {
                            values.add(XMLUtils.unescapeSpecialCharacters(attrValue));
                        }
                    } else {
                        values.addAll(attrValues);
                    }
                }
                String attributeName = attribute.getName();

                if (SAML2Constants.ATTR_WILD_CARD.equals(configMap.get(SAML2Constants.ATTR_WILD_CARD))) {
                    // this is the including all attributes as it is case
                    map.put(attributeName, values);
                } else {
                    String localAttribute = configMap.get(attributeName);
                    if (localAttribute != null && !localAttribute.isEmpty()) {
                        map.put(localAttribute, values);
                    }
                }
            }
            return map;
        } catch(SAML2Exception se) {
            debug.error("DefaultSPAccountMapper.getAttributes:MetaException", se);
            throw new SAML2Exception(se.getMessage());
        }
    }

    /**
     * Decides whether it needs to unescape XML special characters for attribute
     * values or not.
     * @param hostEntityID Entity ID for hosted provider.
     * @param remoteEntityID Entity ID for remote provider.
     * @param realm the providers are in.
     * @return <code>true</code> if it should unescape special characters for
     *   attribute values; <code>false</code> otherwise.
     */
    protected boolean needToUnescapeXMLSpecialCharacters(String hostEntityID,
        String remoteEntityID, String realm)
    {
        return true;
    }

}
