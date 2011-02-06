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
 * $Id: FormLoginTaskHandler.java,v 1.3 2008/07/22 18:01:41 sean_brydon Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.IBaseModuleConstants;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.util.ResourceReader;
import com.sun.identity.agents.util.StringUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for J2EE form based logins.
 * </p>
 */
public class FormLoginTaskHandler extends AmFilterTaskHandler
        implements IFormLoginTaskHandler {
    
    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @param formList the list of URIs that correspond to the various form
     * login pages for the protected applications.
     * @throws AgentException if this task handler could not be initialized
     */
    public FormLoginTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        String[] formList = getConfigurationStrings(CONFIG_FORM_LOGIN_LIST);
        HashSet list = new HashSet();
        for(int i = 0; i < formList.length; i++) {
            list.add(formList[i]);
        }
        setFormLoginPageList(list);
        initFormLoginUseInternalContentFlag();
        initFormLoginContent();
    }
    
    /**
     * Checks to see if the incoming request is that for a J2EE FBL and
     * suggests any action needed to handle such requests appropriately.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle form based logins.
     * @throws AgentException if the request cannot be processed successfully
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
    throws AgentException {
        AmFilterResult result = null;
        if(ctx.isFormLoginRequest()) {
            HttpServletRequest request = ctx.getHttpServletRequest();
            SSOValidationResult ssoValidationResult =
                    ctx.getSSOValidationResult();
            if(isLogMessageEnabled()) {
                logMessage("FormLoginTaskHandler: request URI "
                        + request.getRequestURI()
                        + " is a form login request");
            }
            
            try {
                String userName = ssoValidationResult.getUserId();
                String password =
                        ssoValidationResult.getEncryptedTransportString();
                result = handleFormLogin(ctx, userName, password);
            } catch(Exception ex) {
                throw new AgentException("Failed to handle form login", ex);
            }
        }
        
        return result;
    }
    
    /**
     * Returns a boolean value indicating if this task handler is enabled
     * or not.
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return isModeJ2EEPolicyActive() && (getFormLoginPageList().size() > 0);
    }
    
    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_FORM_LOGIN_TASK_HANDLER_NAME;
    }
    
    private String getInternalContentForFormLogin(String userName,
            String password) throws AgentException {
        StringBuffer buff = new StringBuffer(getInternalContentForFormLogin());
        StringUtils.replaceString(buff, AM_FILTER_J_USERNAME, userName);
        StringUtils.replaceString(buff, AM_FILTER_J_PASSWORD, password);
        return buff.toString();
    }
    
    private AmFilterResult handleFormLogin(AmFilterRequestContext ctx,
            String userName, String password)
            throws AgentException {
        
        AmFilterResult result = null;
        HttpServletRequest request = ctx.getHttpServletRequest();
        
        if(useInternalContentForFormLogin()) {
            result = ctx.getServeDataResult(
                    getInternalContentForFormLogin(userName, password));
        } else {
            request.setAttribute(AM_FILTER_J_USERNAME, userName);
            request.setAttribute(AM_FILTER_J_PASSWORD, password);
            result = ctx.getContinueResult();
        }
        
        return result;
    }
    
    private boolean useInternalContentForFormLogin() {
        return _useInternalContentFlag;
    }
    
    private void setFormLoginUseInternalContentFlag(boolean flag) {
        _useInternalContentFlag = flag;
    }
    
    private void initFormLoginUseInternalContentFlag() {
        setFormLoginUseInternalContentFlag(
                getConfigurationBoolean(
                CONFIG_FORM_LOGIN_USE_INTERNAL_FLAG,
                DEFAULT_FORM_LOGIN_USE_INTERNAL_FLAG));
    }
    
    private String getInternalContentForFormLogin() {
        return _formLoginContent;
    }
    
    private void setFormLoginContent(String contentString) {
        _formLoginContent = contentString;
    }
    
    private void initFormLoginContent() throws AgentException {
        if (useInternalContentForFormLogin()) {
            String fileName = getManager().getConfigurationString(
                    CONFIG_FORM_LOGIN_CONTENT_FILENAME,
                    DEFAULT_FORM_LOGIN_CONTENT_FILENAME);
            
            if (isLogMessageEnabled()) {
                logMessage("FormLoginTaskHandler: Form login content file is: "
                        + fileName);
            }
            
            ResourceReader resourceReader
                    = new ResourceReader(Debug.getInstance(IBaseModuleConstants.AM_FILTER_RESOURCE));
            String contentBufferStr = resourceReader.getTextFromFile(fileName);
            setFormLoginContent(contentBufferStr);
        } else {
            if (isLogMessageEnabled()) {
                logMessage(
                    "FormLoginTaskHandler: Form login " 
                    + "internal content is not set");
            }
        }
    }
    
    /**
     * Method setFormLoginPageList
     *
     * @param formList
     * @throws <code>AgentException</code> in case the list is null
     */
    private void setFormLoginPageList(HashSet formList) throws AgentException {
        if (formList == null) {
            throw new AgentException(
                    "Invalid form list specified: " + formList);
        }
        _formLoginPageList = formList;
    }
    
    
    
    /**
     * Method getFormLoginPageList
     *
     * @return
     */
    private HashSet getFormLoginPageList() {
        return _formLoginPageList;
    }
    
    private boolean _useInternalContentFlag;
    private String _formLoginContent;
    private HashSet _formLoginPageList;
}
