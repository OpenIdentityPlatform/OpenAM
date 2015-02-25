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

import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A policy subject condition that examines claims in a Json Web Token (JWT) subject, such as an OpenID Connect
 * ID token. Currently only supports testing claims for string equality.
 */
public class JwtClaimSubject implements EntitlementSubject {
    private static final Debug DEBUG = Debug.getInstance("amEntitlements");
    private static final String CLAIM_FIELD = "claimName";
    private static final String VALUE_FIELD = "claimValue";

    private static final Map<String, Set<String>> NO_ADVICE = Collections.emptyMap();

    private String claimName;
    private String claimValue;


    @Override
    public void setState(final String state) {
        try {
            final JSONObject json = new JSONObject(state);
            this.claimName = json.getString(CLAIM_FIELD);
            this.claimValue = json.getString(VALUE_FIELD);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getState() {
        try {
            final JSONObject json = new JSONObject();
            json.put(CLAIM_FIELD, claimName);
            json.put(VALUE_FIELD, claimValue);
            return json.toString(2);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Set<String>> getSearchIndexAttributes() {
        return Collections.singletonMap(SubjectAttributesCollector.NAMESPACE_IDENTITY,
                Collections.singleton(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES));
    }

    @Override
    public Set<String> getRequiredAttributeNames() {
        return Collections.emptySet();
    }

    @Override
    public SubjectDecision evaluate(final String realm, final SubjectAttributesManager mgr, final Subject subject,
                                    final String resourceName, final Map<String, Set<String>> environment)
            throws EntitlementException {

        final Set<JwtPrincipal> jwts = subject.getPrincipals(JwtPrincipal.class);

        if (jwts.isEmpty()) {
            DEBUG.message("No JWT principal in subject");
            return new SubjectDecision(false, NO_ADVICE);
        }

        final JwtPrincipal jwt = jwts.iterator().next();
        final String value = jwt.getClaim(claimName);

        final boolean match = StringUtils.equals(claimValue, value);
        return new SubjectDecision(match, NO_ADVICE);
    }

    @Override
    public boolean isIdentity() {
        return true;
    }

    public void setClaimName(String claim) {
        this.claimName = claim;
    }

    public String getClaimName() {
        return claimName;
    }

    public void setClaimValue(String value) {
        this.claimValue = value;
    }

    public String getClaimValue() {
        return claimValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JwtClaimSubject that = (JwtClaimSubject) o;

        return StringUtils.equals(this.claimName, that.claimName)
            && StringUtils.equals(this.claimValue, that.claimValue);
    }

    @Override
    public int hashCode() {
        int result = claimName != null ? claimName.hashCode() : 0;
        result = 31 * result + (claimValue != null ? claimValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JwtClaimSubject{ claimName='" + claimName + "', claimValue='" + claimValue + "' }";
    }
}
