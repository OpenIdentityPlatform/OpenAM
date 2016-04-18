package org.forgerock.oauth2.core;

import java.util.Map;
import java.util.Set;

public interface RefreshToken extends IntrospectableToken, Token {

    void setAuthModules(String authModules);

    String getRedirectUri();

    String getTokenId();

    String getAuthenticationContextClassReference();

    boolean isNeverExpires();

    String getTokenType();

    String getAuthModules();

    Map<String, Object> toMap();

    String getStringProperty(String key);
}
