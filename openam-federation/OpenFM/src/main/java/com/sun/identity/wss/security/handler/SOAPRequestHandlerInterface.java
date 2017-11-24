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
 * $Id: SOAPRequestHandlerInterface.java,v 1.2 2008/06/25 05:50:11 qcheng Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import javax.xml.soap.SOAPMessage;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import org.w3c.dom.Node;

import com.sun.identity.wss.security.SecurityException;

/* iPlanet-PUBLIC-CLASS */

/**
 * <code>SOAPRequestHandlerInterface</code> provides the interfaces
 * to process and secure the  in-bound or out-bound  <code>SOAPMessage</code>s
 * of the web service clients and web service providers. 
 *
 */  
public interface SOAPRequestHandlerInterface {

    /**
     * Initializes the handler with the given configuration. 
     *
     * @param config the configuration map to initializate the provider.
     *
     * @exception SecurityException if the initialization fails.
     */
    public void init(Map config) throws SecurityException;

    /**
     * Authenticates the <code>SOAPMessage</code> from a remote client. 
     *
     * @param soapRequest SOAPMessage that needs to be validated.
     *
     * @param subject the subject that may be used by the callers
     *        to store Principals and credentials validated in the request.
     *
     * @param sharedState that may be used to store any shared state 
     *        information between <code>validateRequest and <secureResponse>
     *
     * @param request the <code>HttpServletRequest</code> associated with 
     *        this SOAP Message request.
     *
     * @param response the <code>HttpServletResponse</code> associated with
     *        this SOAP Message response. 
     *
     * @return Object the authenticated token.
     *
     * @exception SecurityException if any error occured during validation.
     */
    public Object validateRequest(SOAPMessage soapRequest,
                        Subject subject,
                        Map sharedState,
                        HttpServletRequest request,
                        HttpServletResponse response)
        throws SecurityException;

    /**
     * Secures the SOAP Message response to the client.
     *
     * @param soapMessage SOAP Message that needs to be secured.
     *
     * @param sharedState a map for the callers to store any state information
     *        between <code>validateRequest</code> and 
     *        <code>secureResponse</code>.
     *
     * @exception SecurityException if any error occurs during securing. 
     */
    public SOAPMessage secureResponse (SOAPMessage soapMessage, 
              Map sharedState) throws SecurityException;

    /**
     * Secures the <code>SOAPMessage</code> request by adding necessary
     * credential information.
     *
     * @param soapMessage the <code>SOAPMessage</code> that needs to be secured.
     *
     * @param subject  the <code>Subject<code> of the authenticating entity.
     *
     * @param sharedState Any shared state information that may be used between
     *        the <code>secureRequest</code> and <code>validateResponse</code>. 
     *
     * @exception SecurityException if any failure for securing the request.
     */
    public SOAPMessage secureRequest (
                   SOAPMessage soapMessage, 
                   Subject subject,
                   Map sharedState) throws SecurityException;

    /**
     * Validates the SOAP Response from the service provider. 
     *
     * @param soapMessage the <code>SOAPMessage</code> that needs to be 
     *        validated.
     *
     * @param sharedState Any shared data that may be used between the
     *        <code>secureRequest</code> and <code>validateResponse</code>.
     *
     * @exception SecurityException if any failure occured for validating the
     *            response.
     */
    public void validateResponse (SOAPMessage soapMessage, 
                     Map sharedState) throws SecurityException;
    
    /**
     * Prints a Node tree recursively.
     *
     * @param node A DOM tree Node
     *
     * @return An xml String representation of the DOM tree.
     */
    public String print(Node node);
    

}
