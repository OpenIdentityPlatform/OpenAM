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
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins;

import static org.forgerock.openam.utils.AttributeUtils.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.HashSet;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.encode.Base64;

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
 * <p>
 * Supports attribute mappings defined as:
 *
 * [NameFormatURI|]SAML ATTRIBUTE NAME=["]LOCAL NAME["][;binary]
 *
 * where [] elements are optional.
 *
 * Using "" (double quotes) around the LOCAL NAME will turn it into a static value.
 *
 * Adding ;binary at the end of the LOCAL NAME will indicate that this attribute should be treated as binary and Base64
 * encoded.
 * <p>
 * Examples:
 * <p>
 * <code>
 * email=mail
 * </code>
 * will map the local attribute called mail onto a SAML attribute called email.
 * <p>
 * <code>
 * urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:cn=cn
 * </code>
 * will map the local attribute called cn onto a SAML attribute called
 * urn:mace:dir:attribute-def:cn with a name format of urn:oasis:names:tc:SAML:2.0:attrname-format:uri
 * <p>
 * <code>
 * partnerID="staticPartnerIDValue"
 * </code>
 * will add a static SAML attribute called partnerID with a value of staticPartnerIDValue
 * <p>
 * <code>
 * urn:oasis:names:tc:SAML:2.0:attrname-format:uri|nameID="staticNameIDValue"
 * </code>
 * will add a static SAML attribute called nameID with a value of staticNameIDValue
 * with a name format of urn:oasis:names:tc:SAML:2.0:attrname-format:uri
 *<p>
 *<code>
 * objectGUID=objectGUID;binary
 *</code>
 * will map the local binary attribute called objectGUID onto a SAML attribute called objectGUID Base64 encoded.
 *<p>
 *<code>
 * urn:oasis:names:tc:SAML:2.0:attrname-format:uri|objectGUID=objectGUID;binary
 *</code>
 * will map the local binary attribute called objectGUID onto a SAML attribute called objectGUID Base64 encoded with a
 * name format of urn:oasis:names:tc:SAML:2.0:attrname-format:uri.
 */
