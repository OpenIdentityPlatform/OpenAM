/**
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
 * $Id: ReplayPasswd.java,v 1.6 2009/11/04 22:50:35 manish_rustagi Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.sun.identity.authentication.spi;

import com.iplanet.am.util.Misc;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.forgerock.openam.utils.StringUtils;

/**
 * This class is used to set the encrypted password as a session property.
 * It reads the value of the property "com.sun.am.replaypasswd.key" which is
 * the key that is to be used for DES Encryption. Once the password is 
 * encrypted, it assigns a session property "sunIdentityUserPassword" with 
 * this value.
 * This class is also used to set "sharepoint_login_attr_value" as a session
 * property to support Sharepoint. It reads the value of the property 
 * "com.sun.am.sharepoint_login_attr_name" which indicates the user token that
 * Sharepoint uses for authentication and gets its corresponding attribute 
 * value from the user datastore.It will then put this as a value of 
 * "sharepoint_login_attr_value" session property.
 * This class also sets the "owaAuthCookie" for the all the domains for which
 * "iPlanetdirectoryPro" cookie is set.  
 */
public class ReplayPasswd implements AMPostAuthProcessInterface {

    private static final String CIPHER_INSTANCE_NAME =
            "DES/ECB/NoPadding";

    private static final String REPLAY_PASSWORD_KEY =
            "com.sun.am.replaypasswd.key";

    private static final String SUN_IDENTITY_USER_PASSWORD =
            "sunIdentityUserPassword";

    private static final String IIS_OWA_ENABLED =
            "com.sun.am.iis_owa_enabled";

    private static final String OWA_AUTH_COOKIE =
            "owaAuthCookie";

    private static final String OWA_AUTH_COOKIE_VALUE =
            "amOwaValue";

    private static final String SHAREPOINT_LOGIN_ATTR_NAME =
            "com.sun.am.sharepoint_login_attr_name";

    private static final String SHAREPOINT_LOGIN_ATTR_VALUE =
            "sharepoint_login_attr_value";

    private static Debug debug = Debug.getInstance("ReplayPasswd");

