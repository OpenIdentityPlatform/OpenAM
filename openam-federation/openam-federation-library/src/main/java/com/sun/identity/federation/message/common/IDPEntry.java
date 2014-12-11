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
 * $Id: IDPEntry.java,v 1.2 2008/06/25 05:46:47 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */


package com.sun.identity.federation.message.common;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;

/**
 * This class defines methods to set/retrieve single identity provider
 * information trusted by the request issuer to authenticate the presenter.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class IDPEntry {
    public static final int LIB_TYPE_IDP = 0;
    public static final int LIB_TYPE_BROKER = 1;
    
    private String providerID = null;
    private String providerName = null;
    private String location = null;
    
    /**
     * Default Constructor.
     */
    public IDPEntry() {
    }
    
    /**
     * Constructor creates <code>IDPEntry</code> object.
     *
     * @param providerID the Identity Provider identifier.
     * @param providerName the Identity Provider name.
     * @param location
     */
    public IDPEntry(String providerID,String providerName,String location) {
        this.providerID = providerID;
        this.providerName = providerName;
        this.location = location;
    }
    
    /**
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return the value of <code>ProviderID</code> attribute.
     * @see #setProviderID(String)
     */
    
    public String getProviderID() {
        return providerID;
    }
    
    /**
     * Sets the value of <code>ProviderID</code> attribute.
     *
     * @param providerID the value of <code>ProviderID</code> attribute.
     * @see #getProviderID
     */
    public void setProviderID(String providerID) {
        this.providerID = providerID;
    }
    
    /**
     * Returns the Identity Provider Name.
     *
     * @return the Identity Provider Name.
     * @see #setProviderName(String)
     */
    public String getProviderName() {
        return providerName;
    }
    
    /**
     * Sets the Identity Provider Name.
     *
     * @param providerName the Identity Provider Name.
     * @see #getProviderName
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    /**
     * Returns the location URI of the Identity Provider.
     *
     * @return the location URI of the Identity Provider.
     * @see #setLocation(String)
     */
    public String getLocation() {
        return location;
    }
    
    /**
     * Sets the location URI of the Identity Provider.
     *
     * @param location the location URI of the Identity Provider.
     * @see #getLocation
     */
    public void setLocation(String location) {
        this.location = location;
    }
    
    /**
     * Returns the string representation of this object.
     * This method translates the response to an XML document string.
     *
     * @return An XML String representing the response. NOTE: this is a
     *         complete SAML response xml string with ResponseID,
     *         MajorVersion, etc.
     */
    
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    
    /**
     * Returns a String representation of this object.
     *
     * @param includeNS  Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    
    public String toXMLString(boolean includeNS,boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    /**
     * Returns a String representation of this object.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    
    public String toXMLString(boolean includeNS,boolean declareNS,
            boolean includeHeader) throws FSMsgException {
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
                    append(IFSConstants.DEFAULT_ENCODING).append("\" ?>\n");
        }
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            uri = IFSConstants.LIB_NAMESPACE_STRING;
        }
        
        xml.append("<").append(prefix).append("IDPEntry").append(uri).
                append(">\n");
        
        if(providerID != null){
            xml.append("<").append(prefix).append("ProviderID").append(">")
                    .append(providerID)
                    .append("</").append(prefix).append("ProviderID")
                    .append(">\n");
        }
        
        if(providerName != null){
            xml.append("<").append(prefix).append("ProviderName").append(">")
                    .append(providerName)
                    .append("</").append(prefix).append("ProviderName")
                    .append(">\n");
        }
        
        if(location != null){
            xml.append("<").append(prefix).append("Loc").append(">").
                    append(location).
                    append("</").append(prefix).append("Loc").append(">\n");
        }
        
        xml.append("</").append(prefix).append("IDPEntry").append(">\n");
        
        return xml.toString();
    }
    
    /**
     * Constructor creates <code>IDPEntry</code> Object from
     * Document Element.
     *
     * @param root Document Element of <code>IDPEntry<code> object.
     * @throws FSMsgException if <code>IDPEntry<code> cannot be created.
     */
    
    public IDPEntry(Element root) throws FSMsgException {
        if (root == null) {
            SAMLUtils.debug.message("IDPEntry.parseXML: null input.");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("IDPEntry"))) {
            FSUtils.debug.message("IDPEntry.parseXML: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        NodeList nl = root.getChildNodes();
        Node child;
        String nodeName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals("ProviderID")) {
                    if (providerID != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("IDPEntry(Element): should"
                                    + "contain only one ProviderID");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    providerID = XMLUtils.getElementValue((Element) child);
                } else if (nodeName.equals("ProviderName")) {
                    if (providerName != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("IDPEntry(Element): should"
                                    + "contain only one ProviderName");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    providerName = XMLUtils.getElementValue((Element) child);
                } else if (nodeName.equals("Loc")) {
                    if (location != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("IDPEntry(Element): should"
                                    + "contain only one Loc");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    location = XMLUtils.getElementValue((Element) child);
                }
            }
        }
    }
}
