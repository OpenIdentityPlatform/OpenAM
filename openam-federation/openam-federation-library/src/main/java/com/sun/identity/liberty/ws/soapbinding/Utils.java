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
 * $Id: Utils.java,v 1.9 2008/11/10 22:56:59 veiming Exp $
 *
 * Portions Copyright 2013-2016 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.soapbinding; 

import static org.forgerock.openam.utils.Time.*;

import java.io.ByteArrayInputStream;

import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.NotIdentifiableEvent;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MimeHeaders;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.liberty.ws.util.ProviderManager;
import com.sun.identity.liberty.ws.util.ProviderUtil;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class contains utility methods.
 *
 * @supported.api
 */

public class Utils {
    static final String NAMESPACE_PREFIX_MAPPING_LIST_PROP =
                "com.sun.identity.liberty.ws.jaxb.namespacePrefixMappingList";
    static final String JAXB_PACKAGE_LIST_PROP =
                "com.sun.identity.liberty.ws.jaxb.packageList";


    static final String DEFAULT_JAXB_PACKAGES =
        "com.sun.identity.liberty.ws.common.jaxb.soap:" +
        "com.sun.identity.liberty.ws.common.jaxb.assertion:" +
        "com.sun.identity.liberty.ws.common.jaxb.protocol:" +
        "com.sun.identity.liberty.ws.common.jaxb.ac:" +
        "com.sun.identity.liberty.ws.disco.jaxb:" +
        "com.sun.identity.liberty.ws.disco.jaxb11:" +
        "com.sun.identity.liberty.ws.disco.plugins.jaxb:" +
        "com.sun.identity.liberty.ws.interaction.jaxb:" +
        "com.sun.identity.liberty.ws.meta.jaxb:" +
        "com.sun.identity.liberty.ws.paos.jaxb:" +
        "com.sun.identity.liberty.ws.common.jaxb.ps:" +
        "com.sun.identity.liberty.ws.common.jaxb.security:" +
        "com.sun.identity.liberty.ws.soapbinding.jaxb:" +
        "com.sun.identity.liberty.ws.soapbinding.jaxb11:" +
        "com.sun.identity.liberty.ws.idpp.jaxb:" +
        "com.sun.identity.liberty.ws.idpp.plugin.jaxb:" +
        "com.sun.identity.liberty.ws.common.jaxb.secext:" +
        "com.sun.identity.liberty.ws.common.jaxb.utility:" +
        "com.sun.identity.liberty.ws.common.jaxb.xmlenc:" +
        "com.sun.identity.liberty.ws.common.jaxb.xmlsig";

    static com.sun.identity.liberty.ws.common.jaxb.soap.ObjectFactory soapOF =
        new com.sun.identity.liberty.ws.common.jaxb.soap.ObjectFactory();

    static com.sun.identity.liberty.ws.soapbinding.jaxb.ObjectFactory soapBOF =
        new com.sun.identity.liberty.ws.soapbinding.jaxb.ObjectFactory();

    static com.sun.identity.liberty.ws.common.jaxb.secext.ObjectFactory secOF =
        new com.sun.identity.liberty.ws.common.jaxb.secext.ObjectFactory();

    static final QName FAULT_CODE_SERVER =
                             new QName(SOAPBindingConstants.NS_SOAP, "Server");
    static String faultStringServerError = null;
    static Debug debug = null;
    public static ResourceBundle bundle = null;

    static MessageFactory messageFactory = null;

    static HashMap nsPrefix = new HashMap();
    static String jaxbPackages = null;
    static JAXBContext jc = null;

    static final String STALE_TIME_LIMIT_PROP =
                "com.sun.identity.liberty.ws.soap.staleTimeLimit";
    static int stale_time_limit = 300000; // millisec

    static final String SUPPORTED_ACTORS_PROP =
               "com.sun.identity.liberty.ws.soap.supportedActors";
    static final String LIBERTY_WSF_VERSION =
        "com.sun.identity.liberty.wsf.version";
    static Set supportedActors = new HashSet();

