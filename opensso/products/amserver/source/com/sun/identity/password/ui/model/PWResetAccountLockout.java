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
 * $Id: PWResetAccountLockout.java,v 1.4 2009/03/06 22:37:07 hengming Exp $
 *
 */
  
package com.sun.identity.password.ui.model;

import com.sun.identity.common.AccountLockoutInfo;
import com.sun.identity.common.ISAccountLockout;
import com.sun.identity.idm.AMIdentity;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>PWResetAccountLockout</code> defines a set for methods to lock and 
 * unlock the user's account if their attempt to reset password were 
 * unsuccessful after n of tries.
 */
public class PWResetAccountLockout {
    private static Map pwResetFailHash = 
        Collections.synchronizedMap(new HashMap());
    private int userWarningCount = 0;
    private PWResetModelImpl model = null;
    private ISAccountLockout isAccountLockout ;

    /**
     * Constructs a password reset account lockout object.
     *
     * @param model model use in this class
     */
    public PWResetAccountLockout(PWResetModelImpl model) {
        this.model = model;
        isAccountLockout = new ISAccountLockout(
            model.isPasswordResetFailureLockoutEnabled(),
            model.getPasswordResetFailureLockoutTime(),
            model.getPasswordResetFailureLockoutCount(),
            model.getPasswordResetLockoutNotification(),
            model.getPasswordResetLockoutUserWarningCount(),
            model.getPasswordResetLockoutAttributeName(),
            model.getPasswordResetLockoutAttributeValue(),
            model.getPasswordResetFailureLockoutDuration(),
            1,
            null,
            PWResetModel.DEFAULT_RB);
    }

    /**
     * Stores the attempt of the user's password reset failure in the map. It
     * uses the user DN as the key and stored object 
     * <code>AccountLockInfo</code> object.  It will remove the user from
     * hash map if user is physically locked out.
     *
     * @param amid The subject.
     */
    public void invalidAnswer(AMIdentity amid) {
        if (!isAccountLockout.isLockoutEnabled()) {
            model.debugMessage("Password Reset Lockout feature is disabled.");
            return;
        }

        AccountLockoutInfo pwLockoutInfo = 
            (AccountLockoutInfo) pwResetFailHash.get(amid.getUniversalId());
        pwLockoutInfo = isAccountLockout.invalidPasswd(amid, pwLockoutInfo);

        // if user is physically locked out then remove it from the map.
        if (isAccountLockout.isAccountLocked(amid)) {
            pwResetFailHash.remove(amid.getUniversalId());
            userWarningCount = -1;
        } else {
            pwResetFailHash.put(amid.getUniversalId(), pwLockoutInfo);
        }
    }


    /**
     * Returnss the warning count for the user.
     *
     * @param userDN User distinguished name.
     * @return the warning count for the user.
     */
    public int getWarnUserCount(String userDN) {
        AccountLockoutInfo acInfo =
            (AccountLockoutInfo) pwResetFailHash.get(userDN);
        return (acInfo != null) ?
            acInfo.getWarningCount() : userWarningCount;
    }

    /**
     * Removes the user DN from the fail map entry.
     *
     * @param userDN user DN
     */
    public void removeUserLockoutEntry(String userDN) {
        pwResetFailHash.remove(userDN);
        userWarningCount = 0;
    }

    /**
     * Returns true if the user is locked out from resetting password.
     *
     * @param userDN user DN
     * @return true if the user is locked out
     */
    public boolean isLockout(String userDN) {
        boolean locked = false;
        AccountLockoutInfo acInfo = (AccountLockoutInfo)pwResetFailHash.get(
            userDN);
        if ((acInfo != null) && isAccountLockout.isMemoryLocking()) {
            if (acInfo.isLockout()) {
                locked = isAccountLockout.isLockedOut(acInfo);
                if (!locked) {
                    removeUserLockoutEntry(userDN);
                }
            }
        }
        return locked;
    }
}
