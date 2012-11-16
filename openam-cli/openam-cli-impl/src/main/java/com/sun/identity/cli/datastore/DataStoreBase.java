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
 * $Id: DataStoreBase.java,v 1.1 2009/02/11 17:21:31 veiming Exp $
 *
 */

package com.sun.identity.cli.datastore;

import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;

/**
 * Base class for all datastore sub command implementation classes.
 */
public abstract class DataStoreBase extends AuthenticatedCommand {
    protected void validateRealm(String realm)
        throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        try {
            new OrganizationConfigManager(adminSSOToken, realm);
        } catch (SMSException e) {
            Object[] msgArg = {realm};
            throw new CLIException(MessageFormat.format(getResourceString(
                "realm-does-not-exist"), msgArg),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
