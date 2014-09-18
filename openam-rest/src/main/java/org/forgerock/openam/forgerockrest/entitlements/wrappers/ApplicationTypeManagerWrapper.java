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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.forgerockrest.entitlements.wrappers;

import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Simple wrapper to allow abstraction out for the static
 * calls in {@link ApplicationTypeManager}.
 */
public class ApplicationTypeManagerWrapper {

    /**
     * Returns the {@link ApplicationType} appropriate to the
     * requested name, validating that the given user has
     * authorization to get that ApplicationType.
     *
     * @param subject The user requesting the ApplicationType
     * @param name The name of the application type requested
     * @return The requested application type or null
     */
    public ApplicationType getApplicationType(Subject subject, String name) {
        return ApplicationTypeManager.getAppplicationType(subject, name);
    }

    /**
     * Returns the names of all the {@link ApplicationType}s.
     *
     * @param subject The user requesting the names
     * @return A set of names
     */
    public Set<String> getApplicationTypeNames(Subject subject) {
        return ApplicationTypeManager.getApplicationTypeNames(subject);
    }

}
