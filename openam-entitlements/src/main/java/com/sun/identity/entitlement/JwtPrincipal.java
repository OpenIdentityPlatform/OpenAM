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

package com.sun.identity.entitlement;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.util.Reject;

import java.security.Principal;

/**
 * A security principal based on a Json Web Token (JWT). The name of the principal is the "sub" claim in the JWT.
 */
public class JwtPrincipal implements Principal {
    private final JsonValue jwt;

    public JwtPrincipal(final JsonValue jwt) {
        Reject.ifNull(jwt);
        if (!jwt.get("sub").isString()) {
            throw new IllegalArgumentException("Invalid JWT");
        }
        this.jwt = jwt;
    }

    @Override
    public String getName() {
        return jwt.get("sub").asString();
    }

    public String getClaim(String key) {
        return jwt.get(key).asString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JwtPrincipal principal = (JwtPrincipal) o;

        return jwt.toString().equals(principal.jwt.toString());
    }

    @Override
    public int hashCode() {
        return jwt.toString().hashCode();
    }

    @Override
    public String toString() {
        return "JwtPrincipal{ claims = " + jwt + " }";
    }
}
