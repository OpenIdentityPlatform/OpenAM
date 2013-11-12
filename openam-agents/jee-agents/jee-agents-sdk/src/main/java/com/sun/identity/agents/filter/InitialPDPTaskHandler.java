/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.agents.filter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.common.IPDPCache;
import com.sun.identity.agents.common.IPDPCacheEntry;
import com.sun.identity.agents.util.IUtilConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This task handler provides the necessary functionality to cache POSTed data
 * when there is no valid session yet. It make redirection to generated URL
 * which allows to retrieve POSTed data later on by
 * <i>PostSSOPDPTaskHandler</i>. 
 * </p>
 */
public class InitialPDPTaskHandler extends AmFilterTaskHandler
implements IInitialPDPTaskHandler, IPDPTaskConstants {

    /**
     * The constructor that takes a <code>Manager</code> instance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException in case this task handler fails to initialize
     */
    public InitialPDPTaskHandler(Manager manager) {
        super(manager);
    }
    
    @Override
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        initPDPEnabledFlag();
        initPDPMode();
    }

    /**
     * Checks to see if the incoming request is of method POST
     * and suggests any action needed to handle requests that do not comply.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries information
     * about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     * @throws AgentException in case this request processing results in an
     * unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();

        if (isLogMessageEnabled()) {
            logMessage("InitialPDPTaskHandler: HTTP request method: " +
                    request.getMethod());

        }
        //it must be POST request
        if (ctx.HTTP_METHOD_POST.equalsIgnoreCase(request.getMethod())) {
            //get all posted data
            Map parameterMap = getParameterMap(request);
            if (isLogMessageEnabled()) {
                logMessage("InitialPDPTaskHandler: POSTed data: " +
                        parameterMap);
            }
            //store oryginal URL
            String originalURL = ctx.getDestinationURL();
            if (isLogMessageEnabled()) {
                logMessage("InitialPDPTaskHandler: original destination URL: " +
                        originalURL);
            }

            long currentTime = System.currentTimeMillis();
            //create magic number to be appended to DUMMYPOST
            String sunpostpreserve = createMagicURLPart(
                                            currentTime,
                                            _pdpStickySessionLBKeyValue);
            if (isLogMessageEnabled()) {
                logMessage("InitialPDPTaskHandler: sunpostpreserve: " +
                        sunpostpreserve);
            }

            StringBuilder redirectURL = new StringBuilder();
            redirectURL.append(ctx.getApplicationContextURL());
            redirectURL.append('/');
            redirectURL.append(DUMMY_POST);
            redirectURL.append('/');
            redirectURL.append(sunpostpreserve);
            if (_pdpStickySessionMode.equalsIgnoreCase(
                    STICKY_SESSION_URL_MODE)) {
                redirectURL.append('?');
                redirectURL.append(_pdpStickySessionModeValue);
            } else {

                Cookie cookie = new Cookie(_pdpStickySessionLBKeyName,
                                            _pdpStickySessionLBKeyValue);
                cookie.setPath(IUtilConstants.DEFAULT_COOKIE_PATH);
                response.addCookie(cookie);
            }

            //store EntryCache in the Cache
            IPDPCache pdpCache = AmFilterManager.getPDPCache();
            IPDPCacheEntry pdpCacheEntry = ServiceFactory.getPDPCacheEntry(
                    getManager());
            pdpCacheEntry.setCreationTime(currentTime);
            pdpCacheEntry.setOriginalURL(ctx.getDestinationURL());
            pdpCacheEntry.setParameterMap(parameterMap);

            pdpCache.addEntry(sunpostpreserve, pdpCacheEntry);
            if (isLogMessageEnabled()) {
                logMessage("InitialPDPTaskHandler: Post Data preserved: " +
                        " key: " + sunpostpreserve +
                        ", entry: " + pdpCacheEntry);
            }

            result = ctx.getCustomRedirectResult(redirectURL.toString());

        }

        return result;
    }

    private Map getParameterMap(HttpServletRequest request) {
        HashMap parameters = new HashMap();
        for (Enumeration<String> e = request.getParameterNames();
            e.hasMoreElements(); ) {
            String name = e.nextElement();
            String[] values = request.getParameterValues(name);
            parameters.put(name, values);
        }
        return parameters;
    }

    private static synchronized int getNextRequestNumber() {
        int currentRequestNumber =_requestNumber;
        _requestNumber++;
        if (_requestNumber >= 5000) {
            _requestNumber = 1;
        }
        return currentRequestNumber;
    }

    private String createMagicURLPart(long creationTime, String lbAgentKey) {
        StringBuilder urlPart = new StringBuilder();
        // a hardcoded part
        urlPart.append(SUN_POST_PRESERVE);
        //date with time
        Date date = new Date();
        date.setTime(creationTime);
        urlPart.append(_sdf.format(date));
        //LB key of the agent
        urlPart.append('.');
        urlPart.append(  lbAgentKey);
        //request number
        urlPart.append('.');
        urlPart.append(getNextRequestNumber());
        return urlPart.toString();
    }

    private void initPaLbKey() {
        if (_pdpStickySessionModeValue == null) {
            _pdpStickySessionLBKeyName = "";
            _pdpStickySessionLBKeyValue = "";
        }
        int eqCharPos = _pdpStickySessionModeValue.indexOf("=");
        if (eqCharPos == -1) {
            _pdpStickySessionLBKeyName = _pdpStickySessionModeValue;
            _pdpStickySessionLBKeyValue = "";
        } else if (_pdpStickySessionModeValue.length() <= eqCharPos + 1) {
            _pdpStickySessionLBKeyName = _pdpStickySessionModeValue.substring(
                    0, eqCharPos);
            _pdpStickySessionLBKeyValue = "";
        } else {
            _pdpStickySessionLBKeyName = _pdpStickySessionModeValue.substring(
                    0, eqCharPos);
            _pdpStickySessionLBKeyValue =_pdpStickySessionModeValue.substring(
                    eqCharPos+1);
        }
    }

    /**
     * Returns a boolean value indicating if this task handler is enabled or not.
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return  isPDPCheckEnabled();
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_INITIAL_POST_DATA_PRESERVATION_TASK_HANDLER_NAME;
    }

    private boolean isPDPCheckEnabled() {
        return _isPDPCheckEnabled;
    }
    
    private void initPDPEnabledFlag() {
        _isPDPCheckEnabled = getConfigurationBoolean(
                CONFIG_POSTDATA_PRESERVE_ENABLE,
                DEFAULT_POSTDATA_PRESERVE_ENABLE);
        if (isLogMessageEnabled()) {
            logMessage("InitialPDPTaskHandler: enabled: " + _isPDPCheckEnabled);
        }
    }

    private void initPDPMode() throws AgentException {
        _pdpStickySessionMode = getConfiguration(
                CONFIG_POSTDATA_PRESERVE_STICKYSESSION_MODE,
                DEFAULT_POSTDATA_PRESERVE_STICKYSESSION_MODE);
        if (!_pdpStickySessionMode.equalsIgnoreCase("COOKIE") &&
            !_pdpStickySessionMode.equalsIgnoreCase("URL")) {
            logError("InitialPDPTaskHandler: wrong PDP stiky session mode: " +
                     _pdpStickySessionMode);
            throw new AgentException("Wrong PDP sticky session mode");
        }
        if (isLogMessageEnabled()) {
            logMessage("InitialPDPTaskHandler: sticky session mode: " +
                    _pdpStickySessionMode);
        }
        _pdpStickySessionModeValue = getConfiguration(
                CONFIG_POSTDATA_PRESERVE_STICKYSESSION_VALUE, "");
        if (isLogMessageEnabled()) {
            logMessage("InitialPDPTaskHandler: sticky session mode value: " +
                    _pdpStickySessionModeValue);
        }
        initPaLbKey();

    }

    private boolean _isPDPCheckEnabled;
    private String _pdpStickySessionMode;
    private String _pdpStickySessionModeValue;
    private String _pdpStickySessionLBKeyName;
    private String _pdpStickySessionLBKeyValue;

    private static int _requestNumber = 1;
    private static SimpleDateFormat _sdf = new SimpleDateFormat(DATE_FORMAT);
}
