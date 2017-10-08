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
 * $Id: DefaultIDPAttributeMapper.java,v 1.4 2008/08/29 02:29:17 superpat7 Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.wsfederation.plugins;

import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.wsfederation.common.WSFederationException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import org.forgerock.util.encode.Base64;
import org.w3c.dom.Element;

/**
 * This class <code>DefaultAttributeMapper</code> implements the
 * <code>IDPAttributeMapper</code> to return the SAML <code>Attribute</code>
 * objects that may be inserted in the SAML Assertion.
 * This IDP attribute mapper reads the attribute map configuration defined
 * in the hosted IDP configuration and construct the SAML
 * <code>Attribute</code> objects. If the mapped values are not present in
 * the data store, this will try to read from the Single sign-on token.
 */
public class DefaultIDPAttributeMapper extends DefaultAttributeMapper implements IDPAttributeMapper {

    private static final String BINARY_FLAG = ";binary";

    /**
     * Constructor
     */
    public DefaultIDPAttributeMapper() {
        debug.message("DefaultIDPAttributeMapper.Constructor");
        role = IDP;
    }

    /**
     * Returns list of SAML <code>Attribute</code> objects for the 
     * IDP framework to insert into the generated <code>Assertion</code>. 
     * @param session Single sign-on session.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @param realm name of the realm.
     * @exception WSFederationException if any failure.
     */
    public List getAttributes(
        Object session,
        String hostEntityID,
        String remoteEntityID,
        String realm 
    ) throws WSFederationException {
 
        if(hostEntityID == null) {
           throw new WSFederationException(bundle.getString(
                 "nullHostEntityID"));
        }

        if(realm == null) {
           throw new WSFederationException(bundle.getString(
                 "nullRealm"));
        }
       
        if(session == null) {
           throw new WSFederationException(bundle.getString(
                 "nullSSOToken"));
        }

        try {
            if (!SessionManager.getProvider().isValid(session)) {
               if (debug.warningEnabled()) {
                  debug.warning("DefaultIDPAttributeMapper.getAttributes: Invalid session");
               }
               return null;
            }

            Map<String, String> configMap = getConfigAttributeMap(realm, hostEntityID);
            if (isEmpty(configMap)) {
               if (debug.messageEnabled()) {
                  debug.message("DefaultIDPAttributeMapper.getAttributes: Configuration map is not defined.");
               }
               return null;
            }

            List<Attribute> attributes = new ArrayList<>();

            Map<String, Set<String>> stringValueMap = null;
            Map<String, byte[][]> binaryValueMap = null;

            try {
                // Resolve attributes to be read from the datastore.
                Set<String> stringAttributes = new HashSet<>(configMap.size());
                Set<String> binaryAttributes = new HashSet<>(configMap.size());
                for (String localAttribute : configMap.values()) {
                    if (isBinaryAttributeValue(localAttribute)) {
                        // add it to the list of attributes to treat as being binary
                        binaryAttributes.add(removeBinaryFlag(localAttribute));
                    } else {
                        stringAttributes.add(localAttribute);
                    }
                }
                if (!stringAttributes.isEmpty()) {
                    stringValueMap = dsProvider.getAttributes(
                        SessionManager.getProvider().getPrincipalName(session), stringAttributes);
                }
                if (!binaryAttributes.isEmpty()) {
                    binaryValueMap = dsProvider.getBinaryAttributes(
                            SessionManager.getProvider().getPrincipalName(session), binaryAttributes);
                }
            } catch (DataStoreProviderException dse) {
                if (debug.warningEnabled()) {
                    debug.warning("DefaultIDPAttributeMapper.getAttributes: Datastore exception", dse);
                }
                //continue to check in ssotoken.
            }

            for (Map.Entry<String, String> entry : configMap.entrySet()) {

                String namespace = null;
                String samlAttribute = entry.getKey();
                StringTokenizer tokenizer = new StringTokenizer(samlAttribute, "|");
                if (tokenizer.countTokens() > 1) {
                    namespace = tokenizer.nextToken();
                    samlAttribute = tokenizer.nextToken();
                }
                String localAttribute = entry.getValue();

                Set<String> attributeValues = null;
                if (isBinaryAttributeValue(localAttribute)) {
                    // Remove the flag as not used for lookup
                    localAttribute = removeBinaryFlag(localAttribute);
                    attributeValues = getBinaryAttributeValues(samlAttribute, localAttribute, binaryValueMap);
                } else {
                    if (isNotEmpty(stringValueMap)) {
                        attributeValues = stringValueMap.get(localAttribute);
                    } else {
                        if (debug.messageEnabled()) {
                            debug.message("DefaultIDPAttributeMapper.getAttribute: "
                                    + localAttribute + " string value map was empty or null");
                        }
                    }
                }
                // If all else fails, try to get the value from the users ssoToken
                if (isEmpty(attributeValues)) {
                    if (debug.messageEnabled()) {
                        debug.message("DefaultIDPAttributeMapper.getAttribute:"
                                + " user profile does not have value for "
                                + localAttribute + " but is going to check ssotoken:");
                    }
                    attributeValues = asSet(SessionManager.getProvider().getProperty(session, localAttribute));
                }

                if (isEmpty(attributeValues)) {
                    if (debug.messageEnabled()) {
                        debug.message("DefaultIDPAttributeMapper.getAttribute: user does not have " + localAttribute);
                    }
                } else {
                    attributes.add(getSAMLAttribute(namespace, samlAttribute, attributeValues));
                }
            }

            return attributes;
        } catch (WSFederationException sme) {
            debug.error("DefaultIDPAttributeMapper.getAttributes: SAML Exception", sme);
            throw new WSFederationException(sme);

        } catch (SessionException se) {
            debug.error("DefaultIDPAttributeMapper.getAttributes: SessionException", se);
            throw new WSFederationException(se);
        }
    }

