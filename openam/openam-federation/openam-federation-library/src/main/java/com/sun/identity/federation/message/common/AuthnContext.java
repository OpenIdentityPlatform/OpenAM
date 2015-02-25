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
 * $Id: AuthnContext.java,v 1.3 2008/06/25 05:46:46 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message.common;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class <code>AuthnContext</code> represents an Authentication Context
 * for the authenticated user with a requested authn context.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class AuthnContext {
    
    protected String authnContextClassRef = null;
    protected String authnContextStatementRef = null;
    protected int minorVersion = 0;
    
    /**
     * Default constructor
     */
    public AuthnContext() {}
    
    
    /**
     * Constructor creates <code>AuthnContext</code> object.
     *
     * @param authnContextClassRef Authentication Context Class Reference URI
     * @param authnContextStatementRef Authentication Context
     *        Statement Reference URI
     */
    public AuthnContext(String authnContextClassRef,
            String authnContextStatementRef) {
        this.authnContextClassRef = authnContextClassRef;
        this.authnContextStatementRef = authnContextStatementRef;
    }
    
    /**
     * Constructor creates <code>AuthnContext</code> object
     * from the Document Element.
     *
     * @param root the Document Element.
     * @throws FSMsgException on error.
     */
    public AuthnContext(Element root) throws FSMsgException {
        if (root == null) {
            FSUtils.debug.message("AuthnContext(): null input.");
            throw new FSMsgException("nullInput",null);
        }
        String tag = root.getLocalName();
        if ((tag == null) || (!tag.equals("AuthnContext"))) {
            FSUtils.debug.message("AuthnContext: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        String namespace = root.getNamespaceURI();
        if ((namespace != null) && namespace.equals(IFSConstants.FF_12_XML_NS)){
            minorVersion = IFSConstants.FF_12_PROTOCOL_MINOR_VERSION;
        }
        NodeList nl = root.getChildNodes();
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            Node child = nl.item(i);
            String childName = child.getLocalName();
            if (childName == null) {
                continue;
            }
            
            if(childName.equals("AuthnContextClassRef")) {
                if(authnContextClassRef != null) {
                    FSUtils.debug.error("AuthnContext(Element): Should"
                            + "contain only one AuthnContextClassRef element");
                    throw new FSMsgException("wrongInput",null);
                }
                authnContextClassRef = XMLUtils.getElementValue((Element) child);
                
            } else if(childName.equals("AuthnContextStatementRef")) {
                if(authnContextStatementRef != null) {
                    FSUtils.debug.error("AuthnContext(Element): Should contain "
                            + " only one AuthnContextStatementRef element");
                    throw new FSMsgException("wrongInput",null);
                }
                authnContextStatementRef =
                        XMLUtils.getElementValue((Element) child);
                
            } else if(childName.equals("AuthenticationContextStatement")) {
                if(FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("AuthnContext(Element): " +
                            "Authentication Statement");
                }
            }
        }
    }
    
    /**
     * Returns the  AuthnContext Class Reference URI.
     *
     * @return the  AuthnContext Class Reference URI.
     * @see #setAuthnContextClassRef
     */
    public String getAuthnContextClassRef(){
        return authnContextClassRef;
    }
    
    /**
     * Sets the AuthnContext Class Reference URI.
     *
     * @param authnContextClassRef AuthnContext Class Ref URI.
     * @see #getAuthnContextClassRef
     */
    public void setAuthnContextClassRef(String authnContextClassRef) {
        this.authnContextClassRef = authnContextClassRef;
    }
    
    /**
     * Returns the AuthnContext Statement Reference URI.
     *
     * @return the AuthnContext Statement Reference URI.
     * @see #setAuthnContextStatementRef
     */
    public String getAuthnContextStatementRef(){
        return authnContextStatementRef;
    }
    
    /**
     * Sets AuthnContext Statement Reference URI.
     *
     * @param authnContextStatementRef AuthnContext Statement Ref URI.
     * @see #getAuthnContextStatementRef
     */
    public void setAuthnContextStatementRef(
            String authnContextStatementRef) {
        
        this.authnContextStatementRef = authnContextStatementRef;
    }
    
    /**
     * Returns the <code>MinorVersion</code> attribute.
     *
     * @return the Minor Version.
     * @see #setMinorVersion(int)
     */
    
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the <code>MinorVersion</code>.
     *
     * @param version the minor version in the assertion.
     * @see #setMinorVersion(int)
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
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
        return this.toXMLString(true, true);
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
                    append(SAMLConstants.DEFAULT_ENCODING).append("\" ?>");
        }
        String prefixAC = "";
        String prefixLIB = "";
        String uriAC = "";
        String uriLIB = "";
        if (includeNS) {
            prefixLIB = IFSConstants.LIB_PREFIX;
            prefixAC = IFSConstants.AC_PREFIX;
        }
        
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
                uriAC = IFSConstants.AC_12_NAMESPACE_STRING;
            } else {
                uriLIB = IFSConstants.LIB_NAMESPACE_STRING;
                uriAC = IFSConstants.AC_NAMESPACE_STRING;
            }
        }
        
        xml.append("<").append(prefixLIB).
                append("AuthnContext").append(uriLIB).append(">");
        
        if(authnContextClassRef != null &&
                !authnContextClassRef.equals("")) {
            xml.append("<").append(prefixLIB).
                    append("AuthnContextClassRef").append(">");
            xml.append(authnContextClassRef);
            xml.append("</").append(prefixLIB).
                    append("AuthnContextClassRef").append(">");
        } else {
            xml.append("<").append(prefixLIB).
                    append("AuthnContextClassRef").append(">");
            xml.append(IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD);
            xml.append("</").append(prefixLIB).
                    append("AuthnContextClassRef").append(">");
            
        }
        
        if(authnContextStatementRef != null &&
                !authnContextStatementRef.equals("")) {
            xml.append("<").append(prefixLIB).
                    append("AuthnContextStatementRef").append(">");
            xml.append(authnContextStatementRef);
            xml.append("</").append(prefixLIB).
                    append("AuthnContextStatementRef").append(">");
        } else {
            xml.append("<").append(prefixLIB).
                    append("AuthnContextStatementRef").append(">");
            xml.append(IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD);
            xml.append("</").append(prefixLIB).
                    append("AuthnContextStatementRef").append(">");
        }
        
        xml.append("</").append(prefixLIB).append("AuthnContext").append(">");
        return xml.toString();
    }
}
