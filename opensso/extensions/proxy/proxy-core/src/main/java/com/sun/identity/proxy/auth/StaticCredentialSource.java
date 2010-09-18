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
 * $Id: StaticCredentialSource.java,v 1.4 2009/10/17 04:47:58 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.auth;

import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Request;
import com.sun.identity.proxy.http.Exchange;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * A password credential source that supplies a hard-coded set of credentials.
 * This supports implementing test cases and integrating with downstream
 * servers where principal identity does not need to be propagated.
 *
 * @author Paul C. Bryan
 */
public class StaticCredentialSource implements PasswordCredentialSource {

    /** The hard-coded credentials to provide. */
    private final PasswordCredentials credentials = new PasswordCredentials();

    /**
     * Creates a new static credential source.
     *
     * @param username the static username to supply.
     * @param password the static password to supply.
     */
    public StaticCredentialSource(String username, String password) {
        credentials.username = username;
        credentials.password = password;
    }

    /**
     * Returns the hard-coded credentials.
     *
     * @param request ignored, as the same credentials are always supplied.
     */
    @Override
    public PasswordCredentials credentials(Request request) {
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
        throw new HandlerException("failed to authenticate using hard-coded credentials");
    }
}
