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
 * $Id: RealmGetAttributeValues.java,v 1.4 2008/06/25 05:42:16 qcheng Exp $
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gets attribute values of  realm.
 */
public class RealmGetAttributeValues extends AuthenticatedCommand {
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
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        IOutput outputWriter = getOutputWriter();

        try {
            String[] params = {realm, serviceName};

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_GET_REALM_SERVICE_ATTR_VALUES", params);
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, realm);
            Map attributeValues = ocm.getAttributes(serviceName);
            
            if ((attributeValues != null) && !attributeValues.isEmpty()) {
                outputWriter.printlnMessage(FormatUtils.printAttributeValues(
                    getResourceString("get-attr-values-of-realm-result"),
                    attributeValues));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-attr-values-of-realm-no-values"),
                    (Object[])params));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_GET_REALM_SERVICE_ATTR_VALUES", params);
        } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmAttributeValues.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_REALM_SERVICE_ATTR_VALUES", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
