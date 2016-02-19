/*
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
 * $Id: InteractionManager.java,v 1.5 2008/08/06 17:28:10 exu Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.interaction;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TaskRunnable;
import com.sun.identity.common.TimerPool;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.liberty.ws.common.LogUtil;
import com.sun.identity.liberty.ws.interaction.jaxb.InquiryElement;
import com.sun.identity.liberty.ws.interaction.jaxb.InteractionResponseElement;
import com.sun.identity.liberty.ws.interaction.jaxb.RedirectRequestElement;
import com.sun.identity.liberty.ws.interaction.jaxb.StatusElement;
import com.sun.identity.liberty.ws.interaction.jaxb.UserInteractionElement;
import com.sun.identity.liberty.ws.soapbinding.Client;
import com.sun.identity.liberty.ws.soapbinding.CorrelationHeader;
import com.sun.identity.liberty.ws.soapbinding.Message;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingException;
import com.sun.identity.liberty.ws.soapbinding.SOAPFault;
import com.sun.identity.liberty.ws.soapbinding.SOAPFaultDetail;
import com.sun.identity.liberty.ws.soapbinding.SOAPFaultException;
import com.sun.identity.liberty.ws.soapbinding.Utils;
import com.sun.identity.saml.common.SAMLUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;

/**
 *  This class provides the interface and implementation for supporting 
 *  resource owner interaction.  <code>WSC</code> and <code>WSP</code> would
 *  collaborate with the singleton object instance of this class to provide
 *  and use resource owner interaction.  
 *  @supported.api
 */
public class InteractionManager {

    /**
     *
     * Name of URL query parameter to be used by <code>WSC</code> to include
     * <code>returnToURL</code>, while redirecting user agent to
     * <code>WSP</code>.
     *
     * @supported.all.api
     */
    public static final String RETURN_TO_URL = "ReturnToURL";

    /**
     * Name of suggested URL query parameter to be used by <code>WSC</code>
     * to include an ID to refer to request message that led to user agent
     * redirect.
     *
     * @supported.api
     */
    public static final String REQUEST_ID = "RequestID";

    /**
     * Name of URL query parameter to be used by <code>WSC</code> to include 
     * <code>providerID</code> of <code>IDP</code>, that was used to
     * authenticate user.
     *
     * @supported.api
     */
    public static final String IDP = "IDP";

    /**
     * Name of URL query parameter to be used by <code>WSP</code> to include 
     * an ID to indicate that user agent is redirected back to
     * <code>WSC</code> from <code>WSP</code>
     *
     * @supported.api
     */
    public static final String RESEND_MESSAGE = "ResendMessage";

    /**
     * Name space URI of interaction service 
     * @supported.api
     */
    public static final String INTERACTION_NAMESPACE 
            = "urn:liberty:is:2003-08"; 

    /** 
     * <code>QName</code> for <code>s:Server</code>
     * <code>s</code> - soap name space prefix
     */
    public static final QName QNAME_SERVER
            = new QName(SOAPBindingConstants.NS_SOAP, "Server");
    /** 
     * <code>QName</code> for <code>is:interactIfNeeded</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_INTERACT_IF_NEEDED 
            = new QName(INTERACTION_NAMESPACE, "interactIfNeeded", "is");

    /** 
     * <code>QName</code> for <code>is:doNotInteract</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_DO_NOT_INTERACT
            = new QName(INTERACTION_NAMESPACE, "doNotInteract", "is");

    /** 
     * <code>QName</code> for <code>is:doNotInteractForData</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_DO_NOT_INTERACT_FOR_DATA
            = new QName(INTERACTION_NAMESPACE, "doNotInteractForData", "is");

    /** 
     * <code>QName</code> for <code>is:interactionRequired</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_INTERACTION_REQUIRED 
            = new QName(INTERACTION_NAMESPACE, "interactionRequired");

    /** 
     * <code>QName</code> for <code>is:forData</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_INTERACTION_REQUIRED_FOR_DATA 
            = new QName(INTERACTION_NAMESPACE, "forData");

    /** 
     * <code>QName</code> for <code>is:timeNotSufficient</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_INTERACTION_TIME_NOT_SUFFICEINT 
            = new QName(INTERACTION_NAMESPACE, "timeNotSufficient");

    /** 
     * <code>QName</code> for <code>is:timeOut</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_INTERACTION_TIMED_OUT 
            = new QName(INTERACTION_NAMESPACE, "timeOut");

    /** 
     * <code>QName</code> for <code>is:interactIfNeeded</code>
     * <code>is</code> - name space prefix for interaction service
     */
    public static final QName QNAME_INTERACTION_CAN_NOT_DETERMINE_REQUEST_HOST
            = new QName(INTERACTION_NAMESPACE,
                        "canNotDetermineRequestHostName");

    /** 
     * Constant string to indicate generic server error
     */
    public static final String SERVER_ERROR = "Server Error";

    static final String TRANS_ID = "TransID";

    private static InteractionManager interactionManager = null;
    private static Debug debug = Debug.getInstance("libIDWSF");

    private static final String REDIRECT_URL = "redirectURL";

    private static final String FAULT_ACTOR 
            = "http://schemas.xmlsoap.org/soap/actor/next";

    private static final String INTERACTION_RB_NAME = "libInteraction";

    private InteractionCache cache = new InteractionCache();
    private com.sun.identity.liberty.ws.interaction.jaxb.ObjectFactory 
             objectFactory;
    private InteractionConfig interactionConfig;

