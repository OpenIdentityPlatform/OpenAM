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
 * $Id: ResponseHeadersTaskHandler.java,v 1.3 2008/06/25 05:51:48 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Iterator;

/**
 * <p>
 * This task handler class provides necessary functionality to process
 * any custom HTTP header(s) defined in agent configuration 
 * file and adds them to the <code>HttpServletResponse</code> object
 * </p>
 */

public class ResponseHeadersTaskHandler extends AmFilterTaskHandler 
implements IResponseHeadersTaskHandler {

    /**
     * The constructor that takes a <code>Manager</code> instance in order
     * to gain access to the infrastructure services such as configuration
     * and log access
     *
     * @param manager the <code>Manager</code> for the <code>filter</code> 
     * subsystem
     * @throws AgentException in case this task handler fails to initialize
     */

    public ResponseHeadersTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        setHeadersMap(getConfigurationMap(CONFIG_RESPONSE_HEADER_MAP));
    }

    /**
      * Sets custom headers defined in agent configuration file 
      * to the <code>HttpServletResponse</code> object
      *
      * @param ctx an <code>AmFilterRequestContext</code> object that 
      * carries information about the incoming request and response objects
      * @return <code>null</code> always, as no further action is ever necessary
      * @throws AgentException in case this processing results in error
      */
    
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        try {
            setResponseHeaders(ctx);
        } catch (Exception e) {
            throw new AgentException("ResponseHeadersTaskHandler.process(): "
                    + "Failed to process request", e);
        }
        return result;
    }


    /**
     * Returns a boolean value indicating if this task handler is enabled
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return  isModeSSOOnlyActive() && isResponseHeadersEnabled();
    }


    /**
      * Returns this task handlers name
      */
    public String getHandlerName() {
        return AM_FILTER_RESPONSE_HEADERS_TASK_HANDLER_NAME;
    }


    /**
      * Checks if there is any custom headers to be added 
      * to the response object
      */
    private boolean isResponseHeadersEnabled() {
        return (getHeadersMap().size() > 0);
    }


    /**
      * Sets custom headers to <code>HttpServletResponse</code> object
      * @param ctx is a <code>AmFilterRequestContext</code> object containing
      * <code>HttpServletRequest</code> and <code>HttpServletResponse</code> 
     * objects
      * @throws Exception when any processing error occurs
      *
      */
    private void setResponseHeaders(AmFilterRequestContext ctx) 
        throws Exception {

        String headerName = null;
        String headerValue = null;
        HttpServletResponse response = ctx.getHttpServletResponse();
        Iterator headerNames = getHeadersMap().keySet().iterator();

        while (headerNames.hasNext()) {
            headerName = String.valueOf(headerNames.next());
            Object headerValueObj = getHeadersMap().get(headerName);

            /* If the header value is not empty, 
               set the response header */
            if (headerValueObj != null ) {
                headerValue = String.valueOf(headerValueObj);

                if (response.containsHeader(headerName)) {
                    response.setHeader(headerName, headerValue);
                } else {
                    response.addHeader(headerName, headerValue);
                }

                if (isLogMessageEnabled()) {
                    logMessage(
                        "ResponseHeadersTaskHandler.setResponseHeaders():"
                        + "HeaderName => " + headerName
                        + "HeaderValue => "+ headerValue);
                }
            }
        }
    }


    /**
      * Returns a Map object containing the custom headers
      * @return _headersMap with custom headers information
      */
    private Map getHeadersMap(){
        return _headersMap;
    }


    /**
      * Sets the <code>_headersMap</code> instance variable
      * based on the supplied Map
      * @param map containing custom headers
      */
    private void setHeadersMap(Map map){
        _headersMap = map;
    }


    private Map _headersMap = null;

}

