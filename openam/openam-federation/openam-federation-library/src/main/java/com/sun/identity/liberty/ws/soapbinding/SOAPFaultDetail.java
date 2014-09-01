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
 * $Id: SOAPFaultDetail.java,v 1.2 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import java.lang.Object;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.security.cert.X509Certificate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>SOAPFaultDetail</code> class represents the 'Detail' child element
 * of SOAP Fault element. Its children can be of any type. This class provides
 * specific methods to get and set the following children: Status element,
 * <code>CorrelationHeader</code>, <code>ProviderHeader</code>,
 * <code>ConsentHeader</code>, <code>UsageDirectiveHeader</code> and
 * <code>ProcessingContextHeader</code>. It also provides generic methods to
 * get and set other children.
 *
 * @supported.all.api
 */
public class SOAPFaultDetail {
    public static final QName BOGUS_ACTOR =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING, "BogusActor");
    public static final QName BOGUS_MUST_UNSTND =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "BogusMustUnstnd");
    public static final QName STALE_MSG =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING, "StaleMsg");
    public static final QName DUPLICATE_MSG =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING, "DuplicateMsg");
    public static final QName INVALID_REF_TO_MSG_ID =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "invalidRefToMsgID");
    public static final QName PROVIDER_ID_NOT_VALID =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "ProviderIDNotValid");
    public static final QName AFFILIATION_ID_NOT_VALID =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "AffiliationIDNotValid");
    public static final QName ID_STAR_MSG_NOT_UNSTD =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "IDStarMsgNotUnstd");
    public static final QName PROC_CTX_URI_NOT_UNSTD =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "ProcCtxURINotUnstd");
    public static final QName PROC_CTX_UNWILLING =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "ProcCtxUnwilling");
    public static final QName CAN_NOT_HONOUR_USAGE_DIRECTIVE =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING,
                        "CannotHonourUsageDirective");
    public static final QName ENDPOINT_MOVED =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING_11,
                        "EndpointMoved");
    public static final QName INAPPROPRIATE_CREDENTIALS =
              new QName(SOAPBindingConstants.NS_SOAP_BINDING_11,
                        "InappropriateCredentials");

    private QName  statusCode = null;
    private String statusRef = null;
    private String statusComment = null;
    private CorrelationHeader correlationHeader = null;
    private ConsentHeader consentHeader = null;
    private List usageDirectiveHeaders = null;
    private ProviderHeader providerHeader = null;
    private ProcessingContextHeader processingContextHeader = null;
    private ServiceInstanceUpdateHeader serviceInstanceUpdateHeader = null;
    private List otherChildren = null;
    
    /**
     * This constructor takes a status code, a status ref and a status comment.
     * If the status code is not null, a Status child element will be created.
     *
     * @param statusCode the value of <code>code</code> attribute of the Status
     *                   element.
     * @param statusRef the value of <code>ref</code> attribute of the Status
     *                  element.
     * @param statusComment the value of <code>comment</code> attribute of the
     *                      Status element.
     */
    public SOAPFaultDetail(QName statusCode,String statusRef,
            String statusComment) {
        this.statusCode = statusCode;
        this.statusRef = statusRef;
        this.statusComment = statusComment;
    }

    /**
     * This constructor takes a list of children except Status element,
     * <code>CorrelationHeader</code>, <code>ProviderHeader</code>
     * <code>ConsentHeader</code>, <code>UsageDirectiveHeader</code>,
     * <code>ProcessingContextHeader</code> and
     * <code>ServiceInstanceUpdateHeader</code>.
     * Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @param otherChildren a list of children element
     */
    public SOAPFaultDetail(List otherChildren) {
        this.otherChildren = otherChildren;
    }

    /**
     * Parses a <code>org.w3c.dom.Document</code> to construct this object.
     *
     * @param detailElement a <code>org.w3c.dom.Document</code>.
     * @throws SOAPBindingException if an error occurs while parsing
     *                                 the document
     */
    SOAPFaultDetail(Element detailElement) throws SOAPBindingException {
        NodeList nl = detailElement.getChildNodes();
        int length = nl.getLength();

        if (length == 0) {
            return;
        }

        otherChildren = new ArrayList();
        for (int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)child;
                String localName = element.getLocalName();
                String ns = element.getNamespaceURI();

                if (SOAPBindingConstants.NS_SOAP_BINDING.equals(ns)) {
                    if (SOAPBindingConstants.TAG_STATUS.equals(localName)) {
                        String value = XMLUtils.getNodeAttributeValue(
                                               element,
                                               SOAPBindingConstants.ATTR_CODE);
                        if (value == null || value.length() ==0) {
                            String msg = Utils.bundle
                                          .getString("missingFaultStatusCode");
                            Utils.debug.error("SOAPFaultException: " + msg);
                            throw new SOAPBindingException(msg);
                        }
                        statusCode = Utils.convertStringToQName(value,element);
                        statusRef = XMLUtils.getNodeAttributeValue(
                                                element,
                                                SOAPBindingConstants.ATTR_REF);
                        statusComment = XMLUtils.getNodeAttributeValue(
                                            element,
                                            SOAPBindingConstants.ATTR_COMMENT);
                    } else if (SOAPBindingConstants.TAG_CORRELATION
                                                   .equals(localName)) {
                        correlationHeader = new CorrelationHeader(element);
                    } else if (SOAPBindingConstants.TAG_CONSENT
                                                   .equals(localName)) {
                        consentHeader = new ConsentHeader(element);
                    } else if(SOAPBindingConstants.TAG_USAGE_DIRECTIVE
                                                  .equals(localName)){
                        if (usageDirectiveHeaders == null) {
                            usageDirectiveHeaders = new ArrayList();
                        }
                        usageDirectiveHeaders.add(
                                    new UsageDirectiveHeader(element));
                    } else if (SOAPBindingConstants.TAG_PROVIDER
                                                   .equals(localName)) {
                        providerHeader = new ProviderHeader(element);
                    } else if (SOAPBindingConstants.TAG_PROCESSING_CONTEXT
                                                   .equals(localName)){
                            processingContextHeader =
                                    new ProcessingContextHeader(element);
                    } else {
                        otherChildren.add(element);
                    }
                } else if (SOAPBindingConstants.NS_SOAP_BINDING_11.equals(ns)&&
                           SOAPBindingConstants.TAG_SERVICE_INSTANCE_UPDATE
                                               .equals(localName)){
                    serviceInstanceUpdateHeader =
                                    new ServiceInstanceUpdateHeader(element);
                } else {
                    otherChildren.add(element);
                }
            }
        }
        if (otherChildren.isEmpty()) {
            otherChildren = null;
        }
    }

    /**
     * Returns the <code>CorrelationHeader</code>.
     *
     * @return the <code>CorrelationHeader</code>.
     */
    public CorrelationHeader getCorrelationHeader() {
        return correlationHeader;
    }

    /**
     * Returns the <code>ConsentHeader</code>.
     *
     * @return the <code>ConsentHeader</code>.
     */
    public ConsentHeader getConsentHeader() {
        return consentHeader;
    }

    /**
     * Returns a list of <code>UsageDirectiveHeader</code>.
     *
     * @return a list of <code>UsageDirectiveHeader</code>.
     */
    public List getUsageDirectiveHeaders() {
        return usageDirectiveHeaders;
    }

    /**
     * Returns the <code>ProviderHeader</code>.
     *
     * @return the <code>ProviderHeader</code>.
     */
    public ProviderHeader getProviderHeader() {
        return providerHeader;
    }

    /**
     * Returns <code>ProcessingContextHeader</code>.
     *
     * @return <code>ProcessingContextHeader</code>.
     */
    public ProcessingContextHeader getProcessingContextHeader() {
        return processingContextHeader;
    }

    /**
     * Returns <code>ServiceInstanceUpdateHeader</code>.
     *
     * @return <code>ServiceInstanceUpdateHeader</code>.
     */
    public ServiceInstanceUpdateHeader getServiceInstanceUpdateHeader() {
        return serviceInstanceUpdateHeader;
    }

    /**
     * Returns a list of children except Status element,
     * <code>CorrelationHeader</code>, <code>ProviderHeader</code>
     * <code>ConsentHeader</code>, <code>UsageDirectiveHeader</code>,
     * <code>ProcessingContextHeader</code> and
     * <code>ServiceInstanceUpdateHeader</code>.
     * Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @return a list of children element
     */
    public List getOtherChildren() {
        return otherChildren;
    }

    /**
     * Returns value of <code>code</code> attribute of Status element.
     *
     * @return value of <code>code</code> attribute of Status element.
     */
    public QName getStatusCode() {
        return statusCode;
    }

    /**
     * Returns value of <code>ref</code> attribute of Status element.
     *
     * @return value of <code>ref</code> attribute of Status element.
     */
    public String getStatusRef() {
        return statusRef;
    }

    /**
     * Returns value of <code>comment</code> attribute of Status element.
     *
     * @return value of <code>comment</code> attribute of Status element.
     */
    public String getStatusComment() {
        return statusComment;
    }

    /**
     * Sets the value of <code>CorrelationHeader</code>.
     *
     * @param correlationHeader the <code>CorrelationHeader</code>.
     */
    public void setCorrelationHeader(CorrelationHeader correlationHeader) {
        if (correlationHeader != null) {
            this.correlationHeader = correlationHeader;
        }
    }

    /**
     * Sets <code>ConsentHeader</code>.
     *
     * @param consentHeader <code>ConsentHeader</code>.
     */
    public void setConsentHeader(ConsentHeader consentHeader) {
        this.consentHeader = consentHeader;
    }
    
    /**
     * Sets a list of <code>UsageDirectiveHeader</code>.
     *
     * @param usageDirectiveHeaders a list of <code>UsageDirectiveHeader</code>.
     */
    public void setUsageDirectiveHeaders(List usageDirectiveHeaders) {
        this.usageDirectiveHeaders = usageDirectiveHeaders;
    }
    
    /**
     * Sets <code>ProviderHeader</code> if it is not null.
     *
     * @param providerHeader <code>ProviderHeader</code>
     */
    public void setProviderHeader(ProviderHeader providerHeader) {
        if (providerHeader != null) {
            this.providerHeader = providerHeader;
        }
    }
    
    /**
     * Sets <code>ProcessingContextHeader</code>.
     *
     * @param processingContextHeader <code>ProcessingContextHeader</code>
     */
    public void setProcessingContextHeader( 
            ProcessingContextHeader processingContextHeader) {
        this.processingContextHeader = processingContextHeader;
    }

    /**
     * Sets <code>ServiceInstanceUpdateHeader</code>.
     *
     * @param serviceInstanceUpdateHeader
     *        <code>ServiceInstanceUpdateHeader</code>
     */
    public void setServiceInstanceUpdateHeader(
        ServiceInstanceUpdateHeader serviceInstanceUpdateHeader) {
        this.serviceInstanceUpdateHeader = serviceInstanceUpdateHeader;
    }

    /**
     * Sets a list of children except Status element,
     * <code>CorrelationHeader</code>, <code>ProviderHeader</code>
     * <code>ConsentHeader</code>, <code>UsageDirectiveHeader</code>,
     * <code>ProcessingContextHeader</code> and
     * <code>ServiceInstanceUpdateHeader</code>.
     * Each entry will be a <code>org.w3c.dom.Element</code>.
     *
     * @param otherChildren a list of children element
     */
    public void setOtherChildren(List otherChildren) {
        this.otherChildren = otherChildren;
    }

    /**
     * Sets a child except Status element, <code>CorrelationHeader</code>,
     * <code>ProviderHeader</code>, <code>ConsentHeader</code>,
     * <code>UsageDirectiveHeader</code> and
     * <code>ProcessingContextHeader</code> and
     * <code>ServiceInstanceUpdateHeader</code>.
     *
     * @param child the child element.
     */
    public void setOtherChild(Element child) {
        otherChildren = new ArrayList(1);
        otherChildren.add(child);
    }

    /**
     * Sets value of <code>code</code> attribute of Status element.
     *
     * @param statusCode value of <code>code</code> attribute of Status element.
     */
    public void setStatusCode(QName statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Sets value of <code>ref</code> attribute of Status element.
     *
     * @param statusRef value of <code>ref</code> attribute of Status element.
     */
    public void setStatusRef(String statusRef) {
        this.statusRef = statusRef;
    }

    /**
     * Sets value of <code>comment</code> attribute of Status element.
     *
     * @param statusComment value of <code>comment</code> attribute in Status 
     *        element.
     */
    public void setStatusComment(String statusComment) {
        this.statusComment = statusComment;
    }

    /**
     * Converts this header to <code>org.w3c.dom.Element</code> and add to
     * parent Fault Element.
     *
     * @param faultE the Fault Element object.
     */
    void addToParent(Element faultE) {
        Document doc = faultE.getOwnerDocument();
        Element detailE = doc.createElement(SOAPBindingConstants.TAG_DETAIL);
        faultE.appendChild(detailE);

        if (statusCode != null) {
            Element statusE = doc.createElementNS(
                                          SOAPBindingConstants.NS_SOAP_BINDING,
                                          SOAPBindingConstants.PTAG_STATUS);
            String localPart = statusCode.getLocalPart();
            String ns = statusCode.getNamespaceURI();
            if (ns != null && ns.length() > 0) {
                String prefix;
                if (ns.equals(SOAPBindingConstants.NS_SOAP)) {
                    prefix = SOAPBindingConstants.PREFIX_SOAP;
                } else if (ns.equals(SOAPBindingConstants.NS_SOAP_BINDING)) {
                    prefix = SOAPBindingConstants.PREFIX_SOAP_BINDING;
                } else if (ns.equals(SOAPBindingConstants.NS_SOAP_BINDING_11)){
                    prefix = SOAPBindingConstants.PREFIX_SOAP_BINDING_11;
                } else {
                    prefix =
                        SOAPBindingConstants.DEFAULT_PREFIX_FAULT_CODE_VALUE;
                    statusE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                           "xmlns:" + prefix, ns);
                }
                statusE.setAttributeNS(null, SOAPBindingConstants.ATTR_CODE,
                                       prefix +":" +localPart);
            } else {
                statusE.setAttributeNS(null, SOAPBindingConstants.ATTR_CODE,
                                       localPart);
            }

            if (statusRef != null) {
                statusE.setAttributeNS(null, SOAPBindingConstants.ATTR_REF,
                                       statusRef);
            }
            if (statusComment != null) {
                statusE.setAttributeNS(null, SOAPBindingConstants.ATTR_COMMENT,
                                       statusComment);
            }
            detailE.appendChild(statusE);
        }
        if (correlationHeader != null) {
            correlationHeader.addToParent(detailE);
        }
        if (consentHeader != null) {
            consentHeader.addToParent(detailE);
        }
        if (usageDirectiveHeaders != null &&
            !usageDirectiveHeaders.isEmpty()) {
            Iterator iter = usageDirectiveHeaders.iterator();
            while(iter.hasNext()) {
                ((UsageDirectiveHeader)iter.next()).addToParent(detailE);
            }
        }
        if (providerHeader != null) {
            providerHeader.addToParent(detailE);
        }
        if (processingContextHeader != null) {
            processingContextHeader.addToParent(detailE);
        }
        if (serviceInstanceUpdateHeader != null) {
            serviceInstanceUpdateHeader.addToParent(detailE);
        }

        if (otherChildren != null && !otherChildren.isEmpty()) {
            Iterator iter = otherChildren.iterator();
            while(iter.hasNext()) {
                Element childE = (Element)iter.next();
                detailE.appendChild(doc.importNode(childE, true));
            }
        }
    }
}
