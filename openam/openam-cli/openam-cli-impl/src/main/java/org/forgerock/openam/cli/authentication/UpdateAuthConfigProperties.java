/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openam.cli.authentication;

import com.iplanet.sso.SSOException;
import com.sun.identity.cli.authentication.AuthOptions;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Peter Major
 */
public class UpdateAuthConfigProperties extends AuthenticatedCommand {

    private static final List<String> VALID_KEYS = Arrays.asList(new String[]{
                "iplanet-am-auth-login-failure-url",
                "iplanet-am-auth-login-success-url",
                "iplanet-am-auth-post-login-process-class"
            });

    @Override
    public void handleRequest(RequestContext rc) throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String configName = getStringOptionValue(AuthOptions.AUTH_CONFIG_NAME);

        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List<String> listValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (listValues == null)) {
            throw new CLIException(
                    getResourceString(
                    "authentication-set-auth-config-props-missing-data"),
                    ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        String[] params = {realm, configName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SET_AUTH_CONFIG_ENTRIES", params);
        Map<String, Set<String>> attributeValues = AttributeValues.parse(
                getCommandManager(), datafile, listValues);
        validateProperties(attributeValues);

        Map configData = new HashMap();
        configData.putAll(attributeValues);
        try {
            AMAuthConfigUtils.replaceNamedConfig(configName, 0, configData, realm, ssoToken);
            getOutputWriter().printlnMessage(getResourceString(
                    "authentication-set-auth-config-props-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_SET_AUTH_CONFIG_ENTRIES", params);
        } catch (SMSException smse) {
            debugError("GetAuthConfigurationEntries.handleRequest", smse);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_SET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(smse, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException ssoe) {
            debugError("GetAuthConfigurationEntries.handleRequest", ssoe);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_SET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(ssoe, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (AMConfigurationException amce) {
            debugError("GetAuthConfigurationEntries.handleRequest", amce);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_SET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(amce, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void validateProperties(Map<String, Set<String>> attributeValues)
            throws CLIException {
        for (String key : attributeValues.keySet()) {
            if (!VALID_KEYS.contains(key)) {
                throw new CLIException(MessageFormat.format(
                        getResourceString("authentication-set-auth-config-props-invalid-key"), key),
                        ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }
}