    /**
     * Returns the SAML <code>Attribute</code> object.
     * @param name attribute name.
     * @param values attribute values.
     * @exception WSFederationException if any failure.
     */
    protected Attribute getSAMLAttribute(String namespace, String name, Set<String> values) throws WSFederationException {

        if (name == null) {
            throw new WSFederationException(bundle.getString("nullInput"));
        }

        if (namespace == null) {
            namespace = WSFederationConstants.CLAIMS_URI;
        }

        Attribute attribute = null;

        if (values != null) {
            List<Element> list = new ArrayList<>(values.size());
            for (String value : values) {
                // Make the AttributeValue element 'by hand', since Attribute
                // constructor below is expecting a list of AttributeValue
                // elements
                String attrValueString = SAMLUtils.makeStartElementTagXML("AttributeValue", true, true)
                        + XMLUtils.escapeSpecialCharacters(value)
                        + SAMLUtils.makeEndElementTagXML("AttributeValue", true);
                list.add(XMLUtils.toDOMDocument(attrValueString, debug).getDocumentElement());
            }
            try {
                attribute = new Attribute(name, namespace, list);
            } catch (SAMLException se) {
                throw new WSFederationException(se);
            }
        }

        return attribute;
    }

    /**
     * Return a Set of Base64 encoded String values that represent the binary attribute values.
     * @param localAttribute the attribute to find in the map.
     * @param samlAttribute the SAML attribute that will be assigned these values
     * @param binaryValueMap the map of binary values for the all binary attributes.
     * @return Set of Base64 encoded String values for the given binary attribute values.
     */
    private Set<String> getBinaryAttributeValues(String samlAttribute, String localAttribute,
                                                 Map<String, byte[][]> binaryValueMap) {

        Set<String> result = null;

        // Expect to find the value in the binary Map
        if (isNotEmpty(binaryValueMap)) {
            byte[][] values = binaryValueMap.get(localAttribute);
            if (values != null && values.length > 0) {
                // Base64 encode the binary values before they are added as an attribute value
                result = new HashSet<>(values.length);
                for (byte[] value : values) {
                    result.add(Base64.encode(value));
                }
                if (debug.messageEnabled()) {
                    debug.message("DefaultIDPAttributeMapper.getBinaryAttributeValues: adding '{}'" +
                            " as binary for attribute named '{}'.", localAttribute, samlAttribute);
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("DefaultIDPAttributeMapper.getBinaryAttributeValues: '{}'"  +
                            " was flagged as binary but no value was found.", localAttribute);
                }
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("DefaultIDPAttributeMapper.getBinaryAttributeValues: '{}' was flagged as binary but " +
                        "binary value map was empty or null.", localAttribute);
            }
        }

        return result;
    }

    private boolean isBinaryAttributeValue(String attribute) {
        return attribute != null && attribute.endsWith(BINARY_FLAG);
    }

    private String removeBinaryFlag(String attribute) {
        int flagStart = attribute.lastIndexOf(BINARY_FLAG);
        return attribute.substring(0, flagStart);
    }
}
