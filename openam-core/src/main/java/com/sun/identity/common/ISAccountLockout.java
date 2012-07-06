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
 * $Id: ISAccountLockout.java,v 1.15 2009/03/07 08:01:50 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common;

import com.iplanet.am.util.AMSendMail;
import javax.mail.MessagingException;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMAuthCallBackImpl;
import com.sun.identity.authentication.spi.AMAuthCallBackException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.debug.IDebug;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

public class ISAccountLockout {
    private static final String USER_STATUS_ATTR="inetuserstatus";
    private static final String USER_ACTIVE ="Active";
    private static final String USER_INACTIVE ="Inactive";
    private static final String SPACE_DELIM = " ";
    private static final String PIPE_DELIM = "|";
    private static final String FROM_ADDRESS = "lockOutEmailFrom";
    private static final String EMAIL_SUBJECT = "lockOutEmailSub";
    private static final String EMAIL_MESSAGE = "lockOutEmailMsg";
    private static final String INVALID_ATTEMPTS_XML_OBJECT_CLASS =
        "sunAMAuthAccountLockout";
    private static final String DEFAULT_INVALID_ATTEMPTS_XML_ATTR =
        "sunAMAuthInvalidAttemptsData";
    
    // XML related variables
    private static final String BEGIN_XML="<InvalidPassword>";
    private static final String INVALID_PASS_COUNT_BEGIN ="<InvalidCount>";
    private static final String INVALID_PASS_COUNT_END ="</InvalidCount>";
    private static final String LAST_FAILED_BEGIN ="<LastInvalidAt>";
    private static final String LAST_FAILED_END ="</LastInvalidAt>";
    private static final String LOCKEDOUT_AT_BEGIN ="<LockedoutAt>";
    private static final String LOCKEDOUT_AT_END ="</LockedoutAt>";
    private static final String ACTUAL_LOCKOUT_DURATION_BEGIN =
        "<ActualLockoutDuration>";
    private static final String ACTUAL_LOCKOUT_DURATION_END =
        "</ActualLockoutDuration>";
    private static final String END_XML="</InvalidPassword>";
    
    private boolean failureLockoutMode = false;
    private boolean memoryLocking = false;
    private boolean storeInvalidAttemptsInDS = true;
    private long failureLockoutTime = 300;
    private int failureLockoutCount = 5;
    private String lockoutNotification = null;
    private int lockoutUserWarning = 3;
    private int userWarningCount = 0;
    private long failureLockoutDuration = 0;
    private int failureLockoutMultiplier = 1;
    private String lockoutAttrValue=null;
    private String lockoutAttrName=null;
    private String bundleName = null;
    private String invalidAttemptsDataAttrName =
        DEFAULT_INVALID_ATTEMPTS_XML_ATTR;
    private boolean needToSetInvalidAttemptsObjectClass = true;
    static Debug debug = Debug.getInstance("amAccountLockout");
    private AMAuthCallBackImpl callbackImpl = null;
    static Map loginFailHash = Collections.synchronizedMap(new HashMap());
    
    
    /**
     * Using this constructor the caller passes the account lockout
     * attribute values for the service and the resource bundle name
     * from with the localized account locking messages will be picked
     * up.
     *
     * @param failureLockoutMode a boolean indicating whether account locking
     *        is enabled or not.
     * @param failureLockoutTime a long which is the interval in minutes
     *        between 2 failed attempts.
     * @param failureLockoutCount an integer indicating the number of allowed
     *        failed attempts before account will be locked.
     * @param lockoutNotification a String , email address to notify when
     *        account is locked.
     * @param lockoutUserWarning an integer , the number of failed counts after
     *        which user will be warned about the remaining failed attempts
     *        before account will be locked.
     * @param lockoutAttrName a String , name of attribute to be used for
     *        account locking.
     * @param lockoutAttrValue a String , value of lockoutAttrName to be used
     *        for account locking.
     * @param lockoutFailureDuration a long, lockout duration in minutes
     *        used for memory locking.
     * @param invalidAttemptsDataAttrName a String , name of attribute for
     *        storing invalid attempts data.
     * @param bundleName a String, name of the resource bundle.
     */
    public ISAccountLockout(boolean failureLockoutMode,
        long failureLockoutTime,
        int failureLockoutCount,
        String lockoutNotification,
        int lockoutUserWarning,
        String lockoutAttrName,
        String lockoutAttrValue,
        long lockoutFailureDuration,
        int lockoutFailureMultiplier,
        String invalidAttemptsDataAttrName,
        String bundleName
    ) {
        this.failureLockoutMode = failureLockoutMode;
        this.failureLockoutTime = failureLockoutTime;
        this.failureLockoutCount = failureLockoutCount;
        this.lockoutNotification = lockoutNotification;
        this.lockoutUserWarning = lockoutUserWarning;
        this.lockoutAttrName = lockoutAttrName;
        this.lockoutAttrValue = lockoutAttrValue;
        this.failureLockoutDuration = lockoutFailureDuration;
        this.failureLockoutMultiplier = lockoutFailureMultiplier;
        
        if (lockoutFailureDuration > 0) {
            memoryLocking = true;
        }

        if ((invalidAttemptsDataAttrName == null) ||
            (invalidAttemptsDataAttrName.length() == 0)) {
            this.invalidAttemptsDataAttrName =
                DEFAULT_INVALID_ATTEMPTS_XML_ATTR;
            needToSetInvalidAttemptsObjectClass = true;
        } else {
            this.invalidAttemptsDataAttrName = invalidAttemptsDataAttrName;
            needToSetInvalidAttemptsObjectClass = false;
        }
        this.bundleName = bundleName;
    }
    
