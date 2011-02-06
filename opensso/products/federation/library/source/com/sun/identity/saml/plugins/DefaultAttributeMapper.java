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
 * $Id: DefaultAttributeMapper.java,v 1.4 2009/09/22 23:04:36 exu Exp $
 *
 */


package com.sun.identity.saml.plugins;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AttributeDesignator;
import com.sun.identity.saml.assertion.SubjectConfirmation;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLServiceManager;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.saml.protocol.AttributeQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * The class <code>DefaultAttributeMapper</code> provide a default
 * implementation of the <code>AttributeMapper</code> interface. 
 */
public class DefaultAttributeMapper implements AttributeMapper {

    /**
     * Default Constructor
     */
    public DefaultAttributeMapper() {}

    /**
     * This method exams the SubjectConfirmation of the Subject in the
     * AttributeQuery. If it has only one ConfirmationMethod, and this
     * ConfirmationMethod equals to "urn:com:sun:identity"; and its
     * SubjectConfirmationData contains TEXT node only, then the method
     * returns the concatenated string of all the TEXT nodes. Otherwise,
     * it returns null.
     * <p>
     * @param query the <code>AttributeQuery</code> object.
     * @see com.sun.identity.saml.plugins.AttributeMapper#getSSOTokenID
     */
    public String getSSOTokenID(AttributeQuery query) {
        if (query == null) {
            return null;
        }
        SubjectConfirmation sc = query.getSubject().getSubjectConfirmation();
        if (sc == null) {
            return null;
        }

        if (!SAMLUtils.isCorrectConfirmationMethod(sc)) {
            return null;
        }

        Element scData = sc.getSubjectConfirmationData();
        return XMLUtils.getElementString(scData);
    }

