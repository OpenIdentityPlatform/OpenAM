/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMWSSFilterImpl.java,v 1.4 2008/08/19 19:15:12 veiming Exp $
 *
 */

package com.sun.identity.wssagents.weblogic;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import javax.security.auth.Subject;

import com.sun.identity.wss.security.handler.SOAPRequestHandler;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class implements the <code>javax.servlet.Filter</code> interface. It is 
 * depolyed with a webservice provider. The filter intercepts and validates the 
 * incoming SOAP request from a webservice client. It also intercepts and secures 
 * the outgoing SOAP response from the webservice provider that the servlet 
 * filter is associated with.
 */
public class FAMWSSFilterImpl implements Filter {
    public void doFilter(
            ServletRequest request, ServletResponse response,
            FilterChain filterChain)
    throws IOException, ServletException {                
        if (_logger != null) {
            _logger.log(Level.FINE, "FAMWSSFilterImpl.doFilter: "+
                    "intercepted incoming request ==> " + request);
        }

        String method = ((HttpServletRequest)request).getMethod();
        if (method.equalsIgnoreCase("GET")) {
            filterChain.doFilter(request, response);
        } else { 
            FAMHttpServletRequest myReq = 
                new FAMHttpServletRequest((HttpServletRequest)request);
            FAMHttpServletResponse myResp = 
                new FAMHttpServletResponse((HttpServletResponse)response);
    
            try {
                String reqContent = myReq.getContents();
                if (_logger != null) {
                    _logger.log(Level.FINE, "FAMWSSFilterImpl.doFilter: "+
                            "request body=" + reqContent);
                }
                // Constrcut the SOAP Message
                MimeHeaders mimeHeader = new MimeHeaders();
                mimeHeader.addHeader("Content-Type", "text/xml");
                MessageFactory msgFactory = MessageFactory.newInstance();
                SOAPMessage reqMessage = msgFactory.createMessage(mimeHeader,
                    new ByteArrayInputStream(reqContent.getBytes()));
                
                // Construct OpenSSO's SOAPRquestHandler to
                // secure the SOAP message
                SOAPRequestHandler handler = new SOAPRequestHandler();
                HashMap params = new HashMap();
                // TODO: make the configuration provider name configurable
                // Use "wsp" as the configuration provider
                // In AM console the configuration would be "wspWSP" agent
                params.put("providername", "wsp");
                handler.init(params);
                
                // validate the SOAP message using "wsp" configuration
                Subject subject = new Subject();
                handler.validateRequest(reqMessage, subject, 
                    Collections.EMPTY_MAP, null, null);
           
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                reqMessage.writeTo(baos);
                String requestString = baos.toString();
                if (_logger != null) {
                    _logger.log(Level.FINE, "FAMWSSFilterImpl.doFilter: "+
                            "\nEncoded Message:\n" + requestString);
                }
                myReq.setContents(requestString);
                
                // Set the subject to the container
                setPrincipals(subject, (HttpServletRequest)myReq);
                
                filterChain.doFilter(myReq, myResp);
                if (_logger != null) {
                    _logger.log(Level.FINE, "FAMWSSFilterImpl.doFilter: "+
                            "intercepted outgoing response ==> " + response);
                }
                
                String respContent = myResp.getContents();
                if (_logger != null) {
                    _logger.log(Level.FINE, "FAMWSSFilterImpl.doFilter: "+
                            "response body=" + respContent);
                }
                
                SOAPMessage respMessage = msgFactory.createMessage(mimeHeader,
                    new ByteArrayInputStream(respContent.getBytes()));
                
                // Secure the SOAP message using "wsp" configuration
                SOAPMessage encMessage = handler.secureResponse(
                    respMessage, Collections.EMPTY_MAP);
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                encMessage.writeTo(bao);
                String responseString = bao.toString();
                if (_logger != null) {
                    _logger.log(Level.FINE, "FAMWSSFilterImpl.doFilter: "+
                            "\nEncoded Message:\n" + responseString);
                }
                PrintWriter out = myResp.getWriter();
                out.println(responseString);
            } catch (Exception e) {
                if (_logger != null) {
                    _logger.log(Level.SEVERE, "FAMWSSFilterImpl.doFilter: "+ e);
                }
                e.printStackTrace();
            }
        }
    }

    private void setPrincipals(Subject subject, HttpServletRequest req) {
        return;
    }

    public void destroy() {
    }

    public void init(FilterConfig filterConfig) {
    }
    
    private static Logger _logger = null;
    static {
        LogManager logManager = LogManager.getLogManager();
        _logger = logManager.getLogger(
            "javax.enterprise.system.core.security");
    }
}
