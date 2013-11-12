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
 * $Id: SubjectConfirmationDataImpl.java,v 1.5 2008/11/10 22:57:01 veiming Exp $
 *
 */


package com.sun.identity.saml2.assertion.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.text.ParseException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;


/**
 *  The <code>SubjectConfirmationData</code> specifies additional data
 *  that allows the subject to be confirmed or constrains the circumstances
 *  under which the act of subject confirmation can take place. Subejct
 *  confirmation takes place when a relying party seeks to verify the
 *  relationship between an entity presenting the assertion and the 
 *  subject of the assertion's claims.
 */
public class SubjectConfirmationDataImpl implements SubjectConfirmationData {
    public final String elementName = "SubjectConfirmationData";
    protected boolean mutable = false;
    private Date notOnOrAfter = null;
    private String inResponseTo = null;
    private List content = null;
    private String recipient = null;
    private Date notBefore = null;
    private String address = null;
    private String contentType = null;

    public SubjectConfirmationDataImpl() {
        mutable = true;
    }

    public SubjectConfirmationDataImpl(Element element) throws SAML2Exception {
            parseElement(element);
        makeImmutable();
    }

    public SubjectConfirmationDataImpl(String xmlString) throws SAML2Exception {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception("");
        }
        parseElement(doc.getDocumentElement());
        makeImmutable();
    }

    private void parseElement(Element element) throws SAML2Exception {
        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("parseElement: "
                    + "Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        
        // Make sure this is an SubjectConfirmationData.
        String tag = element.getLocalName();
        if ((tag == null) || (!tag.equals(elementName))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("parseElement: "
                    + "not SubjectConfirmationData.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attributes of <SubjectConfirmationData> element
        NamedNodeMap attrs = ((Node)element).getAttributes();
        parseAttributes(attrs);
        parseContent(element);
    }
    
    /**
     *  Parse and sets content values 
     *
     *  @param element Element for this class object 
     */
    protected void parseContent(Element element) 
    throws SAML2Exception
    {
        if (element == null) {
            return;
        }
        // Parses the Content Element.
        NodeList nList = element.getChildNodes();
        if ((nList != null) && (nList.getLength() > 0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                if (childNode.getLocalName() != null) {
                    getContent().add(childNode);
                }
            }
        }

        if (content == null) {
            content = new ArrayList();
            content.add(XMLUtils.getElementValue(element));
        }
    }

    /**
     *  Sets all the attribute values 
     *
     *  @param attrs Map table has attribute name and value pairs 
     */
    protected void parseAttributes(NamedNodeMap attrs) 
    throws SAML2Exception
    {
        if (attrs == null) {
            return;
        }

        try {
            int length = attrs.getLength();
            for (int i = 0; i < length; i++) {
                Attr attr = (Attr) attrs.item(i);
                String attrName = attr.getName().trim();
                String attrValue = attr.getValue().trim();
                if (attrName.equals("Address")) {
                    address = attrValue;
                } else if (attrName.equals("InResponseTo")) {
                    inResponseTo = attrValue;
                } else if (attrName.equals("NotBefore")) {
                    notBefore = DateUtils.stringToDate(attrValue);
                } else if (attrName.equals("NotOnOrAfter")) {
                    notOnOrAfter = DateUtils.stringToDate(attrValue);
                } else if (attrName.equals("Recipient")) {
                    recipient = attrValue;
                } else if (attrName.equals("xsi:type")) {
                    contentType = attrValue;    
                } else {
                    continue;
                }
            }
        } catch (ParseException e) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("parseAttributes: " + e.toString());
            }

            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }
    }
    
    /**
     * Returns the time instant at which the subject can no longer be
     * confirmed
     *
     * @return the time instant at which the subject can no longer be
     *  confirmed
     * @see #setNotOnOrAfter(Date)
     */
    public Date getNotOnOrAfter() {
            return notOnOrAfter;
    }

    /**
     * Sets the time instant at which the subject can no longer be
     * confirmed
     *
     * @param value the time instant at which the subject can no longer be
     * confirmed
     * @exception SAML2Exception if the object is immutable
     * @see #getNotOnOrAfter
     */
    public void setNotOnOrAfter(Date value) throws SAML2Exception
    {
        if (!mutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        notOnOrAfter = value;
    }
    

    /**
     *  Returns the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     *
     *  @return the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     *  @see #setInResponseTo(String)
     */
    public String getInResponseTo()
    {
        return inResponseTo;
    }

    /**
     *  Sets the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     *
     *  @param value the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     *  @exception SAML2Exception if the object is immutable
     *  @see #getInResponseTo
     */
    public void setInResponseTo(String value) throws SAML2Exception
    {
        if (!mutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        inResponseTo = value;
    }

    /**
     * Returns a list of arbitrary XML elements to be added to this 
     * <code>SubejctConfirmationData</code> object.
     *
     * @return a list of arbitrary XML elements to be added to this 
     * <code>SubejctConfirmationData</code> object.
     * @see #setContent(List)
     */
    public List getContent()
    {
        if (content == null) {
            content = new ArrayList();
        }
        return content;
    }

    /**
     * Sets a list of arbitrary XML elements to be added to this 
     * <code>SubejctConfirmationData</code> object.
     *
     * @param value a list of arbitrary XML elements to be added to this 
     * <code>SubejctConfirmationData</code> object.
     * @exception SAML2Exception if the object is immutable
     * @see #getContent()
     */
    public void setContent(List value) throws SAML2Exception
    {
        if (!mutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        content = value;
    }

    /**
     *  Returns the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     *
     *  @return the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     *  @see #setRecipient(String)
     */
    public String getRecipient()
    {
        return recipient;
    }

    /**
     *  Sets the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     *
     *  @param value the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     *  @exception SAML2Exception if the object is immutable
     *  @see #getRecipient
     */
    public void setRecipient(String value) throws SAML2Exception
    {
        if (!mutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        recipient = value;
    }

    /**
     *  Returns the time instant before which the subject cannot be confirmed
     *
     *  @return the time instant before which the subject cannot be confirmed
     *  @see #setNotBefore(Date)
     */
    public Date getNotBefore()
    {
        return notBefore;
    }

    /**
     *  Sets the time instant before which the subject cannot be confirmed
     *
     *  @param value the time instant before which the subject cannot
     *         be confirmed
     *  @exception SAML2Exception if the object is immutable
     *  @see #getNotBefore
     */
    public void setNotBefore(Date value) throws SAML2Exception
    {
        if (!mutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        notBefore = value;
    }

    /**
     *  Returns the network address/location from which an attesting 
     *  entity can present the assertion 
     *
     *  @return the network address/location from which an attesting 
     *  entity can present the assertion 
     *  @see #setAddress(String)
     */
    public String getAddress()
    {
        return address;
    }

    /**
     *  Sets the network address/location from which an attesting 
     *  entity can present the assertion 
     *
     *  @param value the network address/location from which an attesting 
     *  entity can present the assertion 
     *  @exception SAML2Exception if the object is immutable
     *  @see #getAddress
     */
    public void setAddress(String value) throws SAML2Exception
    {
        if (!mutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        address = value;
    }
    
    /**
     *  Returns the content type attribute     
     *
     *  @return the content type attribute     
     *  @see #setContentType(String)
     */
    public String getContentType()
    {
        return contentType;
    }
    
    /**
     *  Sets the content type attribute.
     *
     *  @param attribute attribute type value for the content that will be 
     *         added
     *  @throws SAML2Exception if the object is immutable
     */
    public void setContentType(String attribute) throws SAML2Exception {
        if (!mutable) {
           throw new SAML2Exception(
                    SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        contentType = attribute;
    }

    /**
     * Returns a String representation of the element
     *
     * @return A String representation
     * @exception SAML2Exception if something is wrong during conversion
     */
     public String toXMLString() throws SAML2Exception
     {
         return toXMLString(true, false);
     }

    /**
     * Returns a String representation of the element
     * @param includeNSPrefix Determines whether the namespace qualifier is
     *        prepended to the Element when converted
     * @param declareNS Determines whether the namespace is declared
     *        within the Element.
     * @return A String representation
     * @exception SAML2Exception if something is wrong during conversion
     */
     public String toXMLString(boolean includeNSPrefix, boolean declareNS)
     throws SAML2Exception
     {
        StringBuffer xml = new StringBuffer();

        String NS="";
        String appendNS="";
            
        if (declareNS) {
            NS = SAML2Constants.ASSERTION_DECLARE_STR;
        }
        if (includeNSPrefix) {
            appendNS = SAML2Constants.ASSERTION_PREFIX;
        }

        xml.append("<").append(appendNS).append(elementName);
        xml.append(NS).append(" ");
        xml.append(getElementValue(includeNSPrefix, declareNS));
        xml.append("</").append(appendNS).append(elementName).append(">");

        return xml.toString();    
     }
    
    /**
     * Returns a String representation of the element value
     * @param includeNSPrefix Determines whether the namespace qualifier is
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A String representation
     * @exception SAML2Exception if something is wrong during conversion
     */
    protected String getElementValue(boolean includeNSPrefix, boolean declareNS)
     throws SAML2Exception
    {
        StringBuffer xml = new StringBuffer();

        if (notOnOrAfter != null) {
            xml.append("NotOnOrAfter=\"");
            xml.append(DateUtils.toUTCDateFormat(notOnOrAfter));        
            xml.append("\" ");        
        }
        
        if (inResponseTo != null) {
            xml.append("InResponseTo=\"");
            xml.append(inResponseTo);        
            xml.append("\" ");        
        }
        
        if (recipient != null) {
            xml.append("Recipient=\"");
            xml.append(recipient);        
            xml.append("\" ");        
        }
        
        if (notBefore != null) {
            xml.append("NotBefore=\"");
            xml.append(DateUtils.toUTCDateFormat(notBefore));        
            xml.append("\" ");        
        }
        
        if (address != null) {
            xml.append("Address=\"");
            xml.append(address);        
            xml.append("\" ");        
        }
        
        if(contentType != null) {
            xml.append(SAML2Constants.XSI_DECLARE_STR).append(" ")
               .append("xsi:type=\"")
               .append(contentType)
               .append("\" ");
        }
        
        xml.append(">");
        
        if (!getContent().isEmpty()) {
            Iterator it = getContent().iterator();
            while (it.hasNext()){
                Object obj = it.next();
                if(obj instanceof Element) {
                   xml.append(XMLUtils.print((Element)obj)).append(" ");                   
                } else if(obj instanceof String) {
                    xml.append((String)obj);
                }
            }
        }
                
        return xml.toString();    
     }

   /**
    * Makes the object immutable
    */
    public void makeImmutable()
    {
        if (!mutable) {
            return;
        }

        mutable = false;

        if ((content != null) && (!content.isEmpty())) {
            content = Collections.unmodifiableList(content);
        }
    }

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable()
    {
        return mutable;
    }

}
