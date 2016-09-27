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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session;

import java.security.MessageDigest;
import java.util.List;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.Constants;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.forgerock.http.header.CookieHeader;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.services.context.Context;

/**
 * This class attempts to map the session token hash to a session defined in the HttpContext.
 * <p>
 * If {@literal user-session-token} header is present it tries to map the hash to the session id defined in the header.
 * If {@literal user-session-token} header is not present it attempts to map the hash to session id defined in
 * {@literal amCookie} header.
 * <p>
 * If the mapping fails it throws SSOException
 *
 * The token hash is provided as resource id, instead of actual session token id,  to SessionResource and
 * SessionPropertiesResource. This is to ensure that intermediate proxies does not log the token id in plain text.
 *
 * @see SessionResourceV2
 * @see SessionPropertiesResource
 * @see TokenOwnerAuthzModule
 *
 * @since 14.0.0
 */
public class TokenHashToIDMapper {

    public static final String END_USER_SESSION_ID_COOKIE = "user-session-token";
    public static final String AM_COOKIE = SystemProperties.get(Constants.AM_COOKIE_NAME, "iPlanetDirectoryPro");
    private static final String SHA_256 = "SHA-256";
    private static final String UTF_8 = "UTF-8";
    private static final String COOKIE = "cookie";

    /**
     * Maps the token Hash to the session token on the context
     *
     * @param context   The context.
     * @param tokenHash The session token hash.
     * @return The SSO session token ID
     * @throws SSOException Thrown when fails to map.
     */
    public String map(Context context, String tokenHash) throws SSOException {
        String tokenId = getToken(context);
        if (tokenId != null) {
            byte[] decodedTokenBytes = new byte[0];
            try {
                decodedTokenBytes = Hex.decodeHex(tokenHash.toCharArray());
            } catch (DecoderException e) {
               //ignore
            }
            if (MessageDigest.isEqual(decodedTokenBytes, hash(tokenId))) {
                return tokenId;
            }
        }
        throw new SSOException("Failed to map the token hash " + tokenHash + " to a session token defined in context");
    }

    private String getToken(Context context) {
        String tokenId = endUserSessionToken(context);
        return tokenId == null ? defaultToken(context) : tokenId;

    }

    private String endUserSessionToken(Context context) {
        String tokenId = fromHeader(context, END_USER_SESSION_ID_COOKIE);
        return tokenId == null ? fromCookie(context, END_USER_SESSION_ID_COOKIE) : tokenId;
    }

    private String defaultToken(Context context) {
        String tokenId = fromHeader(context, AM_COOKIE);
        return tokenId == null ? fromCookie(context, AM_COOKIE) : tokenId;
    }

    private String fromHeader(Context context, String cookieName) {
        List<String> headers = context.asContext(HttpContext.class).getHeader(COOKIE);
        for (String header : headers) {
            for (org.forgerock.http.protocol.Cookie cookie : CookieHeader.valueOf(header).getCookies()) {
                if (cookie.getName().equalsIgnoreCase(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String fromCookie(Context context, String cookieName) {
        final List<String> header = context.asContext(HttpContext.class).getHeader(cookieName.toLowerCase());
        if (!header.isEmpty()) {
            return header.get(0);
        }
        return null;
    }

    /**
     * Creates hex encoded SHA-256 hash of the token
     *
     * @param token The session token
     * @return The SHA-256 hash of the token
     */
    byte[] hash(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance(SHA_256);
            sha.update(token.getBytes(UTF_8));
            return sha.digest();
        } catch (Exception ex) {
            return null;
        }
    }
}