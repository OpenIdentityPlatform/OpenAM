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
 * $Id: DeleteResourceBundle.java,v 1.2 2008/06/25 05:42:08 qcheng Exp $
 *
 */

package com.sun.identity.cli;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ISResourceBundle;
import com.sun.identity.sm.SMSException;
import java.util.logging.Level;

/**
 * Deletes resource bundle from data store.
 */
public class DeleteResourceBundle extends AuthenticatedCommand {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();

        String bundleName = getStringOptionValue(
            IArgument.RESOURCE_BUNDLE_NAME);
        String localeName = getStringOptionValue(
            IArgument.RESOURCE_BUNDLE_LOCALE);

        try {
            String[] params = {bundleName, localeName};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_DELETE_RESOURCE_BUNDLE", params);
            ISResourceBundle.deleteResourceBundle(adminSSOToken, bundleName,
                localeName);
            
            IOutput outputWriter = getOutputWriter();
            outputWriter.printlnMessage(
                getResourceString("resourcebundle-deleted"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_DELETE_RESOURCE_BUNDLE", params);
        } catch (SMSException e) {
            String[] args = {bundleName, localeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_RESOURCE_BUNDLE", args );
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {bundleName, localeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_RESOURCE_BUNDLE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
