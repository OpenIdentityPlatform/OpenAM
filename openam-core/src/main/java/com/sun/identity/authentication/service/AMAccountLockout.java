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
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.sun.identity.authentication.service;

import com.sun.identity.common.AccountLockoutInfo;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.ISAccountLockout;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

/**
 * <code>AMAccountLockout</code> contains the utility methods to retrieve and
 * set account lockout related information for the users and also facilitates
 * the enforcement of the user account lockout
 */
class AMAccountLockout {
    LoginState loginState = null;
    static private AuthD ad = AuthD.getAuth();
    static private Debug debug = ad.debug;
    /**
     * Check login failure lockout mode
     */
    public boolean loginFailureLockoutMode = false;
    /**
     * Check login failure lockout mode
     */
    public boolean loginFailureLockoutStoreInDS = true;
    private long loginFailureLockoutTime = 300;
    private int loginFailureLockoutCount = 5;
    private String loginLockoutNotification = null;
    private int loginLockoutUserWarning = 3;
    private long loginFailureLockoutDuration = 0;
    private int loginFailureLockoutMultiplier = 0;
    /**
     * Value of login lockout attribute
     */
    public String loginLockoutAttrValue=null;
    /**
     * Name of login lockout attribute
     */
    public String loginLockoutAttrName=null;
    private ISAccountLockout isAccountLockout ;
    private int warnUser = -1;
    static String bundleName = AuthD.BUNDLE_NAME;
    String token = null;
    private static final String USER_ACTIVE = "active";
    private static final String USER_INACTIVE = "inactive";
    private static final String FALSE_VALUE = "false";
    private static final String INETUSERSTATUS_ATTR ="inetuserstatus";
    private static final String LOGIN_STATUS_ATTR =
        "iplanet-am-user-login-status";
    private static final String NSACCOUNTLOCK_ATTR = "nsaccountlock";
    private AccountLockoutInfo acInfo = null;
    
    /**
     * Creates <code>AMAccountLockout</code> by retrieving account 
     * locking specific attribute values from <code>LoginState</code>
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
        loginFailureLockoutDuration =
            loginState.getLoginFailureLockoutDuration();
        loginFailureLockoutMultiplier =
            loginState.getLoginFailureLockoutMultiplier();
        loginFailureLockoutMode = loginState.getLoginFailureLockoutMode();
        loginFailureLockoutStoreInDS = loginState.
        getLoginFailureLockoutStoreInDS();

        String invalidAttemptsDataAttrName =
            loginState.getInvalidAttemptsDataAttrName();
        isAccountLockout = new ISAccountLockout(loginFailureLockoutMode,
            loginFailureLockoutTime, loginFailureLockoutCount,
            loginLockoutNotification, loginLockoutUserWarning,
            loginLockoutAttrName, loginLockoutAttrValue,
            loginFailureLockoutDuration, loginFailureLockoutMultiplier,
            invalidAttemptsDataAttrName,
            bundleName);

        isAccountLockout.setStoreInvalidAttemptsInDS(
        loginFailureLockoutStoreInDS);
    }
    
    /**
     * Checks the number of times user failed authentication
     * update the account hash with the user information and count of
     * failed authentications.
     *
     * @param token User name.
     */
    public void invalidPasswd(String token) {
        try{
            if (debug.messageEnabled()) {
                debug.message("AMAccountLockout::invalidPasswd : " + token);
            }
            this.token= token;
            if (!isAccountLockout.isLockoutEnabled()) {
                debug.message("Failure lockout mode disabled");
            } else {
                String userDN = null;
                AMIdentity amIdentity = null;
                if ((isAccountLockout.getStoreInvalidAttemptsInDS()) || 
                   (!isAccountLockout.isMemoryLocking())
                ) {
                    amIdentity = AuthD.getAuth().getIdentity(
                        IdType.USER, token, loginState.getOrgDN());
                    userDN = normalizeDN(IdUtils.getDN(amIdentity));
                } else {
                    userDN = normalizeDN(token);
                }
                if (acInfo == null) {
                    acInfo = isAccountLockout.getAcInfo(userDN,amIdentity);
                }
                warnUser = isAccountLockout.invalidPasswd(
                    userDN, token, amIdentity, acInfo);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("invalidPasswd:Error : " + e.toString());
            }
        }
    }
    
