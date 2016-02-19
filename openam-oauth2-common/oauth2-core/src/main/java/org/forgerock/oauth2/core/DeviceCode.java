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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.oauth2.core.Utils.*;
import static org.forgerock.openam.utils.Time.*;

import com.sun.jdi.IntegerValue;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.utils.CollectionUtils;

/**
 * Models a OpenAm OAuth2 Authorization Code.
 *
 * @since 13.0.0
 */
public class DeviceCode extends JsonValue implements Token {

    /**
     * Constructs a new OpenAMAuthorizationCode backed with the data in the specified JsonValue.
     *
     * @param token The JsonValue of the token.
     * @throws InvalidGrantException If the given token is not an Authorization Code token.
     */
    public DeviceCode(JsonValue token) throws InvalidGrantException {
        super(token);
        if (!OAuth2Constants.DeviceCode.DEVICE_CODE.equals(getTokenName()) || getTokenId() == null ||
                getUserCode() == null || getClientId() == null) {
            throw new InvalidGrantException();
        }
    }

    public DeviceCode(String deviceCode, String userCode, String resourceOwnerId, String clientId, String nonce,
            String responseType, String state, String acrValues, String prompt, String uiLocales, String loginHint,
            Integer maxAge, String claims, long expiryTime, Set<String> scope, String realm, String codeChallenge,
            String codeChallengeMethod) {
        super(object());
        setStringProperty(TOKEN_NAME, OAuth2Constants.DeviceCode.DEVICE_CODE);
        setStringProperty(OAuth2Constants.CoreTokenParams.ID, deviceCode);
        setStringProperty(OAuth2Constants.DeviceCode.USER_CODE, userCode);
        setStringProperty(OAuth2Constants.CoreTokenParams.USERNAME, resourceOwnerId);
        setStringProperty(OAuth2Constants.CoreTokenParams.CLIENT_ID, clientId);
        setStringProperty(OAuth2Constants.JWTTokenParams.NONCE, nonce);
        setStringProperty(OAuth2Constants.Params.RESPONSE_TYPE, responseType);
        setStringProperty(OAuth2Constants.Params.STATE, state);
        setStringProperty(OAuth2Constants.Params.ACR_VALUES, acrValues);
        setStringProperty(OAuth2Constants.Custom.PROMPT, prompt);
        setStringProperty(OAuth2Constants.Custom.UI_LOCALES, uiLocales);
        setStringProperty(OAuth2Constants.Params.LOGIN_HINT, loginHint);
        setStringProperty(OAuth2Constants.Params.MAX_AGE, maxAge == null ? null : String.valueOf(maxAge));
        setStringProperty(OAuth2Constants.Custom.CLAIMS, claims);
        setStringProperty(REALM, realm == null || realm.isEmpty() ? "/" : realm);
        setStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE, codeChallenge);
        setStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        put(EXPIRE_TIME, stringToSet(String.valueOf(expiryTime)));
        put(SCOPE, scope);
    }

    /**
     * Gets the Device Code - this is also the {@link #getTokenId() Token ID}.
     * @return The device code.
     */
    public String getDeviceCode() {
        return getTokenId();
    }

    /**
     * Gets the User Code.
     * @return The user code.
     */
    public String getUserCode() {
        return getStringProperty(OAuth2Constants.DeviceCode.USER_CODE);
    }

    /**
     * Gets the Resource Owner ID parameter.
     * @return The Resource Owner ID.
     */
    public String getResourceOwnerId() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.USERNAME);
    }

    /**
     * Sets the Resource Owner ID parameter.
     * @param resourceOwnerId The Resource Owner ID.
     */
    public void setResourceOwnerId(String resourceOwnerId) {
        setStringProperty(OAuth2Constants.CoreTokenParams.USERNAME, resourceOwnerId);
    }

    /**
     * Gets the Client ID parameter.
     * @return The Client ID.
     */
    public String getClientId() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.CLIENT_ID);
    }

    /**
     * Gets the Nonce parameter.
     * @return The nonce.
     */
    public String getNonce() {
        return getStringProperty(OAuth2Constants.JWTTokenParams.NONCE);
    }

    /**
     * Gets the ACR Values parameter.
     * @return The ACR Values String.
     */
    public String getAcrValues() {
        return getStringProperty(OAuth2Constants.Params.ACR_VALUES);
    }

    /**
     * Gets the Code Challenge Method parameter.
     * @return The code challenge method.
     */
    public String getCodeChallengeMethod() {
        return getStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE_METHOD);
    }

    /**
     * Gets the Code Challenge parameter.
     * @return The code challenge.
     */
    public String getCodeChallenge() {
        return getStringProperty(OAuth2Constants.Custom.CODE_CHALLENGE);
    }

    /**
     * Gets the max age parameter.
     * @return The max age.
     */
    public Integer getMaxAge() {
        final String value = getStringProperty(OAuth2Constants.Params.MAX_AGE);
        return value == null ? null : Integer.valueOf(value);
    }

    /**
     * Gets the login hint parameter.
     * @return The login hint.
     */
    public String getLoginHint() {
        return getStringProperty(OAuth2Constants.Params.LOGIN_HINT);
    }

    /**
     * Gets the UI Locales parameter.
     * @return The ui locales.
     */
    public String getUiLocales() {
        return getStringProperty(OAuth2Constants.Custom.UI_LOCALES);
    }

    /**
     * Gets the prompt parameter.
     * @return The prompt.
     */
    public String getPrompt() {
        return getStringProperty(OAuth2Constants.Custom.PROMPT);
    }

    /**
     * Gets the state parameter.
     * @return The state.
     */
    public String getState() {
        return getStringProperty(OAuth2Constants.Params.STATE);
    }

    /**
     * Gets the response type parameter.
     * @return The response type.
     */
    public String getResponseType() {
        return getStringProperty(OAuth2Constants.Params.RESPONSE_TYPE);
    }

    /**
     * Returns the requested claims.
     * @return The requested claims.
     */
    public String getClaims() {
        return getStringProperty(OAuth2Constants.Custom.CLAIMS);
    }

    /**
     * Updates the last poll time of this device code.
     */
    public void poll() {
        this.put("lastQueried", CollectionUtils.asSet(String.valueOf(currentTimeMillis())));
    }

    /**
     * Gets the time that this device code was last polled by the device.
     * @return The last poll time in milliseconds.
     */
    public long getLastPollTime() {
        final String lastQueried = getStringProperty("lastQueried");
        return lastQueried == null ? -1 : Long.valueOf(lastQueried);
    }

    /**
     * Gets the realm.
     * @return The realm.
     */
    public String getRealm() {
        return getStringProperty(REALM);
    }

    @Override
    public String getTokenId() {
        return getStringProperty(OAuth2Constants.CoreTokenParams.ID);
    }

    @Override
    public String getTokenName() {
        return getStringProperty(TOKEN_NAME);
    }

    /**
     * Gets the expiry time of this device code.
     * @return The expiry time (ms since epoch).
     */
    public long getExpiryTime() {
        final Set<String> value = getParameter(EXPIRE_TIME);
        if (CollectionUtils.isNotEmpty(value)) {
            return Long.parseLong(value.iterator().next());
        }
        return 0;
    }

    /**
     * Gets the set of scopes requested.
     * @return The scope values.
     */
    public Set<String> getScope() {
        final Set<String> value = getParameter(SCOPE);
        if (CollectionUtils.isNotEmpty(value)) {
            return value;
        }
        return Collections.emptySet();
    }

    /**
     * Whether this device code has been issued.
     */
    public boolean isIssued() {
        Set<String> issued = getParameter(ISSUED);

        return issued != null && Boolean.parseBoolean(issued.iterator().next());
    }

    @Override
    public Map<String, Object> toMap() throws ServerException {
        return (Map<String, Object>) getObject();
    }

    @Override
    public Map<String, Object> getTokenInfo() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the specified parameter from the JsonValue.
     *
     * @param paramName The parameter name.
     * @return A {@code Set} of the parameter values.
     */
    private Set<String> getParameter(String paramName) {
        final JsonValue param = get(paramName);
        if (param != null) {
            return (Set<String>) param.getObject();
        }
        return null;
    }

    protected String getStringProperty(String key) {
        final Set<String> value = getParameter(key);
        if (CollectionUtils.isNotEmpty(value)) {
            return value.iterator().next();
        }
        return null;
    }

    private void setStringProperty(String key, String value) {
        if (value != null) {
            put(key, CollectionUtils.asSet(value));
        }
    }

    public void setAuthorized(boolean authorized) {
        setStringProperty("AUTHORIZED", Boolean.toString(authorized));
    }

    public boolean isAuthorized() {
        return Boolean.valueOf(getStringProperty("AUTHORIZED"));
    }
}
