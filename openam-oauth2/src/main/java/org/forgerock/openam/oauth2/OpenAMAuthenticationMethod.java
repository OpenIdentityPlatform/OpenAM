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

package org.forgerock.openam.oauth2;

import com.sun.identity.authentication.AuthContext;
import org.forgerock.oauth2.core.AuthenticationMethod;
import org.forgerock.util.Reject;

/**
 * Indicates a method of authentication for OpenAM consisting of an {@link AuthContext.IndexType} plus a name of
 * a service/module/etc.
 */
public final class OpenAMAuthenticationMethod implements AuthenticationMethod {
    private final String name;
    private final AuthContext.IndexType indexType;

    public OpenAMAuthenticationMethod(final String name, final AuthContext.IndexType indexType) {
        Reject.ifNull(name, indexType);
        this.name = name;
        this.indexType = indexType;
    }

    @Override
    public String getName() {
        return name;
    }

    public AuthContext.IndexType getIndexType() {
        return indexType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OpenAMAuthenticationMethod that = (OpenAMAuthenticationMethod) o;

        return indexType.equals(that.indexType) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + indexType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("OpenAMAuthenticationMethod [type=%s, name=%s]", indexType, name);
    }
}
