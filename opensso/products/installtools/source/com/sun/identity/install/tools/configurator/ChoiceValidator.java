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
 * $Id: ChoiceValidator.java,v 1.2 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * System supplied Choice Validator class.
 */
public class ChoiceValidator extends ValidatorBase {

    public ChoiceValidator() throws InstallException {
        super();
    }

    /**
     * Method isChoiceValid
     *
     *
     * @param choice
     * @param props
     * @param IStateAccess
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isChoiceValid(String choice, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        boolean ignoreCase = getIgnoreCaseStat(props);

        if ((choice != null) && (choice.length() > 0)) {
            Iterator iter = props.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry me = (Map.Entry) iter.next();
                String key = (String) me.getKey();
                // We are only interested with keys starting with "value"
                if (key.startsWith(STR_VAL_MATCH_PATTERN)) {
                    String val = (String) me.getValue();
                    if (val != null) {
                        if (ignoreCase) {
                            if (choice.equalsIgnoreCase(val)) {
                                validRes = 
                                    ValidationResultStatus.STATUS_SUCCESS;
                                Debug.log("ChoiceValidator:isChoiceValid(..) "
                                        + " comparing value = " + val
                                        + ", with choice " + "=" + choice
                                        + ", ignore case = " + ignoreCase
                                        + ", comparison result = " + true);
                                break;
                            }
                        } else {
                            if (choice.equals(val)) {
                                validRes = 
                                    ValidationResultStatus.STATUS_SUCCESS;
                                Debug.log("ChoiceValidator:isChoiceValid(..) "
                                        + " comparing value = " + val
                                        + ", with choice " + "=" + choice
                                        + ", ignore case = " + ignoreCase
                                        + ", comparison result = " + true);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_CHOICE,
                    new Object[] { choice });
        } else {
            returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_CHOICE,
                    new Object[] { choice });
        }

        Debug.log("ChoiceValidator:isChoiceValid(..) Is choice valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /*
     * Private helper function to get Ignore Case status
     * @param props
     * @return boolean
     * 
     */
    boolean getIgnoreCaseStat(Map props) {
        boolean result = false;
        if (props.containsKey(STR_IGNORE_CASE_KEY)) {
            String ignoreCaseStat = (String) props.get(STR_IGNORE_CASE_KEY);
            if ((ignoreCaseStat != null) && (ignoreCaseStat.length() > 0)) {
                result = Boolean.valueOf(ignoreCaseStat).booleanValue();
            }
        }
        Debug.log("ChoiceValidator:getIgnoreCaseStat(..) = " + result);
        return result;
    }

    public void initializeValidatorMap() throws InstallException {

        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap().put("VALID_CHOICE",
                    this.getClass().getMethod("isChoiceValid", paramObjs));
        } catch (NoSuchMethodException nsme) {
            Debug.log("ChoiceValidator: "
                    + "NoSuchMethodException thrown while loading method :",
                    nsme);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("ChoiceValidator: "
                    + "SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("ChoiceValidator: "
                    + "Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }
    }

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();

    public static String STR_IGNORE_CASE_KEY = "ignorecase";

    public static String STR_VAL_MATCH_PATTERN = "value";

    public static String LOC_VA_MSG_VAL_CHOICE = "VA_MSG_VAL_CHOICE";

    public static String LOC_VA_WRN_IN_VAL_CHOICE = "VA_WRN_IN_VAL_CHOICE";

}
