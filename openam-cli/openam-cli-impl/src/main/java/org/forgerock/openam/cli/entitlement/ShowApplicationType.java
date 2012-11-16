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

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.log.Level;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Show the details of the named ApplicationType.
 * @author Mark de Reeper mark.dereeper@forgerock.com
 */
public class ShowApplicationType extends ApplicationTypeImpl {
    
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

        String[] params = {appTypeName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SHOW_APPLICATION_TYPE", params);
        ApplicationType applType = 
                ApplicationTypeManager.getAppplicationType(getAdminSubject(), appTypeName);
        
        IOutput writer = getOutputWriter();
        if (applType == null) {
            Object[] param = {appTypeName};
            writer.printlnMessage(MessageFormat.format(getResourceString(
                "show-application-type-not-found"), param));
        } else {
            displayAttrs(writer, applType);
        }
        
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "SUCCEEDED_SHOW_APPLICATION_TYPE", params);
    }

    private void displayAttrs(IOutput writer, ApplicationType applType) {
        
        writer.printlnMessage(ATTR_APPLICATIONTYPE + " " + applType.getName());

        Map<String, Boolean> actions = applType.getActions();
        if ((actions != null) && !actions.isEmpty()) {
            for (String k : actions.keySet()) {
                writer.printlnMessage(ATTR_ACTIONS + "=" + k + "=" +
                    actions.get(k).toString());
            }
        }

        displayClassName(writer, ATTR_RESOURCE_COMPARATOR, applType.getResourceComparator().getClass());
        displayClassName(writer, ATTR_SAVE_INDEX, applType.getSaveIndex().getClass());
        displayClassName(writer, ATTR_SEARCH_INDEX, applType.getSearchIndex().getClass());
    }

    private void displayClassName(IOutput writer, String attrName, Class clazz) {
        
        if (clazz != null) {
            writer.printlnMessage(attrName + "=" + clazz.getName());
        }
    }
}