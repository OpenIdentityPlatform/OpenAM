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
 * $Id: Authenticator.java,v 1.2 2008/06/25 05:52:24 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;


import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.authentication.internal.InvalidAuthContextException;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

class Authenticator {
    private ResourceBundle bundle;
    private SSOToken ssoToken;

    private static final String LOGIN_STATUS = "iplanet-am-user-login-status";
    private static final String ACCOUNT_LIFE = "iplanet-am-user-account-life";
    private static final String STRING_ACTIVE = "active";

    private static Set ACTIVE_STATE_ATTRIBUTES = new HashSet(4);

    static {
        ACTIVE_STATE_ATTRIBUTES.add(LOGIN_STATUS);
        ACTIVE_STATE_ATTRIBUTES.add(ACCOUNT_LIFE);
    }
    
    Authenticator(ResourceBundle bundle) {
        this.bundle = bundle;
    }
    
    SSOToken getSSOToken() {
        return ssoToken;
    }

    void destroySSOToken() {
        if (ssoToken != null) {
            try {
                SSOTokenManager.getInstance().destroyToken(ssoToken);
            } catch (SSOException e) {
                // ignore token may be already destroyed.
                AdminUtils.log("Authenticator.destroySSOToken",e);
            }
        }
    }
    
    AuthContext sessionBasedLogin(String bindUser, String bindPwd)
        throws AdminException
    {
        try {
            return sessionBasedLoginInternal(bindUser, bindPwd);
        } catch (AdminException e) {
            logLoginFailure(bindUser);
            throw e;
        }
    }

    private AuthContext sessionBasedLoginInternal(
        String bindUser,
        String bindPwd
    ) throws AdminException {
        AuthContext lc = getAuthContext();
        processCallback(lc, bindUser, bindPwd);
        
        try {
            ssoToken = lc.getSSOToken();
        } catch (Exception e) {
            throw new AdminException(e);
        }

        return lc;
    }

    void ldapLogin(String bindUser, String bindPwd)
        throws AdminException {
        String installTime = SystemProperties.get(
            AdminTokenAction.AMADMIN_MODE, "false");
        if (installTime.equalsIgnoreCase("false")) {
            try {
                sessionBasedLoginInternal(bindUser, bindPwd);
                AdminUtils.setSSOToken(ssoToken);
            } catch (Exception e) {
                ldapLoginInternal(bindUser, bindPwd);
            }
        }
 
        /*
         * Even if installTime is false, ssoToken could be null if none of
         * the access manager servers are running.
         * Hence check for ssoToken and use local AuthContext
         */
        if (ssoToken == null) {
            ldapLoginInternal(bindUser, bindPwd);
        }
    }

