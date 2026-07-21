/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.AUTHN_BIND_REQUEST;
import static org.forgerock.opendj.ldap.LDAPConnectionFactory.CONNECT_TIMEOUT;
import static org.forgerock.opendj.ldap.LDAPConnectionFactory.SSL_CONTEXT;

import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;
import org.json.JSONObject;

/**
 * Pure, request/response-free helpers used by {@link SetupPage}, ported from the old Click-era
 * {@code AjaxPage} so the configurator keeps byte-identical validation semantics. Kept as a
 * separate class rather than folded into {@code SetupPage} because none of these need page state.
 */
public final class SetupUtils {

    public static final int MIN_PASSWORD_SIZE = 8;

    private SetupUtils() {
    }

    public static Connection getConnection(String host, int port, String bindDN, char[] bindPwd, int timeout,
            boolean isSSl) throws GeneralSecurityException, LdapException {
        Options ldapOptions = Options.defaultOptions()
                .set(CONNECT_TIMEOUT, new Duration((long) timeout, TimeUnit.SECONDS))
                .set(AUTHN_BIND_REQUEST, LDAPRequests.newSimpleBindRequest(bindDN, bindPwd));

        if (isSSl) {
            String defaultProtocolVersion = SystemProperties.get(Constants.LDAP_SERVER_TLS_VERSION, "TLS");
            ldapOptions = ldapOptions.set(SSL_CONTEXT,
                    new SSLContextBuilder().setProtocol(defaultProtocolVersion).getSSLContext());
        }

        ConnectionFactory factory = new LDAPConnectionFactory(host, port, ldapOptions);
        return factory.getConnection();
    }

    /**
     * Maps an LDAP {@link ResultCode} to an {@code amConfigurator} i18n key, or {@code null} if
     * there is no specific message for that code.
     */
    public static String getMessage(ResultCode resultCode) {
        if (ResultCode.CLIENT_SIDE_CONNECT_ERROR.equals(resultCode)) {
            return "ldap.connect.error";
        } else if (ResultCode.CLIENT_SIDE_SERVER_DOWN.equals(resultCode)) {
            return "ldap.server.down";
        } else if (ResultCode.INVALID_DN_SYNTAX.equals(resultCode)) {
            return "ldap.invalid.dn";
        } else if (ResultCode.NO_SUCH_OBJECT.equals(resultCode)) {
            return "ldap.nosuch.object";
        } else if (ResultCode.INVALID_CREDENTIALS.equals(resultCode)) {
            return "ldap.invalid.credentials";
        } else if (ResultCode.UNWILLING_TO_PERFORM.equals(resultCode)) {
            return "ldap.unwilling";
        } else if (ResultCode.INAPPROPRIATE_AUTHENTICATION.equals(resultCode)) {
            return "ldap.inappropriate";
        } else if (ResultCode.CONSTRAINT_VIOLATION.equals(resultCode)) {
            return "ldap.constraint";
        } else {
            return null;
        }
    }

    public static boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        return value.equalsIgnoreCase("on")
                || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("checked");
    }

    public static int parseInt(String value) {
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // fall through to 0
            }
        }
        return 0;
    }

    public static String jsonResponse(boolean valid, String responseBody) {
        return jsonResponse(String.valueOf(valid), responseBody);
    }

    public static String jsonResponse(String valid, String responseBody) {
        JSONObject json = new JSONObject();
        json.put("valid", valid);
        json.put("body", responseBody);
        return json.toString();
    }
}
