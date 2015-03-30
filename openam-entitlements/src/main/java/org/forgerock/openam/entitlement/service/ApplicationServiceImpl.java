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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.entitlement.service;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.security.auth.Subject;

/**
 * An application service implementation that delegates to the static calls against {@link ApplicationManager}.
 *
 * @since 13.0.0
 */
public class ApplicationServiceImpl implements ApplicationService {

    private final Subject subject;
    private final String realm;

    @Inject
    public ApplicationServiceImpl(@Assisted final Subject subject, @Assisted final String realm) {
        Reject.ifNull(subject, realm);
        this.subject = subject;
        this.realm = realm;
    }

    @Override
    public Application getApplication(String applicationName) throws EntitlementException {
        return ApplicationManager.getApplication(subject, realm, applicationName);
    }

}