    /**
     * Returns true if account lockout mode is enabled otherwise false.
     *
     * @return true if lockout is enabled else false
     */
    public boolean isLockoutEnabled() {
        return (failureLockoutMode && failureLockoutCount > 0
            && failureLockoutTime > 0);
    }
    
    public boolean getStoreInvalidAttemptsInDS() {
        return storeInvalidAttemptsInDS;
    }
    
    public void setStoreInvalidAttemptsInDS(boolean aStoreInvalidAttemptsInDS) {
        storeInvalidAttemptsInDS = aStoreInvalidAttemptsInDS;
    }
    
    /**
     * Returns <code>true</code> if memory locking mode is enabled.
     * Memory locking mode is enabled when the failureLockoutDuration
     * is greater then zero.
     *
     * @return <code>true</code> if memory locking is enabled.
     */
    public boolean isMemoryLocking() {
        return memoryLocking;
    }
    /**
     * Determines the number of times user failed authentication
     * updates the accountInfo object with the user information and count of
     * failed authentication attempts.
     *
     * @param  userDN DN of the user
     * @param  userName name of the user
     * @param  amIdentity AMidentity object
     * @param  acInfo AccountLockoutInfo
     * @return updated user lockout information
     */
    public int invalidPasswd(String userDN, String userName,
        AMIdentity amIdentity, AccountLockoutInfo acInfo) {
        if (acInfo == null) {
            acInfo = new AccountLockoutInfo();
            acInfo.setActualLockoutDuration(failureLockoutDuration);
            loginFailHash.put(userDN,acInfo);
        }
        
        if (debug.messageEnabled()) {
            debug.message(
                "ISAccountLockout.invalidPasswd with userDN, AMIdentity");
            debug.message("userDN : " + userDN);
        }
        
        long now = System.currentTimeMillis();
        int fail_count = acInfo.getFailCount();
        long lastFailTime = acInfo.getLastFailTime();
        long lockedAt = acInfo.getLockoutAt();
        
        if((lastFailTime + failureLockoutTime) > now) {
            fail_count = fail_count +1;
        } else {
            fail_count = 1;
        }        
        
        if (((lastFailTime + failureLockoutTime) > now) &&
            (fail_count == failureLockoutCount)
        ) {
            lockedAt = now;
        }
        if (debug.messageEnabled()) {
            debug.message("ISAccountLockout.invalidPasswd:fail_count:"
                 +fail_count);
        }
       
        if (storeInvalidAttemptsInDS) {
            Map attrMap = new HashMap();
            Set invalidAttempts = new HashSet();
            String invalidXML = createInvalidAttemptsXML(
                fail_count,now,lockedAt, acInfo.getActualLockoutDuration());
            invalidAttempts.add(invalidXML);
            
            if (debug.messageEnabled()) {
                debug.message("ISAccountLockout.invalidPasswd: " + 
                    "Invalid Attempt XML being inserted= " + invalidXML);
            }
            
            attrMap.put(invalidAttemptsDataAttrName, invalidAttempts);
            
            try {
                setLockoutObjectClass(amIdentity);
                amIdentity.setAttributes(attrMap);
                amIdentity.store();
                debug.message(
                "ISAccountLockout.invalidPasswd: Stored Invalid Attempt XML");
            } catch (Exception e) {
                debug.error("ISAccountLockout.invalidPasswd", e);
                return -1;
            }
        }
        
        acInfo.setLastFailTime(now);
        acInfo.setFailCount(fail_count);
        acInfo.setLockoutAt(lockedAt);
        if (lockedAt > 0) {
            acInfo.setLockout(true);
        }
        acInfo.setUserToken(userName);
        
        if (fail_count == failureLockoutCount) {
            if (!memoryLocking) {
                inactivateUserAccount(amIdentity);
            }
            try {
                sendLockOutNotice(userName);
                /*
                 * The callback implementation instance is retrieved for
                 * the user's organization. This will be used to notify the
                 * custom plug-ins that a certain event occured on an account.
                 */
                callbackImpl = AMAuthCallBackImpl.getInstance(
                    amIdentity.getRealm());
                // Requesting callback to plugin for account lockout event.
                callbackImpl.processedAccounttLockout(new Long(now), userName);
            } catch (AMAuthCallBackException e) {
                if (debug.getState() >= IDebug.ERROR) {
                    debug.error("ISAccountLockout invalidPasswd : " +
                        "error getting callback implementation " +
                        "instance or error from callback module", e);
                }
            } catch(Exception ex) {
                debug.message("Error activating account/sending"
                    + "notification ", ex);
            }
        }
        
        setWarningCount(fail_count,failureLockoutCount);
        return userWarningCount;
    }
    
