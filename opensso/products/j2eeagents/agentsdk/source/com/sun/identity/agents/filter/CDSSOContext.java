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

package com.sun.identity.agents.filter;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.arch.ICrypt;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.ILibertyAuthnResponseHelper;
import com.sun.identity.agents.common.IURLFailoverHelper;
import com.sun.identity.agents.policy.AmWebPolicyResult;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.agents.util.NameValuePair;
import com.sun.identity.agents.util.SAMLUtils;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * A <code>CDSSOContext</code> encapsulates all the configuration and
 * intializations, when the AmFilter is being operated in CDSSO mode.
 */
public class CDSSOContext extends SSOContext implements ICDSSOContext {

    public CDSSOContext(Manager manager) {
        super(manager);
    }

    public void initialize(AmFilterMode filterMode) throws AgentException {
        super.initialize(filterMode);
        setCryptUtil();
        setCDSSORedirectURI(getConfigurationString(CONFIG_CDSSO_REDIRECT_URI));
        setCDSSOCookieName(getConfigurationString(
                CONFIG_CDSSO_COOKIE_NAME, DEFAULT_CDSSO_COOKIE_NAME));
        setCDSSOTrustedProviderIDs(getConfigurationStrings(
            CONFIG_CDSSO_TRUSTED_ID_PROVIDER));
        CommonFactory cf = new CommonFactory(getModule());
        initCDCServletURLFailoverHelper(cf);

        initAuthnResponseHelper(cf);
        if (isLogMessageEnabled()) {
            logMessage("CDSSOContext: initialized. CDSSO is enabled.");
        }
    }

