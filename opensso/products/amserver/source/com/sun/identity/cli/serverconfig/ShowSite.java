/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ShowSite.java,v 1.5 2010/01/15 18:10:55 veiming Exp $
 *
 */

package com.sun.identity.cli.serverconfig;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * Show site profile.
 */
public class ShowSite extends ServerConfigBase {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String siteName = getStringOptionValue(IArgument.SITE_NAME);
        String[] params = {siteName};
        
        try {
            if (SiteConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_SHOW_SITE", params);
            if (SiteConfiguration.isSiteExist(adminSSOToken, siteName)) {
                String primaryURL = SiteConfiguration.getSitePrimaryURL(
                    adminSSOToken, siteName);
                Set failoverURLs = SiteConfiguration.getSiteSecondaryURLs(
                    adminSSOToken, siteName);
                String siteId = SiteConfiguration.getSiteID(
                    adminSSOToken, siteName);
                Object[] args = {primaryURL};
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("show-site-primaryURL"), args));

                if ((failoverURLs != null) && !failoverURLs.isEmpty()) {
                    outputWriter.printlnMessage(
                        getResourceString("show-site-secondaryURL"));
                    for (Iterator i = failoverURLs.iterator(); i.hasNext(); ) {
                        outputWriter.printlnMessage((String)i.next());
                    }
                } else {
                    outputWriter.printlnMessage(
                        getResourceString("show-site-no-secondaryURL"));
                }

                outputWriter.printlnMessage("");
                args[0] = siteId;
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("show-site-ID"), args));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("show-site-no-exists"),
                    (Object[])params));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_SHOW_SITE", params);
        } catch (SSOException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("ShowSite.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_SITE", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("ShowSite.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_SITE", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
