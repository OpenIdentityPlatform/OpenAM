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
 * $Id: ToolService.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.service;

import com.sun.identity.diagnostic.base.core.ToolContext;


/**
 * This class defines the interface for services. Every service 
 * should implement this interface. It defines the service life 
 * cycle methods that will be used by <code>ToolServiceManager</code> 
 * to manage the services.
 *
 */

public interface ToolService {
    /**
     * This is the first method to be called during service life cycle. 
     * This method is passed with the <code>ToolContext</code> 
     * instance that provides convenient access to all application data 
     * and all the services in the application. 
     */
    public void init(ToolContext tContext);
    
    /**
     * This method is called after the service has been initialized. 
     * This method is called once during every service activation. 
     */
    public void start();    
    
    /**
     * This is the entry point to access the service. This method 
     * accepts the incoming requests and process them.
     */
    public void processRequest(ServiceRequest sReq, ServiceResponse sRes)
        throws Exception;
    
    /**
     * This method is called during application shutdown or service 
     * de-activation. Services may release any system resources/connections 
     * here.
     */
    public void stop();
}
