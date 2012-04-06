/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StringValidator.java,v 1.2 2008/06/25 05:51:24 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class StringValidator extends ValidatorBase {

    public StringValidator() throws InstallException {
        super();
    }

    public ValidationResult isPositiveInteger(String str, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((str != null) && (str.trim().length() >= 0)) {
            try {
                int strVal = Integer.parseInt(str);
                if (strVal >= 0) {
                    returnMessage = LocalizedMessage.get(LOC_VA_MSG_NUM_STR,
                            new Object[] { str });
                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                } else {
                    Debug.log("StringValidator : "
                            + "Value passed is a negative integer");
                }
            } catch (NumberFormatException nfe) {
                Debug.log("StringValidator.isPositiveInteger(..) "
                        + "threw exception :", nfe);
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_NON_NUM_STR,
                    new Object[] { str });
        }

        Debug.log("StringValidator : Is string : " + str + " valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);

    }

    public ValidationResult isStringValid(String str, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if ((str != null) && (str.trim().length() >= 0)) {
            returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_STR,
                    new Object[] { str });
            validRes = ValidationResultStatus.STATUS_SUCCESS;
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_STR,
                    new Object[] { str });
        }

        Debug.log("StringValidator : Is string : " + str + " valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    public ValidationResult isKeyValid(String key, Map props, 
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        try {
            if ((key != null) && (key.length() > 0)) {
                String minSize = (String) props.get(STR_VAL_MIN_DIGITS);
                if ((minSize != null) && (minSize.length() > 0)) {
                    int minLen = Integer.parseInt(minSize);
                    Debug.log("StringValidator : key min length = " + minLen);
                    int passLen = key.length();
                    if (passLen >= minLen) {
                        validRes = ValidationResultStatus.STATUS_SUCCESS;
                    } else {
                        Debug.log("StringValidator : Length of key is " 
                                + "invalid");
                    }
                } else {
                    // User did not enter a min length
                    validRes = ValidationResultStatus.STATUS_SUCCESS;
                }
            }
        } catch (Exception ex) {
            Debug.log("StringValidator : Failed to read key with ex :", ex);
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {           
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_KEY);
        } else {
            Debug.log("StringValidator : key is valid");
            returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_KEY);
        }

        Debug.log("StringValidator : Is Key valid ? " 
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    public void initializeValidatorMap() throws InstallException {

        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap().put("VALID_STRING",
                    this.getClass().getMethod("isStringValid", paramObjs));

            getValidatorMap().put("POSITIVE_INTEGER",
                    this.getClass().getMethod("isPositiveInteger", paramObjs));

            getValidatorMap().put("VALID_KEY",
                    this.getClass().getMethod("isKeyValid", paramObjs));

        } catch (NoSuchMethodException nsme) {
            Debug.log("StringValidator: "
                    + "NoSuchMethodException thrown while loading method :",
                    nsme);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("StringValidator: "
                    + "SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("StringValidator: "
                    + "Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }

    }

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();

    /*
     * Localized messages
     */
    public static String LOC_VA_WRN_IN_VAL_STR = "VA_WRN_IN_VAL_STR";

    public static String LOC_VA_MSG_VAL_STR = "VA_MSG_VAL_STR";

    public static String LOC_VA_MSG_NUM_STR = "VA_MSG_NUM_STR";

    public static String LOC_VA_WRN_NON_NUM_STR = "VA_WRN_NON_NUM_STR";

    public static String LOC_VA_MSG_VAL_KEY = "VA_MSG_VAL_KEY";

    public static String LOC_VA_WRN_IN_VAL_KEY = "VA_WRN_IN_VAL_KEY";

    /*
     * String constants
     */
    public static String STR_VAL_MIN_DIGITS = "minLen";

}
