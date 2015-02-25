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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.time.TimeService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.entitlement.EntitlementException.CONDITION_EVALUTATION_FAILED;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * auth scheme.
 *
 * @since 12.0.0
 */
public class AuthSchemeCondition extends EntitlementConditionAdaptor {

    /**
     * Prefix for key that is used to set session property to track application based session idle timeout.
     */
    public static final String APPLICATION_IDLE_TIMESOUT_AT_PREFIX = "am.protected.policy.AppIdleTimesoutAt.";

    /**
     * <p>Key name that is used to communicate ForceAuth advice.</p>
     *
     * <p>This indicates to authentication service that user needs to authenticate again even if he has already
     * authenticated to required module instance, chain, level or realm.</p>
     */
    public static final String FORCE_AUTH_ADVICE = "ForceAuth";

    /**
     * Constant for representing authentication type of {@code module_instance}.
     */
    public static final String MODULE_INSTANCE = "module_instance";

    private final Debug debug;
    private final EntitlementCoreWrapper coreWrapper;
    private final TimeService timeService;

    private Set<String> authScheme = new HashSet<String>();
    private Integer applicationIdleTimeout = Integer.MAX_VALUE; //minutes
    private String applicationName;
    private String appIdleTimesoutAtSessionKey;
    private boolean appIdleTimeoutEnabled = false;

    /**
     * Constructs a new AuthSchemeCondition instance.
     */
    public AuthSchemeCondition() {
        this(PrivilegeManager.debug, new EntitlementCoreWrapper(), TimeService.SYSTEM);
    }

    /**
     * Constructs a new AuthSchemeCondition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the EntitlementCoreWrapper.
     * @param timeService An instance of the TimeService.
     */
    AuthSchemeCondition(Debug debug, EntitlementCoreWrapper coreWrapper, TimeService timeService) {
        this.debug = debug;
        this.coreWrapper = coreWrapper;
        this.timeService = timeService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);

            JSONArray authSchemes = jo.getJSONArray(AUTH_SCHEME);
            Set<String> authSc = new HashSet<String>();
            for (int i = 0; i < authSchemes.length(); i++) {
                authSc.add(authSchemes.getString(i));
            }
            setAuthScheme(authSc);

