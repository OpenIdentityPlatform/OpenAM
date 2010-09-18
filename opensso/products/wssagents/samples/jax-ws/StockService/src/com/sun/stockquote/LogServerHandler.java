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
 * $Id: LogServerHandler.java,v 1.2 2009/12/04 20:53:52 mrudul_uchil Exp $
 *
 */
package com.sun.stockquote;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class LogServerHandler implements SOAPHandler<SOAPMessageContext> {
    
    PrintStream out;
    
    public boolean handleMessage(SOAPMessageContext smc) {        
        Boolean outboundProperty = (Boolean)
            smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (!outboundProperty.booleanValue()) {
            String fileName = System.getProperty("user.home") +
	                "/opensso/request";
            SOAPMessage message = smc.getMessage();
            try {
                out = new PrintStream(new FileOutputStream(fileName, false));
                message.writeTo(out);
                out.close();
            } catch (SOAPException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return true;
    }
    
    public Set<QName> getHeaders() {
        Set<QName> qnames = new HashSet();
        qnames.add(new QName(
            "http://docs.oasis-open.org/wss/2004/01/" +
            "oasis-200401-wss-wssecurity-secext-1.0.xsd",
            "Security", "wsse"));
        return (qnames);
    }
    
    public boolean handleFault(SOAPMessageContext mc) {
        return true;
    }
    
    public void close(MessageContext context) {
    }
    
}
