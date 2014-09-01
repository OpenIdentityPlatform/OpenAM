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
 * $Id: OutboundLegacyUserAgentTaskHandler.java,v 1.2 2008/06/25 05:51:48 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;

import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;


/**
 * <p>
 * This task handler provides the necessary functionality to process
 * outgoing requests that are Legacy User Agent requests.
 * </p>
 */
public class OutboundLegacyUserAgentTaskHandler
extends LegacyUserAgentTaskHandler 
implements IOutboundLegacyUserAgentTaskHandler {

    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException if this task handler could not be initialized.
     */
    public OutboundLegacyUserAgentTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
    }

    /**
     * Checks to see if the outgoing request is for a legacy user agent and
     * suggests any action needed to handle such requests appropriately.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries 
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        HttpServletRequest request = ctx.getHttpServletRequest();
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        boolean isLegacyUserAgent = false;
        if (userAgent != null) {
            String userAgentFromLegacyCache = 
                    (String) getLegacyUserAgentCache().get(userAgent);
            String userAgentFromNonLegacyCache = (String)
                getNonLegacyUserAgentCache().get(userAgent);
            if (userAgentFromLegacyCache != null) {
                isLegacyUserAgent = true;
                if (isLogMessageEnabled()) {
                    logMessage("OutboundLegacyUserAgentTaskHandler: " 
                               + "user agent found in legacy cache");
                }
            } else if (userAgentFromNonLegacyCache == null) {
                // Need to do pattern matching
                if (getLegacyUserAgentPatternMatcher().match(userAgent)) {
                    isLegacyUserAgent = true;
                    getLegacyUserAgentCache().put(userAgent, userAgent);
                    if (isLogMessageEnabled()) {
                        logMessage("OutboundLegacyUserAgentTaskHandler: " 
                                   + "User-Agent legacy cache state: " 
                                   + NEW_LINE + getLegacyUserAgentCache());
                    }
                }
                else {
                    getNonLegacyUserAgentCache().put(userAgent, userAgent);
                    if (isLogMessageEnabled()) {
                        logMessage(
                            "OutboundLegacyUserAgentTaskHandler: " 
                            + "User-Agent nonlegacy cache state: "
                            + NEW_LINE + getLegacyUserAgentCache());
                    }

                }
                if (isLogMessageEnabled()) {
                    logMessage(
                            "OutboundLegacyUserAgentTaskHandler: (" + userAgent 
                            + ") not found in legacy or non-legacy cache");
                }
            } else {
                if (isLogMessageEnabled()) {
                    logMessage(
                            "OutboundLegacyUserAgentTaskHandler: (" + userAgent
                            + ") found in non-legacy cache");
                }
            }
            if (isLegacyUserAgent) {
                if (isLogMessageEnabled()) {
                    logMessage(
                            "OutboundLegacyUserAgentTaskHandler: (" + userAgent 
                            + ") identified as legacy user agent");
                }
                result = ctx.getCustomRedirectResult(
                    getLegacyUserAgentRedirectURL(ctx));
            }
            else {
                if (isLogMessageEnabled()) {
                    logMessage(
                            "OutboundLegacyUserAgentTaskHandler: (" + userAgent 
                            + ") identified as non-legacy user agent");
                }
            }
        }
        else {
            if (isLogWarningEnabled()) {
                logWarning("OutboundLegacyUserAgentTaskHandler: " 
                           + "Unable to read user agent header");
            }
        }

        if (isLogMessageEnabled()) {
            logMessage("OutboundLegacyUserAgentTaskHandler: " 
                       + "checkForOutboundLegacyUserAgent => result == null ? "
                       + (result == null));
        }

        return result;
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_OUTBOUND_LEGACY_USER_AGENT_TASK_HANDLER_NAME;
    }

    private String getLegacyUserAgentRedirectURL(AmFilterRequestContext ctx) {
        return ctx.getBaseURL() + getLegacyUserAgentRedirectURI()
                + "?" + ctx.getGotoParameterName()
                + "=" + URLEncoder.encode(ctx.getDestinationURL());
    }
}
