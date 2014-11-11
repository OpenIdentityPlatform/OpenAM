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
 * Portions Copyright 2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import static com.sun.identity.entitlement.EntitlementException.PROPERTY_VALUE_NOT_DEFINED;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * principal has authenticated to the specified realm.
 *
 * @since 12.0.0
 */
public class AuthenticateToRealmCondition extends EntitlementConditionAdaptor {

    private final Debug debug;
    private final EntitlementCoreWrapper entitlementCoreWrapper;

    private String authenticateToRealm;

    /**
     * Constructs a new AuthenticateToRealmCondition instance.
     */
    public AuthenticateToRealmCondition() {
        this(PrivilegeManager.debug, new EntitlementCoreWrapper());
    }

    /**
     * Constructs a new AuthenticateToRealmCondition instance.
     *
     * @param debug A Debug instance.
     * @param entitlementCoreWrapper An instance of the EntitlementCoreWrapper.
     */
    AuthenticateToRealmCondition(Debug debug, EntitlementCoreWrapper entitlementCoreWrapper) {
        this.debug = debug;
        this.entitlementCoreWrapper = entitlementCoreWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            authenticateToRealm = jo.getString("authenticateToRealm");
        } catch (JSONException e) {
            debug.message("AuthenticateToRealmCondition: Failed to set state", e);
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
    @SuppressWarnings("unchecked")
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        if (authenticateToRealm == null) {
            throw new EntitlementException(PROPERTY_VALUE_NOT_DEFINED, new Object[]{AUTHENTICATE_TO_REALM}, null);
        }

        // We don't care about case of the realm when doing the comparison so use a CaseInsensitiveHashSet
        Set<String> requestAuthnRealms = new CaseInsensitiveHashSet();
        if (env.get(REQUEST_AUTHENTICATED_TO_REALMS) != null) {
                requestAuthnRealms.addAll(env.get(REQUEST_AUTHENTICATED_TO_REALMS));
                if (debug.messageEnabled()) {
                    debug.message("At AuthenticateToRealmCondition.getConditionDecision(): requestAuthnRealms, from "
                            + "request = " + requestAuthnRealms);
                }
        } else {
            SSOToken token = (SSOToken) subject.getPrivateCredentials().iterator().next();
            Set<String> authenticatedRealms = entitlementCoreWrapper.getAuthenticatedRealms(token);
            if (authenticatedRealms != null) {
                requestAuthnRealms.addAll(authenticatedRealms);
            }
            if (debug.messageEnabled()) {
                debug.message("At AuthenticateToRealmCondition.getConditionDecision(): requestAuthnRealms, from "
                        + "ssoToken = " + requestAuthnRealms);
            }
        }

        boolean allowed = true;
        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        Set<String> adviceMessages = new HashSet<String>(1);
        if (!requestAuthnRealms.contains(authenticateToRealm)) {
            allowed = false;
            adviceMessages.add(authenticateToRealm);
            advices.put(AUTHENTICATE_TO_REALM_CONDITION_ADVICE, adviceMessages);
            if (debug.messageEnabled()) {
                debug.message("At AuthenticateToRealmCondition.getConditionDecision():authenticateToRealm not "
                        + "satisfied = " + authenticateToRealm);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("At AuthenticateToRealmCondition.getConditionDecision():authenticateToRealm = "
                    + authenticateToRealm + "," + "requestAuthnRealms = " + requestAuthnRealms + ", " + " allowed = "
                    + allowed);
        }
        return new ConditionDecision(allowed, advices);
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("authenticateToRealm", authenticateToRealm);
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
            PrivilegeManager.debug.error("AuthenticateToRealmCondition.toString()", e);
        }
        return s;
    }

    public String getAuthenticateToRealm() {
        return authenticateToRealm;
    }

    public void setAuthenticateToRealm(String authenticateToRealm) {
        this.authenticateToRealm = authenticateToRealm;
    }
}
