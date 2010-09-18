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
 * $Id: DiscoSDKUtils.java,v 1.3 2008/08/06 17:28:08 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.disco.common;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.ResourceBundle;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.liberty.ws.disco.DiscoveryException;
import com.sun.identity.liberty.ws.common.Status;
import com.sun.identity.liberty.ws.soapbinding.Utils;

/**
 * Provides utility methods to discovery service.
 */
public class DiscoSDKUtils {

    private static
        com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory discoFac
        = new com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory();
    private static
        com.sun.identity.liberty.ws.disco.jaxb11.ObjectFactory disco11Fac
        = new com.sun.identity.liberty.ws.disco.jaxb11.ObjectFactory();
    private static
        com.sun.identity.liberty.ws.disco.plugins.jaxb.ObjectFactory entryFac
        = new com.sun.identity.liberty.ws.disco.plugins.jaxb.ObjectFactory();
    private static JAXBContext jc;

    /**
     * <code>ResourceBundle</code> object for discovery service.
     */
    public static ResourceBundle bundle = Locale.getInstallResourceBundle(
                                                        "libDisco");

    /**
     * <code>Debug</code> object for discovery service.
     */
    public static Debug debug = Debug.getInstance("libIDWSF");

    /**
     * Key for offerings.
     */
    public static final String OFFERINGS = "offerings";

    /**
     * Key for credentials.
     */
    public static final String CREDENTIALS = "credentials";

    /**
     * Key for credential objects.
     */
    public static final String CREDENTIALS_OBJ = "credentialsObj";

    /**
     * Key for <code>ResourceAccessStatement</code>.
     */
    public static final String RES_STMT = "ResourceAccess";

    /**
     * Key for <code>ResourceAccessStatement</code> with
     * <code>SessionContext</code>.
     */
    public static final String RES_SESSION_STMT = "ResourceAccess_Session";

    /**
     * Key for <code>SessionContextStatement</code>.
     */
    public static final String SESSION_STMT = "SessionContext";

    /**
     * Key for <code>AuthenticationStatement</code>.
     */
    public static final String AUTHN_STMT = "Authentication";

    static {
        try {
            jc = JAXBContext.newInstance(Utils.getJAXBPackages());
        } catch (Exception e) {
            debug.error("DiscoUtils:static: Initialization failed.", e);
        }
     }

    /**
     * Constructor
     * iPlanet-PRIVATE-DEFAULT-CONSTRUCTOR
     */
    protected DiscoSDKUtils() {
    }

    /**
     * Gets discovery service object factory.
     * @return discovery service object factory.
     */
    public static com.sun.identity.liberty.ws.disco.jaxb.ObjectFactory
        getDiscoFactory()
    {
        return discoFac;
    }

    /**
     * Gets discovery service jaxb11 object factory.
     * @return discovery service v1.1 object factory.
     */
    public static com.sun.identity.liberty.ws.disco.jaxb11.ObjectFactory
        getDisco11Factory()
    {
        return disco11Fac;
    }

    /**
     * Gets Disco Entry object factory.
     * @return object factory for <code>DiscoEntry</code>.
     */
    public static com.sun.identity.liberty.ws.disco.plugins.jaxb.ObjectFactory
         getDiscoEntryFactory()
    {
        return entryFac;
    }

    /**
     * Gets marshaller.
     * @return marshaller for discovery service.
     */
    public static Marshaller getDiscoMarshaller() throws JAXBException {
        return jc.createMarshaller();
    }

    /**
     * Gets unmarshaller.
     * @return unmarshaller for discovery service.
     */
    public static Unmarshaller getDiscoUnmarshaller() throws JAXBException {
        return jc.createUnmarshaller();
    }