    void ldapLoginInternal(String bindUser, String bindPwd)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("statusmsg6"));
        }

        try {
            com.sun.identity.authentication.internal.AuthContext ac =
                getLDAPAuthContext(bindUser, bindPwd);
            if (ac.getLoginStatus() ==
            com.sun.identity.authentication.internal.AuthContext.AUTH_SUCCESS)
            {
                if (AdminUtils.logEnabled()) {
                    AdminUtils.log(bundle.getString("statusmsg7"));
                    AdminUtils.log(bundle.getString("statusmsg8"));
                }
                ssoToken = ac.getSSOToken();
                AdminUtils.setSSOToken(ssoToken);
            } else {
                if (AdminUtils.logEnabled()) {
                    AdminUtils.log(bundle.getString("statusmsg9"));
                }
                throw new AdminException(bundle.getString("ldapauthfail"));
            }
        } catch (LoginException le) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("ldapauthfail"), le);
            }
            throw new AdminException(bundle.getString("ldapauthfail"));
        } catch (InvalidAuthContextException iace) {
            if (AdminUtils.logEnabled()) {
                AdminUtils.log(bundle.getString("ldapauthfail"), iace);
            }
            throw new AdminException(bundle.getString("ldapauthfail"));
        }
    }

    private com.sun.identity.authentication.internal.AuthContext
        getLDAPAuthContext(String bindUser, String bindPwd)
        throws LoginException
    {
        com.sun.identity.authentication.internal.AuthPrincipal principal =
            new com.sun.identity.authentication.internal.AuthPrincipal(
                bindUser);
        com.sun.identity.authentication.internal.AuthContext authContext =
             new com.sun.identity.authentication.internal.AuthContext(
                principal, bindPwd.toCharArray());
        return authContext;
    }

    private AuthContext getAuthContext()
        throws AdminException
    {
        try {
            AuthContext lc = new AuthContext("/");
            // In order to support war deployment which uses
            // flat files as the backend, we cannot hardcode "LDAP"
            // as the authentication module. Hence we have introduced
            // a new system property: "com.sun.identity.amadmin.authModule"
            // which defines the module to be used. If the property is null
            // we would fallback to "LDAP" and else use the defined
            // authentication module.
            String authModule = SystemProperties.get(
                "com.sun.identity.amadmin.authModule");
            if (authModule == null) {
                lc.login(AuthContext.IndexType.MODULE_INSTANCE, "LDAP");
            } else if (authModule.length() == 0) {
                lc.login();
            } else {
                lc.login(AuthContext.IndexType.MODULE_INSTANCE, authModule);
            }
            return lc;
        } catch (LoginException le) {
            le.printStackTrace();
            throw new AdminException(bundle.getString("loginFailed"));
        }
    }
    
    private void processCallback(
        AuthContext lc,
        String bindUser,
        String bindPwd
    ) throws AdminException
    {
        while (lc.hasMoreRequirements()) {
            Callback[] callbacks =  lc.getRequirements();
            
            if (callbacks != null) {
                setCallbackValues(callbacks, bindUser, bindPwd);
                lc.submitRequirements(callbacks);
            }
        }
        
        if (lc.getStatus() != AuthContext.Status.SUCCESS) {
            throw new AdminException(bundle.getString("loginFailed"));
        }
    }
    
    private void setCallbackValues(Callback[] callbacks, String bindUser,
        String bindPwd)
    {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback)callbacks[i];
                nc.setName(bindUser);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callbacks[i];
                char pswd[] = bindPwd.toCharArray();
                pc.setPassword(pswd);
            }
        }
    }

    private void logLoginFailure(String bindUser) {
        SSOToken adminToken = getAdminSSOToken();

        if (adminToken != null) {
            String[] args = {bindUser};
//            String message = bundle.getString("loginFail") + " " +  bindUser;
//            AdminUtils.logOperation(
//                AdminUtils.LOG_ERROR, message, adminToken);

            AdminUtils.logOperation(AdminUtils.LOG_ERROR,
                Level.INFO, AdminUtils.LOGIN_FAIL, args);
        }
    }

    private SSOToken getAdminSSOToken() {
        SSOToken adminToken = null;

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            String adminDN = (String)AccessController.doPrivileged(
                new AdminDNAction());
            String adminPassword = (String)AccessController.doPrivileged(
                new AdminPasswordAction());
            adminToken = mgr.createSSOToken(
                new AuthPrincipal(adminDN), adminPassword);
        } catch (SSOException ssoe) {
            AdminUtils.log(ssoe.getMessage());
        }

        return adminToken;
    }

    private boolean isPrincipalActive()
        throws AdminException
    {
        boolean isActive = false;

        try {
            // Check if DAI and Console service has been installed
            // The constructor will throw SMSException if
            // DAI or Console service is not installed
            new ServiceSchemaManager("iPlanetAMAdminConsoleService", ssoToken);
            new ServiceSchemaManager("DAI", ssoToken);

            AMStoreConnection conn = new AMStoreConnection(ssoToken);
            Map attrsMap = null;
            String principal = ssoToken.getProperty("Principal");
            int profileType = 0;

            try {
                profileType = conn.getAMObjectType(principal);
            } catch (AMException ame) {
                /*
                 * cannot get object type if administration service is not
                 * imported. This happens during installation.
                 */
                isActive = true;
            }

            if (!isActive) {
                switch (profileType) {
                case AMObject.USER:
                    AMUser user = conn.getUser(principal);
                    isActive = user.isActivated();

                    if (isActive) {
                        attrsMap = user.getAttributes(ACTIVE_STATE_ATTRIBUTES);

                        String strLoginStatus = getStringValue(
                            attrsMap, LOGIN_STATUS);
                        isActive = (strLoginStatus == null) ||
                            strLoginStatus.equalsIgnoreCase(STRING_ACTIVE);

                        if (isActive) {
                            isActive = !isExpired(getStringValue(
                                attrsMap, ACCOUNT_LIFE));
                        }
                    }
                    break;
                default:
                    AMEntity entity = conn.getEntity(principal);
                    isActive = entity.isActivated();
                    break;
                }
            }
        } catch (AMException e) {
            throw new AdminException(bundle.getString("ldapauthfail"));
        } catch (SSOException e) {
            throw new AdminException(bundle.getString("ldapauthfail"));
        } catch (SMSException smse) {
            /*
             * cannot get ServiceSchemaManager if DAI service is not
             * imported. This happens during installation.
             */
            isActive = true;
        }

        return isActive;
    }

    private static String getStringValue(Map map, String key) {
        String strValue = null;

        if ((map != null) && !map.isEmpty()) {
            Set set = (Set)map.get(key);

            if ((set != null) && !set.isEmpty()) {
                strValue = (String)set.iterator().next();
            }
        }

        return strValue;
    }

    private static boolean isExpired(String strDate) {
        boolean expired = false;

        if ((strDate != null) && (strDate.trim().length() > 0)) {
            Date exprDate = Locale.parseNormalizedDateString(strDate);
            expired = exprDate.before(new Date());
        }

        return expired;
    }
}
