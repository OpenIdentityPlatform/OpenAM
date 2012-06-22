/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RequestHandler.java,v 1.2 2008/06/25 05:41:35 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.iplanet.services.comm.server;

import com.iplanet.services.comm.share.Request;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.services.comm.share.ResponseSet;
import java.util.List;

/**
 * The <code>RequestHandler</code> interface needs to be implemented by high
 * level services and applications in order to be able to receive requests from
 * the Platform Low Level API.
 * </p>
 * High level services and applications shall return Response objects filled
 * with exception classes and exception messages if there is an exception during
 * the request processing.
 * 
 * @see com.iplanet.services.comm.share.Request
 */

public interface RequestHandler {
    /**
     * This interface must be implemented by high level services and
     * applications in order to receive requests from the Platform Low Level
     * API.
     * 
     * @param requests
     *            A Set<Request> of Request objects.
     * @param servletRequest
     *            Reference to HttpServletRequest object.
     * @param servletResponse
     *            Reference to HttpServletResponse object.
     * @param servletContext
     *            Reference to ServletContext object.
     */
    public ResponseSet process(List<Request> requests,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, ServletContext servletContext);
}
