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
 * $Id: ClientHandler.java,v 1.5 2009/11/04 04:55:42 kamna Exp $
 *
 */

package com.sun.identity.wssagents.jaxws.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.security.auth.Subject;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.ProtocolException;
import com.iplanet.am.util.SystemProperties;

import com.sun.identity.wss.security.handler.SOAPRequestHandler;
import com.sun.identity.wss.security.handler.ThreadLocalService;

public class ClientHandler implements SOAPHandler<SOAPMessageContext>{

    private static final Logger logger =
        Logger.getLogger("com.sun.identity.wssagents.jaxws.security");
	
	public Set getHeaders() {
		Set<QName> qnames = new HashSet();
		qnames.add(new QName ( 
	        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
			"Security","wsse"));
		return qnames;
	}
	
	public boolean handleFault(SOAPMessageContext context) {
        if(logger != null && logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE,
                "ClientHandler.handleFault : " +
                context.getMessage().getSOAPPart().toString());
        }
	    return true;
	}
	
	public boolean handleMessage(SOAPMessageContext context) {

    Boolean outboundMsg =
            (Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        SOAPRequestHandler handler = null;
        try {
		    handler = new SOAPRequestHandler();
		    Map map = new HashMap();

            // Get the WSC provider name for WSC profile
            String providerName = SystemProperties.get(
                "com.sun.identity.wss.wsc.providername");
            if((providerName == null) || (providerName.length() == 0)) {
                QName providerQ = (QName)context.get(
                    MessageContext.WSDL_SERVICE);
                providerName = providerQ.getLocalPart();
            }
            if(logger != null && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ClientHandler: providerName : " +
                    providerName);
            }

		    map.put("providername", providerName);
		    handler.init(map);
		} catch (Exception ex) {
		    if(logger != null && logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE,
                    "ClientHandler.initialization failed : ", ex);
            }
            return false;
		}

	    //Outbound Message from Client = REQUEST
	    if(outboundMsg.booleanValue()){
            if(logger != null && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ClientHandler: Outgoing Request : " +
                    handler.print(context.getMessage().getSOAPPart()));
            }

            try {
                Subject subject = null;
                subject = (Subject)ThreadLocalService.getSubject();
                if(subject == null) {
	                if(logger != null && logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "ClientHandler.subject NULL");
                    }
	                subject = new Subject();
	            } else {
                    ThreadLocalService.removeSubject();
                }
                SOAPMessage secureMsg = 
                    handler.secureRequest(context.getMessage(), subject,
	                new HashMap());
                return true;
		    } catch (Exception ex) {
		        if(logger != null && logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE,
                        "ClientHandler.secureRequest failed : ", ex);
                }
                throw new ProtocolException(ex.toString());
		    }
	    } else {
            //if outbound RESPONSE
			if(logger != null && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ClientHandler: Incoming Response : " +
                    handler.print(context.getMessage().getSOAPPart()));
            }

            try {
		        handler.validateResponse(context.getMessage(), new HashMap());
                return true;
		    } catch (Exception ex) {
		        if(logger != null && logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE,
                        "ClientHandler.validateResponse failed : ", ex);
                }
                throw new ProtocolException(ex.toString());
		    }
	    }
	}

	public void close(MessageContext context) {
		//No operation
	}    
}
