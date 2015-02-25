/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMAccountLockout.java,v 1.10 2009/03/06 22:09:20 hengming Exp $
 *
 * Portions Copyrighted 2013-2014 ForgeRock AS.
 */
package com.sun.identity.authentication.service;

import com.sun.identity.common.AccountLockoutInfo;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.ISAccountLockout;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.locale.Locale;
import java.util.Date;
import java.util.Set;
import java.util.Map;

import static org.forgerock.openam.utils.CollectionUtils.*;

/**
 * <code>AMAccountLockout</code> contains the utility methods to retrieve and set account lockout related information
 * for the users and also facilitates the enforcement of the user account lockout.
 */
class AMAccountLockout {

    private static final String USER_ACTIVE = "active";
    private static final String USER_INACTIVE = "inactive";
    private static final String FALSE_VALUE = "false";
    private static final String LOGIN_STATUS_ATTR = "iplanet-am-user-login-status";
    private static final String NSACCOUNTLOCK_ATTR = "nsaccountlock";
    private static final String BUNDLE_NAME = "amAuth";
    private static final Debug DEBUG = Debug.getInstance(BUNDLE_NAME);
    private boolean loginFailureLockoutMode = false;
    private boolean loginFailureLockoutStoreInDS = true;
    private int loginFailureLockoutCount = 5;
    private int loginLockoutUserWarning = 3;
    private int loginFailureLockoutMultiplier = 0;
    private int warnUser = -1;
    private long loginFailureLockoutTime = 300;
    private long loginFailureLockoutDuration = 0;
    private String loginLockoutAttrValue = null;
    private String loginLockoutAttrName = null;
    private String loginLockoutNotification = null;
    private final LoginState loginState;
    private final ISAccountLockout isAccountLockout;
    private AccountLockoutInfo acInfo = null;

    /**
     * Creates <code>AMAccountLockout</code> by retrieving account locking specific attribute values from
     * <code>LoginState</code>.
     *
     * @param loginState Login State object.
     */
    public AMAccountLockout(LoginState loginState) {
        this.loginState = loginState;
        loginFailureLockoutTime = loginState.getLoginFailureLockoutTime();
        loginFailureLockoutCount = loginState.getLoginFailureLockoutCount();
        loginLockoutNotification = loginState.getLoginLockoutNotification();
        loginLockoutUserWarning = loginState.getLoginLockoutUserWarning();
        loginLockoutAttrName = loginState.getLoginLockoutAttrName();
        loginLockoutAttrValue = loginState.getLoginLockoutAttrValue();
        loginFailureLockoutDuration = loginState.getLoginFailureLockoutDuration();
        loginFailureLockoutMultiplier = loginState.getLoginFailureLockoutMultiplier();
        loginFailureLockoutMode = loginState.getLoginFailureLockoutMode();
        loginFailureLockoutStoreInDS = loginState.getLoginFailureLockoutStoreInDS();

        String invalidAttemptsDataAttrName = loginState.getInvalidAttemptsDataAttrName();
        isAccountLockout = new ISAccountLockout(loginFailureLockoutMode, loginFailureLockoutTime,
                loginFailureLockoutCount, loginLockoutNotification, loginLockoutUserWarning, loginLockoutAttrName,
                loginLockoutAttrValue, loginFailureLockoutDuration, loginFailureLockoutMultiplier,
                invalidAttemptsDataAttrName, BUNDLE_NAME);

        isAccountLockout.setStoreInvalidAttemptsInDS(loginFailureLockoutStoreInDS);
    }

