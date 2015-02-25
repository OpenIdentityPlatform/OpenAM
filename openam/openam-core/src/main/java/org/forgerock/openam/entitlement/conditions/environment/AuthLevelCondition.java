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
 * Copyright 2006 Sun Microsystems Inc.
 */
/*
 * Portions Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.entitlement.EntitlementException.AUTH_LEVEL_NOT_INTEGER;
import static com.sun.identity.entitlement.EntitlementException.INVALID_PROPERTY_VALUE;
import static com.sun.identity.entitlement.EntitlementException.PROPERTY_VALUE_NOT_DEFINED;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * requested auth level is greater than or equal to the auth level set in the condition.
 *
 * @since 12.0.0
 */
public class AuthLevelCondition extends EntitlementConditionAdaptor {

    private static final String AUTH_LEVEL = "authLevel";
    private static final String REQUEST_AUTH_LEVEL = "requestAuthLevel";
    private static final String AUTH_LEVEL_CONDITION_ADVICE = "AuthLevelConditionAdvice";

    private final Debug debug;
    private final CoreWrapper coreWrapper;

    //realm qualified level
    private Integer authLevel;
    private String authRealm;

    /**
     * Constructs a new AuthLevelCondition instance.
     */
    public AuthLevelCondition() {
        this(PrivilegeManager.debug, new CoreWrapper());
    }

    /**
     * Constructs a new AuthLevelCondition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    AuthLevelCondition(Debug debug, CoreWrapper coreWrapper) {
        this.debug = debug;
        this.coreWrapper = coreWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            authLevel = jo.getInt(AUTH_LEVEL);
            authRealm = coreWrapper.getRealmFromRealmQualifiedData("" + authLevel);
        } catch (JSONException e) {
            debug.message("AuthLevelCondition: Failed to set state", e);
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

        if (authLevel == null) {
            throw new EntitlementException(PROPERTY_VALUE_NOT_DEFINED, new String[]{AUTH_LEVEL}, null);
        }

        boolean allowed = false;
        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        if (debug.messageEnabled()) {
            debug.message(getConditionName() + ".getConditionDecision():entering");
        }

        try {
            int maxRequestAuthLevel = getMaxRequestAuthLevel(env);
            if (maxRequestAuthLevel == Integer.MIN_VALUE) {
                SSOToken token = (SSOToken) subject.getPrivateCredentials().iterator().next();
                maxRequestAuthLevel = getMaxRequestAuthLevel(token);
            }

            allowed = isAllowed(maxRequestAuthLevel, advices);

            if (debug.messageEnabled()) {
                debug.message("At " + getConditionName() + ".getConditionDecision():authLevel=" + authLevel
                        + ",maxRequestAuthLevel=" + maxRequestAuthLevel + ",allowed = " + allowed);
            }
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("Problem getting auth level from SSOToken: " + e.getMessage(), e);
            }
        }
        return new ConditionDecision(allowed, advices);
    }

    /**
     * <p>Returns {@code true} if the requested auth level is greater than or equal to the configured auth level.</p>
     *
     * <p>Will also set an advice if the requested auth level is less than the configured auth level of the minimum
     * required auth level.</p>
     *
     * @param maxRequestAuthLevel {@inheritDoc}
     * @param advices {@inheritDoc}
     * @return {@code true} if the get {@code maxRequestAuthLevel} is greater than or equal to the configured auth
     * level.
     */
    protected boolean isAllowed(int maxRequestAuthLevel, Map<String, Set<String>> advices) {

        if (maxRequestAuthLevel >= authLevel) {
            return true;
        }

        Set<String> adviceMessages = new HashSet<String>(1);
        adviceMessages.add(authLevel.toString());
        advices.put(AUTH_LEVEL_CONDITION_ADVICE, adviceMessages);

        return false;
    }

    /**
     * Returns the name of the condition for use in logging.
     *
     * @return The name of the condition.
     */
    protected String getConditionName() {
        return "AuthLevelCondition";
    }

