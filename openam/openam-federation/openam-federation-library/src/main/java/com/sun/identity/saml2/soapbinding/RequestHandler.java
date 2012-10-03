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
 * $Id: RequestHandler.java,v 1.2 2008/06/25 05:48:03 qcheng Exp $
 *
 */


package com.sun.identity.saml2.soapbinding; 

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.Response;
import javax.xml.soap.SOAPMessage;

/**
 * The <code>RequestHandler</code> interface needs to be implemented
 * by each SAMLv2 Query Profile implementaion in order to receive Query
 * Requests from Client.
 *
 * The SOAP end point should be defined in the profile's metadata.
 */

public interface RequestHandler {
    
    /**
     * Returns a SAMLv2 Query Response for the received Query Request.
     *
     * @param  hostedEntityID the entity identifier of the host.
     * @param  remoteEntityID the entity identifier of the remote client.
     * @param  request the incoming Query SAMLv2 Request message from client.
     * @param  soapMessage the SOAP Message .
     * @return the SAMLv2 response to be sent to Query client.
     * @throws SAML2Exception if there is an error processing the query. 
     */
    public Response handleQuery(String hostedEntityID,String remoteEntityID,
                                RequestAbstract request,SOAPMessage soapMessage)
                                throws SAML2Exception;
}
