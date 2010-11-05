/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServerHandler.java,v 1.4 2009/12/04 20:53:53 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wssagents.jaxws.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.ProtocolException;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.wss.security.handler.SOAPRequestHandler;

public class ServerHandler implements SOAPHandler<SOAPMessageContext>{

	private static final Logger logger =
        Logger.getLogger("com.sun.identity.wssagents.jaxws.security");
    public static ThreadLocal cred = new ThreadLocal();
    public Subject subject = new Subject();

	public Set<QName> getHeaders() {
	    Set<QName> qnames = new HashSet<QName>();
	    qnames.add(new QName (
		    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
		    "Security","wsse"));
		return qnames;
	}
	
	public boolean handleFault(SOAPMessageContext context) {
        if(logger != null && logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE,
                "ServerHandler.handleFault : " +
                context.getMessage().getSOAPPart().toString());
        }
        return true;
	}

	public boolean handleMessage(SOAPMessageContext context) {
	    Boolean outboundMsg =
            (Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		Map sharedMap = (Map) context.get("WSS_SHARED_MAP");
        if ((sharedMap == null) || (sharedMap.isEmpty())) {
            sharedMap = new HashMap();
            context.put("WSS_SHARED_MAP", sharedMap);
        }

        SOAPRequestHandler handler = null;
        try {
		    handler = new SOAPRequestHandler();
		    Map map = new HashMap();

            // Get the Service End Point URL
            HttpServletRequest request =
                (HttpServletRequest)context.get(MessageContext.SERVLET_REQUEST);
            String providerName = request.getRequestURL().toString();
            if(logger != null && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerHandler: providerName : " +
                    providerName);
            }

		    map.put("providername", providerName);
		    handler.init(map);
		} catch (Exception ex) {
		    if(logger != null && logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE,
                    "ServerHandler.initialization failed : ", ex);
            }
            return false;
		}

		//If the message is not outbound, it is a REQUEST from Client
		if(!(outboundMsg.booleanValue())){
            if(logger != null && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerHandler: Incoming Request : " +
                    handler.print(context.getMessage().getSOAPPart()));
            }

		    try {
		        handler.validateRequest(context.getMessage(), subject,
		            sharedMap, null, null);
                synchronized(this){
                    cred.set(subject);
                }
                return true;
		    } catch (Exception ex) {
		        if(logger != null && logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE,
                        "ServerHandler.validateRequest failed : ", ex);
                }
                throw new ProtocolException(ex.toString());
		    }
		  
		 } else {
			//if outbound RESPONSE
			if(logger != null && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerHandler: Outgoing Response : " +
                    handler.print(context.getMessage().getSOAPPart()));
            }

            try {
		        handler.secureResponse(context.getMessage(), sharedMap);
                return true;
		    } catch (Exception ex) {
		        if(logger != null && logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE,
                        "ServerHandler.secureResponse failed : ", ex);
                }
                throw new ProtocolException(ex.toString());
		    }
	    }
    }
    
	public void close(MessageContext context) {
		//No Operation
	}
}
