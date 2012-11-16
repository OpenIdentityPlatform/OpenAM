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

import com.sun.identity.cli.*;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.log.Level;
import java.text.MessageFormat;
import java.util.List;
import javax.security.auth.Subject;

/**
 * Delete the list of named ApplicationTypes.
 * @author Mark de Reeper mark.dereeper@forgerock.com
 */
public class DeleteApplicationTypes extends ApplicationTypeImpl {

    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc) throws CLIException {
        
        super.handleRequest(rc);

        List<String> appTypeNames = (List)rc.getOption(PARAM_APPL_TYPE_NAMES);
        String[] param = {appTypeNames.toString()};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_DELETE_APPLICATION_TYPES", param);

        Subject adminSubject = getAdminSubject();
        try {
            for (String appTypeName : appTypeNames) {
                // Check it exists before deleting
                ApplicationType applType = 
                        ApplicationTypeManager.getAppplicationType(adminSubject, appTypeName);
                if (applType != null) {
                    ApplicationTypeManager.removeApplicationType(adminSubject, appTypeName);
                }
            }
            
            getOutputWriter().printlnMessage(MessageFormat.format(getResourceString(
                "delete-application-types-succeeded"), (Object[])param));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_DELETE_APPLICATION_TYPES", param);
        } catch (EntitlementException e) {
            String[] params = {appTypeNames.toString(), e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_APPLICATION_TYPES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }    
}