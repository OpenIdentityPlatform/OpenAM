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
 * $Id: XACMLAuthzDecisionQueryImpl.java,v 1.4 2008/06/25 05:48:15 qcheng Exp $
 *
 */

package com.sun.identity.xacml.saml2.impl;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.RequestAbstractImpl;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionQuery;
import com.sun.identity.xacml.context.ContextFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.text.ParseException;

/**
 * The <code>XACMLAuthzDecisionQueryImpl</code> is an impelmentation
 * of <code>XACMLAuthzDecisionQuery</code> interface.
 *
 * The <code>XACMLAuthzDecisionQuery</code> element is a SAML Query that 
 * extends SAML Protocol schema type <code>RequestAbstractType</code>.
 * It allows an XACML PEP to submit an XACML Request Context in a  SAML
 * Query along with other information. This element is an alternative to 
 * SAML defined <code><samlp:AuthzDecisionQuery></code> that allows an 
 * XACML PEP  to communicate with an XACML PDP using SAML2 protocol.
 * <p>
 * <pre>
 *&lt;xs:element name="XACMLAuthzDecisionQuery"
 *         type="XACMLAuthzDecisionQueryType"/>
 *&lt;xs:complexType name="XACMLAuthzDecisionQueryType">
 *  &lt;xs:complexContent>
 *    &lt;xs:extension base="samlp:RequestAbstractType">
 *      &lt;xs:sequence>
 *        &lt;xs:element ref="xacml-context:Request"/>
 *      &lt;xs:sequence>
 *      &lt;xs:attribute name="InputContextOnly"
 *                    type="boolean"
 *                    use="optional"
 *                    default="false"/>
 *      &lt;xs:attribute name="ReturnContext"
 *                    type="boolean"
 *                    use="optional"
 *                    default="false"/>
 *    &lt;xs:extension>
 *  &lt;xs:complexContent>
 *&lt;xs:complexType>
 * </pre>
 *
 * Schema for Base:
 * <pre>
 *  &lt;complexType name="RequestAbstractType" abstract="true">
 *      &lt;sequence>
 *          &lt;element ref="saml:Issuer" minOccurs="0"/>
 *          &lt;element ref="ds:Signature" minOccurs="0"/>
 *          &lt;element ref="samlp:Extensions" minOccurs="0"/>
 *      &lt;sequence>
 *      &lt;attribute name="ID" type="ID" use="required"/>
 *      &lt;attribute name="Version" type="string" use="required"/>
 *      &lt;attribute name="IssueInstant" type="dateTime" use="required"/>
 *      &lt;attribute name="Destination" type="anyURI" use="optional"/>
 *  	&lt;attribute name="Consent" type="anyURI" use="optional"/>
 *  &lt;complexType>
 * </pre>
 *@supported.all.api
 */