    /**
     * Checks the number of times user failed authentication update the account hash with the user information and count
     * of failed authentications.
     *
     * @param username User name.
     */
    public void invalidPasswd(String username) {
        try {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("AMAccountLockout::invalidPasswd: " + username);
            }
            if (!isAccountLockout.isLockoutEnabled()) {
                DEBUG.message("Failure lockout mode disabled");
            } else {
                String userDN;
                AMIdentity amIdentity = null;
                if (isAccountLockout.getStoreInvalidAttemptsInDS() || !isAccountLockout.isMemoryLocking()) {
                    amIdentity = AuthD.getAuth().getIdentity(IdType.USER, username, loginState.getOrgDN());
                    userDN = normalizeDN(IdUtils.getDN(amIdentity));
                } else {
                    userDN = normalizeDN(username);
                }
                if (acInfo == null) {
                    acInfo = isAccountLockout.getAcInfo(userDN, amIdentity);
                }
                warnUser = isAccountLockout.invalidPasswd(userDN, username, amIdentity, acInfo);
            }
        } catch (Exception ex) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("invalidPasswd:Error : ", ex);
            }
        }
    }

    /**
     * Checks if user account is expired.
     *
     * @return <code>true</code> if account has expired.
     */
    public boolean isAccountExpired() {
        DEBUG.message("in AMAccountLockout::isAccountExpired");
        String accountLife = loginState.getAccountLife();
        if (accountLife == null) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("EXIT accountLife is null :" + accountLife);
            }
            return false;
        }
        Date exprDate = Locale.parseNormalizedDateString(accountLife);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("exprDate = " + exprDate);
        }
        if (exprDate != null) {
            return exprDate.before(new Date());
        }
        return false;
    }

    /**
     * Returns the warning count.
     *
     * @return the warning count.
     */
    public int getWarnUserCount() {
        return warnUser;
    }

    /**
     * Sends the lockout notice.
     *
     * @param userDN The distinguished name of the user.
     */
    public void sendLockOutNotice(String userDN) {
        isAccountLockout.sendLockOutNotice(userDN);
    }

    /**
     * Resets the account if passed authentication after a failure.
     *
     * @param token User name.
     * @param resetDuration boolean
     */
    public void resetPasswdLockout(String token, boolean resetDuration) {
        try {
            // remove the hash entry for login failure for tokenID
            String userDN = null;
            if (token != null) {
                AMIdentity amIdentity = null;
                if (isAccountLockout.getStoreInvalidAttemptsInDS()) {
                    amIdentity = AuthD.getAuth().getIdentity(IdType.USER, token, loginState.getOrgDN());
                    userDN = normalizeDN(IdUtils.getDN(amIdentity));
                } else {
                    userDN = normalizeDN(token);
                }

                if (acInfo == null) {
                    acInfo = isAccountLockout.getAcInfo(userDN, amIdentity);
                }
                isAccountLockout.resetLockoutAttempts(userDN, amIdentity, acInfo, resetDuration);
                warnUser = 0;
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("resetPasswordFailCount: token=" + token + "  userDN=" + userDN);
            }
        } catch (Exception ex) {
            DEBUG.message("Exception in resetPasswordLockout", ex);
        }
    }

    /**
     * Checks if the account lockout is enabled.
     *
     * @return <code>true</code> if enabled.
     */
    public boolean isLockoutEnabled() {
        return isAccountLockout.isLockoutEnabled();
    }

    /**
     * Checks if the account is locked out and needs to be unlocked. this is for memory locking. If duration has passed
     * then the user is removed from the <code>loginFailHash</code> Map.
     *
     * @return <code>true</code> if account is locked.
     */
    public boolean isLockedOut() {
        // has this user been locked out.
        String userDN = loginState.getUserToken();
        return isLockedOut(userDN);
    }

    /**
     * Checks if the account is locked out and needs to be unlocked. this is for memory locking. If duration has passed
     * then the user is removed from the <code>loginFailHash</code> Map.
     *
     * @param userName is the user name.
     * @return <code>true</code> if account is locked.
     */
    public boolean isLockedOut(String userName) {
        // has this user been locked out.
        String normUserDN = normalizeDN(userName);
        boolean locked = false;
        try {
            if (isAccountLockout.isMemoryLocking()) {
                locked = isMemoryLockout(normUserDN);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("isLockedOut:Exception : ", e);
            }
        }
        return locked;
    }

    private boolean isMemoryLockout(String aUserName) {
        boolean locked = false;
        try {
            String userDN;
            AMIdentity amIdentity = null;
            if (isAccountLockout.getStoreInvalidAttemptsInDS()) {
                amIdentity = AuthD.getAuth().getIdentity(IdType.USER, aUserName, loginState.getOrgDN());
                userDN = normalizeDN(IdUtils.getDN(amIdentity));
            } else {
                userDN = aUserName;
            }

            if (acInfo == null) {
                acInfo = isAccountLockout.getAcInfo(userDN, amIdentity);
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("isLockedOut:userDN=" + userDN);
                DEBUG.message("isLockedOut:acInfo=" + acInfo);
            }
            if (acInfo != null) {
                locked = isAccountLockout.isLockedOut(acInfo);
                if (!locked && acInfo.isLockout()) {
                    resetPasswdLockout(aUserName, false);
                }
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("isLockedOut :" + locked);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("isLockedOut:Exception : ", e);
            }
        }
        return locked;
    }

    /**
     * Checks if the account is locked out for a user.
     *
     * @param aUserName the user name.
     * @return <code>true</code> if account is locked.
     */
    public boolean isAccountLocked(String aUserName) {
        // has this user been locked out.
        boolean locked = false;
        try {
            AMIdentity amIdentity = AuthD.getAuth().getIdentity(IdType.USER, aUserName, loginState.getOrgDN());
            String userDN = normalizeDN(aUserName);
            if (isAccountLockout.getStoreInvalidAttemptsInDS()) {
                userDN = normalizeDN(IdUtils.getDN(amIdentity));
            }
            if (acInfo == null) {
                acInfo = isAccountLockout.getAcInfo(userDN, amIdentity);
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("userDN=" + userDN);
                DEBUG.message("acInfo=" + acInfo);
            }
            if (isAccountLockout.isMemoryLocking() && acInfo != null) {
                locked = acInfo.isLockout();
            } else {
                if (isAccountValid(amIdentity)) {
                    locked = isAccountLockout.isAccountLocked(amIdentity);
                    if (locked) {
                        resetPasswdLockout(aUserName, false);
                    }
                } else {
                    locked = true;
                    resetPasswdLockout(aUserName, false);
                }
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("isLockedOut :" + locked);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("isAccountLocked:Exception : " + e.toString());
            }
        }
        return locked;
    }

    /**
     * Checks if the account is active.
     *
     * @param amIdentity AMIdentity object.
     * @return <code>true</code> if active.
     */
    boolean isAccountValid(AMIdentity amIdentity) {
        boolean userEnabled = true;
        try {
            String userActive = amIdentity.isActive() ? USER_ACTIVE : USER_INACTIVE;

            Map<String, Set<String>> attrs = amIdentity.getAttributes(asSet(LOGIN_STATUS_ATTR, NSACCOUNTLOCK_ATTR));

            // Check "login_status"
            String loginStatus = CollectionHelper.getMapAttr(attrs, LOGIN_STATUS_ATTR);
            if (loginStatus == null || loginStatus.isEmpty()) {
                loginStatus = USER_ACTIVE;
            }

            // Check "nsaccountlock"
            String nsAccountVal = CollectionHelper.getMapAttr(attrs, NSACCOUNTLOCK_ATTR);
            if (nsAccountVal == null || nsAccountVal.isEmpty()) {
                nsAccountVal = FALSE_VALUE;
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("inetuserstatus : " + userActive);
                DEBUG.message("loginStatus : " + loginStatus);
                DEBUG.message("nsAccountLockVal : " + nsAccountVal);
            }

            userEnabled = userActive.equalsIgnoreCase(USER_ACTIVE) && loginStatus.equalsIgnoreCase(USER_ACTIVE)
                    && nsAccountVal.equalsIgnoreCase(FALSE_VALUE);
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("isAccountValid:Error :" + e.toString());
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("User enabled..." + userEnabled);
        }
        return userEnabled;
    }

    private String normalizeDN(String userDN) {
        String normalizedDN = userDN;
        if (userDN != null && DN.isDN(userDN)) {
            normalizedDN = DNUtils.normalizeDN(userDN);
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Original DN is:" + userDN);
            DEBUG.message("Normalized DN is:" + normalizedDN);
        }
        return normalizedDN;
    }
}
