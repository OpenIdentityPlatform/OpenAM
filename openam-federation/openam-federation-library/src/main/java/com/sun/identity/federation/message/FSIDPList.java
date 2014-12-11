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
 * $Id: FSIDPList.java,v 1.2 2008/06/25 05:46:44 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.federation.message;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.common.IDPEntries;
import com.sun.identity.federation.message.common.GetComplete;
import com.sun.identity.federation.common.FSUtils;

import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class has methods to construct an object or
 * message representing a list of trusted Identity Providers.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSIDPList {
    private List getCompleteList = null;
    /**
     * <code>IDPEntries</code> object.
     */
    public IDPEntries idpEntries = null;
    private int minorVersion = IFSConstants.FF_11_PROTOCOL_MINOR_VERSION;
    
    /**
     * Default Constructor.
     */
    public FSIDPList() {
    }
    
    /**
     * Constructor creates <code>FSIDPList</code> object from
     * Document Element.
     *
     * @param root the Document Element object.
     * @throws FSMsgException if there is an error creating
     *         the object.
     */
    public FSIDPList(Element root) throws FSMsgException {
        if (root == null) {
            FSUtils.debug.message("FSIDPList.parseXML: null input.");
            throw new FSMsgException("nullInput",null);
        }
        String ns = root.getNamespaceURI();
        if (ns == null) {
            FSUtils.debug.error("FSIDPList(Element):No namespace");
            throw new FSMsgException("wrongInput",null);
        }
        
        if (ns.equals(IFSConstants.FF_12_XML_NS)) {
            minorVersion = IFSConstants.FF_12_PROTOCOL_MINOR_VERSION;
        }
        
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals(IFSConstants.IDP_LIST))) {
            FSUtils.debug.message("FSIDPList.parseXML: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals(IFSConstants.GET_COMPLETE)) {
                    if ((getCompleteList == null) ||
                            (getCompleteList == Collections.EMPTY_LIST)) {
                        getCompleteList = new ArrayList();
                    }
                    getCompleteList.add(new GetComplete((Element)child));
                }else if (childName.equals(IFSConstants.IDP_ENTRIES)) {
                    idpEntries = new IDPEntries((Element) child);
                }
            }
        }
    }
    
    /**
     * Constructor creates <code>FSIDPList</code> from <code>IDPEntries</code>
     * object and a list of <code>GetComplete</code> objects.
     *
     * @param idpEntries the <code>IDPEntries</code> object.
     * @param getCompleteList list of <code>GetComplete</code> objects.
     */
    public FSIDPList(IDPEntries idpEntries, List getCompleteList) {
        this.idpEntries = idpEntries;
        this.getCompleteList = getCompleteList;
    }
    
    /**
     * Sets the value of <code>MinorVersion</code> attribute.
     *
     * @param minorVersion the value of <code>MinorVersion</code> attribute
     *        in the assertion.
     * @see #setMinorVersion(int)
     */
    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }
    
    /**
     * Returns the value of <code>MinorVersion</code> attribute.
     *
     * @return the value of <code>MinorVersion</code> attribute.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Returns <code>IDPEntries</code> object.
     *
     * @return the <code>IDPEntries</code> object.
     * @see #setIDPEntries(IDPEntries)
     */
    public IDPEntries getIDPEntries() {
        return idpEntries;
    }
    
    /**
     * Returns list of <code>GetComplete</code> objects.
     *
     * @return list of <code>GetComplete</code> objects.
     * @see #setGetCompleteList(List)
     */
    public List getGetCompleteList() {
        return getCompleteList;
    }
    
    /**
     * Sets <code>IDPEntries</code> object.
     *
     * @param idpEntries <code>IDPEntries</code> object.
     * @see #getIDPEntries
     */
    public void setIDPEntries(IDPEntries idpEntries) {
        this.idpEntries = idpEntries;
    }
    
    /**
     * Sets list of <code>GetComplete</code> objects.
     *
     * @param getCompleteList list of <code>GetComplete</code> objects.
     * @see #setGetCompleteList(List)
     */
    public void setGetCompleteList(List getCompleteList) {
        this.getCompleteList = getCompleteList;
    }
    
    /**
     * Returns a <code>XML</code> string representation of this object.
     *
     * @return XML String representing this object.
     * @throws FSMsgException if there is an error creating
     *         the XML string or if the required elements to create
     *         the string do not conform to the schema.
     */
    
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    
    /**
     * Creates a String representation of this object.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return string containing the valid XML for this element.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Creates a String representation of this element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return A string containing the valid XML for this element.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString(boolean includeNS,boolean declareNS,
            boolean includeHeader) throws FSMsgException {
        
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append(IFSConstants.XML_PREFIX)
            .append(IFSConstants.DEFAULT_ENCODING)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.QUESTION_MARK)
            .append(IFSConstants.RIGHT_ANGLE);
        }
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uri = IFSConstants.LIB_NAMESPACE_STRING;
            }
        }
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefix)
        .append(IFSConstants.IDP_LIST)
        .append(uri)
        .append(IFSConstants.RIGHT_ANGLE);
        
        if (idpEntries != null){
            xml.append(idpEntries.toXMLString(true, false));
        }
        
        if ((this.getCompleteList != null) &&
                (getCompleteList != Collections.EMPTY_LIST)){
            Iterator i = getCompleteList.iterator();
            while (i.hasNext()) {
                xml.append((String)i.next());
            }
        }
        xml.append(IFSConstants.START_END_ELEMENT)
        .append(prefix)
        .append(IFSConstants.IDP_LIST)
        .append(IFSConstants.RIGHT_ANGLE);
        
        return xml.toString();
    }
}
