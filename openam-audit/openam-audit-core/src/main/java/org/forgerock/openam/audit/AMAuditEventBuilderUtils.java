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

import static org.forgerock.openam.audit.AuditConstants.Context.SESSION;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.openam.audit.AuditConstants.EVENT_REALM;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.utils.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of static helper methods for use by AM AuditEventBuilders.
 *
 * @since 13.0.0
 */
public final class AMAuditEventBuilderUtils {

    private static Debug debug = Debug.getInstance(AuditConstants.DEBUG_NAME);

    private static final String COMPONENT = "component";
    private static final String EXTRA_INFO = "extraInfo";
    private static final String CONTEXTS = "contexts";
//    private static final String REALM = "realm";

    private AMAuditEventBuilderUtils() {
        throw new UnsupportedOperationException("Utils class; should not be instantiated.");
    }

    /**
     * Set "component" audit log field.
     *
     * @param value String "component" value.
     */
    static void putComponent(JsonValue jsonValue, String value) {
        jsonValue.put(COMPONENT, value == null ? "" : value);
    }

    /**
     * Set "contexts" audit log field.
     *
     * @param value Map "contexts" value.
     */
    static void putContexts(JsonValue jsonValue, Map<String, String> value) {
        jsonValue.put(CONTEXTS, value == null ? new HashMap<String, String>() : value);
    }

    /**
     * Set "extraInfo" audit log field.
     *
     * @param values String sequence of values that should be stored in the 'extraInfo' audit log field.
     */
    static void putExtraInfo(JsonValue jsonValue, String... values) {
        jsonValue.put(EXTRA_INFO, json(array(values)));
    }

    /**
     * Sets "contextId" audit log field from property of {@link SSOToken}, iff the provided
     * <code>SSOToken</code> is not <code>null</code>.
     *
     * @param ssoToken The SSOToken from which the contextId value will be retrieved.
     */
    static void putContextIdFromSSOToken(JsonValue jsonValue, SSOToken ssoToken) {
        String ssoTokenContext = getContextFromSSOToken(ssoToken);
        putContexts(jsonValue, Collections.singletonMap(SESSION.toString(), ssoTokenContext));
    }

    /**
     * Set "realm" audit log field.
     *
     * @param value String "realm" value.
     */
    static void putRealm(JsonValue jsonValue, String value) {
        jsonValue.put(EVENT_REALM, value == null ? "" : value);
    }

    /**
     * Gets the contextId value from the {@code SSOToken}.
     *
     * @param ssoToken The SSOToken from which the contextId value will be retrieved.
     * @return contextId for SSOToken or empty string if undefined.
     */
    public static String getContextFromSSOToken(SSOToken ssoToken) {
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
     * Get all available {@link AuditConstants.Context} values from the possible list of
     * {@link AuditConstants.Context} values, from the {@link AuditRequestContext}.
     *
     * @return All the available {@link AuditConstants.Context} values.
     */
    public static Map<String, String> getAllAvailableContexts() {
        Map<String, String> map = new HashMap<>();

        for (AuditConstants.Context context : AuditConstants.Context.values()) {
            String contextKey = context.toString();
            String contextValue = AuditRequestContext.getProperty(contextKey);
            if (StringUtils.isNotEmpty(contextValue)) {
                map.put(contextKey, contextValue);
            }
        }

        return map;
    }

}
