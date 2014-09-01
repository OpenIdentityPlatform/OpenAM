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
 *
 */

/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.ILibertyAuthnResponseHelper;

public class CDSSOResultTaskHandler extends AmFilterTaskHandler
implements ICDSSOResultTaskHandler {

    private static final String DEFAULT_GOTO_URL = "openam.agent.default_goto_url";

    public CDSSOResultTaskHandler(Manager manager)
        throws AgentException
    {
        super(manager);
    }

    public AmFilterResult process(AmFilterRequestContext cxt)
        throws AgentException
    {
        AmFilterResult result = null;

        HttpServletRequest request = cxt.getHttpServletRequest();
        HttpServletResponse response = cxt.getHttpServletResponse();
        ICDSSOContext cdssoCxt = getCDSSOContext();

        if (request.getRequestURI().equals(cdssoCxt.getCDSSORedirectURI())) {
            // Now get the Original request from the saved cookie
            String cdssoCookie = cxt.getRequestCookieValue(
                cdssoCxt.getCDSSOCookieName());

            String cdssoTokens[] = null;
            if (cdssoCookie != null) {
                cdssoTokens = cdssoCxt.parseCDSSOCookieValue(cdssoCookie);
            } else {
                String redirectUrl = getSystemConfiguration(DEFAULT_GOTO_URL);
                if (redirectUrl != null) {
                    result = cxt.getCustomRedirectResult(redirectUrl);
                    logError("CDSSOResultTaskHandler : CDSSO cookie not found. "
                            + "Hence redirecting the user to the default goto url");
                } else {
                    result = cxt.getBlockAccessResult();
                    logError("CDSSOResultTaskHandler : CDSSO cookie not found. "
                            + "Hence denying access.");
                }

                return result;
            }

            // Handle the CDSSO Notification. First check if the user token
            // is present in the query string, if not process the request.
            String tokenStr = null;
            String encodedResponse = null;
            if ((tokenStr = request.getParameter(
                    AgentConfiguration.getSSOTokenName())) != null)
            {
                result = processQueryParamAndSetCookies(cxt, tokenStr,
                    cdssoTokens);
            } else if ((encodedResponse = request.getParameter(
                    ILibertyAuthnResponseHelper.AUTHN_PARAM_NAME)) != null)
            {
                result = processAuthnResponseAndSetCookies(cxt, encodedResponse,
                    cdssoTokens);
            } else { // We already redirected for Login
                // The token string was not found either as Query param or
                // AuthnResponse. We need to redirect again to CDSSO to get the
                // token string. We do not have to populate the goto param value
                // because it would have been saved for the original request.
                result = cdssoCxt.getRedirectResult(cxt, null,
                    cdssoTokens[CDSSOContext.INDEX_AUTHN_REQUEST_ID]);
            }
        }

        return result;
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_CDSSO_RESULT_TASK_HANDLER_NAME;
    }

    public boolean isActive() {
        return !isModeNone();
    }

    private AmFilterResult processQueryParamAndSetCookies(
        AmFilterRequestContext cxt,
        String tokenStr,
        String[] cdssoTokens)
    {
        getCDSSOContext().setAuthnResponseFlag(false);
        // The token is passed in URL query string. Extract the
        //token and set the Cookie for this domain.
        if (isLogMessageEnabled()) {
            logMessage("CDSSOResultTaskHandler : Token found in the query "
                     + "parameters. Token - " +  tokenStr);
        }
        HttpServletResponse response = cxt.getHttpServletResponse();
        Cookie[] cookies = getCDSSOContext().createSSOTokenCookie(tokenStr);
        if (cookies != null && cookies.length > 0) {
            for(int i = 0; i < cookies.length; i++) {
                response.addCookie(cookies[i]);
            }
        } else {
            logError("processQueryParamAndSetCookies : no SSO Token cookie created");
        }
        // Remove the temporary CDSSO Cookie
        response.addCookie(getCDSSOContext().getRemoveCDSSOCookie());
        AmFilterResult result = getRedirectResult(cxt, cdssoTokens);

        return result;
    }

    private AmFilterResult processAuthnResponseAndSetCookies(
        AmFilterRequestContext cxt,
        String encodedResponse,
        String[] cdssoTokens)
    {
        ICDSSOContext cdssoContext = getCDSSOContext();
        cdssoContext.setAuthnResponseFlag(true);
        // Could be Libery Post request. Process the request to
        // extract the SSO token. Extract the AuthnResponse data
        if (isLogMessageEnabled()) {
            logMessage("CDSSOResultTaskHandler : Trying to extract the token "
                     + "from Liberty AuthnResponse - " + encodedResponse);
        }
        ILibertyAuthnResponseHelper authnResponse =
            cdssoContext.getAuthnResponseHelper();

        AmFilterResult result = null;
        HttpServletResponse response = cxt.getHttpServletResponse();
        HttpServletRequest request = cxt.getHttpServletRequest();
        try {
            String tokenStr = authnResponse.getSSOTokenString(encodedResponse,
                cdssoTokens[CDSSOContext.INDEX_AUTHN_REQUEST_ID],
                cdssoContext.getTrustedProviderIDs(),
                cdssoContext.getProviderID(cxt));

            Cookie[] cookies = cdssoContext.createSSOTokenCookie(tokenStr);
            if (cookies != null && cookies.length > 0) {
                for(int i = 0; i < cookies.length; i++) {
                    response.addCookie(cookies[i]);
                }
            } else {
               logError("processAuthnResponseAndSetCookies : no SSO Token cookie created");
            }

            // Remove the temporary CDSSO Cookie
            response.addCookie(cdssoContext.getRemoveCDSSOCookie());
            result = getRedirectResult(cxt, cdssoTokens);
        } catch (AgentException ae) {
            logError("CDSSOResultTaskHandler : One or more AuthnResponse "
                   + "conditions might not have been met. Denying "
                   + " to requested URI - " + request.getRequestURI());
            result = cxt.getBlockAccessResult();
        }

        return result;
    }

    protected ICDSSOContext getCDSSOContext() {
        return (ICDSSOContext) getSSOContext();
    }

    private AmFilterResult getRedirectResult(AmFilterRequestContext ctx, String[] cdssoTokens) {
        HttpServletRequest request = ctx.getHttpServletRequest();
        String gotoURL = request.getParameter(ctx.getGotoParameterName());
        if (gotoURL == null || gotoURL.trim().length() == 0) {
            gotoURL = cdssoTokens[CDSSOContext.INDEX_REQUESTED_URL];
        }

        return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT, gotoURL);
    }
}
