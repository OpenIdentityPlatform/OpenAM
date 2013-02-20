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
 * $Id: IValidator.java,v 1.2 2008/06/25 05:41:48 qcheng Exp $
 *
 */

package com.iplanet.ums.validation;

import java.io.Serializable;

/**
 * Interface for all validators. This is handle for doing validations and should
 * be implemented by all validating classes. Implementation is specific to that
 * implementing classs with a set of validating rules
 */
public interface IValidator extends Serializable {

    /**
     * Validates value with the optional rule.
     * 
     * <pre>
     * Example: SetValidator.validate(&quot;A&quot;, &quot;A,B,C,D,F&quot;);
     * RangeValidator.validate(&quot;10&quot;, &quot;1-24&quot;);
     * MailAddressValidator.validate(&quot;bogus@sun.com&quot;, null);
     * </pre>
     * 
     * @param value
     *            attribute value to test
     * @param rule
     *            optional rule applies to each validator
     * @return true if validates successfully
     */
    boolean validate(String value, String rule);

    /**
     * Resource keys
     * 
     * package name
     */
    static final String PKG = "ums";

    /**
     * package prefix
     */
    static final String PREFIX = "validation";

    /**
     * constant string value to represent badvalue
     */
    static final String BAD_VALUE = "badvalue";

    /**
     * constant string value to represent badrule
     */
    static final String BAD_RULE = "badrule";
}