    static final String MESSAGE_ID_CACHE_CLEANUP_INTERVAL_PROP =
        "com.sun.identity.liberty.ws.soap.messageIDCacheCleanupInterval";
    static int message_ID_cleanup_interval = 60000; // millisec    
    private static Map messageIDMap = null;

    static {
        bundle = Locale.getInstallResourceBundle("libSOAPBinding");
        faultStringServerError = bundle.getString("ServerError");
        debug = Debug.getInstance("libIDWSF");

        try {
            messageFactory = MessageFactory.newInstance();
        } catch (Exception ex) {
            debug.error("Utils.static: Unable to create SOAP Message Factory",
                        ex);
        }

        String tmpNSPre =
                  SystemPropertiesManager.get(NAMESPACE_PREFIX_MAPPING_LIST_PROP);
        if (tmpNSPre != null && tmpNSPre.length() > 0) {
            StringTokenizer stz = new StringTokenizer(tmpNSPre, "|");
            while(stz.hasMoreTokens()) {
                String token = stz.nextToken().trim();
                int index = token.indexOf('=');
                if (index != -1 && index != 0 && index != token.length() - 1) {
                    String prefix = token.substring(0, index);
                    String ns = token.substring(index + 1);
                    if (debug.messageEnabled()) {
                        debug.message("Utils.static: add ns = " + ns +
                                      ", prefix = " + prefix);
                    }
                    nsPrefix.put(ns, prefix);
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning("Utils.static: Invalid syntax " +
                                      "for Namespace Prefix Mapping List: " +
                                      token);
                    }
                }
            }          
        }

        String tmpJaxbPkgs = SystemPropertiesManager.get(JAXB_PACKAGE_LIST_PROP);
        if (tmpJaxbPkgs != null && tmpJaxbPkgs.length() > 0) {
            jaxbPackages = DEFAULT_JAXB_PACKAGES + ":" + tmpJaxbPkgs;
        } else {
            jaxbPackages = DEFAULT_JAXB_PACKAGES;
        }
        if (debug.messageEnabled()) {
            debug.message("Utils.static: jaxbPackages = " + jaxbPackages);
        }

        try {
            jc = JAXBContext.newInstance(jaxbPackages);
        } catch (JAXBException jaxbe) {
            Utils.debug.error("Utils.static:", jaxbe);
        }

        String tmpstr = SystemPropertiesManager.get(STALE_TIME_LIMIT_PROP);
        if (tmpstr != null) {
            try {
                stale_time_limit = Integer.parseInt(tmpstr);
            } catch (Exception ex) {
                if (debug.warningEnabled()) {
                    debug.warning("Utils.static: Unable to get stale time " +
                                  "limit. Default value will be used");
                }
            }
        }

