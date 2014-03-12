/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.policy.plugins;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A policy engine condition plugin that allows a condition to test the scope attribute of an OAuth2 access token.
 * Accepts a set of required scopes and checks that any request includes all of those required scopes. Users should
 * pass the request scopes in the {@code scope} attribute of the environment when evaluating the policy. How those
 * scopes are determined is not specified, but typically may involve sending the OAuth2 access token to a tokenInfo
 * end point to determine the scopes associated with the token.
 *
 * @since 12.0.0
 */
public class OAuth2ScopeCondition implements Condition {

    /**
     * The property name used to configure the required scopes.
     */
    public static final String OAUTH2_SCOPE_PROPERTY = "OAuth2Scope";

    /**
     * The attribute that should be sent in the environment map for policy evaluation requests against this condition.
     */
    public static final String REQUEST_SCOPE_ATTRIBUTE = "scope";

    private static final Debug DEBUG = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    /**
     * Allowed delimiters in the scope parameter. Must not overlap with the allowed scope characters below.
     */
    private static final String SCOPE_DELIMITERS = "\\s+";

    /**
     * Valid characters allowed in an OAuth2 scope - defined in
     * <a href="http://tools.ietf.org/html/rfc6749#section-3.3">RFC 6749 Section 3.3.</a>
     */
    private static final Pattern VALID_SCOPE_PATTERN = Pattern.compile("[\\x21\\x23-\\x5B\\x5D-\\x7E]+");

    /**
     * Set of scopes that are required for this condition to allow the request. Configured via the
     * {@link #OAUTH2_SCOPE_PROPERTY} property.
     */
    private volatile Set<String> requiredScopes = Collections.emptySet();

    /**
     * Default constructor initialises the condition plugin with no required scopes.
     * <strong>Note:</strong> this default constructor is required by the policy engine.
     */
    public OAuth2ScopeCondition() {
        // Nothing to initialise
    }

    /**
     * Copy constructor used by the {@link #clone()} method.
     *
     * @param toCopy the condition to copy.
     */
    private OAuth2ScopeCondition(OAuth2ScopeCondition toCopy) {
        this.requiredScopes = toCopy.requiredScopes;
    }

    /**
     * {@inheritDoc} Only a single property is defined by this condition: {@link #OAUTH2_SCOPE_PROPERTY}.
     */
    @Override
    public List<String> getPropertyNames() {
        return Collections.singletonList(OAUTH2_SCOPE_PROPERTY);
    }

    /**
     * {@inheritDoc} The OAuth2 scope is defined as a space-delimited string.
     */
    @Override
    public Syntax getPropertySyntax(String property) {
        return Syntax.ANY;
    }

    /**
     * Gets the display name of the given property for the given locale.
     * @param property {@inheritDoc}
     * @param locale {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public String getDisplayName(String property, Locale locale) throws PolicyException {
        ResourceBundle rb = ResourceBundle.getBundle(ResBundleUtils.rbName, locale);
        return rb.getString(property);
    }

    /**
     * Returns the valid values for the given property. Not used by this condition.
     *
     * @param property property name
     * @return an empty set of values.
     */
    @Override
    public Set<String> getValidValues(String property) {
        // This method is only used for the multi-choice and single-choice list picker syntax which we do not use.
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     * <p>Only a single property is allowed (and required) by this condition: {@link #OAUTH2_SCOPE_PROPERTY}. The value
     * of that property should be a space-delimited string of scope values in the syntax defined by the OAuth2 spec,
     * section 3.3.</p>
     *
     * @see #VALID_SCOPE_PATTERN valid scope syntax
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setProperties(Map<String, Set<String>> properties) throws PolicyException {
        if (properties == null || properties.isEmpty()) {
            throw new PolicyException(ResBundleUtils.rbName, "properties_can_not_be_null_or_empty", null, null);
        }

        // Create a generified copy of the parameters to simplify argument handling
        Map<String, Set<String>> propertyMap = new LinkedHashMap<String, Set<String>>(properties);

        Set<String> scopeValues = propertyMap.remove(OAUTH2_SCOPE_PROPERTY);
        if (!propertyMap.isEmpty()) {
            // Additional invalid properties were specified
            Object[] args = new Object[] { propertyMap.keySet().iterator().next() };
            throw new PolicyException(ResBundleUtils.rbName, "attempt_to_set_invalid_property", args, null);
        }

        if (scopeValues == null || scopeValues.isEmpty()) {
            throw new PolicyException(
                    ResBundleUtils.rbName, "property_value_not_defined", new String[] { OAUTH2_SCOPE_PROPERTY }, null);
        }
        String scopeStr = scopeValues.iterator().next();

        this.requiredScopes = toScopeSet(scopeStr);
    }

    /**
     * {@inheritDoc}. Only a single property ({@link #OAUTH2_SCOPE_PROPERTY}) is defined for this condition. Its value is
     * a space-delimited string of OAuth2 scopes.
     */
    @Override
    public Map<String, Set<String>> getProperties() {
        // Convert the scope set back into a space-delimited string
        return Collections.singletonMap(OAUTH2_SCOPE_PROPERTY,
                Collections.singleton(StringUtils.join(requiredScopes, ' ')));
    }

    /**
     * Determines the policy decision based on the required scopes configured for this condition and the set of scopes
     * defined in the request environment. The {@code env} map should contain a single key {@code scope}, which contains
     * a space-delimited set of OAuth2 scopes associated with the request. The condition decision will be true if the
     * request scopes contain all of the required scopes for this condition, otherwise the request will be denied.
     * Currently no policy advice is returned by this condition.
     *
     * @param token the SSOToken. Unused.
     * @param env the request environment. Must contain a String-valued {@code scope} entry.
     * @return a decision based on the OAuth2 scopes defined in the request.
     */
    @Override
    public ConditionDecision getConditionDecision(SSOToken token, Map<String, Set<String>> env) throws PolicyException, SSOException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Entering OAuth2ScopeCondition.getConditionDecision(). Required scopes=" + requiredScopes
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
        } catch (PolicyException ex) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Invalid scope in request: " + ex.getMessage(), ex);
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("OAuth2ScopeCondition decision: " + allowed);
        }

        // TODO: Do we need policy advice for OAuth2 scopes?
        return new ConditionDecision(allowed);
    }

    /**
     * Returns a clone of this condition with the same required scopes.
     *
     * @return a clone of this condition.
     */
    @Override
    public OAuth2ScopeCondition clone() {
        return new OAuth2ScopeCondition(this);
    }

    /**
     * Converts a parameter value into a set of scope values by first converting to a string and then splitting on
     * whitespace characters. Also checks that all scope values are valid according to the syntax defined in the OAuth2
     * spec.
     *
     * @param value the parameter value to convert to a set of scopes.
     * @return the set of scopes.
     * @throws PolicyException if the value is invalid.
     */
    private Set<String> toScopeSet(String value) throws PolicyException {
        final Set<String> scopes = new LinkedHashSet<String>();
        if (value != null) {
            for (String scope : value.split(SCOPE_DELIMITERS)) {
                if (!VALID_SCOPE_PATTERN.matcher(scope.trim()).matches()) {
                    throw new PolicyException(ResBundleUtils.rbName, "invalid_oauth2_scope", new Object[] { scope },
                            null);
                }
                scopes.add(scope.trim());
            }
        }
        return scopes;
    }
}