    public AmFilterResult getRedirectResult(AmFilterRequestContext cxt,
                                               AmWebPolicyResult policyResult,
                                               String authnRequestID)
        throws AgentException
    {
        StringBuffer buff = new StringBuffer();

        String cdcServletURL = getCDCServletURL();
        buff.append(cdcServletURL);

        if(cdcServletURL.indexOf("?") != -1) {
            buff.append("&");
        } else {
            buff.append("?");
        }

        buff.append(cxt.getGotoParameterName());
        buff.append("=");
        String encodedCDSSORedirectURL = URLEncoder.encode(
            cxt.getBaseURL() + getCDSSORedirectURI());
        buff.append(encodedCDSSORedirectURL);

        /* if resource based authN is enabled, append the original
         * requested URL as query parameter to the redirect URL.
         */
        String requestURL = null;
        if (isResourceBasedAuthN(cdcServletURL)) {
            requestURL = cxt.populateGotoParameterValue();
        }

        // Now add the Policy Advice Query parameters
        buff = addPolicyQueryParams(buff, policyResult);
        buff = addCDSSOQueryParams(buff, cxt, encodedCDSSORedirectURL,
                requestURL, authnRequestID);
        String redirectURL = buff.toString();

        if(isLogMessageEnabled()) {
            logMessage("CDSSOContext: the CDSSO redirectURL :" + redirectURL);
        }

        return new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT,
            redirectURL);
    }

    public List getTrustedProviderIDs() {
        return _cdssoTrustedProviderIDs;
    }

    private StringBuffer addPolicyQueryParams(StringBuffer buff,
                                              AmWebPolicyResult policyResult) {

        if ((policyResult != null) && policyResult.hasNameValuePairs()) {
            NameValuePair[] nvp = policyResult.getNameValuePairs();

            buff.append("&");

            for(int i = 0; i < nvp.length; i++) {
                buff.append(URLEncoder.encode(nvp[i].getName()));
                buff.append("=");
                buff.append(URLEncoder.encode(nvp[i].getValue()));

                if(i != nvp.length - 1) {
                    buff.append("&");
                }
            }
        }

        return buff;
    }

    private StringBuffer addCDSSOQueryParams(StringBuffer buff,
                                             AmFilterRequestContext cxt,
                                             String encodedCDSSORedirectURL,
                                             String requestURL,
                                             String authnRequestID)
    {
        HttpServletRequest request = cxt.getHttpServletRequest();

        buff.append("&").append(CDSSO_REFERER_SERVLET_IDENTIFIER);
        buff.append("=");
        buff.append(encodedCDSSORedirectURL);

        buff.append("&");
        buff.append(CDSSO_LIBERTY_MAJOR_VERSION_IDENTIFIER);
        buff.append("=").append(CDSSO_LIBERTY_MAJOR_VERSION_VALUE);

        buff.append("&");
        buff.append(CDSSO_LIBERTY_MINOR_VERSION_IDENTIFIER);
        buff.append("=").append(CDSSO_LIBERTY_MINOR_VERSION_VALUE);

        buff.append("&");
        buff.append(CDSSO_AUTHNREQUEST_REQUEST_ID_IDENTIFIER);
        buff.append("=").append(URLEncoder.encode(authnRequestID));

        String providerIDParameter = URLEncoder.encode(getProviderID(cxt));
        buff.append("&");
        buff.append(CDSSO_AUTHNREQUEST_PROVIDER_ID_IDENTIFIER);
        buff.append("=").append(providerIDParameter);

        buff.append("&");
        buff.append(CDSSO_AUTHNREQUEST_ISSUE_INSTANT_IDENTIFIER);
        buff.append("=").append(URLEncoder.encode(getIssueInstant()));

        buff.append("&");
        buff.append(CDSSO_AUTHNREQUEST_FORCE_AUTHN_IDENTIFIER);
        buff.append("=");
        buff.append(CDSSO_AUTHNREQUEST_FORCE_AUTHN_VALUE);

        buff.append("&");
        buff.append(CDSSO_AUTHNREQUEST_IS_PASSIVE_IDENTIFIER);
        buff.append("=");
        buff.append(CDSSO_AUTHNREQUEST_IS_PASSIVE_VALUE);

        buff.append("&");
        buff.append(CDSSO_AUTHNREQUEST_FEDERATE_IDENTIFIER);
        buff.append("=");
        buff.append(CDSSO_AUTHNREQUEST_FEDERATE_VALUE);

        if (requestURL != null && requestURL.length() > 0) {
            buff.append("&");
            buff.append(CDSSO_RESOURCE_URL_IDENTIFIER);
            buff.append("=");
            buff.append(URLEncoder.encode(requestURL));
        }


        return buff;
    }

    public String getProviderID(AmFilterRequestContext ctx) {
        String providerID = ctx.getBaseURL() + "/?"
        + CDSSO_AUTHNREQUEST_PROVIDER_ID_REALM_PARAMETER
        + "="
        + URLEncoder.encode(AgentConfiguration.getOrganizationName());

        if (isLogMessageEnabled()) {
            logMessage("CDSSOContext: ProviderID is : " + providerID);
        }

        return providerID;
    }

    private String getIssueInstant() {
        String issueInstant = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        try {
            issueInstant = getSAMLHelper().dateToUTCString(new Date());
        } catch (ParseException pe) {
            if (isLogMessageEnabled()) {
                logMessage("AmFilter : Could not generate CDSSO issue instant"
                       +   " Defaulting to String : " + issueInstant);
            }
        }
        return issueInstant;
    }

    /**
     * check if resource based authN is enabled for CDC Servlet.
     * @param cdcServletURL
     * @return true if resource based authN is enabled, false if it is not
     *         enabled.
     */
    private boolean isResourceBasedAuthN(String cdcServletURL)
            throws AgentException {

        boolean result = false;

        try {
            URL url = new URL(cdcServletURL);
            String queryStr = url.getQuery();
            if (queryStr != null && 
                    (queryStr.toLowerCase().indexOf(
                    CDSSO_RESOURCE_BASED_AUTHN_ENABLED) >= 0)) {

                result = true;
            }
        } catch (MalformedURLException ex) {
                throw new AgentException (
                        "CDC Servlet URL is malformed: " + cdcServletURL,
                        ex);
        }

        return result;
    }

    public Cookie createCDSSOCookie(String gotoURL,
                                     String accessMethod,
                                     String authnRequestID) throws AgentException
    {
        // To keep track of the request and responses, we add the
        // authn requestID to the the cookie
        StringBuffer sb = new StringBuffer();
        sb.append(gotoURL).append("|").append(accessMethod);
        sb.append("|").append(authnRequestID);
        String value = getCryptUtil().encrypt(sb.toString());

        value = URLEncoder.encode(value);
        Cookie cookie = new Cookie(getCDSSOCookieName(), value);
        cookie.setPath(IUtilConstants.DEFAULT_COOKIE_PATH);

        return cookie;
    }

    public Cookie getRemoveCDSSOCookie() {
        Cookie cookie = new Cookie(getCDSSOCookieName(), "reset");
        cookie.setMaxAge(0);
        cookie.setPath(IUtilConstants.DEFAULT_COOKIE_PATH);
        return cookie;
    }

    public String[] parseCDSSOCookieValue(String cdssoCookie)
        throws AgentException
    {
        cdssoCookie = URLDecoder.decode(cdssoCookie);
        String cdssoTokens[] = null;
        StringTokenizer st =
            new StringTokenizer(getCryptUtil().decrypt(cdssoCookie), "|");
        if (st.countTokens() == 3) {
            cdssoTokens = new String[3];
            cdssoTokens[INDEX_REQUESTED_URL] = st.nextToken();
            cdssoTokens[INDEX_ACCESS_METHOD] =  st.nextToken();
            cdssoTokens[INDEX_AUTHN_REQUEST_ID] = st.nextToken();
        } else {
            throw new AgentException("Invalid CDSSO Cookie value: " +
                cdssoCookie);
        }
        return cdssoTokens;
    }

    public String getAuthnRequestID(AmFilterRequestContext cxt)
        throws AgentException
    {
        String cdssoCookie = cxt.getRequestCookieValue(getCDSSOCookieName());
        String cdssoTokens[] = parseCDSSOCookieValue(cdssoCookie);

        return cdssoTokens[INDEX_AUTHN_REQUEST_ID];
    }

    public String getCDCServletURL() throws AgentException {
        return getCDCServletURLFailoverHelper().getAvailableURL();
    }

    private void setCDSSOCookieName(String name) throws AgentException {
        if (name == null || name.trim().length() == 0) {
            throw new AgentException("Invalid CDSSO cookie name specified");
        }
        _cdssoCookieName = name;
    }

    public String getCDSSOCookieName() {
        return _cdssoCookieName;
    }

    public void setAuthnResponseFlag(boolean flag) {
        _authnResponseFlag = flag;
    }

    public boolean isAuthnResponseEnabled() {
        return _authnResponseFlag;
    }

    private void initAuthnResponseHelper(CommonFactory cf) throws AgentException
    {
        setSAMLHelper(new SAMLUtils());
        setAuthnResponseHelper(
             cf.newLibertyAuthnResponseHelper(
                getConfigurationInt(
                        CONFIG_CDSSO_CLOCK_SKEW, DEFAULT_CDSSO_CLOCK_SKEW)));
    }

    private void setAuthnResponseHelper(ILibertyAuthnResponseHelper helper) {
        _authnResponseHelper = helper;
        if (isLogMessageEnabled()) {
            logMessage("CDSSOContext: Liberty authn response helper set to: "
                    + _authnResponseHelper);
        }
    }

    public ILibertyAuthnResponseHelper getAuthnResponseHelper() {
        return _authnResponseHelper;
    }

    private void setCDSSOTrustedProviderIDs(String[] providerIDs) {
        List list = new ArrayList();
        if ((providerIDs != null) && (providerIDs.length > 0)) {
            for(int i = 0; i < providerIDs.length; i++) {
                list.add(providerIDs[i]);
            }
        }
        _cdssoTrustedProviderIDs = list;
    }



    private void setSAMLHelper(SAMLUtils samlHelper) {
        _samlHelper = samlHelper;
        if (isLogMessageEnabled()) {
            logMessage("CDSSOContext: SAML Helper set to: " + _samlHelper);
        }
    }

    public SAMLUtils getSAMLHelper() {
        return _samlHelper;
    }

    private void initCDCServletURLFailoverHelper(CommonFactory cf)
    throws AgentException
    {
        boolean isPrioritized = getConfigurationBoolean(
                CONFIG_LOGIN_URL_PRIORITIZED);
        boolean probeEnabled = getConfigurationBoolean(
                CONFIG_LOGIN_URL_PROBE_ENABLED, true);
        long    timeout = getConfigurationLong(
                CONFIG_LOGIN_URL_PROBE_TIMEOUT, 2000);
        setCDCServletURLFailoverHelper(cf.newURLFailoverHelper(
                probeEnabled,
                isPrioritized,
                timeout,
                getConfigurationStrings(CONFIG_CDSSO_CDC_SERVLET_URL)));
    }

    private void setCDCServletURLFailoverHelper(IURLFailoverHelper helper) {
        _cdcServletURLFailoverHelper = helper;
        if (isLogMessageEnabled()) {
            logMessage("CDSSOContext: cdc servlet url failover helper: "
                    + _cdcServletURLFailoverHelper);
        }
    }

    public IURLFailoverHelper getCDCServletURLFailoverHelper() {
        return _cdcServletURLFailoverHelper;
    }


    public String getCDSSORedirectURL(HttpServletRequest request) {
        return request.getScheme() + "://"
            + request.getServerName() + ":" + request.getServerPort()
            + getCDSSORedirectURI();
    }

    public String getCDSSORedirectURI() {
        return _cdssoRedirectURI;
    }

    private void setCDSSORedirectURI(String uri) throws AgentException {
        _cdssoRedirectURI = uri;
        if (_cdssoRedirectURI == null || _cdssoRedirectURI.trim().length() == 0)
        {
            throw new AgentException("Invalid CDSSO redirect URI");
        }
        if (isLogMessageEnabled()) {
            logMessage("CDSSOContext: cdsso redirect uri: "
                    + _cdssoRedirectURI);
        }
    }

    protected ICrypt getCryptUtil() {
        return _crypt;
    }

    private void setCryptUtil() throws AgentException {
        _crypt = ServiceFactory.getCryptProvider();
    }

    private boolean _authnResponseFlag;

    private SAMLUtils _samlHelper;
    private String _cdssoRedirectURI;
    private String _cdssoCookieName;
    private List _cdssoTrustedProviderIDs;
    private IURLFailoverHelper _cdcServletURLFailoverHelper;
    private ILibertyAuthnResponseHelper _authnResponseHelper;
    private ICrypt _crypt;
}
