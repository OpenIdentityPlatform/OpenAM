/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ValidateResourceResult.java,v 1.2 2009/11/12 18:37:38 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class has an error code which indicates why the resource name is
 * valid or invalid; and also a message.
 */
public class ValidateResourceResult {
    public static final int VALID_CODE_VALID = 0;
    public static final int VALID_CODE_INVALID = 1;
    public static final int VALID_CODE_DOES_NOT_MATCH_VALID_RESOURCES = 2;

    private int validCode;
    private String message;
    private Object[] args;


    /**
     * Constructor.
     *
     * @param validCode valid code.
     * @param message Message.
     */
    public ValidateResourceResult(int validCode, String message) {
        this.validCode = validCode;
        this.message = message;
    }

    /**
     * Constructor.
     *
     * @param validCode valid code.
     * @param message Message.
     * @param args Arguments for the message.
     */
    public ValidateResourceResult(int validCode, String message, Object[] args){
        this.validCode = validCode;
        this.message = message;
        this.args = args;
    }

    /**
     * Returns message.
     *
     * @return message.
     */
    public String getMessage() {
        return getLocalizedMessage(Locale.getDefault());
    }

    /**
     * Returns localized message.
     *
     * @param locale Locale.
     * @return localized message.
     */
    public String getLocalizedMessage(Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(
            "EntitlementException", locale);
        String str = rb.getString(message);
        return ((args != null) && (args.length > 0)) ?
            MessageFormat.format(str, args) : str;
    }

    /**
     * Returns valid code.
     *
     * @return valid code.
     */
    public int getValidCode() {
        return validCode;
    }

    /**
     * Returns <code>true</code> if it is valid.
     *
     * @return <code>true</code> if it is valid.
     */
    public boolean isValid() {
        return (validCode == VALID_CODE_VALID);
    }
}
