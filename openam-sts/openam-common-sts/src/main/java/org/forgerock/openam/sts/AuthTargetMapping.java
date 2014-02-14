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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Each deployed STS instance will be configured with a mapping which specifies the rest authN authIndexType and authIndexValue
 * against which a particular token type will be validated.
 * An instance of this class will be harvested from the UI elements configuring STS instances.
 *
 */
public class AuthTargetMapping {

    public static class AuthTargetMappingBuilder {
        private final Map<Class<?>, AuthTarget> mappings = new HashMap<Class<?>, AuthTarget>();

        public AuthTargetMappingBuilder addMapping(Class<?> tokenClass, String authIndexType, String authIndexValue)  {
            mappings.put(tokenClass, new AuthTarget(authIndexType, authIndexValue));
            return this;
        }

        public AuthTargetMapping build() {
            return new AuthTargetMapping(this);
        }
    }

    public static class AuthTarget {
        private final String authIndexType;
        private final String authIndexValue;

        AuthTarget(String authIndexType, String authIndexValue) {
            if ((authIndexType == null) || (authIndexValue == null)) {
                throw new IllegalArgumentException("authIndexType or authIndexValue were null!");
            }
            this.authIndexType = authIndexType;
            this.authIndexValue = authIndexValue;
        }

        public String getAuthIndexType() {
            return authIndexType;
        }

        public String getAuthIndexValue() {
            return authIndexValue;
        }

        @Override
        public String toString() {
            return "AuthIndexType: " + authIndexType + "; AuthIndexValue: " + authIndexValue;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof AuthTarget) {
                AuthTarget otherTarget = (AuthTarget)other;
                return authIndexType.equals(otherTarget.getAuthIndexType()) &&
                        authIndexValue.equals(otherTarget.getAuthIndexValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
         return (authIndexType + authIndexValue).hashCode();
        }

    }

    private final Map<Class<?>, AuthTarget> mappings;

    private AuthTargetMapping(AuthTargetMappingBuilder builder) {
        this.mappings = Collections.unmodifiableMap(builder.mappings);
    }

    public static AuthTargetMappingBuilder builder() {
        return new AuthTargetMappingBuilder();
    }

    /*
      If a mapping is not present, null will be returned. This will allow the caller (e.g. the AuthenticationUriProvider)
      to know when to decorate the URI.
     */
    public AuthTarget getAuthTargetMapping(Class<?> tokenClass) {
        return mappings.get(tokenClass);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("AuthTargetMapping instance with mappings:\n");
        builder.append(mappings);
        return builder.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AuthTargetMapping) {
            AuthTargetMapping otherMapping = (AuthTargetMapping)other;
            return mappings.equals(otherMapping.mappings);
        }
        return false;
    }
}
