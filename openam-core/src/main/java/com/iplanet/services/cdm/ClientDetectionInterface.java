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
 * $Id: ClientDetectionInterface.java,v 1.3 2008/06/25 05:41:32 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.iplanet.services.cdm;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The <code>ClientDetectionInterface</code> interface needs to be implemented
 * by services and applications serving multiple clients, to determine the
 * client from which the request has originated. This interface detects the
 * client type from the client request.
 * 
 */
public interface ClientDetectionInterface {
    /**
     * Detects the client type based on the request object.
     * 
     * @param request
     *            HTTP Servlet Request
     * @return a String representing the client type.
     * @exception ClientDetectionException
     *                when there is an error retrieving client data
     */
    public String getClientType(HttpServletRequest request)
            throws ClientDetectionException;
}
