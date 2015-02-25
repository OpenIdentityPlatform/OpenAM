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
 * Copyright 2006 Sun Microsystems Inc
 */
/*
 * Portions Copyright 2010-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import static com.sun.identity.entitlement.EntitlementException.CONDITION_EVALUTATION_FAILED;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.LDAP_FILTER;

import javax.security.auth.Subject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.shared.debug.Debug;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

public class LDAPFilterCondition extends EntitlementConditionAdaptor {

    private final Debug debug = PrivilegeManager.debug;
    private final com.sun.identity.policy.plugins.LDAPFilterCondition condition;

    /**
     * Constructs a new LDAPFilterCondition instance.
     */
    public LDAPFilterCondition() {
        this.condition = new com.sun.identity.policy.plugins.LDAPFilterCondition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            setLdapFilter(jo.optString(LDAP_FILTER, null));
        } catch (Exception e) {
            debug.message("LDAPFilterCondition: Failed to set state", e);
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

        SSOToken token = (SSOToken) getValue(subject.getPrivateCredentials());
        try {
            com.sun.identity.policy.ConditionDecision decision = condition.getConditionDecision(token, env);
            return new ConditionDecision(decision.isAllowed(), decision.getAdvices(), decision.getTimeToLive());
        } catch (PolicyException e) {
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        } catch (SSOException e) {
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        }
    }

    private <T> T getValue(Collection<T> values) {
        if (values != null && values.iterator().hasNext()) {
            return values.iterator().next();
        }
        return null;
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put(LDAP_FILTER, getLdapFilter());
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
            PrivilegeManager.debug.error("LDAPFilterCondition.toString()", e);
        }
        return s;
    }

    @SuppressWarnings("unused") // Set by JSON mapping
    public void setLdapFilter(String ldapFilter) throws EntitlementException {
        final Map<String, Set<String>> properties = new HashMap<String, Set<String>>(condition.getProperties());
        properties.put(LDAP_FILTER, Collections.singleton(ldapFilter));
        try {
            condition.setProperties(properties);
        } catch (PolicyException e) {
            throw new EntitlementException(EntitlementException.INVALID_PROPERTY_VALUE,
                    new Object[] { LDAP_FILTER, ldapFilter }, e);
        }
    }

    @SuppressWarnings("unchecked")
    public String getLdapFilter() {
        return getValue((Collection<String>) condition.getProperties().get(LDAP_FILTER));
    }

    @Override
    public void validate() throws EntitlementException {
        try {
            condition.validate();
        } catch (PolicyException e) {
            throw new EntitlementException(EntitlementException.INVALID_VALUE, e.getLocalizedMessage());
        }
    }
}
