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
 * $Id: PortCheckTaskHandler.java,v 1.4 2008/07/22 18:01:41 sean_brydon Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.IBaseModuleConstants;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.util.ResourceReader;
import com.sun.identity.agents.util.StringUtils;
import com.sun.identity.shared.debug.Debug;


/**
 * <p>
 * This task handler provides the necessary functionality needed to correct
 * incoming requests that could have incorrect port number specified in the
 * <code>HOST</code> header.
 * </p>
 */
public class PortCheckTaskHandler extends AmFilterTaskHandler
implements IPortCheckTaskHandler {

    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws <code>AgentException</code> if this task handler fails to 
     * initialize
     */
    public PortCheckTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        init();
        initPortCheckContent();
    }

    /**
     * Checks to see if the incoming request has the correct port number
     * specified in its <code>HOST</code> header. If the value specified is not
     * correct, this method returns a <code>AmFilterResult</code> instance
     * which suggests the necessary action needed to handle such requests
     * appropriately.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries 
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to take the necessary corrective action.
     *
     * @throws <code>AgentException</code> in case if the processing of this
     * request results in an unexpected error condition.
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
            throws AgentException {

        AmFilterResult result = null;

        try {
            int port = ctx.getHttpServletRequest().getServerPort();

            if(getDefaultPortSet().contains(new Integer(port))) {
                if(isLogMessageEnabled()) {
                    logMessage(
                        "PortCheckTaskHandler: request is on valid port");
                }
            } else {
                if(isLogWarningEnabled()) {
                    logWarning(
                        "PortCheckTaskHandler: Detected invalid port in " 
                        + "request: " + port);
                }

                String referer =
                    ctx.getHttpServletRequest().getHeader(REFERER_HEADER);

                if((referer != null) && (referer.trim().length() > 0)) {
                    if(isLogWarningEnabled()) {
                        logWarning(
                            "PortCheckTaskHandler: Invalid port request has " 
                            + "the referer: "
                            + referer);
                    }

                    int index =
                        referer.indexOf(ctx.getGotoParameterName());

                    if(index == -1) {
                        if(isLogWarningEnabled()) {
                            logWarning(
                                "PortCheckTaskHandler: Referer for invalid " 
                                + "port request "
                                + "does not have a goto value specified...");
                        }
                    } else {
                        String encodedValue =
                            referer.substring(
                                index
                                + ctx.getGotoParameterName().length()
                                + 1);
                        String encodedGotoValue = encodedValue;
                        int    ampIndex = encodedValue.indexOf("&");

                        if(ampIndex != -1) {
                            encodedGotoValue =
                                encodedGotoValue.substring(0, ampIndex);
                        }

                        String decodedGotoValue =
                            URLDecoder.decode(encodedGotoValue);

                        if(isLogMessageEnabled()) {
                            logMessage(
                                "PortCheckTaskHandler: Goto value for invalid " 
                                + "port " + "request is: " + decodedGotoValue);
                        }

                        result = ctx.getServeDataResult(
                                getPortCheckContentForURL(decodedGotoValue));
                    }
                } else {
                    if(isLogWarningEnabled()) {
                        logWarning(
                            "PortCheckTaskHandler: No referer found for "
                                + "reqeust with invalid port");
                    }
                }

                if(result == null) {
                    String scheme = ctx.getAgentProtocol();
                    Integer suggestedPortInt =
                        (Integer) getDefaultProtocolMap().get(scheme);

                    if(suggestedPortInt == null) {
                        throw new AgentException("No port for scheme: "
                                                 + scheme
                                                 + " is available");
                    }

                    StringBuffer buff = new StringBuffer(scheme);

                    buff.append("://")
                        .append(ctx.getAgentHost()).append(":");
                    buff.append(suggestedPortInt.toString()).append(
                        ctx.getHttpServletRequest().getRequestURI());

                    if(ctx.getHttpServletRequest().getQueryString()
                            != null) {
                        buff.append("?").append(
                            ctx.getHttpServletRequest().getQueryString());
                    }

                    if(isLogWarningEnabled()) {
                        logWarning(
                            "PortCheckTaskHandler: request will be redirect "
                                + "to the url " + buff.toString());
                    }
                    result = ctx.getServeDataResult(
                            getPortCheckContentForURL(buff.toString()));
                }
            }
        } catch(Exception ex) {
            throw new AgentException(
                "Unable to process port correction request", ex);
        }

        return result;
    }

    /**
     * Returns a boolean value indicating if this task handler is enabled 
     * or not.
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return  isModeSSOOnlyActive() && isPortCheckingEnabled();
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_PORT_CHECK_TASK_HANDLER_NAME;
    }

    private String getPortCheckContentForURL(String url)
            throws AgentException {

        String result = null;

        try {
            StringBuffer buff = new StringBuffer(getPortCheckContent());

            StringUtils.replaceString(buff, AM_FILTER_AGENT_REQUEST_URL, url);

            result = buff.toString();
        } catch(Exception ex) {
            throw new AgentException(
                "Unable to process port correction content", ex);
        }

        return result;
    }

    private void initPortCheckContent() throws AgentException {

        if(isPortCheckingEnabled()) {
            String fileName = getManager().getConfigurationString(
                                  CONFIG_PORT_CHECK_FILENAME,
                                  DEFAULT_PORT_CHECK_FILENAME);

            if(isLogMessageEnabled()) {
                logMessage(
                    "PortCheckTaskHandler: Port check content file is: "
                    + fileName);
            }          
            
            ResourceReader resourceReader
                    = new ResourceReader(Debug.getInstance(IBaseModuleConstants.AM_FILTER_RESOURCE));
            String contentBufferStr = resourceReader.getTextFromFile(fileName);
            setPortCheckContent(contentBufferStr);
        } else {
            if(isLogMessageEnabled()) {
                logMessage(
                    "PortCheckTaskHandler: Port check internal content is " 
                    + "not set");
            }
        }
    }

    private void setPortCheckContent(String contentString) {
        _portCheckContent = contentString;
    }

    private void setDefaultPortMap(Map defaultPortMap) {
        _defaultPortMap = defaultPortMap;
    }

    private void setDefaultProtocolMap(Map defaultProtocolMap) {
        _defaultProtocolMap = defaultProtocolMap;
    }

    private void setDefaultPortSet(Set defaultPortSet) {
        _defaultPortSet = defaultPortSet;
    }

    private void setEnablePortCheckingFlag(boolean flag) {
        _enablePortChecking = flag;
    }

    private void init() throws AgentException {
        boolean enablePortChecking = getConfigurationBoolean(
            CONFIG_PORT_CHECK_ENABLE_FLAG, DEFAULT_PORT_CHECK_ENABLE_FLAG);

        setEnablePortCheckingFlag(enablePortChecking);

        if(enablePortChecking) {
            if(isLogMessageEnabled()) {
                logMessage("PortCheckTaskHandler: Port checking is enabled");
            }

            try {
                Map portMap =
                    getManager().getConfigurationMap(CONFIG_PORT_CHECK_MAP);

                if((portMap == null) || (portMap.size() == 0)) {
                    throw new AgentException(
                        "No default port mapping specified");
                }

                Set defaultPortSet     = new HashSet();
                Map defaultPortMap     = new HashMap();
                Map defaultProtocolMap = new HashMap();

                Iterator it = portMap.keySet().iterator();

                while(it.hasNext()) {
                    String nextPortStr = (String) it.next();

                    if((nextPortStr == null)
                            || (nextPortStr.trim().length() == 0)) {
                        throw new AgentException(
                            "Null port specified for default port map");
                    }

                    Integer nextPortInt = null;

                    try {
                        nextPortInt = new Integer(nextPortStr);
                    } catch(NumberFormatException ex) {
                        throw new AgentException("Invalid port specified: "
                                                 + nextPortStr);
                    }

                    if((nextPortInt.intValue() <= 0)
                            || (nextPortInt.intValue() >= 65535)) {
                        throw new AgentException("Invalid port specified: "
                                                 + nextPortInt);
                    }

                    String nextProtocolScheme =
                        (String) portMap.get(nextPortStr);

                    if((nextProtocolScheme == null)
                            || (nextProtocolScheme.trim().length() == 0)) {
                        throw new AgentException(
                            "Null protocol scheme specified for port: "
                            + nextPortStr);
                    }

                    nextProtocolScheme =
                        nextProtocolScheme.toLowerCase().trim();

                    if( !nextProtocolScheme.equals(PROTOCOL_SCHEME_HTTP)
                            && !nextProtocolScheme.equals(
                                PROTOCOL_SCHEME_HTTPS)) {
                        throw new AgentException(
                            "Invalid protocol scheme specified: " + "port: "
                            + nextPortStr + ", scheme: "
                            + nextProtocolScheme);
                    }

                    defaultPortMap.put(nextPortInt, nextProtocolScheme);
                    defaultProtocolMap.put(nextProtocolScheme, nextPortInt);
                    defaultPortSet.add(nextPortInt);
                }

                if(isLogMessageEnabled()) {
                    StringBuffer buff =
                        new StringBuffer(
                            "PortCheckTaskHandler: Default port mapping is: ");

                    buff.append(NEW_LINE).append(_defaultPortMap);
                    buff.append(NEW_LINE).append(NEW_LINE);
                    buff.append(
                        "PortCheckTaskHandler: Default protocol-scheme " 
                        + "mapping is: ");
                    buff.append(NEW_LINE).append(_defaultProtocolMap);
                    buff.append(NEW_LINE).append(NEW_LINE);
                    buff.append(
                        "PortCheckTaskHandler: Default port set is: ");
                    buff.append(NEW_LINE).append(_defaultPortSet);
                    logMessage(buff.toString());
                }

                setDefaultPortMap(defaultPortMap);
                setDefaultProtocolMap(defaultProtocolMap);
                setDefaultPortSet(defaultPortSet);

            } catch(Exception ex) {
                throw new AgentException(
                    "Invalid default port mapping specified", ex);
            }
        } else {
            if(isLogWarningEnabled()) {
                logWarning("PortCheckTaskHandler: Port checking is disabled");
            }
        }
    }

    private Set getDefaultPortSet() {
        return _defaultPortSet;
    }

    private boolean isPortCheckingEnabled() {
        return _enablePortChecking;
    }

    private Map getDefaultPortMap() {
        return _defaultPortMap;
    }

    private Map getDefaultProtocolMap() {
        return _defaultProtocolMap;
    }

    private String getPortCheckContent() {
        return _portCheckContent;
    }

    private boolean _enablePortChecking;
    private Map _defaultPortMap;
    private Map _defaultProtocolMap;
    private Set _defaultPortSet;
    private String _portCheckContent;
}
