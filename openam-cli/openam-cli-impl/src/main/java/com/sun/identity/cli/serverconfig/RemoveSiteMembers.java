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
 * $Id: RemoveSiteMembers.java,v 1.4 2009/07/07 06:14:12 veiming Exp $
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
import java.util.List;
import java.util.logging.Level;

/**
 * Remove members from site.
 */
public class RemoveSiteMembers extends ServerConfigBase {
    /**
     * Remove members from a site.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        String siteName = getStringOptionValue(IArgument.SITE_NAME);
        List serverNames = (List)rc.getOption(IArgument.SERVER_NAMES);

        String[] params = {siteName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_REMOVE_SITE_MEMBERS", params);
        IOutput outputWriter = getOutputWriter();
        
        try {
            if (SiteConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }
            
            if (SiteConfiguration.isSiteExist(adminSSOToken, siteName)) {
                SiteConfiguration.removeServersFromSite(
                    adminSSOToken, siteName, serverNames);
                outputWriter.printlnMessage(
                    getResourceString("remove-site-members-succeeded"));
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "remove-site-members-site-not-exist"));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_REMOVE_SITE_MEMBERS", params);
        } catch (SSOException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("RemoveSiteMembers.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SITE_MEMBERS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (ConfigurationException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("RemoveSiteMembers.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SITE_MEMBERS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("RemoveSiteMembers.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SITE_MEMBERS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