    /**
     * Gets the maximum auth level specified for the {@link #REQUEST_AUTH_LEVEL} property in the environment Map.
     *
     * @param env The environment map.
     * @return The maximum requested auth level.
     */
    private int getMaxRequestAuthLevel(Map<String, Set<String>> env) throws EntitlementException {
        int maxAuthLevel = Integer.MIN_VALUE;
        if (debug.messageEnabled()) {
            debug.message(getConditionName() + ".getMaxRequestAuthLevel(envMap,realm): entering: envMap= " + env
                    + ", authRealm= " + authRealm + ", conditionAuthLevel= " + authLevel);
        }
        Set<String> envAuthLevelObject = env.get(REQUEST_AUTH_LEVEL);
        if (envAuthLevelObject != null) {
            if (authRealm == null || authRealm.length() == 0) {
                maxAuthLevel = getHighestAuthLevel(envAuthLevelObject);
            } else {
                maxAuthLevel = getHighestRealmAuthLevel(envAuthLevelObject);
            }
        }
        if (debug.messageEnabled()) {
            debug.message(getConditionName() + ".getMaxRequestAuthLevel(): returning: maxAuthLevel=" + maxAuthLevel);
        }
        return maxAuthLevel;
    }

    private int getHighestAuthLevel(Set<String> levels) throws EntitlementException {
        int maxAuthLevel = Integer.MIN_VALUE;
        if (levels != null && !levels.isEmpty()) {
            for (String levelString : levels) {
                int level = getAuthLevel(levelString);
                maxAuthLevel = level > maxAuthLevel ? level : maxAuthLevel;
            }
        }

        return maxAuthLevel;
    }

    private int getHighestRealmAuthLevel(Set<String> levels) throws EntitlementException {
        int maxAuthLevel = Integer.MIN_VALUE;
        if (levels != null && !levels.isEmpty()) {
            for (String qualifiedLevel : levels) {
                String realm = coreWrapper.getRealmFromRealmQualifiedData(qualifiedLevel);
                if (authRealm.equals(realm)) {
                    int level = getAuthLevel(qualifiedLevel);
                    maxAuthLevel = level > maxAuthLevel ? level : maxAuthLevel;
                }
            }
        }

        return maxAuthLevel;
    }

    /**
     * Gets the maximum auth level specified for the {@link #REQUEST_AUTH_LEVEL} property in the SSO token.
     *
     * @param token The SSOToken.
     * @return The maximum auth level.
     */
    @SuppressWarnings("unchecked")
    private int getMaxRequestAuthLevel(SSOToken token) throws EntitlementException, SSOException {
        int maxAuthLevel;
        if (debug.messageEnabled()) {
            debug.message(getConditionName() + ".getMaxRequestAuthLevel(token,realm): entering: authRealm = "
                    + authRealm + ", conditionAuthLevel= " + authLevel);
        }
        if (authRealm == null || authRealm.length() == 0) {
            Set<String> levels = coreWrapper.getAuthenticatedLevels(token);
            if (debug.messageEnabled()) {
                debug.message(getConditionName() + ".getMaxRequestAuthLevel(): levels from token= " + levels);
            }
            maxAuthLevel = getHighestAuthLevel(levels);
        } else {
            Set<String> qualifiedLevels = coreWrapper.getRealmQualifiedAuthenticatedLevels(token);
            if (debug.messageEnabled()) {
                debug.message(getConditionName() + ".getMaxRequestAuthLevel(): qualifiedLevels from token= "
                        + qualifiedLevels);
            }
            maxAuthLevel = getHighestRealmAuthLevel(qualifiedLevels);
        }
        if (debug.messageEnabled()) {
            debug.message(getConditionName() + ".getMaxRequestAuthLevel(): returning:" + " maxAuthLevel= "
                    + maxAuthLevel);
        }
        return maxAuthLevel;
    }

    private int getAuthLevel(String qualifiedLevel) throws EntitlementException {
        String levelString = coreWrapper.getDataFromRealmQualifiedData(qualifiedLevel);
        try {
            return Integer.parseInt(levelString);
        } catch (NumberFormatException nfe) {
            if (debug.warningEnabled()) {
                debug.warning(getConditionName() + ".getAuthLevel(qualifiedLevel):got NumberFormatException:"
                        + "qualifiedLevel=" + qualifiedLevel + ", levelString = " + levelString);
            }
            throw new EntitlementException(AUTH_LEVEL_NOT_INTEGER, new Object[]{levelString}, nfe);
        }
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put(AUTH_LEVEL, authLevel);
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
            PrivilegeManager.debug.error(getConditionName() + ".toString()", e);
        }
        return s;
    }

    public Integer getAuthLevel() {
        return authLevel;
    }

    public void setAuthLevel(Integer authLevel) {
        this.authLevel = authLevel;
    }

    @Override
    public void validate() throws EntitlementException {
        if (authLevel == null) {
            throw new EntitlementException(PROPERTY_VALUE_NOT_DEFINED, AUTH_LEVEL);
        }

        if (authLevel < 0) {
            throw new EntitlementException(INVALID_PROPERTY_VALUE, AUTH_LEVEL, authLevel);
        }
    }
}
