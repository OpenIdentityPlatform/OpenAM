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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

/**
 * A genuine {@link EntitlementSubject} whose no-argument constructor throws an unchecked exception.
 * <p>
 * It passes the resolver's type check, so it exercises the instantiation path: {@code
 * Class.newInstance()} would rethrow the {@code RuntimeException} undeclared, escaping every
 * caller's reflection {@code catch} clauses, whereas the resolver must surface it as a declared
 * {@link InstantiationException} that the deserialization call sites already handle.
 */
public class ThrowingConstructorSubject implements EntitlementSubject {

    public ThrowingConstructorSubject() {
        throw new IllegalStateException("constructor deliberately fails");
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return "";
    }

    @Override
    public Map<String, Set<String>> getSearchIndexAttributes() {
        return null;
    }

    @Override
    public Set<String> getRequiredAttributeNames() {
        return null;
    }

    @Override
    public SubjectDecision evaluate(String realm, SubjectAttributesManager mgr, Subject subject,
            String resourceName, Map<String, Set<String>> environment) {
        return null;
    }

    @Override
    public boolean isIdentity() {
        return false;
    }
}
