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
 * $Id: URLValidator.java,v 1.5 2008/06/25 05:42:31 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common.validation;

import com.sun.identity.shared.validation.ValidationException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Validator for URL format.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.validation.URLValidator}
 */
public class URLValidator
    extends ValidatorBase
{
    private static URLValidator instance = new URLValidator();

    /**
     * Avoid instantiation of this class.
     */
    private URLValidator() {
    }

    public static URLValidator getInstance() {
        return instance;
    }

    protected void performValidation(String strData)
        throws ValidationException
    {
        if ((strData == null) || (strData.trim().length() == 0)) {
            throw new ValidationException(resourceBundleName, "errorCode3"); 
        }

        try {
            new URL(strData);
        } catch (MalformedURLException e) {
            throw new ValidationException(resourceBundleName, "errorCode3"); 
        }
    }

    /** Test */
    public static void main(String[] args) {
        URLValidator inst = getInstance();
        try {
            inst.validate("http://thirdvoice.red.iplanet.com");
            inst.validate("abc");
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }
}
