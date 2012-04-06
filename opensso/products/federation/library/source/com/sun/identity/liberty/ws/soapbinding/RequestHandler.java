/**
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
 * $Id: RequestHandler.java,v 1.2 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

/**
 * The <code>RequestHandler</code> interface needs to be implemented
 * by each web services in order to receive request from your web service
 * client.  After implementing your handler class, register the class in
 * the SOAP Binding Service so SOAP layer could forward incoming WSC
 * requests to your handler.
 *
 * @supported.all.api
 */

public interface RequestHandler {
    
    /**
     * Generates a response according to the request.
     *
     * @param  request the incoming request message from web service client.
     * @return         the response message to be sent to web service client.
     * @throws SOAPFaultException if it fails to process the request and wants
     *                            to send a specific SOAP Fault.
     * @throws Exception if it fails to process the request and wants to send
     *                   a generic SOAP Fault;
     */
    public Message processRequest(Message request)
                   throws SOAPFaultException, Exception;
}