            if (jo.has(APPLICATION_NAME)) {
                setApplicationName(jo.getString(APPLICATION_NAME));
            }
            if (jo.has(APPLICATION_IDLE_TIMEOUT)) {
                setApplicationIdleTimeout(jo.getInt(APPLICATION_IDLE_TIMEOUT));
            }
        } catch (JSONException e) {
            debug.message("AuthSchemeCondition: Failed to set state", e);
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
            debug.message("At AuthSchemeCondition.getConditionDecision():entering:authScheme=" + authScheme
                    + ", appName=" + applicationName + ", applicationIdleTimeout=" + applicationIdleTimeout);
        }
        Set<String> requestAuthSchemes = null;
        Set<String> requestAuthSchemesIgnoreRealm = null;
        SSOToken token = (SSOToken) getValue(subject.getPrivateCredentials());
        if (env.get(REQUEST_AUTH_SCHEMES) != null) {
            requestAuthSchemes = env.get(REQUEST_AUTH_SCHEMES);
            if (debug.messageEnabled()) {
                debug.message("At AuthSchemeCondition.getConditionDecision(): requestAuthSchemes from env= "
                        + requestAuthSchemes);
            }
        } else {
            if (token != null) {
                requestAuthSchemes = coreWrapper.getRealmQualifiedAuthenticatedSchemes(token);
                requestAuthSchemesIgnoreRealm = coreWrapper.getAuthenticatedSchemes(token);
                if (debug.messageEnabled()) {
                    debug.message("At AuthSchemeCondition.getConditionDecision(): requestAuthSchemes from ssoToken= "
                            +  requestAuthSchemes);
                    debug.message("At AuthSchemeCondition.getConditionDecision(): requestAuthSchemesIgnoreRealm from "
                            + "ssoToken= " +  requestAuthSchemesIgnoreRealm);
                }
            }
        }

        if (requestAuthSchemes == null) {
            requestAuthSchemes = Collections.emptySet();
        }

        if (requestAuthSchemesIgnoreRealm == null) {
            requestAuthSchemesIgnoreRealm = Collections.emptySet();
        }

        boolean allowed = true;
        Set<String> adviceMessages = new HashSet<String>(authScheme.size());
        for (String authScheme : this.authScheme) {
            if (!requestAuthSchemes.contains(authScheme)) {
                String schemeRealm = AMAuthUtils.getRealmFromRealmQualifiedData(authScheme);
                if  ((schemeRealm != null) && (schemeRealm.length() != 0)) {
                    allowed = false;
                    adviceMessages.add(authScheme);
                    if (debug.messageEnabled()) {
                        debug.message("At AuthSchemeCondition.getConditionDecision():authScheme not satisfied = "
                                + authScheme);
                    }
                    break;
                } else if (schemeRealm == null || schemeRealm.length() == 0) {
                    if (!requestAuthSchemesIgnoreRealm.contains(authScheme)) {
                        allowed = false;
                        adviceMessages.add(authScheme);
                        if (debug.messageEnabled()) {
                            debug.message("At AuthSchemeCondition.getConditionDecision():authScheme not satisfied = "
                                    + authScheme);
                        }
                        break;
                    }
                }
            }
        }

        if (debug.messageEnabled()) {
            debug.message("At AuthSchemeCondition.getConditionDecision():authScheme = " + authScheme + ","
                    + " requestAuthSchemes = " + requestAuthSchemes + ",  allowed before applicationIdleTimeout "
                    + "check = " + allowed);
        }
        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        if (!allowed) {
            advices.put(AUTH_SCHEME_CONDITION_ADVICE, adviceMessages);
        }
        long timeToLive = Long.MAX_VALUE;

        //following additions are to support application idle timeout
        long currentTimeMillis = timeService.now();
        Set<String> expiredAuthSchemes = new HashSet<String>(); //a collector
        if (appIdleTimeoutEnabled) {
            if (allowed) { //condition satisfied pending idletimeout check
                //do idle timeout check
                long idleTimesOutAtMillis = getApplicationIdleTimesoutAt(token, expiredAuthSchemes, currentTimeMillis);
                if (idleTimesOutAtMillis <= currentTimeMillis) {
                    allowed = false;
                }
                if (debug.messageEnabled()) {
                    debug.message("At AuthSchemeCondition.getConditionDecision():currentTimeMillis = "
                            + currentTimeMillis + ", idleTimesOutAtMillis = " + idleTimesOutAtMillis
                            + ", expiredAuthSchemes = " + expiredAuthSchemes + ", allowed after applicationIdleTimeout "
                            + "check = " + allowed);
                }
            }

            if (allowed) { //condition satisfied
                long appIdleTimesoutAt = currentTimeMillis + getApplicationIdleTimeoutInMilliseconds();
                setTokenProperty(token, appIdleTimesoutAtSessionKey, Long.toString(appIdleTimesoutAt));
                timeToLive = appIdleTimesoutAt;
                if (debug.messageEnabled()) {
                    debug.message("At AuthSchemeCondition.getConditionDecision():app access allowed, revised "
                            + "appIdleTimesOutAt=" + appIdleTimesoutAt + ", currentTimeMillis=" + currentTimeMillis);
                }
            } else { //condiiton not satisifed
                adviceMessages.addAll(expiredAuthSchemes);
                advices.put(AUTH_SCHEME_CONDITION_ADVICE, adviceMessages);
                Set<String> forceAuthAdvices = new HashSet<String>();
                forceAuthAdvices.add(Boolean.TRUE.toString());
                advices.put(FORCE_AUTH_ADVICE, forceAuthAdvices);
            }

        }

        if (debug.messageEnabled()) {
            debug.message("At AuthSchemeCondition.getConditionDecision():just before return:allowed = " + allowed
                    + ", timeToLive = " + timeToLive + ", advices = " + advices );
        }

        return new ConditionDecision(allowed, advices, timeToLive);
    }

    private <T> T getValue(Set<T> values) {
        if (values != null && values.iterator().hasNext()) {
            return values.iterator().next();
        }
        return null;
    }

    private void setTokenProperty(SSOToken token, String key, String value) throws EntitlementException {
        try {
            token.setProperty(key, value);
        } catch (SSOException e) {
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        }
    }

    /**
     * Returns the time at which the application would idle time out.
     *
     * @param ssoToken The {@code SSOToken} of the user
     * @param expiredAuthSchemes A {@code Set} that would be filled with the authentication module instance names that
     *                           require re-authentication. This {@code Set} acts as a collector.
     * @param currentTimeMillis The current time in milli seconds.
     * @throws EntitlementException If the {@code SSOToken} is invalid.
     */
    private long getApplicationIdleTimesoutAt(SSOToken ssoToken, Set<String> expiredAuthSchemes, long currentTimeMillis)
            throws EntitlementException {

        try {
            long idleTimesoutAtMillis = 0;
            String idleTimesoutAtString = ssoToken.getProperty(appIdleTimesoutAtSessionKey);
            if (idleTimesoutAtString != null) {
                try {
                    idleTimesoutAtMillis = Long.parseLong(idleTimesoutAtString);
                } catch (NumberFormatException nfe) {
                    //this should not happen
                    if (debug.warningEnabled()) {
                        debug.warning("At AuthSchemeCondition.getApplicationIdleTimesoutAt():can not parse "
                                + "idleTimeoutAtMillis, defaulting to 0");
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("At AuthSchemeCondition.getApplicationIdleTimesoutAt():,idleTimeoutAtMillis based on "
                            + "last access=" + idleTimesoutAtMillis + ", currentTimeMillis=" + currentTimeMillis);
                }
            } else { //first visit to application
                if (debug.messageEnabled()) {
                    debug.message("At AuthSchemeCondition.getApplicationIdleTimesoutAt():" + appIdleTimesoutAtSessionKey
                            + " not set, first visit to application");
                }
            }
            if (idleTimesoutAtMillis <= currentTimeMillis) {
                for (String authScheme : this.authScheme) {
                    long authInstant = AMAuthUtils.getAuthInstant(ssoToken, MODULE_INSTANCE, authScheme);
                    idleTimesoutAtMillis = authInstant + getApplicationIdleTimeoutInMilliseconds();
                    if (debug.messageEnabled()) {
                        debug.message("At AuthSchemeCondition.getApplicationIdleTimesoutAt():authScheme=" + authScheme
                                + ",authInstant=" + authInstant + ",idleTimesoutAtMillis=" + idleTimesoutAtMillis
                                + ",currentTimeMillis=" + currentTimeMillis);
                    }
                    if (idleTimesoutAtMillis <= currentTimeMillis) {
                        expiredAuthSchemes.add(authScheme);
                        if (debug.messageEnabled()) {
                            debug.message("At AuthSchemeCondition.getApplicationIdleTimesoutAt():expired authScheme="
                                    + authScheme);
                        }
                        break;
                    }
                }
            }
            return idleTimesoutAtMillis;
        } catch (SSOException e) {
            debug.error("AuthSchemeCondition: Condition evaluation failed", e);
            throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
        }
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put(AUTH_SCHEME, getAuthScheme());
        jo.put(APPLICATION_NAME, getApplicationName());
        jo.put(APPLICATION_IDLE_TIMEOUT, getApplicationIdleTimeout());
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
            PrivilegeManager.debug.error("AuthSchemeCondition.toString()", e);
        }
        return s;
    }

    public Set<String> getAuthScheme() {
        return authScheme;
    }

    public void setAuthScheme(Set<String> authScheme) {
        this.authScheme = authScheme;
    }

    public Integer getApplicationIdleTimeout() {
        return applicationIdleTimeout == Integer.MAX_VALUE ? null : applicationIdleTimeout;
    }

    public void setApplicationIdleTimeout(int applicationIdleTimeout) {
        this.applicationIdleTimeout = applicationIdleTimeout;
        updateIdleTimeoutEnabled();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            this.applicationName = null;
        } else {
            this.appIdleTimesoutAtSessionKey = APPLICATION_IDLE_TIMESOUT_AT_PREFIX + applicationName;
            this.applicationName = applicationName;
        }
        updateIdleTimeoutEnabled();
    }

    private void updateIdleTimeoutEnabled() {
        if (applicationName != null && applicationIdleTimeout != Integer.MAX_VALUE) {
            appIdleTimeoutEnabled = true;
        }
    }

    private long getApplicationIdleTimeoutInMilliseconds() {
        return applicationIdleTimeout * 60 * 1000;
    }

    @Override
    public void validate() throws EntitlementException {
        if (authScheme == null || authScheme.isEmpty()) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, AUTH_SCHEME);
        }
        if (StringUtils.isAnyBlank(authScheme)) {
            throw new EntitlementException(EntitlementException.PROPERTY_CONTAINS_BLANK_VALUE, AUTH_SCHEME);
        }
    }
}