public class DefaultLibraryIDPAttributeMapper extends DefaultAttributeMapper implements IDPAttributeMapper {

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
    public List getAttributes(Object session, String hostEntityID, String remoteEntityID, String realm)
            throws SAML2Exception {
 
        if (hostEntityID == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }

        if (realm == null) {
            throw new SAML2Exception(bundle.getString("nullHostEntityID"));
        }
       
        if (session == null) {
            throw new SAML2Exception(bundle.getString("nullSSOToken"));
        }

        String debugMethod = "DefaultLibraryIDPAttributeMapper.getAttributes: ";

        try {
            if (!SessionManager.getProvider().isValid(session)) {
                debug.warning(debugMethod + "Invalid session.");
                return null;
            }

            Map<String, String> configMap = getConfigAttributeMap(realm, remoteEntityID, SP);
            debug.message(debugMethod + "Remote SP attribute map = {}", configMap);
            if (CollectionUtils.isEmpty(configMap)) {
                configMap = getConfigAttributeMap(realm, hostEntityID, IDP);
                if (CollectionUtils.isEmpty(configMap)) {
                    debug.message(debugMethod + "Configuration map is not defined.");
                    return null;
                }
                debug.message(debugMethod + "Hosted IDP attribute map = {}", configMap);
            }

            List<Attribute> attributes = new ArrayList<>();
            Map<String, Set<String>> stringValueMap = null;
            Map<String, byte[][]> binaryValueMap = null;

            // Don't try to read the attributes from the datastore if the ignored profile is enabled in this realm.
            if (!isIgnoredProfile(session, realm)) {
                try {
                    // Resolve attributes to be read from the datastore.
                    Set<String> stringAttributes = new HashSet<>(configMap.size());
                    Set<String> binaryAttributes = new HashSet<>(configMap.size());
                    for (String localAttribute : configMap.values()) {
                        if (isStaticAttribute(localAttribute)) {
                            // skip over, handled directly in next step
                        } else if (isBinaryAttribute(localAttribute)) {
                            // add it to the list of attributes to treat as being binary
                            binaryAttributes.add(removeBinaryAttributeFlag(localAttribute));
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
                    debug.warning(debugMethod + "Error accessing the datastore.", dse);
                    //continue to check in ssotoken.
                }
            }

            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                String samlAttribute =  entry.getKey();
                String localAttribute = entry.getValue();
                String nameFormat = null;
                // check if samlAttribute has format nameFormat|samlAttribute
                StringTokenizer tokenizer = new StringTokenizer(samlAttribute, "|");
                if (tokenizer.countTokens() > 1) {
                    nameFormat = tokenizer.nextToken();
                    samlAttribute = tokenizer.nextToken();
                }

                Set<String> attributeValues = null;
                if (isStaticAttribute(localAttribute)) {
                    localAttribute = removeStaticAttributeFlag(localAttribute);
                    // Remove the static flag before using it as the static value
                    attributeValues = CollectionUtils.asSet(localAttribute);
                    debug.message(debugMethod + "Adding static value {} for attribute named {}",
                            localAttribute, samlAttribute);
                } else {
                    if (isBinaryAttribute(localAttribute)) {
                        // Remove the flag as not used for lookup
                        localAttribute = removeBinaryAttributeFlag(localAttribute);
                        attributeValues = getBinaryAttributeValues(samlAttribute, localAttribute, binaryValueMap);
                    } else {
                        if (stringValueMap != null && !stringValueMap.isEmpty()) {
                            attributeValues = stringValueMap.get(localAttribute);
                        } else {
                            debug.message(debugMethod + "{} string value map was empty or null.", localAttribute);
                        }
                    }

                    // If all else fails, try to get the value from the users ssoToken
                    if (CollectionUtils.isEmpty(attributeValues)) {
                        debug.message(debugMethod + "User profile does not have value for {}, checking SSOToken.",
                                localAttribute);
                        attributeValues =
                               CollectionUtils.asSet(SessionManager.getProvider().getProperty(session, localAttribute));
                    }
                }
                if (CollectionUtils.isEmpty(attributeValues)) {
                    debug.message(debugMethod + "{} not found in user profile or SSOToken.", localAttribute);
                } else {
                    attributes.add(getSAMLAttribute(samlAttribute, nameFormat,
                            attributeValues, hostEntityID, remoteEntityID, realm));
                }
            }

            return attributes;      

        } catch (SessionException se) {
            debug.error(debugMethod + "Error with the user's session.", se);
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
    protected boolean needToEscapeXMLSpecialCharacters(String hostEntityID, String remoteEntityID, String realm) {
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
         Set<String> values, String hostEntityID, String remoteEntityID, String realm) throws SAML2Exception {

        if (name == null) {
            throw new SAML2Exception(bundle.getString("nullInput"));
        }

        AssertionFactory factory = AssertionFactory.getInstance();
        Attribute attribute =  factory.createAttribute();

        attribute.setName(name);
        if (nameFormat != null) {
            attribute.setNameFormat(nameFormat);
        }
        if (values != null && !values.isEmpty()) {
            boolean toEscape = needToEscapeXMLSpecialCharacters(hostEntityID, remoteEntityID, realm);
            List<String> list = new ArrayList<String>();
            for (String value : values) {
                if (toEscape) {
                    list.add(XMLUtils.escapeSpecialCharacters(value));
                } else {
                    list.add(value);
                }
            }
            attribute.setAttributeValueString(list);
        }

        return attribute;
    }

    /**
     * Return true if ignore profile is enabled for this realm.
     *
     * @param session SSOToken to check the profile creation attributes.
     * @param realm realm to check the profile creation attributes.
     * @return true in all cases in this implementation.
     */
    protected boolean isIgnoredProfile(Object session, String realm) {
        return true;
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
        String debugMethod = "DefaultLibraryIDPAttributeMapper.getBinaryAttributeValues: ";

        // Expect to find the value in the binary Map
        if (binaryValueMap != null && !binaryValueMap.isEmpty()) {
            byte[][] values = binaryValueMap.get(localAttribute);
            if (values != null && values.length > 0) {
                // Base64 encode the binary values before they are added as an attribute value
                result = new HashSet<String>(values.length);
                for (byte[] value : values) {
                    result.add(Base64.encode(value));
                }
                debug.message(debugMethod + "adding {} as a binary for attribute named {}",
                        localAttribute, samlAttribute);
            } else {
                debug.message(debugMethod + "{} was flagged as a binary but no value was found",
                        localAttribute);
            }
        } else {
            debug.message(debugMethod + "{} was flagged as a binary but binary value map was empty or null",
                    localAttribute);
        }

        return result;
    }
}
