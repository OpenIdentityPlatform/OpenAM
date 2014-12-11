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
 * $Id: FSAuthnResponseEnvelope.java,v 1.2 2008/06/25 05:46:43 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This  class defines methods for setting and retrieving attributes and
 * elements associated with a Liberty Response . 
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class FSAuthnResponseEnvelope {
   private List otherElements;
   private FSAuthnResponse authnResponse;
   private String assertionConsumerServiceURL = null;
   private int minorVersion = IFSConstants.FF_11_PROTOCOL_MINOR_VERSION;
   
   /**
    * Default Constructor.
    */
    public FSAuthnResponseEnvelope() {    
    }
   
  /**
   * Constructor create <code>FSAuthnResponseEnvelope</code> object.
   *
   * @param authnResponse the <code>FSAuthnResponse</code> object.
   */
   public FSAuthnResponseEnvelope(FSAuthnResponse authnResponse) {    
       this.authnResponse = authnResponse;
       this.otherElements = null;
   }
   
  /**
   * Constructor create <code>FSAuthnResponseEnvelope</code> object.
   *
   * @param root the Document element .
   * @throws FSMsgException if there is an error creating the object.
   * @throws SAMLException if there is an error creating the object.
   */
   public FSAuthnResponseEnvelope(Element root) 
                    throws FSMsgException, SAMLException {    
        if (root == null) {
            FSUtils.debug.message("FSAuthnResponseEnvelope.parseXML: "
                                  + "null input.");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                        (!tag.equals(IFSConstants.AUTHN_RESPONSE_ENVELOPE))) {
            FSUtils.debug.message("FSAuthnResponseEnvelope.parseXML: "
                                  + "wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        String ns = root.getNamespaceURI();
        if (ns == null) {
           FSUtils.debug.error("FSAuthnResponseEnvelope(Element):"
                               + "No namespace");
           throw new FSMsgException("wrongInput",null);
        }

        if (ns.equals(IFSConstants.FF_12_XML_NS)) {
           minorVersion = IFSConstants.FF_12_PROTOCOL_MINOR_VERSION;
        }
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals(IFSConstants.AUTHN_RESPONSE)) {
                    if (authnResponse != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAuthnResponseEnvelope: "
                                + "included more than one <AuthnResponse>");
                        }
                        throw new FSMsgException("moreElement",null);
                    }
                    authnResponse = new FSAuthnResponse((Element) child);
                } else if (childName.equals(
                              IFSConstants.ASSERTION_CONSUMER_SERVICE_URL)) {
                    assertionConsumerServiceURL = 
                        XMLUtils.getElementValue((Element) child);                    
                } 
            } 
       }
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
   * Sets the value of <code>MinorVersion<code> attribute.
   *
   * @param minorVersion the <code>MinorVersion</code> attribute.
   */
   public void setMinorVersion(int minorVersion) {
       this.minorVersion = minorVersion;
   }

   /**
    * Returns a list of elements.
    *
    * @return list of elements.
    * @see #setOtherElements(List)
    */
    public List getOtherElements() {
        return otherElements;
    }
  /**
   * Sets a list of elements.
   *
   * @param otherElement a list of elements.
   * @see #getOtherElements
   */
   public void setOtherElements(List otherElement) {
       this.otherElements = otherElement;
   }
   
   /**
    * Returns the <code>FSAuthnResponse</code> object.
    *
    * @return the <code>FSAuthnResponse</code> object.
    * @see #setAuthnResponse(FSAuthnResponse)
    */

   public FSAuthnResponse getAuthnResponse() {
       return authnResponse;
   }
   
   /**
    * Sets the <code>FSAuthnResponse</code> object.
    *
    * @param authnResponse the <code>FSAuthnResponse</code> object.
    * @see #getAuthnResponse
    */

   public void setAuthnResponse(FSAuthnResponse authnResponse) {
       this.authnResponse = authnResponse;
   }
    
   /**
    * Returns the Assertion Consumer Service URL.
    *
    * @return the  Assertion Consumer Service URL.
    * @see #setAssertionConsumerServiceURL(String)
    */
    public String getAssertionConsumerServiceURL() {
       return assertionConsumerServiceURL;
   }
   /**
    * Sets the Assertion Consumer Service URL.
    *
    * @param assertionConsumerUrl the Assertion Consumer Service Identifier.
    * @see #getAssertionConsumerServiceURL
    */
    public void setAssertionConsumerServiceURL(String assertionConsumerUrl) {
        this.assertionConsumerServiceURL = assertionConsumerUrl;
   }
    
   /**
    * Returns the <code>FSAuthnResponseEnvelope</code> object.
    *
    * @param xml the XML string to create this object from
    * @return <code>FSAuthnResponseEnvelope</code> object.
    * @throws FSMsgException if there is error creating the object.
    */
   public static FSAuthnResponseEnvelope parseXML(String xml)
                                         throws FSMsgException {
      try {
           Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
           Element root = doc.getDocumentElement();
           return new FSAuthnResponseEnvelope(root);           
       } catch(SAMLException ex){
           if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAuthnResponseEnvelope.parseXML: "
                                + "Error while parsing input xml string");
                }
           throw new FSMsgException("parseError",null);
        }          
   }
   /**
    * Returns XML document String for this object based on the Response Schema.
    *
    * @return XML String representing the <code>Response</code>
    * @throws FSMsgException if there is an error.
    */
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    
   /**
    * Creates a String representation of the <code>Response<code> object.
    * @param includeNS : Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
    * @param declareNS : Determines whether or not the namespace is declared
    *        within the Element.
    * @return A string containing the valid XML for this element.
    * @throws FSMsgException if there is an error.
    */
    public String toXMLString(boolean includeNS, boolean declareNS)
                              throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Creates a String representation of the  <code>Response</code> object.
     *
     * @param includeNS  Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS  Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid XML for this object.
     * @throws FSMsgException if there is an error.
     */
    public String toXMLString(boolean includeNS,
                              boolean declareNS,
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
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
               uri = IFSConstants.LIB_NAMESPACE_STRING;
            }
        }       
        xml.append(IFSConstants.LEFT_ANGLE)
           .append(prefix)
           .append(IFSConstants.AUTHN_RESPONSE_ENVELOPE)
           .append(uri)
           .append(IFSConstants.RIGHT_ANGLE);            

        if (authnResponse != null) {
            xml.append(authnResponse.toXMLString());
        }

        if(assertionConsumerServiceURL != null && 
            assertionConsumerServiceURL.length() != 0) {
            xml.append(IFSConstants.LEFT_ANGLE)
               .append(prefix)
               .append(IFSConstants.ASSERTION_CONSUMER_SERVICE_URL)
               .append(uri)
               .append(IFSConstants.RIGHT_ANGLE)
               .append(assertionConsumerServiceURL)
               .append(IFSConstants.START_END_ELEMENT)
               .append(prefix)
               .append(IFSConstants.ASSERTION_CONSUMER_SERVICE_URL)
               .append(IFSConstants.RIGHT_ANGLE);            
        } else {
            throw new FSMsgException("nullInput",null);
        }
        xml.append(IFSConstants.START_END_ELEMENT)
           .append(prefix)
           .append(IFSConstants.AUTHN_RESPONSE_ENVELOPE);

        return xml.toString();
    }
      
   /**
    * Returns a <code>Base64</code> Encoded String.
    *
    * @return a <code>Base64</code> Encoded String.
    * @throws FSMsgException if there is an error encoding     
    *         the string.
    */
    public String toBASE64EncodedString() throws FSMsgException  {        
        return Base64.encode(this.toXMLString().getBytes());
    }
    
   /**
    * Returns <code>FSAuthnResponseEnvelope</code> object. The
    * object is creating by parsing the <code>Base64</code>
    * encoded <code>XML</code> string.
    *
    * @param encodedReq the <code>Based64</code> encoded <code>XML</code> 
    *        string.
    * @throws FSMsgException if there is an error
    *         creating <code>FSAuthnResponseEnvelope</code> object.
    */
    public static FSAuthnResponseEnvelope parseBASE64EncodedString(
                                   String encodedReq) throws FSMsgException {        
        if (encodedReq != null) {
            String decodedAuthnReq = new String(Base64.decode(encodedReq));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnResponseEnvelope."
                    + "parseBASE64EncodedString: decoded input string: \n"
                    + decodedAuthnReq);
            }
            return parseXML(decodedAuthnReq);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnResponseEnvelope."
                    + "parseBASE64EncodedString: null String passed"
                    + "in as argument.");
            }
            throw new FSMsgException("nullInput",null);            
        }
    }
}
