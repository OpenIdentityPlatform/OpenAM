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
 * $Id: SOAPProviderConfig.java,v 1.2 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 

import java.util.Set;

/**
 * This interface is used to read the SOAP Provider configuration from
 * various configuration stores.
 */
public interface SOAPProviderConfig {

    /**
     * Returns supported authentication mechanisms.
     *
     * @return Set set of supported authentication mechanisms.
     */
    public Set getSupportedAuthenticationMechanisms();

    /**
     * Sets the supported authentication mechanisms.
     *
     * @param authMechs Set of liberty authentication mechanisms.
     */
    public void setSupportedAuthenticationMechanisms(Set authMechs);

    /**
     * Returns Web service authenticator implementation.
     *
     * @return WebServiceAuthenticator Authenticator implementation.
     */
    public WebServiceAuthenticator getAuthenticator();

    /**
     * Checks if the SOAP response needs to be signed.
     *
     * @return true if signing is required.
     */
    public boolean isResponseSignEnabled();

    /**
     * Checks is the request authentication is enabled.
     *
     * @return true if the request authentication is required.
     */
    public boolean isRequestAuthEnabled();

    /**
     * Sets the response signing enabled.
     *
     * @param signRequired boolean variable to enable response signing.
     */
    public void setResponseSignEnabled(boolean signRequired);

    /**
     * Sets the request authentication enabled.
     *
     * @param signRequired boolean variable to enable request authentication.
     */
    public void setRequestAuthEnabled(boolean signRequired);
}
