/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.cli.serverconfig;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cli.serverconfig.ServerConfigBase;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * CLI hook for settings the ID of a site
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class SetSiteID extends ServerConfigBase {
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
        String siteID = getStringOptionValue(IArgument.SITE_ID);
        String[] params = {siteName, siteID};

        try {
            if (SiteConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_SET_SITE_ID", params);
            if (SiteConfiguration.isSiteExist(adminSSOToken, siteName)) {
                SiteConfiguration.setSiteID(
                    adminSSOToken, siteName, siteID);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("set-site-id-succeeded"),
                    (Object[])params));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("set-site-id-no-exists"),
                    (Object[])params));
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_SET_SITE_ID", params);
        } catch (SSOException ssoe) {
            String[] args = {siteName, siteID, ssoe.getMessage()};
            debugError("SetSitePrimaryURL.handleRequest", ssoe);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_SITE_ID", args);
            throw new CLIException(ssoe,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (ConfigurationException ce) {
            String[] args = {siteName, siteID, ce.getMessage()};
            debugError("SetSitePrimaryURL.handleRequest", ce);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_SITE_ID", args);
            throw new CLIException(ce,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException smse) {
            String[] args = {siteName, siteID, smse.getMessage()};
            debugError("SetSitePrimaryURL.handleRequest", smse);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_SITE_ID", args);
            throw new CLIException(smse,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