    /**
     * Checks if user account is expired.
     *
     * @return <code>true</code> if account has expired.
     */
    public boolean isAccountExpired() {
        debug.message("in AMAccountLockout::isAccountExpired");
        String accountLife = loginState.getAccountLife();
        if (accountLife == null) {
            if (debug.messageEnabled()) {
                debug.message("EXIT accountLife is null :" + accountLife);
            }
            return false;
        }
        Date exprDate = com.sun.identity.shared.locale.Locale.
            parseNormalizedDateString(accountLife);
        if (debug.messageEnabled()) {
            debug.message("exprDate = "+exprDate);
        }
        if (exprDate != null) {
            return (exprDate.before(new Date()));
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
    public void sendLockOutNotice(String userDN)  {
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
                    amIdentity =
                    AuthD.getAuth().getIdentity(
                        IdType.USER, token, loginState.getOrgDN());
                    userDN = normalizeDN(IdUtils.getDN(amIdentity));
                } else {
                    userDN = normalizeDN(token);
                }
                  
                if (acInfo == null) {
                    acInfo = isAccountLockout.getAcInfo(userDN,amIdentity);
                }
                isAccountLockout.resetLockoutAttempts(userDN,amIdentity,acInfo,
                    resetDuration);
                warnUser = 0;
            }
        
            if ( debug.messageEnabled()) {
                debug.message("resetPasswordFailCount: token="
                + token + "  userDN=" + userDN);
            }
        } catch (Exception exp) {
            debug.message("Exception in resetPasswordLockout");        
        }
    }

    /**
     * Checks if the account lockout is enabled
     * @return <code>true</code> if enabled
     */
    public boolean isLockoutEnabled() {
        return isAccountLockout.isLockoutEnabled();
    }
    
    /**
     * Checks if the account is locked out and needs to be unlocked.
     * this is for memory locking. If duration has passed then
     * the user is removed from the <code>loginFailHash</code> Map.
     *
     * @return <code>true</code> if account is locked.
     */
    public boolean isLockedOut() {
        // has this user been locked out.
        String userDN = loginState.getUserToken();
        return isLockedOut(userDN);
    }
    
    /**
     * Checks if the account is locked out and needs to be unlocked.
     * this is for memory locking. If duration has passed then
     * the user is removed from the <code>loginFailHash</code> Map.
     *
     * @param userName is the user name
     * @return <code>true</code> if account is locked.
     */
    public boolean isLockedOut(String userName) {
        // has this user been locked out.
        String normUserDN = normalizeDN(userName);
        boolean locked=false;
        try {
            if (isAccountLockout.isMemoryLocking()) {
                locked = isMemoryLockout(normUserDN);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("isLockedOut:Exception : " , e);
            }
        }
        return locked;
    }
    
    private boolean isMemoryLockout(String aUserName) {
        boolean locked=false;
        try {
            String userDN = null;
            AMIdentity amIdentity = null;
            if (isAccountLockout.getStoreInvalidAttemptsInDS()) {
                amIdentity =
                AuthD.getAuth().getIdentity(
                    IdType.USER, aUserName, loginState.getOrgDN());
                userDN = normalizeDN(IdUtils.getDN(amIdentity));
            } else {
                userDN = aUserName;
            }
               
            if (acInfo == null) {
                acInfo = isAccountLockout.getAcInfo(userDN, amIdentity);
            }
                
                if (debug.messageEnabled()) {
                    debug.message("isLockedOut:userDN=" + userDN);
                    debug.message("isLockedOut:acInfo=" + acInfo);
                }
                if (acInfo != null) {
                    locked = isAccountLockout.isLockedOut(acInfo);
                    if ((!locked) && acInfo.isLockout()) {
                        resetPasswdLockout(aUserName, false);
                    }
                }
                
                if (debug.messageEnabled()) {
                    debug.message("isLockedOut :" + locked);
                }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("isLockedOut:Exception : " , e);
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
        boolean locked=false;
        try {
            AMIdentity amIdentity =
            AuthD.getAuth().getIdentity(
                IdType.USER, aUserName, loginState.getOrgDN());
            String userDN = normalizeDN(aUserName);
            if (isAccountLockout.getStoreInvalidAttemptsInDS()) {
                userDN = normalizeDN(IdUtils.getDN(amIdentity));
            } 
            if (acInfo == null) {
                acInfo = isAccountLockout.getAcInfo(userDN, amIdentity);
            }
            if (debug.messageEnabled()) {
                debug.message("userDN=" + userDN);
                debug.message("acInfo=" + acInfo);
            }
            if (isAccountLockout.isMemoryLocking() && (acInfo != null)) {
                locked = acInfo.isLockout();
            } else {
                if (isAccountValid(amIdentity)) {
                    locked = isAccountLockout.isAccountLocked(amIdentity) ;
                    if (locked) {
                        resetPasswdLockout(aUserName, false);
                    }
                } else  {
                    locked=true;
                    resetPasswdLockout(aUserName, false);
                }
            }
            
            if (debug.messageEnabled()) {
                debug.message("isLockedOut :" + locked);
            }
        } catch (Exception e) {
            if (ad.debug.messageEnabled()) {
                ad.debug.message("isAccountLocked:Exception : " + e.toString());
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
        boolean userEnabled=true;
        try{
            Iterator attrSet = null;
            String userActive = amIdentity.isActive()? USER_ACTIVE : USER_INACTIVE;
            
            // Check "login_status"
            Set loginStatusSet =
            (Set) amIdentity.getAttribute(LOGIN_STATUS_ATTR);
            String loginStatus = null;
            if ((loginStatusSet == null) || (loginStatusSet.isEmpty())) {
                loginStatus = USER_ACTIVE;
            } else {
                attrSet = loginStatusSet.iterator();
                loginStatus = (String) attrSet.next();
                if ((loginStatus == null) || (loginStatus.length() == 0)) {
                    loginStatus = USER_ACTIVE;
                }
            }
            
            // Check "nsaccountlock"
            Set nsAccountValSet = amIdentity.getAttribute(NSACCOUNTLOCK_ATTR);
            String nsAccountVal = null;
            if ((nsAccountValSet == null) || (nsAccountValSet.isEmpty())) {
                nsAccountVal = FALSE_VALUE;
            } else {
                attrSet = nsAccountValSet.iterator();
                nsAccountVal = (String) attrSet.next();
                if ((nsAccountVal == null) || (nsAccountVal.length() == 0)) {
                    nsAccountVal = FALSE_VALUE;
                }
            }
            
            if (debug.messageEnabled()) {
                debug.message("inetuserstatus : " + userActive);
                debug.message("loginStatus : " + loginStatus);
                debug.message("nsAccountLockVal : " + nsAccountVal);
            }
            
            userEnabled = (userActive.equalsIgnoreCase(USER_ACTIVE)  &&
            loginStatus.equalsIgnoreCase(USER_ACTIVE) &&
            nsAccountVal.equalsIgnoreCase(FALSE_VALUE));
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("isAccountValid:Error :" + e.toString());
            }
        }
        if (debug.messageEnabled()) {
            debug.message("User enabled..." + userEnabled);
        }
        return userEnabled;
    }
        
    /* returns the normalized DN  */
    private String normalizeDN(String userDN) {
        String normalizedDN = userDN;
        if ((userDN != null) && DN.isDN(userDN)) {
            normalizedDN = DNUtils.normalizeDN(userDN);
        }
        if (debug.messageEnabled()) {
            debug.message("Original DN is:" + userDN);
            debug.message("Normalized DN is:" + normalizedDN);
        }
        return normalizedDN;
    }
}
