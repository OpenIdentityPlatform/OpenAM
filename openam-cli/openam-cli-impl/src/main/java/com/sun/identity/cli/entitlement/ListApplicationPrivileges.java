/*
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
 * $Id: ListApplicationPrivileges.java,v 1.1 2009/11/10 19:01:04 veiming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;

import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import jakarta.inject.Inject;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public class ListApplicationPrivileges extends ApplicationPrivilegeBase {

    @Inject
    public ListApplicationPrivileges(ResourceTypeService resourceTypeService,
            ApplicationServiceFactory applicationServiceFactory) {
        super(resourceTypeService, applicationServiceFactory);
    }

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
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String[] params = {realm};

        Subject userSubject = SubjectUtils.createSubject(
            getAdminSSOToken());
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance(realm, userSubject);
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SHOW_APPLICATION_PRIVILEGE", params);
        Set<String> names = apm.search(Collections.EMPTY_SET);
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "SUCCEEDED_LIST_APPLICATION_PRIVILEGES", params);

        if (names.isEmpty()) {
            getOutputWriter().printlnMessage(getResourceString(
                "list-application-privileges-no-privileges"));
        } else {
            IOutput outputWriter = getOutputWriter();
            for (String name : names) {
                outputWriter.printlnMessage(name);
            }
        }
    }
}