    public AccountLockoutInfo getAcInfo(String userDN, AMIdentity amIdentity) {
        AccountLockoutInfo acInfo = null;
        if (storeInvalidAttemptsInDS) {
            acInfo =  new AccountLockoutInfo();
            Set attrValueSet = Collections.EMPTY_SET;
            
            try {
                attrValueSet = amIdentity.getAttribute(
                    invalidAttemptsDataAttrName);
            } catch (Exception e) {
                debug.error("ISAccoutLockout.getAcInfo", e);
                return null;
            }
            String xmlFromDS = null;
            if ((attrValueSet != null) && (!attrValueSet.isEmpty())) {
                Iterator i = attrValueSet.iterator();
                xmlFromDS = (String) i.next();
            }
            int invalid_attempts = 0;
            long last_failed = 0;
            long locked_out_at = 0;
            long actual_lockout_duration = failureLockoutDuration;
            
            if ((xmlFromDS != null) && (xmlFromDS.length() !=0) &&
                (xmlFromDS.indexOf(BEGIN_XML) != -1)
            ) {
                String invalid_attempts_str = getElement(xmlFromDS,
                    INVALID_PASS_COUNT_BEGIN,INVALID_PASS_COUNT_END);
                invalid_attempts = Integer.parseInt(invalid_attempts_str);
                String last_failed_str = getElement(xmlFromDS,
                    LAST_FAILED_BEGIN, LAST_FAILED_END);
                last_failed = Long.parseLong(last_failed_str);
                String locked_out_at_str = getElement(xmlFromDS,
                    LOCKEDOUT_AT_BEGIN, LOCKEDOUT_AT_END);
                locked_out_at = Long.parseLong(locked_out_at_str);
                String actualLockoutDuration = getElement(xmlFromDS,
                    ACTUAL_LOCKOUT_DURATION_BEGIN, ACTUAL_LOCKOUT_DURATION_END);
                if ((actualLockoutDuration != null) &&
                    (actualLockoutDuration.length() != 0)) {
                    actual_lockout_duration = Long.parseLong(
                        actualLockoutDuration);
                } else {
                    actual_lockout_duration = failureLockoutDuration;
                }
            }
            
            acInfo.setLastFailTime(last_failed);
            acInfo.setFailCount(invalid_attempts);
            acInfo.setLockoutAt(locked_out_at);
            acInfo.setActualLockoutDuration(actual_lockout_duration);
            if (locked_out_at > 0) {
                acInfo.setLockout(true);
            }
            
            setWarningCount(invalid_attempts,failureLockoutCount);
            acInfo.setWarningCount(userWarningCount);
        } else {
            acInfo = (AccountLockoutInfo) loginFailHash.get(userDN);
        }
        return acInfo;
    }
    