    /**
     * This method exams the SubjectConfirmationData of the Subject in the
     * AttributeQuery. It returns the first Assertion that contains at least
     * one AuthenticationStatement.
     * <p>
     * @see com.sun.identity.saml.plugins.AttributeMapper#getSSOAssertion
     */
    public Assertion getSSOAssertion(AttributeQuery query) {
        if (query == null) {
            return null;
        }
        SubjectConfirmation sc = query.getSubject().getSubjectConfirmation();
        if (sc == null) {
            return null;
        }
        Element scData = sc.getSubjectConfirmationData();
        if (scData == null) {
            return null;
        }
        Assertion assertion = null;
        try {
            NodeList nl = scData.getChildNodes();
            Node child = null;
            for (int i = 0, length = nl.getLength(); i < length; i++) {
                child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE ) {
                    try {
                        assertion = new Assertion((Element) child);
                        if (SAMLUtils.isAuthNAssertion(assertion)) {
                            return assertion;
                        }
                    } catch (SAMLException se) {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("DefaultAttributeMapper: "
                            + "SAMLException when trying to obtain Assertion:"
                            + se);
                        }
                    }
                }
            }
        } catch (Exception e) {
            SAMLUtils.debug.error("DefaultAttributeMapper: Exception when "
                + "parsing the SubjectConfirmationData:", e);
        }
        return null;
    }

    /**
     * This method first mapps the Subject in the query to a local site
     * account using the AccountMapper defined in the SAML Service.
     * The source ID is used to find the appropriate AccountMapper.
     * It then calls the User Management API to obtain the attribute value
     * using the Session and the attribute name in the AttributeDesignator(s)
     * of the query. If there is no AttributeDesignator in the query,
     * attributes of services specified as userServiceNameList in
     * amSAML.properties will be returned.
     * <p>
     *
     * @param query the <code>AttributeQuery</code> object.
     * @param sourceID the Source Identifier.
     * @param token  User Session
     * @throws SAMLException if there is an error.
     */
    public List getAttributes(AttributeQuery query, String sourceID,
        Object token) throws SAMLException {
        if ((query == null) || (sourceID == null) || (token == null)) {
            SAMLUtils.debug.message("DefaultAttributeMapper: null input.");
            throw new SAMLException(SAMLUtils.bundle.getString("nullInput"));
        }

        Map entries = (Map) SAMLServiceManager.getAttribute(
                                                SAMLConstants.PARTNER_URLS);
        SAMLServiceManager.SOAPEntry destSite = (SAMLServiceManager.SOAPEntry)
                                                entries.get(sourceID);

        String name = null;
        PartnerAccountMapper paMapper = destSite.getPartnerAccountMapper();
        if (paMapper != null) {
            Map map = paMapper.getUser(query, sourceID);
            name = (String) map.get(PartnerAccountMapper.NAME);
        }

        if (name == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("DefaultAttributeMapper: couldn't "
                        + "map the subject to a local user.");
            }
            throw new SAMLException(
                        SAMLUtils.bundle.getString("cannotMapSubject"));
        }
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("user=" + name);
        }
        // assume user in default root realm
        DataStoreProvider provider = null;
        try {
            provider = DataStoreProviderManager.getInstance().
                getDataStoreProvider(SAMLConstants.SAML);
        } catch (DataStoreProviderException de) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("DefaultAttributeMapper.getAttribute:",
                    de);
            }
            throw new SAMLException(
                        SAMLUtils.bundle.getString("cannotMapSubject"));
        }

        List attributes = new ArrayList();
        Attribute attribute = null;
        List attrValues = null;
        String attrValueString = null;
        String attrName = null;
        Set valueSet = null;
        Iterator valueIter = null;
        List designators = query.getAttributeDesignator();
        if ((designators == null) || (designators.isEmpty())) {
            String userAttrName = SystemConfigurationUtil.getProperty(
                "userAttributeNameList");           
            if ((userAttrName == null ) || (userAttrName.length() == 0)) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("DefaultAttributeMapper: "
                        + "userAttributeNameList is not defined " 
                        + "or empty.");
                }
                return attributes;
            }
            Set attrNames = new HashSet(); 
            StringTokenizer stk = new StringTokenizer(userAttrName, ",");
            while (stk.hasMoreTokens()) {
                attrNames.add(stk.nextToken().trim());
            }
            Map valueMap = null; 
            try {
                valueMap = provider.getAttributes(name, attrNames);
            } catch (DataStoreProviderException ie) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("DefaultAttributeMapper: "
                        + "DataStoreProviderException:", ie);
                }
                throw new SAMLException(ie.getMessage());
            }
            Set keySet = valueMap.keySet();
            String keyName = null;
            Iterator keyIter = keySet.iterator();
            while (keyIter.hasNext()) {
                keyName = (String) keyIter.next();
                valueSet = (Set) valueMap.get(keyName);
                valueIter = valueSet.iterator();
                attrValues = new ArrayList();
                while (valueIter.hasNext()) {
                    attrValueString = SAMLUtils.makeStartElementTagXML(
                        "AttributeValue", true, true)
                        + ((String) valueIter.next())
                        + SAMLUtils.makeEndElementTagXML(
                        "AttributeValue", true);
                    attrValues.add(XMLUtils.toDOMDocument(attrValueString,
                        SAMLUtils.debug).getDocumentElement());
                }
                if (!attrValues.isEmpty()) {
                    attribute = new Attribute(keyName, 
                        SAMLConstants.ATTR_NAME_SPACE,
                        attrValues);
                    attributes.add(attribute);
                }
            }
        } else {
            Iterator iter = designators.iterator();
            AttributeDesignator designator = null;
            while (iter.hasNext()) {
                designator = (AttributeDesignator) iter.next();
                attrName = (String) designator.getAttributeName();
                try {
                    valueSet = provider.getAttribute(name, attrName);
                } catch (DataStoreProviderException ie) {
                    if (SAMLUtils.debug.messageEnabled()) {
                        SAMLUtils.debug.message("DefaultAttributeMapper: "
                            + "DataStoreProviderException:", ie);
                    }
                    throw new SAMLException(ie.getMessage());
                }

                valueIter = valueSet.iterator();
                attrValues = new ArrayList();
                while (valueIter.hasNext()) {
                    attrValueString = SAMLUtils.makeStartElementTagXML(
                        "AttributeValue", true, true)
                        + ((String) valueIter.next())
                        + SAMLUtils.makeEndElementTagXML("AttributeValue",true);
                    attrValues.add(XMLUtils.toDOMDocument(attrValueString,
                                        SAMLUtils.debug).getDocumentElement());
                }
                if (!attrValues.isEmpty()) {
                    attribute = new Attribute(attrName,
                        designator.getAttributeNamespace(), attrValues);
                    attributes.add(attribute);
                }
            }
        }
        return attributes;
    }
}
