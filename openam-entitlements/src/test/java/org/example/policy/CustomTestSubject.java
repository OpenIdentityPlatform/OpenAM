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
 * Portions copyright 2026 3A Systems LLC.
 */
package org.example.policy;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Minimal EntitlementSubject fixture in a non-OpenAM package, proving that
 * createDefaultObject accepts custom plugins regardless of package prefix.
 */
public class CustomTestSubject implements EntitlementSubject {

    @Override
    public void setState(String state) {}

    @Override
    public String getState() {
        return "{}";
    }

    @Override
    public Map<String, Set<String>> getSearchIndexAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Set<String> getRequiredAttributeNames() {
        return Collections.emptySet();
    }

    @Override
    public SubjectDecision evaluate(String realm, SubjectAttributesManager mgr,
            Subject subject, String resourceName, Map<String, Set<String>> environment)
            throws EntitlementException {
        return new SubjectDecision(true, Collections.emptyMap());
    }

    @Override
    public boolean isIdentity() {
        return false;
    }
}
