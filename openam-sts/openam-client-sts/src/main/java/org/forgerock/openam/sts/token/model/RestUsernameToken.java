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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.token.model;

import org.forgerock.util.Reject;

import java.util.Arrays;

/**
 * Class representing username and password state for rest-sts token transformations initiated by a UsernameToken. For
 * the soap-sts, a UsernameToken is represented by the org.apache.ws.security.message.token.UsernameToken class.
 * Ultimately, the RestUsernameTokenValidator will delegate its  authentication to an AuthenticationHandler parameterized
 * to this class.
 *
 */
public class RestUsernameToken {
    private final byte[] username;
    private final byte[] password;

    public RestUsernameToken(byte[] username, byte[] password) {
        this.username = username;
        this.password = password;
        Reject.ifNull(username, "Username cannot be null!");
        Reject.ifNull(password, "Password cannot be null!");
    }

    public byte[] getUsername() {
        return username;
    }

    public byte[] getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RestUsernameToken that = (RestUsernameToken) o;
        return Arrays.equals(username, that.username) && Arrays.equals(password, that.password);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(username);
        result = 31 * result + Arrays.hashCode(password);
        return result;
    }
}
