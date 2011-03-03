/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: IdRepoAttributeValidatorImpl.java,v 1.1 2009/11/10 01:48:01 hengming Exp $
 */
package com.sun.identity.idm.server;

import java.util.Map;
import java.util.Set;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;

/**
 * 
 * This interface defines the methods which validate attributes.
 *
 */
public class IdRepoAttributeValidatorImpl implements IdRepoAttributeValidator {
    private static final String PROP_MIN_PASSWORD_LENGTH =
        "minimumPasswordLength";
    private static final String ATTR_USER_PASSWORD = "userpassword";
    private int minPasswordLength = 0;
    private static Debug debug = Debug.getInstance("amIdm");

    /**
     * Initialization paramters as configred for a given plugin.
     * 
     * @param configParams configuration parameters
     */
    public void initialize(Map<String, Set<String>> configParams) {
        if ((configParams == null) || configParams.isEmpty()) {
            return;
        }

        for(String name : configParams.keySet()) {
            if (name.equals(PROP_MIN_PASSWORD_LENGTH)) {
                Set<String> values = configParams.get(name);
                if ((values != null) && (!values.isEmpty())) {
                    String value = values.iterator().next();
                    try {
                         minPasswordLength = Integer.parseInt(value);
                         if (minPasswordLength < 0) {
                             minPasswordLength = 0;
                         }
                    } catch (NumberFormatException nfe) {
                        if (debug.warningEnabled()) {
                            debug.warning("IdRepoAttributeValidatorImpl." +
                                "initialize:", nfe);
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates attributes for create or modify operation. 
     *
     * @param attrMap attributes map to be validated.
     * @param idOp operaton which is ethier <code>IdOperation.CREATE</code> or
     *     <code>IdOperation.EDIT</code>
     * @throws IdRepoException If attributes can't be validated or there are
     *     repository related error conditions.
     */
    public void validateAttributes(Map<String, Set<String>> attrMap,
        IdOperation idOp) throws IdRepoException {

        if (minPasswordLength == 0) {
            return;
        }

        attrMap = new CaseInsensitiveHashMap(attrMap);

        if (!attrMap.containsKey(ATTR_USER_PASSWORD)) {
            if (idOp.equals(IdOperation.CREATE)) {
                Object[] args = { "" + minPasswordLength };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "230",
                    args);
            }
        } else {
            Set<String> values = attrMap.get(ATTR_USER_PASSWORD);
            if ((values == null) || (values.isEmpty())) {
                Object[] args = { "" + minPasswordLength };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "230",
                    args);
            } else {
                String password = values.iterator().next();
                if (password.length() < minPasswordLength) {
                    Object[] args = { "" + minPasswordLength };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "230",
                        args);
                }
            }
        }
    }

}