    /**
     * Sets Lockout Object Class Attribute in <code>AMIdentity</code> Object
     * if it's not already present.
     *
     * @param amIdentity the user object.
     */
    private void setLockoutObjectClass(AMIdentity amIdentity) {
        if (!needToSetInvalidAttemptsObjectClass) {
            return;
        }
        try {
            Set attrValueSetObjectClass =amIdentity.getAttribute("objectClass");
            
            if ((attrValueSetObjectClass != null) &&
                (!attrValueSetObjectClass.contains
                (INVALID_ATTEMPTS_XML_OBJECT_CLASS))
            ) {
                attrValueSetObjectClass.add(INVALID_ATTEMPTS_XML_OBJECT_CLASS);
                Map map = new HashMap(2);
                map.put("ObjectClass", attrValueSetObjectClass);
                amIdentity.setAttributes(map);
            }
        } catch (IdRepoException e) {
            debug.message("ISAccountLockout.setLockoutObjectClass", e);
        } catch (SSOException e) {
            debug.message("ISAccountLockout.setLockoutObjectClass", e);
        }
    }
    
    /**
     * Determines the number of times user failed authentication
     * updates the accountInfo object with the user information and count of
     * failed authentication attempts.
     *
     * @param amIdentity user object.
     * @param acInfo lockout information object.
     * @return updated user lockout information.
     */
    public AccountLockoutInfo invalidPasswd(
        AMIdentity amIdentity,
        AccountLockoutInfo acInfo
    ) {
        return invalidPasswdEx(amIdentity, acInfo);
    }
    
    private AccountLockoutInfo invalidPasswdEx(
        Object subject,
        AccountLockoutInfo acInfo
    ) {
        long now = System.currentTimeMillis();
        if (acInfo == null) {
            // first failure. store key
            debug.message("ISAccountLockout.invalidPasswdEx: First failure." );
            acInfo = new AccountLockoutInfo();
            acInfo.setActualLockoutDuration(failureLockoutDuration);
            acInfo.setLastFailTime(now);
        }
        
        int failCount = 0;
        
        if (acInfo != null) {
            long lastFailTime = acInfo.getLastFailTime();
            failCount = acInfo.getFailCount();
            long failureLockoutInterval = failureLockoutTime;
            
            if ( (lastFailTime + failureLockoutInterval) > now) {
                failCount++;
                acInfo.setFailCount(failCount);
                
                if (failCount >= failureLockoutCount) {
                    // lock out the user.
                    acInfo.setLockoutAt(now);
                    acInfo.setLockout(failureLockoutDuration > 0);
                    
                    if (subject != null) {
                        if (failCount == failureLockoutCount) {
                            String notifyUser = null;
                            if (!memoryLocking) {
                                inactivateUserAccount((AMIdentity)subject);
                            }
                            notifyUser = acInfo.getUserToken();
                            if (notifyUser == null) {
                                notifyUser = ((AMIdentity)subject).getUniversalId();
                            }
                            sendLockOutNotice(notifyUser);
                        }
                    }
                }
            } else {
                debug.message("restart time and count");
                acInfo.setFailCount(1);
                acInfo.setLastFailTime(now);
                failCount = 1;
            }
        }
        
        //  failureLockoutCount = number of failures before lockout
        //  lockoutUserWarning = number of failures before warning user
        //  of impending lockout
        setWarningCount(failCount, failureLockoutCount);
        acInfo.setWarningCount(userWarningCount);
        return acInfo;
    }
    
