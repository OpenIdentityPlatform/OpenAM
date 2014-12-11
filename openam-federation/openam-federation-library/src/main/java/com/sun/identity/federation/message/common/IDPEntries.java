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
 * $Id: IDPEntries.java,v 1.2 2008/06/25 05:46:47 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */


package com.sun.identity.federation.message.common;

import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;

/**
 * This class defines methods to set/retrieve multiple Identity Providers.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class IDPEntries {
    private List idpEntryList = null;
    private List otherElements = null;
    
    /**
     * Default Constructor.
     */
    public IDPEntries() {
    }
    
    
    /**
     * Constructor creates <code>IDPEntries</code> object.
     *
     * @param idpEntries list of identity providers.
     */
    public IDPEntries(List idpEntries) {
        this.idpEntryList = idpEntries;
    }
    
    /**
     * Returns the list of Identity Providers.
     *
     * @return  list of Identity Providers.
     * @see #setIDPEntryList(List)
     */
    public List getIDPEntryList() {
        return idpEntryList;
    }
    
    /**
     * Sets the list of Identity Providers.
     *
     * @param idpEntryList the list of Identity Providers.
     * @see #getIDPEntryList
     */
    public void setIDPEntryList(List idpEntryList) {
        this.idpEntryList = idpEntryList;
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
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    
    public String toXMLString(boolean includeNS, boolean declareNS)
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
        
        xml.append("<").append(prefix).append("IDPEntries").append(uri).
                append(">\n");
        
        
        if((idpEntryList != null) && (idpEntryList != Collections.EMPTY_LIST)){
            Iterator i = idpEntryList.iterator();
            
            while (i.hasNext()) {
                IDPEntry entry = (IDPEntry)i.next();
                xml.append(entry.toXMLString(true, false));
            }
        }
        xml.append("</").append(prefix).append("IDPEntries>\n");
        
        return xml.toString();
    }
    /**
     * Constructor creates <code>IDPEntries</code> object from
     * a Document Element.
     *
     * @param root the Document Element object.
     * @throws FSMsgException on error.
     */
    public IDPEntries(Element root) throws FSMsgException {
        if (root == null) {
            SAMLUtils.debug.message("IDPEntries.parseXML: null input.");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("IDPEntries"))) {
            FSUtils.debug.message("IDPEntries.parseXML: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("IDPEntry")) {
                    if ((idpEntryList == null) ||
                            (idpEntryList == Collections.EMPTY_LIST)) {
                        idpEntryList = new ArrayList();
                    }
                    idpEntryList.add(new IDPEntry((Element)child));
                }else{
                }
            }
        }
    }
}
