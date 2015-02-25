/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EntitlementException.java,v 1.2 2009/09/03 17:06:23 veiming Exp $
 *
 * Portions copyright 2010-2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Entitlement related exception.
 *
 * @supported.all.api
 */
public class EntitlementException extends Exception {
    public static final String RES_BUNDLE_NAME = "EntitlementException";

    /*
     * Selected error code constants. See EntitlementException.properties for full list.
     */

    public static final int UNABLE_TO_CREATE_POLICY = 1;
    public static final int INVALID_PRIVILEGE_CLASS = 2;
    public static final int EMPTY_PRIVILEGE_NAME = 3;
    public static final int NULL_ENTITLEMENT = 4;
    public static final int UNSUPPORTED_OPERATION = 5;
    public static final int INVALID_APPLICATION_CLASS = 6;
    public static final int INVALID_XML = 7;
    public static final int INVALID_WSDL_LOCATION = 8;
    public static final int MISSING_PRIVILEGE_JSON = 9;
    public static final int SESSION_HAS_EXPIRED = 10;
    public static final int INVALID_JSON = 11;
    public static final int MISSING_PRIVILEGE_NAME = 12;
    public static final int POLICY_NAME_MISMATCH = 13;
    public static final int RESOURCE_LIST_EMPTY = 14;

    public static final int UNABLE_TO_SERIALIZE_OBJECT = 200;
    public static final int NO_SUCH_POLICY = 203;
    public static final int POLICY_ALREADY_EXISTS = 217;
    public static final int APPLICATION_ALREADY_EXISTS = 228;
    public static final int APPLICATION_NAME_MISMATCH = 229;
    public static final int APP_RETRIEVAL_ERROR = 248;
    public static final int NO_SUCH_REFERRAL_PRIVILEGE = 263;

    public static final int INCONSISTENT_WILDCARDS = 300;
    public static final int INVALID_PORT = 301;
    public static final int MALFORMED_URL = 302;
    public static final int INVALID_RESOURCE = 303;
    public static final int INVALID_ENTITLEMENT_SUBJECT_CLASS = 310;
    public static final int INVALID_CLASS = 311;

    public static final int INVALID_APP_TYPE = 317;
    public static final int INVALID_APP_REALM = 318;

    public static final int NO_SUCH_APPLICATION = 321;
    public static final int NOT_FOUND = 325;

    public static final int PERMISSION_DENIED = 326;

    public static final int SUBJECT_REQUIRED = 327;
    public static final int INVALID_SEARCH_FILTER = 328;
    public static final int UNKNOWN_POLICY_CLASS = 329;
    public static final int UNKNOWN_RESOURCE_ATTRIBUTE_CLASS = 330;
    public static final int POLICY_CLASS_CAST_EXCEPTION = 331;
    public static final int POLICY_CLASS_NOT_INSTANTIABLE = 332;
    public static final int POLICY_CLASS_NOT_ACCESSIBLE = 333;
    public static final int INVALID_PROPERTY_VALUE = 400;
    public static final int INVALID_VALUE = 401;
    public static final int START_DATE_AFTER_END_DATE = 402;
    public static final int APP_NOT_CREATED_POLICIES_EXIST = 404;
    public static final int INVALID_PROPERTY_VALUE_UNKNOWN_VALUE = 405;
    public static final int IP_CONDITION_CONFIGURATION_REQUIRED = 406;

    public static final int MISSING_RESOURCE = 420;
    public static final int JSON_PARSE_ERROR = 425;
    public static final int AUTHENTICATION_ERROR = 434;
    public static final int CLIENT_IP_EMPTY = 437;
    public static final int RESOURCE_ENV_NOT_KNOWN = 438;

    public static final int CONDITION_EVALUTATION_FAILED = 510;

    public static final int INVALID_OAUTH2_SCOPE = 700;
    public static final int AUTH_LEVEL_NOT_INTEGER = 710;
    public static final int PROPERTY_VALUE_NOT_DEFINED = 711;
    public static final int AUTH_LEVEL_NOT_INT_OR_SET = 712;
    public static final int AUTH_SCHEME_NOT_FOUND = 713;
    public static final int INVALID_ADMIN = 720;
    public static final int AM_ID_SUBJECT_MEMBERSHIP_EVALUATION_ERROR = 721;
    public static final int UNABLE_TO_PARSE_SSOTOKEN_AUTHINSTANT = 730;
    public static final int AT_LEAST_ONE_OF_TIME_PROPS_SHOULD_BE_DEFINED = 740;
    public static final int PAIR_PROPERTY_NOT_DEFINED = 741;
    public static final int END_IP_BEFORE_START_IP = 750;

    public static final int PROPERTY_IS_NOT_AN_INTEGER = 800;
    public static final int PROPERTY_IS_NOT_A_SET = 801;
    public static final int PROPERTY_CONTAINS_BLANK_VALUE = 802;

    public static final int INTERNAL_ERROR = 900;
    public static final int REALM_NOT_FOUND = 901;

    private int errorCode;
    private String message;
    private Object[] params;

    /**
     * Creates an entitlement exception.
     *
     * @param errorCode Error code.
     */
    public EntitlementException(int errorCode) {
        this.errorCode = errorCode;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Creates an entitlement exception.
     * 
     * @param errorCode Error code.
     * @param params Parameters for formatting the message string.
     */
    public EntitlementException(int errorCode, Object... params) {
        this.errorCode = errorCode;
        this.params = params;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Creates an entitlement exception.
     *
     * @param errorCode Error code.
     * @param cause Root cause.
     */
    public EntitlementException(int errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Creates an entitlement exception.
     *
     * @param errorCode Error code.
     * @param params Parameters for formatting the message string.
     * @param cause Root cause.
     */
    public EntitlementException(int errorCode, Object[] params, Throwable cause)
    {
        super(cause);
        this.errorCode = errorCode;
        this.params = params;
        this.message = getLocalizedMessage(Locale.getDefault());
    }

    public EntitlementException(int errorCode, Throwable cause, Object...params) {
        this(errorCode, params, cause);
    }

    /**
     * Returns error code.
     * 
     * @return error code.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns exception message.
     *
     * @return exception message.
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Returns localized exception message.
     *
     * @return localized exception message.
     */
    @Override
    public String getLocalizedMessage() {
        return message;
    }

    /**
     * Returns localized exception message using the errorCode as key.
     *
     * @param locale Locale of the message.
     * @return localized exception message.
     */
    public String getLocalizedMessage(Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(RES_BUNDLE_NAME, locale);
        String msg = rb.getString(Integer.toString(errorCode));
        return (params != null) ? MessageFormat.format(msg, params) : msg;
    }
}