    /**
     * Sends (Email) the lockout notice to the email address
     * specified in the lockout notification attribute with
     * the userDN information of the user whose account is
     * locked.
     *
     * @param userDN Distinguished name of the user
     */
    public void sendLockOutNotice(String userDN)  {
        if (lockoutNotification != null) {
            AMSendMail sm = new AMSendMail();
            StringTokenizer emailTokens = new StringTokenizer(
                lockoutNotification, SPACE_DELIM);
            
            while(emailTokens.hasMoreTokens()) {
                StringTokenizer stz2 = new StringTokenizer(
                    emailTokens.nextToken(), PIPE_DELIM);
                String[] toAddress = { stz2.nextToken() } ;
                String locale = null;
                String charset = null;
                if (stz2.hasMoreTokens()) {
                    locale = stz2.nextToken();
                    if (stz2.hasMoreTokens()) {
                        charset = stz2.nextToken();
                    }
                }
                
                ResourceBundle rb =
                    com.sun.identity.shared.locale.Locale.getResourceBundle(
                        bundleName, locale);
                String fromAddress = rb.getString(FROM_ADDRESS);
                String emailSubject = rb.getString(EMAIL_SUBJECT);
                String obj[] = { userDN };
                String emailMsg = MessageFormat.format(
                    rb.getString(EMAIL_MESSAGE), (Object[])obj);
                
                if ( debug.messageEnabled()) {
                    debug.message("ISAccountLockout.sendLockOutNotice:" +
                        " lockoutNotification = " + lockoutNotification + 
                        " toAddress = " +toAddress);
                }
                
                try {
                    sm.postMail(toAddress, emailSubject, emailMsg,
                        fromAddress, charset);
                } catch (MessagingException ex) {
                    debug.error("cannot email lockout notification:token ", ex);
                }
            }
        }
    }
    
    /**
     * Returns <code>true</code> if account is locked. Checks if user's account 
     * is locked based on the user's lockout info.
     * This method is for memory locking. If this method returns false
     * then the account is unlocked ie. the memory lock duration has
     * elapsed. Callers of this method must update their account lock
     * hash if the account is unlocked.
     *
     * @param acInfo Lockout info for the user.
     * @return <code>true</code> if account is locked.
     */
    public boolean isLockedOut(AccountLockoutInfo acInfo) {
        // has this user been locked out.
        boolean isLockedOut = acInfo.isLockout();
        
        if (debug.messageEnabled()) {
            debug.message("ISAccoutLockout.isLockedOut : " + isLockedOut);
        }
        
        if ((acInfo != null) && isLockedOut) {
            // get the time of locked out.
            // add loginFailureLockoutDuration
            // if less than now then still locked out else
            // reset lock out
            long now = System.currentTimeMillis();
            long lockOutTime = acInfo.getLockoutAt();
            
            if ((lockOutTime + acInfo.getActualLockoutDuration()) < now) {
                // exceeded lockout time. unlock and return false..
                if (debug.messageEnabled()) {
                    debug.message("isLockedOut returns false. " +
                        "loginFailureLockoutDuration=" +
                        acInfo.getActualLockoutDuration() +
                        " lockOutTime=" + lockOutTime +
                        " now=" + now );
                }
                // check if the account has been physically
                // unlocked by the admin.
                isLockedOut=false;
            }
        }
        return isLockedOut;
    }
    
