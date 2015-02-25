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

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.entitlement.EntitlementException.PROPERTY_VALUE_NOT_DEFINED;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * principal has authenticated to the specified service.
 *
 * @since 12.0.0
 */
public class AuthenticateToServiceCondition extends EntitlementConditionAdaptor {
    private static final String AUTHENTICATE_TO_SERVICE_ATTR = "authenticateToService";
    private final Debug debug;
    private final CoreWrapper coreWrapper;
    private final EntitlementCoreWrapper entitlementCoreWrapper;

    private String authenticateToService = null;
    private boolean realmEmpty = false;

    /**
     * Constructs a new AuthenticateToServiceCondition instance.
     */
    public AuthenticateToServiceCondition() {
        this(PrivilegeManager.debug, new CoreWrapper(), new EntitlementCoreWrapper());
    }

    /**
     * Constructs a new AuthenticateToServiceCondition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the CoreWrapper.
     * @param entitlementCoreWrapper An instance of the EntitlementCoreWrapper.
     */
    AuthenticateToServiceCondition(Debug debug, CoreWrapper coreWrapper, EntitlementCoreWrapper entitlementCoreWrapper) {
        this.debug = debug;
        this.coreWrapper = coreWrapper;
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
            authenticateToService = jo.getString(AUTHENTICATE_TO_SERVICE_ATTR);
            String realm = coreWrapper.getRealmFromRealmQualifiedData(authenticateToService);
            realmEmpty = StringUtils.isBlank(realm);
        } catch (JSONException e) {
            debug.message("AuthenticateToServiceCondition: Failed to set state", e);
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

        if (StringUtils.isBlank(authenticateToService)) {
            if (debug.errorEnabled()) {
                debug.error("AuthenticateToServiceCondition.evaluate(): property value not defined, " +
                AUTHENTICATE_TO_SERVICE);
            }
            throw new EntitlementException(PROPERTY_VALUE_NOT_DEFINED, new Object[]{AUTHENTICATE_TO_SERVICE}, null);
        }

        boolean allowed = false;
        Set<String> requestAuthnServices = new HashSet<String>();
        if (env.get(REQUEST_AUTHENTICATED_TO_SERVICES) != null) {
            requestAuthnServices.addAll(env.get(REQUEST_AUTHENTICATED_TO_SERVICES));
            if (debug.messageEnabled()) {
                debug.message("At AuthenticateToServiceCondition.evaluate(): requestAuthnServices "
                        + "from request = " + requestAuthnServices);
            }
        } else {
            SSOToken token = (SSOToken) subject.getPrivateCredentials().iterator().next();
            Set<String> authenticatedServices = entitlementCoreWrapper.getRealmQualifiedAuthenticatedServices(token);
            if (authenticatedServices != null) {
                requestAuthnServices.addAll(authenticatedServices);
            }
            if (debug.messageEnabled()) {
                debug.message("At AuthenticateToServiceCondition.evaluate(): requestAuthnServices from "
                        + "ssoToken = " + requestAuthnServices);
            }
        }

        if (requestAuthnServices.contains(authenticateToService)) {
            allowed = true;
        } else if (realmEmpty) {
            for (String requestAuthnService : requestAuthnServices) {
                String service = coreWrapper.getDataFromRealmQualifiedData(requestAuthnService);
                if (authenticateToService.equals(service)) {
                    allowed = true;
                    break;
                }
            }
        }

        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        final String realmAwareService = getRealmAwareService(authenticateToService, realm);

        if (!allowed) {
            Set<String> adviceMessages = new HashSet<String>(1);

            adviceMessages.add(realmAwareService);
            advices.put(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE, adviceMessages);
            if (debug.messageEnabled()) {
                debug.message("At AuthenticateToServiceCondition.evaluate():authenticateToService not "
                        + "satisfied = " + realmAwareService);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("At AuthenticateToServiceCondition.evaluate():authenticateToService = "
                    + realmAwareService + "," + " requestAuthnServices = " + requestAuthnServices + ", "
                    + " allowed = " + allowed);
        }
        return new ConditionDecision(allowed, advices);
    }

    /**
     * Confirms that the String is in the format /:service, by validating that
     * there is a colon in the String - as per
     * {@link com.sun.identity.authentication.util.AMAuthUtils#getDataFromRealmQualifiedData}.
     */
    private String getRealmAwareService(String authenticateToService, String realm) {

        if (!authenticateToService.contains(ISAuthConstants.COLON)) {
            return realm + ISAuthConstants.COLON + authenticateToService;
        }

        return authenticateToService;
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put(AUTHENTICATE_TO_SERVICE_ATTR, authenticateToService);
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
            PrivilegeManager.debug.error("AuthenticateToServiceCondition.toString()", e);
        }
        return s;
    }

    public String getAuthenticateToService() {
        return authenticateToService;
    }

    public void setAuthenticateToService(String authenticateToService) {
        this.authenticateToService = authenticateToService;
    }

    @Override
    public void validate() throws EntitlementException {
        if (StringUtils.isBlank(authenticateToService)) {
            throw new EntitlementException(PROPERTY_VALUE_NOT_DEFINED, AUTHENTICATE_TO_SERVICE);
        }
    }
}
