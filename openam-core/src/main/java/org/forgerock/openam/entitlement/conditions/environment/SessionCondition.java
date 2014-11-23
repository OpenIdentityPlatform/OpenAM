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
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.time.TimeService;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.entitlement.EntitlementException.CONDITION_EVALUTATION_FAILED;
import static com.sun.identity.entitlement.EntitlementException.UNABLE_TO_PARSE_SSOTOKEN_AUTHINSTANT;

/**
 * <p>An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that defines the maximum user
 * session time during which a policy applies.</p>
 *
 * <p>This is an option to terminate the user session if the session time exceeds the maximum allowed.</p>
 *
 * @since 12.0.0
 */
public class SessionCondition extends EntitlementConditionAdaptor {

    /**
     * Key that is used to define the user session creation time of the request. This is passed in to the {@code env}
     * parameter while invoking {@code getConditionDecision} method of the {@code SessionCondition}. Value for the key
     * should be a {@code Long} whose value is time in milliseconds since epoch.
     */
    public static final String REQUEST_SESSION_CREATION_TIME = "requestSessionCreationTime";

    /**
     * Key that is used to identify the advice messages from this condition.
     */
    public static final String SESSION_CONDITION_ADVICE = "SessionConditionAdvice";

    /**
     * Key that is used in the {@code Advice} to identify the session was terminated.
     */
    public static final String ADVICE_TERMINATE_SESSION = "terminateSession";

    /**
     * Key that is used in the {@code Advice} to identify the condition decision is {@code deny}.
     */
    public static final String ADVICE_DENY = "deny";

    private static final String SSOTOKEN_PROPERTY_AUTHINSTANT = "authInstant";

    private final Debug debug;
    private final CoreWrapper coreWrapper;
    private final TimeService timeService;

    private long maxSessionTime;
    private boolean terminateSession;

    /**
     * Constructs a new SessionCondition instance.
     */
    public SessionCondition() {
        this(PrivilegeManager.debug, new CoreWrapper(), TimeService.SYSTEM);
    }

    /**
     * Constructs a new SessionCondition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the CoreWrapper.
     * @param timeService An instance of the TimeService;
     */
    SessionCondition(Debug debug, CoreWrapper coreWrapper, TimeService timeService) {
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
            setMaxSessionTime(jo.getLong("maxSessionTime"));
            setTerminateSession(jo.getBoolean("terminateSession"));
        } catch (JSONException e) {
            debug.message("SessionCondition: Failed to set state", e);
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
        if (token == null) {
            return new ConditionDecision(true, Collections.<String, Set<String>>emptyMap(), Long.MAX_VALUE);
        }

        String requestSessionCreationTime = getValue(env.get(REQUEST_SESSION_CREATION_TIME));

        long tokenCreationTime;
        if (requestSessionCreationTime != null) {
            tokenCreationTime = Long.parseLong(requestSessionCreationTime);
        } else {
            try {
                tokenCreationTime = DateUtils.stringToDate(token.getProperty(SSOTOKEN_PROPERTY_AUTHINSTANT)).getTime();
            } catch (ParseException e) {
                throw new EntitlementException(UNABLE_TO_PARSE_SSOTOKEN_AUTHINSTANT, e);
            } catch (SSOException e) {
                throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
            }
        }

        long currentTime = timeService.now();
        long expiredTime = tokenCreationTime + maxSessionTime;
        if (debug.messageEnabled()) {
            debug.message("SessionCondition.getConditionDecision():\n  currentTime: " + currentTime +
                    "\n  expiredTime: " + expiredTime);
        }

        if (currentTime < expiredTime) {
            return new ConditionDecision(true, Collections.<String, Set<String>>emptyMap(), expiredTime);
        } else {
            Map<String, Set<String>> advices = new HashMap<String, Set<String>>(1);
            Set<String> adviceMessages = new HashSet<String>(2);
            adviceMessages.add(ADVICE_DENY);

            if (terminateSession) {
                // set advice message
                adviceMessages.add(ADVICE_TERMINATE_SESSION);
                // terminate token session
                try {
                    coreWrapper.destroyToken(token);
                    debug.message("SessionCondition.getConditionDecision(): successfully terminated user session!");
                } catch (SSOException ssoEx) {
                    if (debug.warningEnabled()) {
                        debug.warning("SessionCondition.getConditionDecision(): failed to terminate user session!",
                                ssoEx);
                    }
                }
            }
            advices.put(SESSION_CONDITION_ADVICE, adviceMessages);
            return new ConditionDecision(false, advices, Long.MAX_VALUE);
        }
    }

    private <T> T getValue(Set<T> values) {
        if (values != null && values.iterator().hasNext()) {
            return values.iterator().next();
        }
        return null;
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("maxSessionTime", getMaxSessionTime());
        jo.put("terminateSession", isTerminateSession());
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
            PrivilegeManager.debug.error("SessionCondition.toString()", e);
        }
        return s;
    }

    public long getMaxSessionTime() {
        return maxSessionTime / 60000;
    }

    public void setMaxSessionTime(long maxSessionTime) {
        this.maxSessionTime = maxSessionTime * 60000;
    }

    public boolean isTerminateSession() {
        return terminateSession;
    }

    public void setTerminateSession(boolean terminateSession) {
        this.terminateSession = terminateSession;
    }

    @Override
    public void validate() throws EntitlementException {
        if (maxSessionTime < 0L) {
            throw new EntitlementException(EntitlementException.INVALID_PROPERTY_VALUE, "maxSessionTime",
                    maxSessionTime);
        }
    }
}
