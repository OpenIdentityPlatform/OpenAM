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

import org.forgerock.oauth2.core.ClientAuthentication;

import javax.servlet.http.HttpServletRequest;

/**
 * @since 12.0.0
 */
public class OpenAMClientAuthentication implements ClientAuthentication {

    private final String clientId;
    private final String clientSecret;
    private final String realm;

    public OpenAMClientAuthentication(final String clientId, final String clientSecret, final String realm) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpenAMClientAuthentication that = (OpenAMClientAuthentication) o;

        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (clientSecret != null ? !clientSecret.equals(that.clientSecret) : that.clientSecret != null) return false;
        if (realm != null ? !realm.equals(that.realm) : that.realm != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
        result = 31 * result + (realm != null ? realm.hashCode() : 0);
        return result;
    }
}
