/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.noauth2.wrappers;

import org.forgerock.oauth2.core.ResourceOwnerAuthentication;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @since 12.0.0
 */
public class OpenAMResourceOwnerAuthentication implements ResourceOwnerAuthentication {

    private final String username;
    private final char[] password;
    private final String realm;
    private final HttpServletRequest request;

    public OpenAMResourceOwnerAuthentication(final String username, final char[] password, final String realm,
            final HttpServletRequest request) {
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.request = request;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }

    public String getRealm() {
        return realm;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpenAMResourceOwnerAuthentication that = (OpenAMResourceOwnerAuthentication) o;

        if (!Arrays.equals(password, that.password)) return false;
        if (realm != null ? !realm.equals(that.realm) : that.realm != null) return false;
        if (request != null ? !request.equals(that.request) : that.request != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? Arrays.hashCode(password) : 0);
        result = 31 * result + (realm != null ? realm.hashCode() : 0);
        result = 31 * result + (request != null ? request.hashCode() : 0);
        return result;
    }
}
