package org.forgerock.oauth2.core;

import java.util.Map;
import java.util.Set;

public interface AccessToken extends IntrospectableToken, Token {

    String getNonce();

    String getSessionId();

    String getTokenId();

    String getClaims();

    String getTokenType();

    String getGrantType();

    Map<String, Object> toMap();

    void addExtraData(String key, String value);

    @Override
    String toString();

    String getStringProperty(String key);
}
