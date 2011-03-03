/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HttpCallback.java,v 1.4 2009/07/28 19:40:45 beomsuk Exp $
 *
 */


package com.sun.identity.authentication.spi;

import java.io.Serializable;
import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletResponse;

/**
 * <code>HttpCallback</code> class implements <code>Callback</code>
 * and is used by the authentication module with HTTP protocol based
 * handshaking negotiation.
 *
 * @supported.all.api
 */
public class HttpCallback implements Callback, Serializable {
    private String tokenHeader = null;
    private String authToken = null;
    private String negoHeader = null;
    private String negoValue = null;
    private int errorCode = HttpServletResponse.SC_UNAUTHORIZED;
    static final String HTTP_NEGOTIATE = "Negotiate";
    static final String HTTP_HTTPBASIC = "BASIC realm=\"basic_realm\"";
    
    /**
     * Creates a <code>HttpCallback</code> object.
     * @param authorizationHeader Header name for the authorization string.
     * @param negotiationHeader  Negotiation header string.
     * @param errorCode Error code set in the header for negotiation.
     */
    public HttpCallback(
        String authorizationHeader,
        String negotiationHeader,
        String errorCode) {
        this.tokenHeader = authorizationHeader;
        try {
            this.errorCode   = Integer.parseInt(errorCode);
        } catch (Exception e) {}
        
        int index = negotiationHeader.indexOf(":");
        if (index != -1) {
            this.negoHeader = negotiationHeader.substring(0, index);
            this.negoValue = negotiationHeader.substring(index+1);
        } else {
            this.negoHeader = negotiationHeader;
        }
    }

    /**
     * Creates a <code>HttpCallback</code> object.
     * The negotiation header is constructed using the
     * negotiation name and value in the format 
     * <code>negoName:negoValue</code>.
     *
     * @param authRHeader Header name for the authorization string.
     * @param negoName Negotiation name in the negotiation header.
     * @param negoValue Negotiation value in the negotiation header.
     * @param errorCode Error code set in the header for negotiation.
     */
    public HttpCallback(
        String authRHeader,
        String negoName,
        String negoValue,
        int errorCode) {
        this.tokenHeader = authRHeader;
        this.negoHeader = negoName;
        this.negoValue = negoValue;
        this.errorCode = errorCode;
    }
    
    /**
     * Returns the authorization header string.
     *
     * @return the authorization header string.
     */
    public String getAuthorizationHeader() {
        return tokenHeader;
    }
    
    /**
     * Returns the negotiation header name.
     *
     * @return the negotiation header name.
     */
    public String getNegotiationHeaderName() {
        return negoHeader;
    }
    
    /**
     * Returns the negotiation header value.
     *
     * @return the negotiation header value.
     */
    public String getNegotiationHeaderValue() {
        return negoValue;
    }
    
    /**
     * Returns the negotiation error code.
     *
     * @return the negotiation error code.
     */
    public int getNegotiationCode() {
        return errorCode;
    }
    
    /**
     * Returns the authorization string.
     * @return the authorization string.
     */
    public String getAuthorization() {
        return authToken;
    }
    
    /**
     * Set the authorization string to a <code>HttpCallback</code> object.
     * @param authorization 
     */
    public void setAuthorization(String authorization) {
        this.authToken = authorization;
    }

    /**
     * Returns <code>true<code> if the callback is for HTTPBasic.
     */
    public boolean isForHTTPBasic() {
        return (negoValue != null)
            ? negoValue.equalsIgnoreCase(HTTP_HTTPBASIC) : false;
    }

    /**
     * Returns <code>true<code> if the callback is for WindowsDesktopSSO.
     */
    public boolean isForWindowsDesktopSSO() {
        return (negoValue != null)
            ? negoValue.equalsIgnoreCase(HTTP_NEGOTIATE) : false;
    }
}

