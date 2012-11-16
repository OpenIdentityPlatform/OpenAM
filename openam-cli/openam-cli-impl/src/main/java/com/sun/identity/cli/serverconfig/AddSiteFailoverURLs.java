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
 * $Id: AddSiteFailoverURLs.java,v 1.4 2008/09/19 23:36:42 beomsuk Exp $
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
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

/**
 * Add secondary URLs of a site.
 */
public class AddSiteFailoverURLs extends ServerConfigBase {
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
        IOutput outputWriter = getOutputWriter();
        String siteName = getStringOptionValue(IArgument.SITE_NAME);
        List secondaryURLs = (List)rc.getOption(IArgument.SECONDARY_URLS);
        String[] params = {siteName};
        
        try {
            if (SiteConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_ADD_SITE_FAILOVER_URLS", params);
            if (SiteConfiguration.isSiteExist(adminSSOToken, siteName)) {
                SiteConfiguration.addSiteSecondaryURLs(
                    adminSSOToken, siteName, secondaryURLs);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("add-site-secondary-urls-succeeded"),
                    (Object[])params));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("add-site-secondary-urls-no-exists"),
                    (Object[])params));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_ADD_SITE_FAILOVER_URLS", params);
        } catch (SSOException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("AddSiteFailoverURLs.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SITE_FAILOVER_URLS", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (ConfigurationException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("AddSiteFailoverURLs.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SITE_FAILOVER_URLS", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("AddSiteFailoverURLs.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SITE_FAILOVER_URLS", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
