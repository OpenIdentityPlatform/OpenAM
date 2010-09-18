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
 * $Id: SOAPFault.java,v 1.2 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import java.io.ByteArrayInputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;

import javax.xml.namespace.QName;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MimeHeaders;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>SOAPFault</code> class represents a SOAP Fault element.
 *
 * @supported.all.api
 */

public class SOAPFault {

    private QName  faultcode = null;
    private String faultstring = null;
    private String faultactor = null;
    private SOAPFaultDetail detail = null;


    /**
     * Constructor. 
     *
     * @param faultcode value of <code>faultcode</code> element
     * @param faultstring value of <code>faultstring</code> element
     */
    public SOAPFault(QName faultcode, String faultstring) {
        this(faultcode, faultstring, null, null);
    }

    /**
     * Constructor.
     *
     * @param faultcode value of <code>faultcode</code> element.
     * @param faultstring value of <code>faultstring</code> element.
     * @param faultactor value of <code>faultactor</code> element.
     * @param detail a SOAP Fault Detail.
     */
    public SOAPFault(QName faultcode, String faultstring, 
                     String faultactor,SOAPFaultDetail detail) {
        this.faultcode = faultcode;
        this.faultstring = faultstring;
        this.faultactor = faultactor;
        this.detail = detail;
    }

