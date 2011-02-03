/* The contents of this file are subject to the terms
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
 * $Id: HeaderCredentialSource.java,v 1.6 2009/10/18 18:41:26 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.auth;

import com.sun.identity.proxy.filter.HeaderFilter;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Request;
import java.io.IOException;

/**
 * A password credential source that retrieves credentials from headers of the
 * incoming request. This allows upstream components (such as policy agents) to
 * set credentials that the proxy can use to authenticate with a downstream
 * server.
 * <p>
 * As the request headers contain credentials, it would be advisable to have a
 * {@link HeaderFilter} downstream to strip the headers before they are sent
 * to any remote server.
 *
 * @author Paul C. Bryan
 */
public class HeaderCredentialSource implements PasswordCredentialSource {

    /** The name of the header that contains the username. */
    private String usernameHeader;

    /** The name of the header that contains the password. */
    private String passwordHeader;

    /**
     * Creates a new header password credential source.
     *
     * @param usernameHeader The name of the header that contains the username.
     * @param passwordHeader The name of the header that contains the password.
     */
    public HeaderCredentialSource(String usernameHeader, String passwordHeader) {
        this.usernameHeader = usernameHeader;
        this.passwordHeader = passwordHeader;
    }

    /**
     * Returns the credentials based on the headers of the incoming request.
     */
    @Override
    public PasswordCredentials credentials(Request request) {
        PasswordCredentials credentials = new PasswordCredentials();
        if (request != null && request.headers != null) {
            credentials.username = request.headers.first(usernameHeader);
            credentials.password = request.headers.first(passwordHeader);
        }
        return credentials;
    }

    /**
     * Unconditionally throws a HandlerException. Override this method to
     * provide more specific handling of invalid credentials.
     *
     * @throws HandlerException unconditionally.
     */
    @Override
    public void invalid(Exchange exchange) throws HandlerException, IOException {
        throw new HandlerException("failed to authenticate using header-supplied credentials");
    }
}

