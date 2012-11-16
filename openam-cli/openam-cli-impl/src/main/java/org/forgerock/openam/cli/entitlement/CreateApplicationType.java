/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 */

package org.forgerock.openam.cli.entitlement;

import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.log.Level;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Create a new ApplicationType.
 * @author Mark de Reeper mark.dereeper@forgerock.com
 */
public class CreateApplicationType extends ApplicationTypeImpl {
    
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc) throws CLIException {
        
        super.handleRequest(rc);

        String appTypeName = getStringOptionValue(PARAM_APPL_TYPE_NAME);
        
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if (datafile == null && attrValues == null) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }
        
        Map<String, Set<String>> attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);

        String[] params = {appTypeName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_CREATE_APPLICATION_TYPE", params);
        try {
            Map<String, Boolean> actions = getActions(attributeValues);
            Class searchIndex = getClassAttribute(ATTR_SEARCH_INDEX, attributeValues);
            Class saveIndex = getClassAttribute(ATTR_SAVE_INDEX, attributeValues);
            Class resourceComp = getClassAttribute(ATTR_RESOURCE_COMPARATOR, attributeValues);
            
            ApplicationType applType = 
                    new ApplicationType(appTypeName, actions, searchIndex, saveIndex, resourceComp);
            ApplicationTypeManager.saveApplicationType(getAdminSubject(), applType);
            getOutputWriter().printlnMessage(
                MessageFormat.format(getResourceString(
                "create-application-type-succeeded"), (Object[])params));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_CREATE_APPLICATION_TYPE", params);
        } catch (ClassCastException e) {
            String[] paramExs = {appTypeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION_TYPE", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (EntitlementException e) {
            String[] paramExs = {appTypeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION_TYPE", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (InstantiationException e) {
            String[] paramExs = {appTypeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION_TYPE", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IllegalAccessException e) {
            String[] paramExs = {appTypeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION_TYPE", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (CLIException e) {
            String[] paramExs = {appTypeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION_TYPE", paramExs);
            throw e;
        }        
    }
}