/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WebServiceDefaultAuthenticator.java,v 1.2 2009/01/26 22:42:33 leiming Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.ISystemAccess;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.handler.SOAPRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.security.auth.Subject;

/**
 * WebServiceDefaultAuthenticator class provides default implementation of 
 * authenticating and validating web service requests.
 */
public class WebServiceDefaultAuthenticator implements IWebServiceAuthenticator {
    
    public WebServiceDefaultAuthenticator() throws AgentException {        
        ISystemAccess systemAccess = AmFilterManager.getSystemAccess();
        setSystemAccess(systemAccess);
    }

    /**
     * Gets the SSO Token from the SOAP request
     */
    public SSOToken getUserToken(HttpServletRequest request, 
            String requestMessage, String remoteAddress, String remoteHost,
            AmFilterRequestContext ctx) 
    {
        SSOToken token = null;

        if ((request == null) || (requestMessage == null)) {
            return token;
        }
        if (getSystemAccess().isLogMessageEnabled()) {
            getSystemAccess().logMessage(
            "WebServiceDefaultAuthenticator.getUserToken:incoming request:\n"
             + requestMessage);
        }
        try {
            // Construct the SOAP Message
            MimeHeaders mimeHeader = new MimeHeaders();
            mimeHeader.addHeader("Content-Type", "text/xml");
            MessageFactory msgFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = msgFactory.createMessage(mimeHeader,
                new ByteArrayInputStream(requestMessage.getBytes()));

            // Construct SOAPRquestHandler of OpenSSO
            SOAPRequestHandler handler = new SOAPRequestHandler();
            HashMap params = new HashMap();
            // We are using the endpoint URL as the WSP config name for the 
            // time being until a better name mapping is in place.
            String reqURL = request.getRequestURL().toString();
            params.put("providername", reqURL);
            if (getSystemAccess().isLogMessageEnabled()) {
                getSystemAccess().logMessage(
                "WebServiceDefaultAuthenticator.getUserToken:Request URL:\n"
                 + reqURL);
            }
            handler.init(params);

            // validate the SOAP message using wsp configuration
            Subject subject = new Subject();
            handler.validateRequest(soapMessage, subject,
                Collections.EMPTY_MAP, null, null);
            // The SSO Token is set as a private credential in the subject
            if (subject != null) {
                // get the SSO Token from the Subject
                Set credentials = subject.getPrivateCredentials();
                if ((credentials != null) && !credentials.isEmpty()) {
                    Iterator iter =  credentials.iterator();
                    while (iter.hasNext()) {
                        Object credential = iter.next();
                        if (credential instanceof SSOToken) {
                            token = (SSOToken)credential;
                        }
                    }
                }
            }
            String requestString = WSSUtils.print(soapMessage.getSOAPPart()); 
            if (getSystemAccess().isLogMessageEnabled()) {
                getSystemAccess().logMessage(
                "WebServiceDefaultAuthenticator.getUserToken: "+ 
                "Http Request message after authentication:\n" +
                requestString);
            }
            OpenSSOHttpServletRequest wssRequest = 
                new OpenSSOHttpServletRequest((HttpServletRequest)request);
            wssRequest.setContents(requestString);
            ctx.setRequest((HttpServletRequest)wssRequest);
        } catch (Exception e) {
            getSystemAccess().logError(
                "WebServiceDefaultAuthenticator.getUserToken: ", e);
        }
        return token;
    }

    
    private ISystemAccess getSystemAccess() {
        return _systemAccess;
    }
    
    private void setSystemAccess(ISystemAccess systemAccess) {
        _systemAccess = systemAccess;
    }
    
    private ISystemAccess _systemAccess;
}
