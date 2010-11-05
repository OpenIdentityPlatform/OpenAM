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
 * $Id: ValidatorBase.java,v 1.2 2008/06/25 05:51:25 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * 
 * Abstract class for validators to inherit from
 * 
 */
public abstract class ValidatorBase implements IValidation {

    public ValidatorBase() throws InstallException {
        initializeValidatorMap();
    }

    /**
     * Validate method
     * 
     * 
     * @param name
     *            Validator name
     * @param value
     *            Validation value
     * @param props
     *            Map object
     * @param state
     *            IStateAccess state
     * 
     * @return ValidationResult Object
     * @throws InstallException
     * 
     */
    public ValidationResult validate(String name, String value, Map props,
            IStateAccess state) throws InstallException {
        ValidationResult validRes = null;

        if ((name != null) && (name.length() > 0)) {
            Method validatorMethod = (Method) getValidatorMap().get(name);
            if (validatorMethod != null) {
                Object[] args = { value, props, state };
                try {
                    validRes = (ValidationResult) validatorMethod.invoke(this,
                            args);
                } catch (Exception ex) {
                    Debug.log("Failed to invoke validate method for "
                            + "validator = " + name + " with exception : ",
                            ex);
                    throw new InstallException(LocalizedMessage
                            .get(LOC_VA_ERR_VAL_FAILED_IN_SILENT), ex);
                }
            }
        }

        return validRes;
    }

    /**
     * Initialize Validator Map
     * 
     */
    abstract protected void initializeValidatorMap() throws InstallException;

    /**
     * Get Validator map
     * 
     * @return Map
     */
    protected Map getValidatorMap() {
        return validMap;
    }

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();

    /*
     * Localized constants
     */
    public static String LOC_VA_ERR_VAL_METHOD_NOT_FOUND = 
        "VA_ERR_VAL_METHOD_NOT_FOUND";

    public static String LOC_VA_ERR_VAL_FAILED_IN_SILENT = 
        "VA_ERR_VAL_FAILED_IN_SILENT";

}
