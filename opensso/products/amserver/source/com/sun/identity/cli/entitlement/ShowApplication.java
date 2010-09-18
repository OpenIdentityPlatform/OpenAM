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
 * $Id4
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.log.Level;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

public class ShowApplication extends ApplicationImpl {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throw CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String appName = getStringOptionValue(PARAM_APPL_NAME);

        String[] params = {realm, appName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SHOW_APPLICATION", params);
        try {
            Application appl = ApplicationManager.getApplication(
                getAdminSubject(),
                realm, appName);
            IOutput writer = getOutputWriter();

            if (appl == null) {
                Object[] param = {appName};
                writer.printlnMessage(MessageFormat.format(getResourceString(
                    "show-application-not-found"), param));
            } else {
                displayAttrs(writer, appl);
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_SHOW_APPLICATION", params);
        } catch (EntitlementException ex) {
            String[] paramsEx = {realm, appName, ex.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_SHOW_APPLICATION", paramsEx);
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void displayAttrs(IOutput writer, Application appl) {
        ApplicationType applType = appl.getApplicationType();
        writer.printlnMessage(ATTR_APPLICATIONTYPE + '=' + applType.getName());

        String description = appl.getDescription();
        if (description == null) {
            description = "";
        }
        writer.printlnMessage(ATTR_DESCRIPTION + "=" + description);
        writer.printlnMessage(ATTR_CREATED_BY + "=" + appl.getCreatedBy());
        writer.printlnMessage(ATTR_CREATION_DATE + "=" +
            appl.getCreationDate());
        writer.printlnMessage(ATTR_LAST_MODIFIED_BY + "=" +
            appl.getLastModifiedBy());
        writer.printlnMessage(ATTR_LAST_MODIFICATION_DATE + "=" +
            appl.getLastModifiedDate());

        Map<String, Boolean> actions = appl.getActions();
        if ((actions != null) && !actions.isEmpty()) {
            for (String k : actions.keySet()) {
                writer.printlnMessage(ATTR_ACTIONS + "=" + k + "=" +
                    actions.get(k).toString());
            }
        }

        displayAttributes(writer, ATTR_SUBJECT_ATTRIBUTE_NAMES,
            appl.getAttributeNames());
        displayAttributes(writer, ATTR_RESOURCES, appl.getResources());
        displayAttributes(writer, ATTR_CONDITIONS, appl.getConditions());
        displayAttributes(writer, ATTR_SUBJECTS, appl.getSubjects());

        displayClassName(writer, ATTR_ENTITLEMENT_COMBINER,
            appl.getEntitlementCombinerClass());
        displayClassName(writer, ATTR_RESOURCE_COMPARATOR, 
            appl.getResourceComparatorClass());
        displayClassName(writer, ATTR_SAVE_INDEX, appl.getSaveIndexClass());
        displayClassName(writer, ATTR_SEARCH_INDEX, appl.getSearchIndexClass());
    }

    private void displayClassName(IOutput writer, String attrName,
        Class clazz) {
        if (clazz != null) {
            writer.printlnMessage(attrName + "=" + clazz.getName());
        }
    }

    private void displayAttributes(IOutput writer, String attrName,
        Set<String> values) {
        if ((values != null) && !values.isEmpty()) {
            for (String v : values) {
                writer.printlnMessage(attrName + "=" + v);
            }
        }
    }
}