        tmpstr = SystemPropertiesManager.get(SUPPORTED_ACTORS_PROP);
        if (tmpstr != null) {
            StringTokenizer stz = new StringTokenizer(tmpstr, "|");
            while(stz.hasMoreTokens()) {
                String token = stz.nextToken();
                if (token.length() > 0) {
                    supportedActors.add(token);
                }
	    }
	}
        tmpstr =
            SystemPropertiesManager.get(MESSAGE_ID_CACHE_CLEANUP_INTERVAL_PROP);
        if (tmpstr != null) {
            try {
                message_ID_cleanup_interval = Integer.parseInt(tmpstr);
            } catch (Exception ex) {
                if (debug.warningEnabled()) {
                    debug.warning("Utils.CleanUpThread.static: Unable to" +
                            " get stale time limit. Default value " +
                            "will be used");
                }
            }
        }
        messageIDMap = new PeriodicCleanUpMap(
            message_ID_cleanup_interval, stale_time_limit);
        SystemTimerPool.getTimerPool().schedule((TaskRunnable) messageIDMap,
            new Date(((currentTimeMillis() + message_ID_cleanup_interval)
            / 1000) * 1000));
    }

    /**
     * Returns JAXB namespace prefix mapping. Key is the namespace and value
     * is the prefix.
     *
     * @return a Map of JAXB namespace prefix mapping
     * @supported.api
     */
    static public Map getNamespacePrefixMapping() {
        return nsPrefix;
    }

    /**
     * Returns a String of JAXB packages seperated by ":".
     *
     * @return a String of JAXB packages seperated by ":".
     * @supported.api
     */
    public static String getJAXBPackages() {
        return jaxbPackages;
    }

    /**
     * Converts Document to SOAPMessage
     *
     * @param doc the source Document
     * @return SOAPMessage
     * @throws SOAPBindingException if an error occurs while converting
     *         the document
     * @supported.api
     */
    public static SOAPMessage DocumentToSOAPMessage(Document doc)
    throws SOAPBindingException {
        SOAPMessage msg = null;
        try {
            MimeHeaders mimeHeaders = new MimeHeaders();
            mimeHeaders.addHeader("Content-Type", "text/xml");
            
            String xmlstr = XMLUtils.print(doc);
            if (debug.messageEnabled()) {
                debug.message("Utils.DocumentToSOAPMessage: xmlstr = " +
                        xmlstr);
            }
            msg = messageFactory.createMessage(
                    mimeHeaders,
                    new ByteArrayInputStream(
                    xmlstr.getBytes(SOAPBindingConstants.DEFAULT_ENCODING)));
        } catch (Exception e) {
            debug.error("Utils.DocumentToSOAPMessage", e);
            throw new SOAPBindingException(e.getMessage());
        }
        return msg;
    }

    /**
     * Converts a list of JAXB objects to a list of 
     * <code>org.w3c.dom.Element</code>
     *
     * @param jaxbObjs a list of JAXB objects
     * @return a list of <code>org.w3c.dom.Element</code>
     * @throws JAXBException if an error occurs while converting JAXB objects.
     *
     * @supported.api
     */
    public static List convertJAXBToElement(List jaxbObjs) 
                                            throws JAXBException{
        List result = new ArrayList();
        if (jaxbObjs != null && !jaxbObjs.isEmpty()) {
            Iterator iter = jaxbObjs.iterator();
            while(iter.hasNext()) {
                result.add(convertJAXBToElement(iter.next()));
            }
        }
        return result;
    }

    /**
     * Converts a JAXB object to a <code>org.w3c.dom.Element</code>.
     *
     * @param jaxbObj a JAXB object
     * @return a <code>org.w3c.dom.Element</code>
     * @throws JAXBException if an error occurs while converting JAXB object.
     * @supported.api
     */
    public static Element convertJAXBToElement(Object jaxbObj)
                          throws JAXBException {
        Marshaller m = jc.createMarshaller();
        try {
            m.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                    new NamespacePrefixMapperImpl());
        } catch(PropertyException ex) {
            debug.error("Utils.convertJAXBToElement", ex);
        }
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
        } catch (Exception ex) {
            debug.error("Utils.convertJAXBToElement:", ex);
        }
        m.marshal(jaxbObj, doc);
        return doc.getDocumentElement();
    }

    /**
     * Converts a JAXB object to a <code>org.w3c.dom.Element</code>.
     *
     * @param jaxbObj a JAXB object
     * @return a <code>org.w3c.dom.Element</code>
     * @throws JAXBException if an error occurs while converting JAXB object.
     * @supported.api
     */
    public static Element convertJAXBToElement(Object jaxbObj,
            boolean checkIdref) throws JAXBException {
        Marshaller m = jc.createMarshaller();
        try {
            m.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                    new NamespacePrefixMapperImpl());
            
        } catch(PropertyException ex) {
            debug.error("Utils.convertJAXBToElement", ex);
        }
        
        if (!checkIdref) {
            m.setEventHandler(
                    new DefaultValidationEventHandler() {
                public boolean handleEvent(ValidationEvent event) {
                    if (event instanceof NotIdentifiableEvent) {
                        return true;
                    }
                    return super.handleEvent(event);
                }
            });
        }
        
        Document doc = null;
        try {
            doc = XMLUtils.newDocument();
        } catch (Exception ex) {
            debug.error("Utils.convertJAXBToElement:", ex);
        }
        m.marshal(jaxbObj, doc);
        return doc.getDocumentElement();
    }

    /**
     * Converts a list of <code>org.w3c.dom.Element</code> to a list of 
     * JAXB objects.
     *
     * @param elements a list of <code>org.w3c.dom.Element</code>
     * @return a list of JAXB objects
     * @throws JAXBException if an error occurs while converting
     *                          <code>org.w3c.dom.Element</code>.
     * @supported.api
     */
    public static List convertElementToJAXB(List elements) 
                                            throws JAXBException{
        List result = new ArrayList();
        if (elements != null && !elements.isEmpty()) {
            Iterator iter = elements.iterator();
            while(iter.hasNext()) {
                result.add(convertElementToJAXB((Element)iter.next()));
            }
        }
        return result;
    }

    /**
     * Converts a <code>org.w3c.dom.Element</code> to a JAXB object.
     *
     * @param element a <code>org.w3c.dom.Element</code>.
     * @return a JAXB object
     * @throws JAXBException if an error occurs while converting
     *                          <code>org.w3c.dom.Element</code>
     * @supported.api
     */
    public static Object convertElementToJAXB(Element element)
                         throws JAXBException {
        Unmarshaller u = jc.createUnmarshaller();
        return u.unmarshal(element);
    }

    /**
     * Converts a value of XML boolean type to Boolean object.
     *
     * @param str a value of XML boolean type
     * @return a Boolean object
     * @throws Exception if there is a syntax error
     * @supported.api
     */
    public static Boolean StringToBoolean(String str) throws Exception {
        if (str == null) {
            return null;
        }
        
        if (str.equals("true") || str.equals("1")) {
            return Boolean.TRUE;
        }
        
        if (str.equals("false") || str.equals("0")) {
            return Boolean.FALSE;
        }
        
        throw new Exception();
    }

    /**
     * Converts a Boolean object to a String representing XML boolean.
     *
     * @param bool a Boolean object.
     * @return a String representing the boolean value.
     * @supported.api
     */
    public static String BooleanToString(Boolean bool) {
        if (bool == null) {
            return "";
        }
        
        return bool.booleanValue() ? "1" : "0";
    }

    /**
     * Converts a string value to a QName. The prefix of the string value
     * is resolved to a namespace relative to the element.
     *
     * @param str the String to be converted.
     * @param element the Element object.
     * @return the QName Object.
     * @supported.api
     */
    public static QName convertStringToQName(String str,Element element) {
        if (str == null) {
            return null;
        }
        
        String prefix = "";
        String localPart;
        int index = str.indexOf(":");
        if (index == -1) {
            localPart = str;
        } else {
            prefix = str.substring(0, index);
            localPart = str.substring(index + 1);
        }
        String ns = getNamespaceForPrefix(prefix, element);
        return new QName(ns, localPart);
    }


    /**
     *  Gets the XML namespace URI that is mapped to the specified prefix, in
     *  the context of the DOM element e
     *
     * @param  prefix  The namespace prefix to map
     * @param  e       The DOM element in which to calculate the prefix binding
     * @return         The XML namespace URI mapped to prefix in the context of e
     */
    public static String getNamespaceForPrefix(String prefix, Element e) {
        return e.lookupNamespaceURI(prefix);
    }

    /**
     * Enforces message processiong rules defined in the spec.
     *
     * @param message a message
     * @param requestMessageID the request messageID if we are checking a
     *                         response message or null if we are checking a
     *                         request message
     * @param isServer true if this is a server
     * @throws SOAPBindingException if the message violates rules on client.
     * @throws SOAPFaultException if the message violates rules on server.
     */
    public static void enforceProcessingRules(Message message, String requestMessageID,
            boolean isServer)
            throws SOAPBindingException, SOAPFaultException {
        CorrelationHeader corrH = message.getCorrelationHeader();
        String messageID = corrH.getMessageID();
        checkCorrelationHeader(corrH, requestMessageID, isServer);
        checkProviderHeader(message.getProviderHeader(), messageID, isServer);
        checkProcessingContextHeader(message.getProcessingContextHeader(),
                messageID, isServer);
        checkConsentHeader(message.getConsentHeader(), messageID, isServer);
        List usagHs = message.getUsageDirectiveHeaders();
        if (usagHs != null && !usagHs.isEmpty()) {
            Iterator iter = usagHs.iterator();
            while(iter.hasNext()) {
                UsageDirectiveHeader usagH = (UsageDirectiveHeader)iter.next();
                checkUsageDirectiveHeader(usagH, messageID, isServer);
            }
        }
    }
    
    /**
     * Enforces message Correlation header processiong rules defined
     * in the spec.
     *
     * @param  corrH a Correlation header
     * @param  requestMessageID the request messageID if we are checking a
     *                         response message or null if we are checking a
     *                         request message
     * @param  isServer true if this is a server
     * @throws SOAPBindingException if the Correlation header violates rules
     *         on client side
     * @throws SOAPFaultException if the Correlation header violates rules
     *                               on server side
     */
    static void checkCorrelationHeader(CorrelationHeader corrH,
            String  requestMessageID, boolean isServer)
            throws SOAPBindingException, SOAPFaultException {
        if (corrH == null) {
            if (isServer) {
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                        faultStringServerError, null,
                        new SOAPFaultDetail(
                        SOAPFaultDetail.ID_STAR_MSG_NOT_UNSTD,null,null));
                throw new SOAPFaultException(new Message(sf));
            } else {
                throw new SOAPBindingException(
                        bundle.getString("CorrelationHeaderNull"));
            }
        }
        
        String messageID = corrH.getMessageID();
        
        try {
            checkActorAndMustUnderstand(corrH.getActor(),
                    corrH.getMustUnderstand(),
                    messageID, isServer);
        } catch (SOAPFaultException sfe) {
            sfe.getSOAPFaultMessage().getSOAPFault().getDetail()
            .setCorrelationHeader(corrH);
            throw sfe;
        }
        
        Date timestamp = corrH.getTimestamp();
        Date now = newDate();
        if ((now.getTime() - timestamp.getTime()) > stale_time_limit) {
            if (isServer) {
                SOAPFaultDetail sfd =
                        new SOAPFaultDetail(SOAPFaultDetail.STALE_MSG,
                        messageID, null);
                sfd.setCorrelationHeader(corrH);
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                        faultStringServerError, null, sfd);
                throw new SOAPFaultException(new Message(sf));
                
            } else {
                throw new SOAPBindingException(bundle.getString("staleMsg"));
            }
        }
        
        Long prevMsgIDTime = (Long)messageIDMap.get(messageID);
        long currentTime = currentTimeMillis();
        if (prevMsgIDTime != null &&
                currentTime - prevMsgIDTime.longValue() < stale_time_limit) {
            
            if (isServer) {
                SOAPFaultDetail sfd =
                        new SOAPFaultDetail(SOAPFaultDetail.DUPLICATE_MSG,
                        messageID, null);
                sfd.setCorrelationHeader(corrH);
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                        faultStringServerError, null, sfd);
                throw new SOAPFaultException(new Message(sf));
                
            } else {
                throw new SOAPBindingException(bundle.getString("dupMsg"));
            }
        } else {
            synchronized (messageIDMap) {
                if (debug.messageEnabled()) {
                    debug.message("Utils.checkCorrelationHeader: adding " +
                            "messageID: " + messageID);
                }
                messageIDMap.put(messageID, new Long(currentTime));
            }
        }
        
        String refToMessageID = corrH.getRefToMessageID();
        if (refToMessageID != null && requestMessageID != null &&
                !refToMessageID.equals(requestMessageID)) {
            
            if (isServer) {
                SOAPFaultDetail sfd =
                        new SOAPFaultDetail(
                        SOAPFaultDetail.INVALID_REF_TO_MSG_ID,messageID, null);
                sfd.setCorrelationHeader(corrH);
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                        faultStringServerError, null, sfd);
                throw new SOAPFaultException(new Message(sf));
                
            } else {
                throw new SOAPBindingException(bundle.getString("invalidRef"));
            }
        }
    }

    /**
     * Enforces message Provider header processing rules defined
     * in the spec.
     *
     * @param provH a Correlation header
     * @param messageID the messageID in Correlation header
     * @param isServer true if this is a server
     * @throws SOAPBindingException if the Provider header violates rules
     *                                 on client side
     * @throws SOAPFaultException if the Provider header violates rules
     *                               on server side
     */
    static void checkProviderHeader(ProviderHeader provH,
            String messageID,boolean isServer)
            throws SOAPBindingException, SOAPFaultException {

        if (provH == null) {
            return;
        }
        
        try {
            checkActorAndMustUnderstand(provH.getActor(),
                    provH.getMustUnderstand(),
                    messageID, isServer);
        } catch (SOAPFaultException sfe) {
            sfe.getSOAPFaultMessage().getSOAPFault().getDetail()
            .setProviderHeader(provH);
            throw sfe;
        }
        
        if (isServer && SOAPBindingService.enforceOnlyKnownProviders()) {
            String providerID = provH.getProviderID();
            ProviderManager providerManager = ProviderUtil.getProviderManager();

            if (!providerManager.containsProvider(providerID)) {
                SOAPFaultDetail sfd = new SOAPFaultDetail(
                    SOAPFaultDetail.PROVIDER_ID_NOT_VALID, messageID, null);
                sfd.setProviderHeader(provH);
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                    faultStringServerError, null, sfd);
                throw new SOAPFaultException(new Message(sf));
            }
            
            String affID = provH.getAffiliationID();
            if ((affID != null) &&
                (!providerManager.isAffiliationMember(providerID, affID))) {

                SOAPFaultDetail sfd = new SOAPFaultDetail(
                    SOAPFaultDetail.AFFILIATION_ID_NOT_VALID, messageID, null);
                sfd.setProviderHeader(provH);
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                    faultStringServerError, null, sfd);
                throw new SOAPFaultException(new Message(sf));
            }
        }
    }

    /**
     * Enforces message Processing Context header processiong rules defined
     * in the spec.
     * @param procH a Processing Context header
     * @param messageID the messageID in Correlation header
     * @param isServer true if this is a server
     * @throws SOAPBindingException if the Processing Context header
     *                                 violates rules on client side
     * @throws SOAPFaultException if the Processing Context header violates
     *                               rules on server side
     */
    static void checkProcessingContextHeader(ProcessingContextHeader procH,
            String messageID, boolean isServer)
            throws SOAPBindingException, SOAPFaultException {
        if (procH == null) {
            return;
        }

        try {
            checkActorAndMustUnderstand(procH.getActor(),
                                        procH.getMustUnderstand(),
                                        messageID, isServer);
        } catch (SOAPFaultException sfe) {
            sfe.getSOAPFaultMessage().getSOAPFault().getDetail()
               .setProcessingContextHeader(procH);
            throw sfe;
        }

        if (isServer) {
            SOAPFaultDetail sfd =
                    new SOAPFaultDetail(SOAPFaultDetail.PROC_CTX_URI_NOT_UNSTD,
                                        messageID, null);
            sfd.setProcessingContextHeader(procH);
            SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                                         faultStringServerError, null, sfd);
            throw new SOAPFaultException(new Message(sf));
        } else {
            throw new SOAPBindingException(
                            bundle.getString("ProcessingContextUnsupported"));
        }
    }

    /**
     * Enforces message Consent header processiong rules defined in the spec.
     *
     * @param consH a Consent header
     * @param messageID the messageID in Correlation header
     * @param isServer true if this is a server
     * @throws SOAPBindingException if the Consent header violates rules on
     *                                 client side
     * @throws SOAPFaultException if the Consent header violates rules
     *                               on server side
     */
    static void checkConsentHeader(ConsentHeader consH,String messageID,
            boolean isServer) throws SOAPBindingException, SOAPFaultException {
        if (consH == null) {
            return;
        }
        
        try {
            checkActorAndMustUnderstand(consH.getActor(),
                    consH.getMustUnderstand(),
                    messageID, isServer);
        } catch (SOAPFaultException sfe) {
            sfe.getSOAPFaultMessage().getSOAPFault().getDetail()
            .setConsentHeader(consH);
            throw sfe;
        }
    }

    /**
     * Enforces message Usage Directive header processiong rules defined in
     * the spec.
     *
     * @param usagH a Usage Directive header
     * @param messageID the messageID in Correlation header
     * @param isServer true if this is a server
     * @throws SOAPBindingException if the Usage Directive header violates
     *                                 rules on client side
     * @throws SOAPFaultException if the Usage Directive header violates
     *                               rules on server side
     */
    static void checkUsageDirectiveHeader(UsageDirectiveHeader usagH,
            String messageID,boolean isServer)
            throws SOAPBindingException, SOAPFaultException {
        if (usagH == null) {
            return;
        }
        
        try {
            checkActorAndMustUnderstand(usagH.getActor(),
                    usagH.getMustUnderstand(),
                    messageID, isServer);
        } catch (SOAPFaultException sfe) {
            List usagHs = new ArrayList();
            usagHs.add(usagH);
            sfe.getSOAPFaultMessage().getSOAPFault().getDetail()
            .setUsageDirectiveHeaders(usagHs);
            throw sfe;
        }
    }

    /**
     * Checks 'actor' and 'mustUnderstand' attribute of a header.
     *
     * @param actor the value of 'actor' attribute of a header.
     * @param mustUnderstand the value of 'mustUnderstand' attribute of a
     *                       header.
     * @param messageID the messageID in Correlation header.
     * @param isServer true if this is a server.
     * @throws SOAPBindingException if the actor and mustUnderstand violates
     *                                 rules on client side
     * @throws SOAPFaultException if the actor and mustUnderstand violates
     *                               rules on server side
     */
    static void checkActorAndMustUnderstand(String actor,Boolean mustUnderstand,
            String messageID,boolean isServer)
            throws SOAPBindingException, SOAPFaultException {
        if (actor != null && !supportedActors.contains(actor)) {
            if (isServer) {
                SOAPFaultDetail sfd =
                        new SOAPFaultDetail(SOAPFaultDetail.BOGUS_ACTOR,
                        messageID, null);
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                        faultStringServerError, null, sfd);
                throw new SOAPFaultException(new Message(sf));
            } else {
                throw new SOAPBindingException(
                        bundle.getString("bogusActor"));
            }
        }
        
        if (mustUnderstand != null && !mustUnderstand.booleanValue()) {
            if (isServer) {
                SOAPFaultDetail sfd =
                        new SOAPFaultDetail(SOAPFaultDetail.BOGUS_MUST_UNSTND,
                        messageID, null);
                SOAPFault sf = new SOAPFault(FAULT_CODE_SERVER,
                        faultStringServerError, null, sfd);
                throw new SOAPFaultException(new Message(sf));
            } else {
                throw new SOAPBindingException(
                        bundle.getString("bogusMustUnderstand"));
            }
        }
    }
    
    /**
     * Gets localized string from resource bundle.
     *
     * @param key a key to a resource bundle
     * @param params parameters to MessageFormat
     * @return a localized string.
     * @supported.api
     */
    public static String getString(String key, Object[] params) {
        return MessageFormat.format(bundle.getString(key), params);
    }
    
    /**
     * Returns the default web services version.
     *
     * @return the default web services version.
     */
    public static String getDefaultWSFVersion() {
        return SystemPropertiesManager.get(LIBERTY_WSF_VERSION,
            SOAPBindingConstants.WSF_11_VERSION);
    }

}
