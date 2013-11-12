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
 * $Id: FederationManager.java,v 1.9 2009/06/05 19:33:43 veiming Exp $
 *
 */

package com.sun.identity.federation.cli;

import com.sun.identity.cli.CLIDefinitionBase;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.RequestContext;
import java.util.List;

/**
 * OpenSSO CLI definition class.
 */
public class FederationManager extends CLIDefinitionBase {
    private static final String DEFINITION_CLASS =
        "com.sun.identity.federation.cli.definition.FederationManager";

    static final String DEFAULT_SPECIFICATION = 
        FedCLIConstants.SAML2_SPECIFICATION;
    
    /**
     * Constructs an instance of this class.
     */
    public FederationManager()
        throws CLIException {
        super(DEFINITION_CLASS);
    }

    /**
     * Returns product name.
     *
     * @return product name.
     */
    public String getProductName() {
        return rb.getString(FederationManagerConstants.I18N_PRODUCT_NAME);
    }

    /**
     * Returns <code>true</code> if the option is an authentication related
     * option such as user ID and password.
     *
     * @param opt Name of option.
     * @return <code>true</code> if the option is an authentication related
     *         option such as user ID and password.
     */
    public boolean isAuthOption(String opt) {
        return opt.equals(FederationManagerConstants.ARGUMENT_ADMIN_ID) ||
            opt.equals(FederationManagerConstants.ARGUMENT_PASSWORD_FILE);
    }
    
    
    /**
     * Returns specification version for ID-FF/SAML sub commands.
     *
     * @return specification version for ID-FF/SAML sub commands.
     */
    public static String getIDFFSubCommandSpecification(RequestContext rc) {
        String spec = DEFAULT_SPECIFICATION;
        List specs = rc.getOption(FedCLIConstants.SPECIFICATION_VERSION);
        if ((specs != null) && !specs.isEmpty()) {
            spec = ((String)specs.get(0)).trim();
        }
        return spec;
    }
}
