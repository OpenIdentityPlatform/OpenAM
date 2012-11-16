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
 * $Id: SetConfigurations.java,v 1.1 2009/08/19 05:40:31 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.opensso.EntitlementService;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Set entitlement service configurations.
 */
public class SetConfigurations extends AuthenticatedCommand {
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

        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }
        Map<String, Set<String>> attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_MODIFY_ENTITLEMENT_SVC", null);
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                EntitlementService.SERVICE_NAME,
                getAdminSSOToken());
            validateData(attributeValues, ssm);
            ServiceSchema gss = ssm.getGlobalSchema();
            gss.setAttributeDefaults(attributeValues);
            getOutputWriter().printlnMessage(
                getResourceString("set-entitlement-config-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_MODIFY_ENTITLEMENT_SVC", null);
        } catch (SMSException e) {
            String[] paramExs = {e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_MODIFY_ENTITLEMENT_SVC", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] paramExs = {e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_MODIFY_ENTITLEMENT_SVC", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (CLIException e) {
            String[] paramExs = {e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_MODIFY_ENTITLEMENT_SVC", paramExs);
            throw e;
        }
    }

    private void validateData(Map<String, Set<String>> attributeValues,
        ServiceSchemaManager ssm) throws SMSException, CLIException {
        ServiceSchema gss = ssm.getGlobalSchema();
        Set<String> attributeNames = gss.getAttributeSchemaNames();

        for (String k : attributeValues.keySet()) {
            if (!attributeNames.contains(k)) {
                Object[] param = {k};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "set-entitlement-config-unidentified-attr"),
                    param), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }
}
