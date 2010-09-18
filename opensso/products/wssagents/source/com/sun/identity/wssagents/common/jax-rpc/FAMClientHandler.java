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
 * $Id: FAMClientHandler.java,v 1.3 2008/06/25 05:54:47 qcheng Exp $
 *
 */

package com.sun.identity.wssagents.common.jaxrpc;

import javax.xml.namespace.QName;
import javax.xml.rpc.handler.HandlerInfo;
import javax.xml.rpc.handler.GenericHandler;
import javax.xml.rpc.handler.MessageContext;
import javax.xml.rpc.handler.soap.SOAPMessageContext;
import javax.xml.rpc.JAXRPCException;
import javax.security.auth.Subject;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPHeader;

import com.sun.identity.wss.security.handler.SOAPRequestHandler;
import java.util.HashMap;
import java.util.Map;

//import weblogic.logging.NonCatalogLogger;

/**
 * This class implements a handler in the handler chain, used to access the SOAP
 * request and response message.
 * <p>
 * This class extends the <code>javax.xml.rpc.handler.GenericHandler</code>
 * abstract classs and simply prints the SOAP request and response messages to
 * the server log file before the messages are processed by the backend
 * Java class that implements the Web Service itself.
 */

public class FAMClientHandler extends GenericHandler {

  //private NonCatalogLogger log;

  private HandlerInfo handlerInfo;
  private SOAPRequestHandler soapHandler = null;

  /**
   *  Initializes the instance of the handler.  Creates a nonCatalogLogger to
   *  log messages to.
   */

  public void init(HandlerInfo hi) {

    //log = new NonCatalogLogger("WebService-LogHandler");
    handlerInfo = hi;
    QName qname1 = new QName(
    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-01.xsd",
    "Security", "wsse");
    QName qname2 = new QName(
    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-01.xsd", 
    "Security");
    
    QName[] qnames = {qname1, qname2};
    handlerInfo.setHeaders(qnames);
    soapHandler = new SOAPRequestHandler();
    Map config = new HashMap();
    try {
        config.put("providername", "wsc");
        soapHandler.init(config);
    } catch (Exception ex) {
        ex.printStackTrace();
    }

  }

  /**
   * Specifies that the SOAP request message be logged to a log file before the
   * message is sent to the Java class that implements the Web Service.
   */

  public boolean handleRequest(MessageContext context) {

     SOAPMessageContext messageContext = (SOAPMessageContext) context;

     try {
         soapHandler.secureRequest(messageContext.getMessage(), 
              new Subject(), new HashMap());
         return true;
     } catch (Exception ex) {
         ex.printStackTrace();
         return false;
     }
  }

  /**
   * Specifies that the SOAP response message be logged to a log file before the
   * message is sent back to the client application that invoked the Web
   * service.
   */

  public boolean handleResponse(MessageContext context) {

     SOAPMessageContext messageContext = (SOAPMessageContext) context;
     try {
         SOAPMessage soapMessage = messageContext.getMessage();
         System.out.println("SOAP Response: " + 
             com.sun.identity.shared.xml.XMLUtils.print(
               soapMessage.getSOAPPart().getEnvelope()));
         soapHandler.validateResponse(soapMessage, new HashMap());
         SOAPHeader header = soapMessage.getSOAPPart().getEnvelope().getHeader();
         if(header != null) {
            header.detachNode();
         }
         
         return true;
     } catch (Exception ex) {
         ex.printStackTrace();
         return false;
     }

  }

  /**
   * Specifies that a message be logged to the log file if a SOAP fault is
   * thrown by the Handler instance.
   */

  public boolean handleFault(MessageContext context) {

      return false;

  }

  /**
   * Returns the header blocks processed by this handler instance
   */
  public QName[] getHeaders() {
      QName qname1 = new QName(
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-01.xsd", 
      "Security", "wsse");
      QName qname2 = new QName(
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-01.xsd", 
      "Security");
      
      QName[] qnames = {qname1, qname2};
      handlerInfo.setHeaders(qnames);
      return handlerInfo.getHeaders();

  }

}

