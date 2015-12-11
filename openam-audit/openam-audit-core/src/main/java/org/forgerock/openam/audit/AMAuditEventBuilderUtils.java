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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.audit;

import static org.forgerock.openam.audit.AuditConstants.EVENT_REALM;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collection of static helper methods for use by AM AuditEventBuilders.
 *
 * @since 13.0.0
 */
public final class AMAuditEventBuilderUtils {

    private static Debug debug = Debug.getInstance(AuditConstants.DEBUG_NAME);

    private static final String COMPONENT = "component";

    private AMAuditEventBuilderUtils() {
        throw new UnsupportedOperationException("Utils class; should not be instantiated.");
    }

    /**
     * Set "component" audit log field.
     *
     * @param value String "component" value.
     */
    static void putComponent(JsonValue jsonValue, String value) {
        jsonValue.put(COMPONENT, value);
    }

    /**
     * Set "realm" audit log field.
     *
     * @param value String "realm" value.
     */
    static void putRealm(JsonValue jsonValue, String value) {
        jsonValue.put(EVENT_REALM, value);
    }

    /**
     * Gets the contextId value from the {@code SSOToken}.
     *
     * @param ssoToken The SSOToken from which the contextId value will be retrieved.
     * @return contextId for SSOToken or empty string if undefined.
     */
    public static String getTrackingIdFromSSOToken(SSOToken ssoToken) {
        return getSSOTokenProperty(ssoToken, Constants.AM_CTX_ID, "");
    }

    /**
     * Given the SSO token, retrieves the user's identifier.
     *
     * @param ssoToken
     *         the SSO token
     *
     * @return the associated user identifier
     */
    public static String getUserId(SSOToken ssoToken) {
        return getSSOTokenProperty(ssoToken, Constants.UNIVERSAL_IDENTIFIER, "");
    }

    private static String getSSOTokenProperty(SSOToken ssoToken, String name, String defaultValue) {
        if (ssoToken != null) {
            try {
                return ssoToken.getProperty(name);
            } catch (SSOException e) {
                debug.warning("Unable to obtain property '{}' from SSOToken.", name, e);
            }
        }
        return defaultValue;
    }

    /**
     * Get all available {@link AuditConstants.TrackingIdKey} values from the possible list of
     * {@link AuditConstants.TrackingIdKey} values, from the {@link AuditRequestContext}.
     *
     * @return All the available {@link AuditConstants.TrackingIdKey} values.
     */
    public static Set<String> getAllAvailableTrackingIds() {
        Set<String> trackingIdValues = new LinkedHashSet<>();

        for (AuditConstants.TrackingIdKey trackingIdKey : AuditConstants.TrackingIdKey.values()) {
            String contextKey = trackingIdKey.toString();
            String trackingIdValue = AuditRequestContext.getProperty(contextKey);
            if (StringUtils.isNotEmpty(trackingIdValue)) {
                trackingIdValues.add(trackingIdValue);
            }
        }

        return trackingIdValues;
    }

    /**
     * Generate a map of query parameters from the ampersand-separated list of key-value pairs.
     *
     * @param queryString HTTP query string.
     * @return Map of parameter keys to their values.
     */
    public static Map<String, List<String>> getQueryParametersAsMap(String queryString) {
        Map<String, List<String>> queryParameters = new LinkedHashMap<>();

        if (queryString != null) {
            for (String param : queryString.split("&")) {
                String[] nv = param.split("=", 2);
                if (nv.length == 2) {
                    String name = nv[0];
                    String value = nv[1];
                    List<String> list = queryParameters.get(name);
                    if (list == null) {
                        list = new ArrayList<>();
                        queryParameters.put(name, list);
                    }
                    list.add(value);
                }
            }
        }

        return queryParameters;
    }

}
