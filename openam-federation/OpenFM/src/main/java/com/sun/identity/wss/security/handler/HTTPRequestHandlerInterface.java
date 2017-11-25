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
 * $Id: HTTPRequestHandlerInterface.java,v 1.2 2008/06/25 05:50:11 qcheng Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import javax.xml.soap.SOAPMessage;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import com.sun.identity.wss.security.SecurityException;

/* iPlanet-PUBLIC-CLASS */

/**
 * <code>HTTPRequestHandlerInterface</code> provides the interfaces
 * to process methods required by <code>AMHttpAuthModule</code> and 
 * implememted by <code>HTTPRequestHandler</code> 
 *
 */

public interface HTTPRequestHandlerInterface {

     /**
      * Initialize the HTTP Request handler with a configuration map.
      * @param config the configuration map.
      */
     public void init(Map config);

    /**
     * Checks whether client should be authenticated or not. 
     *
     * @param subject the subject that may be used by the callers
     *        to store Principals and credentials validated in the request.
     *
     * @param request the <code>HttpServletRequest</code> associated with 
     *        this Client message request.
     *
     * @return true if the client should be authenticated. 
     *
     */
    public boolean shouldAuthenticate(Subject subject, 
                HttpServletRequest request);

    /**
     * Returns Login URL for client to be redirected.
     * @param request the <code>HttpServletRequest</code>.
     *
     * @return String Login URL
     */
    public String getLoginURL(HttpServletRequest request);

}
