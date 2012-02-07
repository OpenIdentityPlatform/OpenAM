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
 * $Id: DefaultSummary.java,v 1.13 2009/01/05 23:17:09 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.config;

import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.HttpServletRequestWrapper;
import com.sun.identity.setup.HttpServletResponseWrapper;
import com.sun.identity.setup.SetupConstants;
import javax.servlet.http.HttpServletRequest;
import org.apache.click.control.ActionLink;

public class DefaultSummary extends AjaxPage {
    
    public ActionLink createConfig = 
        new ActionLink("createDefaultConfig", this, "createDefaultConfig");
    
    public void onInit() {
        super.onInit();
    }
    
    public boolean createDefaultConfig() {
        HttpServletRequest req = getContext().getRequest();
        HttpServletRequestWrapper request = 
            new HttpServletRequestWrapper(getContext().getRequest());          
        HttpServletResponseWrapper response =                
            new HttpServletResponseWrapper(getContext().getResponse());        
        
        String adminPassword = (String)getContext().getSessionAttribute(
            SessionAttributeNames.CONFIG_VAR_ADMIN_PWD);
        request.addParameter(
            SetupConstants.CONFIG_VAR_ADMIN_PWD, adminPassword);
        request.addParameter(
            SetupConstants.CONFIG_VAR_CONFIRM_ADMIN_PWD, adminPassword);

        String agentPassword = (String)getContext().getSessionAttribute(
            SessionAttributeNames.CONFIG_VAR_AMLDAPUSERPASSWD);
        request.addParameter(
            SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD, agentPassword);
        request.addParameter(
            SetupConstants.CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM, agentPassword);
        
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_SSL, "SIMPLE");
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST, getHostName());
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT, 
            "" + AMSetupServlet.getUnusedPort(getHostName(),50389, 1000));
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT,
            Integer.toString(AMSetupServlet.getUnusedPort(getHostName(), 4444, 1000)));
        request.addParameter(
            SetupConstants.CONFIG_VAR_DIRECTORY_JMX_SERVER_PORT,
            Integer.toString(AMSetupServlet.getUnusedPort(getHostName(), 1689, 1000)));
        
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_HOST, getHostName());
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_PORT, ""+req.getServerPort());
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_URI, req.getRequestURI());
        request.addParameter(
            SetupConstants.CONFIG_VAR_SERVER_URL, 
                req.getRequestURL().toString());
        
        request.addParameter(
            SetupConstants.CONFIG_VAR_BASE_DIR, getBaseDir(
                getContext().getRequest()));

        request.addParameter(
            SetupConstants.CONFIG_VAR_ENCRYPTION_KEY, 
            AMSetupServlet.getRandomString());
        
        request.addParameter(
            SetupConstants.CONFIG_VAR_COOKIE_DOMAIN, getCookieDomain());
                
        request.addParameter(
            SetupConstants.CONFIG_VAR_DS_MGR_PWD, "");
        
        request.addParameter(
            SetupConstants.CONFIG_VAR_DATA_STORE,
            SetupConstants.SMS_EMBED_DATASTORE);                       
                
        request.addParameter(
            SetupConstants.CONFIG_VAR_PLATFORM_LOCALE, 
            SetupConstants.DEFAULT_PLATFORM_LOCALE);
        request.addParameter("locale", configLocale.toString());
                
        try {
            if (!AMSetupServlet.processRequest(request, response)) {
                responseString = AMSetupServlet.getErrorMessage();
            }
        } catch (Exception e) {
            responseString = e.getMessage();
            debug.error("DefaultSummary.createDefaultConfig()", e);
        }
        writeToResponse(responseString);
        setPath(null);
        return false;
    }
}
