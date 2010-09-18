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
 * $Id: IDPListImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.GetComplete;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class specifies the identity providers trusted by the requester
 * to authenticate the presenter.
 */

public class IDPListImpl implements IDPList {
    
    private List idpEntryList = null;
    private GetComplete getComplete;
    private boolean isMutable=false;
    
    /**
     * Constructor creates the <code>IDPList</code> Object.
     */
    
    public IDPListImpl() {
        isMutable=true;
    }
    
    /**
     * Constructor to create the <code>IDPList</code> Object.
     *
     * @param element Document Element of <code>IDPList</code> Object.
     * @throws SAML2Exception if <code>IDPList<code> cannot be created.
     */
    
    public IDPListImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }
    
    /**
     * Constructor to create the <code>IDPList</code> Object.
     *
     * @param xmlString the XML String Representation of 
     *        <code>IDPList</code> Object.
     * @throws SAML2Exception if <code>IDPList<code> cannot be created.
     */
    
    public IDPListImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
                XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }
    
    /**
     * Returns the list of <code>IDPEntry</code> Objects.
     *
     * @return the list of <code>IDPEntry</code> objects.
     * @see #setIDPEntries(List)
     */
    public List getIDPEntries() {
        return idpEntryList ;
    }
    
    /**
     * Sets the list of <code>IDPEntry</code> Objects.
     *
     * @param idpEntryList list of <code>IDPEntry</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIDPEntries
     */
    public void setIDPEntries(List idpEntryList) throws SAML2Exception {
        if (isMutable) {
            this.idpEntryList = idpEntryList;
        } else {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the <code>GetComplete</code> Object.
     *
     * @return the <code>GetComplete</code> object.
     * @see #setGetComplete(GetComplete)
     */
    public GetComplete getGetComplete() {
        return getComplete;
    }
    
    /**
     * Sets the <code>GetComplete<code> Object.
     *
     * @param getComplete the new <code>GetComplete</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getGetComplete
     */
    
    public void setGetComplete(GetComplete getComplete) throws SAML2Exception {
        if (isMutable) {
            this.getComplete = getComplete;
        } else {
            throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns a String representation of this Object.
     *
     * @return a String representation of this Object.
     * @throws SAML2Exception cannot create String object.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }
    
    /**
     * Returns a String representation of this Object.
     *
     * @param includeNSPrefix determines whether or not the namespace
     *        qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *        within the Element.
     * @return the String representation of this Object.
     * @throws SAML2Exception cannot create String object.
     **/
    
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
    throws SAML2Exception {
        validateIDPEntryList(idpEntryList);
        StringBuffer xmlString = new StringBuffer(150);
        xmlString.append(SAML2Constants.START_TAG);
        if (includeNSPrefix) {
            xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
        }
        xmlString.append(SAML2Constants.IDPLIST).append(SAML2Constants.SPACE);
        
        if (declareNS) {
            xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR);
        }
        xmlString.append(SAML2Constants.END_TAG)
        .append(SAML2Constants.NEWLINE);
        
        if ((idpEntryList == null) || (idpEntryList.isEmpty())) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noIDPEntry"));
        }
        
        Iterator i = idpEntryList.iterator();
        while (i.hasNext()) {
            IDPEntry idpEntry = (IDPEntry)i.next();
            if (idpEntry != null) {
                String idpEntryStr =
                        idpEntry.toXMLString(includeNSPrefix,declareNS);
                xmlString.append(idpEntryStr).append(SAML2Constants.NEWLINE);
            }
        }
        if (getComplete != null) {
            xmlString.append(getComplete.toXMLString(includeNSPrefix,declareNS))
            .append(SAML2Constants.NEWLINE);
        }
        
        xmlString.append(SAML2Constants.SAML2_END_TAG)
        .append(SAML2Constants.IDPLIST)
        .append(SAML2Constants.END_TAG);
        
        
        return xmlString.toString();
    }
    
    
    /**
     * Makes this object immutable.
     */
    public void makeImmutable()  {
	if (isMutable) {
	    if ((idpEntryList != null) && (!idpEntryList.isEmpty())) {
		Iterator i = idpEntryList.iterator();
		while (i.hasNext()) {
		    IDPEntry idpEntry = (IDPEntry) i.next();
		    if ((idpEntry != null) && (idpEntry.isMutable())) {
			idpEntry.makeImmutable();
		    }
		}
	    }
        
	    if ((getComplete != null) && (getComplete.isMutable())) {
		getComplete.makeImmutable();
	    }
            isMutable=false;
	}
    }
    
    /**
     * Returns true if object is mutable.
     *
     * @return true if the object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }
    
    /* Parse the IDPList Element */
    void parseElement(Element element) throws SAML2Exception {
        
        ProtocolFactory protoFactory = ProtocolFactory.getInstance();
        
        // Get the IDPEntry Element, can be 1 or more
	NodeList nList = element.getChildNodes();

        if ((nList == null) || (nList.getLength()==0)) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noIDPEntry"));
        }
	if (idpEntryList == null) {
	    idpEntryList = new ArrayList();
	}
	for (int i = 0; i < nList.getLength(); i++) {
            Node childNode = nList.item(i);
	    String cName = childNode.getLocalName();
            if (cName != null)  {
		if (cName.equals(SAML2Constants.IDPENTRY)) {
		    validateIDPEntry();
		    idpEntryList.add(
		        protoFactory.createIDPEntry(XMLUtils.print(childNode)));
                } else if (cName.equals(SAML2Constants.GETCOMPLETE)) { 
			validateGetComplete();
                	Element getCompleteElement = (Element)childNode;
			getComplete = 
		    	   protoFactory.createGetComplete(getCompleteElement);
		}
	    }
	}
	validateIDPEntryList(idpEntryList);
	idpEntryList=Collections.unmodifiableList(idpEntryList);
    }
    
    /* Validates the existance of IDPEntries */
    private void validateIDPEntryList(List idpEntryList) throws SAML2Exception {
        if ((idpEntryList == null) || (idpEntryList.isEmpty())) {
            SAML2SDKUtils.debug.message("IDPEntry Object is required");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noIDPEntry"));
        }
    }

    /* Validates the IDPEntry sequence */
    private void validateIDPEntry() throws SAML2Exception {
	if (getComplete != null) {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
		SAML2SDKUtils.debug.message("IDPList Element should be the " 
				     + "first element" );
	    }
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidProxyCount"));
        }
    }
       
    /* Validate the existance of GetComplete Object. */
    private void validateGetComplete() throws SAML2Exception {
	if (getComplete != null) {
            SAML2SDKUtils.debug.message("Too may GetComplete Elements");
            throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }
}
