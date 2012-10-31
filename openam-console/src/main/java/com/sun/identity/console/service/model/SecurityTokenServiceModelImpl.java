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
 * $Id: SecurityTokenServiceModelImpl.java,v 1.2 2008/08/15 23:11:17 asyhuang Exp $
 *
 */
package com.sun.identity.console.service.model;

import com.sun.identity.authentication.service.ConfiguredAuthServices;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

public class SecurityTokenServiceModelImpl
        extends AMServiceProfileModelImpl
        implements SecurityTokenServiceModel {

    public static final String SERVICE_NAME = "sunFAMSTSService";

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public SecurityTokenServiceModelImpl(
            HttpServletRequest req,
            Map map) throws AMConsoleException {
        super(req, SERVICE_NAME, map);

    }

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param map of user information
     */
    public SecurityTokenServiceModelImpl(
            HttpServletRequest req,
            String serviceName,
            Map map) throws AMConsoleException {
        super(req, serviceName, map);
    }

    /**
     * Returns all the authentication chains 
     *
     * @return a set with all authentication chains      
     */
    public Set getAuthenticationChains() {      
        Set chains = new TreeSet();
        ConfiguredAuthServices cfg = new ConfiguredAuthServices();
        chains.addAll(cfg.getChoiceValues().keySet());
        return chains;
    }
}