    /**
     * Parses Status element.
     * @param elem Status element.
     * @return Status object.
     * @exception DiscoveryException if error occurs.
     */
    public static Status parseStatus(org.w3c.dom.Element elem)
                throws DiscoveryException
    {
        if(elem == null) {
           debug.message("DiscoUtils.parseStatus: nullInput");
           throw new DiscoveryException(bundle.getString("nullInput"));
        }

        String nameSpaceURI = elem.getNamespaceURI();
        String prefix = elem.getPrefix();
        Status status = new Status(nameSpaceURI, prefix);
        String code = elem.getAttribute("code");
        if ((code == null) || (code.length() == 0)) {
            debug.message("DiscoUtils.parseStatus: missing status code.");
            throw new DiscoveryException(bundle.getString("missingStatusCode"));
        }

        String codeNS = nameSpaceURI;
        String codePrefix = prefix;
        String localPart = code;
        if(code.indexOf(":") != -1) {
            StringTokenizer st = new StringTokenizer(code, ":");
            if (st.countTokens() != 2) {
                debug.message("DiscoUtils.parseStatus: wrong status code.");
                throw new DiscoveryException(bundle.getString("wrongInput"));
            }
            codePrefix = st.nextToken();
            localPart = st.nextToken();
        }
        if ((codePrefix != null) && (prefix != null) &&
            (!codePrefix.equals(prefix)))
        {
            codeNS = elem.getAttribute("xmlns:" + codePrefix);
        }
        if ((codeNS != null) && (codeNS.length() != 0)) {
            if ((codePrefix != null) && (codePrefix.length() != 0)) {
                status.setCode(new QName(codeNS, localPart, codePrefix));
            } else {
                status.setCode(new QName(codeNS, localPart));
            }
        } else {
            status.setCode(new QName(localPart));
        }

        status.setComment(elem.getAttribute("comment"));
        status.setRef(elem.getAttribute("ref"));
        List subStatusL = XMLUtils.getElementsByTagNameNS1(
                elem, DiscoConstants.DISCO_NS, "Status");
        int num = subStatusL.size();
        if (num != 0) {
            if (num == 1) {
                status.setSubStatus(parseStatus((Element) subStatusL.get(0)));
            } else {
                if (debug.messageEnabled()) {
                    debug.message("DiscoUtils.parseStatus: included more than "
                        + "one sub status.");
                }
                throw new DiscoveryException(bundle.getString("moreElement"));
            }
        }
 
        return status;
    }

    /**
     * Parses Options element.
     * @param child Options element.
     * @return List of Option strings.
     * @exception DiscoveryException if error occurs.
     */
    public static List parseOptions(org.w3c.dom.Element child)
                throws DiscoveryException
    {
        List options = new ArrayList();
        NodeList optionnl = child.getChildNodes();
        Node option;
        String nName;
        for (int j = 0, len = optionnl.getLength(); j < len; j++) {
            option = optionnl.item(j);
            if ((nName = option.getLocalName()) != null) {
                String nameSpaceURI = ((Element) child).getNamespaceURI();
                if ((nameSpaceURI == null) ||
                    (!nameSpaceURI.equals(DiscoConstants.DISCO_NS)))
                {
                    if (debug.messageEnabled()) {
                        debug.message("DiscoUtils.parseOption("
                            + "Element): invalid namespace for node " + nName);
                    }
                    throw new DiscoveryException(
                        bundle.getString("wrongInput"));
                }
                if (nName.equals("Option")) {
                    options.add(XMLUtils.getElementValue((Element) option));
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("DiscoUtils.parseOption("
                            + "Element): invalid node" + nName);
                    }
                    throw new DiscoveryException(
                        bundle.getString("wrongInput"));
                }
            }
        }
        return options;
    }

    /**
     * Obtains DOM Element from an xml String.
     * @param xmlString String format of an element.
     * @return DOM Element
     * @exception DiscoveryException if error occurs.
     */
    public static Element parseXML(String xmlString) throws DiscoveryException {
        try {
            debug.message("DiscoUtils.parseXML: xmlString=" + xmlString);
            Document doc = XMLUtils.toDOMDocument(xmlString, debug);
            return doc.getDocumentElement();
        } catch (Exception ex) {
            debug.error("DiscoUtils.parseXML: Parsing error.", ex);
            throw new DiscoveryException(ex);
        }
    }
}
