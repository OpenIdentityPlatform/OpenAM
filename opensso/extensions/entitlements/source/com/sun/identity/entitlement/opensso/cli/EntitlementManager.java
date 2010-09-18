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
 * $Id: EntitlementManager.java,v 1.2 2009/07/21 08:43:40 veiming Exp $
 *
 */

package com.sun.identity.entitlement.opensso.cli;

import com.sun.identity.cli.AccessManagerConstants;
import com.sun.identity.cli.CLIDefinitionBase;
import com.sun.identity.cli.CLIException;

/**
 * OpenSSO CLI definition class.
 */
public class EntitlementManager extends CLIDefinitionBase {
    private static final String DEFINITION_CLASS =
        "com.sun.identity.entitlement.opensso.cli.definition.Entitlement";
   
    /**
     * Constructs an instance of this class.
     */
    public EntitlementManager()
        throws CLIException {
        super(DEFINITION_CLASS);
    }

    /**
     * Returns product name.
     *
     * @return product name.
     */
    public String getProductName() {
        return rb.getString(AccessManagerConstants.I18N_PRODUCT_NAME);
    }

    public boolean isAuthOption(String arg0) {
        return true;
    }

}
