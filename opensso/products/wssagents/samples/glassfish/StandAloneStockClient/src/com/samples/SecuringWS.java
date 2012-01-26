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
 * $Id: SecuringWS.java,v 1.6 2008/08/19 19:15:09 veiming Exp $
 *
 */

package com.samples;

import java.io.*;
import java.util.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.MessageFactory;
import javax.security.auth.Subject;

import com.sun.identity.wss.security.handler.SOAPRequestHandler;
import com.sun.identity.wss.provider.ProviderConfig;


public class SecuringWS {
    
    /** Creates a new instance of SecuringWS */
    public SecuringWS() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String providerName = "wsc";
        
        if (args.length >= 1) {
            providerName = args[0];
        }
        
        // Construct the stock quote request as a String
        StringBuffer soapMessage = getStockQuoteRequest("JAVA");
        System.out.println("SOAPMessage before security headers\n" +
            soapMessage);
        
        try {
            // Constrcut the SOAP Message
            MimeHeaders mimeHeader = new MimeHeaders();
            mimeHeader.addHeader("Content-Type", "text/xml");
            MessageFactory msgFactory = MessageFactory.newInstance();
            SOAPMessage message = msgFactory.createMessage(mimeHeader,
                new ByteArrayInputStream(soapMessage.toString().getBytes()));
            
            // Construct OpenSSO's SOAPRquestHandler to
            // secure the SOAP message
            SOAPRequestHandler handler = new SOAPRequestHandler();
            HashMap params = new HashMap();

            params.put("providername", providerName);
            handler.init(params);
            
            ProviderConfig pc = ProviderConfig.getProvider(
                providerName, ProviderConfig.WSC);
            
            // Secure the SOAP message using "wsc" configuration
            // This should be configured for SAML-HolderOfKey token profile
            SOAPMessage encMessage = handler.secureRequest(
                message, new Subject(), params);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encMessage.writeTo(baos);
            String request = baos.toString();
            System.out.println("\nEncoded Message:\n" + request);
            
            // Send the SOAP message to Stock Quote Service
            String response = getStockQuote(pc.getWSPEndpoint(),request);
            System.out.println("\n\nStock Service Response:\n" + response);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String getStockQuote(String url, String message)
    throws Exception {
        InputStream in_buf = null;
        URL endpoint = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)
        endpoint.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
            "text/xml; charset=\"utf-8\"");
        
        // Output
        byte[] data = message.getBytes("UTF-8");
        int requestLength = data.length;
        connection.setRequestProperty("Content-Length", Integer
            .toString(requestLength));
        OutputStream out = null;
        try {
            out = connection.getOutputStream();
        } catch (ConnectException ce) {
            ce.printStackTrace();
            return (null);
        }
        
        // Write out the message
        out.write(data);
        out.flush();
        
        // Get the response
        try {
            in_buf = connection.getInputStream();
        } catch (IOException ioe) {
            // Could be receiving SOAP fault
            // Debug the exception
            ioe.printStackTrace();
            return (null);
        }
        
        // Return the response
        StringBuffer inbuf = new StringBuffer();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            in_buf, "UTF-8"));
        while ((line = reader.readLine()) != null) {
            inbuf.append(line).append("\n");
        }
        return (new String(inbuf));
    }
    
    private static StringBuffer getStockQuoteRequest(String symbol) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("<env:Envelope ")
        .append("xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\" ")
        .append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ")
        .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        .append("xmlns:enc=\"http://schemas.xmlsoap.org/soap/encoding/\" ")
        .append("xmlns:ns0=\"http://sun.com/stockquote.xsd\" ")
        .append("env:encodingStyle=\"http://schemas.xmlsoap.org")
        .append("/soap/encoding/\"><env:Header>")
        .append(getAddressingHeader())
        .append("</env:Header>")
        .append("<env:Body><ns0:QuoteRequest><Symbol>")
        .append(symbol)
        .append("</Symbol></ns0:QuoteRequest>")      
        .append("</env:Body></env:Envelope>");
        return sb;
    }

    private static String getAddressingHeader() {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("<To xmlns=\"http://www.w3.org/2005/08/addressing\"")
          .append(">http://localhost:8080/StockService/StockService</To>")
          .append("<Action xmlns=\"http://www.w3.org/2005/08/addressing\"")
          .append(">http://sun.com/GetStockQuote</Action>")
          .append("<ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\">")
          .append("<Address>http://www.w3.org/2005/08/addressing/anonymous</Address>")
          .append("</ReplyTo><MessageID xmlns=\"http://www.w3.org/2005/08/addressing\">")
          .append("123456789</MessageID>");
        return sb.toString();
    }
}
