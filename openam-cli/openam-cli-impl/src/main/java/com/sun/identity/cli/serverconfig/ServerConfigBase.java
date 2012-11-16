/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServerConfigBase.java,v 1.3 2008/11/07 20:27:05 veiming Exp $
 *
 */

package com.sun.identity.cli.serverconfig;

import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.RequestContext;
import java.net.MalformedURLException;
import java.net.URL;

public class ServerConfigBase extends AuthenticatedCommand {
    protected static final String DEFAULT_SVR_CONFIG = "default";
    
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        String serverName = getStringOptionValue(IArgument.SERVER_NAME);
        if ((serverName != null) && (serverName.trim().length() > 0) &&
            !serverName.equals(DEFAULT_SVR_CONFIG)) {
            try {
                URL url = new URL(serverName);
                if (url.getPort() == -1) {
                    throw new CLIException(
                        getResourceString("server-config-port-missing"),
                        ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
                if (url.getPath().length() == 0) {
                    throw new CLIException(
                        getResourceString("server-config-uri-missing"),
                        ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }

            } catch (MalformedURLException e) {
                throw new CLIException(e,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }
}