    /**
     * Inactivates user account
     *
     * @param AMIdentity the user object
     */
    
    private void inactivateUserAccount(AMIdentity amIdentity) {
        debug.message("entering ISAccountLockout.inactivateUserAccount");
        try {
            Map attrMap = new HashMap();
            Set attrValSet1 = new HashSet();
            if ((lockoutAttrName != null) && (lockoutAttrValue != null )) {
                attrValSet1.add(lockoutAttrValue);
                attrMap.put(lockoutAttrName, attrValSet1);
            }
            
            Set attrValSet2 = new HashSet();
            attrValSet2.add(USER_INACTIVE);
            attrMap.put(USER_STATUS_ATTR, attrValSet2);
            
            amIdentity.setAttributes(attrMap);
            amIdentity.store();
        } catch (Exception e) {
            debug.error("ISAccountLockout.inactivateUserAccount", e);
        }
    }
    
    /**
     * Sets the warning count for the user. A warning count of -1
     * indicates that the account is locked.
     *
     * @param failCount current fail count on password reset
     * @param failureLockoutCount count at which user is locked out
     */
    
    private void setWarningCount(int failCount,int failureLockoutCount) {
        userWarningCount = 0;
        if (failCount >= failureLockoutCount) {
            // user is locked out
            userWarningCount = -1;
        } else {
            if (lockoutUserWarning > 0 &&
                lockoutUserWarning < failureLockoutCount) {
                int upperLimit = lockoutUserWarning;
                if (failCount >= upperLimit) {
                    // num of tries left
                    userWarningCount = failureLockoutCount - failCount;
                }
            }
        }
    }
    
    /**
     * Checks if user account has been unlocked.This method is only
     * for accounts which are physically locked.
     *
     * @param amIdentity  AMIdentity object
     * @return boolean <code>true</code> if account is locked else <code>false</code>
     */
    
    public boolean isAccountLocked(AMIdentity amIdentity) {
        boolean isLocked=false;
        if (!memoryLocking) {
            try {
                if (lockoutAttrName != null && lockoutAttrValue != null ) {
                    Set attrValueSet = amIdentity.getAttribute(lockoutAttrName);
                    if ((attrValueSet != null) && (!attrValueSet.isEmpty())) {
                        Iterator i = attrValueSet.iterator();
                        String attrValue = (String)i.next();
                        isLocked = attrValue.equals(lockoutAttrValue);
                    }
                }
                if (!isLocked) {
                    Set attrValueSet =amIdentity.getAttribute(USER_STATUS_ATTR);
                    if ((attrValueSet != null) && (!attrValueSet.isEmpty())) {
                        Iterator i = attrValueSet.iterator();
                        String attrValue =  (String)i.next();
                        isLocked = attrValue.equals(USER_INACTIVE);
                    }
                }
            } catch (Exception e) {
                debug.error("Error inactivating user account",e);
            }
        }
        if (debug.messageEnabled()) {
            if (isLocked) {
                debug.message("Account is locked");
            } else {
                debug.message("Account is unlocked");
            }
        }
        return isLocked;
    }