public class XACMLAuthzDecisionQueryImpl extends RequestAbstractImpl 
        implements XACMLAuthzDecisionQuery {
    
    //TODO: need to reimplement toXML, toXML, process,
    //makeImmutable, isMutable methods
    private boolean inputContextOnly = false;
    private boolean returnContext = false;
    private Request request;

    private String xmlString;

    /**
     * Default constructor
     */
    public XACMLAuthzDecisionQueryImpl() {
        isMutable = true;
    }
    
    /**
     * This constructor is used to build <code>XACMLAuthzDecisionQuery</code> 
     * object from a block of existing XML that has already been built into a
     * DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>XACMLAuthzDecisionQuery</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public XACMLAuthzDecisionQueryImpl(Element element) throws SAML2Exception {
        parseDOMElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }
    
    /**
     * This constructor is used to build <code>XACMLAuthzDecisionQuery</code>
     * object from a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        an <code>XACMLAuthzDecisionQuery</code> object
     * @exception XACMLException if it could not process the XML string
     */
    public XACMLAuthzDecisionQueryImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            parseDOMElement(rootElement);
            this.xmlString = xml;
            if(isSigned) {
                signedXMLString = xml;
            }
        } else {
            XACMLSDKUtils.debug.error(
                    "XACMLAuthzDecisionQueryImpl.processElement(): invalid XML "
                     +"input");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "errorObtainingElement"));
        }
    }
    
    
    /**
     * Returns the XML attribute boolean value which governs the
     * source of information that the PDP is allowed to use in
     * making an authorization decision. If this attribute is "true"
     * then it indiactes that the authorization decision has been made
     * solely on the basis of information contained in the <code>
     * XACMLAuthzDecisionQuery</code>; no external attributes have been
     * used. If this value is "false" then the decision may have been made
     * on the basis of external attributes not conatined in the <code>
     * XACMLAuthzDecisionQuery</code>.
     * @return <code>boolean</code> indicating the value
     * of this attribute.
     */
    public boolean getInputContextOnly() {
        return inputContextOnly;
    }
    
    
    /**
     * Sets the XML attribute boolean value which governs the
     * source of information that the PDP is allowed to use in
     * making an authorization decision. If this attribute is "true"
     * then it indicates to the PDP  that the authorization decision has to be 
     * made solely on the basis of information contained in the <code>
     * XACMLAuthzDecisionQuery</code>; no external attributes may be
     * used. If this value is "false" then the decision can be  made
     * on the basis of external attributes not conatined in the <code>
     * XACMlAuthzDecisionQuery</code>.
     * @param inputContextOnly <code>boolean</code> indicating the value
     * of this attribute.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setInputContextOnly(boolean inputContextOnly) throws
            XACMLException 
    {
        this.inputContextOnly = inputContextOnly;
    }
    
    
    /**
     * Returns the XML attribute boolean value which provides means
     * to PEP to request that an <code>xacml-context>Request</code>
     * element be included in the <code>XACMlAuthzdecisionStatement</code>
     * resulting from the request. It also governs the contents of that
     * <code.Request</code> element. If this attribite is "true" then the
     * PDP SHALL include the <code>xacml-context:Request</code> element in the
     * <code>XACMLAuthzDecisionStatement</code> element in the 
     * <code>XACMLResponse</code>.
     * The <code>xacml-context:Request</code> SHALL include all the attributes 
     * supplied by the PEP in the <code>AuthzDecisionQuery</code> which were 
     * used in making the authz decision. Other addtional attributes which may 
     * have been used by the PDP may be included.
     * If this attribute is "false" then the PDP SHALL NOT include the
     * <code>xacml-context:Request</code> element in the 
     * <code>XACMLAuthzDecisionStatement<code>.
     *
     * @return <code>boolean</code> indicating the value
     * of this attribute.
     */
    public boolean getReturnContext() {
        return returnContext;
    }
    
    /**
     * Sets the boolean value for this XML attribute
     *
     * @param returnContext <code>boolean</code> indicating the value
     * of this attribute.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     *
     * @see #getReturnContext()
     */
    public void setReturnContext(boolean returnContext) throws XACMLException {
        this.returnContext = returnContext;
    }
    
    /**
     * Returns the <code>xacml-context:Request</code> element of this object
     *
     * @return the <code>xacml-context:Request</code> elements of this object
     */
    public Request getRequest() {
        return request;
    }
    
    /**
     * Sets the <code>xacml-context:Request</code> element of this object
     *
     * @param request the <code>xacml-context:Request</code> element of this
     * object.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setRequest(Request request) throws XACMLException {
        if (request == null) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "null_not_valid")); 
        }
        this.request = request;
    }
    
    /**
     * Returns a string representation of this object
     *
     * @return a string representation of this object
     * @exception XACMLException if conversion fails for any reason
     */
    public String toXMLString() throws XACMLException {
        //top level element
        return toXMLString(true, true);
    }

    /**
     * Returns a <code>String</code> representation of this object
     * @param includeNSPrefix Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string representation of this object
     * @exception XACMLException if conversion fails for any reason
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws XACMLException {
	if (isSigned && signedXMLString != null) {
	    return signedXMLString;
	}

	//validateData();
        StringBuffer sb = new StringBuffer(1000);
        String nsPrefix = "";
        String nsDeclaration = "";
        if (declareNS) {
            nsDeclaration = XACMLConstants.SAMLP_NS_DECLARATION;
        }
        if (includeNSPrefix) {
            nsPrefix = XACMLConstants.SAMLP_NS_PREFIX;
        }

        sb.append("\n<")
                .append(XACMLConstants.SAMLP_NS_PREFIX)
                .append(XACMLConstants.REQUEST_ABSTRACT)
                .append(XACMLConstants.SAMLP_NS_DECLARATION)
                .append(XACMLConstants.XSI_TYPE_XACML_AUTHZ_DECISION_QUERY)
                .append(XACMLConstants.XSI_NS_DECLARATION)
                .append(XACMLConstants.XACML_SAMLP_NS_DECLARATION)
            .append(XACMLConstants.SPACE)
            .append(XACMLConstants.XACML_SAMLP_NS_PREFIX)
            .append(XACMLConstants.INPUT_CONTEXT_ONLY).append("=")
            .append(XACMLSDKUtils.quote(Boolean.toString(inputContextOnly)))
            .append(XACMLConstants.SPACE)
            .append(XACMLConstants.XACML_SAMLP_NS_PREFIX)
            .append(XACMLConstants.RETURN_CONTEXT).append("=")
            .append(XACMLSDKUtils.quote(Boolean.toString(returnContext)))
            .append(XACMLConstants.SPACE)
            .append("ID").append("=")
            .append(XACMLSDKUtils.quote(requestId))
            .append(XACMLConstants.SPACE)
            .append("Version").append("=")
            .append(XACMLSDKUtils.quote(version))
            .append(XACMLConstants.SPACE)
            .append("IssueInstant").append("=")
            .append(XACMLSDKUtils.quote(DateUtils.toUTCDateFormat(
                    issueInstant)));
	if (destinationURI != null && destinationURI.trim().length() != 0) {
	    sb.append(" Destination=\"").append(destinationURI).
		append("\"");
	}
	if (consent != null && consent.trim().length() != 0) {
	    sb.append(" Consent=\"").append(consent).append("\"");
	}
	sb.append(">\n");
        try {
	if (nameID != null) {
	    sb.append(nameID.toXMLString(includeNSPrefix, declareNS));
	}
	if (signatureString != null) {
	    sb.append(signatureString);
	}
	if (extensions != null) {
	    sb.append(extensions.toXMLString(includeNSPrefix, declareNS));
	}
        } catch (Exception e) {
        }

        if (request != null) {
            sb.append(request.toXMLString(true, true)).append("\n");
        }

        sb.append("\n</")
                .append(XACMLConstants.SAMLP_NS_PREFIX)
                .append(XACMLConstants.REQUEST_ABSTRACT)
                .append(">\n");
        return  sb.toString();
    }
    
    
    protected void parseDOMElement(Element element) throws SAML2Exception {
        //TODO: fix
        String value = null;
        if (element == null) {
            XACMLSDKUtils.debug.error(
                    "XACMLAuthzDecisionQueryImpl.processElement(): "
                    + "invalid root element");
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_element"));
        }
        
        // First check that we're really parsing an XACMLAuthzDecisionQuery
        if (! element.getLocalName().equals(
            XACMLConstants.REQUEST_ABSTRACT)) {
            XACMLSDKUtils.debug.error(
                    "XACMLAuthzDecisionQueryImpl.processElement(): "
                    + "invalid root element");
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_local_name"));
        }
        
        //TODO: check for xsi:type=
        
        // now we get the request
        NodeList nodes = element.getChildNodes();
        ContextFactory factory = ContextFactory.getInstance();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ((node.getNodeType() == Node.ELEMENT_NODE) ||
                    (node.getNodeType() == Node.ATTRIBUTE_NODE)) {
                if (node.getLocalName().equals(XACMLConstants.REQUEST)) {
                    if (request != null) {
                        //validation error, throw error
                    } else {
                        request = factory.getInstance().createRequest(
                                (Element)node);
                    }
                }
            }
        }

        // make sure we got a request
        if (request == null) {
            //throw new XACMLException(
             //       XACMLSDKUtils.xacmlResourceBundle.getString(
             //       "null_not_valid"));
        }
        
        System.out.println("ReturnContex:" + element.getAttributeNS(
                XACMLConstants.XACML_SAMLP_NS_URI,
                XACMLConstants.RETURN_CONTEXT));
        System.out.println("InputContextOnly:" + element.getAttributeNS(
                XACMLConstants.XACML_SAMLP_NS_URI,
                XACMLConstants.INPUT_CONTEXT_ONLY));
        String returnContextString = element.getAttributeNS(
                XACMLConstants.XACML_SAMLP_NS_URI,
                XACMLConstants.RETURN_CONTEXT);
        if (returnContextString != null) {
            returnContext = Boolean.valueOf(returnContextString).booleanValue();
        }

        String inputContextOnlyString = element.getAttributeNS(
                XACMLConstants.XACML_SAMLP_NS_URI,
                XACMLConstants.INPUT_CONTEXT_ONLY);
        if (inputContextOnlyString != null) {
            inputContextOnly = Boolean.valueOf(inputContextOnlyString)
                    .booleanValue();
        }

        NamedNodeMap attrs = element.getAttributes();

        //TODO: change the baseclass impl and call super.parse...

        //parse the attributes of base class RequestAbstract
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
	    int length = atts.getLength();
            for (int i = 0; i < length; i++) {
                Attr attr = (Attr) atts.item(i);
                String attrName = attr.getName();
                String attrValue = attr.getValue().trim();
                if (attrName.equals("ID")) {
                    requestId = attrValue;
                } else if (attrName.equals("Version")) {
                    version = attrValue;
                } else if (attrName.equals("IssueInstant")) {
		    try {
			issueInstant = DateUtils.stringToDate(attrValue);
		    } catch (ParseException pe) {
			throw new XACMLException(pe.getMessage());
		    }
                } else if (attrName.equals("Destination")) {
		    destinationURI = attrValue;
		}
            }
        }

	//parse the elements of base class RequestAbstract
	NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Issuer")) {
		    if (nameID != null) {
			if (XACMLSDKUtils.debug.messageEnabled()) {
                            XACMLSDKUtils.debug.message(
                                "ArtifactResolveImpl.parse"
                                + "Element: included more than one Issuer.");
                        }
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                            "invalid_duplicate_element"));
		    }
		    if (signatureString != null ||
			extensions != null )
		    {
			if (XACMLSDKUtils.debug.messageEnabled()) {
                            XACMLSDKUtils.debug.message(
                                    "ArtifactResolveImpl.parse"	
                                    + "Element:wrong sequence.");
			}
			throw new XACMLException(
			    XACMLSDKUtils.xacmlResourceBundle.getString(
                            "schemaViolation"));
		    }
		    nameID = AssertionFactory.getInstance().createIssuer(
			(Element) child);
		} else if (childName.equals("Signature")) {
		    if (signatureString != null) {
			if (XACMLSDKUtils.debug.messageEnabled()) {
                            XACMLSDKUtils.debug.message(
                                "ArtifactResolveImpl.parse"
                                + "Element:included more than one Signature.");
                        }
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                            "invalid_duplicate_element"));
		    }
		    if (extensions != null ) {
			if (XACMLSDKUtils.debug.messageEnabled()) {
                            XACMLSDKUtils.debug.message(
                                    "ArtifactResolveImpl.parse"	
                                    + "Element:wrong sequence.");
			}
			throw new XACMLException(
			    XACMLSDKUtils.xacmlResourceBundle.getString(
                            "schemaViolation"));
		    }
		    signatureString = XMLUtils.print((Element) child);
		    isSigned = true;
		} else if (childName.equals("Extensions")) {
		    if (extensions != null) {
			if (XACMLSDKUtils.debug.messageEnabled()) {
                            XACMLSDKUtils.debug.message(
                                "ArtifactResolveImpl.parse"
                                + "Element:included more than one Extensions.");
                        }
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                            "invalid_duplicate_element"));
		    }
		    extensions = ProtocolFactory.getInstance().createExtensions(
			(Element) child);
		} else if (childName.equals("Request")) {
                    //no action, it has been processd already
		} else {
		    if (XACMLSDKUtils.debug.messageEnabled()) {
                        XACMLSDKUtils.debug.message(
                            "XACMLAuthzDecisionQueryImpl.parseDOMElement"
                            + "Element: Invalid element:" + childName);
                    }
                    throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalidElement"));
		}
	    }
	}

        validateData();
        
    }
    
    /**
     * Makes the object immutable
     */
    public void makeImmutable() {
        //TODO: fix
    }
    
    protected void validateData() throws SAML2Exception {
        //TODO: fix or remove?
        super.validateData();
    }

}
