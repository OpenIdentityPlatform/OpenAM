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
 * $Id: WebServiceAuthenticator.java,v 1.2 2008/06/25 05:47:24 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javax.security.auth.Subject;

/**
 * This interface provides methods for authentication a web service.
 */
public interface WebServiceAuthenticator {
    /**
     * Authenticates a web service using its certificates.
     *
     * @param message a Message object that needs authentication.
     * @param request the HttpServletRequest object that comes from the web
     *                service
     * @return a token Object for the valid certificates after
     *         successful authentication or null if authentication fails.
     */
    public Object authenticate(Message message,Subject subject,
                               Map state,HttpServletRequest request);
}
