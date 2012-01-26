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
 * $Id: FAMServerAuthContext.java,v 1.5 2009/05/05 01:16:12 mallas Exp $
 *
 */

package com.sun.identity.wssagents.common.provider;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.EndpointAddress;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPFactory;
import javax.xml.namespace.QName;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;

public class FAMServerAuthContext implements ServerAuthContext {
    
    private static final Logger logger =
                   Logger.getLogger("com.sun.identity.wssagents.security");

    private static final String SOAP_NS = 
                         "http://schemas.xmlsoap.org/soap/envelope/";
    private CallbackHandler handler = null;
    
    //***************AuthModule Instance**********
    FAMServerAuthModule  authModule = null;
    
    /** Creates a new instance of FAMServerAuthContext */
    public FAMServerAuthContext(String operation, Subject subject, Map map, 
        CallbackHandler callbackHandler) {
        //initialize the AuthModules and keep references to them
        this.handler = callbackHandler;
        WSEndpoint endPoint = (WSEndpoint)map.get("ENDPOINT");
        String providerName = null;
        if(endPoint != null) {
           WSDLPort port = endPoint.getPort();
           EndpointAddress ep = port.getAddress();
           if(ep != null) {
              java.net.URL url = ep.getURL();
              if(url != null) {
                 providerName = url.toString();
              }
           }
        }
        if(logger.isLoggable(Level.FINE)) {
           logger.log(Level.FINE, "FAMServerAuthContext.endpoint from the " +
                        "WSDL: " + providerName);
        }
        authModule = new FAMServerAuthModule();
        map.put("providername", providerName);
        try {
            authModule.initialize(null, null, null,map);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.SEVERE)) {
               logger.log(Level.SEVERE, "FAMServerAuthContext Init failed", e);
            }
        }
        
    }
    
    public AuthStatus validateRequest(MessageInfo messageInfo, 
        Subject clientSubject, Subject serviceSubject) throws AuthException {
        
        try {
            return authModule.validateRequest(messageInfo, clientSubject, 
                serviceSubject);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING, "FAMServerAuthContext validate" +
                 " request failed", e);
            }
            throw new SOAPFaultException(createSOAPFault(e.getMessage()));
        }
        
    }
    
    public AuthStatus secureResponse(MessageInfo messageInfo, 
        Subject serviceSubject) throws AuthException {
        
        try {
            return authModule.secureResponse(messageInfo, serviceSubject);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING, "FAMServerAuthContext secure" +
                 " response failed", e);
            }
            throw new SOAPFaultException(createSOAPFault(e.getMessage()));
        }
        
    }
    
    public void cleanSubject(MessageInfo messageInfo, Subject subject) 
        throws AuthException {
        try {
            authModule.cleanSubject(messageInfo, subject);
        } catch (AuthException e) {
            if(logger.isLoggable(Level.WARNING)) {
               logger.log(Level.WARNING, "FAMServerAuthContext clean" +
                 " subject failed", e);
            }
        }
    }

    private SOAPFault createSOAPFault(String faultMsg) throws AuthException {
        if(faultMsg == null || faultMsg.length() == 0) {
           faultMsg = "Unknown error";        
        }
        try {
            SOAPFactory sf = SOAPFactory.newInstance();
            return sf.createFault(faultMsg, new QName(SOAP_NS, "Server", "S"));
        } catch (Exception ex) {
            if(logger.isLoggable(Level.SEVERE)) {
               logger.log(Level.SEVERE, "FAMServerAuthContext create" +
                 " SOAPFault failed", ex);
             }
             throw new AuthException(ex.getMessage());
        }
    }

}