    /**
     * Post processing on successful authentication.
     * @param requestParamsMap contains HttpServletRequest parameters
     * @param request HttpServlet  request
     * @param response HttpServlet response
     * @param ssoToken user's session
     * @throws AuthenticationException if there is an error while setting
     * the session password property
     */
    public void onLoginSuccess(Map requestParamsMap,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               SSOToken ssoToken) throws AuthenticationException {

        if (request == null) {
            debug.message("ReplayPasswd.onLoginSuccess: request is not available, password is not saved.");
            return;
        }

        if (debug.messageEnabled()) {
            debug.message("ReplayPasswd.onLoginSuccess called: Req:" + request.getRequestURL());
        }

        try {
            if (requestParamsMap == null) {
                debug.warning("ReplayPasswd: unable to get user password as requestParamsMap is null");
            } else {
                String userpasswd = (String) requestParamsMap.get(ISAuthConstants.SHARED_STATE_PASSWORD);
                if (StringUtils.isNotEmpty(userpasswd)) {
                    String encryptedPassword = encryptPassword(userpasswd);
                    if (StringUtils.isNotBlank(encryptedPassword)) {
                        ssoToken.setProperty(SUN_IDENTITY_USER_PASSWORD, encryptedPassword);
                    } else if (debug.warningEnabled()) {
                        debug.warning("ReplayPasswd: unable to set encrypted Password as encrypted value is empty");
                    }
                } else if (debug.warningEnabled()) {
                    debug.warning("ReplayPasswd: unable to get user password to encrypt");
                }
            }

            String iisOwaEnabled = SystemProperties.get(IIS_OWA_ENABLED);
            String strAttributeName = SystemProperties.get(SHAREPOINT_LOGIN_ATTR_NAME);

            if (Boolean.parseBoolean(iisOwaEnabled)) {
                // Set OWA Auth Cookie
                Cookie owaAuthCookie;
                for (String domain : AuthUtils.getCookieDomainsForRequest(request)) {
                    owaAuthCookie = CookieUtils.newCookie(OWA_AUTH_COOKIE, OWA_AUTH_COOKIE_VALUE, "/", domain);
                    CookieUtils.addCookieToResponse(response, owaAuthCookie);
                }
            }

            if (strAttributeName != null && !strAttributeName.trim().equals("")) {
                AMIdentity amIdentityUser = IdUtils.getIdentity(ssoToken);
                Map attrMap = amIdentityUser.getAttributes();
                String strAttributeValue = Misc.getMapAttr(attrMap, strAttributeName, null);
                if (strAttributeValue != null) {
                    ssoToken.setProperty(SHAREPOINT_LOGIN_ATTR_VALUE, strAttributeValue);
                }
                if (debug.messageEnabled()) {
                    debug.message("ReplayPasswd.onLoginSuccess: " + strAttributeName + "=" + strAttributeValue);
                }
            }

            if (debug.messageEnabled()) {
                debug.message("ReplayPasswd.onLoginSuccess: Replay password concluded successfully");
            }
        } catch (IdRepoException ire) {
            debug.error("ReplayPasswd.onLoginSuccess: IOException while fetching user attributes: " + ire);
        } catch (NoSuchAlgorithmException noe) {
            debug.error("ReplayPasswd.onLoginSuccess: NoSuchAlgorithmException"
                    + " while setting session password property: " + noe);
        } catch (InvalidKeyException ike) {
            debug.error("ReplayPasswd.onLoginSuccess: InvalidKeyException "
                    + "while setting session password property: " + ike);
        } catch (IllegalBlockSizeException ibe) {
            debug.error("ReplayPasswd.onLoginSuccess:IllegalBlockSizeException"
                    + " while setting session password property: " + ibe);
        } catch (NoSuchPaddingException npe) {
            debug.error("ReplayPasswd.onLoginSuccess: NoSuchPaddingException "
                    + "while setting session password property: " + npe);
        } catch (BadPaddingException bpe) {
            debug.error("ReplayPasswd.onLoginSuccess: BadPaddingException "
                    + "while setting session password property: " + bpe);
        } catch (SSOException sse) {
            debug.error("ReplayPasswd.onLoginSuccess: SSOException while setting session password property: "
                    + sse);
        }
    }

    /**
     * Post processing on failed authentication.
     * @param requestParamsMap contains HttpServletRequest parameters
     * @param req HttpServlet request
     * @param res HttpServlet response
     * @throws AuthenticationException if there is an error
     */
    public void onLoginFailure(Map requestParamsMap,
                               HttpServletRequest req,
                               HttpServletResponse res) throws AuthenticationException {
        debug.message("ReplayPasswd.onLoginFailure: called");
    }

    /**
     * Post processing on Logout.
     * @param req HttpServlet request
     * @param res HttpServlet response
     * @param ssoToken user's session
     * @throws AuthenticationException if there is an error
     */
    public void onLogout(HttpServletRequest req,
                         HttpServletResponse res,
                         SSOToken ssoToken) throws AuthenticationException {
        debug.message("ReplayPasswd.onLogout called");
    }

    /**
     * Encrypts the provided password.
     * @param userpasswd the password
     * @return the encrypted password
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws SSOException
     */
    private String encryptPassword(String userpasswd) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, SSOException {

        String deskeystr = SystemProperties.get(REPLAY_PASSWORD_KEY);
        if (StringUtils.isBlank(deskeystr)){
            if (debug.messageEnabled()) {
                debug.message("ReplayPasswd.encodePassword: cannot encode password, Replay PaswordKey is empty");
            }
            return null;
        }

        byte[] desKey = Base64.decode(deskeystr);
        SecretKeySpec keySpec = new SecretKeySpec(desKey, "DES");
        Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        // The array size must be a multiply of 8 (DES block size)
        int length = userpasswd.length() + (8 - userpasswd.length() % 8);
        byte[] data = new byte[length];
        System.arraycopy(userpasswd.getBytes(), 0, data, 0, userpasswd.length());
        byte[] ciphertext = cipher.doFinal(data);
        return Base64.encode(ciphertext);
    }
}