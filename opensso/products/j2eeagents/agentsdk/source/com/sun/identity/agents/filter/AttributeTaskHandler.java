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
 * $Id: AttributeTaskHandler.java,v 1.2 2008/06/25 05:51:44 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IHttpServletRequestHelper;
import com.sun.identity.agents.util.CookieUtils;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.arch.ICrypt;

/**
 * The attribute task handler of agent filter
 */
public abstract class AttributeTaskHandler extends AmFilterTaskHandler {
    
    public AttributeTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode, 
            String modeConfigKey, String queryMapConfigKey) 
    throws AgentException {
        super.initialize(context, mode);
            setCryptUtil();
        initAttributeDateFormatString();
        initAttributeCookieSeparatorCharacter();
        setAttributeEncode(getConfigurationBoolean(CONFIG_ATTRIBUTE_ENCODE,
                DEFAULT_ATTRIBUTE_ENCODE));
        CommonFactory cf = new CommonFactory(getModule());
        setCommonFactory(cf);
        initAttributeFetchMode(modeConfigKey);
        initAttributeQueryMap(queryMapConfigKey);
    }
    
    public AmFilterResult process(AmFilterRequestContext ctx)
            throws AgentException {
        AmFilterResult result = null;
        IHttpServletRequestHelper helper = null;
        HttpServletRequest request = ctx.getHttpServletRequest();
        String userDN = ctx.getUserPrincipal();
        if (userDN == null) {
            throw new AgentException("Attribute Task Handler cannot add"
             + " necessary information without user DN being set");
        }
        if (isAttributeFetchEnabled() && !isRequestProcessed(request)) {
            Map userAttributes = getUserAttributes(ctx, getAttributeQueryMap());
            switch(getAttributeFetchMode().getIntValue()) {
                case AttributeFetchMode.INT_MODE_HTTP_HEADER:
                    helper = getHttpServletRequestHelper(userAttributes);
                    break;
                case AttributeFetchMode.INT_MODE_COOKIE:
                    int count =
                        addAttributesAsCookies(ctx, userAttributes);
                    if (count != 0) {
                        // need to redirect to self
                        if (isLogMessageEnabled()) {
                            logMessage("AttributeTaskHandler: since " 
                                    + count + " cookies were set, "
                                    + "redirecting the user to self");
                        }
                        result = ctx.getRedirectToSelfResult();
                    }
                    break;
                case AttributeFetchMode.INT_MODE_REQUEST_ATTRIBUTE:
                    addAttributesToRequest(request, userAttributes);
                    break;
            }
            markRequestProcessed(request);
        }

        if (helper != null) {
            ctx.addHttpServletRequestHelper(helper);
        }

        return result;
    }

    public boolean isActive() {        
        return (!isModeNone()) && isAttributeFetchEnabled();
    }
    
    protected AttributeFetchMode getAttributeFetchMode() {
        return _attributeFetchMode;
    }
    
    protected Map getAttributeQueryMap() {
        return _attributeQueryMap;
    }
    
    protected abstract String getRequestMarker();
    
    protected abstract Map getUserAttributes(
            AmFilterRequestContext ctx, Map queryMap) throws AgentException;
    
    protected boolean isAttributeFetchEnabled() {
        return (getAttributeFetchMode().getIntValue() != 
            AttributeFetchMode.INT_MODE_NONE )
            && (getAttributeQueryMap().size() > 0);
    }
    
    private int addAttributesAsCookies(
            AmFilterRequestContext ctx, Map attributeMap) 
    {
        int count = 0;
        HttpServletResponse response = ctx.getHttpServletResponse();
        if (attributeMap != null && attributeMap.size() > 0) {
            Iterator it = attributeMap.keySet().iterator();
            while (it.hasNext()) {
                String nextName = (String) it.next();
                Set nextValueSet = (Set) attributeMap.get(nextName);
                String nextValue = "";
                if (nextValueSet != null && nextValueSet.size() > 0) {
                    Iterator valueIterator = nextValueSet.iterator();
                    while (valueIterator.hasNext()) {
                        String value = (String) valueIterator.next();
                        nextValue += value;
                        if (valueIterator.hasNext()) {
                            nextValue += getAttributeCookieSeparatorCharacter();
                        }
                    }

                    String setValue = ctx.getRequestCookieValue(nextName);
                    if (getAttributeEncode()) {
                        nextValue = URLEncoder.encode(nextValue);
                    }
                    if (setValue != null && setValue.equals(nextValue)) {
                        if (isLogMessageEnabled()) {
                            logMessage("AttributeTaskHandler: Attribute "
                                    + nextName + "="  + nextValue
                                    + " is already set as a cookie");
                        }
                    } else {
                        if (isLogMessageEnabled()) {
                            logMessage("AttributeTaskHandler: Adding "
                                    + "Attribute " + nextName + "="
                                    + nextValue + " as cookie");
                        }
                        response.addCookie(CookieUtils.getResponseCookie(
                                nextName, nextValue));
                        count++;
                    }
                } else {
                    Map requestCookies = CookieUtils.getRequestCookies(ctx
                            .getHttpServletRequest());
                    if (requestCookies.containsKey(nextName)) {
                        if (isLogMessageEnabled()) {
                            logMessage("AttributeTaskHandler: Reseting"
                                    + " Attribute " + nextName);
                        }
                        ctx.expireCookie(nextName);
                        count++;
                    }
                }
            }
        }

        return count;
    }
    
    private int addAttributesToRequest(HttpServletRequest request, 
            Map attributeMap) 
    {
        int count = 0;
        if (attributeMap != null && attributeMap.size() > 0) {
            Iterator it = attributeMap.keySet().iterator();
            while (it.hasNext()) {
                String nextName = null;
                Set nextValue = null;
                Object setValue = null;
                try {
                    nextName = (String) it.next();
                    nextValue = (Set) attributeMap.get(nextName);
                    setValue = request.getAttribute(nextName);
                    if (setValue != null) {
                        if (setValue instanceof Set) {
                            nextValue.addAll((Set) setValue);
                            if (isLogWarningEnabled()) {
                                logWarning("AttributeTaskHandler: "
                                   + "Merging the value of request attribute " 
                                   + nextName);
                            }
                        } else {
                            if (isLogWarningEnabled()) {
                                logWarning("AttributeTaskHandler: "
                                    + "Ignoring old value of attribute: "
                                    + nextName + ", old value: " + setValue);
                            }
                        }
                    }
                    request.setAttribute(nextName, nextValue);
                    count++;
                } catch (Exception ex) {
                    if (isLogWarningEnabled()) {
                        logWarning("AttributeTaskHandler: failed to add "
                                + "request attribute: name: " + nextName 
                                + ", value: " + nextValue + ", setValue: " 
                                + setValue, ex);
                    }
                }
            }
        }
        return count;
    }
    
    protected IHttpServletRequestHelper getHttpServletRequestHelper(
            Map attributeMap)throws AgentException
    {
        return getCommonFactory().newServletRequestHelper(
                getAttributeDateFormatString(), attributeMap);
    }
    
    protected CommonFactory getCommonFactory() {
        return _commonFactory;
    }
    
    private void markRequestProcessed(HttpServletRequest request) 
            throws AgentException {
        request.setAttribute(getRequestMarker(),
                getCryptUtil().encrypt(
                String.valueOf(System.currentTimeMillis())));
    }

    private boolean isRequestProcessed(HttpServletRequest request) 
    throws AgentException {
        boolean result = false;
        String markerValue = null;
        try {
            markerValue = (String) request.getAttribute(
                    getRequestMarker());
            if (markerValue != null) {
                String innerValue = getCryptUtil().decrypt(markerValue);
                long actualValue = Long.parseLong(innerValue);
                if (actualValue <= System.currentTimeMillis()) {
                    result = true;
                }
            }
        } catch (Exception ex) {
            throw new AgentException("Process check on request failed, marker: "
                    + markerValue, ex);
        }
        return result;
    }
    
    private void initAttributeDateFormatString() {
        setAttributeDateFormatString(getConfigurationString(
            CONFIG_ATTRIBUTE_DATE_FORMAT,
            DEFAULT_DATE_FORMAT_STRING));
    }
    
    private void initAttributeCookieSeparatorCharacter() {
        String separator = getManager().getConfigurationString(
                CONFIG_ATTRIBUTE_SEPARATOR,
                DEFAULT_ATTRIBUTE_SEPARATOR);
        if (separator.length() != 1) {
            if (isLogWarningEnabled()) {
                logWarning(
                    "AttributeTaskHandler: Invalid value for "
                        + "attribute cookie separator "
                        + separator + ", using default: "
                        + DEFAULT_ATTRIBUTE_SEPARATOR);
            }
            separator = DEFAULT_ATTRIBUTE_SEPARATOR;
        }
        char separatorChar = separator.charAt(0);

        if (isLogMessageEnabled()) {
            logMessage("AttributeTaskHandler: Attribute cookie "
                             + " spearator: " +  separatorChar);
        }
        setAttributeCookieSeparatorCharacter(separatorChar);
    }
    
    private void initAttributeFetchMode(String configKey) {
        AttributeFetchMode mode = AttributeFetchMode.get(
                                                getConfigurationString(configKey));
        if (mode == null) {
            mode = AttributeFetchMode.MODE_NONE;
            if (isLogWarningEnabled()) {
                logWarning("AttributeTaskHandler: failed to read attribute "
                                + "fetch mode for key: " + configKey 
                                + ", using default mode:" + mode);
            }
        }
        if (isLogMessageEnabled()) {
            logMessage("AttributeTaskHandler: Attribute fetch mode key: "
                    + configKey + ", value: " + mode);
        }
        setAttributeFetchMode(mode);
    }
    
    private void initAttributeQueryMap(String queryMapConfigKey) {
        Map queryMap = getConfigurationMap(queryMapConfigKey);
        setAttributeQueryMap(queryMap);
        if (isLogMessageEnabled()) {
            logMessage("AttributeTaskHandler: Attribute fetch map key: "
                    + queryMapConfigKey + ", value: " + queryMap); 
        }
    }
    
    private void setAttributeQueryMap(Map map) {
        _attributeQueryMap = map;
    }
    
    private void setAttributeFetchMode(AttributeFetchMode attributeFetchMode) {
        _attributeFetchMode = attributeFetchMode;
    }
    
    private char getAttributeCookieSeparatorCharacter() {
        return _attributeCookieSeparatorChar;
    }
    
    private void setAttributeCookieSeparatorCharacter(char separator) {
        _attributeCookieSeparatorChar = separator;
    }
    
    private void setAttributeDateFormatString(String formatString) {
        _attributeDateFormatString = formatString;
    }

    protected String getAttributeDateFormatString() {
        return _attributeDateFormatString;
    }
    
    private void setAttributeEncode(boolean encode) {
        _attributeEncode= encode;
    }
    
    private boolean getAttributeEncode() {
        return _attributeEncode;
    }
    
    private void setCommonFactory(CommonFactory cf) {
        _commonFactory = cf;
    }

    protected ICrypt getCryptUtil() {
        return _crypt;
    }

    private void setCryptUtil() throws AgentException {
        _crypt = ServiceFactory.getCryptProvider();
    }
    
    private String _attributeDateFormatString;
    private char _attributeCookieSeparatorChar;
    private boolean _attributeEncode;
    private CommonFactory _commonFactory;
    private AttributeFetchMode _attributeFetchMode;
    private Map _attributeQueryMap;
    private ICrypt _crypt;
}
