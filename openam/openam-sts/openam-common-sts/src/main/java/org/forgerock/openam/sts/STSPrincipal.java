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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;

import java.security.Principal;

/**
 * The responses from all successful token validations must include a Principal implementation associated with the
 * validated token. This class implements the Principal interface for the STS.
 */
public class STSPrincipal implements Principal {
    private final String name;
    public STSPrincipal(String name) {
        if (name == null) {
            throw new IllegalArgumentException("The name parameter provided to the STSPrincipal ctor cannot be null.");
        }
        this.name = name;
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof STSPrincipal) {
            STSPrincipal otherPrincipal = (STSPrincipal)other;
            return name.equals(otherPrincipal.getName());
        }
        return false;
    }
}
