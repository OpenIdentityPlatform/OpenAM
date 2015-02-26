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
 * $Id: LegacyUserAgentTaskHandler.java,v 1.2 2008/06/25 05:51:47 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.util.Hashtable;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IPatternMatcher;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * and outgoing requests that are Legacy User Agent requests.
 * </p>
 */
public abstract class LegacyUserAgentTaskHandler extends AmFilterTaskHandler {

    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException if this task handler could not be initialized.
     */
    public LegacyUserAgentTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        setLegacyUserAgentCache(new Hashtable());
        setNonLegacyUserAgentCache(new Hashtable());
        initLegacyUserAgentSupportEnabledFlag();

        initLegacyUserAgentRedirectURI();
        CommonFactory cf = new CommonFactory(getModule());
        setLegacyUserAgentPatternMatcher(cf.newPatternMatcher(
            getConfigurationStrings(CONFIG_LEGACY_USER_AGENT_LIST)));
    }

    /**
     * Returns a boolean value indicating if this task handler is enabled 
     * or not.
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return  isModeSSOOnlyActive() && isLegacyUserAgentSupportEnabled();
    }

    /**
     * Used to retrieve the cache for legacy user agent headers that have
     * previously been evaluated.
     * @return the legacy user agent cache as a <code>Hashtable</code>
     */
    protected Hashtable getLegacyUserAgentCache() {
        return _legacyUserAgentCache;
    }

    /**
     * Used to retrieve the cache for non-legacy user agent headers that
     * have previously been evaluated.
     *
     * @return the non-legacy user agent cache as a <code>Hashtable</code>
     */
    protected Hashtable getNonLegacyUserAgentCache() {
        return _nonLegacyUserAgentCache;
    }

    /**
     * Used to retrieve the legacy user agent redirect URI as read from the
     * configuration.
     *
     * @return the redirect URI used as an intermediate redirect point for
     * enabling the support of legacy user agents.
     */
    protected String getLegacyUserAgentRedirectURI() {
        return _legacyUserAgentRedirectURI;
    }

    /**
     * Used to retrieve the instance of <code>PatternMatcher</code> that may
     * be used to match the given user agent header values against the patterns
     * set in the configuration.
     *
     * @return an instance of <code>PatternMatcher</code> for matching the given
     * user agent header value with the patters as set in the configuration.
     */
    protected IPatternMatcher getLegacyUserAgentPatternMatcher() {
        return _legacyUserAgentPatternMatcher;
    }

     private void setLegacyUserAgentRedirectURI(String uri) {
         _legacyUserAgentRedirectURI = uri;
     }

    private void initLegacyUserAgentRedirectURI() throws AgentException {
        String legacyUserAgentRedirectURI = getManager().getConfigurationString(
            CONFIG_LEGACY_REDIRECT_URI);
        if (legacyUserAgentRedirectURI == null 
                || legacyUserAgentRedirectURI.trim().length() == 0) {
            throw new AgentException(
                    "Invalid Legacy User-Agent Intermediate Redirect URI");
        }
        setLegacyUserAgentRedirectURI(legacyUserAgentRedirectURI);
    }

    private boolean isLegacyUserAgentSupportEnabled() {
        return _legacyUserAgentSupportEnableFlag;
    }

    private void setLegacyUserAgentSupportEnabledFlag(boolean flag) {
        _legacyUserAgentSupportEnableFlag = flag;
    }

    private void initLegacyUserAgentSupportEnabledFlag() {
        boolean legacyUserAgentSupportEnableFlag = getConfigurationBoolean(
            CONFIG_LEGACY_SUPPORT_FLAG, DEFAULT_LEGACY_SUPPORT_FLAG);
        if (isLogMessageEnabled()) {
            logMessage(
                    "LegacyUserAgentTaskHandler: Legacy support enable flag: " +
                   legacyUserAgentSupportEnableFlag);
        }
        setLegacyUserAgentSupportEnabledFlag(legacyUserAgentSupportEnableFlag);
    }

    private void setLegacyUserAgentCache(Hashtable cache) {
        _legacyUserAgentCache = cache;
    }

    private void setNonLegacyUserAgentCache(Hashtable cache) {
        _nonLegacyUserAgentCache = cache;
    }

    private void setLegacyUserAgentPatternMatcher(IPatternMatcher matcher) {
        _legacyUserAgentPatternMatcher = matcher;
    }

    private Hashtable                       _legacyUserAgentCache;
    private Hashtable                       _nonLegacyUserAgentCache;
    private String                          _legacyUserAgentRedirectURI;
    private boolean                         _legacyUserAgentSupportEnableFlag;
    private IPatternMatcher                  _legacyUserAgentPatternMatcher;

}
