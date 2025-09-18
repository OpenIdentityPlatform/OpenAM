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
 * Copyright 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.authentication.modules.adaptive;

import static org.forgerock.openam.authentication.modules.adaptive.Adaptive.*;

import java.security.AccessController;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;

/**
 * Post Authentication Plugin for the Adaptive authentication module.
 * The adaptive authentication plugin serves to save cookies and profile attributes after successful authentication.
 * Add it to your authentication chains that use the adaptive authentication module configured to save cookies and
 * profile attributes.
 */
public class AdaptivePostAuthenticationPlugin implements AMPostAuthProcessInterface {
    private static final String ADAPTIVE = "amAuthAdaptive";
    private static final Debug debug = Debug.getInstance(ADAPTIVE);

    @Override
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response,
                               SSOToken token) throws AuthenticationException {

        debug.message("{} executing PostProcessClass", ADAPTIVE);

        try {
            String s = token.getProperty("ADAPTIVE");
            if (s != null && !s.isEmpty()) {
                Map<String, String> adaptiveState = stringToMap(s);
                token.setProperty("ADAPTIVE", "");

                if (adaptiveState != null) {
                    if (adaptiveState.containsKey("IPSAVE")) {
                        String value = adaptiveState.get("IPSAVE");
                        String name = adaptiveState.get("IPAttr");

                        // Now we save the attribs, since we can do it in one shot
                        try {
                            AMIdentity id = IdUtils.getIdentity(
                                    AccessController.doPrivileged(AdminTokenAction.getInstance()),
                                    token.getProperty(Constants.UNIVERSAL_IDENTIFIER));
                            id.setAttributes(Collections.singletonMap(name, Collections.singleton(value)));
                            id.store();
                        } catch (Exception e) {
                            debug.error("{}.onLoginSuccess : Unable to save Attribute : {} - {}", ADAPTIVE, name, value, e);
                        }
                    }

                    int autoLoginExpire = (int) TimeUnit.SECONDS.convert(365L, TimeUnit.DAYS); // 1 year, configurable?
                    final Set<String> cookieDomains = AuthUtils.getCookieDomainsForRequest(request);

                    processKey(adaptiveState, "LOGINNAME", "LOGINVALUE", response, cookieDomains, autoLoginExpire);
                    processKey(adaptiveState, "COOKIENAME", "COOKIEVALUE", response, cookieDomains, autoLoginExpire);
                    processKey(adaptiveState, "DEVICENAME", "DEVICEVALUE", response, cookieDomains, autoLoginExpire);
                }
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("{}.getIdentity : Unable to Retrieve PostAuthN Params", ADAPTIVE, e);
            }
        }
    }

    private void processKey(Map<String, String> adaptiveState, String nameKey, String valueKey,
                            HttpServletResponse response, Set<String> cookieDomains, int autoLoginExpire) {
        if (adaptiveState != null && adaptiveState.containsKey(nameKey)) {
            String name = adaptiveState.get(nameKey);
            String value = adaptiveState.get(valueKey);

            addCookieToResponse(response, cookieDomains, name, value, autoLoginExpire);
        }
    }

    private void addCookieToResponse(HttpServletResponse response, Set<String> cookieDomains, String name,
                                     String value, int expire) {
        for (String domain : cookieDomains) {
            CookieUtils.addCookieToResponse(response, CookieUtils.newCookie(name, value, expire, "/", domain));
        }
    }

    @Override
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
    }

    @Override
    public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken token)
            throws AuthenticationException {
    }
}