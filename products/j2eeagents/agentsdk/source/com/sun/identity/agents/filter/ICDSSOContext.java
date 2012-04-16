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

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.common.ILibertyAuthnResponseHelper;
import com.sun.identity.agents.common.IURLFailoverHelper;
import com.sun.identity.agents.policy.AmWebPolicyResult;
import com.sun.identity.agents.util.SAMLUtils;

/**
 * @author
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface ICDSSOContext extends ISSOContext {
    public abstract AmFilterResult getRedirectResult(
            AmFilterRequestContext cxt, AmWebPolicyResult policyResult,
            String authnRequestID) throws AgentException;

    public abstract Cookie createCDSSOCookie(String gotoURL,
            String accessMethod, String authnRequestID) throws AgentException;

    public abstract Cookie getRemoveCDSSOCookie();

    public abstract String[] parseCDSSOCookieValue(String cdssoCookie)
            throws AgentException;

    public abstract String getAuthnRequestID(AmFilterRequestContext cxt)
            throws AgentException;

    public abstract String getCDCServletURL() throws AgentException;

    public abstract String getCDSSOCookieName();
    
    public abstract List getTrustedProviderIDs();

    public abstract void setAuthnResponseFlag(boolean flag);

    public abstract boolean isAuthnResponseEnabled();

    public abstract ILibertyAuthnResponseHelper getAuthnResponseHelper();

    public abstract SAMLUtils getSAMLHelper();

    public abstract IURLFailoverHelper getCDCServletURLFailoverHelper();

    public abstract String getCDSSORedirectURL(HttpServletRequest request);

    public abstract String getCDSSORedirectURI();

    public abstract String getProviderID(AmFilterRequestContext ctx);

    public static final int INDEX_REQUESTED_URL = 0;

    public static final int INDEX_ACCESS_METHOD = 1;

    public static final int INDEX_AUTHN_REQUEST_ID = 2;

    // CDSSO Constants and their default values used in the system
    // ( only used in this package )
    public static final String CDSSO_REQUEST_METHOD_IDENTIFIER = "sunwMethod";

    public static final String CDSSO_LIBERTY_MAJOR_VERSION_IDENTIFIER = 
        "MajorVersion";

    public static final String CDSSO_LIBERTY_MINOR_VERSION_IDENTIFIER = 
        "MinorVersion";

    public static final String CDSSO_AUTHNREQUEST_REQUEST_ID_IDENTIFIER = 
        "RequestID";

    public static final String CDSSO_AUTHNREQUEST_PROVIDER_ID_IDENTIFIER = 
        "ProviderID";

    public static final String CDSSO_AUTHNREQUEST_ISSUE_INSTANT_IDENTIFIER = 
        "IssueInstant";

    public static final String CDSSO_AUTHNREQUEST_FORCE_AUTHN_IDENTIFIER = 
        "ForceAuthn";

    public static final String CDSSO_AUTHNREQUEST_IS_PASSIVE_IDENTIFIER = 
        "IsPassive";

    public static final String CDSSO_AUTHNREQUEST_FEDERATE_IDENTIFIER = "Federate";

    public static final String CDSSO_REFERER_SERVLET_IDENTIFIER = "refererservlet";     // Present for IS 6.0 Compatibility

    public static final String CDSSO_LIBERTY_MAJOR_VERSION_VALUE = "1";

    public static final String CDSSO_LIBERTY_MINOR_VERSION_VALUE = "0";

    public static final String CDSSO_AUTHNREQUEST_FORCE_AUTHN_VALUE = "false";

    public static final String CDSSO_AUTHNREQUEST_IS_PASSIVE_VALUE = "false";

    public static final String CDSSO_AUTHNREQUEST_FEDERATE_VALUE = "false";
    
    public static final String CDSSO_AUTHNREQUEST_PROVIDER_ID_REALM_PARAMETER = 
        "Realm";

    public static final String CDSSO_RESOURCE_BASED_AUTHN_ENABLED =
            "resource=true";

    public static final String CDSSO_RESOURCE_URL_IDENTIFIER =
        "resourceURL";
}
