/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.xacml.v3;

import org.forgerock.openam.entitlement.service.ApplicationService;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;

/**
 * Describes how an Application read from XACML will be imported into OpenAM.
 *
 * @since 13.5.0
 */
class ApplicationImportStep implements PersistableImportStep<Application> {

    public static final String TYPE = "Application";

    private final DiffStatus diffStatus;
    private final Application application;
    private final ApplicationService applicationService;

    /**
     * Constructs ApplicationImportStep instance.
     *
     * @param diffStatus
     *         Import status
     * @param application
     *         The application to be imported.
     * @param applicationService
     *         Service instance to invoke any data store operations.
     */
    ApplicationImportStep(DiffStatus diffStatus, Application application,
            ApplicationService applicationService) {
        this.diffStatus = diffStatus;
        this.application = application;
        this.applicationService = applicationService;
    }

    @Override
    public DiffStatus getDiffStatus() {
        return diffStatus;
    }

    @Override
    public String getName() {
        return application.getName();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Application get() {
        return application;
    }

    @Override
    public void apply() throws EntitlementException {
        switch (diffStatus) {
        case ADD:
        case UPDATE:
            applicationService.saveApplication(application);
            break;
        }
    }

}
