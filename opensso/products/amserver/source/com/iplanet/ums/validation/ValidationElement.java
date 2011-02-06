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
 * $Id: ValidationElement.java,v 1.3 2008/06/25 05:41:48 qcheng Exp $
 *
 */

package com.iplanet.ums.validation;

import java.io.Serializable;

/**
 * Represents a validator/rule pair. A validator/rule pair is required to
 * represent a complete validation rountine.
 *
 * @supported.all.api
 */
public class ValidationElement implements Serializable {

    private String _validator = null;

    private String _rule = null;

    /**
     * Construct an validator rule pair object Initialises the ValidationElement
     * object with validator and rule
     * 
     * @param validator
     *            validator
     * @param rule
     *            rule applies to the validator
     */
    public ValidationElement(String validator, String rule) {
        _validator = validator;
        _rule = rule;
    }

    /**
     * Returns a validator
     * 
     * @return returns a validator
     */
    public String getValidator() {
        return _validator;
    }

    /**
     * Returns a rule
     * 
     * @return returns a rule
     */
    public String getRule() {
        return _rule;
    }
}