    /**
     * This constructor takes SOAP Fault element.
     *
     * @param faultElement a SOAP Fault element
     * @throws SOAPBindingException if an error occurs while parsing
     *                                 SOAP Fault element
     */
    SOAPFault(Element faultElement) throws SOAPBindingException {

        NodeList nl = faultElement.getChildNodes();
        int length = nl.getLength();

        boolean foundInvalidChild = false;
        Element detailE = null;
        int numElements = 0;
        for (int i = 0; i < length; i++) {
            Node child = nl.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)child;
                String localName = element.getLocalName();
                String namespaceURI = element.getNamespaceURI();
                numElements++;
                if (numElements == 1) {
                    if (SOAPBindingConstants.TAG_FAULT_CODE.equals(localName)&&
                        namespaceURI == null) {

                        String value = XMLUtils.getElementValue(element);
                        if (value == null || value.length() ==0) {
                            String msg =
                                    Utils.bundle.getString("missingFaultCode");
                            Utils.debug.error("SOAPFaultException: " + msg);
                            throw new SOAPBindingException(msg);
                        }
                        faultcode = Utils.convertStringToQName(value, element);
                    } else {
                        String msg =Utils.bundle.getString("missingFaultCode");
                        Utils.debug.error("SOAPFaultException: " + msg);
                        throw new SOAPBindingException(msg);
                    }
                } else if (numElements == 2) {
                    if (SOAPBindingConstants.TAG_FAULT_STRING.equals(localName)
                        && namespaceURI == null) {
                        faultstring = XMLUtils.getElementValue(element);
                    } else {
                        String msg =
                                Utils.bundle.getString("missingFaultString");
                        Utils.debug.error("SOAPFaultException: " + msg);
                        throw new SOAPBindingException(msg);
                    }
                } else if (numElements == 3) {
                    if (SOAPBindingConstants.TAG_FAULT_ACTOR.equals(localName)
                        && namespaceURI == null) {
                        faultactor = XMLUtils.getElementValue(element);
                    } else if (SOAPBindingConstants.TAG_DETAIL
                                                   .equals(localName) &&
                        namespaceURI == null) {
                        detailE = element;
                    } else {
                        String msg = Utils.bundle.getString("invalidChild");
                        Utils.debug.error("SOAPFaultException: " + msg);
                        throw new SOAPBindingException(msg);
                    }
                } else if (numElements == 4) {
                    if (detailE == null &&
                        SOAPBindingConstants.TAG_DETAIL.equals(localName) &&
                        namespaceURI == null) {
                        detailE = element;
                    } else {
                        String msg = Utils.bundle.getString("invalidChild");
                        Utils.debug.error("SOAPFaultException: " + msg);
                        throw new SOAPBindingException(msg);
                    }
                } else {
                    String msg = Utils.bundle.getString("invalidChild");
                    Utils.debug.error("SOAPFaultException: " + msg);
                    throw new SOAPBindingException(msg);
                }
            }
        }

        if (detailE != null) {
            detail = new SOAPFaultDetail(detailE);
        }
    }

    /**
     * Returns value of <code>faultcode</code> element.
     *
     * @return value of <code>faultcode</code> element.
     */
    public QName getFaultCode() {
        return faultcode;
    }

    /**
     * Returns value of <code>faultstring</code> element.
     *
     * @return value of <code>faultstring</code> element.
     */
    public String getFaultString() {
        return faultstring;
    }

    /**
     * Returns value of <code>faultactor</code> element.
     *
     * @return value of <code>faultactor</code> element.
     */
    public String getFaultActor() {
        return faultactor;
    }

    /**
     * Returns a SOAP Fault Detail.
     *
     * @return a SOAP Fault Detail.
     */
    public SOAPFaultDetail getDetail() {
        return detail;
    }

    /**
     * Sets value of <code>faultcode</code> element.
     *
     * @param faultcode value of <code>faultcode</code> element
     */
    public void setFaultCode(QName faultcode) {
        this.faultcode = faultcode;
    }

    /**
     * Sets value of <code>faultstring</code> element.
     *
     * @param faultstring value of <code>faultstring</code> element.
     */
    public void setFaultString(String faultstring) {
        this.faultstring = faultstring;
    }

    /**
     * Sets value of <code>faultactor</code> element.
     *
     * @param faultactor value of <code>faultactor</code> element.
     */
    public void setFaultActor(String faultactor) {
        this.faultactor = faultactor;
    }

    /**
     * Sets a SOAP Fault Detail.
     *
     * @param detail a SOAP Fault Detail.
     */
    public void setDetail(SOAPFaultDetail detail) {
        this.detail = detail;
    }

    /**
     * Appends the SOAPFault Header to the SOAP Element.
     */
    void addToParent(Element bodyE) {
        Document doc = bodyE.getOwnerDocument();
        Element faultE = doc.createElementNS(SOAPBindingConstants.NS_SOAP,
                                             SOAPBindingConstants.PTAG_FAULT);
        bodyE.appendChild(faultE);

        Element faultcodeE =
                    doc.createElement(SOAPBindingConstants.TAG_FAULT_CODE);
        String localPart = faultcode.getLocalPart();
        String ns = faultcode.getNamespaceURI();
        if (Utils.debug.messageEnabled()) {
            Utils.debug.message("SOAPFault.addToParent: faultcode ns" +
                                " = " + ns + ", localPart = " + localPart);
        }
        if (ns != null && ns.length() > 0) {
            String prefix;
            if (ns.equals(SOAPBindingConstants.NS_SOAP)) {
                prefix = SOAPBindingConstants.PREFIX_SOAP;
            } else if (ns.equals(SOAPBindingConstants.NS_SOAP_BINDING)) {
                prefix = SOAPBindingConstants.PREFIX_SOAP_BINDING;
            } else {
                prefix = SOAPBindingConstants.DEFAULT_PREFIX_FAULT_CODE_VALUE;
                faultcodeE.setAttributeNS(SOAPBindingConstants.NS_XML,
                                          "xmlns:" + prefix, ns);
            }
            faultcodeE.appendChild(doc.createTextNode(prefix +":" +localPart));
        } else {
            faultcodeE.appendChild(doc.createTextNode(localPart));
        }
        faultE.appendChild(faultcodeE);

        Element faultstringE =
                    doc.createElement(SOAPBindingConstants.TAG_FAULT_STRING);
        faultstringE.appendChild(doc.createTextNode(faultstring));
        faultE.appendChild(faultstringE);

        if (faultactor != null) {
            Element faultactorE =
                    doc.createElement(SOAPBindingConstants.TAG_FAULT_ACTOR);
            faultactorE.appendChild(doc.createTextNode(faultactor));
            faultE.appendChild(faultactorE);
        }

        if (detail != null) {
            detail.addToParent(faultE);
        }
    }
}
