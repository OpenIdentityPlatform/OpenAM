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

package org.forgerock.openam.entitlement.conditions.environment;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.sun.identity.entitlement.EntitlementException.INVALID_OAUTH2_SCOPE;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * request OAuth2 scopes are sufficient to gain access.
 *
 * @since 12.0.0
 */
public class OAuth2ScopeCondition extends EntitlementConditionAdaptor {

    /**
     * The attribute that should be sent in the environment map for policy evaluation requests against this condition.
     */
    public static final String REQUEST_SCOPE_ATTRIBUTE = "scope";

    /**
     * Allowed delimiters in the scope parameter. Must not overlap with the allowed scope characters below.
     */
    private static final String SCOPE_DELIMITERS = "\\s+";

    /**
     * Valid characters allowed in an OAuth2 scope - defined in
     * <a href="http://tools.ietf.org/html/rfc6749#section-3.3">RFC 6749 Section 3.3.</a>
     */
    private static final Pattern VALID_SCOPE_PATTERN = Pattern.compile("[\\x21\\x23-\\x5B\\x5D-\\x7E]+");

    private static final String ATTR_SCOPES = "requiredScopes";

    private final Debug debug;

    /**
     * Set of scopes that are required for this condition to allow the request.
     */
    private volatile Set<String> requiredScopes = new HashSet<String>();

    /**
     * Constructs a new OAuth2ScopeCondition instance.
     */
    public OAuth2ScopeCondition() {
        debug = PrivilegeManager.debug;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            JSONArray scopes = jo.getJSONArray(ATTR_SCOPES);
            for (int i = 0; i < scopes.length(); i++) {
                requiredScopes.add(scopes.getString(i));
            }
        } catch (JSONException e) {
            debug.message("OAuth2ScopeCondition: Failed to set state", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {
        if (debug.messageEnabled()) {
            debug.message("Entering OAuth2ScopeCondition.getConditionDecision(). Required scopes=" + requiredScopes
                    + ". Request scopes=" + (env == null ? "<missing>" : env.get(REQUEST_SCOPE_ATTRIBUTE)));
        }

        Set<String> scopeParams = (env == null ? null : env.get(REQUEST_SCOPE_ATTRIBUTE));
        String scopes = "";
        if (scopeParams != null && !scopeParams.isEmpty()) {
            scopes = scopeParams.iterator().next();
        }

        boolean allowed = false;

        try {
            Set<String> requestScopes = toScopeSet(scopes);
            allowed = requestScopes.containsAll(requiredScopes);
        } catch (EntitlementException e) {
            if (debug.messageEnabled()) {
                debug.message("Invalid scope in request: " + e.getMessage(), e);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("OAuth2ScopeCondition decision: " + allowed);
        }

        // TODO: Do we need policy advice for OAuth2 scopes?
        return new ConditionDecision(allowed, Collections.<String, Set<String>>emptyMap());
    }

    /**
     * Converts a parameter value into a set of scope values by first converting to a string and then splitting on
     * whitespace characters. Also checks that all scope values are valid according to the syntax defined in the OAuth2
     * spec.
     *
     * @param value the parameter value to convert to a set of scopes.
     * @return the set of scopes.
     * @throws EntitlementException if the value is invalid.
     */
    private Set<String> toScopeSet(String value) throws EntitlementException {
        final Set<String> scopes = new LinkedHashSet<String>();
        if (value != null) {
            for (String scope : value.split(SCOPE_DELIMITERS)) {
                if (!VALID_SCOPE_PATTERN.matcher(scope.trim()).matches()) {
                    if (debug.errorEnabled()) {
                        debug.error("OAuth2ScopeCondition.toScopeSet(): invalid OAuth2 scope, " + scope);
                    }
                    throw new EntitlementException(INVALID_OAUTH2_SCOPE, scope);
                }
                scopes.add(scope.trim());
            }
        }
        return scopes;
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        JSONArray scopes = new JSONArray();
        for (String scope : requiredScopes) {
            scopes.put(scope);
        }
        jo.put(ATTR_SCOPES, scopes);
        return jo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("OAuth2ScopeCondition.toString()", e);
        }
        return s;
    }

    public Set<String> getRequiredScopes() {
        return requiredScopes;
    }

    public void setRequiredScopes(Set<String> requiredScopes) {
        this.requiredScopes = requiredScopes;
    }

    @Override
    public void validate() throws EntitlementException {
        if (requiredScopes == null || requiredScopes.isEmpty()) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, ATTR_SCOPES);
        }

        for (String scope : requiredScopes) {
            if (!VALID_SCOPE_PATTERN.matcher(scope.trim()).matches()) {
                throw new EntitlementException(INVALID_OAUTH2_SCOPE, scope);
            }
        }
    }
}
