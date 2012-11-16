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
 * $Id: GetResourceBundle.java,v 1.2 2008/06/25 05:42:08 qcheng Exp $
 *
 */

package com.sun.identity.cli;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ISResourceBundle;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * Gets resource bundle from data store.
 */
public class GetResourceBundle extends AuthenticatedCommand {
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
                "ATTEMPT_GET_RESOURCE_BUNDLE", params);
            ResourceBundle rb = ISResourceBundle.getResourceBundle(
                adminSSOToken, bundleName, localeName);
            IOutput outputWriter = getOutputWriter();

            if (rb != null) {
                for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
                    String key = (String)e.nextElement();
                    outputWriter.printlnMessage(key + "=" + rb.getString(key));
                }

                outputWriter.printlnMessage(
                    getResourceString("resourcebundle-returned"));
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_GET_RESOURCE_BUNDLE", params);
            } else {
                outputWriter.printlnMessage(
                    getResourceString("resourcebundle-not-found"));
                String[] args = {bundleName, localeName,
                    "resource bundle not found"};
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "FAILED_GET_RESOURCE_BUNDLE", args);
                throw new CLIException(
                     getResourceString("resourcebundle-not-found"),
                     ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (SSOException e) {
            String[] args = {bundleName, localeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_GET_RESOURCE_BUNDLE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (MissingResourceException e) {
            String[] args = {bundleName, localeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_GET_RESOURCE_BUNDLE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
