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
 * $Id: PsuedoAmFilter.java,v 1.1 2009/01/30 12:09:41 kalpanakm Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.opensso.agents.jsr196;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.filter.AmFilter;
import com.sun.identity.agents.filter.IAmFilter;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IURLFailoverHelper;

/**
 *
 * @author kalpana
 */
public class PsuedoAmFilter extends AmFilter implements IAmFilter {
    
    public PsuedoAmFilter(Manager manager){
        super(manager);
    }
    
    /**
     * 
     * @param req
     * @param res
     * @return
     * @throws java.lang.Exception
     */
    
    
    String getLoginURL(HttpServletRequest req, HttpServletResponse res) throws Exception {
        CommonFactory cf = new CommonFactory(getModule());
        String[] loginURLs = getConfigurationStrings(CONFIG_LOGIN_URL);
        boolean isPrioritized = getConfigurationBoolean(
                CONFIG_LOGIN_URL_PRIORITIZED);
        boolean probeEnabled = getConfigurationBoolean(
                CONFIG_LOGIN_URL_PROBE_ENABLED, true);
        long    timeout = getConfigurationLong(
                CONFIG_LOGIN_URL_PROBE_TIMEOUT, 2000);
        IURLFailoverHelper ufh;
        String url = null;
        try {
            ufh = cf.newURLFailoverHelper(probeEnabled, isPrioritized,
                    timeout,
                    loginURLs,
                    getParsedConditionalUrls(CONFIG_CONDITIONAL_LOGIN_URL));

            url = ufh.getAvailableURL(req);
        } catch (Exception e) {
            throw e;
        }
        
        return url;        
    }
    
}