    /**
     * Resets attempts related data in Data store if the user has successfully
     * authenticated.
     *
     * @param userDN Distinguished name of user.
     * @param amIdentity <code>AMidentity</code> object.
     * @param acInfo Account Lockout Information.
     */
    public void resetLockoutAttempts(
        String userDN,
        AMIdentity amIdentity,
        AccountLockoutInfo acInfo,
        boolean resetDuration
    ) {
        debug.message("entering ISAccountLockout.resetLockoutAttempts");
        if (debug.messageEnabled()) {
            debug.message("ISAccountLockout.resetLockoutAttempts:: "+
                "resetDuration ="+resetDuration);
        }
        int fail_count = 0;
        long lastFailTime = 0;
        long locked_out_at = 0;
        long actualLockoutDuration = failureLockoutDuration;
        long currentLockoutDuration = acInfo.getActualLockoutDuration();
        if (acInfo != null) {
            fail_count = acInfo.getFailCount();
            lastFailTime = acInfo.getLastFailTime();
            locked_out_at = acInfo.getLockoutAt();
            long now = System.currentTimeMillis();
            if (!resetDuration) { 
                actualLockoutDuration = currentLockoutDuration;
                if (debug.messageEnabled()) {
                    debug.message("ISAccountLockout.resetLockoutAttempts::"
                        +"Locked out At ="+locked_out_at);
                }
                if (locked_out_at > 0) {
                if (debug.messageEnabled()) {
                    debug.message("ISAccountLockout.resetLockoutAttempts::"
                        +" Using the multiplier");
                }
                    actualLockoutDuration = failureLockoutMultiplier*
                        (currentLockoutDuration);
                }
            }
        }

        if (storeInvalidAttemptsInDS) {
            try {
                if ((fail_count !=0)||(lastFailTime !=0)||(locked_out_at !=0)||
                    (actualLockoutDuration != currentLockoutDuration)) {
                    Map attrMap = new HashMap();
                    Set invalidAttempts = new HashSet();
                    String invalidXML = createInvalidAttemptsXML(0,0,0,
                        actualLockoutDuration);
                    invalidAttempts.add(invalidXML);
                    attrMap.put(invalidAttemptsDataAttrName, invalidAttempts);
                    setLockoutObjectClass(amIdentity);
                    amIdentity.setAttributes(attrMap);
                    debug.message("Saving XML = "+invalidXML);
                    amIdentity.store();
                }
                debug.message("ISAccountLockout::resetLockoutAttempts done");
            } catch (Exception exp) {
                debug.message("error reseting Lockout Attempts");
            }
        } 
        //loginFailHash.remove(userDN);
        acInfo.setFailCount(0);
        acInfo.setLastFailTime(0);
        acInfo.setLockoutAt(0);
        acInfo.setLockout(false);
        acInfo.setActualLockoutDuration(actualLockoutDuration);
        
    }
    
    /**
     * Returns XML to be stored in data store the format is like this
     * &lt;InvalidPassword>
     *    &lt;InvalidCount>failureLockoutCount&lt;/LockoutCount>
     *    &lt;LastInvalidAt>failureLockoutDuration&lt;/LockoutDuration>
     *    &lt;LockedoutAt>failureLockoutTime&lt;/LockoutTime>
     *  &lt;/InvalidPassword>
     *
     */
    private static String createInvalidAttemptsXML(
        int invalidCount, long lastFailed, long lockedOutAt, 
        long actualLockoutDuration) {
        StringBuilder xmlBuffer = new StringBuilder(150);
        xmlBuffer.append(BEGIN_XML).append(INVALID_PASS_COUNT_BEGIN)
            .append(String.valueOf(invalidCount)).append(INVALID_PASS_COUNT_END)
            .append(LAST_FAILED_BEGIN).append(String.valueOf(lastFailed))
            .append(LAST_FAILED_END).append(LOCKEDOUT_AT_BEGIN)
            .append(String.valueOf(lockedOutAt)).append(LOCKEDOUT_AT_END)
            .append(ACTUAL_LOCKOUT_DURATION_BEGIN)
            .append(String.valueOf(actualLockoutDuration))
            .append(ACTUAL_LOCKOUT_DURATION_END)
            .append(END_XML);
        return xmlBuffer.toString();
    }

    private static String getElement(
        String content, 
        String start,
        String end
    ) {
        String answer = null;
        if (content != null) {
            int startIndex = content.indexOf(start);
            int endIndex = content.indexOf(end);
            if (startIndex != -1 && endIndex != -1 &&
                startIndex + start.length() < endIndex ) {
                answer = content.substring(startIndex + start.length(),
                    endIndex);
            }
        }
        return (answer);
    }
    
}