    /**
     * Gets singleton object instance of <code>InteractionManager</code>
     * @return singleton object instance of <code>InteractionManager</code>
     *
     * @supported.api
     */
    synchronized public static InteractionManager getInstance() {
        if (interactionManager == null) {
            interactionManager = new InteractionManager();
        }
        return interactionManager;
    }

    private InteractionManager() {
        objectFactory = JAXBObjectFactory.getObjectFactory();
        interactionConfig = InteractionConfig.getInstance();
        if (debug.messageEnabled()) {
            debug.message(
                    "InteractionManager():constructed singleton instance");
        }
    }

    /**
     * Sends SOAP request to <code>WSP</code>.  
     * This would be invoked at <code>WSC</code> side.
     *
     * @param requestMessage request message.
     * @param connectTo SOAP URL to which to send the SOAP request
     * @param certAlias SOAP Client Certificate Alias
     * @param soapAction SOAP Action Attribute
     * @param returnToURL URL to which to redirect user agent after
     *                   <code>WSP</code> - resource owner interactions
     * @param httpRequest HTTP request object of current user agent request
     * @param httpResponse HTTP response object of current user agent request
     * @return response SOAP response message sent by <code>WSP</code>.
     *
     * @throws InteractionException for generic interaction error
     * @throws InteractionRedirectException if user agent is redirected to 
     *                     <code>WSP</code> for resource owner interactions
     * @throws SOAPBindingException  for generic SOAP binding errors
     * @throws SOAPFaultException if the response message has SOAP fault
     *
     * @supported.api
     */
    public Message sendRequest(Message requestMessage, 
            String connectTo,
            String certAlias,
            String soapAction,
            String returnToURL,
            HttpServletRequest httpRequest, 
            HttpServletResponse httpResponse) 
            throws InteractionException, InteractionRedirectException, 
            SOAPBindingException, SOAPFaultException  {

        if (debug.messageEnabled()) {
            debug.message("InteractionManager.sendRequest():"
                    + " entering with messageID="
                    + requestMessage.getCorrelationHeader().getMessageID()
                    + ":refToMessageID="
                    + requestMessage.getCorrelationHeader().getRefToMessageID()
                    + ":requestMessage=" + requestMessage);
        }

        // construct and set UserInteraction element in requestMessage
        if (interactionConfig.wscIncludesUserInteractionHeader()) {
            Enumeration locales = httpRequest.getLocales();
            List acceptLanguages = new ArrayList();
            while (locales.hasMoreElements()) {
                acceptLanguages.add(locales.nextElement().toString());
            }
            if (debug.messageEnabled()) {
                debug.message("InteractionManager.sendRequest():"
                        + "Accept-Language specified by httpRequest="
                        + acceptLanguages);
            }
            UserInteractionElement ue = createUserInteractionElement(
                    acceptLanguages);
            String id = SAMLUtils.generateID();
            ue.setId(id);
            if (ue != null) {
                try {
                    Element element = Utils.convertJAXBToElement(ue);
                    requestMessage.setOtherSOAPHeader(
                            element,
                            id);

                } catch (JAXBException je) {
                    debug.error("InteractionManager.sendRequest():"
                    + "not setting userInteractionHeader:"
                    + "can not convert JAXBObject to Element", je);
                } 
            }
        }

        Message responseMessage = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("InteractionManager.sendRequest():"
                        + "invoking soap Client.sendRequest():"
                        + ":requestMessage=" + requestMessage
                        + ":connecTo=" + connectTo);
            }
            if (LogUtil.isLogEnabled()) {
                String[] objs =new String[1];
                objs[0] = requestMessage.getCorrelationHeader()
                        .getMessageID();
                LogUtil.access(Level.INFO, LogUtil.IS_SENDING_MESSAGE,objs);
            }
            responseMessage = Client.sendRequest(requestMessage, 
                connectTo, certAlias, soapAction);
        } catch (SOAPFaultException sfe) {
            if (debug.messageEnabled()) {
                debug.message("InteractionManager.sendRequest():"
                        + " catching SOAPFaultException="
                        + sfe);
            }
            String redirectURL = getRedirectURL(sfe);
            if(redirectURL == null) {
               throw sfe;
            }
            String responseID = getResponseID(sfe);
            responseMessage = handleRedirectRequest(requestMessage, 
                    redirectURL, responseID, connectTo, certAlias, soapAction,
                    returnToURL, httpRequest, httpResponse);
        }
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.sendRequest():"
                    + " returning response message=" + responseMessage);
        }
        if (LogUtil.isLogEnabled()) {
            String[] objs = new String[2];
            objs[0] = responseMessage.getCorrelationHeader().getMessageID();
            objs[1] = requestMessage.getCorrelationHeader().getMessageID();
            LogUtil.access(Level.INFO,LogUtil.IS_RETURNING_RESPONSE_MESSAGE,
                           objs);
        }
        return responseMessage;
    }

    /**
     * Resends a previously cached SOAP request message to <code>WSP</code>.
     * This would be invoked at <code>WSC</code> side. Message ID for the cached
     * message should be provided as value of <code>REQUEST_ID</code> query
     * parameter in <code>httpRequest</code>.
     *
     * @param returnToURL URL to which to redirect user agent after
     *        <code>WSP</code> - resource owner interactions
     * @param httpRequest HTTP request object of current user agent request
     * @param httpResponse HTTP response object of current user agent request
     * @return response SOAP message sent by <code>WSP</code>.
     *
     * @throws InteractionException for generic interaction error
     * @throws InteractionRedirectException if user agent is redirected to 
     *         <code>WSP</code> for resource owner interactions
     * @throws SOAPBindingException if there are generic SOAP errors
     * @throws SOAPFaultException if the response message has SOAP fault
     *
     * @see #REQUEST_ID
     *
     * @supported.api
     */
    public Message resendRequest(String returnToURL,
            HttpServletRequest httpRequest,  
            HttpServletResponse httpResponse) 
            throws InteractionRedirectException, InteractionException, 
            SOAPBindingException, SOAPFaultException {

        return resendRequest(returnToURL, httpRequest, httpResponse, null);
    }

    /**
     * Resends a SOAP request message to <code>WSP</code>.
     * This would be invoked at <code>WSC</code> side.
     *
     * @param returnToURL URL to which to redirect user agent after
     *                    <code>WSP</code> - resource owner interactions
     * @param httpRequest HTTP request object of current user agent request
     * @param httpResponse HTTP response object of current user agent request
     * @param requestMessage SOAP message to be resent.
     * @return response SOAP message sent by <code>WSP</code>.
     *
     * @throws InteractionException for generic interaction error
     * @throws InteractionRedirectException if user agent is redirected to 
     *                     <code>WSP</code> for resource owner interactions
     * @throws SOAPBindingException  for generic SOAP errors
     * @throws SOAPFaultException if the response message has SOAP fault
     *
     * @supported.api
     */
    public Message resendRequest(String returnToURL,
            HttpServletRequest httpRequest,  
            HttpServletResponse httpResponse, Message requestMessage) 
            throws InteractionRedirectException, InteractionException, 
            SOAPBindingException, SOAPFaultException {

        if (debug.messageEnabled()) {
            debug.message("InteractionManager.resendRequest():entering ");
        }

        //check for RESEND_MESSAGE parameter
        String messageID = httpRequest.getParameter(RESEND_MESSAGE);
        if (messageID == null) {
            debug.error("InteractionManager.resend():"
                    + " request without " + RESEND_MESSAGE + " in requestURL=" 
                    + httpRequest.getRequestURL().toString());
            String objs[] = {RESEND_MESSAGE};
            throw new InteractionException(INTERACTION_RB_NAME, 
                    "missing_query_parameter", objs);
        }

        //check whether WSP advised not to resend
        if ( (messageID == "0") || (messageID.equals("false")) ) {
            debug.error("InteractionManager.resend():"
                    + " resend not allowed in requestURL=" 
                    + httpRequest.getRequestURL().toString());
            throw new InteractionException(INTERACTION_RB_NAME, 
                    "wsp_advised_not_to_resend", null);
        }

        //check for original REQUEST_ID
        messageID = httpRequest.getParameter(REQUEST_ID);
        if (messageID == null) {
            debug.error("InteractionManager.resend():"
                    + " request without " +  REQUEST_ID + " in requestURL=" 
                    +  httpRequest.getRequestURL().toString());
            String[] objs = {REQUEST_ID};
            throw new InteractionException(INTERACTION_RB_NAME, 
                    "request_missing_query_parameter", objs);
        }

        String connectTo = getConnectTo(messageID);
        if (connectTo == null) {
            debug.error("InteractionManager.resend():"
                    + " old connectTo not  found for messageID=" 
                    + messageID);
            throw new InteractionException(INTERACTION_RB_NAME, 
                    "old_connectTo_not_found", null);
        }

        if (requestMessage == null) {
            if (debug.messageEnabled()) {
                debug.message("InteractionManager.resendRequest():"
                        + "invoking with null requestMessage:"
                        + "old cached message would be used");
            }
            Message oldMessage = getRequestMessage(messageID);
            if (oldMessage == null) {
                debug.error("InteractionManager.resend():"
                        + " old message not  found for messageID=" 
                        + messageID);
                throw new InteractionException(INTERACTION_RB_NAME, 
                        "old_message_not_found", null);
            }
            requestMessage = oldMessage;
        } else {
            if (debug.messageEnabled()) {
                debug.message("InteractionManager.resendRequest():"
                        + "invoking with non null requestMessage");
            }
        }

        CorrelationHeader ch = new CorrelationHeader();
        ch.setRefToMessageID(InteractionManager.getInstance()
                .getRequestMessageID(messageID));
        requestMessage.setCorrelationHeader(ch);

        if (debug.messageEnabled()) {
            debug.message("InteractionManager.resendRequest():"
                    + "invoking InteractionManager.sendRequest():"
                    + "with requestMessage=" + requestMessage
                    + ":returnToURL=" + returnToURL);
        }

        if (LogUtil.isLogEnabled()) {
            String[] objs =new String[2];
            objs[0] = messageID;
            objs[1] = ch.getMessageID();
            LogUtil.access(Level.INFO,LogUtil.IS_RESENDING_MESSAGE,objs);
        }
        Message responseMessage = sendRequest(requestMessage, connectTo, 
                getClientCert(messageID), getSoapAction(messageID),
                returnToURL, httpRequest,  httpResponse);
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.resendRequest():"
                    + " returning responseMessage=" + responseMessage);
        }
        return responseMessage;
    }

    private Message handleRedirectRequest(Message requestMessage, 
            String redirectURL, String messageID, String connectTo, 
            String certAlias, String soapAction, String returnToURL, 
            HttpServletRequest httpRequest,  HttpServletResponse httpResponse)
            throws InteractionRedirectException, InteractionException {
        
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.handleRedirectRequest():"
                    + "entering with redirectURL="
                    + redirectURL);
        }

        //redirectURL should not have ReturnToURL parameter
        if (!(redirectURL.indexOf(RETURN_TO_URL + "=") == -1)) {
            debug.error(
                    "InteractionManager.handleRedirectRequest():"
                    + "Invalid redirectURL - illegal parameter " 
                    + RETURN_TO_URL
                    + " in redirectURL=" + redirectURL); 
            String objs[] = {RETURN_TO_URL, REDIRECT_URL};
            throw new InteractionException(INTERACTION_RB_NAME,
                    "illegal_parameter_in_redirectURL", objs);
        }

        //redirectURL should not have IDP parameter
        if (!(redirectURL.indexOf(IDP + "=") == -1)) {
            debug.error(
                    "InteractionManager.handleRedirectRequest():"
                    + "Invalid redirectURL - illegal parameter:" 
                    + IDP 
                    + " in redirectURL=" + redirectURL); 
            String objs[] = {IDP, REDIRECT_URL};
            throw new InteractionException(INTERACTION_RB_NAME,
                    "illegal_parameter_in_redirectURL", objs);
        }

        //redirectURL should be https
        if (InteractionConfig.getInstance().wscEnforcesHttpsCheck()
                    && (redirectURL.indexOf("https") != 0) ) {
            debug.error(
                    "InteractionManager.handleRedirectRequest():"
                    + "Invalid Request " 
                    + InteractionManager.REDIRECT_URL
                    + " not https"
                    + " in redirectURL=" + redirectURL); 
            throw new InteractionException(INTERACTION_RB_NAME,
                    "redirectURL_not_https", null);
        }

        //redirectURL should point to connectTo host
        if (InteractionConfig.getInstance()
                    .wspEnforcesReturnToHostEqualsRequestHost()
                    && !checkRedirectHost(redirectURL, connectTo)) {
            debug.error(
                    "InteractionManager.handleRedirectRequest():"
                    + "Invalid Request redirectToHost differs from " 
                    + " connectToHost:"
                    + " in redirectURL=" + redirectURL 
                    + ":connectTo=" + connectTo); 
            throw new InteractionException(INTERACTION_RB_NAME,
                    "redirectURL_differs_from_connectTo", null);
        }

        String requestID = requestMessage.getCorrelationHeader().getMessageID();
        setRequestMessage(requestID, requestMessage);
        setConnectTo(requestID, connectTo);
        setSoapAction(requestID, soapAction);
        setClientCert(requestID, certAlias);
        setRequestMessageID(requestID, messageID);
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.handleRedirectRequest():cached "
                    + " request message for messageID=" + messageID
                    + ":requestID=" + requestID
                    + ":requestMessage:" + (requestMessage == null) );
        }

        returnToURL = returnToURL + "?" + REQUEST_ID + "=" + requestID;
        redirectURL = redirectURL +"&"+ RETURN_TO_URL + "="
                + URLEncDec.encode(returnToURL);
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.handleRedirectRequest():"
                    + "redirecting user agent to redirectURL= "
                    + redirectURL);
        }
        try {
            httpResponse.sendRedirect(redirectURL);
        } catch (IOException ioe) { //IOException
            debug.error("InteractionManager.handleRedirectRequest():"
                    + " catching IOException", ioe);
            throw new InteractionException(INTERACTION_RB_NAME,
                    "IOException_in_Interaction_Manager", null);
        }
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.handleRedirectRequest():"
                    + "redirected user agent to redirectURL= "
                    + redirectURL);
        }
        if (LogUtil.isLogEnabled()) {
            String[] objs =new String[1];
            objs[0] = requestID;
            LogUtil.access(Level.INFO,LogUtil.IS_REDIRECTED_USER_AGENT,objs);
        }
        throw new InteractionRedirectException(requestID);
        //return returnMessage;
    }

    /**
     * Handles resource owner interactions on behalf of <code>WSP</code>.
     * This is invoked at <code>WSP</code> side.
     *
     * @param requestMessage SOAP request that requires resource
     *             owner interactions
     * @param inquiryElement query that <code>WSP</code> wants to pose to
     *        resource owner.
     * @return SOAP message that contains <code>InteractionResponse</code>,
     *         gathered by <code>InteractionManager</code>
     *
     * @throws InteractionException for generic interaction error
     * @throws InteractionSOAPFaultException if a SOAP fault
     *         has to be returned  to <code>WSC</code>
     * @throws SOAPFaultException if the response message has SOAP fault
     *
     * @deprecated
     *
     */
    public Message handleInteraction(Message requestMessage,
            InquiryElement inquiryElement) throws InteractionException, 
            InteractionSOAPFaultException, SOAPFaultException {
        return handleInteraction(requestMessage, inquiryElement, null);
    }

    /**
     * Handles resource owner interactions on behalf of <code>WSP</code>.
     * This is invoked at <code>WSP</code> side.
     *
     * @param requestMessage SOAP request that requires resource
     *             owner interactions
     * @param inquiryElement query that <code>WSP</code> wants to pose to
     *        resource owner 
     * @param language language in which the query page needs to be rendered
     * @return SOAP message that contains <code>InteractionResponse</code>,
     *         gathered by <code>InteractionManager</code>
     *
     * @throws InteractionException for generic interaction error
     * @throws InteractionSOAPFaultException if a SOAP fault
     *         has to be returned  to <code>WSC</code>
     * @throws SOAPFaultException if the response message has SOAP fault
     *
     * @supported.api
     */
    public Message handleInteraction(Message requestMessage,
           InquiryElement inquiryElement, String language)
           throws InteractionException, 
           InteractionSOAPFaultException, SOAPFaultException {

        if (debug.messageEnabled()) {
            debug.message("InteractionManager.handleInteraction():entering");
        }

        //Check redirect is enabled for WSP
        if (!interactionConfig.wspSupportsRedirect()) {
            if (debug.warningEnabled()) {
                debug.warning("InteractionManager.handleInteraction():"
                        + " WSP requests for interaction:wspWillRedirect=" 
                        + interactionConfig.wspSupportsRedirect());
                debug.warning("InteractionManager.handleInteraction():"
                        + "throwing InteractionException");
            }
            throw new InteractionException(INTERACTION_RB_NAME,
                    "wsp_does_not_support_interaction", null);
        }

        //Check wsc provided UserInteraction header
        UserInteractionElement ue 
                = getUserInteractionElement(requestMessage);
        if (ue == null) {
            SOAPFaultException sfe = newRedirectFaultError(
                    QNAME_INTERACTION_REQUIRED);
            if (debug.warningEnabled()) {
                debug.warning("InteractionManager.handleInteraction():"
                        + " WSP requests for interaction - WSC did not "
                        + " provide UserInteractionHeader");
                debug.warning("InteractionManager.handleInteraction():"
                        + "throwing InteractionSOAPFaultException="
                        + sfe);
            }
            throw new InteractionSOAPFaultException(sfe);
        }

        //Check WSC is willing to redirect
        if (ue.isRedirect() == false) {
            SOAPFaultException sfe = newRedirectFaultError(
                    QNAME_INTERACTION_REQUIRED);
            if (debug.warningEnabled()) {
                debug.warning("InteractionManager.handleInteraction():"
                        + "WSP rquests for interaction - WSC  "
                        + " says redirect=false");
                debug.warning("InteractionManager.handleInteraction():"
                        + "throwing InteractionSOAPFaultException="
                        + sfe);
            }
            throw new InteractionSOAPFaultException(sfe);
        }

        //Check WSC allowed interaction
        if (ue.getInteract().equals(QNAME_DO_NOT_INTERACT)) {
            SOAPFaultException sfe = newRedirectFaultError(
                    QNAME_INTERACTION_REQUIRED);
            if (debug.warningEnabled()) {
                debug.warning("InteractionManager.handleInteraction():"
                        + "WSP rquests for interaction - WSC  "
                        + " UserInteractionHeader says doNotInteract");
                debug.warning("InteractionManager.handleInteraction():"
                        + "throwing InteractionSOAPFaultException="
                        + sfe);
            }
            throw new InteractionSOAPFaultException(sfe);
        }

        //Check WSC allowed interaction for data
        if (interactionConfig.wspRedirectsForData()
                    && ue.getInteract().equals(
                    QNAME_DO_NOT_INTERACT_FOR_DATA)) {
            SOAPFaultException sfe = newRedirectFaultError(
                    QNAME_INTERACTION_REQUIRED_FOR_DATA);
            if (debug.warningEnabled()) {
                debug.warning("InteractionManager.handleInteraction():"
                        + "WSP rquests interaction for data - WSC  "
                        + " UserInteractionHeader says doNotInteractForData");
                debug.warning("InteractionManager.handleInteraction():"
                        + "throwing InteractionSOAPFaultException="
                        + sfe);
            }
            throw new InteractionSOAPFaultException(sfe);
        }

        //Check WSP will not exceed maxInteractionTime specified by WSC
        BigInteger uemi =  ue.getMaxInteractTime();
        if ( (uemi != null) && (interactionConfig.getWSPRedirectTime() 
                    > uemi.intValue()) ) {
            SOAPFaultException sfe = newRedirectFaultError(
                    QNAME_INTERACTION_TIME_NOT_SUFFICEINT); 
            if (debug.warningEnabled()) {
                debug.warning("InteractionManager.handleInteraction():"
                        + "WSP inteaction time =" 
                        + interactionConfig.getWSPRedirectTime() 
                        + " exceeds WSC maxInteractTime= "
                        + ue.getMaxInteractTime());
                debug.warning("InteractionManager.handleInteraction():"
                        + "throwing InteractionSOAPFaultException="
                        + sfe);
            }
            throw new InteractionSOAPFaultException(sfe);
        }

        String requestMessageID 
                = requestMessage.getCorrelationHeader().getMessageID();
        SOAPFaultException sfe =
                newRedirectFault(requestMessageID);
        String redirectResponseID = getResponseID(sfe);
        String requestIP 
                = requestMessage.getIPAddress();
        String requestHost = null;
        if (interactionConfig.wspEnforcesReturnToHostEqualsRequestHost()) {
            try {
                InetAddress inetAddress = InetAddress.getByName(requestIP);
                //requestHost = inetAddress.getCanonicalHostName();
                requestHost = inetAddress.getHostName();
                if (debug.messageEnabled()) {
                    debug.message("InteractionManager.handleInteraction():"
                            + " caching requestHost=" + requestHost
                            + ", for redirectResponseID= " 
                            + redirectResponseID);
                }
                setRequestHost(redirectResponseID, requestHost);
            } catch (UnknownHostException uhe) {
                debug.error("InteractionManager.handleInteraction():"
                        + " can not resolve host name", uhe);
                debug.error("InteractionManager.handleInteraction():"
                        + " throwing InteractionSOAPFaultException", sfe);
                SOAPFaultException sfe1 = newRedirectFaultError(
                        QNAME_INTERACTION_CAN_NOT_DETERMINE_REQUEST_HOST); 
                throw new InteractionSOAPFaultException(sfe1);
            }
        }

        setInquiryElement(redirectResponseID, inquiryElement);
        setRequestMessageID(redirectResponseID, requestMessageID);
        setLanguage(redirectResponseID, language);
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.handleInteraction():"
                    + " throwing InteractionSOAPFaultException "
                    + " to redirect user agent="
                    + sfe);
        }

        throw new InteractionSOAPFaultException(sfe);
        //return responseMessage;

    }

    private void setInquiryElement(String messageID, 
            InquiryElement inquiryElement) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setInquiryElement(inquiryElement);

    }

    InquiryElement getInquiryElement(String messageID) {
        InquiryElement inquiryElement = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            inquiryElement = cacheEntry.getInquiryElement();
        }
        return inquiryElement;
    }

    void setInteractionResponseElement(String messageID, 
            InteractionResponseElement interactionResponse) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setInteractionResponseElement(interactionResponse);
    }

    /**
     * Gets interaction response that was gathered from resource owner
     * by <code>InteractionManager</code>
     *
     * @param requestMessage request message.
     *
     * @return interaction response that was gathered by
     *         <code>InteractionManager</code>.
     *
     * @throws InteractionException for interaction error
     *
     * @supported.api
     */
    public InteractionResponseElement getInteractionResponseElement(
            Message requestMessage) 
            throws InteractionException {
        InteractionResponseElement interactionResponseElement = null;
        CorrelationHeader ch = requestMessage.getCorrelationHeader();
        String messageID = ch.getRefToMessageID();
        CacheEntry cacheEntry = null;
        if (messageID != null) {
            cacheEntry = cache.getCacheEntry(messageID);
            if ( cacheEntry != null) {
                interactionResponseElement 
                        = cacheEntry.getInteractionResponseElement();
            }
            if (debug.messageEnabled()) {
                debug.message("InteractionManager.getInteractionResponseElement():"
                        + "for messageID=" + messageID + ":"
                        + "responseElement="
                        + (interactionResponseElement != null));
            }
        }
        if (LogUtil.isLogEnabled()) {
            String[] objs =new String[3];
            objs[0] = ch.getMessageID();
            objs[1] = ch.getRefToMessageID();
            objs[2] = (cacheEntry == null) ? "NULL" : "NOT NULL";
            LogUtil.access(Level.INFO,LogUtil.IS_RETURNING_RESPONSE_ELEMENT,
                objs);
        }
        return interactionResponseElement;
    }

    private void setRequestMessage(String messageID, Message requestMessage) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setRequestMessage(requestMessage);
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.setRequestMessage():"
                    + " cached  request message for messageID="
                    + messageID  
                    + ":requestMessage:" + (requestMessage == null) );
        }
    }

    private Message getRequestMessage(String messageID) {
        Message requestMessage = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            requestMessage = cacheEntry.getRequestMessage();
        }
        if (debug.messageEnabled()) {
            debug.message("InteractionManager.getRequestMessage():"
                    + " looking up request message for messageID="
                    + messageID  
                    + ":requestMessage=" + (requestMessage == null) );
        }
        return requestMessage;
    }

    private void setRequestMessageID(String messageID,
            String requestMessageID) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setRequestMessageID(requestMessageID);
    }

    String getRequestMessageID(String messageID) {
        String requestMessageID = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            requestMessageID = cacheEntry.getRequestMessageID();
        }
        return requestMessageID;
    }


    void setReturnToURL(String messageID, String returnToURL) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setReturnToURL(returnToURL);
    }

    String getReturnToURL(String messageID) {
        String returnToURL = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            returnToURL = cacheEntry.getReturnToURL();
        }
        return returnToURL;
    }

    void setRequestHost(String messageID, String requestHost) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setRequestHost(requestHost);
    }

    String getRequestHost(String messageID) {
        String requestHost = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            requestHost = cacheEntry.getRequestHost();
        }
        return requestHost;
    }

    private void setConnectTo(String messageID, String ConnectTo) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setConnectTo(ConnectTo);
    }

   
    private void setSoapAction(String messageID, String soapAction) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setSoapAction(soapAction);
    }

    private void setClientCert(String messageID, String certAlias) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setClientCert(certAlias);
    }

    private String getConnectTo(String messageID) {
        String connectTo = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            connectTo = cacheEntry.getConnectTo();
        }
        return connectTo;
    }

    private String getClientCert(String messageID) {
        String clientCert = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            clientCert = cacheEntry.getClientCert();
        }
        return clientCert;
    }

    private String getSoapAction(String messageID) {
        String soapAction = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            soapAction = cacheEntry.getSoapAction();
        }
        return soapAction;
    }

    private void setLanguage(String messageID, String language) {
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry == null) {
            cacheEntry = new CacheEntry(messageID);
            cache.putCacheEntry(messageID, cacheEntry);
        }
        cacheEntry.setLanguage(language);
    }

    String getLanguage(String messageID) {
        String language = null;
        CacheEntry cacheEntry = cache.getCacheEntry(messageID);
        if ( cacheEntry != null) {
            language = cacheEntry.getLanguage();
        }
        return language;
    }

    private UserInteractionElement createUserInteractionElement(
            List acceptLanguages) {
        UserInteractionElement ue = null; 
        try {
            ue =objectFactory.createUserInteractionElement();

            ue.setInteract(interactionConfig
                    .getWSCSpecifiedInteractionChoice());
            ue.setRedirect(interactionConfig.wscSupportsRedirect());
            ue.setMaxInteractTime(
                    java.math.BigInteger.valueOf(interactionConfig
                    .getWSCSpecifiedMaxInteractionTime()));
            ue.getLanguage().addAll(acceptLanguages);
        } catch (JAXBException je) {
            debug.error("InteractionManager.createUserInteractionElement():"
                    + " can not create UserInteractionElement", je);
        }
        return ue;
    }

    static UserInteractionElement getUserInteractionElement(
            Message message) {
        UserInteractionElement ue = null; 
        List list = message.getOtherSOAPHeaders();
        try {
            list = Utils.convertElementToJAXB(list);
        } catch (JAXBException je) {
            debug.error("InteractionManager.getUserInteractionElement():"
                    + "not able to get userInteractionElement:"
                    + "can not convert Element to JAXBObject", je);
            return null;
        }
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Object obj = (Object)iter.next();
            if (obj instanceof UserInteractionElement) {
                ue = (UserInteractionElement)obj;
                break;
            }
        }
        return ue;
    }

    private SOAPFaultException newRedirectFault(String messageID) {
        RedirectRequestElement re = null;
        try{
            re = objectFactory.createRedirectRequestElement();

        } catch (JAXBException je) {
            debug.error("InteractionManager.newRedirectFault():"
                    + " can not create RedirectRequestElement", je);
        }

        CorrelationHeader ch = new CorrelationHeader();
        String responseID = ch.getMessageID();
        ch.setRefToMessageID(messageID);

        String redirectUrl = null;
        String lbRedirectUrl = interactionConfig.getLbWSPRedirectHandler();
        String wspRedirectUrl = interactionConfig.getWSPRedirectHandler();
        if(debug.messageEnabled()) {
            debug.message("InteractionManager.newRedirectURLFault():"
                    + "wspRedirectURL:" + wspRedirectUrl
                    + ", lbRedirectUrl:" + lbRedirectUrl);
        }
        if (lbRedirectUrl == null) {
            redirectUrl = wspRedirectUrl + "?" + TRANS_ID + "=" + responseID;
            if(debug.messageEnabled()) {
                debug.message("InteractionManager.newRedirectURLFault():"
                        + "lbRedirectURL is null, rediectUrl:"
                        + redirectUrl);
            }
        } else { //lbRedirectUrl defined
            redirectUrl = lbRedirectUrl + "?" + TRANS_ID + "=" + responseID
                    + "&" + InteractionConfig.HANDLER_HOST_ID 
                    + "=" + InteractionConfig.getInstance().getLocalServerId();
            if(debug.messageEnabled()) {
                debug.message("InteractionManager.newRedirectURLFault():"
                        + "lbRedirectURL is not null, rediectUrl:"
                        + redirectUrl);
            }
        }
        re.setRedirectURL(redirectUrl);
        List details = new ArrayList();
        try {
            details.add(Utils.convertJAXBToElement(re));
        } catch (JAXBException je) {
            debug.error("InteractionManager.newRedirectFault():"
                    + " can not create newRedirectFault:"
                    + " can not convert JAXBObject to Element", je);
        }

        SOAPFault sf = new SOAPFault(
                QNAME_SERVER, 
                SERVER_ERROR,
                FAULT_ACTOR, new SOAPFaultDetail(details));
        Message sfmsg = new Message(sf);
        sfmsg.setCorrelationHeader(ch);
        SOAPFaultException sfe = new SOAPFaultException(sfmsg);
        return sfe;
    }

    private SOAPFaultException newRedirectFaultError(QName errorCode) {
        StatusElement se = null;
        try{
            se = objectFactory.createStatusElement();

        } catch (JAXBException je) {
            debug.error("InteractionManager.newRedirectFaultError():"
                    + " can not create StatusElement", je);
        }

        se.setCode(errorCode);
        List details = new ArrayList();
        try {
            details.add(Utils.convertJAXBToElement(se));
        } catch (JAXBException je) {
            debug.error("InteractionManager.newRedirectFaultError():"
                    + "can not create new RedirectFaultError:" 
                    + "can not convert JAXBObject to Element", je);
        }
        SOAPFault sf = new SOAPFault(
                QNAME_SERVER, 
                SERVER_ERROR,
                FAULT_ACTOR, new SOAPFaultDetail(details));
        SOAPFaultException sfe = new SOAPFaultException(new Message(sf));
        return sfe;
    }

    String getRedirectURL(SOAPFaultException sfe) throws SOAPFaultException {
        String redirectURL = null;
        List details = null;
        SOAPFaultDetail sfd =
                sfe.getSOAPFaultMessage().getSOAPFault().getDetail();
        if (sfd != null) {
            details = sfd.getOtherChildren();
        }
        try {
            details = Utils.convertElementToJAXB(details);
        } catch (JAXBException je) {
            debug.error("InteractionManager.getRedirectURL():"
                    + " can not get Redirect URL", je);
        }

        if ( (details != null) && (details.size() > 0)
                    && (details.get(0) instanceof RedirectRequestElement)) {
            RedirectRequestElement rre 
                    = (RedirectRequestElement)details.get(0);
            if (rre != null) {
                redirectURL = rre.getRedirectURL();
            }
        }
        if (redirectURL == null) {
            throw sfe;
        }
        return redirectURL;
    }

    private boolean checkRedirectHost(String redirectURL, String connectTo) {
        boolean answer = false;
        try {
            URL redirectToURL = new URL(redirectURL);
            URL connectToURL = new URL(connectTo);
            String redirectHost = redirectToURL.getHost();
            String connectToHost = connectToURL.getHost();
            if (redirectHost.equals(connectToHost)) {
                answer = true;
            }
        } catch (MalformedURLException mfe) {
            debug.error("InteractionManager.checkRedirectHost():"
                    + "redirectURL not a valid URL"
                    + " :redirectURL=" + redirectURL
                    + " :connectTo=" + connectTo, mfe);
        }
        return answer;
    }

    String getResponseID(SOAPFaultException sfe) throws SOAPFaultException{
        String responseID = null;
        CorrelationHeader ch =
                sfe.getSOAPFaultMessage().getCorrelationHeader();
        if (ch == null) {
            debug.error("InteractionManager.getResponseID():"
                    + "null CorrelationHeader in SOAPFaultException");
            throw sfe;
        }
        responseID = ch.getMessageID();
        if (responseID == null) {
            debug.error("InteractionManager.getResponseID():"
                    + "null messageID in SOAPFaultException");
            throw sfe;
        }
        return responseID;
    }

    static class InteractionCache {

        private static final boolean VERBOSE_MESSAGE = false;
        private static final int SWEEP_INTERVAL = 60 * 1000;
        private static final int CACHE_ENTRY_MAX_IDLE_TIME = 120 * 1000;

        PeriodicCleanUpMap cache = new PeriodicCleanUpMap(SWEEP_INTERVAL,
            CACHE_ENTRY_MAX_IDLE_TIME);

        InteractionCache() {
            if (debug.messageEnabled()) {
                debug.message("InteactionCache.InteractionCache() "
                        + " - entering constructor");
            }
            if (debug.messageEnabled()) {
                debug.message("InteactionCache.InteractionCache() "
                        + " - starting sweeper thread");
                debug.message("InteractionCache.InteractionCache() "
                        + " SWEEP_INTERVAL = " + SWEEP_INTERVAL);
                debug.message("InteractionCache.InteractionCache() "
                        + " CACHE_ENTRY_MAX_IDLE_TIME = " 
                        + CACHE_ENTRY_MAX_IDLE_TIME);
            }
            SystemTimerPool.getTimerPool().schedule((TaskRunnable) cache,
                new Date(((currentTimeMillis() + SWEEP_INTERVAL) / 1000)
                * 1000));
            if (debug.messageEnabled()) {
                debug.message("InteactionCache.InteractionCache() "
                        + " - returning from constructor");
            }
        }

        CacheEntry getCacheEntry(String messageID) {
            if (VERBOSE_MESSAGE && debug.messageEnabled()) {
                debug.message("InteractionCache.getCacheEntry():"
                        + "looking up cacheEntry  for messageID=" + messageID);
                debug.message("InteractionCache.getCacheEntry():"
                        + " cached messageIDs=" + cache.keySet());
            }
            CacheEntry entry = (CacheEntry)cache.get(messageID);
            if (entry != null) {
                cache.removeElement(messageID);
                cache.addElement(messageID);
            }
            return entry;
        }

        void putCacheEntry(String messageID, CacheEntry cacheEntry) {
            cache.put(messageID, cacheEntry);
            if (VERBOSE_MESSAGE && debug.messageEnabled()) {
                debug.message("InteractionCache.putCacheEntry():"
                        + " cached cacheEntry for messageID=" + messageID);
            }
        }

    }

    static class CacheEntry {
        String messageID; //key
        String requestMessageID;
        Message requestMessage;
        InquiryElement inquiryElement;
        InteractionResponseElement interactionResponseElement;
        String connectTo;
        String certAlias;
        String soapAction;
        String returnToURL;
        String requestHost;
        String language;

        CacheEntry(String messageID) {
            this.messageID= messageID;
        }

        CacheEntry(String messageID, Message message) {
            this.messageID= messageID;
            this.requestMessage= message;
        }

        void setRequestMessage(Message requestMessage) {
            this.requestMessage = requestMessage;
        }

        Message getRequestMessage() {
            return requestMessage;
        }

        void setRequestMessageID(String requestMessageID) {
            this.requestMessageID = requestMessageID;
        }

        String getRequestMessageID() {
            return requestMessageID;
        }

        void setInquiryElement(InquiryElement inquiryElement) {
            this.inquiryElement = inquiryElement;
        }

        InquiryElement getInquiryElement() {
            return inquiryElement;
        }

        void setInteractionResponseElement(
                InteractionResponseElement interactionResponseElement) {
            this.interactionResponseElement = interactionResponseElement;
        }

        InteractionResponseElement getInteractionResponseElement() {
            return interactionResponseElement;
        }

        void setConnectTo(String connectTo) {
            this.connectTo = connectTo;
        }

        String getConnectTo() {
            return connectTo;
        }

        void setClientCert(String certAlias) {
            this.certAlias = certAlias;
        }

        String getClientCert() {
            return certAlias;
        }

        void setSoapAction(String soapAction) {
            this.soapAction = soapAction;
        }

        String getSoapAction() {
            return soapAction;
        }

        void setReturnToURL(String returnToURL) {
            this.returnToURL = returnToURL;
        }

        String getReturnToURL() {
            return returnToURL;
        }

        void setRequestHost(String requestHost) {
            this.requestHost = requestHost;
        }

        String getRequestHost() {
            return requestHost;
        }

        void setLanguage(String language) {
            this.language = language;
        }

        String getLanguage() {
            return language;
        }

    }

}

