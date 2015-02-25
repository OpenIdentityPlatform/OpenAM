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
 * $Id: FSLECPConsumerHandler.java,v 1.2 2008/06/25 05:46:58 qcheng Exp $
 *
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * <code>SP</code> assertion consumer service handler handles 
 * <code>LECP</code> profile.
 */
public class FSLECPConsumerHandler extends FSAssertionArtifactHandler {
    protected FSAuthnResponse authnResponse = null;
    
    protected FSLECPConsumerHandler () {
    }
    
    /**
     * Constructs a <code>FSLECPConsumerHandler</code> object.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object
     * @param idpDescriptor <code>IDP</code> provider descriptor
     * @param idpEntityId <code>IDP</code> entity id
     * @param authnRequest <code>FSAuthnRequest</code> from soap
     * @param doFederate a flag indicating if it is a federation request
     * @param relayState <code>RelayState</code> url
     */
    public FSLECPConsumerHandler (HttpServletRequest request, 
                                 HttpServletResponse response, 
                                 IDPDescriptorType idpDescriptor, 
                                 String idpEntityId,
                                 FSAuthnRequest authnRequest, 
                                 boolean doFederate, 
                                 String relayState) 
    {
        super(request,
            response,
            idpDescriptor,
            idpEntityId,
            authnRequest,
            doFederate,
            relayState);
    }
    
    protected void redirectToResource (String resourceURL) throws FSException {
        try {
            FSUtils.debug.message (
                "FSLECPConsumerHandler.redirectToResource: Called");
            if(resourceURL == null){
                FSUtils.debug.error("FSLECPConsumerHandler.redirectToResource: "
                    + "Resource URL is null");
                response.sendError (response.SC_INTERNAL_SERVER_ERROR,
                    FSUtils.bundle.getString ("nullInputParameter"));
                return;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSLECPConsumerHandler.redirectToResource: " +
                    "User's Authentication Assertion verified redirecting " +
                    "to Resource:" +
                    resourceURL);
            } 
            response.setContentType ("application/vnd.liberty-request+xml");
            response.setHeader ("Pragma", "no-cache");
            FSUtils.forwardRequest(request, response, resourceURL);
        } catch(IOException e) {
            throw new FSException (e.getMessage ());
        }
    }
    
}
