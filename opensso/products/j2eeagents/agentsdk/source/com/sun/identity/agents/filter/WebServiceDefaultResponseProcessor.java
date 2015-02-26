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
 * $Id: WebServiceDefaultResponseProcessor.java,v 1.1 2008/10/07 17:36:32 huacui Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.ISystemAccess;
import com.sun.identity.wss.security.handler.SOAPRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.security.auth.Subject;

/**
 * WebServiceDefaultResponseProcessor class provides default implementation of 
 * authenticating and validating web service requests.
 */
public class WebServiceDefaultResponseProcessor implements IWebServiceResponseProcessor {
    
    public WebServiceDefaultResponseProcessor() throws AgentException {        
        ISystemAccess systemAccess = AmFilterManager.getSystemAccess();
        setSystemAccess(systemAccess);
    }

    /**
     * Processes the response
     */
    public String process(String providerName, String respContent) {
        String processedResponseContent = null;
        try {
            // Constrcut the SOAP Message
            MimeHeaders mimeHeader = new MimeHeaders();
            mimeHeader.addHeader("Content-Type", "text/xml");
            MessageFactory msgFactory = MessageFactory.newInstance();
            // Construct SOAPRquestHandler of OpenSSO to
            // secure the SOAP message
            SOAPRequestHandler handler = new SOAPRequestHandler();
            HashMap params = new HashMap();
            params.put("providername", providerName);
            handler.init(params);
            if (getSystemAccess().isLogMessageEnabled()) {
                getSystemAccess().logMessage(
                "WebServiceDefaultResponseProcessor:provider name: " 
                + providerName
                + "\n response content: " + respContent);
            }

            SOAPMessage respMessage = msgFactory.createMessage(mimeHeader,
                new ByteArrayInputStream(respContent.getBytes()));

            // Secure the SOAP message
            SOAPMessage encMessage = handler.secureResponse(
                respMessage, Collections.EMPTY_MAP);
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            encMessage.writeTo(bao);
            processedResponseContent = bao.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (getSystemAccess().isLogMessageEnabled()) {
            getSystemAccess().logMessage(
            "WebServiceDefaultResponseProcessor: processed response content= "
            + processedResponseContent);
        }
        return processedResponseContent;
    }

    
    private ISystemAccess getSystemAccess() {
        return _systemAccess;
    }
    
    private void setSystemAccess(ISystemAccess systemAccess) {
        _systemAccess = systemAccess;
    }
    
    private ISystemAccess _systemAccess;
}
