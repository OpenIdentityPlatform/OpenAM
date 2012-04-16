/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.authentication.modules.ldap;

/**
 * The enum wraps up all of the possible user states in the LDAP module
 * 
 * @author steve
 */
public enum ModuleState {
    USER_NOT_FOUND(1, "userNotFound"),
            
    CONFIG_ERROR(4, "configError"),

    CANNOT_CONTACT_SERVER(5, "contactServer"),
    
    PASSWORD_EXPIRED_STATE(20, "passwordExpired"),

    PASSWORD_EXPIRING(21, "passwordExpiring"),
    
    GRACE_LOGINS(22, "graceLogins"),

    PASSWORD_MISMATCH(23, "passwordMismatch"),

    PASSWORD_NOT_UPDATE(25, "notUpdate"),

    SUCCESS(26, "success"),

    WRONG_PASSWORD_ENTERED(27, "wrongPassword"),

    PASSWORD_UPDATED_SUCCESSFULLY(28, "updateSuccessfully"),

    USER_PASSWORD_SAME(29, "passwordSame"),

    PASSWORD_MIN_CHARACTERS(30, "minCharacters"),

    SERVER_DOWN(31, "serverDown"),
    
    PASSWORD_RESET_STATE(32, "resetState"),
    
    USER_FOUND(33, "userFound"),
    
    ACCOUNT_LOCKED(34, "accountLocked"),

    INSUFFICIENT_PASSWORD_QUALITY(35, "insufficientPasswordQuality"),
    
    MUST_SUPPLY_OLD_PASSWORD(36, "mustSupplyOldPassword"),
    
    PASSWORD_IN_HISTORY(37, "passwordInHistory"),
    
    PASSWORD_MOD_NOT_ALLOWED(38, "modNotAllowed"),
    
    PASSWORD_TOO_SHORT(39, "passwordTooShort"),
    
    PASSWORD_TOO_YOUNG(40, "passwordTooYoung"),
    
    TIME_BEFORE_EXPIRATION(41, "timeBeforeExpiration"),
    
    CHANGE_AFTER_RESET(42, "changeAfterReset");

    private final int intValue;
    private final String name;

    private ModuleState(final int intValue, final String name) {
        this.intValue = intValue;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    int intValue() {
        return intValue;
    }    
}
