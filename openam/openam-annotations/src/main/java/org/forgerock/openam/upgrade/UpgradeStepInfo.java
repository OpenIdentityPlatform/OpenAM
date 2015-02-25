/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.upgrade;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking an upgrade step and store its dependencies on other upgrade steps. This annotation will be
 * processed compilation-time, so in case there is a circular dependency the build will fail. The order of the upgrade
 * steps will be stored in a file named <code>upgradesteps.properties</code>.
 *
 * @author Peter Major
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface UpgradeStepInfo {

    /**
     * References to UpgradeStep implementations that needs to run prior to the current step.
     *
     * @return The names of other steps that needs to run before executing the currently annotated step.
     */
    String[] dependsOn() default {};
}
